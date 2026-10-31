package skywa1ker404.absencium.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import skywa1ker404.absencium.data.ai.AiProvider
import skywa1ker404.absencium.logging.DebugLogger
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores the user's AI API keys, one per provider, each encrypted with an AES-256-GCM key that lives in the Android
 * Keystore. Only the encrypted bytes are written to app storage; the key never leaves the Keystore. A plain key is never written
 * to debug.log. Having a key saved for several providers is what lets the student switch between them without typing a key again.
 */
class ApiKeyVault(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Before keys were kept per provider there was one key. It belongs to the provider that was set up then, which only the
     * settings know; this tells the vault which one (null while the settings are still loading).
     */
    var legacyOwner: () -> AiProvider? = { null }

    private fun cipherKey(provider: AiProvider) = "key_ct_${provider.name}"
    private fun lastKey(provider: AiProvider) = "key_l4_${provider.name}"

    /** Moves the single key of an older version to the provider it was saved for. */
    private fun adopt(provider: AiProvider) {
        val legacy = prefs.getString(KEY_CIPHERTEXT, null) ?: return
        if (legacyOwner() != provider) return
        prefs.edit {
            if (!prefs.contains(cipherKey(provider))) {
                putString(cipherKey(provider), legacy)
                prefs.getString(KEY_LAST_FOUR, null)?.let { putString(lastKey(provider), it) }
            }
            remove(KEY_CIPHERTEXT)
            remove(KEY_LAST_FOUR)
        }
    }

    fun hasKey(provider: AiProvider): Boolean {
        adopt(provider)
        return prefs.contains(cipherKey(provider))
    }

    /** The providers a key is saved for. */
    fun providersWithKey(): List<AiProvider> = AiProvider.entries.filter { hasKey(it) }

    /** Encrypts and stores [apiKey] for [provider]. Returns false, after logging the reason, on failure. */
    fun save(provider: AiProvider, apiKey: String): Boolean {
        val trimmed = apiKey.trim()
        if (trimmed.isEmpty()) return false
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val encrypted = cipher.iv + cipher.doFinal(trimmed.toByteArray(Charsets.UTF_8))
            prefs.edit {
                putString(cipherKey(provider), Base64.encodeToString(encrypted, Base64.NO_WRAP))
                putString(lastKey(provider), trimmed.takeLast(4))
            }
            DebugLogger.i(TAG, "API key for ${provider.name} saved (encrypted)")
            true
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Could not encrypt and save the API key", e)
            false
        }
    }

    /** The decrypted key of [provider], or null if none is stored or it could not be decrypted. */
    fun read(provider: AiProvider): String? {
        adopt(provider)
        val stored = prefs.getString(cipherKey(provider), null) ?: return null
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

    /** The last four characters of the key saved for [provider], for showing "saved" without revealing it. */
    fun lastFour(provider: AiProvider): String? {
        adopt(provider)
        return prefs.getString(lastKey(provider), null)
    }

    fun clear(provider: AiProvider) {
        adopt(provider)
        prefs.edit {
            remove(cipherKey(provider))
            remove(lastKey(provider))
        }
        DebugLogger.i(TAG, "API key for ${provider.name} removed")
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
        const val PREFS_NAME = "absencium_key_vault"
        const val KEY_CIPHERTEXT = "api_key_ciphertext"
        const val KEY_LAST_FOUR = "api_key_last_four"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "absencium_api_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
