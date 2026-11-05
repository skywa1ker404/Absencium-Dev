package skywa1ker404.absencium

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import skywa1ker404.absencium.display.RefreshRatePreference
import skywa1ker404.absencium.ui.theme.AbsenciumTheme
import skywa1ker404.absencium.ui.theme.isDarkTheme
import skywa1ker404.absencium.data.backup.BackupManager
import kotlinx.coroutines.runBlocking
import skywa1ker404.absencium.data.db.AbsenciumDatabase
import skywa1ker404.absencium.logging.DebugLogger
import kotlin.concurrent.thread

class AbsenciumApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // The logger comes first so everything after it, including crashes, is captured.
        DebugLogger.init(this)
        DebugLogger.lifecycle("Application created")
        // A restore chosen in Settings is applied here, before anything opens the database.
        BackupManager.applyStagedRestore(this)
        container = AppContainer(this)
        installChatHost()

        // Opening the database and touching storage are file operations, so keep them
        // off the main thread. Results and any failures go to debug.log.
        thread(name = "startup-checks") {
            AbsenciumDatabase.verifyOpen(this)
            runStartupMaintenance()
            DebugLogger.i("Storage", "Notes storage: ${container.notesStorage.describeState()}")
        }
    }

    /** Things that can wait for the database to be open: settings from a restore, the Trash clean-up. */
    private fun runStartupMaintenance() {
        try {
            container.backupManager.takeStagedSettings()?.let {
                runBlocking { container.settingsRepository.importJson(it) }
            }
            runBlocking { container.trashRepository.purgeExpired() }
        } catch (e: Exception) {
            DebugLogger.e("Startup", "Maintenance at startup failed", e)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        DebugLogger.w("Memory", "onLowMemory")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        DebugLogger.w("Memory", "onTrimMemory level=$level")
        me.rerere.rikkahub.LastChatApp.trim(level)
    }

    /** The embedded Absencium AI chat wears this app's theme engine and follows its refresh-rate cap. */
    private fun installChatHost() {
        me.rerere.rikkahub.LastChatHost.theme = { content ->
            val settings by container.settingsState.collectAsStateWithLifecycle()
            settings?.let { loaded ->
                AbsenciumTheme(settings = loaded) { content(isDarkTheme(loaded)) }
            }
        }
        val chatTools = skywa1ker404.absencium.data.ai.ChatAppTools(container)
        me.rerere.rikkahub.LastChatHost.tools = { chatTools.tools() }
        val rates = HashMap<Activity, RefreshRatePreference>()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                if (activity.javaClass.name.startsWith("me.rerere.rikkahub.")) {
                    rates.getOrPut(activity) { RefreshRatePreference(activity) }.start()
                }
            }

            override fun onActivityStopped(activity: Activity) {
                rates.remove(activity)?.stop()
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }
}
