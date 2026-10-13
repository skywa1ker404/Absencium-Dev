package com.trajectory.vault.ui.home

import com.trajectory.vault.ui.theme.LocalQuality
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
import com.trajectory.vault.data.calendar.CalendarData
import com.trajectory.vault.data.db.ExamEntity
import com.trajectory.vault.data.db.Priority
import com.trajectory.vault.data.settings.AppSettings
import com.trajectory.vault.domain.Margin
import com.trajectory.vault.domain.SubjectAttendance
import com.trajectory.vault.domain.attendanceMargin
import com.trajectory.vault.domain.hm
import com.trajectory.vault.domain.nextLecture
import com.trajectory.vault.domain.overall
import com.trajectory.vault.domain.pretty
import com.trajectory.vault.domain.subjectAttendance
import com.trajectory.vault.ui.calendar.daysLabel
import com.trajectory.vault.ui.calendar.label
import com.trajectory.vault.ui.calendar.subjectColor
import com.trajectory.vault.ui.components.AppCard
import com.trajectory.vault.ui.components.PillButton
import com.trajectory.vault.ui.components.SemiGauge
import com.trajectory.vault.ui.components.StatPill
import com.trajectory.vault.ui.components.TileGrid
import com.trajectory.vault.ui.components.SectionTitle
import com.trajectory.vault.ui.main.TabColumn
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

@Composable
private fun AttendanceCard(overallPercent: Double?, present: Int, held: Int, threshold: Int) {
    val colors = MaterialTheme.colorScheme
    val below = overallPercent != null && overallPercent < threshold
    val container = if (below) colors.errorContainer else colors.primaryContainer
    val onContainer = if (below) colors.onErrorContainer else colors.onPrimaryContainer
    AppCard(modifier = Modifier.fillMaxWidth(), container = container) {
        SemiGauge(
            percent = overallPercent,
            threshold = threshold,
            arcColor = if (below) colors.onErrorContainer else colors.primary,
            trackColor = onContainer.copy(alpha = 0.14f),
            belowColor = colors.onErrorContainer,
            markerColor = onContainer,
            numberColor = onContainer,
            captionColor = onContainer.copy(alpha = 0.7f),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            StatPill("Present: $present", colors.primary, colors.onPrimary)
            StatPill("Absent: ${(held - present).coerceAtLeast(0)}", colors.errorContainer.takeIf { !below } ?: colors.error, if (below) colors.onError else colors.onErrorContainer)
            StatPill("Total: $held", colors.surface, colors.onSurface)
        }
        Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Bunk buffer", style = MaterialTheme.typography.titleMedium, color = onContainer)
            if (overallPercent == null) {
                Text(
                    "Log a lecture on the Attendance tab to see how many you can safely miss.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = onContainer.copy(alpha = 0.8f),
                )
            } else {
                val margin = attendanceMargin(present, held, threshold)
                Text(
                    text = marginHeadline(margin),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                    color = onContainer,
                )
                Text(
                    text = marginSentence(margin, threshold),
                    style = MaterialTheme.typography.bodySmall,
                    color = onContainer.copy(alpha = 0.8f),
                )
            }
        }
    }
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
