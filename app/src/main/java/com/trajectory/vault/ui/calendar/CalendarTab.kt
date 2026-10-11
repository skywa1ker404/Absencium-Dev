package com.trajectory.vault.ui.calendar

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
import androidx.compose.material3.FilterChip
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
import com.trajectory.vault.domain.lecturesOn
import com.trajectory.vault.domain.pretty
import com.trajectory.vault.domain.subjectAttendance
import com.trajectory.vault.domain.overall
import com.trajectory.vault.ui.components.AppCard
import com.trajectory.vault.ui.components.HintText
import com.trajectory.vault.ui.components.NavCard
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
    onOpenTimetable: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val today = now.toLocalDate()
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    var selectedIso by rememberSaveable { mutableStateOf(today.toString()) }
    var holidayToDelete by remember { mutableStateOf<HolidayEntity?>(null) }
    val selected = LocalDate.parse(selectedIso)
    val month = YearMonth.from(today).plusMonths(monthOffset.toLong())
    val overall = data.subjectAttendance().overall()

    val todaysLectures = data.lecturesOn(today)

    TabColumn(contentPadding) {
        // Overview card with the one-tap bulk buttons for today.
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Text("Attendance", style = MaterialTheme.typography.titleMedium)
            val overallPercent = overall.percent
            Text(
                text = if (overallPercent == null) {
                    "No lectures logged yet"
                } else {
                    "${"%.1f".format(overallPercent)}% · ${overall.present} present, " +
                        "${overall.held - overall.present} absent"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            val todaysSubjects = todaysLectures.map { it.subject.id }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(
                    text = "Present today",
                    enabled = todaysSubjects.isNotEmpty(),
                    onClick = { viewModel.markDay(todaysSubjects, today, AttendanceStatus.PRESENT) },
                )
                PillButton(
                    text = "Absent today",
                    tonal = true,
                    enabled = todaysSubjects.isNotEmpty(),
                    onClick = { viewModel.markDay(todaysSubjects, today, AttendanceStatus.ABSENT) },
                )
            }
            if (todaysSubjects.isEmpty()) {
                HintText(
                    text = if (data.holidayOn(today)?.autoSkip == true) "Today is a holiday." else "No lectures today.",
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

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
            onOpenExam = onOpenExam,
            onDeleteHoliday = { holidayToDelete = it },
        )

        SectionTitle("Schedule")
        NavCard(
            emoji = "🗓️",
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

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(
                onClick = onPrevious,
                modifier = Modifier.semantics { contentDescription = "Previous month" },
            ) { Text("‹", style = MaterialTheme.typography.headlineSmall) }
            Text(
                text = "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}",
                style = MaterialTheme.typography.titleMedium,
            )
            TextButton(
                onClick = onNext,
                modifier = Modifier.semantics { contentDescription = "Next month" },
            ) { Text("›", style = MaterialTheme.typography.headlineSmall) }
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
                                holiday = data.holidayOn(date),
                                exams = data.exams.filter { it.date == date },
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
    holiday: HolidayEntity?,
    exams: List<ExamEntity>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val background = when {
        isSelected -> colors.primaryContainer
        holiday != null -> colors.tertiaryContainer.copy(alpha = 0.6f)
        else -> colors.surfaceContainerHigh
    }
    val textColor = when {
        isSelected -> colors.onPrimaryContainer
        holiday != null -> colors.onTertiaryContainer
        else -> colors.onSurface
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(background)
            .then(if (isToday) Modifier.border(2.dp, colors.primary, CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = textColor,
            )
            // One coloured dot per exam, up to three, in the subject's colour.
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 1.dp)) {
                exams.take(3).forEach { exam ->
                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(subjectColor(exam.subjectId)))
                }
                if (exams.isEmpty()) Box(modifier = Modifier.size(5.dp))
            }
        }
    }
}

@Composable
private fun DayPanel(
    date: LocalDate,
    today: LocalDate,
    data: CalendarData,
    onSetStatus: (Long, AttendanceStatus) -> Unit,
    onOpenExam: (ExamEntity) -> Unit,
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
                    Text("🏖️ ${holiday.title}", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (holiday.autoSkip) "Lectures are skipped this day." else "Lectures still count this day.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { onDeleteHoliday(holiday) }) { Text("Remove") }
            }
        }
    }
    exams.forEach { exam ->
        AppCard(modifier = Modifier.fillMaxWidth(), onClick = { onOpenExam(exam) }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(subjectColor(exam.subjectId)))
                Column(modifier = Modifier.weight(1f)) {
                    Text("🚨 ${exam.title}", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = subjectNames[exam.subjectId] ?: "Unknown subject",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                Text("›", style = MaterialTheme.typography.headlineSmall, color = colors.onSurfaceVariant)
            }
        }
    }
    if (lectures.isEmpty() && holiday == null && exams.isEmpty()) {
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
        val logged = data.attendance.firstOrNull { it.subjectId == lecture.subject.id && it.date == date }?.status
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
                    FilterChip(
                        selected = logged == status,
                        enabled = !isFuture,
                        onClick = { onSetStatus(lecture.subject.id, status) },
                        label = { Text(status.chipLabel()) },
                        shape = CircleShape,
                    )
                }
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
