package com.trajectory.vault.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trajectory.vault.AppContainer
import com.trajectory.vault.data.calendar.CalendarData
import com.trajectory.vault.data.db.AttendanceStatus
import com.trajectory.vault.data.db.ExamEntity
import com.trajectory.vault.data.db.HolidayEntity
import com.trajectory.vault.data.db.TimetableEntity
import com.trajectory.vault.logging.DebugLogger
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

    fun addHoliday(date: LocalDate, title: String, autoSkip: Boolean) =
        perform("add the holiday") { repository.addHoliday(date, title, autoSkip) }

    fun deleteHoliday(holiday: HolidayEntity) = perform("delete the holiday") { repository.deleteHoliday(holiday) }

    fun addExam(
        subjectId: Long,
        date: LocalDate,
        title: String,
        weightage: Double?,
        room: String?,
        syllabusNotes: String?,
    ) = perform("add the exam") { repository.addExam(subjectId, date, title, weightage, room, syllabusNotes) }

    fun deleteExam(exam: ExamEntity) = perform("delete the exam") { repository.deleteExam(exam) }

    fun addSlot(day: DayOfWeek, subjectId: Long, start: LocalTime, end: LocalTime, room: String?) =
        perform("add the lecture") { repository.addSlot(day, subjectId, start, end, room) }

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
