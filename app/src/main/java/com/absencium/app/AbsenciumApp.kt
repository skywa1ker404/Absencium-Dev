package com.absencium.app

import android.app.Application
import com.absencium.app.data.backup.DatabaseBackup
import com.absencium.app.data.db.AbsenciumDatabase
import com.absencium.app.logging.DebugLogger
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
        DatabaseBackup.applyStagedRestore(this)
        container = AppContainer(this)

        // Opening the database and touching storage are file operations, so keep them
        // off the main thread. Results and any failures go to debug.log.
        thread(name = "startup-checks") {
            AbsenciumDatabase.verifyOpen(this)
            DebugLogger.i("Storage", "Notes storage: ${container.notesStorage.describeState()}")
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
}
