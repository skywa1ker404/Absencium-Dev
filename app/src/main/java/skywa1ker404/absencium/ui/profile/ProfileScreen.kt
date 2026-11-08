package skywa1ker404.absencium.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.ramcosta.composedestinations.generated.destinations.AiHubScreenDestination
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

    ScreenScaffold(title = "Profile", onBack = { navigator.navigateUp() }, largeTitle = true) {
        // Who
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ProfileAvatar(name = name, percent = overall.percent, threshold = threshold)
                    AppTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Your name") },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f),
                    )
                }
                AppTextField(
                    value = course,
                    onValueChange = { course = it },
                    label = { Text("Course or class") },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppTextField(
                        value = year,
                        onValueChange = { year = it },
                        label = { Text("Year or semester") },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f),
                    )
                    AppTextField(
                        value = college,
                        onValueChange = { college = it },
                        label = { Text("College") },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f),
                    )
                }
                val changed = name.trim() != loaded.displayName.trim() || course.trim() != loaded.profileCourse ||
                    year.trim() != loaded.profileYear || college.trim() != loaded.profileCollege
                if (changed) {
                    PillButton(text = "Save", onClick = {
                        settingsViewModel.setDisplayName(name.trim())
                        settingsViewModel.setProfileDetails(course, year, college)
                    })
                }
                HintText("Kept on this phone only. It is used for greetings and by Absencium AI.")
            }
        }

        // How am I doing
        SectionTitle("Attendance")
        AppCard(modifier = Modifier.fillMaxWidth()) {
            val percent = overall.percent
            if (percent == null) {
                HintText("Add subjects and log lectures to see attendance here.")
            } else {
                val margin = attendanceMargin(overall.present, overall.held, threshold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "${percent.roundToInt()}%",
                        style = MaterialTheme.typography.displaySmall,
                        color = if (percent < threshold) colors.error else colors.primary,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${overall.present} present of ${overall.held} held", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (margin.mustAttend > 0) "Attend the next ${margin.mustAttend} to reach your $threshold% target"
                            else "You can skip ${margin.canSkip} and stay above your $threshold% target",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        rows.forEach { item ->
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(subjectColor(item.subject.id)))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.subject.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "${item.present} present · ${item.absent} absent · ${item.cancelled} cancelled",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    val percent = item.percent
                    if (percent != null) {
                        val margin = attendanceMargin(item.present, item.held, threshold)
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${percent.roundToInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (percent < threshold) colors.error else colors.primary,
                            )
                            Text(
                                text = if (margin.mustAttend > 0) "attend ${margin.mustAttend}" else "can skip ${margin.canSkip}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // Syllabus
        val withSyllabus = rows.map { it.subject }.filter { subject -> syllabus.any { it.subjectId == subject.id } }
        if (withSyllabus.isNotEmpty()) {
            SectionTitle("Syllabus")
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    withSyllabus.forEach { subject ->
                        val progress = SyllabusTree.progress(syllabus.filter { it.subjectId == subject.id }, null)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(subject.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                Text("${(progress * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge, color = colors.primary)
                            }
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().clip(CircleShape),
                                color = subjectColor(subject.id),
                                trackColor = colors.surfaceContainerHighest,
                            )
                        }
                    }
                }
            }
        }

        // What is next
        val today = LocalDate.now()
        val upcoming = remember(data) { data?.exams.orEmpty().filter { !it.date.isBefore(today) }.sortedBy { it.date }.take(3) }
        if (upcoming.isNotEmpty()) {
            SectionTitle("Next exams")
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
                                text = when (days) { 0L -> "today"; 1L -> "tomorrow"; else -> "in $days days" },
                                style = MaterialTheme.typography.labelLarge,
                                color = if (days <= 7) colors.error else colors.primary,
                            )
                        },
                    )
                }
            }
        }

        // What I have
        vault?.let { v ->
            val notes = v.notes.count { it.status != NoteStatus.DRAFT }
            SectionTitle("Vault")
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    VaultStat("$notes", "Notes")
                    VaultStat("${v.guides.size}", "Study material")
                    VaultStat("${v.drafts.size}", "Drafts")
                    VaultStat("${rows.size}", "Subjects")
                }
            }
        }

        // Where to go
        SectionTitle("More")
        SettingGroup {
            SettingRow(
                title = "Absencium AI",
                subtitle = if (loaded.askAiEnabled) "Chat, attach files, and let it look things up or change your notes" else "AI Features are off. Turn them on in AI settings, or ignore this.",
                onClick = { navigator.navigate(AiHubScreenDestination()) },
                leading = { Icon(AppIcons.AutoAwesome, contentDescription = null, tint = colors.primary) },
            )
            SettingDivider()
            SettingRow(
                title = "Settings",
                subtitle = "General, appearance, storage and AI",
                onClick = { navigator.navigate(SettingsHubScreenDestination) },
                leading = { Icon(AppIcons.Tune, contentDescription = null, tint = colors.primary) },
            )
        }
    }
}

@Composable
private fun VaultStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
