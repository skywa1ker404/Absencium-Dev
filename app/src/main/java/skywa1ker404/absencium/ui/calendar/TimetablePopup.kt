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
    var sameEveryDay by remember { mutableStateOf(false) }
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
                sameEveryDay = false
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

        // The add form lives in a dropdown that is closed by default; changing a saved lecture opens it.
        val formOpen = addOpen || editing != null
        val formTitle = when {
            editing != null -> "Edit lecture"
            sameEveryDay -> "Add a lecture every weekday"
            else -> "Add a lecture on ${day.full()}"
        }
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = editing == null) { addOpen = !addOpen },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(formTitle, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
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
            if (editing == null) Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                    Text("Same time every day", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Adds this lecture to Monday through Friday at the same time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                AppSwitch(checked = sameEveryDay, onCheckedChange = { sameEveryDay = it })
            }
            val startTime = parseTimeInput(start)
            val endTime = parseTimeInput(end)
            val order = startTime != null && endTime != null && !endTime.isAfter(startTime)
            val is24 = android.text.format.DateFormat.is24HourFormat(androidx.compose.ui.platform.LocalContext.current)
            val timeFormat = java.time.format.DateTimeFormatter.ofPattern(if (is24) "HH:mm" else "h:mm a")
            fun endFor(from: java.time.LocalTime, minutes: Int): java.time.LocalTime =
                if (from.hour * 60 + from.minute + minutes >= 24 * 60) java.time.LocalTime.of(23, 59) else from.plusMinutes(minutes.toLong())
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PillButton(
                    text = startTime?.format(timeFormat) ?: "Start time",
                    tonal = !startOpen,
                    onClick = {
                        startOpen = !startOpen
                        durOpen = false
                        if (startTime == null) {
                            val first = java.time.LocalTime.of(9, 0)
                            start = first.hm()
                            end = endFor(first, if (durSet) durMin else 60).hm()
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
                // The length is optional and shown only once a start time is chosen.
                if (startTime != null) {
                    PillButton(
                        text = if (durSet) "${durMin / 60} h ${durMin % 60} min" else "Duration (1 h)",
                        tonal = !durOpen,
                        onClick = { durOpen = !durOpen; startOpen = false },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
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
            androidx.compose.animation.AnimatedVisibility(visible = durOpen && startTime != null) {
                if (startTime != null) {
                    skywa1ker404.absencium.ui.components.DurationWheels(
                        minutes = if (durSet) durMin else 60,
                        onChange = { minutes ->
                            durMin = minutes
                            durSet = true
                            end = endFor(startTime, minutes.coerceAtLeast(1)).hm()
                        },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
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
                        shape = skywa1ker404.absencium.ui.theme.CircleShape,
                    )
                }
            }
            if (order) HintText("The end time must be after the start time.", modifier = Modifier.padding(top = 8.dp))
            val chosen = subjectId
            val slotBeingEdited = editing
            PillButton(
                text = if (slotBeingEdited != null) "Save changes" else "Add lecture",
                enabled = chosen != null && startTime != null && endTime != null && !order,
                onClick = {
                    if (chosen != null && startTime != null && endTime != null) {
                        val roomText = room.trim().ifEmpty { null }
                        val teacherText = teacher.trim().ifEmpty { null }
                        if (slotBeingEdited != null) {
                            viewModel.updateSlot(
                                slotBeingEdited.copy(
                                    dayOfWeek = day,
                                    subjectId = chosen,
                                    startTime = startTime,
                                    endTime = endTime,
                                    roomNumber = roomText,
                                    type = lectureType,
                                    teacher = teacherText,
                                ),
                            )
                            editing = null
                        } else if (sameEveryDay) {
                            viewModel.addSlots(weekDays, chosen, startTime, endTime, roomText, lectureType, teacherText)
                        } else {
                            viewModel.addSlot(day, chosen, startTime, endTime, roomText, lectureType, teacherText)
                        }
                        start = ""
                        end = ""
                        room = ""
                        teacher = ""
                        startOpen = false
                        durOpen = false
                        durSet = false
                        addOpen = false
                    }
                },
                modifier = Modifier.padding(top = 12.dp),
            )
            if (slotBeingEdited != null) {
                TextButton(
                    onClick = {
                        editing = null
                        start = ""
                        end = ""
                        room = ""
                        teacher = ""
                        startOpen = false
                        durOpen = false
                        durSet = false
                    },
                ) { Text("Cancel editing") }
            }
                    }
                }
            }
        }
    }
    }
}