package com.absencium.app.ui.vault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import com.absencium.app.ui.components.AppTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import com.absencium.app.data.db.AttachmentKind
import com.absencium.app.ui.vault.entry.PhotoPreviewDialog
import com.absencium.app.ui.vault.entry.PhotoThumb
import com.absencium.app.data.db.NoteEntity
import com.absencium.app.data.db.NoteStatus
import com.absencium.app.ui.components.AppCard
import com.absencium.app.ui.components.HintText
import com.absencium.app.ui.components.PillButton
import com.absencium.app.ui.components.ScreenScaffold
import com.absencium.app.ui.containerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val UNTITLED = "Untitled note"

/** Edits a saved note. Changes are saved a moment after typing stops, and again when the screen closes. */
@Destination<RootGraph>
@Composable
fun NoteEditorScreen(noteId: Long, navigator: DestinationsNavigator) {
    val viewModel = containerViewModel { VaultViewModel(it) }
    val message by viewModel.message.collectAsStateWithLifecycle()
    var note by remember { mutableStateOf<NoteEntity?>(null) }
    var missing by remember { mutableStateOf(false) }

    LaunchedEffect(noteId) {
        note = viewModel.getNote(noteId)
        if (note == null) missing = true
    }

    ScreenScaffold(title = "Note", onBack = { navigator.navigateUp() }) {
        if (message != null) {
            AppCard(modifier = Modifier.fillMaxWidth(), highlighted = true, onClick = { viewModel.clearMessage() }) {
                Text(message.orEmpty(), style = MaterialTheme.typography.bodyMedium)
            }
        }
        val loaded = note
        when {
            loaded != null -> EditorBody(loaded, viewModel, navigator)
            missing -> HintText("This note could not be found. It may have been deleted.")
            else -> HintText("Loading...")
        }
    }
}

@Composable
private fun EditorBody(note: NoteEntity, viewModel: VaultViewModel, navigator: DestinationsNavigator) {
    val scope = rememberCoroutineScope()
    var title by remember(note.id) { mutableStateOf(note.title) }
    var body by remember(note.id) { mutableStateOf(note.bodyText) }
    var savedTitle by remember(note.id) { mutableStateOf(note.title) }
    var savedBody by remember(note.id) { mutableStateOf(note.bodyText) }
    var status by remember(note.id) { mutableStateOf(note.status) }
    // Set once the note is deleted or filed, so closing the screen does not save it again.
    var skipFlush by remember(note.id) { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val latestTitle by rememberUpdatedState(title)
    val latestBody by rememberUpdatedState(body)
    val latestStatus by rememberUpdatedState(status)

    fun cleanTitle(value: String) = value.trim().ifEmpty { UNTITLED }

    LaunchedEffect(title, body) {
        if (title != savedTitle || body != savedBody) {
            delay(600)
            val id = viewModel.saveNote(note.id, note.subjectId, cleanTitle(title), body, status)
            if (id != null) {
                savedTitle = title
                savedBody = body
            }
        }
    }
    DisposableEffect(note.id) {
        onDispose {
            if (!skipFlush && (latestTitle != savedTitle || latestBody != savedBody)) {
                viewModel.saveInBackground(note.id, note.subjectId, cleanTitle(latestTitle), latestBody, latestStatus)
            }
        }
    }

    AppTextField(
        value = title,
        onValueChange = { title = it },
        label = { Text("Title") },
        singleLine = true,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    )
    AppTextField(
        value = body,
        onValueChange = { body = it },
        label = { Text("Note") },
        minLines = 12,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    )
    HintText("Changes are saved automatically.")

    val vault by viewModel.data.collectAsStateWithLifecycle()
    val photos = vault?.attachmentsOf(note.id).orEmpty().filter { it.kind == AttachmentKind.IMAGE }
    var previewIndex by remember { mutableStateOf<Int?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        viewModel.addPhotos(note.id, uris)
    }
    if (photos.isNotEmpty()) {
        Text("Photos", style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(photos, key = { _, p -> p.id }) { index, photo ->
                PhotoThumb(
                    path = photo.filePath,
                    onClick = { previewIndex = index },
                    onRemove = { viewModel.deleteAttachment(photo) },
                )
            }
        }
    }
    PillButton(text = "Add photos", tonal = true, onClick = { photoPicker.launch("image/*") })
    previewIndex?.let { index ->
        PhotoPreviewDialog(
            paths = photos.map { it.filePath },
            startIndex = index.coerceIn(0, (photos.size - 1).coerceAtLeast(0)),
            onBack = { previewIndex = null },
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (status == NoteStatus.FINISHED) {
            PillButton(
                text = "Reopen",
                onClick = {
                    skipFlush = true
                    scope.launch {
                        viewModel.saveNote(note.id, note.subjectId, cleanTitle(title), body, NoteStatus.ACTIVE)
                        navigator.navigateUp()
                    }
                },
            )
        } else {
            PillButton(
                text = "Mark as finished",
                onClick = {
                    skipFlush = true
                    scope.launch {
                        // Finished notes move to the subject's Finished / Written Notes archive.
                        viewModel.saveNote(note.id, note.subjectId, cleanTitle(title), body, NoteStatus.FINISHED)
                        navigator.navigateUp()
                    }
                },
            )
        }
        PillButton(text = "Delete", tonal = true, onClick = { confirmDelete = true })
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this note?") },
            text = { Text("${cleanTitle(title)} will be deleted for good.") },
            confirmButton = {
                TextButton(onClick = {
                    skipFlush = true
                    confirmDelete = false
                    viewModel.deleteNote(note)
                    navigator.navigateUp()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep") } },
        )
    }
}
