import java.io.File
import org.gradle.api.attributes.Attribute

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.akhilflix.app"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":plugin-api"))

    val composeBom = platform("androidx.compose:compose-bom:2024.01.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Networking (config + course catalogs)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Images
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Local storage (My List + config cache)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Advanced player (ExoPlayer / media3) — HLS, DASH, MP4, MKV, WebM
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.2.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// ---------------------------------------------------------------------------
// Plugin DEX build (CloudStream-style).
// Produces build/plugin/akhilflix-plugin.dex containing ONLY the plugin's
// own classes. Libraries (Compose, Retrofit, ExoPlayer, ...) stay in the
// host app and are passed to d8 as --lib so they are NOT bundled into the
// DEX — the plugin resolves them from the host at runtime.
// ---------------------------------------------------------------------------
tasks.register("buildPluginDex") {
    dependsOn("assembleRelease")
    doLast {
        val buildDir = project.layout.buildDirectory.get().asFile

        // 1. Extract classes.jar from the release AAR.
        val aar = File(buildDir, "outputs/aar/plugin-release.aar")
        check(aar.exists()) { "AAR not found: ${aar.absolutePath}. Run assembleRelease first." }
        val classesJarDir = File(buildDir, "tmp/plugin-classes")
        project.copy {
            from(project.zipTree(aar))
            include("classes.jar")
            into(classesJarDir)
        }
        val classesJar = File(classesJarDir, "classes.jar")
        check(classesJar.exists()) { "classes.jar not found in ${aar.absolutePath}" }

        // 2. Locate d8 in the Android SDK.
        val sdkDir = android.sdkDirectory
        val d8File = File(sdkDir, "build-tools").listFiles()
            ?.sortedDescending()
            ?.firstNotNullOfOrNull { dir -> File(dir, "d8").takeIf { it.exists() } }
            ?: error("d8 not found in Android SDK build-tools under ${sdkDir.absolutePath}")

        // 3. Collect --lib entries: android.jar + every dependency's classes.
        // Resolve via an artifact view requesting "jar" artifacts so AARs
        // are transformed to their classes.jar and Gradle can match a
        // variant of the :plugin-api project dependency. (A plain .files
        // resolve of either classpath fails: the consumer never asks for
        // an artifactType, so no :plugin-api variant matches.)
        val libArgs = mutableListOf<String>()
        val androidJar = File(sdkDir, "platforms/android-${android.compileSdk}/android.jar")
        if (androidJar.exists()) libArgs += listOf("--lib", androidJar.absolutePath)
        val libFiles = configurations.getByName("releaseCompileClasspath")
            .incoming
            .artifactView {
                lenient(true)
                attributes {
                    attribute(Attribute.of("artifactType", String::class.java), "jar")
                }
            }
            .artifacts
            .artifactFiles
            .files
        libFiles.forEach { file ->
            libArgs += listOf("--lib", file.absolutePath)
        }

        // 4. Run d8 -> classes.dex, then rename to akhilflix-plugin.dex.
        val dexOutDir = File(buildDir, "plugin")
        project.exec {
            commandLine(
                d8File.absolutePath,
                "--release",
                "--output", dexOutDir.absolutePath,
                *libArgs.toTypedArray(),
                classesJar.absolutePath
            )
        }
        val dex = File(dexOutDir, "classes.dex")
        val pluginDex = File(dexOutDir, "akhilflix-plugin.dex")
        if (dex.exists()) dex.renameTo(pluginDex)
        check(pluginDex.exists()) { "DEX build failed: ${pluginDex.absolutePath} not produced." }
        println("Plugin DEX ready: ${pluginDex.absolutePath} (${pluginDex.length()} bytes)")
    }
}
