package skywa1ker404.absencium.ui.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import skywa1ker404.absencium.data.calendar.CalendarData
import skywa1ker404.absencium.data.db.ExamEntity
import skywa1ker404.absencium.domain.Lecture
import skywa1ker404.absencium.domain.hm
import skywa1ker404.absencium.domain.holidayOn
import skywa1ker404.absencium.domain.lecturesOn
import skywa1ker404.absencium.domain.pretty
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.components.PopupBody
import skywa1ker404.absencium.ui.components.PopupShell
import skywa1ker404.absencium.ui.components.SectionTitle
import skywa1ker404.absencium.ui.theme.pillBackground

/**
 * Lectures, exams and holidays of the coming days on one timeline, one block per day that has something. Starts with two weeks and
 * shows two more each time "Show more" is pressed. Tapping a lecture or an exam opens its details.
 */
@Composable
fun TimelinePopup(
    data: CalendarData,
    today: LocalDate,
    onOpenLecture: (LocalDate, Lecture) -> Unit,
    onOpenExam: (ExamEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    var days by remember { mutableIntStateOf(14) }
    PopupShell(onDismissRequest = onDismiss, dismissOnOutsideTap = true) {
        PopupBody(title = "Coming up", onClose = onDismiss) {
            val colors = MaterialTheme.colorScheme
            val names = remember(data) { data.subjects.associate { it.id to it.name } }
            var shown = 0
            for (offset in 0 until days) {
                val date = today.plusDays(offset.toLong())
                val holiday = data.holidayOn(date)
                val exams = data.exams.filter { it.date == date }
                val lectures = data.lecturesOn(date)
                if (holiday == null && exams.isEmpty() && lectures.isEmpty()) continue
                shown++
                SectionTitle(if (offset == 0) "Today" else if (offset == 1) "Tomorrow" else date.pretty())
                if (holiday != null) {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Text(holiday.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (holiday.autoSkip) "Holiday, lectures are skipped" else "Holiday, lectures still count",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
                exams.forEach { exam ->
                    AppCard(modifier = Modifier.fillMaxWidth(), highlighted = true, onClick = { onOpenExam(exam) }) {
                        Text(exam.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            names[exam.subjectId].orEmpty() + (exam.room?.takeIf { it.isNotBlank() }?.let { " · Room $it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
                if (lectures.isNotEmpty()) {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            lectures.forEach { lecture ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { onOpenLecture(date, lecture) },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Box(modifier = Modifier.size(10.dp).pillBackground(subjectColor(lecture.subject.id)))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(lecture.subject.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            "${lecture.slot.startTime.hm()} to ${lecture.slot.endTime.hm()}" +
                                                (lecture.slot.roomNumber?.takeIf { it.isNotBlank() }?.let { " · Room $it" } ?: ""),
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
            if (shown == 0) HintText("Nothing is scheduled in the next $days days.", modifier = Modifier.padding(top = 4.dp))
            PillButton(text = "Show more", tonal = true, onClick = { days += 14 }, modifier = Modifier.fillMaxWidth())
        }
    }
}
