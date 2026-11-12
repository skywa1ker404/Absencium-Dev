package skywa1ker404.absencium.ui.study

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import skywa1ker404.absencium.appContainer
import skywa1ker404.absencium.data.db.EventEntity
import skywa1ker404.absencium.data.db.EventRepeat
import skywa1ker404.absencium.data.db.Priority
import skywa1ker404.absencium.data.db.StudyGoalEntity
import skywa1ker404.absencium.domain.AgendaEntry
import skywa1ker404.absencium.domain.AgendaKind
import skywa1ker404.absencium.domain.EventCategories
import skywa1ker404.absencium.domain.agendaFor
import skywa1ker404.absencium.domain.hm
import skywa1ker404.absencium.domain.parseTimeInput
import skywa1ker404.absencium.domain.pretty
import skywa1ker404.absencium.ui.components.AnimatedPopup
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.AppTextField
import skywa1ker404.absencium.ui.components.DatePickerPopup
import skywa1ker404.absencium.ui.components.ExpressiveChip
import skywa1ker404.absencium.ui.components.Hue
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.components.PopupBody
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.components.bounceIn
import skywa1ker404.absencium.ui.components.drawHatch
import skywa1ker404.absencium.ui.components.hueFor
import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.theme.LocalQuality
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
private fun kindHue(kind: AgendaKind): Hue {
    val c = MaterialTheme.colorScheme
    return when (kind) {
        AgendaKind.LECTURE -> hueFor(0)
        AgendaKind.EVENT -> hueFor(1)
        AgendaKind.TODO -> hueFor(3)
        AgendaKind.HOLIDAY -> hueFor(4)
        AgendaKind.EXAM -> Hue(c.errorContainer, c.onErrorContainer, c.error)
    }
}

