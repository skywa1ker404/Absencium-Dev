package me.rerere.rikkahub.ui.pages.backup

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rerere.ai.provider.ProviderSetting
import me.rerere.rikkahub.data.ai.models.ModelMetadataResolver
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.datastore.WebDavConfig
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.sync.BackupContentOption
import me.rerere.rikkahub.data.sync.WebDavBackupItem
import me.rerere.rikkahub.data.sync.WebdavSync
import me.rerere.rikkahub.utils.UiState
import java.io.File

private const val TAG = "BackupVM"

class BackupVM(
    private val settingsStore: SettingsStore,
    private val webdavSync: WebdavSync,
    private val modelMetadataResolver: ModelMetadataResolver,
    private val conversationRepository: me.rerere.rikkahub.data.repository.ConversationRepository,
) : ViewModel() {
    val settings = settingsStore.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = Settings.dummy()
    )

    fun updateSettings(settings: Settings) {
        viewModelScope.launch {
            settingsStore.update(settings)
        }
    }

    suspend fun exportToFile(
        selectedContent: Set<BackupContentOption>? = null,
        selectedKeyIds: Set<kotlin.uuid.Uuid>? = null,
    ): File {
        return webdavSync.prepareBackupFile(
            settings.value.webDavConfig.copy(items = WebDavConfig.BackupItem.entries),
            selectedContent = selectedContent,
            selectedKeyIds = selectedKeyIds,
        )
    }

    suspend fun restoreFromLocalFile(file: File): WebdavSync.RestoreResult {
        return webdavSync.restoreFromLocalFile(file, settings.value.webDavConfig)
    }

    suspend fun getAssistantsSnapshot(): List<Assistant> {
        return settingsStore.settingsFlow.first().assistants
    }
    
    fun restartApp(context: android.content.Context) {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = intent?.component
        val mainIntent = android.content.Intent.makeRestartActivityTask(componentName)
        context.startActivity(mainIntent)
        kotlin.system.exitProcess(0)
    }


    private fun providerImportKey(provider: ProviderSetting): String {
        return when (provider) {
            is ProviderSetting.OpenAI -> "openai|${provider.baseUrl}|${provider.apiKey}"
            is ProviderSetting.Google -> "google|${provider.baseUrl}|${provider.apiKey}"
            is ProviderSetting.Claude -> "claude|${provider.baseUrl}|${provider.apiKey}"
            is ProviderSetting.ComfyUI -> "comfyui|${provider.baseUrl}|${provider.workflowJson.hashCode()}"
            is ProviderSetting.LiteRtLocal -> "litert_local"
        }
    }
}
