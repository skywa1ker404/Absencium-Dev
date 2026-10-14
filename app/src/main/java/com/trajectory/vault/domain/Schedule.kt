package com.trajectory.vault.domain

import com.trajectory.vault.data.calendar.CalendarData
import com.trajectory.vault.data.db.HolidayEntity
import com.trajectory.vault.data.db.SubjectEntity
import com.trajectory.vault.data.db.TimetableEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** One timetable slot together with its subject. */
data class Lecture(val slot: TimetableEntity, val subject: SubjectEntity)

fun CalendarData.holidayOn(date: LocalDate): HolidayEntity? = holidays.firstOrNull { it.date == date }

/** True when [date] is a Saturday or Sunday that the user has chosen to treat as a holiday. */
fun CalendarData.isWeekendHoliday(date: LocalDate): Boolean = when (date.dayOfWeek) {
    DayOfWeek.SATURDAY -> saturdayHoliday
    DayOfWeek.SUNDAY -> sundayHoliday
    else -> false
}

/** The weekdays the timetable offers: Monday to Friday, plus a weekend day when it is not a holiday. */
fun CalendarData.timetableDays(): List<DayOfWeek> = DayOfWeek.entries.filter {
    it.value <= DayOfWeek.FRIDAY.value ||
        (it == DayOfWeek.SATURDAY && !saturdayHoliday) ||
        (it == DayOfWeek.SUNDAY && !sundayHoliday)
}

/**
 * The lectures expected on [date]. Holiday weekends and days with an auto-skip holiday have none.
 * Slots of inactive subjects are left out.
 */
fun CalendarData.lecturesOn(date: LocalDate): List<Lecture> {
    if (isWeekendHoliday(date)) return emptyList()
    val holiday = holidayOn(date)
    if (holiday != null && holiday.autoSkip) return emptyList()
    val activeSubjects = subjects.filter { it.isActive }.associateBy { it.id }
    return timetable
        .filter { it.dayOfWeek == date.dayOfWeek }
        .mapNotNull { slot -> activeSubjects[slot.subjectId]?.let { Lecture(slot, it) } }
        .sortedBy { it.slot.startTime }
}

/** The next lecture that has not finished yet, looking up to two weeks ahead. */
fun CalendarData.nextLecture(now: LocalDateTime): Pair<LocalDate, Lecture>? {
    for (offset in 0..14) {
        val date = now.toLocalDate().plusDays(offset.toLong())
        val lecture = lecturesOn(date)
            .firstOrNull { offset > 0 || it.slot.endTime.isAfter(now.toLocalTime()) }
        if (lecture != null) return date to lecture
    }
    return null
}

private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val TimeInputFormat = DateTimeFormatter.ofPattern("H:mm")
private val DayFormat = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())

fun LocalTime.hm(): String = format(TimeFormat)

fun LocalDate.pretty(): String = format(DayFormat)

/** Parses "9:30" or "09:30". Returns null if the text is not a valid time. */
fun parseTimeInput(text: String): LocalTime? =
    try {
        LocalTime.parse(text.trim(), TimeInputFormat)
    } catch (e: java.time.format.DateTimeParseException) {
        null
    }
