package skywa1ker404.absencium

import android.app.Application
import skywa1ker404.absencium.data.backup.BackupManager
import kotlinx.coroutines.flow.first
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

        // Opening the database and touching storage are file operations, so keep them
        // off the main thread. Results and any failures go to debug.log.
        thread(name = "startup-checks") {
            AbsenciumDatabase.verifyOpen(this)
            runStartupMaintenance()
            DebugLogger.i("Storage", "Notes storage: ${container.notesStorage.describeState()}")
        }
    }

    /** Things that can wait for the database to be open: settings from a restore, the Trash clean-up, the daily snapshot. */
    private fun runStartupMaintenance() {
        try {
            container.backupManager.takeStagedSettings()?.let {
                runBlocking { container.settingsRepository.importJson(it) }
            }
            runBlocking { container.trashRepository.purgeExpired() }
            val settings = runBlocking { container.settingsRepository.settings.first() }
            val newest = container.backupManager.snapshots().firstOrNull()?.createdAt ?: 0L
            if (settings.autoSnapshots && System.currentTimeMillis() - newest > SNAPSHOT_INTERVAL_MS) {
                val settingsJson = runBlocking { container.settingsRepository.exportJson() }
                container.backupManager.createSnapshot(settingsJson)
            }
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
    }

    private companion object {
        const val SNAPSHOT_INTERVAL_MS = 24L * 60 * 60 * 1000
    }
}
