package skywa1ker404.absencium.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import skywa1ker404.absencium.domain.Lecture
import skywa1ker404.absencium.domain.SubjectAttendance
import skywa1ker404.absencium.domain.hm
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.components.PopupBody
import skywa1ker404.absencium.ui.components.PopupShell
import skywa1ker404.absencium.ui.theme.pillBackground

/** The details of one lecture: when it is, where, who teaches it and how attendance for its subject stands. Opened from Home's Next up card. */
@Composable
fun LectureDetailSheet(
    date: LocalDate,
    lecture: Lecture,
    now: LocalDateTime,
    attendance: SubjectAttendance?,
    threshold: Int,
    onDismiss: () -> Unit,
    onOpenTimetable: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val slot = lecture.slot
    val minutes = Duration.between(slot.startTime, slot.endTime).toMinutes()
    val start = date.atTime(slot.startTime)
    val end = date.atTime(slot.endTime)
    val status = when {
        now.isBefore(start) -> {
            val until = Duration.between(now, start).toMinutes()
            when {
                until < 60 -> "Starts in $until min"
                until < 24 * 60 -> "Starts in ${until / 60} h ${until % 60} min"
                else -> daysLabel(now.toLocalDate(), date)
            }
        }
        now.isBefore(end) -> "In progress, ${Duration.between(now, end).toMinutes()} min left"
        else -> "Finished"
    }
    PopupShell(onDismissRequest = onDismiss, dismissOnOutsideTap = true) {
        PopupBody(title = "Lecture", onClose = onDismiss) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(modifier = Modifier.size(14.dp).pillBackground(subjectColor(lecture.subject.id)))
                Column(modifier = Modifier.weight(1f)) {
                    Text(lecture.subject.name, style = MaterialTheme.typography.titleLarge)
                    Text(status, style = MaterialTheme.typography.bodyMedium, color = colors.primary)
                }
            }
            AppCard(modifier = Modifier.fillMaxWidth()) {
                DetailLine("When", "${daysLabel(now.toLocalDate(), date)}, ${slot.startTime.hm()} to ${slot.endTime.hm()} (${minutes / 60} h ${minutes % 60} min)")
                slot.roomNumber?.takeIf { it.isNotBlank() }?.let { DetailLine("Room", it) }
                slot.teacher?.takeIf { it.isNotBlank() }?.let { DetailLine("Teacher", it) }
                DetailLine("Type", slot.type.name.lowercase().replaceFirstChar { it.uppercase() })
            }
            if (attendance != null) {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    val percent = attendance.percent
                    DetailLine(
                        "Attendance",
                        if (percent == null) "No lectures held yet" else "${"%.0f".format(percent)}% (${attendance.present} of ${attendance.held}), goal $threshold%",
                    )
                }
            }
            PillButton(text = "Open the timetable", tonal = true, onClick = onOpenTimetable, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
