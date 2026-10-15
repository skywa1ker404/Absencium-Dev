package skywa1ker404.absencium.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import skywa1ker404.absencium.AppContainer
import skywa1ker404.absencium.data.calendar.CalendarData
import skywa1ker404.absencium.data.db.AttendanceStatus
import skywa1ker404.absencium.data.db.ExamEntity
import skywa1ker404.absencium.data.db.HolidayEntity
import skywa1ker404.absencium.data.db.Priority
import skywa1ker404.absencium.data.db.TimetableEntity
import skywa1ker404.absencium.logging.DebugLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class CalendarViewModel(container: AppContainer) : ViewModel() {

    private val repository = container.calendarRepository

    /** Null until the first read from the database finishes. */
    val data: StateFlow<CalendarData?> = repository.data
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    fun setAttendance(subjectId: Long, date: LocalDate, status: AttendanceStatus) =
        perform("log attendance") { repository.setAttendance(subjectId, date, status) }

    fun markDay(subjectIds: Collection<Long>, date: LocalDate, status: AttendanceStatus) =
        perform("log attendance") { repository.markDay(subjectIds, date, status) }

    fun addHoliday(date: LocalDate, title: String, autoSkip: Boolean, priority: Priority) =
        perform("add the holiday") { repository.addHoliday(date, title, autoSkip, priority) }

    fun addHolidayRange(start: LocalDate, end: LocalDate, title: String, autoSkip: Boolean, priority: Priority) {
        viewModelScope.launch {
            try {
                val (added, skipped) = repository.addHolidayRange(start, end, title, autoSkip, priority)
                if (skipped > 0) {
                    _message.value = "Added $added ${if (added == 1) "day" else "days"}. " +
                        "$skipped ${if (skipped == 1) "day" else "days"} already had a holiday and were left as they are."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DebugLogger.e("Calendar", "Could not add the holiday", e)
                _message.value = "Could not add the holiday. See debug.log for details."
            }
        }
    }

    fun setNotesCollected(subjectIds: Collection<Long>, date: LocalDate, collected: Boolean) =
        perform("update the notes status") { repository.setNotesCollected(subjectIds, date, collected) }

    fun deleteHoliday(holiday: HolidayEntity) = perform("delete the holiday") { repository.deleteHoliday(holiday) }

    fun addExam(
        subjectId: Long,
        date: LocalDate,
        title: String,
        weightage: Double?,
        room: String?,
        syllabusNotes: String?,
        priority: Priority,
    ) = perform("add the exam") {
        repository.addExam(subjectId, date, title, weightage, room, syllabusNotes, priority)
    }

    fun deleteExam(exam: ExamEntity) = perform("delete the exam") { repository.deleteExam(exam) }

    fun updateExam(exam: ExamEntity) = perform("save the exam") { repository.updateExam(exam) }

    fun updateHoliday(holiday: HolidayEntity) = perform("save the holiday") {
        if (!repository.updateHoliday(holiday)) {
            _message.value = "That day already has a holiday, so the change was not saved."
        }
    }

    fun updateSlot(slot: TimetableEntity) = perform("save the lecture") { repository.updateSlot(slot) }

    fun addSlot(day: DayOfWeek, subjectId: Long, start: LocalTime, end: LocalTime, room: String?) =
        perform("add the lecture") { repository.addSlot(day, subjectId, start, end, room) }

    fun addSlots(days: Collection<DayOfWeek>, subjectId: Long, start: LocalTime, end: LocalTime, room: String?) =
        perform("add the lectures") { repository.addSlots(days, subjectId, start, end, room) }

    fun deleteSlot(slot: TimetableEntity) = perform("delete the lecture") { repository.deleteSlot(slot) }

    private fun perform(action: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DebugLogger.e("Calendar", "Could not $action", e)
                _message.value = "Could not $action. See debug.log for details."
            }
        }
    }
}
