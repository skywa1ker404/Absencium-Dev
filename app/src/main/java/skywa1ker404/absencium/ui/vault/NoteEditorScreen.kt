package skywa1ker404.absencium.ui.vault

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.NoteEditorScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import skywa1ker404.absencium.data.db.AttachmentKind
import skywa1ker404.absencium.data.db.NoteEntity
import skywa1ker404.absencium.data.db.NoteStatus
import skywa1ker404.absencium.data.share.ShareFormat
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.AppTextField
import skywa1ker404.absencium.ui.components.ExpressiveChip
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.containerViewModel
import skywa1ker404.absencium.ui.vault.entry.PhotoPreviewDialog
import skywa1ker404.absencium.ui.vault.entry.PhotoThumb
import skywa1ker404.absencium.ui.vault.markdown.Edited
import skywa1ker404.absencium.ui.vault.markdown.MarkdownEdit
import skywa1ker404.absencium.ui.vault.markdown.NoteTags

private const val UNTITLED = "Untitled note"

/** Where the text on screen stands compared with what is in the database. */
private enum class SaveState { SAVED, UNSAVED, SAVING, FAILED }

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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditorBody(note: NoteEntity, viewModel: VaultViewModel, navigator: DestinationsNavigator) {
    val scope = rememberCoroutineScope()
    var title by remember(note.id) { mutableStateOf(note.title) }
    var body by remember(note.id) { mutableStateOf(TextFieldValue(note.bodyText)) }
    var tags by remember(note.id) { mutableStateOf(NoteTags.parse(note.tags)) }
    var savedTitle by remember(note.id) { mutableStateOf(note.title) }
    var savedBody by remember(note.id) { mutableStateOf(note.bodyText) }
    var savedTags by remember(note.id) { mutableStateOf(NoteTags.join(NoteTags.parse(note.tags))) }
    var saveState by remember(note.id) { mutableStateOf(SaveState.SAVED) }
    var status by remember(note.id) { mutableStateOf(note.status) }
    var preview by remember(note.id) { mutableStateOf(false) }
    var showLinkPicker by remember { mutableStateOf(false) }
    var showTemplates by remember { mutableStateOf(false) }
    var showShare by remember { mutableStateOf(false) }
    var linkProblem by remember { mutableStateOf<String?>(null) }
    // Set once the note is deleted or filed, so closing the screen does not save it again.
    var skipFlush by remember(note.id) { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val latestTitle by rememberUpdatedState(title)
    val latestBody by rememberUpdatedState(body.text)
    val latestTags by rememberUpdatedState(NoteTags.join(tags))
    val latestStatus by rememberUpdatedState(status)

    fun cleanTitle(value: String) = value.trim().ifEmpty { UNTITLED }

    fun isDirty() = title != savedTitle || body.text != savedBody || NoteTags.join(tags) != savedTags

    /** Writes what is on screen now. The save runs to the end even if the screen moves on, and the state says what happened. */
    suspend fun saveNow() {
        val t = title
        val b = body.text
        val g = NoteTags.join(tags)
        saveState = SaveState.SAVING
        val id = withContext(NonCancellable) { viewModel.saveNote(note.id, note.subjectId, cleanTitle(t), b, status, g) }
        if (id != null) {
            savedTitle = t
            savedBody = b
            savedTags = g
            // Typing may have continued during the save; then it is not saved yet, and the next pass writes it.
            saveState = if (isDirty()) SaveState.UNSAVED else SaveState.SAVED
        } else {
            saveState = SaveState.FAILED
        }
    }

    LaunchedEffect(title, body.text, tags) {
        if (isDirty()) {
            saveState = SaveState.UNSAVED
            delay(600)
            saveNow()
        }
    }
    DisposableEffect(note.id) {
        onDispose {
            val dirty = latestTitle != savedTitle || latestBody != savedBody || latestTags != savedTags
            if (!skipFlush && dirty) {
                viewModel.saveInBackground(note.id, note.subjectId, cleanTitle(latestTitle), latestBody, latestStatus, latestTags)
            }
        }
    }

    val vault by viewModel.data.collectAsStateWithLifecycle()
    val otherNotes = remember(vault, note.id) { vault?.notes.orEmpty().filter { it.id != note.id } }

    fun applyEdit(edit: Edited) {
        body = TextFieldValue(edit.text, TextRange(edit.start, edit.end))
    }

    fun onAction(action: MarkdownAction) {
        val text = body.text
        val s = body.selection.min
        val e = body.selection.max
        when (action) {
            MarkdownAction.BOLD -> applyEdit(MarkdownEdit.toggleWrap(text, s, e, "**", "bold"))
            MarkdownAction.ITALIC -> applyEdit(MarkdownEdit.toggleWrap(text, s, e, "*", "italic"))
            MarkdownAction.STRIKE -> applyEdit(MarkdownEdit.toggleWrap(text, s, e, "~~", "text"))
            MarkdownAction.CODE -> applyEdit(MarkdownEdit.toggleWrap(text, s, e, "`", "code"))
            MarkdownAction.H1 -> applyEdit(MarkdownEdit.heading(text, s, e, 1))
            MarkdownAction.H2 -> applyEdit(MarkdownEdit.heading(text, s, e, 2))
            MarkdownAction.H3 -> applyEdit(MarkdownEdit.heading(text, s, e, 3))
            MarkdownAction.BULLET -> applyEdit(MarkdownEdit.bullet(text, s, e))
            MarkdownAction.NUMBERED -> applyEdit(MarkdownEdit.numbered(text, s, e))
            MarkdownAction.CHECK -> applyEdit(MarkdownEdit.checklist(text, s, e))
            MarkdownAction.QUOTE -> applyEdit(MarkdownEdit.quote(text, s, e))
            MarkdownAction.CODE_BLOCK -> applyEdit(MarkdownEdit.codeBlock(text, s, e))
            MarkdownAction.LINK_NOTE -> showLinkPicker = true
            MarkdownAction.TEMPLATE -> showTemplates = true
        }
    }

    fun openLink(linkTitle: String) {
        val target = otherNotes.firstOrNull { it.title.trim().equals(linkTitle.trim(), ignoreCase = true) }
        if (target != null) {
            linkProblem = null
            navigator.navigate(NoteEditorScreenDestination(noteId = target.id))
        } else {
            linkProblem = "There is no note called \"$linkTitle\"."
        }
    }

    // Save state, in words that match what is really in the database.
    val saveText = when (saveState) {
        SaveState.SAVED -> "Saved"
        SaveState.UNSAVED -> "Not saved yet…"
        SaveState.SAVING -> "Saving…"
        SaveState.FAILED -> "Could not save. Your text is still here. Tap to try again."
    }
    Text(
        text = saveText,
        style = MaterialTheme.typography.labelLarge,
        color = if (saveState == SaveState.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = if (saveState == SaveState.FAILED) Modifier.clickable { scope.launch { saveNow() } } else Modifier,
    )

    AppTextField(
        value = title,
        onValueChange = { title = it },
        label = { Text("Title") },
        singleLine = true,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ExpressiveChip(selected = !preview, onClick = { preview = false }, label = { Text("Edit") })
        ExpressiveChip(selected = preview, onClick = { preview = true }, label = { Text("Preview") })
    }

    if (preview) {
        MarkdownPreview(
            text = body.text,
            onToggleCheck = { line -> body = body.copy(text = MarkdownEdit.toggleCheckboxLine(body.text, line)) },
            onOpenLink = { openLink(it) },
        )
        linkProblem?.let { HintText(it) }
    } else {
        MarkdownToolbar(onAction = { onAction(it) })
        AppTextField(
            value = body,
            onValueChange = { body = it },
            label = { Text("Note") },
            minLines = 12,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        HintText("Changes are saved automatically. Link another note with [[its title]].")
    }

    Text("Tags", style = MaterialTheme.typography.titleMedium)
    TagEditor(
        tags = tags,
        otherNotesTags = otherNotes.map { it.tags },
        onChange = { tags = it },
    )

    // Notes that link here.
    val mentionedIn = remember(otherNotes, note.title) {
        if (note.title.isBlank()) emptyList() else otherNotes.filter { it.bodyText.contains("[[${note.title}]]", ignoreCase = true) }
    }
    if (mentionedIn.isNotEmpty()) {
        Text("Linked from", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            mentionedIn.forEach { other ->
                ExpressiveChip(
                    selected = false,
                    onClick = { navigator.navigate(NoteEditorScreenDestination(noteId = other.id)) },
                    label = { Text(other.title.ifBlank { UNTITLED }) },
                )
            }
        }
    }

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
                        viewModel.saveNote(note.id, note.subjectId, cleanTitle(title), body.text, NoteStatus.ACTIVE, NoteTags.join(tags))
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
                        viewModel.saveNote(note.id, note.subjectId, cleanTitle(title), body.text, NoteStatus.FINISHED, NoteTags.join(tags))
                        navigator.navigateUp()
                    }
                },
            )
        }
        PillButton(text = "Delete", tonal = true, onClick = { confirmDelete = true })
    }
    PillButton(text = "Share or export", tonal = true, onClick = { showShare = true })
    ShareHost(viewModel)
    if (showShare) {
        ShareFormatDialog(
            formats = listOf(ShareFormat.PDF, ShareFormat.MARKDOWN, ShareFormat.TEXT, ShareFormat.ZIP),
            onPick = { format ->
                showShare = false
                // What is on screen, even if the automatic save has not run yet.
                val current = note.copy(title = cleanTitle(title), bodyText = body.text, tags = NoteTags.join(tags))
                viewModel.share(listOf(current), format)
            },
            onDismiss = { showShare = false },
        )
    }

    if (showLinkPicker) {
        LinkNoteDialog(
            notes = otherNotes,
            onPick = { picked ->
                showLinkPicker = false
                applyEdit(MarkdownEdit.insert(body.text, body.selection.min, body.selection.max, "[[$picked]]"))
            },
            onDismiss = { showLinkPicker = false },
        )
    }
    if (showTemplates) {
        TemplateDialog(
            onPick = { template ->
                showTemplates = false
                val text = body.text
                if (text.isBlank()) {
                    applyEdit(Edited(template.body, template.body.length, template.body.length))
                } else {
                    val at = body.selection.max
                    val lead = if (at == 0 || text[at - 1] == '\n') "" else "\n"
                    applyEdit(MarkdownEdit.insert(text, at, at, lead + template.body))
                }
            },
            onDismiss = { showTemplates = false },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this note?") },
            text = { Text("${cleanTitle(title)} moves to the Trash. You can restore it from Settings, Trash, for 30 days.") },
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
