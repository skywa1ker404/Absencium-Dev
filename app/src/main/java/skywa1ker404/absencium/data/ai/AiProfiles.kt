package skywa1ker404.absencium.data.ai

import android.content.Context
import androidx.core.content.edit

/** What is saved for one provider besides its key: where to reach it, which model, and which request style a custom endpoint speaks. */
data class AiProfile(val provider: AiProvider, val baseUrl: String, val model: String, val style: ApiStyle)

/**
 * The connection details of every provider the student has set up, kept on this phone only (no secrets here: keys live in the
 * [skywa1ker404.absencium.data.security.ApiKeyVault]). The settings still hold the one provider in use; this remembers the others
 * so that switching back brings its URL and model with it.
 */
class AiProfiles(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("absencium_ai_profiles", Context.MODE_PRIVATE)

    fun save(profile: AiProfile) = prefs.edit {
        putString("${profile.provider.name}_url", profile.baseUrl)
        putString("${profile.provider.name}_model", profile.model)
        putString("${profile.provider.name}_style", profile.style.name)
    }

    fun get(provider: AiProvider): AiProfile? {
        val url = prefs.getString("${provider.name}_url", null) ?: return null
        val model = prefs.getString("${provider.name}_model", "").orEmpty()
        val style = runCatching { ApiStyle.valueOf(prefs.getString("${provider.name}_style", null) ?: "") }.getOrDefault(provider.style)
        return AiProfile(provider, url, model, style)
    }

    /** Providers with saved details, in the order of the provider list. */
    fun all(): List<AiProfile> = AiProvider.entries.mapNotNull { get(it) }

    fun remove(provider: AiProvider) = prefs.edit {
        remove("${provider.name}_url")
        remove("${provider.name}_model")
        remove("${provider.name}_style")
    }
}
