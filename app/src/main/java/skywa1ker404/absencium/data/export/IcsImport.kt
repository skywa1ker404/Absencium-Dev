package skywa1ker404.absencium.data.export

import skywa1ker404.absencium.data.db.EventEntity
import skywa1ker404.absencium.data.db.EventRepeat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/** One event read from a calendar file, before it is turned into one of the student's own events. */
data class IcsEvent(
    val title: String,
    val date: LocalDate,
    val start: LocalTime?,
    val end: LocalTime?,
    val notes: String?,
    val repeat: EventRepeat,
)

/**
 * Reads the events of an iCalendar (.ics) file on this phone. Only what Absencium's own events can hold is kept: title, day, start and
 * end time, a note (the location and description) and a daily, weekly or monthly repeat. Times written in UTC (a trailing Z) are
 * shown in this phone's time zone; times with a named zone are taken as written. Nothing is read from the network.
 */
object IcsImport {
    const val MAX_BYTES = 2_000_000
    const val MAX_EVENTS = 500

    fun parse(text: String, zone: ZoneId = ZoneId.systemDefault()): List<IcsEvent> {
        val out = ArrayList<IcsEvent>()
        var props: MutableMap<String, Pair<Map<String, String>, String>>? = null
        for (line in unfold(text)) {
            when {
                line.equals("BEGIN:VEVENT", ignoreCase = true) -> props = HashMap()
                line.equals("END:VEVENT", ignoreCase = true) -> {
                    props?.let { p -> toEvent(p, zone)?.let { out.add(it) } }
                    props = null
                    if (out.size >= MAX_EVENTS) return out
                }
                props != null -> {
                    val colon = line.indexOf(':')
                    if (colon > 0) {
                        val head = line.substring(0, colon).split(';')
                        val name = head[0].uppercase()
                        val params = head.drop(1).mapNotNull { it.split('=', limit = 2).takeIf { kv -> kv.size == 2 }?.let { kv -> kv[0].uppercase() to kv[1] } }.toMap()
                        // The first value wins, so a repeated property cannot replace the real one.
                        if (name !in props) props[name] = params to line.substring(colon + 1)
                    }
                }
            }
        }
        return out
    }

    /** Joins the lines the format wraps (a continuation line starts with a space or a tab). */
    internal fun unfold(text: String): List<String> {
        val out = ArrayList<String>()
        for (raw in text.replace("\r\n", "\n").replace('\r', '\n').split('\n')) {
            if ((raw.startsWith(" ") || raw.startsWith("\t")) && out.isNotEmpty()) out[out.lastIndex] = out.last() + raw.substring(1)
            else if (raw.isNotEmpty()) out.add(raw)
        }
        return out
    }

    private fun toEvent(p: Map<String, Pair<Map<String, String>, String>>, zone: ZoneId): IcsEvent? {
        val (startParams, startValue) = p["DTSTART"] ?: return null
        val start = parseTime(startValue, startParams, zone) ?: return null
        val end = p["DTEND"]?.let { (params, v) -> parseTime(v, params, zone) }
        val title = p["SUMMARY"]?.second?.let(::unescape)?.trim().orEmpty().ifBlank { "Event" }
        val where = p["LOCATION"]?.second?.let(::unescape)?.trim().orEmpty()
        val about = p["DESCRIPTION"]?.second?.let(::unescape)?.trim().orEmpty()
        val notes = listOf(where.takeIf { it.isNotEmpty() }?.let { "Location: $it" }, about.takeIf { it.isNotEmpty() })
            .filterNotNull().joinToString("\n").ifBlank { null }
        val date = start.first
        val startTime = start.second
        // An end on another day (an overnight event) is cut to the start's day rather than invented.
        val endTime = if (startTime != null && end != null && end.first == date && end.second != null && end.second!!.isAfter(startTime)) end.second else null
        val rule = p["RRULE"]?.second.orEmpty().uppercase()
        val repeat = when {
            "FREQ=DAILY" in rule -> EventRepeat.DAILY
            "FREQ=WEEKLY" in rule -> EventRepeat.WEEKLY
            "FREQ=MONTHLY" in rule -> EventRepeat.MONTHLY
            else -> EventRepeat.NONE
        }
        return IcsEvent(title, date, startTime, endTime, notes, repeat)
    }

    /** A day (DATE value, all-day) or a day with a time. Returns null for anything that is not a valid date. */
    internal fun parseTime(value: String, params: Map<String, String>, zone: ZoneId): Pair<LocalDate, LocalTime?>? = try {
        val v = value.trim()
        if (params["VALUE"].equals("DATE", true) || (v.length == 8 && v.all { it.isDigit() })) {
            LocalDate.of(v.substring(0, 4).toInt(), v.substring(4, 6).toInt(), v.substring(6, 8).toInt()) to null
        } else {
            val utc = v.endsWith("Z", ignoreCase = true)
            val digits = v.trimEnd('Z', 'z')
            require(digits.length >= 15 && digits[8] == 'T')
            var at = LocalDateTime.of(
                digits.substring(0, 4).toInt(), digits.substring(4, 6).toInt(), digits.substring(6, 8).toInt(),
                digits.substring(9, 11).toInt(), digits.substring(11, 13).toInt(), digits.substring(13, 15).toInt(),
            )
            if (utc) at = at.atOffset(ZoneOffset.UTC).atZoneSameInstant(zone).toLocalDateTime()
            at.toLocalDate() to at.toLocalTime().withSecond(0).withNano(0)
        }
    } catch (e: Exception) {
        null
    }

    private fun unescape(text: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == '\\' && i + 1 < text.length) {
                when (val n = text[i + 1]) {
                    'n', 'N' -> sb.append('\n')
                    else -> sb.append(n)
                }
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    /** The events that are not already among [existing] (same title, day and start), as the student's own events. */
    fun newEvents(found: List<IcsEvent>, existing: List<EventEntity>): List<EventEntity> {
        val known = existing.map { Triple(it.title.trim().lowercase(), it.date, it.startTime) }.toMutableSet()
        return found.mapNotNull { e ->
            val key = Triple(e.title.trim().lowercase(), e.date, e.start)
            if (!known.add(key)) null
            else EventEntity(title = e.title, date = e.date, startTime = e.start, endTime = e.end, notes = e.notes, repeat = e.repeat)
        }
    }
}
