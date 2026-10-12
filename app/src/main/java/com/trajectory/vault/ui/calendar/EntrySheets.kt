package com.trajectory.vault.ui.calendar

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import com.trajectory.vault.ui.components.sheetContainerColor
import com.trajectory.vault.ui.components.sheetScrimColor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.trajectory.vault.data.db.ExamEntity
import com.trajectory.vault.data.db.Priority
import com.trajectory.vault.data.db.SubjectEntity
import com.trajectory.vault.domain.pretty
import com.trajectory.vault.ui.components.HintText
import com.trajectory.vault.ui.icons.AppIcons
import com.trajectory.vault.ui.components.PillButton
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private enum class EntryType { EXAM, HOLIDAY }

/** The date picker works in UTC milliseconds at the start of the chosen day. */
private fun LocalDate.toPickerMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.fromPickerMillis(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/** The Add Calendar Entry form: a scheduled exam or a college holiday. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntrySheet(
    subjects: List<SubjectEntity>,
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onSaveExam: (subjectId: Long, date: LocalDate, title: String, weightage: Double?, room: String?, notes: String?, priority: Priority) -> Unit,
    onSaveHoliday: (date: LocalDate, title: String, autoSkip: Boolean, priority: Priority) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var type by remember { mutableStateOf(EntryType.EXAM) }
    var title by remember { mutableStateOf("") }
    var subjectId by remember { mutableStateOf<Long?>(subjects.firstOrNull()?.id) }
    var date by remember { mutableStateOf(initialDate) }
    var showPicker by remember { mutableStateOf(false) }
    var priority by remember { mutableStateOf(Priority.HIGH) }
    var autoSkip by remember { mutableStateOf(true) }
    var weightage by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val weightageValue = weightage.trim().toDoubleOrNull()
    val weightageInvalid = weightage.isNotBlank() && weightageValue == null
    val canSave = title.isNotBlank() && !weightageInvalid &&
        (type == EntryType.HOLIDAY || subjectId != null)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = sheetContainerColor(),
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = sheetScrimColor(),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Add Calendar Entry", style = MaterialTheme.typography.headlineSmall)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = type == EntryType.EXAM,
                    onClick = {
                        type = EntryType.EXAM
                        priority = Priority.HIGH
                    },
                    label = { Text("🚨 Scheduled Exam") },
                    shape = CircleShape,
                )
                FilterChip(
                    selected = type == EntryType.HOLIDAY,
                    onClick = {
                        type = EntryType.HOLIDAY
                        priority = Priority.LOW
                    },
                    label = { Text("🏖️ College Holiday") },
                    shape = CircleShape,
                )
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(if (type == EntryType.EXAM) "Exam title" else "Holiday name") },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            if (type == EntryType.EXAM) {
                Text("Subject", style = MaterialTheme.typography.labelLarge)
                if (subjects.isEmpty()) {
                    HintText("Add a subject first in Settings, General, Subjects.")
                } else {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        subjects.forEach { subject ->
                            FilterChip(
                                selected = subjectId == subject.id,
                                onClick = { subjectId = subject.id },
                                label = { Text(subject.name) },
                                shape = CircleShape,
                            )
                        }
                    }
                }
            }

            Text("Date", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(onClick = { showPicker = true }, shape = CircleShape) {
                Icon(AppIcons.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("${date.pretty()} ${date.year}", modifier = Modifier.padding(start = 8.dp))
            }

            Text("Priority", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.entries.forEach { option ->
                    FilterChip(
                        selected = priority == option,
                        onClick = { priority = option },
                        label = { Text(option.label()) },
                        shape = CircleShape,
                    )
                }
            }
            HintText(
                "High shows an emergency banner on Home within 48 hours. Normal only appears in the countdown. " +
                    "Low stays quiet.",
            )

            if (type == EntryType.HOLIDAY) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Skip lectures on this day", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Lectures that day are not counted in attendance.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    Switch(checked = autoSkip, onCheckedChange = { autoSkip = it })
                }
            } else {
                OutlinedTextField(
                    value = weightage,
                    onValueChange = { weightage = it },
                    label = { Text("Weightage in marks or percent (optional)") },
                    isError = weightageInvalid,
                    supportingText = if (weightageInvalid) {
                        { Text("Enter a number such as 40") }
                    } else {
                        null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Room (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Syllabus notes (optional)") },
                    minLines = 2,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            PillButton(
                text = "Save",
                enabled = canSave,
                onClick = {
                    val cleanTitle = title.trim()
                    if (type == EntryType.EXAM) {
                        onSaveExam(
                            subjectId ?: return@PillButton,
                            date,
                            cleanTitle,
                            weightageValue,
                            room.trim().ifEmpty { null },
                            notes.trim().ifEmpty { null },
                            priority,
                        )
                    } else {
                        onSaveHoliday(date, cleanTitle, autoSkip, priority)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = date.toPickerMillis())
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { date = it.fromPickerMillis() }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/** Exam details: topics, weightage, room and syllabus notes, with a delete action. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamDetailSheet(
    exam: ExamEntity,
    subjectName: String,
    today: LocalDate,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var confirmDelete by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = sheetContainerColor(),
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = sheetScrimColor(),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(exam.title, style = MaterialTheme.typography.headlineSmall)
            DetailLine("Subject", subjectName)
            DetailLine("Date", "${exam.date.pretty()} ${exam.date.year} (${daysLabel(today, exam.date)})")
            DetailLine("Priority", exam.priority.label())
            DetailLine("Weightage", exam.weightage?.let { trimNumber(it) } ?: "Not set")
            DetailLine("Room", exam.room ?: "Not set")
            Text("Syllabus notes", style = MaterialTheme.typography.labelLarge, color = colors.primary)
            Text(
                text = exam.syllabusNotes ?: "No notes added.",
                style = MaterialTheme.typography.bodyMedium,
            )
            PillButton(
                text = "Delete exam",
                tonal = true,
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this exam?") },
            text = { Text("${exam.title} will be removed from the calendar.") },
            confirmButton = { TextButton(onClick = onDelete) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep") } },
        )
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

/** 40.0 becomes "40" and 12.5 stays "12.5". */
private fun trimNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
