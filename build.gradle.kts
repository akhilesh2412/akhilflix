// Top-level build file where you can add configuration options common to all
// sub-projects/modules.
plugins {
    // AGP 8.5.2: minimum required by Kotlin Gradle Plugin 2.4.0
    // (run #87 failed: AGP 8.2.2 < 8.5.2).
    id("com.android.application") version "8.5.2" apply false
    // Kotlin 2.4.0: required to consume the CloudStream API library
    // (com.github.recloudstream.cloudstream:library), which is compiled
    // with Kotlin 2.4 metadata — a 1.9 compiler cannot read it.
    id("org.jetbrains.kotlin.android") version "2.4.0" apply false
    // Kotlin 2.0+: the Compose Compiler Gradle plugin is required when
    // compose is enabled (run #91 failed without it).
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0" apply false
}
