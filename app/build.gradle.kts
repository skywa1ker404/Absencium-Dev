plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "skywa1ker404.absencium"
    compileSdk = 37

    defaultConfig {
        applicationId = "skywa1ker404.absencium"
        // Android 8.0. Monet (dynamic color) and Liquid Glass (RenderEffect blur)
        // need Android 12 (API 31) and are disabled below that at runtime.
        minSdk = 26
        targetSdk = 36
        // Every CI build gets a higher number than the one before, so a new APK can always be
        // installed over an old one. Local builds use 1.
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt() + 1000
        versionName = "0.1.0"
    }

    // The signing key is part of Absencium's identity: every APK must be signed with the same key or Android refuses
    // to install it over the previous one. The key comes from the environment when CI has it (GitHub secrets, see
    // docs/SIGNING.md); otherwise the upgrade key kept in this private repository is used. Never replace it casually.
    signingConfigs {
        create("absencium") {
            val envStore = System.getenv("ABSENCIUM_KEYSTORE_FILE")
            if (envStore != null && file(envStore).exists()) {
                storeFile = file(envStore)
                storePassword = System.getenv("ABSENCIUM_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ABSENCIUM_KEY_ALIAS")
                keyPassword = System.getenv("ABSENCIUM_KEY_PASSWORD")
            } else {
                storeFile = file("signing/absencium-upgrade.jks")
                storePassword = "kbJJ8oariMOhUxvZyUvlrECKll61"
                keyAlias = "absencium"
                keyPassword = "kbJJ8oariMOhUxvZyUvlrECKll61"
            }
        }
    }

    buildTypes {
        // Debug builds are signed with the same key so every APK installs over the one before.
        debug {
            signingConfig = signingConfigs.getByName("absencium")
        }
        // The optimised build: shrunk and optimised with R8, not debuggable, signed with the same key as the
        // debug build so it installs over it. Debug builds are much slower than this, mostly in the first seconds.
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("absencium")
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

ksp {
    // Room writes the schema of each database version here; CI keeps these files in the repository.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.profileinstaller)
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
