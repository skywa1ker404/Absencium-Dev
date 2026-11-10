plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

// Ported from LastChat / RikkaHub (AGPL-3.0). See THIRD_PARTY_NOTICES.md.
android {
    namespace = "me.rerere.rikkahub.ui.core"
    compileSdk = 37
    defaultConfig {
        minSdk = 28
    }
    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":aihub:shared"))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons.extended)
    implementation(libs.kotlinx.datetime)
    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
}
