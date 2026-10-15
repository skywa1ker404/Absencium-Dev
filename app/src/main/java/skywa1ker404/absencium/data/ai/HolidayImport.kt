package skywa1ker404.absencium.data.ai

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import org.json.JSONArray
import org.json.JSONException
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** One holiday the AI found. A single day has the same [start] and [end]. */
data class HolidayCandidate(val start: LocalDate, val end: LocalDate, val title: String) {
    val dayCount: Int get() = (ChronoUnit.DAYS.between(start, end) + 1).toInt()
}

/** A file the user added: [images] are JPEG pages ready to send, [text] is the text of a text file. */
class ImportAttachment(val name: String, val images: List<AiImage>, val text: String?)

/** Turns photos, PDFs and text files into what an AI request can carry. Call off the main thread. */
object AttachmentReader {

    private const val MAX_EDGE = 1600
    private const val MAX_PDF_PAGES = 8
    private const val MAX_TEXT_CHARS = 60_000
    private const val JPEG_QUALITY = 80

    fun read(resolver: ContentResolver, uri: Uri): ImportAttachment {
        val mime = resolver.getType(uri).orEmpty()
        val name = displayName(resolver, uri)
        return when {
            mime.startsWith("image/") -> ImportAttachment(name, listOf(readImage(resolver, uri)), null)
            mime == "application/pdf" -> ImportAttachment(name, readPdf(resolver, uri), null)
            mime.startsWith("text/") -> ImportAttachment(name, emptyList(), readText(resolver, uri))
            else -> throw AiException("\"$name\" is not a photo, PDF or text file.")
        }
    }

    private fun displayName(resolver: ContentResolver, uri: Uri): String =
        try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (e: Exception) {
            null
        } ?: "file"

    private fun readText(resolver: ContentResolver, uri: Uri): String {
        val stream = resolver.openInputStream(uri) ?: throw AiException("Could not open that file.")
        return stream.use { it.readBytes().toString(Charsets.UTF_8).take(MAX_TEXT_CHARS) }
    }

    private fun readImage(resolver: ContentResolver, uri: Uri): AiImage {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // decodeStream returns null when it only reads the size, so the stream itself is what is checked for a failure to open.
        val opened = resolver.openInputStream(uri) ?: throw AiException("Could not open that photo.")
        opened.use { BitmapFactory.decodeStream(it, null, bounds) }
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        if (longest <= 0) throw AiException("Could not read that photo.")
        var sample = 1
        while (longest / (sample * 2) >= MAX_EDGE) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: throw AiException("Could not read that photo.")

        val degrees = try {
            resolver.openInputStream(uri)?.use { stream ->
                when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (e: Exception) {
            0f
        }
        val upright = if (degrees == 0f) {
            decoded
        } else {
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(degrees) }, true)
                .also { decoded.recycle() }
        }
        return upright.toJpegImage()
    }

    private fun readPdf(resolver: ContentResolver, uri: Uri): List<AiImage> {
        val descriptor = resolver.openFileDescriptor(uri, "r") ?: throw AiException("Could not open that PDF.")
        val renderer = try {
            PdfRenderer(descriptor)
        } catch (e: Exception) {
            descriptor.close()
            throw AiException("That PDF could not be opened. It may be protected or damaged.")
        }
        try {
            val pages = minOf(renderer.pageCount, MAX_PDF_PAGES)
            val images = ArrayList<AiImage>(pages)
            for (index in 0 until pages) {
                val page = renderer.openPage(index)
                try {
                    val scale = MAX_EDGE.toFloat() / maxOf(page.width, page.height)
                    val bitmap = Bitmap.createBitmap(
                        (page.width * scale).toInt().coerceAtLeast(1),
                        (page.height * scale).toInt().coerceAtLeast(1),
                        Bitmap.Config.ARGB_8888,
                    )
                    // A PDF page renders on transparent, which a model would read as black.
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    images += bitmap.toJpegImage()
                } finally {
                    page.close()
                }
            }
            return images
        } finally {
            renderer.close()
        }
    }

    private fun Bitmap.toJpegImage(): AiImage {
        val scaled = if (maxOf(width, height) > MAX_EDGE) {
            val factor = MAX_EDGE.toFloat() / maxOf(width, height)
            Bitmap.createScaledBitmap(this, (width * factor).toInt(), (height * factor).toInt(), true)
        } else {
            this
        }
        val bytes = ByteArrayOutputStream().also { scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }.toByteArray()
        if (scaled !== this) scaled.recycle()
        recycle()
        return AiImage("image/jpeg", Base64.encodeToString(bytes, Base64.NO_WRAP))
    }
}

/** Asks the user's AI model to read a holiday list and returns what it found, for the user to review. */
class HolidayImporter(private val client: AiClient) {

