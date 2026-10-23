package skywa1ker404.absencium.data.share

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import skywa1ker404.absencium.data.db.AttachmentEntity
import skywa1ker404.absencium.data.db.AttachmentKind
import skywa1ker404.absencium.data.db.NoteEntity
import skywa1ker404.absencium.logging.DebugLogger
import skywa1ker404.absencium.ui.vault.markdown.MarkdownParse
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class ShareFormat(val label: String, val extension: String, val mime: String) {
    PDF("PDF", "pdf", "application/pdf"),
    MARKDOWN("Markdown", "md", "text/markdown"),
    TEXT("Plain text", "txt", "text/plain"),
    ZIP("ZIP with photos", "zip", "application/zip"),
}

/** A finished export, ready to hand to Android's Sharesheet. */
data class PreparedShare(val uri: Uri, val mime: String, val displayName: String, val note: String?)

/**
 * Turns notes into a file and shares it through Android's Sharesheet with a FileProvider URI, so the receiving app gets
 * read access to that one file and nothing else. Files are written to the cache and the older ones removed on the next
 * export. This is the one place that sends notes out of the app; read-only sharing (Step 17) extends it.
 */
class ShareService(private val context: Context) {

    private val exportDir: File get() = File(context.cacheDir, DIR)

    /** Builds the file. Returns the share or a message that explains why not. */
    suspend fun prepare(
        notes: List<NoteEntity>,
        attachmentsOf: (Long) -> List<AttachmentEntity>,
        format: ShareFormat,
    ): Result<PreparedShare> = withContext(Dispatchers.IO) {
        try {
            require(notes.isNotEmpty()) { "Nothing to share." }
            val dir = exportDir
            if (!dir.exists() && !dir.mkdirs()) throw IOException("Could not create a folder for the export.")
            clean(dir)
            // Rough need: the text, plus every photo for a ZIP. Refuse early instead of failing half way.
            val photoBytes = if (format == ShareFormat.ZIP) notes.sumOf { n -> attachmentsOf(n.id).filter { it.kind == AttachmentKind.IMAGE }.sumOf { File(it.filePath).length() } } else 0L
            val need = photoBytes + notes.sumOf { it.bodyText.length.toLong() * 4 } + 2_000_000L
            if (dir.usableSpace < need) throw IOException("There is not enough free storage to prepare this export.")

            var skipped = 0
            val file: File
            val displayName: String
            if (format == ShareFormat.ZIP || notes.size > 1) {
                displayName = if (notes.size == 1) ExportNames.fileName(notes[0].title, "zip") else "Absencium notes.zip"
                file = File(dir, displayName)
                skipped = writeZip(file, notes, attachmentsOf, includePhotos = format == ShareFormat.ZIP)
            } else {
                val note = notes[0]
                displayName = ExportNames.fileName(note.title, format.extension)
                file = File(dir, displayName)
                when (format) {
                    ShareFormat.PDF -> {
                        val attachments = attachmentsOf(note.id)
                        file.outputStream().use {
                            PdfNoteRenderer.render(note.title, MarkdownParse.blocks(note.bodyText), it) { embed ->
                                attachments.firstOrNull { a -> a.id == embed.id }?.let { a -> skywa1ker404.absencium.ui.vault.entry.ImageTools.decode(a.filePath, 1600) }
                            }
                        }
                    }
                    ShareFormat.MARKDOWN -> file.writeText(ExportNames.markdown(note.title, note.tags, note.bodyText))
                    ShareFormat.TEXT -> file.writeText(ExportNames.plainText(note.title, note.tags, note.bodyText))
                    ShareFormat.ZIP -> error("handled above")
                }
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
            val mime = if (file.extension == "zip") ShareFormat.ZIP.mime else format.mime
            val note = if (skipped > 0) "$skipped photo(s) could not be read and were left out." else null
            Result.success(PreparedShare(uri, mime, displayName, note))
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        } catch (e: IOException) {
            DebugLogger.e(TAG, "Export failed", e)
            Result.failure(IOException(e.message ?: "The export could not be written."))
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Export failed", e)
            Result.failure(IOException("The export could not be prepared. See debug.log for details."))
        }
    }

    private fun writeZip(target: File, notes: List<NoteEntity>, attachmentsOf: (Long) -> List<AttachmentEntity>, includePhotos: Boolean): Int {
        val names = ExportNames.unique(notes.map { ExportNames.fileName(it.title, "md") })
        var skipped = 0
        ZipOutputStream(target.outputStream().buffered()).use { zip ->
            notes.forEachIndexed { index, note ->
                zip.putNextEntry(ZipEntry(names[index]))
                zip.write(ExportNames.markdown(note.title, note.tags, note.bodyText).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                if (includePhotos) {
                    val folder = names[index].removeSuffix(".md")
                    val photoNames = ExportNames.unique(attachmentsOf(note.id).filter { it.kind == AttachmentKind.IMAGE }.map { File(it.filePath).name })
                    attachmentsOf(note.id).filter { it.kind == AttachmentKind.IMAGE }.forEachIndexed { i, photo ->
                        val source = File(photo.filePath)
                        if (!source.isFile) {
                            skipped++
                        } else {
                            zip.putNextEntry(ZipEntry("$folder/${photoNames[i]}"))
                            source.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                }
            }
        }
        return skipped
    }

    /** Removes exports from earlier shares; the receiving app has had its chance to read them. */
    private fun clean(dir: File) {
        val cutoff = System.currentTimeMillis() - 60L * 60 * 1000
        dir.listFiles()?.forEach { if (it.isFile && it.lastModified() < cutoff) it.delete() }
    }

    /** Opens the Sharesheet. Returns null on success, or a message when no app can take it. */
    fun share(share: PreparedShare): String? {
        val send = Intent(Intent.ACTION_SEND)
            .setType(share.mime)
            .putExtra(Intent.EXTRA_STREAM, share.uri)
            .putExtra(Intent.EXTRA_SUBJECT, share.displayName.substringBeforeLast('.'))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(share.displayName, share.uri)
        val chooser = Intent.createChooser(send, "Share ${share.displayName}").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(chooser)
            null
        } catch (e: ActivityNotFoundException) {
            "No app on this phone can receive this file."
        } catch (e: SecurityException) {
            "Android did not allow sharing this file."
        }
    }

    private companion object {
        const val DIR = "share_exports"
        const val TAG = "Share"
    }
}
