package skywa1ker404.absencium.domain

import skywa1ker404.absencium.data.calendar.CalendarData
import skywa1ker404.absencium.data.settings.AppSettings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class ReminderKind { LECTURE, EXAM, EVENT, LOW_ATTENDANCE }

/** One notification to post at [at]. [text] of a low-attendance alert is worked out when it fires. */
data class Reminder(val key: String, val kind: ReminderKind, val at: LocalDateTime, val title: String, val text: String)

private val EXAM_HOUR = LocalTime.of(8, 0)
private val LOW_ATTENDANCE_HOUR = LocalTime.of(18, 0)

/**
 * The next reminder after [now] across everything the student turned on, or null when nothing is on or due.
 * One alarm is kept for this one reminder; when it fires the next is worked out again, so nothing polls.
 */
fun nextReminder(data: CalendarData, settings: AppSettings, now: LocalDateTime, events: List<skywa1ker404.absencium.data.db.EventEntity> = emptyList()): Reminder? {
    val found = ArrayList<Reminder>()
    val today = now.toLocalDate()

    if (settings.remindLectureMinutes > 0) {
        for (offset in 0L..7L) {
            val date = today.plusDays(offset)
            data.lecturesOn(date).forEach { lecture ->
                val at = date.atTime(lecture.slot.startTime).minusMinutes(settings.remindLectureMinutes.toLong())
                if (at.isAfter(now)) {
                    val room = lecture.slot.roomNumber?.takeIf { it.isNotBlank() }?.let { " in room $it" }.orEmpty()
                    found += Reminder(
                        key = "lecture-${lecture.slot.id}-$date", kind = ReminderKind.LECTURE, at = at,
                        title = lecture.subject.name,
                        text = "Starts at ${lecture.slot.startTime.hm()}$room",
                    )
                }
            }
        }
    }

    if (settings.remindExamDays > 0) {
        val names = data.subjects.associate { it.id to it.name }
        data.exams.filter { it.deletedAt == null }.forEach { exam ->
            val at = exam.date.minusDays(settings.remindExamDays.toLong()).atTime(EXAM_HOUR)
            if (at.isAfter(now) && !exam.date.isBefore(today)) {
                found += Reminder(
                    key = "exam-${exam.id}", kind = ReminderKind.EXAM, at = at,
                    title = "${names[exam.subjectId] ?: "Exam"}: ${exam.title}",
                    text = if (settings.remindExamDays == 1) "Tomorrow" else "In ${settings.remindExamDays} days, ${exam.date.pretty()}",
                )
            }
        }
    }

    // The student's own events carry their own reminder; a repeating one is looked at over the next 60 days.
    events.filter { it.remindMinutes != null }.forEach { event ->
        event.occurrences(today, today.plusDays(60)).forEach { date ->
            val start = event.startTime ?: EXAM_HOUR
            val at = date.atTime(start).minusMinutes(if (event.startTime == null) 0 else event.remindMinutes!!.toLong())
            if (at.isAfter(now)) {
                found += Reminder(
                    key = "event-${event.id}-$date", kind = ReminderKind.EVENT, at = at,
                    title = event.title,
                    text = listOfNotNull(event.category, event.startTime?.hm()).joinToString(" · "),
                )
            }
        }
    }

    if (settings.remindLowAttendance) {
        var date: LocalDate = today
        if (!now.isBefore(date.atTime(LOW_ATTENDANCE_HOUR))) date = date.plusDays(1)
        found += Reminder("low-$date", ReminderKind.LOW_ATTENDANCE, date.atTime(LOW_ATTENDANCE_HOUR), "Attendance", "")
    }
    return found.minByOrNull { it.at }
}

/** The alert for subjects under their goal, or null when every subject is fine. */
fun lowAttendanceText(data: CalendarData, settings: AppSettings): String? {
    val low = data.subjectAttendance().filter { item ->
        val p = item.percent
        p != null && p < (item.subject.attendanceGoal ?: settings.attendanceThresholdPercent)
    }
    if (low.isEmpty()) return null
    return low.joinToString(", ", limit = 3, truncated = "and more") { it.subject.name } + " " + (if (low.size == 1) "is" else "are") + " under the attendance goal"
}
