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
    }
}

rootProject.name = "NerdWatch"

// The interactive Kotlin app: stopwatch, flashlight, night vision, sensors.
include(":app")

// The always-on dial. Watch Face Format requires a wholly separate, code-free
// bundle — it cannot be merged into :app.
include(":watchface")
