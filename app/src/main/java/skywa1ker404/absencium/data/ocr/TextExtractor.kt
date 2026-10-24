package skywa1ker404.absencium.data.ocr

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import skywa1ker404.absencium.data.ai.AiClient
import skywa1ker404.absencium.data.ai.AiConnection
import skywa1ker404.absencium.data.ai.AiException
import skywa1ker404.absencium.data.ai.AiImage
import skywa1ker404.absencium.logging.DebugLogger
import skywa1ker404.absencium.ui.vault.entry.ImageTools
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import kotlin.coroutines.coroutineContext

/** How text is taken out of a file: by the user's AI provider, or on the phone without any network. */
enum class ExtractMode { AI, LOCAL }

/** What kind of file it is, as far as reading text from it goes. */
enum class FileKind { IMAGE, PDF, AUDIO, WORD, PLAIN, OTHER;

    companion object {
        fun of(mime: String, name: String): FileKind {
            val lower = name.lowercase()
            return when {
                mime.startsWith("image/") -> IMAGE
                mime == "application/pdf" || lower.endsWith(".pdf") -> PDF
                mime.startsWith("audio/") || lower.endsWith(".m4a") || lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".aac") || lower.endsWith(".ogg") -> AUDIO
                mime == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" || lower.endsWith(".docx") -> WORD
                mime.startsWith("text/") || lower.endsWith(".txt") || lower.endsWith(".md") -> PLAIN
                else -> OTHER
            }
        }
    }
}

/** What an extraction gave back. */
sealed interface Extracted {
    data class Text(val text: String) : Extracted
    data class Failed(val message: String, val setupMissing: Boolean = false) : Extracted
}

/**
 * Turns a stored file into text. [connection] says which AI to use, or null when none is set up.
 * With [ExtractMode.AI] the file goes to the provider (pictures, PDFs and recordings as they are; Word and text files are read on the
 * phone first and then tidied by the AI). With [ExtractMode.LOCAL] pictures and PDF pages go through the on-phone reader, Word
 * and text files are read directly, and a recording cannot be done at all.
 */
