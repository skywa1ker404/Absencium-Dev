package skywa1ker404.absencium.data.ai

import org.json.JSONArray
import org.json.JSONObject
import skywa1ker404.absencium.data.db.AttendanceStatus
import skywa1ker404.absencium.data.db.SyllabusStatus
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

    /** Asks to read a note or a study material file. Only after the student approves is its content sent to the AI, in the next message. */
    data class ReadContent(val material: Boolean, val subject: String, val title: String) : AiAction {
        override val scope = if (material) AiScope.MATERIAL else AiScope.NOTES
        override fun describe() = "Read the ${if (material) "study material" else "note"} \"$title\" in $subject and send it to the AI"
    }

    data class AddSubject(val name: String) : AiAction {
        override val scope = AiScope.SUBJECTS
        override fun describe() = "Add the subject \"$name\""
    }

    /** A new note. When [body] is empty the app's note writer writes it from [brief] after the student applies it, at the given [length]. */
    data class AddNote(val subject: String, val title: String, val body: String, val brief: String = "", val length: NoteLength = NoteLength.LENGTHY) : AiAction {
        override val scope = AiScope.NOTES
        override fun describe() = if (body.isBlank()) {
            "Write the note \"$title\" in $subject (${length.label.lowercase()}, written by the note writer)"
        } else {
            "Add the note \"$title\" to $subject"
        }
    }

    /** Something durable to remember about the student. Saved by the chat itself (when Memory is allowed), never shown as a card. */
    data class Remember(val text: String, val chatOnly: Boolean = false) : AiAction {
        override val scope = AiScope.MEMORY
        override fun describe() = "Remember: $text"
    }

    /** Marks the syllabus topic whose title is [topic] (inside [subject]) as not started, in progress or completed. */
    data class SetSyllabusStatus(val subject: String, val topic: String, val status: SyllabusStatus) : AiAction {
        override val scope = AiScope.SYLLABUS
        override fun describe() = "Mark \"$topic\" in $subject as ${status.name.lowercase().replace('_', ' ')}"
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
data class ParsedReply(val text: String, val json: String?, val actions: List<AiAction>, val remembered: List<AiAction.Remember> = emptyList())

/**
 * Finds the changes the assistant proposed and takes them out of the answer text. Models do not always follow the requested form, so any
 * fenced block that holds a JSON list of known changes counts (labelled absencium-actions, json, or labelled by a first line inside the
 * fence), and so does a bare list at the very end. Blocks labelled absencium-actions are always hidden, even when they cannot be read.
 * Anything that is not understood is dropped, never guessed.
 */
object AiActionParser {
    private const val LABEL = "absencium-actions"
    private val fence = Regex("```[ \\t]*([A-Za-z0-9_-]*)[ \\t]*\\r?\\n(.*?)(```|\\z)", RegexOption.DOT_MATCHES_ALL)

    fun split(reply: String): ParsedReply {
        var text = reply
        val found = ArrayList<JSONArray>()
        for (match in fence.findAll(reply).toList().asReversed()) {
            var body = match.groupValues[2].trim()
            var labelled = match.groupValues[1].equals(LABEL, ignoreCase = true)
            if (body.lineSequence().firstOrNull()?.trim().equals(LABEL, ignoreCase = true)) {
                labelled = true
                body = body.substringAfter('\n', "").trim()
            }
            val array = toArray(body)
            if (array == null || parse(array.toString()).isEmpty()) {
                if (labelled) text = text.removeRange(match.range)
                continue
            }
            found += array
            text = text.removeRange(match.range)
        }
        found.reverse()
        if (found.isEmpty()) {
            // A bare list at the very end of the answer, with no fence around it.
            val start = reply.lastIndexOf("[{").takeIf { it >= 0 } ?: reply.lastIndexOf("[\n{")
            val end = reply.lastIndexOf(']')
            if (start >= 0 && end > start && reply.substring(end + 1).isBlank()) {
                val array = toArray(reply.substring(start, end + 1))
                if (array != null && parse(array.toString()).isNotEmpty()) {
                    found += array
                    text = reply.substring(0, start)
                }
            }
        }
        // Memories are kept apart from the cards: they are saved by the chat, so they never need a card.
        val cards = JSONArray()
        val remembered = ArrayList<AiAction.Remember>()
        for (a in found) {
            for (i in 0 until a.length()) {
                val o = a.optJSONObject(i) ?: continue
                when (val action = runCatching { one(o) }.getOrNull()) {
                    null -> Unit
                    is AiAction.Remember -> remembered += action
                    else -> cards.put(o)
                }
            }
        }
        val json = if (cards.length() > 0) cards.toString() else null
        text = text.replace(Regex("\n{3,}"), "\n\n").trim()
        return ParsedReply(text, json, if (json == null) emptyList() else parse(json), remembered)
    }

    private fun toArray(body: String): JSONArray? = try {
        when {
            body.startsWith("[") -> JSONArray(body)
            body.startsWith("{") -> JSONArray().put(JSONObject(body))
            else -> null
        }
    } catch (e: Exception) {
        null
    }

    fun parse(json: String): List<AiAction> {
        val array = toArray(json.trim()) ?: return emptyList()
        return (0 until array.length()).mapNotNull { i -> array.optJSONObject(i)?.let { runCatching { one(it) }.getOrNull() } }
    }

    private fun one(o: JSONObject): AiAction? {
        fun str(key: String) = o.optString(key).trim().takeIf { it.isNotEmpty() && it != "null" }
        return when (str("type")?.lowercase()) {
            "add_subject" -> AiAction.AddSubject(str("name") ?: return null)
            "read_content" -> AiAction.ReadContent(
                material = str("kind")?.lowercase()?.startsWith("mat") == true || str("kind")?.lowercase()?.startsWith("study") == true,
                subject = str("subject") ?: return null,
                title = str("title") ?: return null,
            )
            "remember" -> AiAction.Remember(str("text") ?: return null, o.optBoolean("chat_only"))
            "add_note" -> AiAction.AddNote(
                str("subject") ?: return null, str("title") ?: return null, o.optString("body").let { if (it == "null") "" else it },
                str("brief") ?: "", noteLength(str("length")),
            )
            "set_syllabus_status" -> AiAction.SetSyllabusStatus(
                str("subject") ?: return null, str("topic") ?: return null,
                runCatching { SyllabusStatus.valueOf(str("status")!!.uppercase().replace(' ', '_')) }.getOrNull() ?: return null,
            )
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

    fun noteLength(text: String?): NoteLength = when (text?.lowercase()?.replace(' ', '_')) {
        "moderate", "short" -> NoteLength.MODERATE
        "in_depth", "indepth", "deep", "detailed" -> NoteLength.IN_DEPTH
        else -> NoteLength.LENGTHY
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