    suspend fun extract(
        connection: AiConnection,
        text: String,
        attachments: List<ImportAttachment>,
        today: LocalDate,
    ): List<HolidayCandidate> {
        val pastedText = buildString {
            if (text.isNotBlank()) append(text.trim())
            attachments.mapNotNull { it.text }.forEach { append("\n\n").append(it) }
        }.trim()
        val images = attachments.flatMap { it.images }
        if (pastedText.isEmpty() && images.isEmpty()) throw AiException("Add some text, a photo or a PDF first.")

        val prompt = buildString {
            append("You read school and college holiday calendars. Today is $today. ")
            append("From the text and images provided, list every holiday or break. ")
            append("Reply with ONLY a JSON array and nothing else. Each item is ")
            append("{\"start\":\"YYYY-MM-DD\",\"end\":\"YYYY-MM-DD\",\"title\":\"short name\"}. ")
            append("A single day has the same start and end. A run of days is one item. ")
            append("Leave out exams, events that are not holidays, and ordinary weekends. ")
            append("If a date has no year, use the next occurrence on or after today. If there are none, reply [].")
            if (pastedText.isNotEmpty()) append("\n\nText:\n").append(pastedText)
        }
        val reply = client.generate(connection, prompt, images)
        return parseHolidays(reply)
    }

    companion object {
        private const val MAX_ITEMS = 100
        private const val MAX_RANGE_DAYS = 60

        /**
         * Reads the JSON array out of a model reply, tolerating code fences and text around it. Items with
         * a missing or invalid date, an end before the start, or a range over 60 days are dropped.
         */
        fun parseHolidays(reply: String): List<HolidayCandidate> {
            val from = reply.indexOf('[')
            val to = reply.lastIndexOf(']')
            if (from < 0 || to <= from) throw AiException("The AI did not return a list of holidays. Try again.")
            val array = try {
                JSONArray(reply.substring(from, to + 1))
            } catch (e: JSONException) {
                throw AiException("The AI's reply could not be read as a list. Try again.")
            }
            val found = ArrayList<HolidayCandidate>()
            for (i in 0 until minOf(array.length(), MAX_ITEMS)) {
                val item = array.optJSONObject(i) ?: continue
                val title = item.optString("title").trim().take(80)
                val start = parseDate(item.optString("start")) ?: continue
                val end = parseDate(item.optString("end")) ?: start
                if (title.isEmpty() || end.isBefore(start)) continue
                if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_RANGE_DAYS) continue
                found += HolidayCandidate(start, end, title)
            }
            return found.distinct().sortedBy { it.start }
        }

        private fun parseDate(text: String): LocalDate? =
            try {
                LocalDate.parse(text.trim())
            } catch (e: Exception) {
                null
            }
    }
}


/** One exam the AI found. [subject] is the name as written in the document, matched to the user's subjects afterwards. */
data class ExamCandidate(val subject: String, val title: String, val date: LocalDate, val room: String?)

/** Asks the user's AI model to read an exam timetable and returns what it found, for the user to review. */
class ExamImporter(private val client: AiClient) {

    suspend fun extract(
        connection: AiConnection,
        text: String,
        attachments: List<ImportAttachment>,
        today: LocalDate,
        subjectNames: List<String>,
    ): List<ExamCandidate> {
        val pastedText = buildString {
            if (text.isNotBlank()) append(text.trim())
            attachments.mapNotNull { it.text }.forEach { append("\n\n").append(it) }
        }.trim()
        val images = attachments.flatMap { it.images }
        if (pastedText.isEmpty() && images.isEmpty()) throw AiException("Add some text, a photo or a PDF first.")

        val prompt = buildString {
            append("You read school and college exam timetables. Today is $today. ")
            append("From the text and images provided, list every exam. ")
            append("Reply with ONLY a JSON array and nothing else. Each item is ")
            append("{\"subject\":\"subject name\",\"title\":\"short exam name such as Mid-term\",\"date\":\"YYYY-MM-DD\",\"room\":\"room or empty\"}. ")
            if (subjectNames.isNotEmpty()) {
                append("The student's subjects are: ${subjectNames.joinToString(", ")}. Use these exact names when an exam clearly is one of them. ")
            }
            append("If a date has no year, use the next occurrence on or after today. If there are none, reply [].")
            if (pastedText.isNotEmpty()) append("\n\nText:\n").append(pastedText)
        }
        return parseExams(client.generate(connection, prompt, images))
    }

    companion object {
        private const val MAX_ITEMS = 100

        /** Reads the JSON array out of a model reply, tolerating code fences. Items without a valid date or subject are dropped. */
        fun parseExams(reply: String): List<ExamCandidate> {
            val from = reply.indexOf('[')
            val to = reply.lastIndexOf(']')
            if (from < 0 || to <= from) throw AiException("The AI did not return a list of exams. Try again.")
            val array = try {
                JSONArray(reply.substring(from, to + 1))
            } catch (e: JSONException) {
                throw AiException("The AI's reply could not be read as a list. Try again.")
            }
            val found = ArrayList<ExamCandidate>()
            for (i in 0 until minOf(array.length(), MAX_ITEMS)) {
                val item = array.optJSONObject(i) ?: continue
                val subject = item.optString("subject").trim().take(80)
                val date = try {
                    LocalDate.parse(item.optString("date").trim())
                } catch (e: java.time.format.DateTimeParseException) {
                    null
                } ?: continue
                if (subject.isEmpty()) continue
                val title = item.optString("title").trim().ifEmpty { "Exam" }.take(80)
                found += ExamCandidate(subject, title, date, item.optString("room").trim().ifEmpty { null }?.take(40))
            }
            return found.distinct().sortedBy { it.date }
        }
    }
}
