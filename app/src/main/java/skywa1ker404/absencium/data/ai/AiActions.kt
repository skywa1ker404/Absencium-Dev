package skywa1ker404.absencium.data.ai

import org.json.JSONArray
import org.json.JSONObject
import skywa1ker404.absencium.data.db.AttendanceStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

/** One change Absencium AI proposed. Nothing happens until the student applies it. */
sealed interface AiAction {
    val scope: AiScope

    /** One plain sentence that says exactly what will change. */
    fun describe(): String

    data class AddSubject(val name: String) : AiAction {
        override val scope = AiScope.SUBJECTS
        override fun describe() = "Add the subject \"$name\""
    }

    data class AddNote(val subject: String, val title: String, val body: String) : AiAction {
        override val scope = AiScope.NOTES
        override fun describe() = "Add the note \"$title\" to $subject"
    }

    data class AddSlot(val day: DayOfWeek, val subject: String, val start: LocalTime, val end: LocalTime, val room: String?) : AiAction {
        override val scope = AiScope.TIMETABLE
        override fun describe() = "Add $subject on ${dayName(day)}, $start to $end" + (room?.let { ", room $it" } ?: "")
    }

    /** Moves or edits the lecture of [subject] on [day] (the one starting at [matchStart] when the subject has several that day). */
    data class UpdateSlot(
        val subject: String,
        val day: DayOfWeek,
        val matchStart: LocalTime?,
        val newDay: DayOfWeek?,
        val newStart: LocalTime?,
        val newEnd: LocalTime?,
        val room: String?,
    ) : AiAction {
        override val scope = AiScope.TIMETABLE
        override fun describe() = buildString {
            append("Change $subject on ${dayName(day)}")
            matchStart?.let { append(" at $it") }
            newDay?.let { append(" to ${dayName(it)}") }
            if (newStart != null || newEnd != null) append(" at ${newStart ?: "same start"} to ${newEnd ?: "same end"}")
            room?.let { append(", room $it") }
        }
    }

    data class AddExam(val subject: String, val date: LocalDate, val title: String, val room: String?, val weightage: Double?) : AiAction {
        override val scope = AiScope.EXAMS
        override fun describe() = "Add the exam \"$title\" for $subject on $date" + (room?.let { ", room $it" } ?: "")
    }

    /** Edits the exam whose title or subject contains [match]. */
    data class UpdateExam(val match: String, val title: String?, val date: LocalDate?, val room: String?) : AiAction {
        override val scope = AiScope.EXAMS
        override fun describe() = buildString {
            append("Change the exam \"$match\"")
            title?.let { append(", rename to \"$it\"") }
            date?.let { append(", move to $it") }
            room?.let { append(", room $it") }
        }
    }

    data class AddHoliday(val date: LocalDate, val title: String) : AiAction {
        override val scope = AiScope.HOLIDAYS
        override fun describe() = "Add the holiday \"$title\" on $date"
    }

    data class SetAttendance(val subject: String, val date: LocalDate, val status: AttendanceStatus) : AiAction {
        override val scope = AiScope.ATTENDANCE
        override fun describe() = "Mark $subject ${status.name.lowercase()} on $date"
    }
}

/** The weekday written out in the phone's language. */
fun dayName(day: DayOfWeek): String = day.getDisplayName(TextStyle.FULL, Locale.getDefault())

/** The assistant's answer with its proposed changes taken out. [json] is kept so the cards can be shown again later. */
data class ParsedReply(val text: String, val json: String?, val actions: List<AiAction>)

/**
 * Finds the block of changes the assistant was told to append to its answer, a fenced block labelled `absencium-actions`
 * holding a JSON array. Anything that is not understood is dropped, never guessed.
 */
object AiActionParser {
    private const val FENCE = "```absencium-actions"

    fun split(reply: String): ParsedReply {
        val start = reply.lastIndexOf(FENCE)
        if (start < 0) return ParsedReply(reply.trim(), null, emptyList())
        val bodyStart = start + FENCE.length
        val end = reply.indexOf("```", bodyStart)
        val json = (if (end < 0) reply.substring(bodyStart) else reply.substring(bodyStart, end)).trim()
        val text = (reply.substring(0, start) + (if (end < 0) "" else reply.substring(end + 3))).trim()
        return ParsedReply(text, json, parse(json))
    }

    fun parse(json: String): List<AiAction> {
        val array = try {
            JSONArray(json)
        } catch (e: Exception) {
            return emptyList()
        }
        return (0 until array.length()).mapNotNull { i -> array.optJSONObject(i)?.let { runCatching { one(it) }.getOrNull() } }
    }

    private fun one(o: JSONObject): AiAction? {
        fun str(key: String) = o.optString(key).trim().takeIf { it.isNotEmpty() && it != "null" }
        return when (str("type")?.lowercase()) {
            "add_subject" -> AiAction.AddSubject(str("name") ?: return null)
            "add_note" -> AiAction.AddNote(str("subject") ?: return null, str("title") ?: return null, o.optString("body"))
            "add_timetable" -> AiAction.AddSlot(
                day(str("day")) ?: return null, str("subject") ?: return null,
                time(str("start")) ?: return null, time(str("end")) ?: return null, str("room"),
            )
            "update_timetable" -> AiAction.UpdateSlot(
                str("subject") ?: return null, day(str("day")) ?: return null, time(str("match_start")),
                day(str("new_day")), time(str("start")), time(str("end")), str("room"),
            )
            "add_exam" -> AiAction.AddExam(
                str("subject") ?: return null, date(str("date")) ?: return null, str("title") ?: return null,
                str("room"), if (o.has("weightage")) o.optDouble("weightage").takeIf { !it.isNaN() && it > 0 } else null,
            )
            "update_exam" -> AiAction.UpdateExam(str("match") ?: return null, str("title"), date(str("date")), str("room"))
                .takeIf { it.title != null || it.date != null || it.room != null }
            "add_holiday" -> AiAction.AddHoliday(date(str("date")) ?: return null, str("title") ?: return null)
            "set_attendance" -> AiAction.SetAttendance(
                str("subject") ?: return null, date(str("date")) ?: return null,
                runCatching { AttendanceStatus.valueOf(str("status")!!.uppercase()) }.getOrNull() ?: return null,
            )
            else -> null
        }
    }

    fun day(text: String?): DayOfWeek? {
        val t = text?.trim()?.lowercase() ?: return null
        return DayOfWeek.entries.firstOrNull { it.name.lowercase() == t || it.name.lowercase().startsWith(t.take(3)) && t.length >= 3 }
    }

    fun date(text: String?): LocalDate? = text?.trim()?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    /** Accepts 9:00, 09:00, 9:00 am and 14:30. */
    fun time(text: String?): LocalTime? {
        val t = text?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
        val pm = t.endsWith("pm")
        val am = t.endsWith("am")
        val digits = t.removeSuffix("am").removeSuffix("pm").trim()
        val parts = digits.split(':')
        var hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        if (pm && hour < 12) hour += 12
        if (am && hour == 12) hour = 0
        return if (hour in 0..23 && minute in 0..59) LocalTime.of(hour, minute) else null
    }
}
