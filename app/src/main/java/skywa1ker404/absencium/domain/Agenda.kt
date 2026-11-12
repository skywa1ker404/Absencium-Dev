package skywa1ker404.absencium.domain

import skywa1ker404.absencium.data.calendar.CalendarData
import skywa1ker404.absencium.data.db.EventEntity
import skywa1ker404.absencium.data.db.EventRepeat
import skywa1ker404.absencium.data.db.StudyGoalEntity
import java.time.LocalDate
import java.time.LocalTime

enum class AgendaKind { LECTURE, EXAM, EVENT, HOLIDAY, TODO }

/** One line of a day's agenda. A to-do carries its [goalId] so it can be ticked from the agenda. */
data class AgendaEntry(
    val kind: AgendaKind,
    val date: LocalDate,
    val start: LocalTime?,
    val end: LocalTime?,
    val title: String,
    val detail: String,
    val goalId: Long? = null,
    val done: Boolean = false,
)

/** Everything on [date], with timed entries in order and all-day entries (holidays, exams, to-dos, events without a time) first. */
fun agendaFor(
    date: LocalDate,
    data: CalendarData,
    events: List<EventEntity>,
    goals: List<StudyGoalEntity>,
): List<AgendaEntry> {
    val names = data.subjects.associate { it.id to it.name }
    val out = ArrayList<AgendaEntry>()
    data.holidays.filter { it.deletedAt == null && it.date == date }.forEach {
        out += AgendaEntry(AgendaKind.HOLIDAY, date, null, null, it.title, "Holiday")
    }
    data.lecturesOn(date).forEach { lecture ->
        val slot = lecture.slot
        val kind = slot.type.name.lowercase().replaceFirstChar { it.uppercase() }
        val room = slot.roomNumber?.takeIf { it.isNotBlank() }?.let { "Room $it" }
        out += AgendaEntry(AgendaKind.LECTURE, date, slot.startTime, slot.endTime, lecture.subject.name, listOfNotNull(kind, room).joinToString(" · "))
    }
    data.exams.filter { it.deletedAt == null && it.date == date }.forEach {
        out += AgendaEntry(AgendaKind.EXAM, date, null, null, "${names[it.subjectId] ?: "Exam"}: ${it.title}", listOfNotNull("Exam", it.room?.takeIf { r -> r.isNotBlank() }?.let { r -> "Room $r" }).joinToString(" · "))
    }
    events.forEach { e ->
        if (e.occurrences(date, date).isNotEmpty()) {
            val repeats = if (e.repeat != EventRepeat.NONE) "repeats ${e.repeat.name.lowercase()}" else null
            out += AgendaEntry(AgendaKind.EVENT, date, e.startTime, e.endTime, e.title, listOfNotNull(e.category, e.subjectId?.let { names[it] }, repeats).joinToString(" · "))
        }
    }
    goals.filter { it.targetDate == date }.forEach {
        out += AgendaEntry(AgendaKind.TODO, date, null, null, it.title, listOfNotNull("To-do", it.subjectId?.let { id -> names[id] }).joinToString(" · "), it.id, it.done)
    }
    return out.sortedWith(compareBy({ it.start != null }, { it.start ?: LocalTime.MIN }))
}