/** My Calendar: a week strip, the selected day as a timeline of pastel cards, and a to-do list with progress. */
@Destination<RootGraph>
@Composable
fun PlannerScreen(navigator: DestinationsNavigator) {
    val container = LocalContext.current.applicationContext.appContainer
    val goals by container.database.studyGoalDao().observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val events by container.database.eventDao().observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val data by container.calendarRepository.data.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val today = LocalDate.now()
    val subjects = data?.subjects?.filter { it.isActive }.orEmpty()
    val askPermission = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }

    var selected by remember { mutableStateOf(today) }
    var tab by remember { mutableIntStateOf(0) }
    var filter by remember { mutableIntStateOf(0) }
    var sheet by remember { mutableIntStateOf(0) }
    var aiBusy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    // The add form.
    var isEvent by remember { mutableStateOf(true) }
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Event") }
    var custom by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(today) }
    var time by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf(EventRepeat.NONE) }
    var remind by remember { mutableStateOf<Int?>(null) }
    var subjectId by remember { mutableStateOf<Long?>(null) }
    var priority by remember { mutableStateOf(Priority.NORMAL) }

    val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val days = remember(weekStart) { (0L until 21L).map { weekStart.plusDays(it) } }
    val agendas = remember(days, data, events, goals) {
        val d = data
        if (d == null) emptyMap() else days.associateWith { agendaFor(it, d, events, goals) }
    }
    val entries = agendas[selected] ?: data?.let { agendaFor(selected, it, events, goals) }.orEmpty()

    fun openAdd(event: Boolean) {
        isEvent = event
        date = selected
        sheet = 1
    }

    ScreenScaffold(
        title = "My Calendar", onBack = { navigator.navigateUp() }, largeTitle = true,
        overlay = {
            AnimatedPopup(sheet.takeIf { it != 0 }) { step ->
                if (step == 2) {
                    DatePickerPopup(initial = date, onPicked = { date = it; sheet = 1 }, onDismiss = { sheet = 1 })
                } else {
                    PopupBody(title = "New", onClose = { sheet = 0 }) {
                        Column(modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ExpressiveChip(selected = isEvent, onClick = { isEvent = true }, label = { Text("Event") })
                                ExpressiveChip(selected = !isEvent, onClick = { isEvent = false }, label = { Text("To-do") })
                            }
                            AppTextField(value = title, onValueChange = { title = it }, label = { Text(if (isEvent) "Title" else "What do you want to do?") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                ExpressiveChip(selected = date == today, onClick = { date = today }, label = { Text("Today") })
                                ExpressiveChip(selected = date == today.plusDays(1), onClick = { date = today.plusDays(1) }, label = { Text("Tomorrow") })
                                PillButton(text = date.pretty(), tonal = true, compact = true, onClick = { sheet = 2 })
                            }
                            if (isEvent) {
                                AppTextField(
                                    value = time, onValueChange = { time = it }, label = { Text("Time (optional, like 14:30)") }, singleLine = true,
                                    isError = time.isNotBlank() && parseTimeInput(time) == null, modifier = Modifier.fillMaxWidth(),
                                )
                                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    EventCategories.forEach { c -> ExpressiveChip(selected = category == c && custom.isBlank(), onClick = { category = c; custom = "" }, label = { Text(c) }) }
                                }
                                AppTextField(value = custom, onValueChange = { custom = it }, label = { Text("Or your own type") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                Text("Repeats", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    EventRepeat.entries.forEach { r ->
                                        ExpressiveChip(selected = repeat == r, onClick = { repeat = r }, label = { Text(if (r == EventRepeat.NONE) "Never" else r.name.lowercase().replaceFirstChar { it.uppercase() }) })
                                    }
                                }
                                Text("Remind me", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf<Pair<Int?, String>>(null to "No", 0 to "At the time", 10 to "10 min before", 60 to "1 hour before", 1440 to "1 day before").forEach { (m, label) ->
                                        ExpressiveChip(selected = remind == m, onClick = { remind = m }, label = { Text(label) })
                                    }
                                }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Priority.entries.reversed().forEach { p ->
                                        ExpressiveChip(selected = priority == p, onClick = { priority = p }, label = { Text(p.name.lowercase().replaceFirstChar { it.uppercase() }) })
                                    }
                                }
                            }
                            if (subjects.isNotEmpty()) {
                                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ExpressiveChip(selected = subjectId == null, onClick = { subjectId = null }, label = { Text("No subject") })
                                    subjects.forEach { s -> ExpressiveChip(selected = subjectId == s.id, onClick = { subjectId = s.id }, label = { Text(s.name) }) }
                                }
                            }
                            if (isEvent) AppTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth())
                            PillButton(
                                text = "Save",
                                enabled = title.isNotBlank() && (!isEvent || time.isBlank() || parseTimeInput(time) != null),
                                onClick = {
                                    if (isEvent) {
                                        val event = EventEntity(
                                            title = title.trim(), date = date, startTime = parseTimeInput(time),
                                            category = custom.trim().ifBlank { category }.take(30), notes = notes.trim().ifBlank { null },
                                            subjectId = subjectId, repeat = repeat, remindMinutes = remind,
                                        )
                                        scope.launch { container.database.eventDao().insert(event) }
                                        if (remind != null && android.os.Build.VERSION.SDK_INT >= 33 && !skywa1ker404.absencium.notify.ReminderScheduler.canNotify(context)) {
                                            askPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    } else {
                                        val goal = StudyGoalEntity(subjectId = subjectId, title = title.trim(), targetDate = date, priority = priority)
                                        scope.launch { container.database.studyGoalDao().insert(goal) }
                                    }
                                    selected = date
                                    title = ""; notes = ""; time = ""; custom = ""
                                    sheet = 0
                                },
                            )
                        }
                    }
                }
            }
        },
    ) {
        // Header: the chosen day and the round add button.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    selected.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (entries.isEmpty()) "A free day" else "${entries.size} ${if (entries.size == 1) "thing" else "things"} on this day",
                    style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                )
            }
            val q = LocalQuality.current
            val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val pressScale by animateFloatAsState(if (pressed) 0.88f else 1f, q.spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "addPress")
            Box(
                modifier = Modifier.size(52.dp).graphicsLayer { scaleX = pressScale; scaleY = pressScale }.clip(CircleShape).background(colors.primary)
                    .clickable(interactionSource = interaction, indication = androidx.compose.foundation.LocalIndication.current) { openAdd(tab == 0) }
                    .semantics { contentDescription = "Add" },
                contentAlignment = Alignment.Center,
            ) { Icon(AppIcons.Add, contentDescription = null, tint = colors.onPrimary) }
        }

        // The week strip.
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            days.forEach { day ->
                DayCapsule(day, selected = day == selected, isToday = day == today, items = agendas[day]?.size ?: 0) { selected = day }
            }
        }

        // Schedule or to-do.
        val openGoals = goals.count { !it.done }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExpressiveChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("Schedule") })
            ExpressiveChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text(if (openGoals > 0) "To-do · $openGoals" else "To-do") })
        }

        if (tab == 0) {
            if (entries.isEmpty()) {
                AppCard(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), container = hueFor(2).container) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        ShapeBadge(AppIcons.CalendarMonth, BadgeShape.FLOWER, hueFor(2), size = 56.dp)
                        Column {
                            Text("Nothing planned", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = hueFor(2).onContainer)
                            Text("Tap + to add an event or a to-do for this day.", style = MaterialTheme.typography.bodySmall, color = hueFor(2).onContainer.copy(alpha = 0.8f))
                        }
                    }
                }
            }
            val now = LocalTime.now()
            val showNow = selected == today && entries.any { it.start != null }
            val nowIndex = if (showNow) (entries.indexOfFirst { it.start != null && it.start.isAfter(now) }.takeIf { it >= 0 } ?: entries.size) else -1
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
                entries.forEachIndexed { index, entry ->
                    if (index == nowIndex) NowMarker()
                    TimelineRow(entry, delayMs = (index * 50).coerceAtMost(300)) {
                        goals.firstOrNull { it.id == entry.goalId }?.let { g -> scope.launch { container.database.studyGoalDao().update(g.copy(done = !g.done)) } }
                    }
                }
                if (nowIndex == entries.size && showNow) NowMarker()
            }
        } else {
            val done = goals.count { it.done }
            val progressHue = hueFor(0)
            AppCard(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), container = progressHue.container) {
                Text("Progress", style = MaterialTheme.typography.labelLarge, color = progressHue.onContainer.copy(alpha = 0.75f))
                Text(
                    if (goals.isEmpty()) "No to-dos yet" else "$done of ${goals.size} done",
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = progressHue.onContainer,
                )
                CapsuleProgress(if (goals.isEmpty()) 0f else done.toFloat() / goals.size, progressHue.accent, Modifier.padding(top = 10.dp))
                PillButton(
                    text = "Plan with AI", tonal = true, compact = true, loading = aiBusy, enabled = !aiBusy, modifier = Modifier.padding(top = 12.dp),
                    onClick = {
                        aiBusy = true
                        message = null
                        scope.launch {
                            val result = container.aiTasks.run(
                                "Study plan",
                                "Make a study to-do list for the next two weeks. Use app_overview, list_exams, get_syllabus and list_study_goals first so nothing repeats. Add one to-do per study block with add_study_goal (a short title, the subject and a date before each exam). Do not add more than 12.",
                            )
                            message = result.error ?: "Done. The new to-dos are in the list."
                            aiBusy = false
                        }
                    },
                )
                message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = progressHue.onContainer, modifier = Modifier.padding(top = 8.dp)) }
            }
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Open", "Done", "High").forEachIndexed { i, label -> ExpressiveChip(selected = filter == i, onClick = { filter = i }, label = { Text(label) }) }
            }
            val shown = goals.filter {
                when (filter) { 1 -> !it.done; 2 -> it.done; 3 -> it.priority == Priority.HIGH && !it.done; else -> true }
            }
            if (shown.isEmpty()) HintText("Nothing here.", modifier = Modifier.padding(top = 8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
                shown.forEachIndexed { index, goal ->
                    val hue = hueFor(goal.subjectId ?: goal.id)
                    val subjectName = data?.subjects?.firstOrNull { it.id == goal.subjectId }?.name
                    val late = goal.targetDate?.isBefore(today) == true && !goal.done
                    TodoCard(
                        goal = goal, hue = hue, delayMs = (index * 40).coerceAtMost(300),
                        detail = listOfNotNull(subjectName, goal.targetDate?.pretty()).joinToString(" · "),
                        tag = when { late -> "Overdue"; goal.priority == Priority.HIGH && !goal.done -> "High"; else -> null },
                        onToggle = { scope.launch { container.database.studyGoalDao().update(goal.copy(done = !goal.done)) } },
                        onDelete = { scope.launch { container.database.studyGoalDao().delete(goal.id) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCapsule(date: LocalDate, selected: Boolean, isToday: Boolean, items: Int, onClick: () -> Unit) {
    val q = LocalQuality.current
    val colors = MaterialTheme.colorScheme
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else if (selected) 1.04f else 1f,
        animationSpec = q.spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "dayScale",
    )
    val container by animateColorAsState(if (selected) colors.primaryContainer else colors.surfaceContainerLow, q.spring(), label = "dayBg")
    val circle by animateColorAsState(if (selected) colors.primary else if (isToday) colors.secondaryContainer else colors.surfaceContainerHighest, q.spring(), label = "dayCircle")
    Column(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .width(54.dp).height(92.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(container)
            .then(if (selected) Modifier.border(2.dp, colors.primary, RoundedCornerShape(30.dp)) else Modifier)
            .clickable(interactionSource = interaction, indication = androidx.compose.foundation.LocalIndication.current, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), style = MaterialTheme.typography.labelLarge, color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant)
        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(circle), contentAlignment = Alignment.Center) {
            Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = if (selected) colors.onPrimary else colors.onSurface)
        }
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (items > 0) (if (selected) colors.primary else colors.tertiary) else androidx.compose.ui.graphics.Color.Transparent))
    }
}

@Composable
private fun NowMarker() {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Now", style = MaterialTheme.typography.labelMedium, color = colors.onPrimary, modifier = Modifier.clip(CircleShape).background(colors.primary).padding(horizontal = 10.dp, vertical = 3.dp))
        Box(modifier = Modifier.weight(1f).height(2.dp).clip(CircleShape).background(colors.primary.copy(alpha = 0.5f)))
    }
}

