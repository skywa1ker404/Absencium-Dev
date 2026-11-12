package skywa1ker404.absencium.domain

import skywa1ker404.absencium.data.db.EventEntity
import skywa1ker404.absencium.data.db.EventRepeat
import java.time.LocalDate

/** The days from [from] to [to] (both included) on which this event happens, counting its repeats. */
fun EventEntity.occurrences(from: LocalDate, to: LocalDate): List<LocalDate> {
    if (to.isBefore(from)) return emptyList()
    return when (repeat) {
        EventRepeat.NONE -> if (date in from..to) listOf(date) else emptyList()
        EventRepeat.DAILY -> {
            val first = if (date.isAfter(from)) date else from
            generateSequence(first) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.toList()
        }
        EventRepeat.WEEKLY -> generateSequence(date) { it.plusWeeks(1) }
            .dropWhile { it.isBefore(from) }.takeWhile { !it.isAfter(to) }.toList()
        // Counted from the first day, so a 31st does not slide to the 28th for good after one short month.
        EventRepeat.MONTHLY -> generateSequence(0L) { it + 1 }.map { date.plusMonths(it) }
            .dropWhile { it.isBefore(from) }.takeWhile { !it.isAfter(to) }.toList()
    }
}

val EventCategories = listOf("Assignment", "Test", "Event", "Meeting", "Birthday", "Trip", "Other")
