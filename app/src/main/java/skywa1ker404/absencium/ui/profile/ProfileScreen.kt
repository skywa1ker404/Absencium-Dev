package skywa1ker404.absencium.ui.profile

import androidx.compose.foundation.background
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Spacer
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import skywa1ker404.absencium.ui.components.ExpressiveChip
import skywa1ker404.absencium.ui.components.hueFor
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
import com.ramcosta.composedestinations.generated.destinations.PlannerScreenDestination
import com.ramcosta.composedestinations.generated.destinations.AttendanceInsightsScreenDestination
import androidx.compose.material3.toShape
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

/** The student's profile as a page: a banner, the picture and name, tabs, and attendance, statistics and subjects with their shortcuts. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
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

    val rows = remember(data) { data?.subjectAttendance().orEmpty() }
    val overall = remember(rows) { rows.overall() }
    val details = listOfNotNull(loaded.profileCourse.takeIf { it.isNotBlank() }, loaded.profileYear.takeIf { it.isNotBlank() }).joinToString(", ")
    var tab by rememberSaveable { mutableStateOf(0) }
    val tabs = listOf("Overview", "Statistics", "Subjects")

    ScreenScaffold(
        title = "Profile",
        onBack = { navigator.navigateUp() },
        customTopBar = {
            // Back on the left; Absencium AI and Settings as small round icons on the right.
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RoundIcon(AppIcons.ArrowBack, "Back") { navigator.navigateUp() }
                Spacer(Modifier.weight(1f))
                RoundIcon(AppIcons.AutoAwesome, "Absencium AI") { navigator.navigate(AiSettingsScreenDestination) }
                RoundIcon(AppIcons.Settings, "Settings") { navigator.navigate(SettingsHubScreenDestination) }
            }
        },
    ) {
        // Banner with the picture overlapping its lower edge and the edit button under it on the right.
        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 52.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Brush.linearGradient(listOf(colors.primaryContainer, colors.tertiaryContainer)))
                    .drawBehind {
                        // Two soft circles give the band some life without a picture.
                        drawCircle(colors.onPrimaryContainer.copy(alpha = 0.07f), radius = size.height * 0.9f, center = Offset(size.width * 0.85f, size.height * 0.1f))
                        drawCircle(colors.onTertiaryContainer.copy(alpha = 0.06f), radius = size.height * 0.6f, center = Offset(size.width * 0.2f, size.height * 1.05f))
                    },
            )
            AvatarPicture(
                fileName = loaded.profileAvatar,
                initials = loaded.displayName,
                size = 104,
                shape = androidx.compose.material3.MaterialShapes.Cookie9Sided.toShape(),
                modifier = Modifier.align(Alignment.TopStart).offset(x = 20.dp, y = 140.dp - 52.dp),
            )
            RoundIcon(
                AppIcons.Edit, "Edit profile",
                modifier = Modifier.align(Alignment.TopEnd).offset(y = 140.dp + 6.dp),
            ) { navigator.navigate(com.ramcosta.composedestinations.generated.destinations.EditProfileScreenDestination) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(
                text = loaded.displayName.ifBlank { "Add your name" },
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                if (details.isNotBlank()) InfoPill(details)
                if (loaded.profileCollege.isNotBlank()) InfoPill(loaded.profileCollege)
                if (details.isBlank() && loaded.profileCollege.isBlank()) InfoPill("Add your course and college")
            }
        }

        // Tabs.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            tabs.forEachIndexed { index, label ->
                ExpressiveChip(selected = tab == index, onClick = { tab = index }, label = { Text(label) })
            }
        }

        val percent = overall.percent
        val notesCount = vault?.notes?.count { it.status != NoteStatus.DRAFT } ?: 0
        val today = LocalDate.now()
        AnimatedContent(
            targetState = tab,
            transitionSpec = { fadeIn(androidx.compose.animation.core.tween(160)) togetherWith fadeOut(androidx.compose.animation.core.tween(100)) },
            label = "profileTab",
        ) { page ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (page) {
                    0 -> {
                        AppCard(modifier = Modifier.fillMaxWidth(), onClick = { navigator.navigate(AttendanceInsightsScreenDestination) }) {
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
                        val upcoming = data?.exams.orEmpty().filter { !it.date.isBefore(today) }.sortedBy { it.date }.take(3)
                        if (upcoming.isNotEmpty()) {
                            SectionLink("Upcoming exams") { navigator.navigate(PlannerScreenDestination) }
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
                        val withSyllabus = rows.map { it.subject }.filter { subject -> syllabus.any { it.subjectId == subject.id } }
                        if (withSyllabus.isNotEmpty()) {
                            SectionLink("Syllabus") { navigator.navigate(com.ramcosta.composedestinations.generated.destinations.SyllabusOverviewScreenDestination) }
                            AppCard(modifier = Modifier.fillMaxWidth()) {
                                withSyllabus.forEachIndexed { index, subject ->
                                    if (index > 0) SettingDivider()
                                    val progress = SyllabusTree.progress(syllabus.filter { it.subjectId == subject.id }, null)
                                    ProgressRow(subject.name, null, progress, "${(progress * 100).roundToInt()}%", subjectColor(subject.id))
                                }
                            }
                        }
                    }
                    1 -> {
                        // Three big numbers with small capital labels, a divider, then a capsule bar and a legend.
                        SectionLink("Statistics") { navigator.navigate(AttendanceInsightsScreenDestination) }
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                BigNumber(percent?.let { "${it.roundToInt()}%" } ?: "–", "ATTENDANCE", "of ${overall.held} held")
                                BigNumber("$notesCount", "NOTES", "${vault?.guides?.size ?: 0} materials")
                                BigNumber("${rows.size}", "SUBJECTS", "${data?.exams.orEmpty().count { !it.date.isBefore(today) }} exams ahead")
                            }
                            SettingDivider()
                            val present = overall.present
                            val absent = (overall.held - overall.present).coerceAtLeast(0)
                            val cancelled = rows.sumOf { it.cancelled }
                            CapsuleBar(
                                listOf(
                                    Triple("Present", present, colors.primary),
                                    Triple("Absent", absent, colors.error),
                                    Triple("Cancelled", cancelled, colors.outline),
                                ),
                            )
                        }
                        SectionLink("Last 12 weeks") { navigator.navigate(AttendanceInsightsScreenDestination) }
                        AppCard(modifier = Modifier.fillMaxWidth()) { HeatMap(data?.attendance.orEmpty(), today) }
                    }
                    else -> {
                        SectionLink("Top subjects") { navigator.navigate(AttendanceInsightsScreenDestination) }
                        if (rows.isEmpty()) {
                            AppCard(modifier = Modifier.fillMaxWidth()) {
                                Text("Add subjects and log lectures to see them here", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                            }
                        }
                        rows.sortedByDescending { it.percent ?: -1.0 }.take(6).chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                pair.forEach { item ->
                                    val hue = hueFor(item.subject.id)
                                    val p = item.percent
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(28.dp))
                                            .background(hue.container)
                                            .clickable { navigator.navigate(com.ramcosta.composedestinations.generated.destinations.SubjectFolderScreenDestination(subjectId = item.subject.id)) }
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Text(p?.let { "${it.roundToInt()}%" } ?: "–", style = MaterialTheme.typography.displaySmall, color = hue.onContainer)
                                        Text(item.subject.name, style = MaterialTheme.typography.titleSmall, color = hue.onContainer, maxLines = 1)
                                        Text("${item.present} of ${item.held} lectures", style = MaterialTheme.typography.bodySmall, color = hue.onContainer.copy(alpha = 0.75f))
                                    }
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A small round icon button (back, Absencium AI, Settings, Edit). */
@Composable
private fun RoundIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
    }
}

