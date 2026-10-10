package com.trajectory.vault.data.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.edit
import androidx.documentfile.provider.DocumentFile
import com.trajectory.vault.logging.DebugLogger

enum class NotesStorageMode {
    /** `Android/data/<package>/files/notes`, no permission needed. */
    APP_FILES,

    /** A folder the user picked through the Storage Access Framework. */
    CUSTOM_FOLDER,
}

/**
 * Remembers where notes are stored: the chosen [NotesStorageMode] and, for a custom folder,
 * the picked tree URI. Nothing is chosen until the onboarding step runs, so [selectedMode]
 * is null on a fresh install.
 */
class NotesStorageSettings(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun selectedMode(): NotesStorageMode? =
        prefs.getString(KEY_MODE, null)?.let { stored ->
            NotesStorageMode.entries.firstOrNull { it.name == stored }
        }

    fun customTreeUri(): Uri? = prefs.getString(KEY_TREE_URI, null)?.let { Uri.parse(it) }

    /** Selects the default location inside the app's files. */
    fun useAppFiles() {
        releaseCustomFolderPermission()
        prefs.edit {
            putString(KEY_MODE, NotesStorageMode.APP_FILES.name)
            remove(KEY_TREE_URI)
        }
        DebugLogger.i(TAG, "Notes storage set to app files")
    }

    /**
     * Selects a folder returned by the system folder picker (`OpenDocumentTree`).
     * Takes a persistable read and write grant so access survives restarts. Returns false,
     * after logging the reason, if the grant could not be taken or the folder is not writable.
     */
    fun useCustomFolder(treeUri: Uri): Boolean {
        try {
            appContext.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        } catch (e: SecurityException) {
            DebugLogger.e(TAG, "Could not take persistable permission for $treeUri", e)
            return false
        }

        val root = DocumentFile.fromTreeUri(appContext, treeUri)
        if (root == null || !root.exists() || !root.isDirectory || !root.canWrite()) {
            DebugLogger.e(TAG, "Chosen folder is missing or not writable: $treeUri")
            return false
        }

        val previous = customTreeUri()
        prefs.edit {
            putString(KEY_MODE, NotesStorageMode.CUSTOM_FOLDER.name)
            putString(KEY_TREE_URI, treeUri.toString())
        }
        if (previous != null && previous != treeUri) releasePermission(previous)
        DebugLogger.i(TAG, "Notes storage set to custom folder: $treeUri")
        return true
    }

    private fun releaseCustomFolderPermission() {
        customTreeUri()?.let { releasePermission(it) }
    }

    private fun releasePermission(uri: Uri) {
        try {
            appContext.contentResolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        } catch (e: SecurityException) {
            DebugLogger.w(TAG, "Could not release permission for $uri", e)
        }
    }

    private companion object {
        const val TAG = "Storage"
        const val PREFS_NAME = "trajectory_storage"
        const val KEY_MODE = "notes_storage_mode"
        const val KEY_TREE_URI = "notes_custom_tree_uri"
    }
}
