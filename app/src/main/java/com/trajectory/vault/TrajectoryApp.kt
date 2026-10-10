package com.trajectory.vault

import android.app.Application
import com.trajectory.vault.data.db.TrajectoryDatabase
import com.trajectory.vault.data.storage.NotesStorage
import com.trajectory.vault.logging.DebugLogger
import kotlin.concurrent.thread

class TrajectoryApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // The logger comes first so everything after it, including crashes, is captured.
        DebugLogger.init(this)
        DebugLogger.lifecycle("Application created")

        // Opening the database and touching storage are file operations, so keep them
        // off the main thread. Results and any failures go to debug.log.
        thread(name = "startup-checks") {
            TrajectoryDatabase.verifyOpen(this)
            DebugLogger.i("Storage", "Notes storage: ${NotesStorage(this).describeState()}")
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
