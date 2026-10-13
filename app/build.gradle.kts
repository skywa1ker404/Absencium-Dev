plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.trajectory.vault"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.trajectory.vault"
        // Android 8.0. Monet (dynamic color) and Liquid Glass (RenderEffect blur)
        // need Android 12 (API 31) and are disabled below that at runtime.
        minSdk = 26
        targetSdk = 36
        // Every CI build gets a higher number than the one before, so a new APK can always be
        // installed over an old one. Local builds use 1.
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt() + 1000
        versionName = "0.1.0"
    }

    // A fixed signing key. Without it every CI run signs with a fresh random debug key, and Android
    // refuses to install a build over one signed with a different key ("package conflicts").
    // This is a debug-grade key kept in the private repo; do not reuse it for a store release.
    signingConfigs {
        getByName("debug") {
            storeFile = file("signing/trajectory-debug.jks")
            storePassword = "trajectory-debug"
            keyAlias = "trajectory"
            keyPassword = "trajectory-debug"
        }
    }

    buildTypes {
        // The optimised build: shrunk and optimised with R8, not debuggable, signed with the same key as the
        // debug build so it installs over it. Debug builds are much slower than this, mostly in the first seconds.
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)

    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.documentfile)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.kyant.backdrop)
    implementation(libs.material.kolor)
    implementation(libs.compose.destinations.core)
    ksp(libs.compose.destinations.ksp)
    implementation(libs.filekit.dialogs.compose)
}
