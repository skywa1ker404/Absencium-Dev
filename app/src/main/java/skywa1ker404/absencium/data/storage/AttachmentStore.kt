package skywa1ker404.absencium.data.storage

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import skywa1ker404.absencium.logging.DebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** A file that was copied into the app's own storage. */
data class StoredFile(val path: String, val displayName: String, val mimeType: String, val sizeBytes: Long)

/**
 * Keeps photos, documents and recordings of vault entries in the app's private storage, so they stay
 * available offline and do not depend on a folder the user may later move. Only this class touches the files.
 */
class AttachmentStore(private val context: Context) {

    private val directory: File
        get() = File(context.filesDir, FOLDER).also { it.mkdirs() }

    /** An empty file with a unique name, for a camera or recorder to write into. */
    fun newFile(extension: String): File = File(directory, "${UUID.randomUUID()}.${extension.trimStart('.')}")

    fun uriFor(file: File): Uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)

    /** Copies a picked file into the store. Returns null (after logging) when it cannot be read. */
    suspend fun import(source: Uri): StoredFile? = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val mime = resolver.getType(source).orEmpty()
            var name = "file"
            var size = -1L
            resolver.query(source, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    c.getString(0)?.let { name = it }
                    if (!c.isNull(1)) size = c.getLong(1)
                }
            }
            val extension = name.substringAfterLast('.', "").lowercase().ifEmpty {
                MimeTypeMap.getSingleton().getExtensionFromMimeType(mime).orEmpty()
            }.ifEmpty { "bin" }
            val target = newFile(extension)
            val input = resolver.openInputStream(source) ?: return@withContext null
            input.use { i -> target.outputStream().use { o -> i.copyTo(o) } }
            val resolvedMime = mime.ifEmpty {
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
            }
            StoredFile(target.absolutePath, name, resolvedMime, if (size >= 0) size else target.length())
        } catch (e: Exception) {
            DebugLogger.e("Attachments", "Could not copy the picked file", e)
            null
        }
    }

    /** Describes a file this store already holds, such as a camera photo. */
    fun describe(file: File, displayName: String, mimeType: String): StoredFile =
        StoredFile(file.absolutePath, displayName, mimeType, file.length())

    /** Deletes a stored file, ignoring any path outside the store. */
    fun delete(path: String) {
        try {
            val file = File(path)
            if (file.parentFile?.canonicalPath == directory.canonicalPath) file.delete()
        } catch (e: Exception) {
            DebugLogger.e("Attachments", "Could not delete $path", e)
        }
    }

    companion object {
        /** Name of the folder inside the app's files directory that holds the attachments. */
        const val FOLDER = "vault_attachments"
    }
}