/** A rounded line of detail under the name. */
@Composable
private fun InfoPill(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        maxLines = 1,
        modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

/** A section title with an arrow that opens the page it belongs to. */
@Composable
private fun SectionLink(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(4.dp).height(20.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, modifier = Modifier.padding(start = 10.dp).weight(1f))
        Icon(AppIcons.ChevronRight, contentDescription = "Open", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A large number, its spaced capital label and a caption. */
@Composable
private fun BigNumber(value: String, label: String, caption: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(value, style = MaterialTheme.typography.headlineLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** One capsule split into the given parts (label, count, colour), with a dot legend under it. */
@Composable
private fun CapsuleBar(parts: List<Triple<String, Int, Color>>) {
    val total = parts.sumOf { it.second }.coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(14.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            parts.filter { it.second > 0 }.forEach { (_, count, color) ->
                Box(modifier = Modifier.weight(count.toFloat() / total).fillMaxHeight().background(color))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            parts.forEach { (label, count, color) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                    Text("$label $count", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** Twelve weeks of days as small squares: present is the primary colour, absent the error colour, nothing logged is faint. */
@Composable
private fun HeatMap(records: List<skywa1ker404.absencium.data.db.AttendanceEntity>, today: LocalDate) {
    val colors = MaterialTheme.colorScheme
    val byDate = remember(records) {
        records.groupBy { it.date }.mapValues { (_, list) ->
            val absent = list.count { it.status == skywa1ker404.absencium.data.db.AttendanceStatus.ABSENT }
            val present = list.count { it.status == skywa1ker404.absencium.data.db.AttendanceStatus.PRESENT }
            when {
                absent > 0 -> 2
                present > 0 -> 1
                else -> 0
            }
        }
    }
    val start = today.minusDays((today.dayOfWeek.value % 7).toLong()).minusWeeks(11)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        repeat(12) { week ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                repeat(7) { day ->
                    val date = start.plusDays((week * 7 + day).toLong())
                    val level = if (date.isAfter(today)) -1 else byDate[date] ?: 0
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when (level) {
                                    1 -> colors.primary
                                    2 -> colors.error
                                    0 -> colors.surfaceContainerHighest.copy(alpha = 0.7f)
                                    else -> Color.Transparent
                                },
                            ),
                    )
                }
            }
        }
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