@Composable
private fun TimelineRow(entry: AgendaEntry, delayMs: Int, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val hue = kindHue(entry.kind)
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).bounceIn(delayMs), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(modifier = Modifier.width(48.dp).padding(top = 12.dp)) {
            if (entry.start != null) {
                Text(entry.start.hm(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                entry.end?.let { Text(it.hm(), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
            } else {
                Text("All day", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            }
        }
        AppCard(
            modifier = Modifier.weight(1f), container = hue.container,
            onClick = if (entry.kind == AgendaKind.TODO) onToggle else null,
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Box(modifier = Modifier.fillMaxWidth().then(if (entry.kind == AgendaKind.HOLIDAY) Modifier.clip(MaterialTheme.shapes.medium).drawBehind { drawHatch(hue.accent.copy(alpha = 0.25f)) } else Modifier)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.width(5.dp).height(40.dp).clip(CircleShape).background(hue.accent))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = hue.onContainer,
                            textDecoration = if (entry.done) TextDecoration.LineThrough else null,
                        )
                        Text(entry.detail, style = MaterialTheme.typography.bodySmall, color = hue.onContainer.copy(alpha = 0.78f))
                    }
                    if (entry.kind == AgendaKind.TODO) {
                        Box(
                            modifier = Modifier.size(26.dp).clip(RoundedCornerShape(9.dp))
                                .then(if (entry.done) Modifier.background(hue.accent) else Modifier.border(2.dp, hue.accent, RoundedCornerShape(9.dp))),
                            contentAlignment = Alignment.Center,
                        ) { if (entry.done) Icon(AppIcons.Check, contentDescription = "Done", tint = hue.container, modifier = Modifier.size(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoCard(goal: StudyGoalEntity, hue: Hue, delayMs: Int, detail: String, tag: String?, onToggle: () -> Unit, onDelete: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val q = LocalQuality.current
    val fill by animateColorAsState(if (goal.done) hue.accent else androidx.compose.ui.graphics.Color.Transparent, q.spring(), label = "todoFill")
    AppCard(modifier = Modifier.fillMaxWidth().bounceIn(delayMs), container = hue.container, onClick = onToggle, shape = MaterialTheme.shapes.extraLarge) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(28.dp).clip(RoundedCornerShape(10.dp)).background(fill).border(2.dp, hue.accent, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) { if (goal.done) Icon(AppIcons.Check, contentDescription = "Done", tint = hue.container, modifier = Modifier.size(18.dp)) }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    goal.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = hue.onContainer.copy(alpha = if (goal.done) 0.6f else 1f),
                    textDecoration = if (goal.done) TextDecoration.LineThrough else null,
                )
                if (detail.isNotEmpty()) Text(detail, style = MaterialTheme.typography.bodySmall, color = hue.onContainer.copy(alpha = 0.78f))
            }
            if (tag != null) {
                Text(
                    tag, style = MaterialTheme.typography.labelMedium,
                    color = if (tag == "Overdue") colors.onError else hue.container,
                    modifier = Modifier.clip(CircleShape).background(if (tag == "Overdue") colors.error else hue.accent).padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onDelete), contentAlignment = Alignment.Center) {
                Icon(AppIcons.Close, contentDescription = "Delete", tint = hue.onContainer.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
            }
        }
    }
}
