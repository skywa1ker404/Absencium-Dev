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
        // Only the LaTeX renderer used by the embedded Absencium AI chat comes from JitPack.
        maven("https://jitpack.io") {
            content { includeGroup("com.github.rikkahub.jlatexmath-android") }
        }
    }
}

rootProject.name = "Absencium"
include(":app")
include(":aihub:shared", ":aihub:common", ":aihub:ai", ":aihub:search", ":aihub:highlight", ":aihub:document", ":aihub:ui-core", ":aihub:tts", ":aihub:speech", ":aihub:app")
