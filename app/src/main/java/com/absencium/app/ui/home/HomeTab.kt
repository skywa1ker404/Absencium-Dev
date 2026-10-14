package com.absencium.app.ui.home

import com.absencium.app.ui.theme.LocalQuality
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.absencium.app.data.calendar.CalendarData
import com.absencium.app.data.db.ExamEntity
import com.absencium.app.data.db.Priority
import com.absencium.app.data.settings.AppSettings
import com.absencium.app.domain.Margin
import com.absencium.app.domain.SubjectAttendance
import com.absencium.app.domain.attendanceMargin
import com.absencium.app.domain.hm
import com.absencium.app.domain.nextLecture
import com.absencium.app.domain.overall
import com.absencium.app.domain.pretty
import com.absencium.app.domain.subjectAttendance
import com.absencium.app.ui.calendar.daysLabel
import com.absencium.app.ui.calendar.label
import com.absencium.app.ui.calendar.subjectColor
import com.absencium.app.ui.components.AppCard
import com.absencium.app.ui.components.PillButton
import com.absencium.app.ui.components.RingGauge
import com.absencium.app.ui.components.TileGrid
import com.absencium.app.ui.components.SectionTitle
import com.absencium.app.ui.main.TabColumn
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Subjects within this many points above the threshold are shown as "approaching". */
private const val APPROACHING_POINTS = 5

private fun greeting(hour: Int): String = when {
    hour < 5 -> "Burning the midnight oil"
    hour < 12 -> "Good morning"
    hour < 17 -> "Good afternoon"
    else -> "Good evening"
}

/** Exams whose date starts within 48 hours (or is today) and that are marked High priority. */
private fun crammable(exams: List<ExamEntity>, now: LocalDateTime): List<ExamEntity> =
    exams.filter { exam ->
        exam.priority == Priority.HIGH &&
            !exam.date.isBefore(now.toLocalDate()) &&
            Duration.between(now, exam.date.atStartOfDay()).toHours() <= 48
    }.sortedBy { it.date }

@Composable
fun HomeTab(
    data: CalendarData,
    settings: AppSettings,
    now: LocalDateTime,
    contentPadding: PaddingValues,
    onOpenExam: (ExamEntity) -> Unit,
    onManageSubjects: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val today = now.toLocalDate()
    val threshold = settings.attendanceThresholdPercent
    val perSubject = remember(data) { data.subjectAttendance() }
    val overall = remember(perSubject) { perSubject.overall() }
    val subjectNames = remember(data.subjects) { data.subjects.associate { it.id to it.name } }
    val cram = remember(data.exams, now) { crammable(data.exams, now) }
    val upcoming = remember(data.exams, today) { data.exams.filter { !it.date.isBefore(today) }.sortedBy { it.date }.take(5) }
    val next = remember(data, now) { data.nextLecture(now) }

    TabColumn(contentPadding) {
        Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
            val name = settings.displayName.trim()
            Text(
                text = if (name.isEmpty()) greeting(now.hour) else "${greeting(now.hour)}, $name",
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = today.pretty(),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }

        // Adaptive: the emergency banner only appears when a High priority exam is 48 hours away or less.
        cram.forEach { exam ->
            val hours = Duration.between(now, exam.date.atStartOfDay()).toHours()
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                container = colors.errorContainer,
                onClick = { onOpenExam(exam) },
            ) {
                Text("Exam coming up", style = MaterialTheme.typography.titleMedium, color = colors.onErrorContainer)
                Text(
                    text = "${exam.title} (${subjectNames[exam.subjectId] ?: "Unknown subject"}) " +
                        if (hours <= 0) "is today." else "starts in about $hours hours.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onErrorContainer,
                )
                Text(
                    text = "Tap for the exam details.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onErrorContainer.copy(alpha = 0.75f),
                )
            }
        }

        if (data.subjects.none { it.isActive }) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Text("Add your subjects", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Attendance, the timetable and exams all start from your subject list.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                PillButton(text = "Add subjects", onClick = onManageSubjects)
            }
        } else {
            AttendanceCard(overallPercent = overall.percent, present = overall.present, held = overall.held, threshold = threshold)

            val watch = perSubject.filter { s -> s.percent?.let { it < threshold + APPROACHING_POINTS } == true }
            if (watch.isNotEmpty()) {
                SectionTitle("Watch these subjects")
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        watch.forEach { WatchRow(it, threshold) }
                    }
                }
            }
        }

        SectionTitle("Next up")
        AppCard(modifier = Modifier.fillMaxWidth(), container = colors.secondaryContainer) {
            if (next == null) {
                Text("No upcoming lectures", style = MaterialTheme.typography.titleMedium, color = colors.onSecondaryContainer)
                Text(
                    "Add lectures to your timetable from the Attendance tab.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSecondaryContainer.copy(alpha = 0.75f),
                )
            } else {
                val (date, lecture) = next
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(subjectColor(lecture.subject.id)),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(lecture.subject.name, style = MaterialTheme.typography.titleLarge, color = colors.onSecondaryContainer)
                        val room = lecture.slot.roomNumber?.takeIf { it.isNotBlank() }
                        Text(
                            text = "${daysLabel(today, date)}, ${lecture.slot.startTime.hm()} to ${lecture.slot.endTime.hm()}" +
                                (if (room != null) " · Room $room" else ""),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSecondaryContainer.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }

        if (upcoming.isNotEmpty()) {
            SectionTitle("Exam countdown")
            TileGrid(upcoming) { index, exam ->
                // Each tile takes a tonal colour from the theme; a High priority exam is always the warm one.
                val (container, onContainer) = when {
                    exam.priority == Priority.HIGH -> colors.errorContainer to colors.onErrorContainer
                    index % 3 == 0 -> colors.primaryContainer to colors.onPrimaryContainer
                    index % 3 == 1 -> colors.tertiaryContainer to colors.onTertiaryContainer
                    else -> colors.secondaryContainer to colors.onSecondaryContainer
                }
                AppCard(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    container = container,
                    onClick = { onOpenExam(exam) },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(subjectColor(exam.subjectId)))
                        Text(
                            text = subjectNames[exam.subjectId] ?: "Unknown subject",
                            style = MaterialTheme.typography.labelMedium,
                            color = onContainer.copy(alpha = 0.8f),
                            maxLines = 1,
                        )
                    }
                    Text(
                        text = exam.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = onContainer,
                        maxLines = 2,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Text(
                        text = daysLabel(today, exam.date),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                        color = onContainer,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                    Text(
                        text = "${exam.date.pretty()} · ${exam.priority.label()} priority",
                        style = MaterialTheme.typography.labelSmall,
                        color = onContainer.copy(alpha = 0.75f),
                    )
                }
            }
        }
    }
}

