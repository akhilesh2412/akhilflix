pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // CloudStream API library (JitPack) — the plugin engine.
        maven { url = uri("https://jitpack.io") }
    }
}
rootProject.name = "AkhilFlix"
include(":app")
include(":plugin-api")
include(":plugin")
