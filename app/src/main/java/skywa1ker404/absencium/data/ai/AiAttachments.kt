package skywa1ker404.absencium.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import skywa1ker404.absencium.logging.DebugLogger
import java.io.File

/** What came of adding a file to a message: the stored [attachment], or the plain reason it could not be used. */
data class AttachmentResult(val attachment: ChatAttachment? = null, val error: String? = null)

/** The part of a turn one attached file becomes: a picture or PDF sent as data, or the text of a text file. */
data class AttachmentPart(val image: AiImage? = null, val text: String? = null)

/**
 * Files attached to chat messages. A copy is kept in the app's private storage (never uploaded anywhere but to the model the student
 * is chatting with), so a question can be asked again later. Pictures are scaled down, PDFs and text files are kept as they are.
 */
class AiAttachments(private val context: Context) {
    private val dir get() = File(context.filesDir, "ai_attachments").also { it.mkdirs() }

    suspend fun import(uri: Uri, displayName: String? = null): AttachmentResult = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val name = displayName ?: resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            } ?: "file"
            val mime = (resolver.getType(uri) ?: guessMime(name))
            val stamp = System.nanoTime()
            when {
                mime.startsWith("image/") -> {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                    var sample = 1
                    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_EDGE * 2) sample *= 2
                    val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
                        ?: return@withContext AttachmentResult(error = "That picture could not be opened.")
                    val scale = MAX_EDGE.toFloat() / maxOf(bitmap.width, bitmap.height)
                    val scaled = if (scale < 1f) Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1), (bitmap.height * scale).toInt().coerceAtLeast(1), true) else bitmap
                    val file = File(dir, "$stamp.jpg")
                    file.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 82, it) }
                    AttachmentResult(ChatAttachment(name, "image/jpeg", file.absolutePath))
                }
                mime == "application/pdf" || mime.startsWith("text/") || isTextName(name) -> {
                    val isPdf = mime == "application/pdf"
                    val limit = if (isPdf) MAX_PDF_BYTES else MAX_TEXT_BYTES
                    val file = File(dir, "$stamp-${name.filter { it.isLetterOrDigit() || it == '.' || it == '-' }.takeLast(40)}")
                    var copied = 0L
                    resolver.openInputStream(uri)?.use { input ->
                        file.outputStream().use { out ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                val n = input.read(buffer)
                                if (n < 0) break
                                copied += n
                                if (copied > limit) break
                                out.write(buffer, 0, n)
                            }
                        }
                    } ?: return@withContext AttachmentResult(error = "That file could not be opened.")
                    if (copied > limit) {
                        file.delete()
                        return@withContext AttachmentResult(error = if (isPdf) "That PDF is over ${MAX_PDF_BYTES / 1_000_000} MB. Send fewer pages." else "That text file is too large. Send a shorter part.")
                    }
                    AttachmentResult(ChatAttachment(name, if (isPdf) "application/pdf" else "text/plain", file.absolutePath))
                }
                mime.startsWith("audio/") || mime.startsWith("video/") -> {
                    val file = File(dir, "$stamp-${name.filter { it.isLetterOrDigit() || it == '.' || it == '-' }.takeLast(40)}")
                    var copied = 0L
                    resolver.openInputStream(uri)?.use { input ->
                        file.outputStream().use { out ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                val n = input.read(buffer)
                                if (n < 0) break
                                copied += n
                                if (copied > MAX_MEDIA_BYTES) break
                                out.write(buffer, 0, n)
                            }
                        }
                    } ?: return@withContext AttachmentResult(error = "That file could not be opened.")
                    if (copied > MAX_MEDIA_BYTES) {
                        file.delete()
                        return@withContext AttachmentResult(error = "That ${if (mime.startsWith("audio/")) "recording" else "video"} is over ${MAX_MEDIA_BYTES / 1_000_000} MB. Send a shorter part.")
                    }
                    AttachmentResult(ChatAttachment(name, mime, file.absolutePath))
                }
                else -> AttachmentResult(error = "Absencium AI can read pictures, PDFs, text files, recordings and short videos.")
            }
        } catch (e: Exception) {
            DebugLogger.e("AiAttachments", "A file could not be attached", e)
            AttachmentResult(error = "That file could not be attached.")
        }
    }

    /** What to send for [attachment], or null when the stored copy is gone. */
    suspend fun part(attachment: ChatAttachment): AttachmentPart? = withContext(Dispatchers.IO) {
        val file = File(attachment.path)
        if (!file.isFile) return@withContext null
        try {
            if (attachment.mime == "text/plain") {
                AttachmentPart(text = "Attached file \"${attachment.name}\":\n${file.readText().take(MAX_TEXT_CHARS)}")
            } else {
                // Pictures, PDFs, recordings and videos all travel as data with their own type.
                AttachmentPart(image = AiImage(attachment.mime, Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)))
            }
        } catch (e: Exception) {
            DebugLogger.e("AiAttachments", "A stored file could not be read", e)
            null
        }
    }

    fun discard(attachment: ChatAttachment) {
        runCatching { File(attachment.path).delete() }
    }

    private fun guessMime(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "pdf" -> "application/pdf"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "m4a" -> "audio/mp4"
        "ogg" -> "audio/ogg"
        "aac" -> "audio/aac"
        "flac" -> "audio/flac"
        "mp4" -> "video/mp4"
        "webm" -> "video/webm"
        "mov" -> "video/quicktime"
        else -> if (isTextName(name)) "text/plain" else "application/octet-stream"
    }

    private fun isTextName(name: String) = name.substringAfterLast('.', "").lowercase() in TEXT_EXTENSIONS

    companion object {
        private const val MAX_EDGE = 1600
        private const val MAX_PDF_BYTES = 15_000_000L
        private const val MAX_MEDIA_BYTES = 15_000_000L
        private const val MAX_TEXT_BYTES = 400_000L
        private const val MAX_TEXT_CHARS = 60_000
        private val TEXT_EXTENSIONS = setOf("txt", "md", "csv", "json", "xml", "html", "css", "js", "ts", "kt", "java", "py", "c", "cpp", "h", "cs", "go", "rs", "sql", "tex", "yaml", "yml", "log")
    }
}
