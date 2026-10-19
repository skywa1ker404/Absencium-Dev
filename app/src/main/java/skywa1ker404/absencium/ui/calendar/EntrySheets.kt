package skywa1ker404.absencium.ui.calendar

import skywa1ker404.absencium.ui.theme.LocalQuality
import androidx.compose.animation.AnimatedVisibility
import skywa1ker404.absencium.ui.components.Reveal
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.ExperimentalMaterial3Api
import skywa1ker404.absencium.ui.components.ExpressiveChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import skywa1ker404.absencium.ui.components.AppTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import skywa1ker404.absencium.ui.components.OverlayStage
import skywa1ker404.absencium.ui.components.AnimatedPopup
import skywa1ker404.absencium.ui.components.DatePickerPopup
import skywa1ker404.absencium.ui.components.PopupShell
import skywa1ker404.absencium.ui.components.sheetContainerColor
import skywa1ker404.absencium.ui.components.sheetScrimColor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import skywa1ker404.absencium.data.db.ExamEntity
import skywa1ker404.absencium.data.db.HolidayEntity
import skywa1ker404.absencium.data.db.Priority
import skywa1ker404.absencium.data.db.SubjectEntity
import skywa1ker404.absencium.domain.pretty
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.components.PillButton
import java.time.LocalDate

private enum class EntryType { EXAM, HOLIDAY }

