package com.trajectory.vault.ui.calendar

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.trajectory.vault.ui.components.ExpressiveChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.SubjectsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.trajectory.vault.data.db.TimetableEntity
import com.trajectory.vault.domain.hm
import com.trajectory.vault.domain.timetableDays
import com.trajectory.vault.domain.parseTimeInput
import com.trajectory.vault.ui.components.AppCard
import com.trajectory.vault.ui.components.HintText
import com.trajectory.vault.ui.components.PillButton
import com.trajectory.vault.ui.components.ScreenScaffold
import com.trajectory.vault.ui.components.SectionTitle
import com.trajectory.vault.ui.containerViewModel
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/** Monday to Friday. */

private fun DayOfWeek.full(): String = getDisplayName(TextStyle.FULL, Locale.getDefault())

/** The weekly timetable: add and remove lecture slots per weekday. */
@Destination<RootGraph>
@Composable
fun TimetableScreen(navigator: DestinationsNavigator) {
    val viewModel = containerViewModel { CalendarViewModel(it) }
    val data by viewModel.data.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme

    var day by remember { mutableStateOf(DayOfWeek.MONDAY) }
    var subjectId by remember { mutableStateOf<Long?>(null) }
    var start by remember { mutableStateOf("") }
    var end by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }
    var sameEveryDay by remember { mutableStateOf(false) }
    // The saved lecture being changed, or null while adding a new one.
    var editing by remember { mutableStateOf<TimetableEntity?>(null) }

    ScreenScaffold(title = "Timetable", onBack = { navigator.navigateUp() }) {
        val loaded = data
        if (loaded == null) {
            HintText("Loading...")
            return@ScreenScaffold
        }
        val subjects = loaded.subjects.filter { it.isActive }
        val weekDays = loaded.timetableDays()
        if (message != null) {
            AppCard(modifier = Modifier.fillMaxWidth(), highlighted = true, onClick = { viewModel.clearMessage() }) {
                Text(message.orEmpty(), style = MaterialTheme.typography.bodyMedium)
            }
        }

        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Saturday and Sunday only appear when they are not set as holidays in Settings.
            weekDays.forEach { option ->
                ExpressiveChip(
                    selected = day == option,
                    onClick = { day = option },
                    label = { Text(option.getDisplayName(TextStyle.SHORT, Locale.getDefault())) },
                    shape = CircleShape,
                )
            }
        }

        SectionTitle(day.full())
        val slots = loaded.timetable.filter { it.dayOfWeek == day }.sortedBy { it.startTime }
        val subjectById = loaded.subjects.associateBy { it.id }
        if (slots.isEmpty()) {
            HintText("No lectures on ${day.full()}.")
        }
        slots.forEach { slot ->
            val subject = subjectById[slot.subjectId]
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(subjectColor(slot.subjectId)))
                    androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                        Text(subject?.name ?: "Unknown subject", style = MaterialTheme.typography.titleMedium)
                        val roomText = slot.roomNumber?.takeIf { it.isNotBlank() }
                        Text(
                            text = "${slot.startTime.hm()} to ${slot.endTime.hm()}" +
                                (if (roomText != null) " · Room $roomText" else ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    TextButton(
                        onClick = {
                            editing = slot
                            sameEveryDay = false
                            subjectId = slot.subjectId
                            start = slot.startTime.hm()
                            end = slot.endTime.hm()
                            room = slot.roomNumber.orEmpty()
                        },
                    ) { Text("Edit") }
                    TextButton(
                        onClick = {
                            if (editing?.id == slot.id) editing = null
                            viewModel.deleteSlot(slot)
                        },
                    ) { Text("Remove") }
                }
            }
        }

        SectionTitle(
            when {
                editing != null -> "Edit lecture"
                sameEveryDay -> "Add a lecture every weekday"
                else -> "Add a lecture on ${day.full()}"
            },
        )
        if (subjects.isEmpty()) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Text("Add a subject first", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Every lecture belongs to a subject.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                PillButton(text = "Manage subjects", onClick = { navigator.navigate(SubjectsScreenDestination) })
            }
            return@ScreenScaffold
        }
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                subjects.forEach { subject ->
                    ExpressiveChip(
                        selected = subjectId == subject.id,
                        onClick = { subjectId = subject.id },
                        label = { Text(subject.name) },
                        shape = CircleShape,
                    )
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
                Switch(checked = sameEveryDay, onCheckedChange = { sameEveryDay = it })
            }
            val startTime = parseTimeInput(start)
            val endTime = parseTimeInput(end)
            val startBad = start.isNotBlank() && startTime == null
            val endBad = end.isNotBlank() && endTime == null
            val order = startTime != null && endTime != null && !endTime.isAfter(startTime)
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = start,
                    onValueChange = { start = it },
                    label = { Text("Start (9:30)") },
                    isError = startBad,
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = end,
                    onValueChange = { end = it },
                    label = { Text("End (10:30)") },
                    isError = endBad || order,
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = room,
                onValueChange = { room = it },
                label = { Text("Room (optional)") },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            if (startBad || endBad) {
                HintText("Use 24 hour times like 9:30 or 14:00.", modifier = Modifier.padding(top = 8.dp))
            } else if (order) {
                HintText("The end time must be after the start time.", modifier = Modifier.padding(top = 8.dp))
            }
            val chosen = subjectId
            val slotBeingEdited = editing
            PillButton(
                text = if (slotBeingEdited != null) "Save changes" else "Add lecture",
                enabled = chosen != null && startTime != null && endTime != null && !order,
                onClick = {
                    if (chosen != null && startTime != null && endTime != null) {
                        val roomText = room.trim().ifEmpty { null }
                        if (slotBeingEdited != null) {
                            viewModel.updateSlot(
                                slotBeingEdited.copy(
                                    dayOfWeek = day,
                                    subjectId = chosen,
                                    startTime = startTime,
                                    endTime = endTime,
                                    roomNumber = roomText,
                                ),
                            )
                            editing = null
                        } else if (sameEveryDay) {
                            viewModel.addSlots(weekDays, chosen, startTime, endTime, roomText)
                        } else {
                            viewModel.addSlot(day, chosen, startTime, endTime, roomText)
                        }
                        start = ""
                        end = ""
                        room = ""
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
                    },
                ) { Text("Cancel editing") }
            }
        }
    }
}
