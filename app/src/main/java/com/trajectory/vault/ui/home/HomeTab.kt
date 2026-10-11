package com.trajectory.vault.ui.home

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    val perSubject = data.subjectAttendance()
    val overall = perSubject.overall()
    val subjectNames = data.subjects.associate { it.id to it.name }
    val cram = crammable(data.exams, now)
    val upcoming = data.exams.filter { !it.date.isBefore(today) }.sortedBy { it.date }.take(5)
    val next = data.nextLecture(now)

    TabColumn(contentPadding) {
        Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
            val name = settings.displayName.trim()
            Text(
                text = if (name.isEmpty()) greeting(now.hour) else "${greeting(now.hour)}, $name",
                style = MaterialTheme.typography.headlineMedium,
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
            AppCard(modifier = Modifier.fillMaxWidth(), highlighted = true, onClick = { onOpenExam(exam) }) {
                Text("Exam coming up", style = MaterialTheme.typography.titleMedium, color = colors.error)
                Text(
                    text = "${exam.title} (${subjectNames[exam.subjectId] ?: "Unknown subject"}) " +
                        if (hours <= 0) "is today." else "starts in about $hours hours.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Tap for the exam details.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
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
        AppCard(modifier = Modifier.fillMaxWidth()) {
            if (next == null) {
                Text("No upcoming lectures", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Add lectures to your timetable from the Attends tab.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
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
                        Text(lecture.subject.name, style = MaterialTheme.typography.titleMedium)
                        val room = lecture.slot.roomNumber?.takeIf { it.isNotBlank() }
                        Text(
                            text = "${daysLabel(today, date)}, ${lecture.slot.startTime.hm()} to ${lecture.slot.endTime.hm()}" +
                                (if (room != null) " · Room $room" else ""),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (upcoming.isNotEmpty()) {
            SectionTitle("Exam countdown")
            upcoming.forEach { exam ->
                AppCard(modifier = Modifier.fillMaxWidth(), onClick = { onOpenExam(exam) }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(subjectColor(exam.subjectId)),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(exam.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${subjectNames[exam.subjectId] ?: "Unknown subject"} · ${exam.date.pretty()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = daysLabel(today, exam.date),
                                style = MaterialTheme.typography.titleSmall,
                                color = if (exam.priority == Priority.HIGH) colors.error else colors.primary,
                            )
                            Text(
                                text = "${exam.priority.label()} priority",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceCard(overallPercent: Double?, present: Int, held: Int, threshold: Int) {
    val colors = MaterialTheme.colorScheme
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            AttendanceGauge(percent = overallPercent, threshold = threshold)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Bunk buffer", style = MaterialTheme.typography.titleMedium)
                if (overallPercent == null) {
                    Text(
                        "Log a lecture on the Attends tab to see how many you can safely miss.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                } else {
                    val margin = attendanceMargin(present, held, threshold)
                    Text(
                        text = marginHeadline(margin),
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (margin.mustAttend > 0) colors.error else colors.primary,
                    )
                    Text(
                        text = marginSentence(margin, threshold),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                    Text(
                        text = "$present present of $held held",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
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

/** A 270 degree arc that springs to the attendance percentage, with a dot at the safe threshold. */
@Composable
private fun AttendanceGauge(percent: Double?, threshold: Int, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val target = ((percent ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "gauge",
    )
    val below = percent != null && percent < threshold
    val arcColor by animateColorAsState(
        targetValue = if (below) colors.error else colors.primary,
        animationSpec = spring(),
        label = "gaugeColor",
    )
    val trackColor = colors.surfaceContainerHighest
    val markerColor = colors.onSurfaceVariant

    Box(modifier = modifier.size(132.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            if (progress > 0f) {
                drawArc(
                    color = arcColor,
                    startAngle = 135f,
                    sweepAngle = 270f * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
            val angle = Math.toRadians(135.0 + 270.0 * threshold / 100.0)
            val radius = arcSize.width / 2
            drawCircle(
                color = markerColor,
                radius = 4.dp.toPx(),
                center = Offset(
                    x = size.width / 2 + (radius * cos(angle)).toFloat(),
                    y = size.height / 2 + (radius * sin(angle)).toFloat(),
                ),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (percent == null) "--" else "${percent.roundToInt()}%",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "attendance",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}
