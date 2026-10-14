package com.trajectory.vault.data.calendar

import com.trajectory.vault.data.db.AttendanceEntity
import com.trajectory.vault.data.db.AttendanceStatus
import com.trajectory.vault.data.db.ExamEntity
import com.trajectory.vault.data.db.HolidayEntity
import com.trajectory.vault.data.db.Priority
import com.trajectory.vault.data.db.SubjectEntity
import com.trajectory.vault.data.db.TimetableEntity
import com.trajectory.vault.data.db.TrajectoryDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** The longest holiday range that can be added in one go. */
private const val MAX_HOLIDAY_DAYS = 366

/** Everything the Calendar and Home screens need, read from Room in one snapshot. */
data class CalendarData(
    val subjects: List<SubjectEntity> = emptyList(),
    val timetable: List<TimetableEntity> = emptyList(),
    val holidays: List<HolidayEntity> = emptyList(),
    val attendance: List<AttendanceEntity> = emptyList(),
    val exams: List<ExamEntity> = emptyList(),
    /** From Settings: Saturdays and Sundays that are always holidays. */
    val saturdayHoliday: Boolean = true,
    val sundayHoliday: Boolean = true,
)

/** Offline-first source of truth for the timetable, attendance, holidays and exams. Room is read as a Flow. */
class CalendarRepository(
    private val database: TrajectoryDatabase,
    private val settings: kotlinx.coroutines.flow.Flow<com.trajectory.vault.data.settings.AppSettings>,
) {

    val data: Flow<CalendarData> = combine(
        database.subjectDao().observeAll(),
        database.timetableDao().observeAll(),
        database.holidayDao().observeAll(),
        database.attendanceDao().observeAll(),
        database.examDao().observeAll(),
        settings.map { it.saturdayHoliday to it.sundayHoliday }.distinctUntilChanged(),
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val weekend = values[5] as Pair<Boolean, Boolean>
        CalendarData(
            subjects = values[0] as List<SubjectEntity>,
            timetable = values[1] as List<TimetableEntity>,
            holidays = values[2] as List<HolidayEntity>,
            attendance = values[3] as List<AttendanceEntity>,
            exams = values[4] as List<ExamEntity>,
            saturdayHoliday = weekend.first,
            sundayHoliday = weekend.second,
        )
    }

    /**
     * Logs [status] for a subject on a date. Tapping the status that is already logged clears it.
     * One record is kept per subject per day, because the attendance table has no lecture slot.
     */
    suspend fun setAttendance(subjectId: Long, date: LocalDate, status: AttendanceStatus) {
        val dao = database.attendanceDao()
        val existing = dao.getFor(subjectId, date)
        when {
            existing == null -> dao.insert(AttendanceEntity(subjectId = subjectId, date = date, status = status))
            existing.status == status -> dao.delete(existing)
            else -> dao.update(existing.copy(status = status, notesCollected = false))
        }
    }

    /** Sets [status] for every subject in [subjectIds] on [date], replacing any other status already logged. */
    suspend fun markDay(subjectIds: Collection<Long>, date: LocalDate, status: AttendanceStatus) {
        val dao = database.attendanceDao()
        for (id in subjectIds.distinct()) {
            val existing = dao.getFor(id, date)
            when {
                existing == null -> dao.insert(AttendanceEntity(subjectId = id, date = date, status = status))
                existing.status != status -> dao.update(existing.copy(status = status, notesCollected = false))
            }
        }
    }

    /** Marks whether the notes for an absent day have been collected, for each of [subjectIds]. */
    suspend fun setNotesCollected(subjectIds: Collection<Long>, date: LocalDate, collected: Boolean) {
        val dao = database.attendanceDao()
        for (id in subjectIds.distinct()) dao.setNotesCollected(id, date, collected)
    }

    suspend fun addHoliday(date: LocalDate, title: String, autoSkip: Boolean, priority: Priority) {
        database.holidayDao().insert(HolidayEntity(date = date, title = title, autoSkip = autoSkip, priority = priority))
    }

    /**
     * Adds one holiday row per day from [start] to [end] (inclusive). A day that already has a holiday
     * is left as it is. Returns how many days were added and how many already had a holiday.
     */
    suspend fun addHolidayRange(
        start: LocalDate,
        end: LocalDate,
        title: String,
        autoSkip: Boolean,
        priority: Priority,
    ): Pair<Int, Int> {
        val dao = database.holidayDao()
        var added = 0
        var skipped = 0
        var day = start
        while (!day.isAfter(end) && added + skipped < MAX_HOLIDAY_DAYS) {
            if (dao.getForDate(day) == null) {
                dao.insert(HolidayEntity(date = day, title = title, autoSkip = autoSkip, priority = priority))
                added++
            } else {
                skipped++
            }
            day = day.plusDays(1)
        }
        return added to skipped
    }

    suspend fun deleteHoliday(holiday: HolidayEntity) = database.holidayDao().delete(holiday)

    suspend fun addExam(
        subjectId: Long,
        date: LocalDate,
        title: String,
        weightage: Double?,
        room: String?,
        syllabusNotes: String?,
        priority: Priority,
    ) {
        database.examDao().insert(
            ExamEntity(
                subjectId = subjectId,
                date = date,
                title = title,
                weightage = weightage,
                room = room,
                syllabusNotes = syllabusNotes,
                priority = priority,
            ),
        )
    }

    suspend fun deleteExam(exam: ExamEntity) = database.examDao().delete(exam)

    suspend fun updateExam(exam: ExamEntity) = database.examDao().update(exam)

    /**
     * Saves changes to a holiday. Returns false, and changes nothing, when the new date already has a
     * different holiday, because a day can only have one.
     */
    suspend fun updateHoliday(holiday: HolidayEntity): Boolean {
        val dao = database.holidayDao()
        val other = dao.getForDate(holiday.date)
        if (other != null && other.id != holiday.id) return false
        dao.update(holiday)
        return true
    }

    suspend fun updateSlot(slot: TimetableEntity) = database.timetableDao().update(slot)

    /** Adds the same lecture to every weekday in [days] (the "same time every day" option). */
    suspend fun addSlots(days: Collection<DayOfWeek>, subjectId: Long, start: LocalTime, end: LocalTime, room: String?) {
        for (day in days) addSlot(day, subjectId, start, end, room)
    }

    suspend fun addSlot(
        day: DayOfWeek,
        subjectId: Long,
        start: LocalTime,
        end: LocalTime,
        room: String?,
    ) {
        database.timetableDao().insert(
            TimetableEntity(dayOfWeek = day, subjectId = subjectId, startTime = start, endTime = end, roomNumber = room),
        )
    }

    suspend fun deleteSlot(slot: TimetableEntity) = database.timetableDao().delete(slot)
}
