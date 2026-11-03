plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

// Ported from LastChat / RikkaHub (AGPL-3.0). See THIRD_PARTY_NOTICES.md.
android {
    namespace = "me.rerere.lastchat.common"
    compileSdk = 37
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        optIn.addAll("kotlin.uuid.ExperimentalUuidApi", "kotlin.time.ExperimentalTime")
    }
}

dependencies {
    api(project(":lastchat:shared"))
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.datetime)
    api(libs.kotlinx.serialization.json)
    api(libs.okhttp)
    api(libs.okhttp.sse)
    api(libs.okhttp.logging)
    implementation(libs.androidx.core.ktx)
}
