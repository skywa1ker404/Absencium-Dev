package skywa1ker404.absencium.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import me.rerere.rikkahub.ui.components.dots.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.generated.destinations.AttendanceInsightsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.EditProfileScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ExportScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PlannerScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ReadingScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SubjectFolderScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SyllabusOverviewScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import skywa1ker404.absencium.AppContainer
import skywa1ker404.absencium.data.calendar.CalendarData
import skywa1ker404.absencium.data.db.AttendanceStatus
import skywa1ker404.absencium.data.db.EventEntity
import skywa1ker404.absencium.data.db.NoteStatus
import skywa1ker404.absencium.data.db.StudyGoalEntity
import skywa1ker404.absencium.data.db.SyllabusNodeEntity
import skywa1ker404.absencium.data.db.UserStatsEntity
import skywa1ker404.absencium.data.settings.AppSettings
import skywa1ker404.absencium.data.syllabus.SyllabusTree
import skywa1ker404.absencium.data.vault.VaultData
import skywa1ker404.absencium.domain.OverallAttendance
import skywa1ker404.absencium.domain.SubjectAttendance
import skywa1ker404.absencium.domain.attendanceMargin
import skywa1ker404.absencium.ui.calendar.subjectColor
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.SettingDivider
import skywa1ker404.absencium.ui.components.SettingGroup
import skywa1ker404.absencium.ui.components.SettingRow
import skywa1ker404.absencium.ui.components.hueFor
import skywa1ker404.absencium.ui.icons.AppIcons
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

/** Everything the three Profile tabs read, all of it already in Room or Settings; nothing here needs a new feature. */
internal class ProfileTabData(
    val settings: AppSettings,
    val rows: List<SubjectAttendance>,
    val overall: OverallAttendance,
    val calendar: CalendarData?,
    val vault: VaultData?,
    val syllabus: List<SyllabusNodeEntity>,
    val stats: List<UserStatsEntity>,
    val goals: List<StudyGoalEntity>,
    val events: List<EventEntity>,
    val today: LocalDate,
)

private fun minutesToText(minutes: Int): String = if (minutes < 60) "$minutes min" else "${minutes / 60} h ${minutes % 60} min"

