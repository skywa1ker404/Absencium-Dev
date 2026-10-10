package com.trajectory.vault.data.storage

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.trajectory.vault.logging.DebugLogger
import java.io.File

/**
 * Resolves the notes root folder from the user's storage choice.
 *
 * Folder names used inside the root for the vault layout:
 *  - one folder per subject
 *  - [FINISHED_FOLDER] for notes marked complete
 *  - [GUIDES_FOLDER] for imported PDFs
 * They are plain names because file systems do not allow "/" in a name; the UI shows the
 * friendlier labels.
 */
class NotesStorage(
    context: Context,
    private val settings: NotesStorageSettings = NotesStorageSettings(context),
) {

    private val appContext = context.applicationContext

    /**
     * The root folder for the current choice, or null when no choice has been made yet
     * (onboarding pending) or the chosen custom folder can no longer be used.
     * May touch the file system, so call it off the main thread.
     */
    fun rootFolder(): StorageFolder? =
        when (settings.selectedMode()) {
            null -> null
            NotesStorageMode.APP_FILES -> appFilesRoot()
            NotesStorageMode.CUSTOM_FOLDER -> customRoot()
        }

    /** One line describing the current state, for the startup log. */
    fun describeState(): String {
        val mode = settings.selectedMode() ?: return "not configured yet (onboarding pending)"
        val root = rootFolder() ?: return "mode=$mode but the folder is unavailable"
        return "mode=$mode root=${root.displayPath}"
    }

    private fun appFilesRoot(): StorageFolder? {
        val external = appContext.getExternalFilesDir(null)
        val base = external ?: appContext.filesDir
        if (external == null) {
            DebugLogger.w(TAG, "External files directory unavailable, using internal storage")
        }
        val notes = File(base, NOTES_FOLDER)
        return try {
            if (!notes.exists() && !notes.mkdirs()) {
                DebugLogger.e(TAG, "Could not create ${notes.absolutePath}")
                null
            } else {
                FileStorageFolder(notes)
            }
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Could not open ${notes.absolutePath}", e)
            null
        }
    }

    private fun customRoot(): StorageFolder? {
        val uri = settings.customTreeUri()
        if (uri == null) {
            DebugLogger.e(TAG, "Custom folder mode is selected but no folder URI is stored")
            return null
        }
        val granted = appContext.contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission && it.isWritePermission
        }
        if (!granted) {
            DebugLogger.e(TAG, "Permission for the chosen folder was lost: $uri")
            return null
        }
        val root = DocumentFile.fromTreeUri(appContext, uri)
        if (root == null || !root.exists() || !root.canWrite()) {
            DebugLogger.e(TAG, "Chosen folder no longer exists or is not writable: $uri")
            return null
        }
        return SafStorageFolder(appContext, root)
    }

    companion object {
        const val NOTES_FOLDER = "notes"
        const val FINISHED_FOLDER = "Finished"
        const val GUIDES_FOLDER = "Guides and Question Banks"
        private const val TAG = "Storage"
    }
}
