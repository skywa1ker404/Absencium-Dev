package me.rerere.rikkahub

import android.app.Application
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rerere.common.android.appTempFolder
import me.rerere.rikkahub.di.appModule
import me.rerere.rikkahub.di.dataSourceModule
import me.rerere.rikkahub.di.repositoryModule
import me.rerere.rikkahub.di.viewModelModule
import me.rerere.rikkahub.utils.DatabaseUtil
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.datastore.withRecoveredAssistantsFromConversations
import me.rerere.rikkahub.data.ai.models.ModelMetadataResolver
import me.rerere.rikkahub.data.ai.models.ModelCatalogService
import me.rerere.rikkahub.data.ai.models.mergeCatalogIntoSettings
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import me.rerere.rikkahub.service.CHAT_STORAGE_MAINTENANCE_WORK_NAME
import me.rerere.rikkahub.service.ChatStorageMaintenanceWorker
import me.rerere.rikkahub.service.MemoryConsolidationWorker
import me.rerere.rikkahub.service.SPONTANEOUS_NOTIFICATION_CHANNEL_ID
import me.rerere.rikkahub.service.SPONTANEOUS_WORK_INTERVAL_MINUTES
import me.rerere.rikkahub.service.SPONTANEOUS_WORK_NAME
import me.rerere.rikkahub.service.SpontaneousWorker
import me.rerere.rikkahub.service.initializeAiWorkManager
import me.rerere.rikkahub.data.search.AndroidBingSearchClient
import java.util.concurrent.TimeUnit
import org.koin.core.context.startKoin
import me.rerere.common.platform.PlatformHttpClient
import me.rerere.rikkahub.di.SEARCH_PLATFORM_HTTP_CLIENT
import me.rerere.rikkahub.utils.acceptLanguageHeader
import me.rerere.search.SearchService
import org.koin.core.qualifier.named

import coil3.SingletonImageLoader
import me.rerere.rikkahub.ui.image.AppImageLoaderFactory
import me.rerere.rikkahub.ui.modifier.isHazeDetachedCoordinateCrash

private const val TAG = "AiApp"
private const val MEMORY_MAINTENANCE_WORK_NAME = "memory_consolidation_automatic"

const val CHAT_COMPLETED_NOTIFICATION_CHANNEL_ID = "chat_completed"

/**
 * The embedded Absencium AI runtime (Absencium). It is not an Application: Absencium calls [start] the first time the AI chat is opened,
 * so nothing of it (database, workers, search, image loader) runs for a student who never uses AI.
 */
object AiApp {
    lateinit var instance: Application
        private set

    private var started = false

    private inline fun <reified T : Any> get(qualifier: org.koin.core.qualifier.Qualifier? = null): T =
        org.koin.core.context.GlobalContext.get().get<T>(qualifier)

    @Synchronized
    fun start(application: Application) {
        if (started) return
        started = true
        onCreate(application)
    }

