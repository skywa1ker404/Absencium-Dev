package me.rerere.rikkahub.data.datastore

import me.rerere.ai.provider.Model
import me.rerere.ai.provider.ProviderSetting

/** Keeps the speech-to-text models installed on this device listed under the on-device provider. */
fun Settings.withSyncedLocalProviderModels(
    llmAndEmbeddingModels: List<Model>,
    sttModels: List<Model>,
): Settings {
    val mergedModels = sttModels + llmAndEmbeddingModels
    val current = providers.filterIsInstance<ProviderSetting.LiteRtLocal>().firstOrNull()
    if (current == null) {
        if (mergedModels.isEmpty()) return this
        return copy(providers = providers + ProviderSetting.LiteRtLocal(models = mergedModels))
    }
    if (current.models == mergedModels) return this
    return copy(
        providers = providers.map { provider ->
            if (provider is ProviderSetting.LiteRtLocal) provider.copy(models = mergedModels) else provider
        }
    )
}
