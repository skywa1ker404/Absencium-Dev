plugins {
    alias(libs.plugins.android.library)
}

// Ported from LastChat / RikkaHub (AGPL-3.0); bundles MuPDF (AGPL-3.0). See THIRD_PARTY_NOTICES.md.
android {
    namespace = "me.rerere.document"
    compileSdk = 37
    defaultConfig {
        minSdk = 28
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
}
