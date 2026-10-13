package com.trajectory.vault.ui.calendar

import com.trajectory.vault.ui.theme.LocalQuality
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import com.trajectory.vault.ui.components.AttendanceToggle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import com.trajectory.vault.ui.components.ExpressiveChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trajectory.vault.data.calendar.CalendarData
import com.trajectory.vault.data.db.AttendanceStatus
import com.trajectory.vault.data.db.ExamEntity
import com.trajectory.vault.data.db.HolidayEntity
import com.trajectory.vault.domain.hm
import com.trajectory.vault.domain.holidayOn
import com.trajectory.vault.domain.isWeekend
import com.trajectory.vault.domain.lecturesOn
import com.trajectory.vault.domain.pretty
import com.trajectory.vault.domain.subjectAttendance
import com.trajectory.vault.domain.overall
import com.trajectory.vault.ui.components.AppCard
import com.trajectory.vault.ui.components.ExpressiveChip
import com.trajectory.vault.ui.components.HintText
import com.trajectory.vault.ui.components.NavCard
import com.trajectory.vault.ui.icons.AppIcons
import com.trajectory.vault.ui.components.PillButton
import com.trajectory.vault.ui.components.SectionTitle
import com.trajectory.vault.ui.main.TabColumn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun CalendarTab(
    data: CalendarData,
    now: LocalDateTime,
    contentPadding: PaddingValues,
    viewModel: CalendarViewModel,
    onOpenExam: (ExamEntity) -> Unit,
    onEditHoliday: (HolidayEntity) -> Unit,
    onOpenTimetable: () -> Unit,
    onAddNotes: (subjectId: Long, date: LocalDate) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val today = now.toLocalDate()
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    var selectedIso by rememberSaveable { mutableStateOf(today.toString()) }
    var holidayToDelete by remember { mutableStateOf<HolidayEntity?>(null) }
    val selected = LocalDate.parse(selectedIso)
    val month = YearMonth.from(today).plusMonths(monthOffset.toLong())
    val overall = remember(data) { data.subjectAttendance().overall() }

    TabColumn(contentPadding) {
        TodayCard(
            data = data,
            today = today,
            overall = overall,
            viewModel = viewModel,
            onAddNotes = onAddNotes,
        )

        MonthCard(
            month = month,
            today = today,
            selected = selected,
            data = data,
            onPrevious = { monthOffset-- },
            onNext = { monthOffset++ },
            onSelect = { selectedIso = it.toString() },
        )

        DayPanel(
            date = selected,
            today = today,
            data = data,
            onSetStatus = { subjectId, status -> viewModel.setAttendance(subjectId, selected, status) },
            onSetNotes = { subjectId, collected -> viewModel.setNotesCollected(listOf(subjectId), selected, collected) },
            onOpenExam = onOpenExam,
            onEditHoliday = onEditHoliday,
            onDeleteHoliday = { holidayToDelete = it },
        )

        SectionTitle("Schedule")
        NavCard(
            icon = AppIcons.Schedule,
            title = "Timetable",
            subtitle = "Weekly lectures with times and rooms",
            onClick = onOpenTimetable,
        )
    }

    val holiday = holidayToDelete
    if (holiday != null) {
        AlertDialog(
            onDismissRequest = { holidayToDelete = null },
            title = { Text("Remove holiday?") },
            text = { Text("${holiday.title} on ${holiday.date.pretty()} will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteHoliday(holiday)
                    holidayToDelete = null
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { holidayToDelete = null }) { Text("Keep") } },
        )
    }
}

