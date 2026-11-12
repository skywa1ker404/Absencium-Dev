package skywa1ker404.absencium.ui.study

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import skywa1ker404.absencium.appContainer
import skywa1ker404.absencium.data.db.StudyGoalEntity
import skywa1ker404.absencium.data.db.EventEntity
import skywa1ker404.absencium.data.db.EventRepeat
import skywa1ker404.absencium.data.db.Priority
import skywa1ker404.absencium.domain.EventCategories
import skywa1ker404.absencium.domain.hm
import skywa1ker404.absencium.domain.occurrences
import skywa1ker404.absencium.domain.parseTimeInput
import skywa1ker404.absencium.ui.components.AnimatedPopup
import skywa1ker404.absencium.ui.components.DatePickerPopup
import skywa1ker404.absencium.domain.lecturesOn
import skywa1ker404.absencium.domain.pretty
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.AppTextField
import skywa1ker404.absencium.ui.components.ExpressiveChip
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.components.SectionTitle
import skywa1ker404.absencium.ui.components.SettingDivider
import skywa1ker404.absencium.ui.icons.AppIcons
import java.time.LocalDate

private data class AgendaItem(val date: LocalDate, val time: String?, val title: String, val kind: String)

/** A to-do list (study goals with an optional subject and day) and an agenda of what is coming up. */
@Destination<RootGraph>
@Composable
fun PlannerScreen(navigator: DestinationsNavigator) {
    val container = LocalContext.current.applicationContext.appContainer
    val goals by container.database.studyGoalDao().observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val data by container.calendarRepository.data.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val today = LocalDate.now()
    val subjects = data?.subjects?.filter { it.isActive }.orEmpty()
    val names = data?.subjects?.associate { it.id to it.name }.orEmpty()
    val events by container.database.eventDao().observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    var priority by remember { mutableStateOf(Priority.NORMAL) }
    var showEventForm by remember { mutableStateOf(false) }
    var evTitle by remember { mutableStateOf("") }
    var evCategory by remember { mutableStateOf("Event") }
    var evCustom by remember { mutableStateOf("") }
    var evDate by remember { mutableStateOf(today) }
    var evTime by remember { mutableStateOf("") }
    var evNotes by remember { mutableStateOf("") }
    var evRepeat by remember { mutableStateOf(EventRepeat.NONE) }
    var evRemind by remember { mutableStateOf<Int?>(null) }
    var evSubject by remember { mutableStateOf<Long?>(null) }
    var pickingDate by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val askPermission = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }

    var tab by remember { mutableStateOf(0) }
    var title by remember { mutableStateOf("") }
    var subjectId by remember { mutableStateOf<Long?>(null) }
    var dayChoice by remember { mutableStateOf(0) }
    var aiBusy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    ScreenScaffold(
        title = "To-do & upcoming", onBack = { navigator.navigateUp() }, largeTitle = true,
        overlay = {
            AnimatedPopup(if (pickingDate) true else null) {
                DatePickerPopup(initial = evDate, onPicked = { evDate = it }, onDismiss = { pickingDate = false })
            }
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExpressiveChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("To-do") })
            ExpressiveChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("Upcoming") })
        }
        if (tab == 0) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppTextField(value = title, onValueChange = { title = it }, label = { Text("What do you want to do?") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("No date", "Today", "Tomorrow", "In a week").forEachIndexed { i, label ->
                            ExpressiveChip(selected = dayChoice == i, onClick = { dayChoice = i }, label = { Text(label) })
                        }
                    }
                    if (subjects.isNotEmpty()) {
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ExpressiveChip(selected = subjectId == null, onClick = { subjectId = null }, label = { Text("No subject") })
                            subjects.forEach { s -> ExpressiveChip(selected = subjectId == s.id, onClick = { subjectId = s.id }, label = { Text(s.name) }) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Priority.entries.reversed().forEach { p ->
                            ExpressiveChip(selected = priority == p, onClick = { priority = p }, label = { Text(p.name.lowercase().replaceFirstChar { it.uppercase() }) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillButton(
                            text = "Add", enabled = title.isNotBlank(),
                            onClick = {
                                val day = when (dayChoice) { 1 -> today; 2 -> today.plusDays(1); 3 -> today.plusDays(7); else -> null }
                                val goal = StudyGoalEntity(subjectId = subjectId, title = title.trim(), targetDate = day, priority = priority)
                                scope.launch { container.database.studyGoalDao().insert(goal) }
                                title = ""
                            },
                        )
                        PillButton(
                            text = "Plan with AI", tonal = true, loading = aiBusy, enabled = !aiBusy,
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
                    }
                    message?.let { HintText(it) }
                }
            }
            if (goals.isEmpty()) HintText("Nothing on your list yet.", modifier = Modifier.padding(top = 8.dp))
            else AppCard(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                goals.forEachIndexed { index, goal ->
                    if (index > 0) SettingDivider()
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier.size(28.dp).clip(CircleShape)
                                .then(if (goal.done) Modifier.background(colors.primary) else Modifier.border(2.dp, colors.outline, CircleShape))
                                .clickable { scope.launch { container.database.studyGoalDao().update(goal.copy(done = !goal.done)) } },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (goal.done) Icon(AppIcons.Check, contentDescription = "Done", tint = colors.onPrimary, modifier = Modifier.size(18.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                goal.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (goal.done) colors.onSurfaceVariant else colors.onSurface,
                                textDecoration = if (goal.done) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                            )
                            val line = listOfNotNull(goal.subjectId?.let { names[it] }, goal.targetDate?.pretty(), if (goal.priority == Priority.HIGH) "High priority" else null).joinToString(" · ")
                            if (line.isNotEmpty()) {
                                val late = goal.targetDate?.isBefore(today) == true && !goal.done
                                Text(line, style = MaterialTheme.typography.bodySmall, color = if (late) colors.error else colors.onSurfaceVariant)
                            }
                        }
                        PillButton(text = "Delete", tonal = true, compact = true, onClick = { scope.launch { container.database.studyGoalDao().delete(goal.id) } })
                    }
                }
            }
        } else {
            val agenda = remember(data, goals, events) {
                val d = data
                buildList {
                    if (d != null) {
                        d.exams.filter { it.deletedAt == null && !it.date.isBefore(today) && it.date.isBefore(today.plusDays(60)) }.forEach {
                            add(AgendaItem(it.date, null, "${names[it.subjectId] ?: "Exam"}: ${it.title}", "Exam"))
                        }
                        d.holidays.filter { it.deletedAt == null && !it.date.isBefore(today) && it.date.isBefore(today.plusDays(60)) }.forEach {
                            add(AgendaItem(it.date, null, it.title, "Holiday"))
                        }
                        for (offset in 0L..3L) {
                            val date = today.plusDays(offset)
                            d.lecturesOn(date).forEach { add(AgendaItem(date, it.slot.startTime.hm(), it.subject.name, "Lecture")) }
                        }
                    }
                    events.forEach { e ->
                        e.occurrences(today, today.plusDays(60)).forEach { day ->
                            add(AgendaItem(day, e.startTime?.hm(), e.title, e.category + (if (e.repeat != EventRepeat.NONE) " · repeats" else "")))
                        }
                    }
                    goals.filter { !it.done && it.targetDate != null && !it.targetDate.isBefore(today) }.forEach {
                        add(AgendaItem(it.targetDate!!, null, it.title, "To-do"))
                    }
                }.sortedWith(compareBy({ it.date }, { it.time ?: "00:00" }))
            }
            PillButton(text = if (showEventForm) "Close" else "Add an event", tonal = true, onClick = { showEventForm = !showEventForm })
            androidx.compose.animation.AnimatedVisibility(
                visible = showEventForm,
                enter = androidx.compose.animation.expandVertically(androidx.compose.animation.core.spring(0.8f, 380f)) + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.shrinkVertically(androidx.compose.animation.core.spring(1f, 500f)) + androidx.compose.animation.fadeOut(),
            ) {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppTextField(value = evTitle, onValueChange = { evTitle = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            EventCategories.forEach { c -> ExpressiveChip(selected = evCategory == c && evCustom.isBlank(), onClick = { evCategory = c; evCustom = "" }, label = { Text(c) }) }
                        }
                        AppTextField(value = evCustom, onValueChange = { evCustom = it }, label = { Text("Or your own type") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PillButton(text = evDate.pretty(), tonal = true, compact = true, onClick = { pickingDate = true })
                            AppTextField(
                                value = evTime, onValueChange = { evTime = it }, label = { Text("Time (optional, 14:30)") }, singleLine = true,
                                isError = evTime.isNotBlank() && parseTimeInput(evTime) == null, modifier = Modifier.weight(1f),
                            )
                        }
                        Text("Repeats", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            EventRepeat.entries.forEach { r ->
                                ExpressiveChip(selected = evRepeat == r, onClick = { evRepeat = r }, label = { Text(r.name.lowercase().replaceFirstChar { it.uppercase() }.let { if (r == EventRepeat.NONE) "Never" else it }) })
                            }
                        }
                        Text("Remind me", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf<Pair<Int?, String>>(null to "No", 0 to "At the time", 10 to "10 min before", 60 to "1 hour before", 1440 to "1 day before").forEach { (m, label) ->
                                ExpressiveChip(selected = evRemind == m, onClick = { evRemind = m }, label = { Text(label) })
                            }
                        }
                        if (subjects.isNotEmpty()) {
                            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ExpressiveChip(selected = evSubject == null, onClick = { evSubject = null }, label = { Text("No subject") })
                                subjects.forEach { sub -> ExpressiveChip(selected = evSubject == sub.id, onClick = { evSubject = sub.id }, label = { Text(sub.name) }) }
                            }
                        }
                        AppTextField(value = evNotes, onValueChange = { evNotes = it }, label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth())
                        PillButton(
                            text = "Save event", enabled = evTitle.isNotBlank() && (evTime.isBlank() || parseTimeInput(evTime) != null),
                            onClick = {
                                val event = EventEntity(
                                    title = evTitle.trim(), date = evDate, startTime = parseTimeInput(evTime),
                                    category = evCustom.trim().ifBlank { evCategory }.take(30), notes = evNotes.trim().ifBlank { null },
                                    subjectId = evSubject, repeat = evRepeat, remindMinutes = evRemind,
                                )
                                scope.launch { container.database.eventDao().insert(event) }
                                if (evRemind != null && android.os.Build.VERSION.SDK_INT >= 33 && !skywa1ker404.absencium.notify.ReminderScheduler.canNotify(context)) {
                                    askPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                }
                                evTitle = ""; evNotes = ""; evTime = ""; showEventForm = false
                            },
                        )
                    }
                }
            }
            if (events.isNotEmpty()) {
                SectionTitle("Your events")
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    events.forEachIndexed { index, e ->
                        if (index > 0) SettingDivider()
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(e.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    listOfNotNull(e.category, e.date.pretty(), e.startTime?.hm(), if (e.repeat != EventRepeat.NONE) e.repeat.name.lowercase() else null, e.subjectId?.let { names[it] }).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                                )
                            }
                            PillButton(text = "Delete", tonal = true, compact = true, onClick = { scope.launch { container.database.eventDao().delete(e.id) } })
                        }
                    }
                }
            }
            SectionTitle("Coming up")
            if (agenda.isEmpty()) HintText("Nothing coming up. Exams, holidays, lectures and to-dos with a date show here.")
            agenda.groupBy { it.date }.forEach { (date, items) ->
                SectionTitle(if (date == today) "Today" else if (date == today.plusDays(1)) "Tomorrow" else date.pretty())
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    items.forEachIndexed { index, item ->
                        if (index > 0) SettingDivider()
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.titleSmall)
                                Text(listOfNotNull(item.kind, item.time).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            HintText("Lectures are shown for the next four days.", modifier = Modifier.padding(top = 8.dp))
        }
    }
}
