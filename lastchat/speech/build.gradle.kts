import java.net.URI
import java.security.MessageDigest

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

// Ported from LastChat / RikkaHub (AGPL-3.0). See THIRD_PARTY_NOTICES.md.
val sherpaOnnxVersion = "1.13.4"
val sherpaOnnxSha256 = "03f9c4df965f21c71269365a7951a7f23b5696fddd093fa318c80d65550ab780"
val sherpaOnnxAar = layout.buildDirectory.file("sherpa/sherpa-onnx-$sherpaOnnxVersion.aar")

val downloadSherpaOnnxAar by tasks.registering {
    group = "build setup"
    description = "Downloads the pinned official sherpa-onnx Android AAR"
    outputs.file(sherpaOnnxAar)
    doLast {
        val target = sherpaOnnxAar.get().asFile
        fun sha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().buffered().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }

        if (target.exists() && sha256(target) == sherpaOnnxSha256) return@doLast
        target.parentFile.mkdirs()
        val partial = File(target.parentFile, "${target.name}.part")
        partial.delete()
        URI(
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/" +
                "v$sherpaOnnxVersion/sherpa-onnx-$sherpaOnnxVersion.aar"
        ).toURL().openStream().buffered().use { input ->
            partial.outputStream().buffered().use(input::copyTo)
        }
        check(sha256(partial) == sherpaOnnxSha256) { "Downloaded sherpa-onnx AAR checksum mismatch" }
        check(partial.renameTo(target)) { "Unable to install sherpa-onnx AAR at $target" }
    }
}

android {
    namespace = "me.rerere.speech"
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

kotlin {
    compilerOptions {
        optIn.addAll(
            "kotlin.uuid.ExperimentalUuidApi",
            "kotlin.time.ExperimentalTime",
            "kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
    }
}

tasks.named("preBuild").configure { dependsOn(downloadSherpaOnnxAar) }

dependencies {
    implementation(project(":lastchat:common"))
    implementation(project(":lastchat:ai"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.okhttp)
    compileOnly(files(sherpaOnnxAar))
    implementation(libs.commons.compress)
    implementation(libs.androidx.datastore.preferences)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}
