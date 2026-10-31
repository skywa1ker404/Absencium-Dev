package skywa1ker404.absencium.ui.home

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import skywa1ker404.absencium.ui.components.Reveal
import skywa1ker404.absencium.ui.components.ExpandChevron
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import skywa1ker404.absencium.ui.components.Hue
import skywa1ker404.absencium.ui.components.hueFor
import skywa1ker404.absencium.ui.components.bounceIn
import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.theme.LocalQuality
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
import androidx.compose.foundation.layout.requiredSize
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
import skywa1ker404.absencium.data.calendar.CalendarData
import skywa1ker404.absencium.data.db.ExamEntity
import skywa1ker404.absencium.data.db.Priority
import skywa1ker404.absencium.data.settings.AppSettings
import skywa1ker404.absencium.domain.Margin
import skywa1ker404.absencium.domain.SubjectAttendance
import skywa1ker404.absencium.domain.attendanceMargin
import skywa1ker404.absencium.domain.hm
import skywa1ker404.absencium.domain.nextLecture
import skywa1ker404.absencium.domain.overall
import skywa1ker404.absencium.domain.pretty
import skywa1ker404.absencium.domain.subjectAttendance
import skywa1ker404.absencium.ui.calendar.daysLabel
import skywa1ker404.absencium.ui.calendar.label
import skywa1ker404.absencium.ui.calendar.subjectColor
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.components.RingGauge
import skywa1ker404.absencium.ui.components.TileGrid
import skywa1ker404.absencium.ui.components.SectionTitle
import skywa1ker404.absencium.ui.main.TabColumn
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
    onOpenAttendance: () -> Unit,
    onLogAttendance: () -> Unit,
    onOpenTimetable: () -> Unit,
    onOpenAiHub: (() -> Unit)? = null,
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
            // Two weights, like the headings of the reference designs: the greeting quiet, the name strong.
            Text(
                text = if (name.isEmpty()) greeting(now.hour) else "${greeting(now.hour)},",
                style = MaterialTheme.typography.headlineLarge,
                color = if (name.isEmpty()) colors.onSurface else colors.onSurfaceVariant,
            )
            if (name.isNotEmpty()) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                )
            }
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

        if (data.subjects.any { it.isActive }) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                QuickAction(AppIcons.CalendarMonth, "Attendance", hueFor(0), onLogAttendance, Modifier.bounceIn(0))
                QuickAction(AppIcons.Schedule, "Timetable", hueFor(1), onOpenTimetable, Modifier.bounceIn(60))
                // Subjects has its own pinned button beside the pill on this screen; the AI Hub takes the third place.
                if (onOpenAiHub != null) QuickAction(AppIcons.AutoAwesome, "Absencium AI", hueFor(2), onOpenAiHub, Modifier.bounceIn(120))
            }
        }

        if (data.subjects.none { it.isActive }) {
            skywa1ker404.absencium.ui.components.ExpressiveEmptyState(
                icon = AppIcons.School,
                title = "Add your subjects",
                text = "Attendance, the timetable and exams all start from your subject list.",
                action = { PillButton(text = "Add subjects", onClick = onManageSubjects) },
            )
        } else {
            val watch = perSubject.filter { s -> s.percent?.let { it < threshold + APPROACHING_POINTS } == true }
            var watchOpen by rememberSaveable { mutableStateOf(false) }
            val overallPct = overall.percent
            val below = overallPct != null && overallPct < threshold
            val joined = if (below) colors.errorContainer else colors.primaryContainer
            val onJoined = if (below) colors.onErrorContainer else colors.onPrimaryContainer
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                AttendanceCard(
                    overallPercent = overall.percent,
                    present = overall.present,
                    held = overall.held,
                    threshold = threshold,
                    onClick = onOpenAttendance,
                    hasWatch = watch.isNotEmpty(),
                    watchOpen = watchOpen,
                    onToggleWatch = { watchOpen = !watchOpen },
                )
                // Closed by default: the subjects that need watching hang from the bunk buffer card like a drawer.
                Reveal(visible = watchOpen && watch.isNotEmpty()) {
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        container = joined.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 28.dp, bottomEnd = 28.dp),
                        onClick = onOpenAttendance,
                    ) {
                        Text("Watch these subjects", style = MaterialTheme.typography.labelLarge, color = onJoined.copy(alpha = 0.8f))
                        watch.forEachIndexed { index, item ->
                            Box(modifier = Modifier.padding(top = if (index == 0) 8.dp else 12.dp)) { WatchRow(item, threshold) }
                        }
                    }
                }
            }
        }

        SectionTitle("Next up")
        AppCard(modifier = Modifier.fillMaxWidth(), container = colors.secondaryContainer, onClick = onOpenTimetable) {
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
private fun AttendanceCard(
    overallPercent: Double?,
    present: Int,
    held: Int,
    threshold: Int,
    onClick: () -> Unit,
    hasWatch: Boolean,
    watchOpen: Boolean,
    onToggleWatch: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val below = overallPercent != null && overallPercent < threshold
    val container = if (below) colors.errorContainer else colors.primaryContainer
    val onContainer = if (below) colors.onErrorContainer else colors.onPrimaryContainer
    val bottomCorner by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (watchOpen && hasWatch) 8.dp else 28.dp,
        animationSpec = LocalQuality.current.spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "bunkCorner",
    )
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        container = container,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = bottomCorner, bottomEnd = bottomCorner),
        onClick = onClick,
    ) {
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
            }
        }
        // Present / Absent / Total always sit side by side on their own line under the ring and the text, with the arrow at the end.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 10.dp).fillMaxWidth(),
        ) {
            CompactPill("Present $present", colors.primary, colors.onPrimary)
            CompactPill(
                "Absent ${(held - present).coerceAtLeast(0)}",
                if (below) colors.error else colors.errorContainer,
                if (below) colors.onError else colors.onErrorContainer,
            )
            CompactPill("Total $held", colors.surface, colors.onSurface)
            if (hasWatch) {
                Spacer(Modifier.weight(1f))
                // No text: a small arrow in the corner opens the subjects to watch.
                Box(
                    modifier = Modifier
                        .requiredSize(36.dp)
                        .clip(CircleShape)
                        .background(onContainer.copy(alpha = 0.22f))
                        .clickable(onClick = onToggleWatch),
                    contentAlignment = Alignment.Center,
                ) {
                    ExpandChevron(
                        expanded = watchOpen,
                        description = if (watchOpen) "Hide the subjects to watch" else "Show the subjects to watch",
                        modifier = Modifier.requiredSize(26.dp),
                        tint = onContainer,
                    )
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

/** A round shortcut with its name under it, like the quick actions of reference image 05. */
@Composable
private fun QuickAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    hue: Hue,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp)).clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape).background(hue.container.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = hue.onContainer, modifier = Modifier.size(28.dp)) }
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurface)
    }
}