class TextExtractor(
    private val ai: AiClient,
    private val ocr: LocalOcr,
    private val connection: () -> AiConnection?,
) {

    fun aiReady(): Boolean = connection() != null

    /** Whether [mode] can work on this kind of file at all. */
    fun supports(kind: FileKind, mode: ExtractMode): Boolean = when (kind) {
        FileKind.IMAGE, FileKind.PDF -> true
        FileKind.AUDIO -> mode == ExtractMode.AI
        FileKind.WORD, FileKind.PLAIN -> true
        FileKind.OTHER -> false
    }

    suspend fun extract(path: String, mime: String, name: String, mode: ExtractMode, onStep: (String) -> Unit = {}): Extracted =
        withContext(Dispatchers.IO) {
            val kind = FileKind.of(mime, name)
            val file = File(path)
            if (!file.isFile) return@withContext Extracted.Failed("The file is no longer stored on this phone.")
            if (!supports(kind, mode)) {
                return@withContext Extracted.Failed(
                    if (kind == FileKind.AUDIO) "A recording can only be turned into text with AI." else "Absencium cannot read text from this kind of file.",
                )
            }
            try {
                val text = when (mode) {
                    ExtractMode.LOCAL -> local(file, kind, onStep)
                    ExtractMode.AI -> {
                        val conn = connection() ?: return@withContext Extracted.Failed("AI is not set up yet.", setupMissing = true)
                        viaAi(conn, file, kind, mime, onStep)
                    }
                }
                if (text.isBlank()) Extracted.Failed("No text was found in ${if (kind == FileKind.AUDIO) "that recording" else "that file"}.")
                else Extracted.Text(text.trim())
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: AiException) {
                Extracted.Failed(e.message ?: "The AI request failed.")
            } catch (e: IOException) {
                Extracted.Failed(e.message ?: "The file could not be read.")
            } catch (e: Exception) {
                DebugLogger.e("Extract", "Text extraction failed", e)
                Extracted.Failed("Something went wrong: ${e.message ?: e.javaClass.simpleName}")
            }
        }

    private suspend fun local(file: File, kind: FileKind, onStep: (String) -> Unit): String = when (kind) {
        FileKind.IMAGE -> {
            onStep("Reading the picture")
            val bitmap = ImageTools.decode(file.absolutePath, OCR_EDGE) ?: throw IOException("The picture could not be opened.")
            ocr.read(bitmap)
        }
        FileKind.PDF -> {
            val parts = ArrayList<String>()
            val total = PdfPages.render(file, PDF_WIDTH, MAX_PDF_PAGES) { index, bitmap ->
                onStep("Reading page ${index + 1}")
                parts += ocr.read(bitmap)
            }
            if (total == 0) throw IOException("This PDF could not be opened. It may be protected by a password.")
            coroutineContext.ensureActive()
            val note = if (total > MAX_PDF_PAGES) "\n\n[Only the first $MAX_PDF_PAGES of $total pages were read.]" else ""
            parts.filter { it.isNotBlank() }.joinToString("\n\n") + note
        }
        FileKind.WORD -> DocText.readDocx(file) ?: throw IOException("This Word file could not be read. Only .docx files work.")
        FileKind.PLAIN -> DocText.readPlain(file) ?: throw IOException("This text file is too large or cannot be read.")
        else -> ""
    }

    private suspend fun viaAi(conn: AiConnection, file: File, kind: FileKind, mime: String, onStep: (String) -> Unit): String {
        onStep("Sending to the AI")
        return when (kind) {
            FileKind.IMAGE -> {
                val bitmap = ImageTools.decode(file.absolutePath, AI_EDGE) ?: throw IOException("The picture could not be opened.")
                val bytes = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }.toByteArray()
                ai.generate(conn, IMAGE_PROMPT, listOf(AiImage("image/jpeg", Base64.encodeToString(bytes, Base64.NO_WRAP))))
            }
            FileKind.PDF -> ai.generate(conn, PDF_PROMPT, listOf(AiImage("application/pdf", encode(file, "PDF"))))
            FileKind.AUDIO -> {
                val audioMime = if (mime.startsWith("audio/")) mime else "audio/mp4"
                ai.generate(conn, AUDIO_PROMPT, listOf(AiImage(audioMime, encode(file, "recording"))))
            }
            FileKind.WORD, FileKind.PLAIN -> {
                val raw = local(file, kind, onStep)
                if (raw.isBlank()) "" else ai.generate(conn, TIDY_PROMPT + "\n\n" + raw.take(MAX_TIDY_CHARS))
            }
            FileKind.OTHER -> ""
        }
    }

    private fun encode(file: File, what: String): String {
        if (file.length() > MAX_AI_FILE_BYTES) {
            throw AiException("This $what is larger than ${MAX_AI_FILE_BYTES / 1_000_000} MB, which is too big to send to the AI. Try Locally (without AI) for a PDF, or a shorter recording.")
        }
        return Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
    }

    companion object {
        const val OCR_EDGE = 2600
        const val AI_EDGE = 1800
        const val PDF_WIDTH = 1500
        const val MAX_PDF_PAGES = 30
        const val MAX_AI_FILE_BYTES = 15_000_000L
        const val MAX_TIDY_CHARS = 60_000

        const val IMAGE_PROMPT =
            "Read all the text in the attached picture. Return only the text, keeping its structure (headings, lists, line breaks, " +
                "formulas written as plain text). Do not add commentary. If a part is unreadable write [unclear]. If there is no text at all, reply with nothing."
        const val PDF_PROMPT =
            "Convert the attached PDF into clean text notes. Keep its structure: write headings with # marks, lists with - bullets, and tables " +
                "as | cells |. Keep every word of the content; do not summarise and do not add commentary. If there is no text at all, reply with nothing."
        const val AUDIO_PROMPT =
            "Write down exactly what is said in the attached recording, word for word, in the language spoken. Put a blank line between " +
                "paragraphs. Do not summarise and do not add commentary. If nothing can be heard, reply with nothing."
        const val TIDY_PROMPT =
            "Tidy the following text into clean notes without changing its meaning or leaving anything out: headings with # marks, " +
                "lists with - bullets, tables as | cells |. Return only the notes."
    }
}
