package skywa1ker404.absencium.ui.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
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
import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.theme.pillBackground

/** The details of one lecture: when it is, where, who teaches it and how attendance for its subject stands. Opened from Home's Next up card. */
@Composable
fun LectureDetailSheet(
    date: LocalDate,
    lecture: Lecture,
    now: LocalDateTime,
    attendance: SubjectAttendance?,
    threshold: Int,
    later: List<Pair<LocalDate, Lecture>>,
    onOpenLecture: (LocalDate, Lecture) -> Unit,
    onDismiss: () -> Unit,
    onOpenTimetable: () -> Unit,
) {
    var showLater by rememberSaveable { mutableStateOf(false) }
    val goal = lecture.subject.attendanceGoal ?: threshold
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
                        if (percent == null) "No lectures held yet" else "${"%.0f".format(percent)}% (${attendance.present} of ${attendance.held}), goal $goal%",
                    )
                }
            }
            if (later.isNotEmpty()) {
                AppCard(modifier = Modifier.fillMaxWidth(), onClick = { showLater = !showLater }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Next lectures", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text("${later.size}", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                        Icon(if (showLater) AppIcons.ExpandLess else AppIcons.ExpandMore, contentDescription = if (showLater) "Hide" else "Show", tint = colors.onSurfaceVariant)
                    }
                    AnimatedVisibility(visible = showLater) {
                        Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            later.forEach { (laterDate, next) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { onOpenLecture(laterDate, next) },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Box(modifier = Modifier.size(10.dp).pillBackground(subjectColor(next.subject.id)))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(next.subject.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            "${daysLabel(now.toLocalDate(), laterDate)}, ${next.slot.startTime.hm()}" + (next.slot.roomNumber?.takeIf { it.isNotBlank() }?.let { " \u00b7 Room $it" } ?: ""),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
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
