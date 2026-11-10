package me.rerere.rikkahub

import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import me.rerere.ai.provider.ProviderSetting
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.datastore.findModelById

/**
 * What the host app (Absencium) reads from the chat's settings, so its own AI features (note writer, syllabus import, text from files,
 * homework) use the same provider, key and model the student picked in Absencium AI. One place to set up, nothing to keep in step.
 */
object AiApi {
    enum class Kind { OPENAI, CLAUDE, GOOGLE }

    /** The chat model picked in the chat's settings, with what is needed to call it. */
    class Choice(
        val kind: Kind,
        val providerName: String,
        val baseUrl: String,
        val apiKey: String,
        val modelId: String,
        val modelName: String,
    )

    /**
     * The model chosen for chat, or null while none is chosen, its provider is switched off, or it is a Vertex AI service account
     * (that needs a signed token, which only the chat itself can make). Starts the chat runtime on first use and waits briefly for its
     * settings to load, so call it off the main thread where possible.
     */
    fun choice(app: Application): Choice? {
        AiApp.start(app)
        val store = org.koin.core.context.GlobalContext.get().get<SettingsStore>()
        val settings = runBlocking(Dispatchers.IO) { withTimeoutOrNull(4_000) { store.settingsFlow.first { !it.init } } } ?: return null
        val model = settings.findModelById(settings.chatModelId) ?: return null
        val provider = settings.providers.firstOrNull { it.enabled && it.models.any { m -> m.id == model.id } } ?: return null
        fun pick(single: String, pool: List<me.rerere.ai.provider.ApiKeyEntry>): String =
            pool.firstOrNull { it.enabled && it.key.isNotBlank() }?.key ?: single.substringBefore(',').trim()
        return when (provider) {
            is ProviderSetting.OpenAI -> Choice(Kind.OPENAI, provider.name, provider.baseUrl, pick(provider.apiKey, provider.apiKeyPool), model.modelId, model.displayName)
            is ProviderSetting.Claude -> Choice(Kind.CLAUDE, provider.name, provider.baseUrl, pick(provider.apiKey, provider.apiKeyPool), model.modelId, model.displayName)
            is ProviderSetting.Google ->
                if (provider.vertexAI) null
                else Choice(Kind.GOOGLE, provider.name, provider.baseUrl, pick(provider.apiKey, provider.apiKeyPool), model.modelId, model.displayName)
            else -> null
        }
    }
}
