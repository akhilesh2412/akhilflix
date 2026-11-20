// Top-level build file where you can add configuration options common to all
// sub-projects/modules.
plugins {
    id("com.android.application") version "8.2.2" apply false
    // Kotlin 2.4.0: required to consume the CloudStream API library
    // (com.github.recloudstream.cloudstream:library), which is compiled
    // with Kotlin 2.4 metadata — a 1.9 compiler cannot read it.
    id("org.jetbrains.kotlin.android") version "2.4.0" apply false
}
