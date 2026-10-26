package skywa1ker404.absencium.ui.vault

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import skywa1ker404.absencium.ui.components.verticalScrollbar
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import skywa1ker404.absencium.ui.icons.AppIcons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AiSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.NewEntryScreenDestination
import com.ramcosta.composedestinations.generated.destinations.NoteEditorScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import skywa1ker404.absencium.data.db.GuideEntity
import skywa1ker404.absencium.data.db.NoteStatus
import skywa1ker404.absencium.data.share.ShareFormat
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.vault.entry.AddEntryPopup
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.MessageCard
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.components.SectionTitle
import skywa1ker404.absencium.ui.containerViewModel

/** One subject's folder: its notes, the Finished / Written Notes archive, and Guides and Question Banks. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Destination<RootGraph>
@Composable
fun SubjectFolderScreen(subjectId: Long, navigator: DestinationsNavigator) {
    val viewModel = containerViewModel { VaultViewModel(it) }
    val data by viewModel.data.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var finishedOpen by rememberSaveable { mutableStateOf(false) }
    var addEntry by remember { mutableStateOf(false) }
    var guideToDelete by remember { mutableStateOf<GuideEntity?>(null) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var confirmBatchDelete by remember { mutableStateOf(false) }
    var showMove by remember { mutableStateOf(false) }
    var showShare by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme

    val loaded = data
    val subject = loaded?.subjects?.firstOrNull { it.id == subjectId }

    // A lazy list: a folder with hundreds of notes only builds the rows on screen instead of every card at once.
    ScreenScaffold(title = subject?.name ?: "Folder", onBack = { navigator.navigateUp() }, scrollable = false) {
        if (loaded == null) {
            HintText("Loading...")
            return@ScreenScaffold
        }
        if (subject == null) {
            HintText("This subject no longer exists.")
            return@ScreenScaffold
        }
        val active = remember(loaded, subjectId) { loaded.notesOf(subjectId, NoteStatus.ACTIVE) }
        val finished = remember(loaded, subjectId) { loaded.notesOf(subjectId, NoteStatus.FINISHED) }
        val guides = remember(loaded, subjectId) { loaded.guides.filter { it.subjectId == subjectId } }
        val drafts = remember(loaded, subjectId) { loaded.notesOf(subjectId, NoteStatus.DRAFT) }
        // Selected notes that still exist (one may have been deleted or moved elsewhere while selected).
        val everyNote = active + finished
        val selected = everyNote.filter { it.id in selectedIds }
        val selecting = selectedIds.isNotEmpty()
        fun toggle(id: Long) {
            selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
        }
        BackHandler(enabled = selecting) { selectedIds = emptySet() }
        val listState = rememberLazyListState()

        Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().verticalScrollbar(listState, colors.onSurfaceVariant),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            if (message != null) {
                item(key = "message") {
                    AppCard(modifier = Modifier.fillMaxWidth(), highlighted = true, onClick = { viewModel.clearMessage() }) {
                        Text(message.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item(key = "crumbs") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Vault",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primary,
                        modifier = Modifier.clickable { navigator.navigateUp() },
                    )
                    Text("\u203a", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                    Text(subject.name, style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant, maxLines = 1)
                }
            }
            item(key = "new") {
                if (selecting) {
                    Text("${selected.size} selected. Use the bar at the top.", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                } else {
                    PillButton(
                        text = "New entry in this folder",
                        onClick = { addEntry = true },
                    )
                }
            }

            if (drafts.isNotEmpty()) {
                item(key = "drafts-title") { SectionTitle("Drafts") }
                items(drafts, key = { "draft-${it.id}" }) { draft ->
                    NoteCard(
                        draft,
                        photoCount = loaded.attachmentsOf(draft.id).size,
                        onClick = { navigator.navigate(NewEntryScreenDestination(subjectId = subjectId, draftId = draft.id)) },
                    )
                }
            }
            item(key = "notes-title") { SectionTitle("Notes") }
            if (active.isEmpty()) {
                item(key = "notes-empty") {
                    HintText(
                        if (finished.isEmpty() && drafts.isEmpty()) "This folder is empty. Add an entry above, or press and hold a note later to select several."
                        else "No notes here right now. Notes you mark as finished move to the archive below.",
                    )
                }
            }
            items(active, key = { "note-${it.id}" }) { note ->
                NoteCard(
                    note,
                    photoCount = loaded.attachmentsOf(note.id).size,
                    selected = note.id in selectedIds,
                    onLongClick = { toggle(note.id) },
                    onClick = { if (selecting) toggle(note.id) else navigator.navigate(NoteEditorScreenDestination(noteId = note.id)) },
                )
            }

            item(key = "finished-title") { SectionTitle("Finished / Written Notes") }
            item(key = "finished-toggle") {
                AppCard(modifier = Modifier.fillMaxWidth(), onClick = { finishedOpen = !finishedOpen }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "${finished.size} ${if (finished.size == 1) "note" else "notes"} archived",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = if (finishedOpen) "Hide" else "Show",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.primary,
                        )
                    }
                }
            }
            if (finishedOpen) {
                items(finished, key = { "done-${it.id}" }) { note ->
                    NoteCard(
                        note,
                        photoCount = loaded.attachmentsOf(note.id).size,
                        selected = note.id in selectedIds,
                        onLongClick = { toggle(note.id) },
                        onClick = { if (selecting) toggle(note.id) else navigator.navigate(NoteEditorScreenDestination(noteId = note.id)) },
                    )
                }
            }

            item(key = "guides-title") { SectionTitle("Guides & Question Banks") }
            if (guides.isEmpty()) {
                item(key = "guides-empty") { HintText("No PDFs yet. Add one from a new entry with the Guide / Answer PDF format.") }
            }
            items(guides, key = { "guide-${it.id}" }) { guide ->
                GuideCard(
                    guide = guide,
                    onClick = {
                        openGuide(context, guide)?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                    },
                    onDelete = { guideToDelete = guide },
                )
            }
        }
        if (selecting) {
            val allFinished = selected.isNotEmpty() && selected.all { it.status == NoteStatus.FINISHED }
            // Material 3 Expressive floating toolbar: the count on the left, the actions in the middle, Cancel on the right.
            HorizontalFloatingToolbar(
                expanded = true,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp),
                leadingContent = { Text("${selected.size}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 12.dp)) },
                trailingContent = {
                    IconButton(onClick = { selectedIds = emptySet() }) { Icon(AppIcons.Close, contentDescription = "Cancel selection") }
                },
                content = {
                    TextButton(onClick = {
                        viewModel.setNotesStatus(selected, if (allFinished) NoteStatus.ACTIVE else NoteStatus.FINISHED)
                        selectedIds = emptySet()
                    }) { Text(if (allFinished) "Reopen" else "Finish") }
                    TextButton(onClick = { showShare = true }) { Text("Share") }
                    TextButton(onClick = { showMove = true }) { Text("Move") }
                    TextButton(onClick = { confirmBatchDelete = true }) { Text("Delete") }
                },
            )
        }
        }
    }

    ShareHost(viewModel)
    if (showShare) {
        val toShare = remember(selectedIds, data) { (data?.notes.orEmpty()).filter { it.id in selectedIds } }
        ShareFormatDialog(
            // One entry can be a PDF, Markdown or text file; several always travel as a ZIP.
            formats = if (toShare.size == 1) listOf(ShareFormat.PDF, ShareFormat.MARKDOWN, ShareFormat.TEXT, ShareFormat.ZIP)
            else listOf(ShareFormat.MARKDOWN, ShareFormat.ZIP),
            onPick = { format ->
                showShare = false
                viewModel.share(toShare, format)
                selectedIds = emptySet()
            },
            onDismiss = { showShare = false },
        )
    }
    if (confirmBatchDelete) {
        val victims = remember(selectedIds, data) { (data?.notes.orEmpty()).filter { it.id in selectedIds } }
        AlertDialog(
            onDismissRequest = { confirmBatchDelete = false },
            title = { Text("Delete ${victims.size} ${if (victims.size == 1) "entry" else "entries"}?") },
            text = { Text("They move to the Trash and can be restored from Settings, Trash, for 30 days. Drafts are deleted for good.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteNotes(victims)
                    selectedIds = emptySet()
                    confirmBatchDelete = false
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmBatchDelete = false }) { Text("Keep") } },
        )
    }
    if (showMove) {
        val targets = (data?.subjects.orEmpty()).filter { it.id != subjectId && it.isActive }
        val moving = remember(selectedIds, data) { (data?.notes.orEmpty()).filter { it.id in selectedIds } }
        AlertDialog(
            onDismissRequest = { showMove = false },
            title = { Text("Move to which subject?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (targets.isEmpty()) HintText("There is no other active subject to move to.")
                    targets.forEach { target ->
                        Text(
                            text = target.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.moveNotes(moving, target.id)
                                    selectedIds = emptySet()
                                    showMove = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showMove = false }) { Text("Cancel") } },
        )
    }

    val pending = guideToDelete
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { guideToDelete = null },
            title = { Text("Remove this PDF?") },
            text = { Text("${pending.fileName} will be removed from the vault and from the notes folder.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteGuide(pending)
                    guideToDelete = null
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { guideToDelete = null }) { Text("Keep") } },
        )
    }

    if (addEntry) {
        AddEntryPopup(
            onDismiss = { addEntry = false },
            onSaved = { id ->
                addEntry = false
                navigator.navigate(NoteEditorScreenDestination(noteId = id, startInEdit = true))
            },
            onOpenAiSettings = {
                addEntry = false
                navigator.navigate(AiSettingsScreenDestination)
            },
            initialSubjectId = subjectId,
        )
    }
}
