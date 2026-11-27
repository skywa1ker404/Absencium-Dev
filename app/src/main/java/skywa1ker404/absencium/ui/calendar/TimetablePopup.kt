package skywa1ker404.absencium.ui.calendar

import skywa1ker404.absencium.ui.components.AppSwitch
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import skywa1ker404.absencium.ui.theme.CircleShape
import skywa1ker404.absencium.ui.theme.RoundedCornerShape
import skywa1ker404.absencium.ui.components.ExpressiveChip
import androidx.compose.material3.MaterialTheme
import skywa1ker404.absencium.ui.components.AppTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.clickable
import skywa1ker404.absencium.ui.components.ExpandChevron
import skywa1ker404.absencium.ui.components.Reveal
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import skywa1ker404.absencium.data.db.TimetableEntity
import skywa1ker404.absencium.domain.hm
import skywa1ker404.absencium.domain.timetableDays
import skywa1ker404.absencium.domain.parseTimeInput
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.components.PopupBody
import skywa1ker404.absencium.ui.components.PopupShell
import skywa1ker404.absencium.ui.components.SectionTitle
import skywa1ker404.absencium.ui.containerViewModel
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/** Monday to Friday. */

private fun DayOfWeek.full(): String = getDisplayName(TextStyle.FULL, Locale.getDefault())

