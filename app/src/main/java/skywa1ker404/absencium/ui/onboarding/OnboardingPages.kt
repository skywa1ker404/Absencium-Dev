package skywa1ker404.absencium.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.rerere.rikkahub.ui.components.dots.Icon
import skywa1ker404.absencium.appContainer
import skywa1ker404.absencium.data.settings.AppSettings
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.AppTextField
import skywa1ker404.absencium.ui.components.DurationWheels
import skywa1ker404.absencium.ui.components.ExpressiveChip
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.components.SectionTitle
import skywa1ker404.absencium.ui.components.TimeWheels
import skywa1ker404.absencium.ui.components.hueFor
import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.settings.SettingsViewModel
import skywa1ker404.absencium.ui.study.BadgeShape
import skywa1ker404.absencium.ui.study.badgeShape
import skywa1ker404.absencium.ui.theme.CircleShape
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

// ---------------------------------------------------------------------------------------------------------------------
// Page 0: the welcome
// ---------------------------------------------------------------------------------------------------------------------

/**
 * The first thing a student sees: a few big shapes in the theme's colours behind a rounded sheet with a two-line title. The Next
 * button of the setup bar plays the part of "Get started".
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WelcomePage() {
    val colors = MaterialTheme.colorScheme
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val spin by animateFloatAsState(targetValue = if (shown) 1f else 0f, animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.5f, stiffness = 60f), label = "welcomeShapes")
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier.size(210.dp).rotate(-12f * spin).clip(badgeShape(BadgeShape.COOKIE9)).background(colors.primaryContainer),
            )
            Box(
                modifier = Modifier.align(Alignment.TopEnd).offset(x = (-12).dp, y = 8.dp).size(78.dp).rotate(20f * spin)
                    .clip(badgeShape(BadgeShape.CLOVER)).background(colors.tertiaryContainer),
            )
            Box(
                modifier = Modifier.align(Alignment.BottomStart).offset(x = 12.dp, y = (-6).dp).size(64.dp).rotate(-8f * spin)
                    .clip(badgeShape(BadgeShape.SUNNY)).background(colors.secondaryContainer),
            )
            Box(
                modifier = Modifier.size(104.dp).clip(badgeShape(BadgeShape.BURST)).background(colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(AppIcons.School, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(46.dp))
            }
        }
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Your classes,\nyour notes, your phone.", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "Absencium keeps your attendance, timetable, exams and notes on this phone. No account, no ads, no internet needed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    "Setup takes a minute and you leave with your subjects and timetable already in.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// Page 1: you
// ---------------------------------------------------------------------------------------------------------------------

/** Name, course and year. Everything is optional and stays on the phone; it fills the Profile page. */
@Composable
fun YouPage(settings: AppSettings, viewModel: SettingsViewModel) {
    var name by rememberSaveable { mutableStateOf(settings.displayName) }
    var course by rememberSaveable { mutableStateOf(settings.profileCourse) }
    var year by rememberSaveable { mutableStateOf(settings.profileYear) }
    // Saved a moment after typing stops, so each key press is not a write.
    LaunchedEffect(name) { delay(400); if (name != settings.displayName) viewModel.setDisplayName(name) }
    LaunchedEffect(course, year) {
        delay(400)
        if (course != settings.profileCourse || year != settings.profileYear) viewModel.setProfileDetails(course, year, settings.profileCollege)
    }
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppTextField(
                value = name, onValueChange = { name = it.take(40) }, singleLine = true,
                label = { Text("Your name") }, modifier = Modifier.fillMaxWidth(),
            )
            AppTextField(
                value = course, onValueChange = { course = it.take(60) }, singleLine = true,
                label = { Text("Course") }, placeholder = { Text("For example B.Sc. Computer Science") }, modifier = Modifier.fillMaxWidth(),
            )
            AppTextField(
                value = year, onValueChange = { year = it.take(30) }, singleLine = true,
                label = { Text("Year or semester") }, placeholder = { Text("For example Semester 3") }, modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    HintText("All optional. This stays on your phone.")
}

// ---------------------------------------------------------------------------------------------------------------------
// Page: subjects
// ---------------------------------------------------------------------------------------------------------------------

private val SUGGESTED_SUBJECTS = listOf("Mathematics", "Physics", "Chemistry", "Biology", "English", "Computer Science", "Economics", "History")

/** Adds the student's subjects straight into the database, with quick suggestions; each can be removed again. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubjectsPage() {
    val container = LocalContext.current.applicationContext.appContainer
    val subjects by container.subjectRepository.subjects.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    var text by rememberSaveable { mutableStateOf("") }
    var problem by remember { mutableStateOf<String?>(null) }

    fun add(name: String) {
        scope.launch {
            problem = container.subjectRepository.add(name)
            if (problem == null && name == text.trim()) text = ""
        }
    }

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AppTextField(
                    value = text, onValueChange = { text = it.take(60); problem = null }, singleLine = true,
                    placeholder = { Text("Subject name") }, modifier = Modifier.weight(1f),
                )
                PillButton(text = "Add", onClick = { add(text) }, enabled = text.isNotBlank(), compact = true)
            }
            problem?.let { HintText(it) }
            val missing = SUGGESTED_SUBJECTS.filter { s -> subjects.none { it.name.equals(s, ignoreCase = true) } }
            if (missing.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    missing.forEach { s -> ExpressiveChip(selected = false, onClick = { add(s) }, label = { Text("+ $s") }) }
                }
            }
        }
    }
    if (subjects.isNotEmpty()) {
        SectionTitle("Your subjects")
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                subjects.forEach { subject ->
                    val hue = hueFor(subject.id)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(hue.accent))
                        Text(subject.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(
                            AppIcons.Close, contentDescription = "Remove ${subject.name}", tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp).clip(CircleShape).clickable { scope.launch { container.subjectRepository.delete(subject) } },
                        )
                    }
                }
            }
        }
    }
    HintText("Add the ones you attend now. More can be added later in the Vault.")
}

// ---------------------------------------------------------------------------------------------------------------------
// Page: timetable
// ---------------------------------------------------------------------------------------------------------------------

private val QUICK_MINUTES = listOf(45, 60, 90, 120)

/** One lecture slot at a time: a subject, the days it happens on, the time and the length. Added slots are listed below the form. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimetablePage(settings: AppSettings) {
    val container = LocalContext.current.applicationContext.appContainer
    val subjects by container.subjectRepository.subjects.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    val is24 = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    val timeFormat = remember(is24) { java.time.format.DateTimeFormatter.ofPattern(if (is24) "HH:mm" else "h:mm a") }

    var subjectId by rememberSaveable { mutableStateOf(-1L) }
    var days by rememberSaveable { mutableStateOf(setOf<Int>()) }
    var startMinutes by rememberSaveable { mutableStateOf(9 * 60) }
    var duration by rememberSaveable { mutableStateOf(60) }
    var custom by rememberSaveable { mutableStateOf(false) }
    var room by rememberSaveable { mutableStateOf("") }
    var added by rememberSaveable { mutableStateOf(listOf<String>()) }

    if (subjects.isEmpty()) {
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Text("Add your subjects on the previous page first.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
        }
        HintText("You can also skip this page and build the timetable in the app: Attendance, then Timetable.")
        return
    }
    val chosen = subjects.firstOrNull { it.id == subjectId } ?: subjects.first()
    val weekdays = DayOfWeek.values().filter {
        !(it == DayOfWeek.SATURDAY && settings.saturdayHoliday) && !(it == DayOfWeek.SUNDAY && settings.sundayHoliday)
    }
    val start = LocalTime.of(startMinutes / 60, startMinutes % 60)
    val end = start.plusMinutes(duration.toLong()).let { if (start.plusMinutes(duration.toLong()).isBefore(start)) LocalTime.of(23, 59) else it }

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Subject", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                subjects.forEach { s -> ExpressiveChip(selected = s.id == chosen.id, onClick = { subjectId = s.id }, label = { Text(s.name) }) }
            }
            Text("Days", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                weekdays.forEach { d ->
                    ExpressiveChip(
                        selected = d.value in days,
                        onClick = { days = if (d.value in days) days - d.value else days + d.value },
                        label = { Text(d.getDisplayName(TextStyle.SHORT, Locale.getDefault())) },
                    )
                }
            }
            Text("Starts at ${start.format(timeFormat)}", style = MaterialTheme.typography.labelLarge)
            TimeWheels(value = start, is24Hour = is24, onChange = { startMinutes = it.hour * 60 + it.minute })
            Text("Length", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                QUICK_MINUTES.forEach { m ->
                    ExpressiveChip(selected = !custom && duration == m, onClick = { custom = false; duration = m }, label = { Text(if (m % 60 == 0) "${m / 60} h" else "$m min") })
                }
                ExpressiveChip(selected = custom, onClick = { custom = true }, label = { Text("Custom") })
            }
            AnimatedVisibility(visible = custom) { DurationWheels(minutes = duration, onChange = { duration = it.coerceAtLeast(1) }) }
            HintText("Ends at ${end.format(timeFormat)}")
            AppTextField(
                value = room, onValueChange = { room = it.take(30) }, singleLine = true,
                label = { Text("Room (optional)") }, modifier = Modifier.fillMaxWidth(),
            )
            PillButton(
                text = if (days.size > 1) "Add to ${days.size} days" else "Add lecture",
                enabled = days.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val picked = weekdays.filter { it.value in days }
                    scope.launch {
                        container.calendarRepository.addSlots(picked, chosen.id, start, end, room.trim().ifBlank { null })
                        added = added + "${chosen.name}  ·  ${picked.joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }}  ·  ${start.format(timeFormat)}"
                        days = emptySet()
                    }
                },
            )
        }
    }
    if (added.isNotEmpty()) {
        SectionTitle("Added")
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                added.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
    HintText("Different times on different days? Add each as its own lecture. You can also import with AI or a calendar file later.")
}
