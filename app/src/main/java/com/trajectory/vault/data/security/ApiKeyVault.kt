package com.trajectory.vault.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import com.trajectory.vault.logging.DebugLogger
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores the user's AI API key encrypted with an AES-256-GCM key that lives in the Android
 * Keystore. Only the encrypted bytes are written to app storage; the key never leaves the
 * Keystore. The plain key is never written to debug.log.
 */
class ApiKeyVault(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasKey(): Boolean = prefs.contains(KEY_CIPHERTEXT)

    /** Encrypts and stores [apiKey]. Returns false, after logging the reason, on failure. */
    fun save(apiKey: String): Boolean {
        val trimmed = apiKey.trim()
        if (trimmed.isEmpty()) return false
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val encrypted = cipher.iv + cipher.doFinal(trimmed.toByteArray(Charsets.UTF_8))
            prefs.edit {
                putString(KEY_CIPHERTEXT, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                putString(KEY_LAST_FOUR, trimmed.takeLast(4))
            }
            DebugLogger.i(TAG, "API key saved (encrypted)")
            true
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Could not encrypt and save the API key", e)
            false
        }
    }

    /** The decrypted key, or null if none is stored or it could not be decrypted. */
    fun read(): String? {
        val stored = prefs.getString(KEY_CIPHERTEXT, null) ?: return null
        return try {
            val bytes = Base64.decode(stored, Base64.NO_WRAP)
            val iv = bytes.copyOfRange(0, IV_BYTES)
            val body = bytes.copyOfRange(IV_BYTES, bytes.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, iv))
            String(cipher.doFinal(body), Charsets.UTF_8)
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Could not decrypt the stored API key", e)
            null
        }
    }

    /** The last four characters of the stored key, for showing "saved" without revealing it. */
    fun lastFour(): String? = prefs.getString(KEY_LAST_FOUR, null)

    fun clear() {
        prefs.edit {
            remove(KEY_CIPHERTEXT)
            remove(KEY_LAST_FOUR)
        }
        DebugLogger.i(TAG, "API key removed")
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val TAG = "KeyVault"
        const val PREFS_NAME = "trajectory_key_vault"
        const val KEY_CIPHERTEXT = "api_key_ciphertext"
        const val KEY_LAST_FOUR = "api_key_last_four"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "trajectory_api_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
