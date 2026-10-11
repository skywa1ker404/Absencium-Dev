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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Everything the Calendar and Home screens need, read from Room in one snapshot. */
data class CalendarData(
    val subjects: List<SubjectEntity> = emptyList(),
    val timetable: List<TimetableEntity> = emptyList(),
    val holidays: List<HolidayEntity> = emptyList(),
    val attendance: List<AttendanceEntity> = emptyList(),
    val exams: List<ExamEntity> = emptyList(),
)

/** Offline-first source of truth for the timetable, attendance, holidays and exams. Room is read as a Flow. */
class CalendarRepository(private val database: TrajectoryDatabase) {

    val data: Flow<CalendarData> = combine(
        database.subjectDao().observeAll(),
        database.timetableDao().observeAll(),
        database.holidayDao().observeAll(),
        database.attendanceDao().observeAll(),
        database.examDao().observeAll(),
    ) { subjects, timetable, holidays, attendance, exams ->
        CalendarData(subjects, timetable, holidays, attendance, exams)
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
            else -> dao.update(existing.copy(status = status))
        }
    }

    /** Sets [status] for every subject in [subjectIds] on [date], replacing any other status already logged. */
    suspend fun markDay(subjectIds: Collection<Long>, date: LocalDate, status: AttendanceStatus) {
        val dao = database.attendanceDao()
        for (id in subjectIds.distinct()) {
            val existing = dao.getFor(id, date)
            when {
                existing == null -> dao.insert(AttendanceEntity(subjectId = id, date = date, status = status))
                existing.status != status -> dao.update(existing.copy(status = status))
            }
        }
    }

    suspend fun addHoliday(date: LocalDate, title: String, autoSkip: Boolean, priority: Priority) {
        database.holidayDao().insert(HolidayEntity(date = date, title = title, autoSkip = autoSkip, priority = priority))
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