    private fun onCreate(app: Application) {
        instance = app
        startKoin {
            androidLogger()
            androidContext(app)
            modules(appModule, viewModelModule, dataSourceModule, repositoryModule)
        }
        SingletonImageLoader.setSafe { context -> get<AppImageLoaderFactory>().create(context) }
        installHazeDetachedCoordinateCrashGuard()
        val searchHttpClient = get<PlatformHttpClient>(named(SEARCH_PLATFORM_HTTP_CLIENT))
        SearchService.installPlatformHttpClient(searchHttpClient)
        SearchService.installBingSearchClient(AndroidBingSearchClient(searchHttpClient))
        SearchService.installAcceptLanguageProvider { app.acceptLanguageHeader() }
        createNotificationChannel(app)

        // set cursor window size (4MB avoids native virtual memory exhaustion)
        DatabaseUtil.setCursorWindowSize(4 * 1024 * 1024)

        // delete temp files
        deleteTempFiles()

        val workManager = app.initializeAiWorkManager()
        workManager?.enqueueUniquePeriodicWork(
            SPONTANEOUS_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<SpontaneousWorker>(
                SPONTANEOUS_WORK_INTERVAL_MINUTES,
                TimeUnit.MINUTES
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
        )

        workManager?.enqueueUniquePeriodicWork(
            CHAT_STORAGE_MAINTENANCE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<ChatStorageMaintenanceWorker>(
                1,
                TimeUnit.DAYS
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
        )

        // Post-reply jobs do the normal Core + Episodic consolidation. This periodic scan is the
        // durable safety net for process death, provider failures, and restored data.
        workManager?.apply {
            cancelUniqueWork("memory_consolidation")
            cancelUniqueWork("memory_maintenance_v3")
            enqueueUniquePeriodicWork(
                MEMORY_MAINTENANCE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<MemoryConsolidationWorker>(6, TimeUnit.HOURS).build(),
            )
        }
        
        // Update app shortcuts when recently used assistants change
        val appShortcutManager = me.rerere.rikkahub.utils.AppShortcutManager(app)
        get<AppScope>().launch {
            get<SettingsStore>().settingsFlow
                .map { Triple(it.recentlyUsedAssistants, it.assistants, it.init) }
                .distinctUntilChanged()
                .collect { (recentlyUsed, assistants, isInit) ->
                    if (!isInit) {
                        withContext(Dispatchers.IO) {
                            appShortcutManager.updateAssistantShortcuts(recentlyUsed, assistants)
                        }
                    }
                }
        }

        get<AppScope>().launch(Dispatchers.IO) {
            runCatching {
                val settingsStore = get<SettingsStore>()
                val conversationRepo = get<me.rerere.rikkahub.data.repository.ConversationRepository>()
                val settings = settingsStore.settingsFlow.first { !it.init }
                val recovered = settings.withRecoveredAssistantsFromConversations(
                    conversationRepo.getDistinctAssistantIds(),
                )
                if (recovered != settings) {
                    Log.i(TAG, "Restored ${recovered.assistants.size - settings.assistants.size} assistants from existing chats")
                    settingsStore.update(recovered)
                }
            }.onFailure {
                Log.w(TAG, "Conversation assistant recovery failed", it)
            }
            runCatching {
                val catalogService = get<ModelCatalogService>()
                catalogService.warmUp()
                val snapshot = catalogService.snapshotOrNull() ?: return@runCatching
                val settingsStore = get<SettingsStore>()
                val settings = settingsStore.settingsFlow.first { !it.init }
                val merged = mergeCatalogIntoSettings(
                    settings = settings,
                    snapshot = snapshot,
                    resolver = get<ModelMetadataResolver>(),
                )
                if (merged != settings) {
                    settingsStore.update(merged)
                }
            }.onFailure {
                Log.w(TAG, "Model catalog warm-up failed", it)
            }
        }
    }

    private fun installHazeDetachedCoordinateCrashGuard() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (isHazeDetachedCoordinateCrash(throwable)) {
                Log.w(TAG, "Suppressed Haze LayoutCoordinate crash after detach", throwable)
                return@setDefaultUncaughtExceptionHandler
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun deleteTempFiles() {
        get<AppScope>().launch(Dispatchers.IO) {
            val dir = instance.appTempFolder
            if (dir.exists()) {
                dir.deleteRecursively()
            }
        }
    }

    private fun createNotificationChannel(context: Application) {
        val getString = { id: Int -> context.getString(id) }
        val notificationManager = NotificationManagerCompat.from(context)
        val chatCompletedChannel = NotificationChannelCompat
            .Builder(
                CHAT_COMPLETED_NOTIFICATION_CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_HIGH
            )
            .setName(getString(R.string.notification_channel_chat_completed))
            .setVibrationEnabled(true)
            .build()
        val spontaneousChannel = NotificationChannelCompat
            .Builder(
                SPONTANEOUS_NOTIFICATION_CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_DEFAULT
            )
            .setName(getString(R.string.notification_channel_spontaneous))
            .setVibrationEnabled(true)
            .build()
        notificationManager.createNotificationChannel(chatCompletedChannel)
        notificationManager.createNotificationChannel(spontaneousChannel)
    }

    /** Called by Absencium's Application when memory is low; does nothing until the runtime has started. */
    fun trim(level: Int) {
        if (!started) return
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
            level == android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
        ) {
            runCatching { coil3.SingletonImageLoader.get(instance).memoryCache?.clear() }
            runCatching { get<me.rerere.rikkahub.service.ChatService>().checkAllConversationsReferences() }
            runCatching { me.rerere.rikkahub.data.model.clearCompiledRegexCache() }
            runCatching { me.rerere.rikkahub.service.assist.AssistScreenHolder.clear() }
        }
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            runCatching { get<me.rerere.rikkahub.data.ai.AILoggingManager>().clearLogs() }
        }
    }
}

class AppScope : CoroutineScope by CoroutineScope(
    SupervisorJob()
        + Dispatchers.Default
        + CoroutineName("AppScope")
        + CoroutineExceptionHandler { _, e ->
            Log.e(TAG, "AppScope exception", e)
        }
)
