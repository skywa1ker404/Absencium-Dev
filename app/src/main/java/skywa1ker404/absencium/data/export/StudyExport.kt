package skywa1ker404.absencium.data.export

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import skywa1ker404.absencium.data.calendar.CalendarData
import skywa1ker404.absencium.data.db.AttendanceStatus
import skywa1ker404.absencium.data.share.PreparedShare
import skywa1ker404.absencium.domain.attendanceMargin
import skywa1ker404.absencium.domain.subjectAttendance
import skywa1ker404.absencium.domain.subjectGrades
import java.io.File
import java.io.IOException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

enum class StudyExportKind(val label: String, val extension: String, val mime: String) {
    CALENDAR_ICS("Calendar (.ics)", "ics", "text/calendar"),
    ATTENDANCE_CSV("Attendance (.csv)", "csv", "text/csv"),
    MARKS_CSV("Marks (.csv)", "csv", "text/csv"),
    REPORT_PDF("Report (PDF)", "pdf", "application/pdf"),
}

/**
 * Writes the student's timetable, exams, attendance and marks to a file in the cache and hands back a FileProvider URI for
 * the Sharesheet. Everything is written here with the platform APIs only (no library, no network).
 */
class StudyExport(private val context: Context) {

    suspend fun prepare(kind: StudyExportKind, data: CalendarData, thresholdPercent: Int, today: LocalDate = LocalDate.now()): Result<PreparedShare> =
        withContext(Dispatchers.IO) {
            try {
                val dir = File(context.cacheDir, "share_exports").apply { if (!exists() && !mkdirs()) throw IOException("Could not create a folder for the export.") }
                val name = "Absencium " + when (kind) {
                    StudyExportKind.CALENDAR_ICS -> "calendar"
                    StudyExportKind.ATTENDANCE_CSV -> "attendance"
                    StudyExportKind.MARKS_CSV -> "marks"
                    StudyExportKind.REPORT_PDF -> "report"
                } + "." + kind.extension
                val file = File(dir, name)
                when (kind) {
                    StudyExportKind.CALENDAR_ICS -> file.writeText(ics(data, today), Charsets.UTF_8)
                    StudyExportKind.ATTENDANCE_CSV -> file.writeText(attendanceCsv(data), Charsets.UTF_8)
                    StudyExportKind.MARKS_CSV -> file.writeText(marksCsv(data), Charsets.UTF_8)
                    StudyExportKind.REPORT_PDF -> file.outputStream().use { pdf(it, data, thresholdPercent, today) }
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                Result.success(PreparedShare(uri, kind.mime, name, null))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ---- .ics -------------------------------------------------------------------------------------------------

    internal fun ics(data: CalendarData, today: LocalDate): String {
        val names = data.subjects.associate { it.id to it.name }
        val stamp = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"))
        val day = DateTimeFormatter.BASIC_ISO_DATE
        val dateTime = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
        val out = StringBuilder()
        fun line(text: String) {
            // Lines are folded at 75 octets (RFC 5545 section 3.1); a continuation line starts with a space.
            var rest = text
            var first = true
            while (rest.toByteArray(Charsets.UTF_8).size > 73) {
                var cut = 73
                while (rest.substring(0, cut).toByteArray(Charsets.UTF_8).size > 73) cut--
                out.append(if (first) "" else " ").append(rest, 0, cut).append("\r\n")
                rest = rest.substring(cut)
                first = false
            }
            out.append(if (first) "" else " ").append(rest).append("\r\n")
        }
        fun esc(text: String) = text.replace("\\", "\\\\").replace(";", "\;").replace(",", "\\,").replace("\n", "\\n")
        fun byDay(d: DayOfWeek) = d.name.take(2)

        line("BEGIN:VCALENDAR")
        line("VERSION:2.0")
        line("PRODID:-//Absencium//Study export//EN")
        line("CALSCALE:GREGORIAN")
        data.timetable.filter { it.deletedAt == null }.forEach { slot ->
            // The first occurrence on or after today; the event then repeats every week. Times have no zone, so they stay local.
            var start = today
            while (start.dayOfWeek != slot.dayOfWeek) start = start.plusDays(1)
            val subject = names[slot.subjectId] ?: "Lecture"
            line("BEGIN:VEVENT")
            line("UID:${slot.uuid}@absencium")
            line("DTSTAMP:$stamp")
            line("DTSTART:${start.atTime(slot.startTime).format(dateTime)}")
            line("DTEND:${start.atTime(slot.endTime).format(dateTime)}")
            line("RRULE:FREQ=WEEKLY;BYDAY=${byDay(slot.dayOfWeek)}")
            line("SUMMARY:${esc(subject)}")
            slot.roomNumber?.takeIf { it.isNotBlank() }?.let { line("LOCATION:${esc("Room $it")}") }
            line("DESCRIPTION:${esc(slot.type.name.lowercase().replaceFirstChar { it.uppercase() })}")
            line("END:VEVENT")
        }
        data.exams.filter { it.deletedAt == null }.forEach { exam ->
            line("BEGIN:VEVENT")
            line("UID:${exam.uuid}@absencium")
            line("DTSTAMP:$stamp")
            line("DTSTART;VALUE=DATE:${exam.date.format(day)}")
            line("DTEND;VALUE=DATE:${exam.date.plusDays(1).format(day)}")
            line("SUMMARY:${esc("${names[exam.subjectId] ?: "Exam"}: ${exam.title}")}")
            exam.room?.takeIf { it.isNotBlank() }?.let { line("LOCATION:${esc("Room $it")}") }
            exam.syllabusNotes?.takeIf { it.isNotBlank() }?.let { line("DESCRIPTION:${esc(it)}") }
            line("END:VEVENT")
        }
        data.holidays.filter { it.deletedAt == null }.forEach { holiday ->
            line("BEGIN:VEVENT")
            line("UID:${holiday.uuid.ifBlank { UUID.randomUUID().toString() }}@absencium")
            line("DTSTAMP:$stamp")
            line("DTSTART;VALUE=DATE:${holiday.date.format(day)}")
            line("DTEND;VALUE=DATE:${holiday.date.plusDays(1).format(day)}")
            line("SUMMARY:${esc(holiday.title)}")
            line("TRANSP:TRANSPARENT")
            line("END:VEVENT")
        }
        line("END:VCALENDAR")
        return out.toString()
    }

    // ---- CSV --------------------------------------------------------------------------------------------------

    private fun csv(vararg cells: String): String =
        cells.joinToString(",") { c ->
            // A cell that starts with = + - @ would run as a formula in a spreadsheet, so it gets a leading apostrophe.
            val safe = if (c.isNotEmpty() && c[0] in "=+-@") "'$c" else c
            if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
        } + "\r\n"

    internal fun attendanceCsv(data: CalendarData): String {
        val names = data.subjects.associate { it.id to it.name }
        val out = StringBuilder(csv("Date", "Subject", "Status", "Notes"))
        data.attendance.sortedWith(compareBy({ it.date }, { names[it.subjectId].orEmpty() })).forEach {
            out.append(csv(it.date.toString(), names[it.subjectId].orEmpty(), it.status.name.lowercase(), it.notes.orEmpty()))
        }
        return out.toString()
    }

    internal fun marksCsv(data: CalendarData): String {
        val names = data.subjects.associate { it.id to it.name }
        val out = StringBuilder(csv("Subject", "Exam", "Date", "Marks", "Out of", "Percent", "Weightage"))
        data.exams.filter { it.deletedAt == null }.sortedWith(compareBy({ names[it.subjectId].orEmpty() }, { it.date })).forEach {
            val percent = if (it.marksObtained != null && (it.maxMarks ?: 0.0) > 0) String.format(Locale.US, "%.1f", it.marksObtained / it.maxMarks!! * 100) else ""
            out.append(csv(names[it.subjectId].orEmpty(), it.title, it.date.toString(), it.marksObtained?.toString().orEmpty(), it.maxMarks?.toString().orEmpty(), percent, it.weightage?.toString().orEmpty()))
        }
        return out.toString()
    }

    // ---- PDF --------------------------------------------------------------------------------------------------

    private fun pdf(out: java.io.OutputStream, data: CalendarData, threshold: Int, today: LocalDate) {
        val doc = PdfDocument()
        val width = 595
        val height = 842
        val margin = 40f
        val title = Paint().apply { textSize = 20f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        val head = Paint().apply { textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        val body = Paint().apply { textSize = 11f; isAntiAlias = true }
        val muted = Paint().apply { textSize = 9f; color = 0xFF666666.toInt(); isAntiAlias = true }
        var pageNumber = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(width, height, pageNumber).create())
        var canvas = page.canvas
        var y = margin + 20f

        fun newPage() {
            doc.finishPage(page)
            pageNumber++
            page = doc.startPage(PdfDocument.PageInfo.Builder(width, height, pageNumber).create())
            canvas = page.canvas
            y = margin
        }
        fun ensure(room: Float) { if (y + room > height - margin) newPage() }
        // Columns: name, then four right-aligned figures.
        val xs = floatArrayOf(margin, 300f, 355f, 410f, 470f, 535f)
        fun row(cells: List<String>, paint: Paint) {
            ensure(18f)
            canvas.drawText(cells[0].take(34), xs[0], y, paint)
            for (i in 1 until cells.size) {
                val w = paint.measureText(cells[i])
                canvas.drawText(cells[i], xs[i] + 40f - w, y, paint)
            }
            y += 16f
        }

        canvas.drawText("Absencium report", margin, y, title)
        y += 16f
        canvas.drawText("Made on ${today.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))} · attendance goal $threshold%", margin, y, muted)
        y += 26f

        val rows = data.subjectAttendance()
        canvas.drawText("Attendance", margin, y, head); y += 18f
        row(listOf("Subject", "Present", "Absent", "Cancelled", "Percent", "To goal"), head)
        rows.forEach { item ->
            val goal = item.subject.attendanceGoal ?: threshold
            val m = attendanceMargin(item.present, item.held, goal)
            row(
                listOf(
                    item.subject.name, "${item.present}", "${item.absent}", "${item.cancelled}",
                    item.percent?.let { String.format(Locale.US, "%.1f%%", it) } ?: "–",
                    if (item.held == 0) "–" else if (m.mustAttend > 0) "attend ${m.mustAttend}" else "skip ${m.canSkip}",
                ),
                body,
            )
        }
        val overallHeld = rows.sumOf { it.held }
        if (overallHeld > 0) {
            y += 4f
            canvas.drawText("Overall " + String.format(Locale.US, "%.1f%%", rows.sumOf { it.present } * 100.0 / overallHeld) + " over $overallHeld lectures held. Cancelled and excused lectures are not counted.", margin, y, muted)
            y += 22f
        }

        val grades = data.subjectGrades().filter { it.percent != null }
        if (grades.isNotEmpty()) {
            ensure(60f)
            canvas.drawText("Marks", margin, y, head); y += 18f
            row(listOf("Subject", "Graded", "Percent", "Pass %", "Result", ""), head)
            grades.forEach { g ->
                row(
                    listOf(g.subject.name, "${g.graded}", String.format(Locale.US, "%.1f%%", g.percent), String.format(Locale.US, "%.0f%%", g.subject.passMark), if (g.passing == true) "pass" else "below", ""),
                    body,
                )
            }
            y += 8f
        }

        val upcoming = data.exams.filter { it.deletedAt == null && !it.date.isBefore(today) }.sortedBy { it.date }
        if (upcoming.isNotEmpty()) {
            ensure(50f)
            canvas.drawText("Upcoming exams", margin, y, head); y += 18f
            val names = data.subjects.associate { it.id to it.name }
            upcoming.forEach {
                ensure(16f)
                canvas.drawText("${it.date}  ${names[it.subjectId].orEmpty()} – ${it.title}", margin, y, body)
                y += 16f
            }
        }
        doc.finishPage(page)
        try { doc.writeTo(out) } finally { doc.close() }
    }
}