/** The Overview tab: attendance, the numbers that matter, what is due next, the notes opened last and the syllabus. */
@Composable
internal fun OverviewTab(nav: DestinationsNavigator, d: ProfileTabData) {
    val colors = MaterialTheme.colorScheme
    val threshold = d.settings.attendanceThresholdPercent
    val percent = d.overall.percent
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = { nav.navigate(AttendanceInsightsScreenDestination) }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = percent?.let { "${it.roundToInt()}%" } ?: "–",
                style = MaterialTheme.typography.displaySmall,
                color = if (percent != null && percent < threshold) colors.error else colors.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                if (percent == null) {
                    Text("No attendance yet", style = MaterialTheme.typography.titleMedium)
                    Text("Log lectures to see it here", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                } else {
                    val margin = attendanceMargin(d.overall.present, d.overall.held, threshold)
                    Text(if (margin.mustAttend > 0) "Below target" else "On track", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (margin.mustAttend > 0) "Attend the next ${margin.mustAttend} lectures to reach $threshold%"
                        else "You can skip ${margin.canSkip} lectures and stay above $threshold%",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (margin.mustAttend > 0) colors.error else colors.onSurfaceVariant,
                    )
                }
            }
            Icon(AppIcons.ChevronRight, contentDescription = null, tint = colors.onSurfaceVariant)
        }
    }

    // Academic progress, tasks and this week's study in three numbers.
    val withSyllabus = d.rows.map { it.subject }.filter { s -> d.syllabus.any { it.subjectId == s.id } }
    val syllabusAverage = if (withSyllabus.isEmpty()) null
    else withSyllabus.map { s -> SyllabusTree.progress(d.syllabus.filter { it.subjectId == s.id }, null) }.average().toFloat()
    val weekStart = d.today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekMinutes = d.stats.filter { !it.date.isBefore(weekStart) && !it.date.isAfter(d.today) }.sumOf { it.studyDurationMinutes }
    val goalsDone = d.goals.count { it.done }
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BigNumber(syllabusAverage?.let { "${(it * 100).roundToInt()}%" } ?: "–", "SYLLABUS", "${withSyllabus.size} subjects planned")
            BigNumber("$goalsDone/${d.goals.size}", "TASKS", "goals completed")
            BigNumber(if (weekMinutes == 0) "–" else minutesToText(weekMinutes), "STUDY", "this week")
        }
    }

    // Upcoming deadlines: exams, events and goals with a date, soonest first.
    class Deadline(val title: String, val line: String, val date: LocalDate)
    val subjectName = { id: Long? -> d.calendar?.subjects?.firstOrNull { it.id == id }?.name }
    val deadlines = buildList {
        d.calendar?.exams.orEmpty().filter { !it.date.isBefore(d.today) }.forEach { add(Deadline(it.title, listOfNotNull("Exam", subjectName(it.subjectId)).joinToString(" · "), it.date)) }
        d.events.filter { !it.date.isBefore(d.today) }.forEach { add(Deadline(it.title, listOfNotNull(it.category, subjectName(it.subjectId)).joinToString(" · "), it.date)) }
        d.goals.filter { !it.done && it.targetDate != null && !it.targetDate.isBefore(d.today) }.forEach { add(Deadline(it.title, listOfNotNull("Goal", subjectName(it.subjectId)).joinToString(" · "), it.targetDate!!)) }
    }.sortedBy { it.date }.take(5)
    SectionLink("Upcoming deadlines") { nav.navigate(PlannerScreenDestination) }
    if (deadlines.isEmpty()) {
        AppCard(modifier = Modifier.fillMaxWidth()) { Text("Nothing due soon", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
    } else {
        SettingGroup {
            deadlines.forEachIndexed { index, item ->
                if (index > 0) SettingDivider()
                val days = ChronoUnit.DAYS.between(d.today, item.date)
                SettingRow(
                    title = item.title,
                    subtitle = "${item.line} · ${item.date}",
                    trailing = {
                        Text(
                            text = when (days) { 0L -> "Today"; 1L -> "Tomorrow"; else -> "In $days days" },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (days <= 7) colors.error else colors.primary,
                        )
                    },
                )
            }
        }
    }

    // Recently accessed notes.
    val notes = d.vault?.notes.orEmpty()
        .filter { it.status != NoteStatus.DRAFT && it.deletedAt == null }
        .sortedByDescending { maxOf(it.updatedAt, it.createdDate) }
        .take(4)
    if (notes.isNotEmpty()) {
        SectionLink("Recent notes")
        SettingGroup {
            notes.forEachIndexed { index, note ->
                if (index > 0) SettingDivider()
                SettingRow(
                    title = note.title.ifBlank { "Untitled" },
                    subtitle = d.vault?.subjects?.firstOrNull { it.id == note.subjectId }?.name,
                    onClick = { nav.navigate(ReadingScreenDestination(noteId = note.id)) },
                )
            }
        }
    }

    if (withSyllabus.isNotEmpty()) {
        SectionLink("Syllabus") { nav.navigate(SyllabusOverviewScreenDestination) }
        AppCard(modifier = Modifier.fillMaxWidth()) {
            withSyllabus.forEachIndexed { index, subject ->
                if (index > 0) SettingDivider()
                val progress = SyllabusTree.progress(d.syllabus.filter { it.subjectId == subject.id }, null)
                ProgressRow(subject.name, null, progress, "${(progress * 100).roundToInt()}%", subjectColor(subject.id))
            }
        }
    }
}

/** The Academic tab: course and college, subjects, the week's timetable, attendance history, every exam and the goals. */
@Composable
internal fun AcademicTab(nav: DestinationsNavigator, container: AppContainer, d: ProfileTabData) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val s = d.settings
    SectionLink("Course and institution") { nav.navigate(EditProfileScreenDestination) }
    SettingGroup {
        SettingRow("Course", subtitle = s.profileCourse.ifBlank { "Not set" }, onClick = { nav.navigate(EditProfileScreenDestination) })
        SettingDivider()
        SettingRow("Institution", subtitle = s.profileCollege.ifBlank { "Not set" }, onClick = { nav.navigate(EditProfileScreenDestination) })
        SettingDivider()
        SettingRow("Academic year", subtitle = s.profileYear.ifBlank { "Not set" }, onClick = { nav.navigate(EditProfileScreenDestination) })
    }

    SectionLink("Subjects") { nav.navigate(AttendanceInsightsScreenDestination) }
    if (d.rows.isEmpty()) {
        AppCard(modifier = Modifier.fillMaxWidth()) { Text("Add subjects to see them here", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
    } else {
        SettingGroup {
            d.rows.forEachIndexed { index, item ->
                if (index > 0) SettingDivider()
                val p = item.percent
                SettingRow(
                    title = item.subject.name,
                    subtitle = "${item.present} of ${item.held} lectures attended",
                    onClick = { nav.navigate(SubjectFolderScreenDestination(subjectId = item.subject.id)) },
                    trailing = {
                        Text(
                            p?.let { "${it.roundToInt()}%" } ?: "–",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (p != null && p < d.settings.attendanceThresholdPercent) colors.error else colors.primary,
                        )
                    },
                )
            }
        }
    }

    // Timetable overview: lectures per weekday as small bars.
    val timetable = d.calendar?.timetable.orEmpty().filter { it.deletedAt == null }
    SectionLink("Timetable")
    if (timetable.isEmpty()) {
        AppCard(modifier = Modifier.fillMaxWidth()) { Text("No timetable yet", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
    } else {
        val days = DayOfWeek.values().toList()
        val counts = days.map { day -> timetable.count { it.dayOfWeek == day } }
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Bars(counts.map { it.toFloat() }, days.map { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3) }, counts.map { it.toString() }, colors.primary)
            HintText("${timetable.size} lectures a week")
        }
    }

    // Attendance history: the latest entries.
    val history = d.calendar?.attendance.orEmpty().sortedWith(compareByDescending<skywa1ker404.absencium.data.db.AttendanceEntity> { it.date }.thenByDescending { it.id }).take(6)
    SectionLink("Attendance history") { nav.navigate(AttendanceInsightsScreenDestination) }
    if (history.isEmpty()) {
        AppCard(modifier = Modifier.fillMaxWidth()) { Text("No lectures logged yet", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
    } else {
        SettingGroup {
            history.forEachIndexed { index, entry ->
                if (index > 0) SettingDivider()
                SettingRow(
                    title = d.calendar?.subjects?.firstOrNull { it.id == entry.subjectId }?.name ?: "Subject",
                    subtitle = entry.date.toString(),
                    trailing = {
                        Text(
                            entry.status.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelLarge,
                            color = when (entry.status) { AttendanceStatus.ABSENT -> colors.error; AttendanceStatus.PRESENT -> colors.primary; else -> colors.onSurfaceVariant },
                        )
                    },
                )
            }
        }
    }

    // Exam schedule: every exam still ahead, then the latest results.
    val exams = d.calendar?.exams.orEmpty().filter { !it.date.isBefore(d.today) }.sortedBy { it.date }
    SectionLink("Exam schedule") { nav.navigate(PlannerScreenDestination) }
    if (exams.isEmpty()) {
        AppCard(modifier = Modifier.fillMaxWidth()) { Text("No exams ahead", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
    } else {
        SettingGroup {
            exams.take(8).forEachIndexed { index, exam ->
                if (index > 0) SettingDivider()
                val days = ChronoUnit.DAYS.between(d.today, exam.date)
                SettingRow(
                    title = exam.title,
                    subtitle = listOfNotNull(d.calendar?.subjects?.firstOrNull { it.id == exam.subjectId }?.name, exam.date.toString()).joinToString(" · "),
                    trailing = {
                        Text(
                            when (days) { 0L -> "Today"; 1L -> "Tomorrow"; else -> "In $days days" },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (days <= 7) colors.error else colors.primary,
                        )
                    },
                )
            }
        }
    }

    // Academic goals, ticked off right here.
    SectionLink("Academic goals") { nav.navigate(PlannerScreenDestination) }
    if (d.goals.isEmpty()) {
        AppCard(modifier = Modifier.fillMaxWidth()) { Text("No goals yet. Add one in the Planner.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
    } else {
        SettingGroup {
            d.goals.take(8).forEachIndexed { index, goal ->
                if (index > 0) SettingDivider()
                SettingRow(
                    title = goal.title,
                    subtitle = listOfNotNull(d.calendar?.subjects?.firstOrNull { it.id == goal.subjectId }?.name, goal.targetDate?.toString()).joinToString(" · ").ifBlank { null },
                    onClick = { scope.launch { container.database.studyGoalDao().update(goal.copy(done = !goal.done)) } },
                    trailing = {
                        androidx.compose.material3.Checkbox(
                            checked = goal.done,
                            onCheckedChange = { scope.launch { container.database.studyGoalDao().update(goal.copy(done = it)) } },
                        )
                    },
                )
            }
        }
    }

    // Data portability: everything is already on the device, and Export writes it out without any cloud.
    SectionLink("Your data") { nav.navigate(ExportScreenDestination) }
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = { nav.navigate(ExportScreenDestination) }) {
        Text("Export or back up your data", style = MaterialTheme.typography.titleSmall)
        HintText("Everything stays on this device. Export writes a file you keep, no account or cloud.")
    }
}

/** The Insights tab: trends and statistics worked out from attendance, study time, goals, exams and notes. */
@Composable
internal fun InsightsTab(nav: DestinationsNavigator, d: ProfileTabData) {
    val colors = MaterialTheme.colorScheme
    val attendance = d.calendar?.attendance.orEmpty()
    val weekStart = d.today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    // Weekly recap.
    val thisWeek = attendance.filter { !it.date.isBefore(weekStart) && !it.date.isAfter(d.today) }
    val weekPresent = thisWeek.count { it.status == AttendanceStatus.PRESENT }
    val weekAbsent = thisWeek.count { it.status == AttendanceStatus.ABSENT }
    val weekMinutes = d.stats.filter { !it.date.isBefore(weekStart) && !it.date.isAfter(d.today) }.sumOf { it.studyDurationMinutes }
    val weekNotes = d.vault?.notes.orEmpty().count { it.deletedAt == null && java.time.Instant.ofEpochMilli(maxOf(it.updatedAt, it.createdDate)).atZone(java.time.ZoneId.systemDefault()).toLocalDate() >= weekStart }
    SectionLink("This week") { nav.navigate(AttendanceInsightsScreenDestination) }
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BigNumber("$weekPresent", "ATTENDED", "$weekAbsent missed")
            BigNumber(if (weekMinutes == 0) "–" else minutesToText(weekMinutes), "STUDIED", "since Monday")
            BigNumber("$weekNotes", "NOTES", "written or edited")
        }
    }

    // Attendance trend: the last eight weeks.
    val weeks = (7 downTo 0).map { weekStart.minusWeeks(it.toLong()) }
    val trend = weeks.map { start ->
        val list = attendance.filter { !it.date.isBefore(start) && it.date.isBefore(start.plusWeeks(1)) }
        val p = list.count { it.status == AttendanceStatus.PRESENT }
        val a = list.count { it.status == AttendanceStatus.ABSENT }
        if (p + a == 0) null else p * 100f / (p + a)
    }
    SectionLink("Attendance trend") { nav.navigate(AttendanceInsightsScreenDestination) }
    AppCard(modifier = Modifier.fillMaxWidth()) {
        if (trend.all { it == null }) Text("Log lectures to see the trend", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        else Bars(
            trend.map { (it ?: 0f) / 100f },
            weeks.map { "${it.dayOfMonth}/${it.monthValue}" },
            trend.map { it?.let { v -> "${v.roundToInt()}" } ?: "" },
            colors.primary,
        )
        HintText("Weekly attendance, week starting on the date shown")
    }

    // Study time, last seven days.
    val days = (6 downTo 0).map { d.today.minusDays(it.toLong()) }
    val minutes = days.map { day -> d.stats.filter { it.date == day }.sumOf { it.studyDurationMinutes } }
    SectionLink("Study time")
    AppCard(modifier = Modifier.fillMaxWidth()) {
        if (minutes.all { it == 0 }) Text("No study time logged in the last week", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        else {
            val top = minutes.max().coerceAtLeast(1)
            Bars(minutes.map { it.toFloat() / top }, days.map { it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3) }, minutes.map { if (it == 0) "" else it.toString() }, colors.tertiary)
            HintText("Minutes a day, last 7 days")
        }
    }

    // Streaks and the study calendar.
    val studyDays = d.stats.filter { it.studyDurationMinutes > 0 }.map { it.date }.toSet()
    var current = 0
    var cursor = if (studyDays.contains(d.today)) d.today else d.today.minusDays(1)
    while (studyDays.contains(cursor)) { current++; cursor = cursor.minusDays(1) }
    var best = 0
    var run = 0
    studyDays.sorted().let { sorted ->
        sorted.forEachIndexed { i, date ->
            run = if (i > 0 && sorted[i - 1].plusDays(1) == date) run + 1 else 1
            if (run > best) best = run
        }
    }
    SectionLink("Study streaks")
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BigNumber("$current", "CURRENT", if (current == 1) "day in a row" else "days in a row")
            BigNumber("$best", "BEST", "in the last months")
            BigNumber("${studyDays.size}", "STUDY DAYS", "logged")
        }
        SettingDivider()
        StudyCalendar(studyDays, d.today)
    }

    // Tasks completed vs pending.
    val done = d.goals.count { it.done }
    val pending = d.goals.size - done
    SectionLink("Tasks") { nav.navigate(PlannerScreenDestination) }
    AppCard(modifier = Modifier.fillMaxWidth()) {
        if (d.goals.isEmpty()) Text("No goals yet", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        else CapsuleBar(listOf(Triple("Completed", done, colors.primary), Triple("Pending", pending, colors.outline)))
    }

    // Subject-wise performance: average exam result per subject next to its pass mark.
    val graded = d.calendar?.exams.orEmpty().filter { it.marksObtained != null && (it.maxMarks ?: 0.0) > 0.0 }
    SectionLink("Subject performance") { nav.navigate(PlannerScreenDestination) }
    AppCard(modifier = Modifier.fillMaxWidth()) {
        if (graded.isEmpty()) {
            Text("Add marks to your exams to see results per subject", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        } else {
            val bySubject = graded.groupBy { it.subjectId }
            var first = true
            bySubject.forEach { (id, list) ->
                val subject = d.calendar?.subjects?.firstOrNull { it.id == id } ?: return@forEach
                val avg = list.map { it.marksObtained!! * 100.0 / it.maxMarks!! }.average()
                if (!first) SettingDivider()
                first = false
                ProgressRow(
                    subject.name,
                    "${list.size} ${if (list.size == 1) "exam" else "exams"} · pass mark ${subject.passMark.roundToInt()}%",
                    (avg / 100.0).toFloat(),
                    "${avg.roundToInt()}%",
                    if (avg < subject.passMark) colors.error else subjectColor(subject.id),
                )
            }
        }
    }

    // Monthly activity: lectures logged over the last six months.
    val months = (5 downTo 0).map { d.today.withDayOfMonth(1).minusMonths(it.toLong()) }
    val monthly = months.map { m -> attendance.count { it.date.year == m.year && it.date.month == m.month } }
    SectionLink("Monthly activity") { nav.navigate(AttendanceInsightsScreenDestination) }
    AppCard(modifier = Modifier.fillMaxWidth()) {
        if (monthly.all { it == 0 }) Text("No lectures logged in the last six months", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        else {
            val top = monthly.max().coerceAtLeast(1)
            Bars(monthly.map { it.toFloat() / top }, months.map { it.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3) }, monthly.map { it.toString() }, colors.secondary)
            HintText("Lectures logged each month")
        }
    }

    SectionLink("Last 12 weeks") { nav.navigate(AttendanceInsightsScreenDestination) }
    AppCard(modifier = Modifier.fillMaxWidth()) { HeatMap(attendance, d.today) }
}

/** Vertical bars: [fractions] are 0..1 heights, [labels] sit under each bar and [values] on top of it. */
@Composable
private fun Bars(fractions: List<Float>, labels: List<String>, values: List<String>, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
        fractions.forEachIndexed { i, f ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(values.getOrElse(i) { "" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Box(
                    modifier = Modifier
                        .padding(vertical = 3.dp)
                        .fillMaxWidth()
                        .height((72 * f.coerceIn(0f, 1f)).coerceAtLeast(4f).dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (f <= 0f) MaterialTheme.colorScheme.surfaceContainerHighest else color),
                )
                Text(labels.getOrElse(i) { "" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

/** Twelve weeks of days as small squares; a day with study time logged is filled. */
@Composable
private fun StudyCalendar(studyDays: Set<LocalDate>, today: LocalDate) {
    val colors = MaterialTheme.colorScheme
    val start = today.minusDays((today.dayOfWeek.value % 7).toLong()).minusWeeks(11)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
        repeat(12) { week ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                repeat(7) { day ->
                    val date = start.plusDays((week * 7 + day).toLong())
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when {
                                    date.isAfter(today) -> Color.Transparent
                                    studyDays.contains(date) -> colors.tertiary
                                    else -> colors.surfaceContainerHighest.copy(alpha = 0.7f)
                                },
                            ),
                    )
                }
            }
        }
    }
}
