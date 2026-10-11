package com.trajectory.vault.logging

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Centralized file logger.
 *
 * Every entry is appended to `debug.log` immediately (open, append, close), so the
 * file is current even if the process dies right after a write. The file lives in the
 * app-specific external files directory:
 *
 *     Android/data/com.trajectory.vault/files/debug.log
 *
 * If external storage is unavailable the logger falls back to the app's internal files
 * directory and records that fact in the log header.
 *
 * Captured automatically once [init] has run:
 *  - unhandled exceptions on any thread (written before the process terminates)
 *  - activity lifecycle events
 *
 * The logger never throws. A failure to write is reported to Logcat only.
 */
object DebugLogger {

    const val LOG_FILE_NAME = "debug.log"
    private const val ROLLED_FILE_NAME = "debug.log.1"
    private const val MAX_LOG_BYTES = 2L * 1024L * 1024L
    private const val TAG = "TrajectoryLog"

    private val lock = Any()
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    @Volatile
    private var logFile: File? = null

    @Volatile
    private var initialized = false

    /** Absolute path of the active log file, or null before [init]. */
    val logFilePath: String?
        get() = logFile?.absolutePath

    /** The last [maxLines] lines of the log file, for the Test Lab. */
    fun readTail(maxLines: Int = 120): String {
        val file = logFile ?: return "(logger not initialised)"
        return synchronized(lock) {
            try {
                file.readLines().takeLast(maxLines).joinToString("\n")
            } catch (e: Exception) {
                "Could not read the log: ${e.message ?: e.javaClass.simpleName}"
            }
        }
    }

    fun init(application: Application) {
        synchronized(lock) {
            if (initialized) return
            val externalDir = application.getExternalFilesDir(null)
            val directory = externalDir ?: application.filesDir
            directory.mkdirs()
            val file = File(directory, LOG_FILE_NAME)
            rollIfTooLarge(file)
            logFile = file
            initialized = true
            if (externalDir == null) {
                writeLine(
                    "WARN",
                    TAG,
                    "External files directory unavailable, logging to internal storage instead: ${file.absolutePath}",
                    null,
                )
            }
        }
        writeSessionHeader(application)
        installCrashHandler()
        application.registerActivityLifecycleCallbacks(LifecycleLogger)
    }

    fun d(tag: String, message: String, throwable: Throwable? = null) = log("DEBUG", tag, message, throwable)

    fun i(tag: String, message: String, throwable: Throwable? = null) = log("INFO", tag, message, throwable)

    fun w(tag: String, message: String, throwable: Throwable? = null) = log("WARN", tag, message, throwable)

    fun e(tag: String, message: String, throwable: Throwable? = null) = log("ERROR", tag, message, throwable)

    fun lifecycle(message: String) = log("LIFECYCLE", "Lifecycle", message, null)

    private fun log(level: String, tag: String, message: String, throwable: Throwable?) {
        when (level) {
            "DEBUG" -> Log.d(tag, message, throwable)
            "INFO", "LIFECYCLE" -> Log.i(tag, message, throwable)
            "WARN" -> Log.w(tag, message, throwable)
            else -> Log.e(tag, message, throwable)
        }
        synchronized(lock) { writeLine(level, tag, message, throwable) }
    }

    /** Must be called while holding [lock], or during init before the logger is shared. */
    private fun writeLine(level: String, tag: String, message: String, throwable: Throwable?) {
        val file = logFile ?: return
        try {
            val text = buildString {
                append(timestampFormat.format(Date()))
                append(' ')
                append(level)
                append(" [")
                append(Thread.currentThread().name)
                append("] ")
                append(tag)
                append(": ")
                append(message)
                append('\n')
                if (throwable != null) {
                    append(Log.getStackTraceString(throwable))
                    append('\n')
                }
            }
            file.appendText(text, Charsets.UTF_8)
        } catch (t: Throwable) {
            Log.e(TAG, "Could not write to ${file.absolutePath}", t)
        }
    }

    private fun rollIfTooLarge(file: File) {
        try {
            if (file.exists() && file.length() > MAX_LOG_BYTES) {
                val rolled = File(file.parentFile, ROLLED_FILE_NAME)
                if (rolled.exists()) rolled.delete()
                file.renameTo(rolled)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Could not roll log file", t)
        }
    }

    private fun writeSessionHeader(application: Application) {
        val versionName = try {
            application.packageManager.getPackageInfo(application.packageName, 0).versionName
        } catch (t: Throwable) {
            "unknown"
        }
        log("INFO", TAG, "================ NEW SESSION ================", null)
        log(
            "INFO",
            TAG,
            "app=${application.packageName} version=$versionName " +
                "device=${Build.MANUFACTURER} ${Build.MODEL} android=${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            null,
        )
        log("INFO", TAG, "log file: ${logFilePath}", null)
    }

    private fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                log("FATAL", "UncaughtException", "Unhandled exception in thread \"${thread.name}\"", throwable)
            } catch (ignored: Throwable) {
                // The crash handler must never throw.
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private object LifecycleLogger : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) =
            DebugLogger.lifecycle("${activity.javaClass.simpleName} created (restoredState=${savedInstanceState != null})")

        override fun onActivityStarted(activity: Activity) =
            DebugLogger.lifecycle("${activity.javaClass.simpleName} started")

        override fun onActivityResumed(activity: Activity) =
            DebugLogger.lifecycle("${activity.javaClass.simpleName} resumed")

        override fun onActivityPaused(activity: Activity) =
            DebugLogger.lifecycle("${activity.javaClass.simpleName} paused")

        override fun onActivityStopped(activity: Activity) =
            DebugLogger.lifecycle("${activity.javaClass.simpleName} stopped")

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) =
            DebugLogger.lifecycle("${activity.javaClass.simpleName} saveInstanceState")

        override fun onActivityDestroyed(activity: Activity) =
            DebugLogger.lifecycle("${activity.javaClass.simpleName} destroyed (finishing=${activity.isFinishing})")
    }
}
