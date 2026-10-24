plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Which build this repository produces. gradle.properties says `dev` in Absencium-Dev and `stable` in Absencium; a build can
// override it with -PabsenciumChannel=... The two channels are different apps on a phone (different application IDs), so a
// development build never replaces or shares data with the stable app.
val channel = (findProperty("absenciumChannel") as String?) ?: "stable"
val isDev = channel == "dev"

android {
    namespace = "skywa1ker404.absencium"
    compileSdk = 37

    defaultConfig {
        applicationId = if (isDev) "skywa1ker404.absencium.dev" else "skywa1ker404.absencium"
        manifestPlaceholders["appLabel"] = if (isDev) "Absencium Dev" else "Absencium"
        // Android 8.0. Monet (dynamic color) and Liquid Glass (RenderEffect blur)
        // need Android 12 (API 31) and are disabled below that at runtime.
        minSdk = 26
        targetSdk = 36
        // Dev builds: every CI run is higher than the one before (run number + 1000), so a new dev APK always installs over
        // the old one. Stable builds: the release workflow passes -PabsenciumVersionCode (derived from the SemVer tag).
        versionCode = (findProperty("absenciumVersionCode") as String?)?.toInt()
            ?: ((System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt() + 1000)
        versionName = (findProperty("absenciumVersionName") as String?) ?: "0.1.0"
    }

    // Two separate keys, on purpose.
    //  * "dev": app/signing/absencium-dev.jks (Dev repository only). A public throwaway key (password in this file) that only signs development
    //    builds. It protects nothing and must never sign anything that is called stable.
    //  * "stable": read only from the environment (GitHub Actions secrets ABSENCIUM_KEYSTORE_FILE, _PASSWORD, _KEY_ALIAS,
    //    _KEY_PASSWORD, see docs/SIGNING.md). There is no key and no password for it anywhere in the repository. Without
    //    the secrets a stable release build is left unsigned and CI refuses to publish it.
    signingConfigs {
        // The dev key file exists only in the Dev repository; the stable repository does not contain it.
        if (file("signing/absencium-dev.jks").exists()) {
            create("dev") {
                storeFile = file("signing/absencium-dev.jks")
                storePassword = "absencium-dev"
                keyAlias = "absenciumdev"
                keyPassword = "absencium-dev"
            }
        }
        val envStore = System.getenv("ABSENCIUM_KEYSTORE_FILE")
        if (envStore != null && file(envStore).exists()) {
            create("stable") {
                storeFile = file(envStore)
                storePassword = System.getenv("ABSENCIUM_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ABSENCIUM_KEY_ALIAS")
                keyPassword = System.getenv("ABSENCIUM_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        // Debug builds always use the dev key, in both channels.
        debug {
            signingConfig = signingConfigs.findByName("dev")
        }
        // The optimised build: shrunk and optimised with R8, not debuggable. Dev channel: dev key. Stable channel: the
        // stable key from the secrets, or unsigned when they are missing.
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (isDev) signingConfigs.findByName("dev") else signingConfigs.findByName("stable")
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
    implementation(libs.tesseract4android)
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

    testImplementation(libs.junit)
}