@Composable
private fun MonthCard(
    month: YearMonth,
    today: LocalDate,
    selected: LocalDate,
    data: CalendarData,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelect: (LocalDate) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val firstDay = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    val weekdays = (0..6).map { firstDay.plus(it.toLong()) }
    // Number of empty cells before the 1st so it lands under the right weekday.
    val leading = (month.atDay(1).dayOfWeek.value - firstDay.value + 7) % 7
    val totalCells = ((leading + month.lengthOfMonth() + 6) / 7) * 7
    // Looked up once per change instead of once per day cell on every frame.
    val holidaysByDate = remember(data.holidays) { data.holidays.associateBy { it.date } }
    val examsByDate = remember(data.exams) { data.exams.groupBy { it.date } }

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(
                onClick = onPrevious,
                modifier = Modifier.semantics { contentDescription = "Previous month" },
            ) { Icon(AppIcons.ChevronLeft, contentDescription = null) }
            Text(
                text = "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}",
                style = MaterialTheme.typography.titleMedium,
            )
            TextButton(
                onClick = onNext,
                modifier = Modifier.semantics { contentDescription = "Next month" },
            ) { Icon(AppIcons.ChevronRight, contentDescription = null) }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdays.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        Column(modifier = Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (week in 0 until totalCells / 7) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (column in 0..6) {
                        val dayNumber = week * 7 + column - leading + 1
                        if (dayNumber < 1 || dayNumber > month.lengthOfMonth()) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val date = month.atDay(dayNumber)
                            DayCell(
                                date = date,
                                isToday = date == today,
                                isSelected = date == selected,
                                isOff = holidaysByDate.containsKey(date) || date.isWeekend(),
                                exams = examsByDate[date].orEmpty(),
                                onClick = { onSelect(date) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    isOff: Boolean,
    exams: List<ExamEntity>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val background = when {
        isSelected -> colors.primaryContainer
        // Holidays and weekends use a solid blend, never a see-through colour.
        isOff -> lerp(colors.surfaceContainerHigh, colors.tertiaryContainer, 0.6f)
        else -> colors.surfaceContainerHigh
    }
    val textColor = when {
        isSelected -> colors.onPrimaryContainer
        isOff -> colors.onTertiaryContainer
        else -> colors.onSurface
    }
    val primary = colors.primary
    val dotColors = exams.take(3).map { subjectColor(it.subjectId) }
    // One text node; the round background, today ring and exam dots are drawn directly so a month of
    // cells stays cheap to compose, measure and draw.
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .drawBehind {
                drawCircle(background)
                if (isToday) drawCircle(primary, radius = size.minDimension / 2f - 1.dp.toPx(), style = Stroke(width = 2.dp.toPx()))
                val dot = 2.5.dp.toPx()
                val gap = 2.dp.toPx()
                val total = dotColors.size * dot * 2f + (dotColors.size - 1).coerceAtLeast(0) * gap
                var x = (size.width - total) / 2f + dot
                val y = size.height * 0.78f
                dotColors.forEach { color ->
                    drawCircle(color, radius = dot, center = Offset(x, y))
                    x += dot * 2f + gap
                }
            }
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            modifier = Modifier.offset(y = if (dotColors.isEmpty()) 0.dp else (-2).dp),
        )
    }
}

@Composable
private fun DayPanel(
    date: LocalDate,
    today: LocalDate,
    data: CalendarData,
    onSetStatus: (Long, AttendanceStatus) -> Unit,
    onSetNotes: (Long, Boolean) -> Unit,
    onOpenExam: (ExamEntity) -> Unit,
    onEditHoliday: (HolidayEntity) -> Unit,
    onDeleteHoliday: (HolidayEntity) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val holiday = data.holidayOn(date)
    val exams = data.exams.filter { it.date == date }
    val lectures = data.lecturesOn(date)
    val isFuture = date.isAfter(today)
    val subjectNames = data.subjects.associate { it.id to it.name }

    SectionTitle(if (date == today) "Today" else date.pretty())
    if (holiday != null) {
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(holiday.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (holiday.autoSkip) "Lectures are skipped this day." else "Lectures still count this day.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { onEditHoliday(holiday) }) { Text("Edit") }
                TextButton(onClick = { onDeleteHoliday(holiday) }) { Text("Remove") }
            }
        }
    }
    if (holiday == null && date.isWeekend()) {
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Text("Weekend", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Saturdays and Sundays are always holidays.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
    exams.forEach { exam ->
        AppCard(modifier = Modifier.fillMaxWidth(), onClick = { onOpenExam(exam) }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(subjectColor(exam.subjectId)))
                Column(modifier = Modifier.weight(1f)) {
                    Text(exam.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = subjectNames[exam.subjectId] ?: "Unknown subject",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                Icon(AppIcons.ChevronRight, contentDescription = null, tint = colors.onSurfaceVariant)
            }
        }
    }
    if (lectures.isEmpty() && holiday == null && exams.isEmpty() && !date.isWeekend()) {
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Text("Nothing scheduled", style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (data.timetable.isEmpty()) "Add lectures in the timetable to track attendance." else "No lectures on this day.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
    lectures.forEach { lecture ->
        val record = data.attendance.firstOrNull { it.subjectId == lecture.subject.id && it.date == date }
        val logged = record?.status
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(subjectColor(lecture.subject.id)))
                Column(modifier = Modifier.weight(1f)) {
                    Text(lecture.subject.name, style = MaterialTheme.typography.titleMedium)
                    val room = lecture.slot.roomNumber?.takeIf { it.isNotBlank() }
                    Text(
                        text = "${lecture.slot.startTime.hm()} to ${lecture.slot.endTime.hm()}" +
                            (if (room != null) " · Room $room" else ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AttendanceStatus.entries.forEach { status ->
                    ExpressiveChip(
                        selected = logged == status,
                        enabled = !isFuture,
                        onClick = { onSetStatus(lecture.subject.id, status) },
                        label = { Text(status.chipLabel()) },
                        shape = CircleShape,
                    )
                }
            }
            if (record != null && record.status == AttendanceStatus.ABSENT) {
                ExpressiveChip(
                    selected = record.notesCollected,
                    onClick = { onSetNotes(lecture.subject.id, !record.notesCollected) },
                    label = { Text("Notes collected") },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (isFuture) {
                HintText("Attendance can be logged once the day has come.", modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
    if (lectures.map { it.subject.id }.distinct().size < lectures.size) {
        HintText("A subject with two lectures on one day shares one attendance record.")
    }
}

private fun AttendanceStatus.chipLabel(): String = when (this) {
    AttendanceStatus.PRESENT -> "Present"
    AttendanceStatus.ABSENT -> "Absent"
    AttendanceStatus.CANCELLED -> "Cancelled"
}

/**
 * The attendance overview with the animated Present / Absent switch for today. When today is marked
 * absent, a "did you collect today's notes?" section appears that can be expanded to pick the
 * subject whose notes to add. Once every absent subject has its notes the list folds away, and a
 * small arrow stays so it can be reopened.
 */
@Composable
private fun TodayCard(
    data: CalendarData,
    today: LocalDate,
    overall: com.trajectory.vault.domain.OverallAttendance,
    viewModel: CalendarViewModel,
    onAddNotes: (subjectId: Long, date: LocalDate) -> Unit,
) {
    val q = LocalQuality.current
    val colors = MaterialTheme.colorScheme
    val lectures = remember(data) { data.lecturesOn(today) }
    val subjectIds = remember(lectures) { lectures.map { it.subject.id }.distinct() }
    val records = data.attendance.filter { it.date == today && it.subjectId in subjectIds }
    val status: AttendanceStatus? = when {
        subjectIds.isEmpty() -> null
        subjectIds.all { id -> records.any { it.subjectId == id && it.status == AttendanceStatus.PRESENT } } ->
            AttendanceStatus.PRESENT
        subjectIds.all { id -> records.any { it.subjectId == id && it.status == AttendanceStatus.ABSENT } } ->
            AttendanceStatus.ABSENT
        else -> null
    }
    val absent = records.filter { it.status == AttendanceStatus.ABSENT }
    val uncollected = absent.filter { !it.notesCollected }
    val subjectNames = remember(data.subjects) { data.subjects.associate { it.id to it.name } }
    var expanded by rememberSaveable { mutableStateOf(false) }
    // When the last missing note is added, the list collapses by itself.
    LaunchedEffect(uncollected.isEmpty()) { if (uncollected.isEmpty()) expanded = false }
    val arrowRotation by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = q.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
        ),
        label = "notesArrow",
    )

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text("Attendance", style = MaterialTheme.typography.titleMedium)
        val overallPercent = overall.percent
        Text(
            text = if (overallPercent == null) {
                "No lectures logged yet"
            } else {
                "${"%.1f".format(overallPercent)}% · ${overall.present} present, ${overall.held - overall.present} absent"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        AttendanceToggle(
            selected = status,
            enabled = subjectIds.isNotEmpty(),
            onPresent = { viewModel.markDay(subjectIds, today, AttendanceStatus.PRESENT) },
            onAbsent = { viewModel.markDay(subjectIds, today, AttendanceStatus.ABSENT) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (subjectIds.isEmpty()) {
            HintText(
                text = when {
                    today.isWeekend() -> "Today is a weekend."
                    data.holidayOn(today)?.autoSkip == true -> "Today is a holiday."
                    else -> "No lectures today."
                },
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        AnimatedVisibility(
            visible = absent.isNotEmpty(),
            enter = fadeIn() + expandVertically(q.spring(dampingRatio = Spring.DampingRatioLowBouncy)),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { expanded = !expanded },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (uncollected.isEmpty()) "Today's notes collected" else "Did you collect today's notes?",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = "${absent.size - uncollected.size} of ${absent.size} " +
                                if (absent.size == 1) "subject" else "subjects",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = AppIcons.ExpandMore,
                        contentDescription = if (expanded) "Hide notes list" else "Show notes list",
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.graphicsLayer { rotationZ = arrowRotation },
                    )
                }
                if (uncollected.isNotEmpty() && !expanded) {
                    Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillButton(
                            text = "Yes, I have them",
                            tonal = true,
                            onClick = { viewModel.setNotesCollected(uncollected.map { it.subjectId }, today, true) },
                        )
                        PillButton(text = "Add today's notes", onClick = { expanded = true })
                    }
                }
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn() + expandVertically(q.spring(dampingRatio = Spring.DampingRatioLowBouncy)),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        absent.forEach { record ->
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(subjectColor(record.subjectId)))
                                    Text(
                                        text = subjectNames[record.subjectId] ?: "Unknown subject",
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.weight(1f),
                                    )
                                    if (record.notesCollected) {
                                        Icon(AppIcons.Check, contentDescription = "Collected", tint = colors.primary, modifier = Modifier.size(18.dp))
                                        TextButton(onClick = { viewModel.setNotesCollected(listOf(record.subjectId), today, false) }) {
                                            Text("Undo")
                                        }
                                    }
                                }
                                if (!record.notesCollected) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        PillButton(text = "Add notes", onClick = { onAddNotes(record.subjectId, today) })
                                        PillButton(
                                            text = "I have them",
                                            tonal = true,
                                            onClick = { viewModel.setNotesCollected(listOf(record.subjectId), today, true) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
