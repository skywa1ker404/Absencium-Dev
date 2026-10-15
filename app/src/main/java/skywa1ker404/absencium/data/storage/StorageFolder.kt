package skywa1ker404.absencium.data.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import skywa1ker404.absencium.logging.DebugLogger
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * A folder in the notes storage, backed either by a plain directory in the app's files
 * ([FileStorageFolder]) or by a folder the user picked through the Storage Access Framework
 * ([SafStorageFolder]).
 *
 * Operations that can fail return null (or an empty list) and log the reason to debug.log,
 * so callers can show an error without catching storage exceptions themselves.
 */
interface StorageFolder {
    /** A readable description of where this folder is, for display in the UI. */
    val displayPath: String

    /** The subfolder called [name], created if it does not exist yet. */
    fun childFolder(name: String): StorageFolder?

    /** Names of the files (not folders) directly inside this folder, sorted. */
    fun listFileNames(): List<String>

    fun openInput(fileName: String): InputStream?

    /** Opens [fileName] for writing, creating it or replacing its contents. */
    fun openOutput(fileName: String, mimeType: String): OutputStream?

    /**
     * Where [fileName] can be found again: an absolute path for a plain folder, a document URI
     * for a folder the user picked. Null if the file does not exist.
     */
    fun locationOf(fileName: String): String?

    /** Deletes [fileName]. Returns true if it is gone afterwards. */
    fun delete(fileName: String): Boolean
}

private const val TAG = "Storage"
private const val MAX_NAME_LENGTH = 100

/** Replaces characters that are not allowed in file names on common file systems. */
fun sanitizeFileName(name: String): String {
    val cleaned = name
        .map { ch -> if (ch.isISOControl() || ch in "/\\:*?\"<>|") '_' else ch }
        .joinToString("")
        .trim()
        .take(MAX_NAME_LENGTH)
    return cleaned.ifBlank { "_" }
}

class FileStorageFolder(private val directory: File) : StorageFolder {

    override val displayPath: String
        get() = directory.absolutePath

    override fun childFolder(name: String): StorageFolder? {
        val child = File(directory, sanitizeFileName(name))
        return try {
            if (!child.exists() && !child.mkdirs()) {
                DebugLogger.e(TAG, "Could not create folder ${child.absolutePath}")
                null
            } else if (child.isDirectory) {
                FileStorageFolder(child)
            } else {
                DebugLogger.e(TAG, "${child.absolutePath} exists but is not a folder")
                null
            }
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Could not open folder ${child.absolutePath}", e)
            null
        }
    }

    override fun listFileNames(): List<String> =
        directory.listFiles()?.filter { it.isFile }?.map { it.name }?.sorted() ?: emptyList()

    override fun openInput(fileName: String): InputStream? {
        val file = File(directory, sanitizeFileName(fileName))
        return try {
            if (file.isFile) file.inputStream() else null
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Could not read ${file.absolutePath}", e)
            null
        }
    }

    override fun openOutput(fileName: String, mimeType: String): OutputStream? {
        val file = File(directory, sanitizeFileName(fileName))
        return try {
            file.outputStream()
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Could not write ${file.absolutePath}", e)
            null
        }
    }

    override fun locationOf(fileName: String): String? =
        File(directory, sanitizeFileName(fileName)).takeIf { it.isFile }?.absolutePath

    override fun delete(fileName: String): Boolean {
        val file = File(directory, sanitizeFileName(fileName))
        return try {
            !file.exists() || file.delete()
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Could not delete ${file.absolutePath}", e)
            false
        }
    }
}

/**
 * A folder inside a tree the user granted through the system folder picker.
 *
 * Note: when a new file is created, Android may append an extension that matches [mimeType]
 * to the file name, so the stored name can differ from the requested one.
 */
class SafStorageFolder(
    private val context: Context,
    private val folder: DocumentFile,
) : StorageFolder {

    override val displayPath: String
        get() = Uri.decode(folder.uri.toString())

    override fun childFolder(name: String): StorageFolder? {
        val safeName = sanitizeFileName(name)
        return try {
            val existing = folder.findFile(safeName)
            val child = when {
                existing == null -> folder.createDirectory(safeName)
                existing.isDirectory -> existing
                else -> null
            }
            if (child == null) {
                DebugLogger.e(TAG, "Could not create or open folder \"$safeName\" in $displayPath")
                null
            } else {
                SafStorageFolder(context, child)
            }
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Could not open folder \"$safeName\" in $displayPath", e)
            null
        }
    }

    override fun listFileNames(): List<String> =
        try {
            folder.listFiles().filter { it.isFile }.mapNotNull { it.name }.sorted()
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Could not list files in $displayPath", e)
            emptyList()
        }

    override fun openInput(fileName: String): InputStream? {
        val safeName = sanitizeFileName(fileName)
        return try {
            val file = folder.findFile(safeName)
            if (file != null && file.isFile) context.contentResolver.openInputStream(file.uri) else null
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Could not read \"$safeName\" in $displayPath", e)
            null
        }
    }

    override fun openOutput(fileName: String, mimeType: String): OutputStream? {
        val safeName = sanitizeFileName(fileName)
        return try {
            val target = folder.findFile(safeName) ?: folder.createFile(mimeType, safeName)
            if (target == null) {
                DebugLogger.e(TAG, "Could not create \"$safeName\" in $displayPath")
                null
            } else {
                // "wt" truncates an existing file instead of overwriting from the start.
                context.contentResolver.openOutputStream(target.uri, "wt")
            }
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Could not write \"$safeName\" in $displayPath", e)
            null
        }
    }

    override fun locationOf(fileName: String): String? =
        try {
            folder.findFile(sanitizeFileName(fileName))?.takeIf { it.isFile }?.uri?.toString()
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Could not locate \"$fileName\" in $displayPath", e)
            null
        }

    override fun delete(fileName: String): Boolean =
        try {
            val file = folder.findFile(sanitizeFileName(fileName))
            file == null || file.delete()
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Could not delete \"$fileName\" in $displayPath", e)
            false
        }
}
