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

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        return Base64.encodeToString(cipher.iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }

    private fun decrypt(stored: String): String? = try {
        val bytes = Base64.decode(stored, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, bytes.copyOfRange(0, IV_BYTES)))
        String(cipher.doFinal(bytes.copyOfRange(IV_BYTES, bytes.size)), Charsets.UTF_8)
    } catch (e: Exception) {
        DebugLogger.e(TAG, "Could not decrypt a stored API key", e)
        null
    }

    private fun extraCipher(provider: AiProvider, slot: Int) = "xkey_ct_${provider.name}_$slot"
    private fun extraLast(provider: AiProvider, slot: Int) = "xkey_l4_${provider.name}_$slot"

    /** Slots of the extra keys in use, in order. The first key of a provider is stored as before; up to [MAX_EXTRA] more sit in slots 1..[MAX_EXTRA]. */
    private fun extraSlots(provider: AiProvider): List<Int> = (1..MAX_EXTRA).filter { prefs.contains(extraCipher(provider, it)) }

    /** How many keys are saved for [provider] (the main one plus extras that are tried when it hits its limit). */
    fun keyCount(provider: AiProvider): Int = (if (hasKey(provider)) 1 else 0) + extraSlots(provider).size

    /** The last four characters of every key saved for [provider], the main one first. */
    fun lastFours(provider: AiProvider): List<String> =
        listOfNotNull(lastFour(provider)) + extraSlots(provider).mapNotNull { prefs.getString(extraLast(provider, it), null) }

    /** Every key of [provider], decrypted, the main one first. */
    fun keys(provider: AiProvider): List<String> =
        listOfNotNull(read(provider)) + extraSlots(provider).mapNotNull { prefs.getString(extraCipher(provider, it), null)?.let(::decrypt) }

    /** Adds another key for [provider], tried when the earlier ones hit their limit. Returns false when it is empty, a duplicate, or there is no room. */
    fun addKey(provider: AiProvider, apiKey: String): Boolean {
        val trimmed = apiKey.trim()
        if (trimmed.isEmpty() || keys(provider).any { it == trimmed }) return false
        if (!hasKey(provider)) return save(provider, trimmed)
        val slot = (1..MAX_EXTRA).firstOrNull { !prefs.contains(extraCipher(provider, it)) } ?: return false
        return try {
            prefs.edit {
                putString(extraCipher(provider, slot), encrypt(trimmed))
                putString(extraLast(provider, slot), trimmed.takeLast(4))
            }
            true
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Could not encrypt and save the extra API key", e)
            false
        }
    }

    /** Removes the key at [index] of [lastFours]. Removing the main key promotes the next one. */
    fun removeKey(provider: AiProvider, index: Int) {
        val slots = extraSlots(provider)
        if (index == 0) {
            val next = slots.firstOrNull()
            if (next == null) {
                clear(provider)
            } else {
                prefs.edit {
                    putString(cipherKey(provider), prefs.getString(extraCipher(provider, next), null))
                    putString(lastKey(provider), prefs.getString(extraLast(provider, next), null))
                    remove(extraCipher(provider, next))
                    remove(extraLast(provider, next))
                }
            }
        } else {
            slots.getOrNull(index - 1)?.let { slot -> prefs.edit { remove(extraCipher(provider, slot)); remove(extraLast(provider, slot)) } }
        }
    }

    private val limitedUntil = HashMap<String, Long>()

    /** Remembers that [apiKey] hit its limit, so it is skipped for [millis] while other keys are tried. */
    @Synchronized
    fun markLimited(apiKey: String, millis: Long = LIMIT_COOLDOWN_MS) {
        limitedUntil[apiKey] = System.currentTimeMillis() + millis
    }

    /** The keys of [provider] that are not resting after hitting a limit, the main one first. */
    @Synchronized
    fun usableKeys(provider: AiProvider): List<String> {
        val now = System.currentTimeMillis()
        return keys(provider).filter { (limitedUntil[it] ?: 0L) <= now }
    }

    /** Encrypts and stores [apiKey] for [provider]. Returns false, after logging the reason, on failure. */
    fun save(provider: AiProvider, apiKey: String): Boolean {
        val trimmed = apiKey.trim()
        if (trimmed.isEmpty()) return false
        return try {
            val encrypted = encrypt(trimmed)
            prefs.edit {
                putString(cipherKey(provider), encrypted)
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
        return decrypt(stored)
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
            for (slot in 1..MAX_EXTRA) {
                remove(extraCipher(provider, slot))
                remove(extraLast(provider, slot))
            }
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
        const val MAX_EXTRA = 8
        const val LIMIT_COOLDOWN_MS = 60_000L
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