/** The weekly timetable as a popup: add, change and remove lecture slots per weekday. */
@Composable
fun TimetablePopup(onDismiss: () -> Unit, onManageSubjects: () -> Unit) {
    val viewModel = containerViewModel { CalendarViewModel(it) }
    val data by viewModel.data.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme

    val today = remember { java.time.LocalDate.now() }
    // The time of day, refreshed every half minute so the lecture in progress moves on while the popup is open.
    val clock by androidx.compose.runtime.produceState(java.time.LocalTime.now()) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            value = java.time.LocalTime.now()
        }
    }
    var day by remember { mutableStateOf(today.dayOfWeek) }
    var subjectId by remember { mutableStateOf<Long?>(null) }
    var start by remember { mutableStateOf("") }
    var end by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }
    var teacher by remember { mutableStateOf("") }
    var startOpen by remember { mutableStateOf(false) }
    var durOpen by remember { mutableStateOf(false) }
    var durMin by remember { mutableStateOf(60) }
    var durSet by remember { mutableStateOf(false) }
    var lectureType by remember { mutableStateOf(skywa1ker404.absencium.data.db.LectureType.LECTURE) }
    // The days a new lecture is added to; empty means the day shown above. A lecture being changed has exactly one.
    var chosenDays by remember { mutableStateOf(emptySet<DayOfWeek>()) }
    // The saved lecture being changed, or null while adding a new one.
    var editing by remember { mutableStateOf<TimetableEntity?>(null) }
    var addOpen by rememberSaveable { mutableStateOf(false) }

    PopupShell(onDismissRequest = onDismiss, dismissOnOutsideTap = true) {
    PopupBody(title = "Timetable", onClose = onDismiss) {
        val loaded = data
        if (loaded == null) {
            HintText("Loading...")
            return@PopupBody
        }
        val subjects = loaded.subjects.filter { it.isActive }
        val weekDays = loaded.timetableDays()
        // Opened on a weekend that is a holiday: start on the first working day instead of a day that is not in the strip.
        androidx.compose.runtime.LaunchedEffect(weekDays) { if (weekDays.isNotEmpty() && day !in weekDays) day = weekDays.first() }
        if (message != null) {
            AppCard(modifier = Modifier.fillMaxWidth(), highlighted = true, onClick = { viewModel.clearMessage() }) {
                Text(message.orEmpty(), style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Saturday and Sunday only appear when they are not set as holidays in Settings.
        WeekStrip(days = weekDays, selected = day, today = today, onSelect = { day = it })

        SectionTitle(day.full() + if (day == today.dayOfWeek) " \u00b7 today" else "")
        val slots = loaded.timetable.filter { it.dayOfWeek == day }.sortedBy { it.startTime }
        val subjectById = loaded.subjects.associateBy { it.id }
        if (slots.isEmpty()) {
            HintText("No lectures on ${day.full()}.")
        }
        TimetableDay(
            slots = slots,
            subjectName = { subjectById[it]?.name ?: "Unknown subject" },
            now = if (day == today.dayOfWeek) clock else null,
            onEdit = { slot ->
                editing = slot
                chosenDays = setOf(slot.dayOfWeek)
                subjectId = slot.subjectId
                start = slot.startTime.hm()
                end = slot.endTime.hm()
                room = slot.roomNumber.orEmpty()
                teacher = slot.teacher.orEmpty()
                durMin = java.time.Duration.between(slot.startTime, slot.endTime).toMinutes().toInt().coerceAtLeast(1)
                durSet = true
                lectureType = slot.type
            },
            onDelete = { slot ->
                if (editing?.id == slot.id) editing = null
                viewModel.deleteSlot(slot)
            },
        )

        // The add form lives in a card that is closed by default; changing a saved lecture opens it.
        val formOpen = addOpen || editing != null
        val formDays = chosenDays.ifEmpty { setOf(day) }
        val is24 = android.text.format.DateFormat.is24HourFormat(androidx.compose.ui.platform.LocalContext.current)
        val timeFormat = java.time.format.DateTimeFormatter.ofPattern(if (is24) "HH:mm" else "h:mm a")
        // Start where the day's last lecture ends and last as long as it did, so a new lecture is usually one tap from done.
        fun suggest(forDay: DayOfWeek) {
            val last = loaded.timetable.filter { it.dayOfWeek == forDay }.maxByOrNull { it.endTime }
            val first = if (last != null && last.endTime.hour < 22) last.endTime else java.time.LocalTime.of(9, 0)
            val minutes = last?.let { java.time.Duration.between(it.startTime, it.endTime).toMinutes().toInt() }?.coerceIn(15, 240) ?: 60
            start = first.hm()
            durMin = minutes
            durSet = true
            end = endFor(first, minutes).hm()
        }
        fun resetForm() {
            editing = null
            chosenDays = emptySet()
            start = ""
            end = ""
            room = ""
            teacher = ""
            startOpen = false
            durOpen = false
            durSet = false
        }
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = editing == null) {
                        addOpen = !addOpen
                        if (addOpen && start.isBlank()) suggest(formDays.minOrNull() ?: day)
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (editing != null) "Edit lecture" else "Add a lecture", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                ExpandChevron(expanded = formOpen, description = if (formOpen) "Close the form" else "Open the form")
            }
            Reveal(visible = formOpen) {
                androidx.compose.foundation.layout.Column(modifier = Modifier.padding(top = 12.dp)) {
                    if (subjects.isEmpty()) {
                        Text("Add a subject first", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Every lecture belongs to a subject.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                        PillButton(text = "Manage subjects", onClick = onManageSubjects)
                    } else {
                        var subjectMenu by remember { mutableStateOf(false) }
                        Box {
                            PillButton(
                                text = subjects.firstOrNull { it.id == subjectId }?.name ?: "Choose a subject",
                                tonal = true,
                                onClick = { subjectMenu = true },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            androidx.compose.material3.DropdownMenu(expanded = subjectMenu, onDismissRequest = { subjectMenu = false }) {
                                subjects.forEach { subject ->
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text(subject.name) },
                                        onClick = { subjectId = subject.id; subjectMenu = false },
                                    )
                                }
                            }
                        }

                        // Several days at once: tick every day the lecture is held at this time.
                        Text(
                            if (editing != null) "Day" else "Days",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                        )
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            weekDays.forEach { d ->
                                ExpressiveChip(
                                    selected = d in formDays,
                                    onClick = {
                                        chosenDays = when {
                                            editing != null -> setOf(d)
                                            d in formDays && formDays.size > 1 -> formDays - d
                                            d in formDays -> formDays
                                            else -> formDays + d
                                        }
                                    },
                                    label = { Text(d.getDisplayName(TextStyle.SHORT, Locale.getDefault())) },
                                    shape = CircleShape,
                                )
                            }
                            if (editing == null && weekDays.size > 1) {
                                ExpressiveChip(
                                    selected = formDays.size == weekDays.size,
                                    onClick = { chosenDays = if (formDays.size == weekDays.size) setOf(day) else weekDays.toSet() },
                                    label = { Text("Every day") },
                                    shape = CircleShape,
                                )
                            }
                        }

                        val startTime = parseTimeInput(start)
                        val endTime = parseTimeInput(end)
                        val order = startTime != null && endTime != null && !endTime.isAfter(startTime)
                        PillButton(
                            text = startTime?.let { "Starts at ${it.format(timeFormat)}" } ?: "Start time",
                            tonal = !startOpen,
                            onClick = {
                                startOpen = !startOpen
                                durOpen = false
                                if (startTime == null) suggest(formDays.minOrNull() ?: day)
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        )
                        androidx.compose.animation.AnimatedVisibility(visible = startOpen && startTime != null) {
                            if (startTime != null) {
                                skywa1ker404.absencium.ui.components.TimeWheels(
                                    value = startTime,
                                    is24Hour = is24,
                                    onChange = { picked ->
                                        start = picked.hm()
                                        end = endFor(picked, if (durSet) durMin else 60).hm()
                                    },
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }

                        Text(
                            "Duration",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                        )
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            QUICK_DURATIONS.forEach { m ->
                                ExpressiveChip(
                                    selected = durSet && durMin == m && !durOpen,
                                    onClick = {
                                        durMin = m
                                        durSet = true
                                        durOpen = false
                                        if (startTime != null) end = endFor(startTime, m).hm()
                                    },
                                    label = { Text(durationLabel(m)) },
                                    shape = CircleShape,
                                )
                            }
                            val custom = durSet && durMin !in QUICK_DURATIONS
                            ExpressiveChip(
                                selected = durOpen || custom,
                                onClick = { durOpen = !durOpen; startOpen = false },
                                label = { Text(if (custom) durationLabel(durMin) else "Custom") },
                                shape = CircleShape,
                            )
                        }
                        androidx.compose.animation.AnimatedVisibility(visible = durOpen && startTime != null) {
                            if (startTime != null) {
                                skywa1ker404.absencium.ui.components.DurationWheels(
                                    minutes = durMin,
                                    onChange = { minutes ->
                                        durMin = minutes.coerceAtLeast(1)
                                        durSet = true
                                        end = endFor(startTime, durMin).hm()
                                    },
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }
                        if (startTime != null && endTime != null && !order) {
                            HintText("Ends at ${endTime.format(timeFormat)}", modifier = Modifier.padding(top = 6.dp))
                        }

                        AppTextField(
                            value = room,
                            onValueChange = { room = it },
                            label = { Text("Room (optional)") },
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                        AppTextField(
                            value = teacher,
                            onValueChange = { teacher = it },
                            label = { Text("Teacher (optional)") },
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                        Row(
                            modifier = Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            skywa1ker404.absencium.data.db.LectureType.entries.forEach { option ->
                                ExpressiveChip(
                                    selected = lectureType == option,
                                    onClick = { lectureType = option },
                                    label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                    shape = CircleShape,
                                )
                            }
                        }
                        if (order) HintText("The end time must be after the start time.", modifier = Modifier.padding(top = 8.dp))
                        // A clash is only a warning: two lectures at the same time can be real (a split class, an elective).
                        val clash = if (startTime != null && endTime != null && !order) {
                            formDays.sorted().firstNotNullOfOrNull { d ->
                                loaded.timetable.firstOrNull {
                                    it.dayOfWeek == d && it.id != editing?.id && it.startTime.isBefore(endTime) && it.endTime.isAfter(startTime)
                                }?.let { d to it }
                            }
                        } else null
                        if (clash != null) {
                            val (clashDay, clashSlot) = clash
                            HintText(
                                "Overlaps ${subjectById[clashSlot.subjectId]?.name ?: "another lecture"} on ${clashDay.full()}, ${clashSlot.startTime.hm()} to ${clashSlot.endTime.hm()}.",
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        val chosen = subjectId
                        val slotBeingEdited = editing
                        PillButton(
                            text = when {
                                slotBeingEdited != null -> "Save changes"
                                formDays.size > 1 -> "Add to ${formDays.size} days"
                                else -> "Add lecture"
                            },
                            enabled = chosen != null && startTime != null && endTime != null && !order,
                            onClick = {
                                if (chosen != null && startTime != null && endTime != null) {
                                    val roomText = room.trim().ifEmpty { null }
                                    val teacherText = teacher.trim().ifEmpty { null }
                                    if (slotBeingEdited != null) {
                                        viewModel.updateSlot(
                                            slotBeingEdited.copy(
                                                dayOfWeek = formDays.first(),
                                                subjectId = chosen,
                                                startTime = startTime,
                                                endTime = endTime,
                                                roomNumber = roomText,
                                                type = lectureType,
                                                teacher = teacherText,
                                            ),
                                        )
                                    } else if (formDays.size > 1) {
                                        viewModel.addSlots(formDays.sorted(), chosen, startTime, endTime, roomText, lectureType, teacherText)
                                    } else {
                                        viewModel.addSlot(formDays.first(), chosen, startTime, endTime, roomText, lectureType, teacherText)
                                    }
                                    // Show the day the lecture went to.
                                    day = formDays.minOrNull() ?: day
                                    resetForm()
                                    addOpen = false
                                }
                            },
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        if (slotBeingEdited != null) {
                            TextButton(onClick = { resetForm() }) { Text("Cancel editing") }
                        }
                    }
                }
            }
        }
    }
    }
}

private val QUICK_DURATIONS = listOf(30, 45, 60, 90, 120)

private fun durationLabel(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}

/** The end of a lecture that starts at [from] and lasts [minutes], never past the end of the day. */
private fun endFor(from: java.time.LocalTime, minutes: Int): java.time.LocalTime =
    if (from.hour * 60 + from.minute + minutes >= 24 * 60) java.time.LocalTime.of(23, 59) else from.plusMinutes(minutes.toLong())
