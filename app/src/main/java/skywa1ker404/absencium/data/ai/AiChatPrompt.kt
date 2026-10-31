package skywa1ker404.absencium.data.ai

import skywa1ker404.absencium.data.calendar.CalendarData
import skywa1ker404.absencium.data.db.AttendanceStatus
import skywa1ker404.absencium.data.db.NoteEntity
import java.time.LocalDate

/**
 * Builds what Absencium AI is sent with each chat message: who it is, which parts of the app it may use (only those the student
 * allowed), the app data of those parts, how to propose a change, and the conversation so far. Pure text, so it can be tested.
 */
object AiChatPrompt {
    private const val MAX_HISTORY_CHARS = 6_000
    private const val MAX_LINES = 40

    fun build(
        allowed: Set<AiScope>,
        calendar: CalendarData,
        notes: List<NoteEntity>,
        history: List<Pair<ChatRole, String>>,
        message: String,
        today: LocalDate = LocalDate.now(),
    ): String = buildString {
        append("You are Absencium AI, the study assistant inside Absencium, an offline student app. ")
        append("Answer clearly and briefly, in Markdown. Today is $today (${dayName(today.dayOfWeek)}).\n")
        if (allowed.isEmpty()) {
            append("You cannot see or change anything in the app. If asked to, say the student can allow it in Settings, AI & Integrations.\n")
        } else {
            append("The student allowed you to use these parts of the app: ${allowed.joinToString { it.label }}. Nothing else is available to you.\n\n")
            append(context(allowed, calendar, notes, today))
            append("\nTo propose a change, finish your answer with ONE fenced block labelled absencium-actions holding a JSON array, and only for allowed parts. ")
            append("The student sees each change as a card and decides; never say a change has been made. Use subject names exactly as listed. Dates are YYYY-MM-DD, times HH:mm, days MONDAY..SUNDAY. Types:\n")
            val types = buildList {
                if (AiScope.SUBJECTS in allowed) add("""{"type":"add_subject","name":""}""")
                if (AiScope.NOTES in allowed) add("""{"type":"add_note","subject":"","title":"","body":"markdown"}""")
                if (AiScope.TIMETABLE in allowed) {
                    add("""{"type":"add_timetable","day":"MONDAY","subject":"","start":"09:00","end":"10:00","room":""}""")
                    add("""{"type":"update_timetable","subject":"","day":"MONDAY","match_start":"09:00","new_day":"","start":"","end":"","room":""} (leave out what stays the same)""")
                }
                if (AiScope.EXAMS in allowed) {
                    add("""{"type":"add_exam","subject":"","date":"","title":"","room":"","weightage":0}""")
                    add("""{"type":"update_exam","match":"title or subject","title":"","date":"","room":""} (leave out what stays the same)""")
                }
                if (AiScope.HOLIDAYS in allowed) add("""{"type":"add_holiday","date":"","title":""}""")
                if (AiScope.ATTENDANCE in allowed) add("""{"type":"set_attendance","subject":"","date":"","status":"PRESENT|ABSENT|CANCELLED"}""")
            }
            types.forEach { append("- ").append(it).append('\n') }
            append("If a subject does not exist yet, add it first in the same array.\n")
        }
        if (history.isNotEmpty()) {
            append("\nConversation so far:\n")
            var used = 0
            val kept = ArrayList<Pair<ChatRole, String>>()
            for (item in history.asReversed()) {
                val length = minOf(item.second.length, 1_500)
                if (used + length > MAX_HISTORY_CHARS && kept.isNotEmpty()) break
                used += length
                kept += item
            }
            kept.reverse()
            kept.forEach { (role, text) -> append(if (role == ChatRole.USER) "Student: " else "Assistant: ").append(text.take(1_500)).append("\n") }
        }
        append("\nStudent: ").append(message)
    }

    fun context(allowed: Set<AiScope>, data: CalendarData, notes: List<NoteEntity>, today: LocalDate): String = buildString {
        val names = data.subjects.associate { it.id to it.name }
        append("APP DATA (read-only snapshot)\n")
        if (names.isNotEmpty()) append("Subjects: ").append(names.values.joinToString(", ")).append('\n')
        if (AiScope.TIMETABLE in allowed) {
            val lines = data.timetable.sortedWith(compareBy({ it.dayOfWeek }, { it.startTime })).take(MAX_LINES)
                .map { "${it.dayOfWeek} ${it.startTime}-${it.endTime} ${names[it.subjectId].orEmpty()}" + (it.roomNumber?.let { r -> " room $r" } ?: "") }
            if (lines.isNotEmpty()) append("Timetable:\n").append(lines.joinToString("\n") { "- $it" }).append('\n')
        }
        if (AiScope.EXAMS in allowed) {
            val lines = data.exams.filter { !it.date.isBefore(today) }.sortedBy { it.date }.take(MAX_LINES)
                .map { "${it.date} ${it.title} (${names[it.subjectId].orEmpty()})" + (it.room?.let { r -> " room $r" } ?: "") }
            if (lines.isNotEmpty()) append("Upcoming exams:\n").append(lines.joinToString("\n") { "- $it" }).append('\n')
        }
        if (AiScope.HOLIDAYS in allowed) {
            val lines = data.holidays.filter { !it.date.isBefore(today) }.sortedBy { it.date }.take(MAX_LINES).map { "${it.date} ${it.title}" }
            if (lines.isNotEmpty()) append("Upcoming holidays:\n").append(lines.joinToString("\n") { "- $it" }).append('\n')
        }
        if (AiScope.ATTENDANCE in allowed) {
            val lines = data.subjects.mapNotNull { s ->
                val rows = data.attendance.filter { it.subjectId == s.id }
                if (rows.isEmpty()) null else {
                    val present = rows.count { it.status == AttendanceStatus.PRESENT }
                    val absent = rows.count { it.status == AttendanceStatus.ABSENT }
                    "${s.name}: $present present, $absent absent"
                }
            }
            if (lines.isNotEmpty()) append("Attendance:\n").append(lines.joinToString("\n") { "- $it" }).append('\n')
        }
        if (AiScope.NOTES in allowed) {
            val lines = notes.filter { it.deletedAt == null }.sortedByDescending { it.updatedAt }.take(MAX_LINES)
                .map { "${names[it.subjectId].orEmpty()}: ${it.title}" }
            if (lines.isNotEmpty()) append("Recent note titles:\n").append(lines.joinToString("\n") { "- $it" }).append('\n')
        }
    }
}
