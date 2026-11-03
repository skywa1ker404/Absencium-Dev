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

rootProject.name = "Absencium"
include(":app")
include(":lastchat:shared", ":lastchat:common", ":lastchat:ai", ":lastchat:search", ":lastchat:highlight", ":lastchat:document")
