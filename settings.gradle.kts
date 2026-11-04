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
        // Only the LaTeX renderer used by the embedded LastChat chat comes from JitPack.
        maven("https://jitpack.io") {
            content { includeGroup("com.github.rikkahub.jlatexmath-android") }
        }
    }
}

rootProject.name = "Absencium"
include(":app")
include(":lastchat:shared", ":lastchat:common", ":lastchat:ai", ":lastchat:search", ":lastchat:highlight", ":lastchat:document", ":lastchat:ui-core", ":lastchat:tts", ":lastchat:speech", ":lastchat:app")
