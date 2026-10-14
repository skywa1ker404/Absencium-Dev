package com.absencium.app.ui.vault

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import com.absencium.app.ui.components.ExpressiveChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.SubjectsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.absencium.app.data.db.GuideType
import com.absencium.app.data.db.NoteStatus
import com.absencium.app.ui.components.AppCard
import com.absencium.app.ui.components.HintText
import com.absencium.app.ui.components.PillButton
import com.absencium.app.ui.components.ScreenScaffold
import com.absencium.app.ui.components.SectionTitle
import com.absencium.app.ui.containerViewModel
import kotlinx.coroutines.launch

private enum class EntryFormat { NOTE, GUIDE }

/**
 * Create New Entry: a lecture note or a guide / answer PDF. If you leave with anything typed or
 * attached, a dialog offers Discard, Save to Drafts or Cancel. A draft is a note with the DRAFT status
 * in Notes_Table, so it survives restarts, and saving it again updates the same row.
 */
@Destination<RootGraph>
@Composable
fun NewEntryScreen(
    navigator: DestinationsNavigator,
    subjectId: Long = 0L,
    draftId: Long = 0L,
    /** ISO date of an absent day whose notes this entry is for; saving the entry marks them collected. Empty otherwise. */
    collectDate: String = "",
) {
    val viewModel = containerViewModel { VaultViewModel(it) }
    val data by viewModel.data.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    var format by rememberSaveable { mutableStateOf(EntryFormat.NOTE) }
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var selectedSubject by rememberSaveable { mutableLongStateOf(subjectId) }
    var guideType by rememberSaveable { mutableStateOf(GuideType.QP) }
    var pickedPdf by rememberSaveable { mutableStateOf<Uri?>(null) }
    var currentDraftId by rememberSaveable { mutableLongStateOf(draftId) }
    var draftLoaded by rememberSaveable { mutableStateOf(draftId == 0L) }
    var done by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(draftId) {
        if (!draftLoaded) {
            viewModel.getNote(draftId)?.let { draft ->
                title = draft.title
                body = draft.bodyText
                selectedSubject = draft.subjectId
            }
            draftLoaded = true
        }
    }

    val subjects = data?.subjects?.filter { it.isActive }.orEmpty()
    val subject = subjects.firstOrNull { it.id == selectedSubject } ?: subjects.firstOrNull()

    // Anything typed or attached counts, except whitespace on its own.
    val meaningful = !done && (title.isNotBlank() || body.isNotBlank() || pickedPdf != null)

    fun leave() {
        done = true
        navigator.navigateUp()
    }

    fun requestExit() {
        if (meaningful) showExitDialog = true else navigator.navigateUp()
    }

    BackHandler(enabled = meaningful) { showExitDialog = true }

    val textPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                viewModel.readTextFile(uri)?.let { text ->
                    body = if (body.isBlank()) text else body.trimEnd() + "\n\n" + text
                }
            }
        }
    }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pickedPdf = uri
    }

    fun saveDraft() {
        val target = subject
        if (target == null) {
            problem = "Add a subject first, then save the draft."
            return
        }
        if (busy) return
        busy = true
        scope.launch {
            val id = viewModel.saveNote(
                currentDraftId,
                target.id,
                title.trim().ifEmpty { "Untitled draft" },
                body,
                NoteStatus.DRAFT,
            )
            busy = false
            if (id != null) {
                currentDraftId = id
                leave()
            }
        }
    }

    fun save() {
        val target = subject ?: return
        if (busy) return
        busy = true
        problem = null
        scope.launch {
            if (format == EntryFormat.NOTE) {
                val id = viewModel.saveNote(currentDraftId, target.id, title.trim(), body, NoteStatus.ACTIVE)
                busy = false
                if (id != null) {
                    viewModel.markNotesCollected(target.id, collectDate)
                    leave()
                }
            } else {
                val pdf = pickedPdf
                if (pdf == null) {
                    busy = false
                    return@launch
                }
                val error = viewModel.importGuide(pdf, title, target, guideType)
                busy = false
                if (error != null) {
                    problem = error
                } else {
                    // The draft that held this title is no longer needed.
                    if (currentDraftId != 0L) viewModel.discardDraft(currentDraftId)
                    viewModel.markNotesCollected(target.id, collectDate)
                    leave()
                }
            }
        }
    }

    ScreenScaffold(title = "New Entry", onBack = { requestExit() }) {
        val shownProblem = problem ?: message
        if (shownProblem != null) {
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                highlighted = true,
                onClick = {
                    problem = null
                    viewModel.clearMessage()
                },
            ) { Text(shownProblem, style = MaterialTheme.typography.bodyMedium) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExpressiveChip(
                selected = format == EntryFormat.NOTE,
                onClick = { format = EntryFormat.NOTE },
                label = { Text("Lecture Note") },
                shape = CircleShape,
            )
            ExpressiveChip(
                selected = format == EntryFormat.GUIDE,
                onClick = { format = EntryFormat.GUIDE },
                label = { Text("Guide / Answer PDF") },
                shape = CircleShape,
            )
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Note title / chapter") },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        SectionTitle("Subject")
        if (subjects.isEmpty()) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Text("Add a subject first", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Every note and guide lives in a subject folder.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                PillButton(text = "Manage subjects", onClick = { navigator.navigate(SubjectsScreenDestination) })
            }
        } else {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                subjects.forEach { option ->
                    ExpressiveChip(
                        selected = subject?.id == option.id,
                        onClick = { selectedSubject = option.id },
                        label = { Text(option.name) },
                        shape = CircleShape,
                    )
                }
            }
        }

        if (format == EntryFormat.NOTE) {
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Note") },
                minLines = 8,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Dropzone(
                text = "Tap to attach a Markdown or text file",
                detail = "Its text is added to the note.",
                onClick = { textPicker.launch(arrayOf("text/*", "application/octet-stream")) },
            )
        } else {
            SectionTitle("Type")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GuideType.entries.forEach { option ->
                    ExpressiveChip(
                        selected = guideType == option,
                        onClick = { guideType = option },
                        label = { Text(option.label()) },
                        shape = CircleShape,
                    )
                }
            }
            val picked = pickedPdf
            Dropzone(
                text = if (picked == null) {
                    "Tap to attach a PDF"
                } else {
                    DocumentFile.fromSingleUri(context, picked)?.name ?: "PDF attached"
                },
                detail = if (picked == null) "It is copied into the Guides and Question Banks folder." else "Tap to choose a different PDF.",
                onClick = { pdfPicker.launch(arrayOf("application/pdf")) },
            )
        }

        PillButton(
            text = "Save",
            loading = busy,
            enabled = subject != null && title.isNotBlank() &&
                (format == EntryFormat.NOTE || pickedPdf != null),
            onClick = { save() },
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Save this entry?") },
            text = {
                Text(
                    if (format == EntryFormat.GUIDE && pickedPdf != null) {
                        "You have unsaved changes. A draft keeps the title but not the attached PDF."
                    } else {
                        "You have unsaved changes."
                    },
                )
            },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = {
                        showExitDialog = false
                        leave()
                    }) { Text("Discard") }
                    TextButton(onClick = {
                        showExitDialog = false
                        saveDraft()
                    }) { Text("Save to Drafts") }
                    TextButton(onClick = { showExitDialog = false }) { Text("Cancel") }
                }
            },
        )
    }
}

@Composable
private fun Dropzone(text: String, detail: String, onClick: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Text(text, style = MaterialTheme.typography.titleMedium)
        Text(
            detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
