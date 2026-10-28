package skywa1ker404.absencium.ui.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.time.LocalDate
import skywa1ker404.absencium.data.calendar.CalendarData
import skywa1ker404.absencium.domain.overall
import skywa1ker404.absencium.domain.subjectAttendance
import skywa1ker404.absencium.ui.components.PopupBody
import skywa1ker404.absencium.ui.components.PopupShell

/**
 * Today's attendance as a popup, opened from the Home screen: the overall figure, the Present / Absent switch for today and,
 * after an absence, the "did you collect today's notes?" list. The same card as on the Attendance tab.
 */
@Composable
fun AttendancePopup(
    data: CalendarData,
    today: LocalDate,
    viewModel: CalendarViewModel,
    onAddNotes: (subjectId: Long, date: LocalDate) -> Unit,
    onOpenAttendanceTab: () -> Unit,
    onDismiss: () -> Unit,
) {
    val overall = remember(data) { data.subjectAttendance().overall() }
    PopupShell(onDismissRequest = onDismiss, dismissOnOutsideTap = true) {
        PopupBody(title = "Log attendance", onClose = onDismiss) {
            TodayCard(data = data, today = today, overall = overall, viewModel = viewModel, onAddNotes = onAddNotes)
            skywa1ker404.absencium.ui.components.PillButton(text = "Open the calendar", tonal = true, onClick = onOpenAttendanceTab)
        }
    }
}
