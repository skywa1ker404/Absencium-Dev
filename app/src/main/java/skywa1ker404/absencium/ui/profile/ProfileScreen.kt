package skywa1ker404.absencium.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import skywa1ker404.absencium.ui.components.NavCard
import skywa1ker404.absencium.ui.components.GroupPosition
import skywa1ker404.absencium.ui.components.CardStack
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AiSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingsHubScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import skywa1ker404.absencium.appContainer
import skywa1ker404.absencium.data.db.NoteStatus
import skywa1ker404.absencium.data.syllabus.SyllabusTree
import skywa1ker404.absencium.domain.attendanceMargin
import skywa1ker404.absencium.domain.overall
import skywa1ker404.absencium.domain.subjectAttendance
import skywa1ker404.absencium.ui.calendar.subjectColor
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.AppTextField
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.components.SectionTitle
import skywa1ker404.absencium.ui.components.SettingDivider
import skywa1ker404.absencium.ui.components.SettingGroup
import skywa1ker404.absencium.ui.components.SettingRow
import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.main.initialsOf
import skywa1ker404.absencium.ui.settings.rememberSettingsViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/** The student's profile as a page: who they are, how they are doing, what is next, and the shortcuts they need. */
@Destination<RootGraph>
@Composable
fun ProfileScreen(navigator: DestinationsNavigator) {
    val container = LocalContext.current.applicationContext.appContainer
    val settingsViewModel = rememberSettingsViewModel()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val data by container.calendarRepository.data.collectAsStateWithLifecycle(initialValue = null)
    val vault by container.vaultRepository.data.collectAsStateWithLifecycle(initialValue = null)
    val syllabus by container.database.syllabusDao().observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val colors = MaterialTheme.colorScheme
    val loaded = settings
    val threshold = loaded.attendanceThresholdPercent

    var name by remember(loaded.displayName) { mutableStateOf(loaded.displayName) }
    var course by remember(loaded.profileCourse) { mutableStateOf(loaded.profileCourse) }
    var year by remember(loaded.profileYear) { mutableStateOf(loaded.profileYear) }
    var college by remember(loaded.profileCollege) { mutableStateOf(loaded.profileCollege) }
    val rows = remember(data) { data?.subjectAttendance().orEmpty() }
    val overall = remember(rows) { rows.overall() }

    var editing by remember { mutableStateOf(false) }
    val details = listOf(loaded.profileCourse, loaded.profileYear, loaded.profileCollege).filter { it.isNotBlank() }

    ScreenScaffold(title = "Profile", onBack = { navigator.navigateUp() }, largeTitle = true) {
        // Who: the avatar with its attendance ring straight on the page, then the name and details.
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ProfileAvatar(name = loaded.displayName, percent = overall.percent, threshold = threshold)
            Text(
                text = loaded.displayName.ifBlank { "Add your name" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                text = if (details.isEmpty()) "Add your course and college" else details.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            if (!editing) {
                PillButton(text = "Edit profile", onClick = { editing = true }, tonal = true, compact = true, modifier = Modifier.padding(top = 10.dp))
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = editing,
            enter = androidx.compose.animation.expandVertically(androidx.compose.animation.core.spring(0.8f, 380f)) + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.shrinkVertically(androidx.compose.animation.core.spring(1f, 500f)) + androidx.compose.animation.fadeOut(),
        ) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppTextField(
                        value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true,
                        shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(),
                    )
                    AppTextField(
                        value = course, onValueChange = { course = it }, label = { Text("Course") }, singleLine = true,
                        shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(),
                    )
                    AppTextField(
                        value = year, onValueChange = { year = it }, label = { Text("Year or semester") }, singleLine = true,
                        shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(),
                    )
                    AppTextField(
                        value = college, onValueChange = { college = it }, label = { Text("College") }, singleLine = true,
                        shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
                        PillButton(text = "Save", onClick = {
                            settingsViewModel.setDisplayName(name.trim())
                            settingsViewModel.setProfileDetails(course, year, college)
                            editing = false
                        })
                        PillButton(text = "Cancel", tonal = true, onClick = {
                            name = loaded.displayName; course = loaded.profileCourse; year = loaded.profileYear; college = loaded.profileCollege
                            editing = false
                        })
                    }
                }
            }
        }

        // Overview: the overall percentage large, what it means, and three counts underneath.
        val percent = overall.percent
        val notesCount = vault?.notes?.count { it.status != NoteStatus.DRAFT } ?: 0
        AppCard(modifier = Modifier.fillMaxWidth()) {
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
                        val margin = attendanceMargin(overall.present, overall.held, threshold)
                        Text(
                            text = if (margin.mustAttend > 0) "Below target" else "On track",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = if (margin.mustAttend > 0) "Attend the next ${margin.mustAttend} lectures to reach $threshold%"
                            else "You can skip ${margin.canSkip} lectures and stay above $threshold%",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (margin.mustAttend > 0) colors.error else colors.onSurfaceVariant,
                        )
                    }
                }
            }
            SettingDivider()
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Count("${rows.size}", "Subjects")
                Count("$notesCount", "Notes")
                Count("${vault?.guides?.size ?: 0}", "Material")
            }
        }

        if (rows.isNotEmpty()) {
            SectionTitle("Attendance")
            AppCard(modifier = Modifier.fillMaxWidth()) {
                rows.forEachIndexed { index, item ->
                    if (index > 0) SettingDivider()
                    val p = item.percent
                    val margin = attendanceMargin(item.present, item.held, threshold)
                    ProgressRow(
                        title = item.subject.name,
                        subtitle = if (p == null) "No lectures yet" else "${item.present} of ${item.held} lectures" +
                            (if (margin.mustAttend > 0) " · attend ${margin.mustAttend}" else ""),
                        value = p?.let { (it / 100.0).toFloat() },
                        valueText = p?.let { "${it.roundToInt()}%" } ?: "–",
                        color = if (p != null && p < threshold) colors.error else subjectColor(item.subject.id),
                    )
                }
            }
        }

        val withSyllabus = rows.map { it.subject }.filter { subject -> syllabus.any { it.subjectId == subject.id } }
        if (withSyllabus.isNotEmpty()) {
            SectionTitle("Syllabus")
            AppCard(modifier = Modifier.fillMaxWidth()) {
                withSyllabus.forEachIndexed { index, subject ->
                    if (index > 0) SettingDivider()
                    val progress = SyllabusTree.progress(syllabus.filter { it.subjectId == subject.id }, null)
                    ProgressRow(
                        title = subject.name,
                        subtitle = null,
                        value = progress,
                        valueText = "${(progress * 100).roundToInt()}%",
                        color = subjectColor(subject.id),
                    )
                }
            }
        }

        val today = LocalDate.now()
        val upcoming = remember(data) { data?.exams.orEmpty().filter { !it.date.isBefore(today) }.sortedBy { it.date }.take(3) }
        if (upcoming.isNotEmpty()) {
            SectionTitle("Upcoming exams")
            SettingGroup {
                upcoming.forEachIndexed { index, exam ->
                    if (index > 0) SettingDivider()
                    val days = ChronoUnit.DAYS.between(today, exam.date)
                    val subject = data?.subjects?.firstOrNull { it.id == exam.subjectId }?.name
                    SettingRow(
                        title = exam.title,
                        subtitle = listOfNotNull(subject, exam.date.toString()).joinToString(" · "),
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

        CardStack {
            NavCard(
                icon = AppIcons.School,
                title = "Syllabus",
                subtitle = null,
                onClick = { navigator.navigate(com.ramcosta.composedestinations.generated.destinations.SyllabusOverviewScreenDestination) },
                position = GroupPosition.First,
            )
            NavCard(
                icon = AppIcons.AutoAwesome,
                title = "Absencium AI",
                subtitle = null,
                onClick = { navigator.navigate(AiSettingsScreenDestination) },
                position = GroupPosition.Middle,
            )
            NavCard(
                icon = AppIcons.Tune,
                title = "Settings",
                subtitle = null,
                onClick = { navigator.navigate(SettingsHubScreenDestination) },
                position = GroupPosition.Last,
            )
        }
    }
}

/** A number with its label under it. */
@Composable
private fun Count(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A title and a line under it, a number at the end, and a thin bar showing how far along it is. */
@Composable
private fun ProgressRow(title: String, subtitle: String?, value: Float?, valueText: String, color: androidx.compose.ui.graphics.Color) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Text(valueText, style = MaterialTheme.typography.titleMedium, color = color)
        }
        LinearProgressIndicator(
            progress = { (value ?: 0f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            color = color,
            trackColor = colors.surfaceContainerHighest.copy(alpha = 0.6f),
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}

/** The avatar: initials (or a person) on the primary container, ringed by the overall attendance with its number on a small badge. */
@Composable
private fun ProfileAvatar(name: String, percent: Double?, threshold: Int) {
    val colors = MaterialTheme.colorScheme
    val ringTrack = colors.surfaceContainerHighest
    val ringColor = if (percent != null && percent < threshold) colors.error else colors.primary
    Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.size(92.dp)) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .drawBehind {
                    val stroke = 5.dp.toPx()
                    val inset = stroke / 2f
                    val arc = Size(size.width - stroke, size.height - stroke)
                    drawArc(ringTrack, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
                    if (percent != null) {
                        drawArc(ringColor, -90f, 360f * (percent / 100.0).toFloat().coerceIn(0f, 1f), false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
                    }
                }
                .padding(9.dp)
                .clip(CircleShape)
                .background(colors.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            val initials = initialsOf(name)
            if (initials == null) {
                Icon(AppIcons.Person, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(36.dp))
            } else {
                Text(initials, style = MaterialTheme.typography.headlineMedium, color = colors.onPrimaryContainer)
            }
        }
        if (percent != null) {
            Text(
                text = "${percent.roundToInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = colors.onPrimary,
                modifier = Modifier.clip(CircleShape).background(ringColor).padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}
