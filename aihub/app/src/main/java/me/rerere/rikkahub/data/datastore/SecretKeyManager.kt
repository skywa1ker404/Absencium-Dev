package me.rerere.rikkahub.data.datastore

import me.rerere.asr.ASRProviderSetting
import me.rerere.ai.provider.ApiKeyEntry
import me.rerere.ai.provider.ProviderSetting
import me.rerere.ai.provider.withApiKeyPool
import me.rerere.ai.util.PooledKey
import kotlin.uuid.Uuid

/**
 * Manages secrets for providers and other sensitive data.
 * Handles migration from plaintext DataStore to encrypted SecureStore.
 * 
 * Key naming conventions:
 * - Provider API key: "provider_apikey_{providerId}"
 * - Provider API key pool: "provider_apikey_pool_{providerId}_{entryId}"
 * - Provider private key (Vertex AI): "provider_privatekey_{providerId}"
 * - WebDAV password: "webdav_password"
 */
class SecretKeyManager(
    private val secureStore: SecureStore
) {
    companion object {
        private const val PROVIDER_APIKEY_PREFIX = "provider_apikey_"
        private const val PROVIDER_APIKEY_POOL_PREFIX = "provider_apikey_pool_"
        private const val PROVIDER_PRIVATEKEY_PREFIX = "provider_privatekey_"
        /** Keys of the removed text-to-speech providers; wiped on start so no API key is left behind. */
        private const val REMOVED_TTS_APIKEY_PREFIX = "tts_provider_apikey_"
        private const val STT_PROVIDER_APIKEY_PREFIX = "stt_provider_apikey_"
        private const val WEBDAV_PASSWORD_KEY = "webdav_password"
        private const val HUGGINGFACE_TOKEN_KEY = "huggingface_token"
        private const val MCP_OAUTH_PREFIX = "mcp_oauth_"
    }

    // ========== Provider API Key Management ==========

    /**
     * Get the API key for a provider. First checks SecureStore, then falls back
     * to the plaintext value (for migration).
     */
    fun getApiKey(providerId: Uuid, plaintextFallback: String): String {
        val key = "$PROVIDER_APIKEY_PREFIX$providerId"
        return secureStore.getSecret(key) ?: plaintextFallback
    }

    /**
     * Store an API key securely for a provider.
     */
    fun setApiKey(providerId: Uuid, apiKey: String) {
        val key = "$PROVIDER_APIKEY_PREFIX$providerId"
        if (apiKey.isNotBlank()) {
            secureStore.putSecret(key, apiKey)
        } else {
            secureStore.removeSecret(key)
        }
    }

    /**
     * Get the private key for a Google Vertex AI provider.
     */
    fun getPrivateKey(providerId: Uuid, plaintextFallback: String): String {
        val key = "$PROVIDER_PRIVATEKEY_PREFIX$providerId"
        return secureStore.getSecret(key) ?: plaintextFallback
    }

    /**
     * Store a private key securely for a Google Vertex AI provider.
     */
    fun setPrivateKey(providerId: Uuid, privateKey: String) {
        val key = "$PROVIDER_PRIVATEKEY_PREFIX$providerId"
        if (privateKey.isNotBlank()) {
            secureStore.putSecret(key, privateKey)
        } else {
            secureStore.removeSecret(key)
        }
    }

    /**
     * Get an API key from the pool for a provider.
     */
    fun getPoolApiKey(providerId: Uuid, entryId: Uuid, plaintextFallback: String = ""): String {
        val key = "$PROVIDER_APIKEY_POOL_PREFIX${providerId}_$entryId"
        return secureStore.getSecret(key) ?: plaintextFallback
    }

    /**
     * Store an API key in the pool securely for a provider.
     */
    fun setPoolApiKey(providerId: Uuid, entryId: Uuid, apiKey: String) {
        val key = "$PROVIDER_APIKEY_POOL_PREFIX${providerId}_$entryId"
        if (apiKey.isNotBlank()) {
            secureStore.putSecret(key, apiKey)
        } else {
            secureStore.removeSecret(key)
        }
    }

    /**
     * Remove a single pool API key for a provider.
     */
    fun removePoolApiKey(providerId: Uuid, entryId: Uuid) {
        secureStore.removeSecret("$PROVIDER_APIKEY_POOL_PREFIX${providerId}_$entryId")
    }

    /**
     * Remove all pool API keys for a provider.
     */
    fun removeAllPoolApiKeys(providerId: Uuid) {
        val prefix = "$PROVIDER_APIKEY_POOL_PREFIX${providerId}_"
        secureStore.getAllKeys()
            .filter { it.startsWith(prefix) }
            .forEach(secureStore::removeSecret)
    }

    /**
     * Remove all secrets for a provider (when provider is deleted).
     */
    fun removeProviderSecrets(providerId: Uuid) {
        secureStore.removeSecret("$PROVIDER_APIKEY_PREFIX$providerId")
        secureStore.removeSecret("$PROVIDER_PRIVATEKEY_PREFIX$providerId")
        removeAllPoolApiKeys(providerId)
    }

    // ========== STT API Key Management ==========

    fun getSttApiKey(providerId: Uuid, plaintextFallback: String): String {
        val key = "$STT_PROVIDER_APIKEY_PREFIX$providerId"
        return secureStore.getSecret(key) ?: plaintextFallback
    }

    fun setSttApiKey(providerId: Uuid, apiKey: String) {
        val key = "$STT_PROVIDER_APIKEY_PREFIX$providerId"
        if (apiKey.isNotBlank()) {
            secureStore.putSecret(key, apiKey)
        } else {
            secureStore.removeSecret(key)
        }
    }

    // ========== WebDAV Password Management ==========

    fun getWebDavPassword(plaintextFallback: String): String {
        return secureStore.getSecret(WEBDAV_PASSWORD_KEY) ?: plaintextFallback
    }

    fun setWebDavPassword(password: String) {
        if (password.isNotBlank()) {
            secureStore.putSecret(WEBDAV_PASSWORD_KEY, password)
        } else {
            secureStore.removeSecret(WEBDAV_PASSWORD_KEY)
        }
    }

    // ========== HuggingFace Token Management ==========



    /** MCP servers and text-to-speech were removed; drop the secrets they left in the secure store. */
    private fun removeLeftoverMcpSecrets() {
        secureStore.getAllKeys()
            .filter { it.startsWith(MCP_OAUTH_PREFIX) || it.startsWith(REMOVED_TTS_APIKEY_PREFIX) }
            .forEach(secureStore::removeSecret)
    }

    // ========== Migration Logic ==========

    /**
     * Migrate secrets from plaintext Settings to SecureStore.
     * Returns updated Settings with credentials cleared (moved to SecureStore).
     * 
     * This should be called once when settings are loaded to ensure migration.
     */
    fun migrateSecretsFromSettings(settings: Settings): Settings {
        var migrated = false

        removeLeftoverMcpSecrets()

        // Migrate provider API keys
        val migratedProviders = settings.providers.map { provider ->
            migrateProviderSecrets(provider).also { 
                if (it != provider) migrated = true
            }
        }

        // Migrate WebDAV password
        val migratedWebDav = if (settings.webDavConfig.password.isNotBlank()) {
            setWebDavPassword(settings.webDavConfig.password)
            migrated = true
            settings.webDavConfig.copy(password = "") // Clear plaintext
        } else {
            settings.webDavConfig
        }

        return if (migrated) {
            settings.copy(
                providers = migratedProviders,
                webDavConfig = migratedWebDav,
            )
        } else {
            settings
        }
    }

    /**
     * Handle explicit secret deletions.
     * When a user clears a secret field (changes from non-empty to empty),
     * we need to remove it from SecureStore so it doesn't get re-populated.
     * 
     * This should be called BEFORE migrateSecretsFromSettings() in the update flow.
     */
    fun handleExplicitSecretDeletions(oldSettings: Settings, newSettings: Settings) {
        // Provider presets have stable IDs. If a deleted provider's secrets remain in
        // SecureStore, adding the same preset again silently restores those credentials.
        // Treat removal from Settings as deletion of every secret owned by that provider.
        val newProviderIds = newSettings.providers.asSequence().map { it.id }.toHashSet()
        oldSettings.providers
            .asSequence()
            .map { it.id }
            .filterNot(newProviderIds::contains)
            .forEach(::removeProviderSecrets)

        // Handle provider secrets (API keys and private keys)
        for (newProvider in newSettings.providers) {
            val oldProvider = oldSettings.providers.find { it.id == newProvider.id } ?: continue
            
            // Handle pool entry deletions
            val newEntryIds = newProvider.apiKeyPool.map { it.id }.toSet()
            for (oldEntry in oldProvider.apiKeyPool) {
                if (oldEntry.id !in newEntryIds) {
                    removePoolApiKey(newProvider.id, oldEntry.id)
                }
            }

            when {
                oldProvider is ProviderSetting.OpenAI && newProvider is ProviderSetting.OpenAI -> {
                    // Check if API key was explicitly cleared
                    if (oldProvider.apiKey.isNotBlank() && newProvider.apiKey.isBlank()) {
                        setApiKey(newProvider.id, "")
                    }
                }
                oldProvider is ProviderSetting.Google && newProvider is ProviderSetting.Google -> {
                    if (oldProvider.apiKey.isNotBlank() && newProvider.apiKey.isBlank()) {
                        setApiKey(newProvider.id, "")
                    }
                    if (oldProvider.privateKey.isNotBlank() && newProvider.privateKey.isBlank()) {
                        setPrivateKey(newProvider.id, "")
                    }
                }
                oldProvider is ProviderSetting.Claude && newProvider is ProviderSetting.Claude -> {
                    if (oldProvider.apiKey.isNotBlank() && newProvider.apiKey.isBlank()) {
                        setApiKey(newProvider.id, "")
                    }
                }
            }
        }

        // Handle WebDAV password
        if (oldSettings.webDavConfig.password.isNotBlank() && 
            newSettings.webDavConfig.password.isBlank()) {
            setWebDavPassword("")
        }
    }

    /**
     * Migrate a single provider's secrets to SecureStore.
     * Returns provider with credentials cleared if migration occurred.
     */
    private fun migrateProviderSecrets(provider: ProviderSetting): ProviderSetting {
        var updated = provider

        // Store any plaintext entry keys in SecureStore and clear them
        if (updated.apiKeyPool.isNotEmpty()) {
            var poolModified = false
            val cleanedPool = updated.apiKeyPool.map { entry ->
                if (entry.key.isNotBlank()) {
                    setPoolApiKey(updated.id, entry.id, entry.key)
                    poolModified = true
                    entry.copy(key = "")
                } else {
                    entry
                }
            }
            if (poolModified) {
                updated = updated.withApiKeyPool(cleanedPool)
            }
        }

        return when (updated) {
            is ProviderSetting.OpenAI -> {
                val legacyKey = getApiKey(updated.id, updated.apiKey)
                if (updated.apiKeyPool.isEmpty() && legacyKey.isNotBlank()) {
                    val entry = ApiKeyEntry(
                        name = "Primary",
                        enabled = true,
                        exportable = true,
                    )
                    setPoolApiKey(updated.id, entry.id, legacyKey)
                    setApiKey(updated.id, "")
                    updated.copy(apiKey = "", apiKeyPool = listOf(entry))
                } else if (updated.apiKey.isNotBlank()) {
                    setApiKey(updated.id, updated.apiKey)
                    updated.copy(apiKey = "")
                } else updated
            }

            is ProviderSetting.Google -> {
                var g = updated
                val legacyKey = getApiKey(g.id, g.apiKey)
                if (g.apiKeyPool.isEmpty() && legacyKey.isNotBlank()) {
                    val entry = ApiKeyEntry(
                        name = "Primary",
                        enabled = true,
                        exportable = true,
                    )
                    setPoolApiKey(g.id, entry.id, legacyKey)
                    setApiKey(g.id, "")
                    g = g.copy(apiKey = "", apiKeyPool = listOf(entry))
                } else if (g.apiKey.isNotBlank()) {
                    setApiKey(g.id, g.apiKey)
                    g = g.copy(apiKey = "")
                }
                if (g.privateKey.isNotBlank()) {
                    setPrivateKey(g.id, g.privateKey)
                    g = g.copy(privateKey = "")
                }
                g
            }

            is ProviderSetting.Claude -> {
                val legacyKey = getApiKey(updated.id, updated.apiKey)
                if (updated.apiKeyPool.isEmpty() && legacyKey.isNotBlank()) {
                    val entry = ApiKeyEntry(
                        name = "Primary",
                        enabled = true,
                        exportable = true,
                    )
                    setPoolApiKey(updated.id, entry.id, legacyKey)
                    setApiKey(updated.id, "")
                    updated.copy(apiKey = "", apiKeyPool = listOf(entry))
                } else if (updated.apiKey.isNotBlank()) {
                    setApiKey(updated.id, updated.apiKey)
                    updated.copy(apiKey = "")
                } else updated
            }

            is ProviderSetting.ComfyUI -> updated
            is ProviderSetting.LiteRtLocal -> updated // on-device, no secrets
        }
    }


    // ========== Backup/Export Support ==========

    /**
     * Populate settings with decrypted secrets for export.
     * This creates a copy with all secrets in plaintext for backup portability.
     */
    fun populateSecretsForExport(
        settings: Settings,
        selectedKeyIds: Set<Uuid>? = null,
    ): Settings {
        val providersWithSecrets = settings.providers.map { provider ->
            populateProviderSecrets(provider, selectedKeyIds, forExport = (selectedKeyIds != null))
        }

        val webDavWithPassword = settings.webDavConfig.copy(
            password = getWebDavPassword(settings.webDavConfig.password)
        )

        return settings.copy(
            providers = providersWithSecrets,
            webDavConfig = webDavWithPassword,
        )
    }

    /**
     * Populate a single provider with its secrets.
     * When [forExport] is true, only exportable/selected keys are decrypted into entry.key.
     * When [forExport] is false, all pool keys are decrypted into entry.key.
     * In both cases, [resolvedApiKeyPool] is fully populated with all enabled keys.
     */
    fun populateProviderSecrets(
        provider: ProviderSetting,
        selectedKeyIds: Set<Uuid>? = null,
        forExport: Boolean = false,
    ): ProviderSetting {
        // Resolve pool keys from SecureStore
        val resolvedPool = provider.apiKeyPool
            .filter { it.enabled }
            .mapIndexed { index, entry ->
                val secret = getPoolApiKey(provider.id, entry.id, entry.key)
                PooledKey(
                    id = entry.id,
                    name = entry.name,
                    value = secret,
                    priority = index,
                    providerId = provider.id,
                    providerName = provider.name,
                )
            }

        // Populate entry.key
        val poolWithSecrets = provider.apiKeyPool.map { entry ->
            val secret = getPoolApiKey(provider.id, entry.id, entry.key)
            val shouldExport = if (!forExport) {
                true
            } else if (selectedKeyIds != null) {
                entry.id in selectedKeyIds
            } else {
                entry.exportable
            }
            entry.copy(key = if (shouldExport) secret else "")
        }

        val updated = when (provider) {
            is ProviderSetting.OpenAI -> {
                provider.copy(
                    apiKey = getApiKey(provider.id, provider.apiKey),
                    apiKeyPool = poolWithSecrets,
                    models = provider.models.map { hydrateModelProviderOverwrite(it) },
                )
            }

            is ProviderSetting.Google -> {
                provider.copy(
                    apiKey = getApiKey(provider.id, provider.apiKey),
                    privateKey = getPrivateKey(provider.id, provider.privateKey),
                    apiKeyPool = poolWithSecrets,
                    models = provider.models.map { hydrateModelProviderOverwrite(it) },
                )
            }

            is ProviderSetting.Claude -> {
                provider.copy(
                    apiKey = getApiKey(provider.id, provider.apiKey),
                    apiKeyPool = poolWithSecrets,
                    models = provider.models.map { hydrateModelProviderOverwrite(it) },
                )
            }

            is ProviderSetting.ComfyUI -> provider.copy(
                models = provider.models.map { hydrateModelProviderOverwrite(it) },
            )
            is ProviderSetting.LiteRtLocal -> provider.copy(
                models = provider.models.map { hydrateModelProviderOverwrite(it) },
            )
        }

        updated.resolvedApiKeyPool = resolvedPool
        return updated
    }

    /**
     * Hydrate nested [me.rerere.ai.provider.Model.providerOverwrite] secrets.
     * Overwrites are stored on models and were previously skipped by populate,
     * so chat with an overwrite sent empty credentials after key-pool migration.
     */
    private fun hydrateModelProviderOverwrite(model: me.rerere.ai.provider.Model): me.rerere.ai.provider.Model {
        val overwrite = model.providerOverwrite ?: return model
        return model.copy(providerOverwrite = populateProviderSecrets(overwrite))
    }


}

