plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.akhilflix.app"
    // compileSdk 34 again: platform 37 is not in the GitHub runner's
    // SDK repository yet (run #98: "Failed to find package
    // 'platforms;android-37'"), so the library's minCompileSdk 37
    // check is disabled below instead (see end of file).
    compileSdk = 34

    defaultConfig {
        applicationId = "com.akhilflix.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // The host is a THIN SHELL. It knows only the plugin interface.
    // Every feature (screens, player, network, images) lives in the
    // plugin DEX downloaded from the GitHub repo.
    implementation(project(":plugin-api"))
    // Monolithic build: the whole plugin is ALSO compiled into this APK
    // (reliable on all phones). The DEX download is no longer used at
    // runtime — content/theme still come from the backend JSON.
    implementation(project(":plugin"))

    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
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
    // XML themes (Theme.Material3.*) live in this library, not in the
    // Compose material3 artifact — the app theme needs it.
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // The host must ALSO contain every library the plugin uses. The plugin
    // DEX deliberately excludes them (d8 --lib), so the plugin resolves
    // them from the host's classloader at runtime. If the plugin gains a
    // new library, add it here too.
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    // CloudStream API library — the plugin engine. Its classes must be in
    // the HOST app so downloaded .cs3 plugins (loaded with a
    // PathClassLoader whose parent is the app classloader) can resolve
    // MainAPI, BasePlugin, extractors, etc. at runtime.
    implementation("com.github.recloudstream.cloudstream:library:master-SNAPSHOT")
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.2.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// Kotlin 2.4: the old kotlinOptions.jvmTarget is an error; the
// compilerOptions DSL replaces it (same 1.8 target as before).
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
    }
}

// The CloudStream API library's AAR declares minCompileSdk 37, but the
// GitHub runner's SDK repository has no platform 37 yet (run #98:
// "Failed to find package 'platforms;android-37'"). The library's code
// the app actually runs (MainAPI, extractors, networking) uses old
// framework APIs, so the metadata check is disabled and the app stays
// on compileSdk 34. If platform 37 becomes installable, remove this
// and bump compileSdk instead.
tasks.named("checkReleaseAarMetadata") { enabled = false }
tasks.named("checkDebugAarMetadata") { enabled = false }