/**
 * The bunk buffer, in one compact row: the attendance ring on the left; on the right the buffer headline, the
 * sentence that explains it, and the Present / Absent / Total counts. About 55 to 60 percent shorter than the
 * earlier stacked layout, with every number still on it.
 */
@Composable
private fun AttendanceCard(overallPercent: Double?, present: Int, held: Int, threshold: Int) {
    val colors = MaterialTheme.colorScheme
    val below = overallPercent != null && overallPercent < threshold
    val container = if (below) colors.errorContainer else colors.primaryContainer
    val onContainer = if (below) colors.onErrorContainer else colors.onPrimaryContainer
    AppCard(modifier = Modifier.fillMaxWidth(), container = container) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            RingGauge(
                percent = overallPercent,
                threshold = threshold,
                arcColor = colors.primary,
                trackColor = onContainer.copy(alpha = 0.14f),
                belowColor = colors.onErrorContainer,
                markerColor = onContainer,
                numberColor = onContainer,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (overallPercent == null) {
                    Text("Bunk buffer", style = MaterialTheme.typography.titleMedium, color = onContainer)
                    Text(
                        "Log a lecture on the Attendance tab to see how many you can safely miss.",
                        style = MaterialTheme.typography.bodySmall,
                        color = onContainer.copy(alpha = 0.8f),
                    )
                } else {
                    val margin = attendanceMargin(present, held, threshold)
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = marginHeadline(margin),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                            color = onContainer,
                        )
                        Text(
                            text = "bunk buffer",
                            style = MaterialTheme.typography.labelMedium,
                            color = onContainer.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                    Text(
                        text = marginSentence(margin, threshold),
                        style = MaterialTheme.typography.bodySmall,
                        color = onContainer.copy(alpha = 0.8f),
                        maxLines = 2,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                    CompactPill("Present $present", colors.primary, colors.onPrimary)
                    CompactPill(
                        "Absent ${(held - present).coerceAtLeast(0)}",
                        if (below) colors.error else colors.errorContainer,
                        if (below) colors.onError else colors.onErrorContainer,
                    )
                    CompactPill("Total $held", colors.surface, colors.onSurface)
                }
            }
        }
    }
}

@Composable
private fun CompactPill(label: String, container: androidx.compose.ui.graphics.Color, content: androidx.compose.ui.graphics.Color) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = content,
        maxLines = 1,
        modifier = Modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 9.dp, vertical = 4.dp),
    )
}

private fun marginHeadline(margin: Margin): String =
    if (margin.mustAttend > 0) "Attend ${margin.mustAttend}" else "Skip ${margin.canSkip}"

private fun marginSentence(margin: Margin, threshold: Int): String =
    if (margin.mustAttend > 0) {
        "Attend the next ${margin.mustAttend} lectures in a row to get back to $threshold%."
    } else {
        "You can miss ${margin.canSkip} more lectures and stay at or above $threshold%."
    }

@Composable
private fun WatchRow(item: SubjectAttendance, threshold: Int) {
    val colors = MaterialTheme.colorScheme
    val margin = attendanceMargin(item.present, item.held, threshold)
    val percent = item.percent ?: 0.0
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(subjectColor(item.subject.id)))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.subject.name, style = MaterialTheme.typography.titleSmall)
            Text(
                text = marginSentence(margin, threshold),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
        Text(
            text = "${percent.roundToInt()}%",
            style = MaterialTheme.typography.titleMedium,
            color = if (percent < threshold) colors.error else colors.tertiary,
        )
    }
}