/** The Add Calendar Entry form: a scheduled exam or a college holiday. */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AddEntrySheet(
    subjects: List<SubjectEntity>,
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onSaveExam: (subjectId: Long, date: LocalDate, title: String, weightage: Double?, room: String?, notes: String?, priority: Priority) -> Unit,
    onSaveHoliday: (start: LocalDate, end: LocalDate, title: String, autoSkip: Boolean, priority: Priority) -> Unit,
    /** Set to edit a saved exam instead of adding one; the form opens filled in. */
    editExam: ExamEntity? = null,
    /** Set to edit a saved holiday instead of adding one. A single day can be edited. */
    editHoliday: HolidayEntity? = null,
    onUpdateExam: (ExamEntity) -> Unit = {},
    onUpdateHoliday: (HolidayEntity) -> Unit = {},
    /** Opens the AI holiday import. Shown in the holiday form when set. */
    onImportWithAi: (() -> Unit)? = null,
) {
    val q = LocalQuality.current
    val colors = MaterialTheme.colorScheme
    val editing = editExam != null || editHoliday != null
    var type by remember {
        mutableStateOf(if (editHoliday != null) EntryType.HOLIDAY else EntryType.EXAM)
    }
    var title by remember { mutableStateOf(editExam?.title ?: editHoliday?.title ?: "") }
    var subjectId by remember { mutableStateOf<Long?>(editExam?.subjectId ?: subjects.firstOrNull()?.id) }
    var date by remember { mutableStateOf(editExam?.date ?: editHoliday?.date ?: initialDate) }
    var showPicker by remember { mutableStateOf(false) }
    // Which date the picker is editing: the start (or only) day, or the last day of a holiday range.
    var pickingEnd by remember { mutableStateOf(false) }
    var multiDay by remember { mutableStateOf(false) }
    var endDate by remember { mutableStateOf(initialDate) }
    var priority by remember { mutableStateOf(editExam?.priority ?: editHoliday?.priority ?: Priority.HIGH) }
    var autoSkip by remember { mutableStateOf(editHoliday?.autoSkip ?: true) }
    var weightage by remember { mutableStateOf(editExam?.weightage?.let { trimNumber(it) } ?: "") }
    var room by remember { mutableStateOf(editExam?.room ?: "") }
    var notes by remember { mutableStateOf(editExam?.syllabusNotes ?: "") }

    val weightageValue = weightage.trim().toDoubleOrNull()
    val weightageInvalid = weightage.isNotBlank() && weightageValue == null
    val rangeInvalid = type == EntryType.HOLIDAY && multiDay && endDate.isBefore(date)
    val canSave = title.isNotBlank() && !weightageInvalid && !rangeInvalid &&
        (type == EntryType.HOLIDAY || subjectId != null)

    PopupShell(onDismissRequest = onDismiss, scrollable = false, dismissOnOutsideTap = false) {
        OverlayStage {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = when {
                    editExam != null -> "Edit exam"
                    editHoliday != null -> "Edit holiday"
                    else -> "Add Calendar Entry"
                },
                style = MaterialTheme.typography.headlineSmall,
            )

            if (!editing) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExpressiveChip(
                    selected = type == EntryType.EXAM,
                    onClick = {
                        type = EntryType.EXAM
                        priority = Priority.HIGH
                    },
                    label = { Text("🚨 Scheduled Exam", maxLines = 1, softWrap = false) },
                    shape = CircleShape,
                )
                ExpressiveChip(
                    selected = type == EntryType.HOLIDAY,
                    onClick = {
                        type = EntryType.HOLIDAY
                        priority = Priority.LOW
                    },
                    label = { Text("🏖️ College Holiday", maxLines = 1, softWrap = false) },
                    shape = CircleShape,
                )
            }

            if (type == EntryType.HOLIDAY && !editing && onImportWithAi != null) {
                OutlinedButton(onClick = onImportWithAi, shape = CircleShape, modifier = Modifier.fillMaxWidth()) {
                    Icon(AppIcons.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Import from text, photo or PDF with AI", modifier = Modifier.padding(start = 8.dp))
                }
            }

            AppTextField(
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
                            ExpressiveChip(
                                selected = subjectId == subject.id,
                                onClick = { subjectId = subject.id },
                                label = { Text(subject.name) },
                                shape = CircleShape,
                            )
                        }
                    }
                }
            }

            Text(if (type == EntryType.HOLIDAY && multiDay) "From" else "Date", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(onClick = { pickingEnd = false; showPicker = true }, shape = CircleShape) {
                Icon(AppIcons.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("${date.pretty()} ${date.year}", modifier = Modifier.padding(start = 8.dp))
            }
            if (type == EntryType.HOLIDAY && !editing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Several days in a row", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "One festival or break across a range of days, for example Diwali break.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = multiDay,
                        onCheckedChange = {
                            multiDay = it
                            if (it && endDate.isBefore(date)) endDate = date
                        },
                    )
                }
                Reveal(visible = multiDay) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("To", style = MaterialTheme.typography.labelLarge)
                        OutlinedButton(onClick = { pickingEnd = true; showPicker = true }, shape = CircleShape) {
                            Icon(AppIcons.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("${endDate.pretty()} ${endDate.year}", modifier = Modifier.padding(start = 8.dp))
                        }
                        if (rangeInvalid) HintText("The last day must not be before the first day.")
                    }
                }
            }

            Text("Priority", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.entries.forEach { option ->
                    ExpressiveChip(
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
                AppTextField(
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
                AppTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Room (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                AppTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Syllabus notes (optional)") },
                    minLines = 2,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            PillButton(
                text = if (editing) "Save changes" else "Save",
                enabled = canSave,
                onClick = {
                    val cleanTitle = title.trim()
                    if (editExam != null) {
                        onUpdateExam(
                            editExam.copy(
                                subjectId = subjectId ?: return@PillButton,
                                date = date,
                                title = cleanTitle,
                                weightage = weightageValue,
                                room = room.trim().ifEmpty { null },
                                syllabusNotes = notes.trim().ifEmpty { null },
                                priority = priority,
                            ),
                        )
                    } else if (editHoliday != null) {
                        onUpdateHoliday(
                            editHoliday.copy(date = date, title = cleanTitle, autoSkip = autoSkip, priority = priority),
                        )
                    } else if (type == EntryType.EXAM) {
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
                        onSaveHoliday(date, if (multiDay) endDate else date, cleanTitle, autoSkip, priority)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        }
    }

    AnimatedPopup(if (showPicker) true else null) {
        DatePickerPopup(
            initial = if (pickingEnd) endDate else date,
            title = if (pickingEnd) "Pick the end date" else "Pick a date",
            onPicked = {
                if (pickingEnd) {
                    endDate = it
                } else {
                    date = it
                    if (endDate.isBefore(date)) endDate = date
                }
            },
            onDismiss = { showPicker = false },
        )
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
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var confirmDelete by remember { mutableStateOf(false) }

    PopupShell(onDismissRequest = onDismiss, scrollable = false, dismissOnOutsideTap = true) {
        OverlayStage {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 24.dp),
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
                text = "Edit exam",
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth(),
            )
            PillButton(
                text = "Delete exam",
                tonal = true,
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }
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
