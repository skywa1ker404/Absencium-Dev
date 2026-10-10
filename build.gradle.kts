// AGP 9 compiles Kotlin with its built-in Kotlin support, so the
// org.jetbrains.kotlin.android plugin is intentionally not applied.
// The Kotlin Gradle Plugin version AGP uses at runtime is raised here so it
// matches the Compose compiler plugin. Keep this in sync with `kotlin` in
// gradle/libs.versions.toml (the version catalog is not available in buildscript {}).
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
