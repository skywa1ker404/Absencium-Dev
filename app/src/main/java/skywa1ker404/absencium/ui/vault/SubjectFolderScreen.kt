package skywa1ker404.absencium.ui.vault

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableIntStateOf
import com.ramcosta.composedestinations.generated.destinations.SyllabusScreenDestination
import skywa1ker404.absencium.data.db.GuideType
import skywa1ker404.absencium.data.syllabus.SyllabusTree
import skywa1ker404.absencium.ui.components.ExpressiveChip
import skywa1ker404.absencium.ui.syllabus.SyllabusPickerDialog
import skywa1ker404.absencium.ui.syllabus.syllabusLabel
import skywa1ker404.absencium.ui.vault.entry.ChoiceBar
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
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var topicFilter by rememberSaveable { mutableStateOf<Long?>(null) }
    var showFilter by remember { mutableStateOf(false) }
    var fileGuide by remember { mutableStateOf<GuideEntity?>(null) }
    val nodes by remember(subjectId) { viewModel.syllabusNodes(subjectId) }.collectAsStateWithLifecycle(emptyList())
    val colors = MaterialTheme.colorScheme

    val loaded = data
    val subject = loaded?.subjects?.firstOrNull { it.id == subjectId }

    // A lazy list: a folder with hundreds of notes only builds the rows on screen instead of every card at once.
    ScreenScaffold(
        title = subject?.name ?: "Folder",
        onBack = { navigator.navigateUp() },
        scrollable = false,
        overlay = {
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
        },
    ) {
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
        // The notes under the chosen topic and everything inside it; null shows every note.
        val allowed = remember(nodes, topicFilter) { topicFilter?.let { SyllabusTree.withDescendants(nodes, it).toSet() } }
        val activeShown = if (allowed == null) active else active.filter { it.syllabusNodeId in allowed }
        val finishedShown = if (allowed == null) finished else finished.filter { it.syllabusNodeId in allowed }
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

            item(key = "tabs") {
                val names = listOf("Notes", "Study material", "Syllabus")
                val icons = listOf(AppIcons.Description, AppIcons.Book, AppIcons.School)
                skywa1ker404.absencium.ui.components.TabPill(
                    options = listOf(0, 1, 2),
                    selected = tab,
                    label = { names[it] },
                    icon = { icons[it] },
                    onSelect = { tab = it; selectedIds = emptySet() },
                )
            }

            if (tab == 0) {
            item(key = "topic-filter") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExpressiveChip(
                        selected = topicFilter != null,
                        onClick = { showFilter = true },
                        label = { Text("Topic: " + if (topicFilter == null) "All" else syllabusLabel(nodes, topicFilter).substringAfterLast(" › ")) },
                    )
                    if (topicFilter != null) TextButton(onClick = { topicFilter = null }) { Text("Clear") }
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
            if (activeShown.isEmpty()) {
                item(key = "notes-empty") {
                    HintText(
                        if (topicFilter != null) "No notes under this topic. Clear the topic to see every note."
                        else if (finished.isEmpty() && drafts.isEmpty()) "This folder is empty. Add an entry above, or press and hold a note later to select several."
                        else "No notes here right now. Notes you mark as finished move to the archive below.",
                    )
                }
            }
            items(activeShown, key = { "note-${it.id}" }) { note ->
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
                            text = "${finishedShown.size} ${if (finishedShown.size == 1) "note" else "notes"} archived",
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
                items(finishedShown, key = { "done-${it.id}" }) { note ->
                    NoteCard(
                        note,
                        photoCount = loaded.attachmentsOf(note.id).size,
                        selected = note.id in selectedIds,
                        onLongClick = { toggle(note.id) },
                        onClick = { if (selecting) toggle(note.id) else navigator.navigate(NoteEditorScreenDestination(noteId = note.id)) },
                    )
                }
            }

            }

            if (tab == 1) {
                item(key = "material-intro") {
                    HintText("Previous year papers, answer books, guides and the official syllabus of this subject. File each one under the topic it covers.")
                }
                item(key = "material-add") {
                    PillButton(text = "Add study material", tonal = true, onClick = { navigator.navigate(NewEntryScreenDestination(subjectId = subjectId)) })
                }
                if (guides.isEmpty()) {
                    item(key = "guides-empty") { HintText("Nothing here yet. Add a PDF from a new entry with the Guide / Answer PDF format.") }
                }
                GuideType.entries.forEach { type ->
                    val ofType = guides.filter { it.guideType == type }
                    if (ofType.isNotEmpty()) {
                        item(key = "type-${type.name}") { SectionTitle(type.sectionTitle()) }
                        items(ofType, key = { "guide-${it.id}" }) { guide ->
                            GuideCard(
                                guide = guide,
                                topic = syllabusLabel(nodes, guide.syllabusNodeId).substringAfterLast(" › "),
                                onFile = { fileGuide = guide },
                                onClick = {
                                    openGuide(context, guide)?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                },
                                onDelete = { guideToDelete = guide },
                            )
                        }
                    }
                }
            }

            if (tab == 2) {
                item(key = "syllabus-card") {
                    val overall = SyllabusTree.progress(nodes, null)
                    AppCard(modifier = Modifier.fillMaxWidth(), onClick = { navigator.navigate(SyllabusScreenDestination(subjectId = subjectId)) }) {
                        Text(
                            if (nodes.isEmpty()) "No syllabus yet" else "${(overall * 100).toInt()}% of the syllabus studied",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            if (nodes.isEmpty()) "Add units, chapters and topics, or import an outline." else "${nodes.size} items. Tap to open the full tree.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
                val units = nodes.filter { it.parentId == null }.sortedWith(compareBy({ it.position }, { it.id }))
                items(units, key = { "unit-${it.id}" }) { unit ->
                    AppCard(modifier = Modifier.fillMaxWidth(), onClick = { navigator.navigate(SyllabusScreenDestination(subjectId = subjectId)) }) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(unit.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            Text("${(SyllabusTree.progress(nodes, unit.id) * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = selecting,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = androidx.compose.animation.scaleIn(
                initialScale = 0.6f,
                animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
            ) + androidx.compose.animation.slideInVertically(
                initialOffsetY = { -it },
                animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
            ) + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.scaleOut(targetScale = 0.7f) + androidx.compose.animation.slideOutVertically { -it } + androidx.compose.animation.fadeOut(),
        ) {
            val allFinished = selected.isNotEmpty() && selected.all { it.status == NoteStatus.FINISHED }
            // Material 3 Expressive floating toolbar: the count on the left, the actions in the middle, Cancel on the right.
            HorizontalFloatingToolbar(
                expanded = true,
                modifier = Modifier.padding(top = 4.dp),
                leadingContent = {
                    // The count sits on a round primary badge so it reads as the state of the whole toolbar.
                    Box(
                        modifier = Modifier.padding(start = 4.dp).size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) { Text("${selected.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary) }
                },
                trailingContent = {
                    IconButton(onClick = { selectedIds = emptySet() }) { Icon(AppIcons.Close, contentDescription = "Cancel selection") }
                },
                content = {
                    SelectionAction(AppIcons.Check, if (allFinished) "Reopen" else "Finish") {
                        viewModel.setNotesStatus(selected, if (allFinished) NoteStatus.ACTIVE else NoteStatus.FINISHED)
                        selectedIds = emptySet()
                    }
                    SelectionAction(AppIcons.Share, "Share") { showShare = true }
                    SelectionAction(AppIcons.Folder, "Move") { showMove = true }
                    SelectionAction(AppIcons.Delete, "Delete", destructive = true) { confirmBatchDelete = true }
                },
            )
        }
        }
    }

    if (showFilter) {
        SyllabusPickerDialog(
            nodes = nodes,
            current = topicFilter,
            noneLabel = "All topics",
            title = "Show notes under",
            onPick = { topicFilter = it; showFilter = false },
            onDismiss = { showFilter = false },
            onOpenSyllabus = { showFilter = false; navigator.navigate(SyllabusScreenDestination(subjectId = subjectId)) },
        )
    }
    fileGuide?.let { guide ->
        SyllabusPickerDialog(
            nodes = nodes,
            current = guide.syllabusNodeId,
            onPick = { viewModel.setGuideSyllabus(guide.id, it); fileGuide = null },
            onDismiss = { fileGuide = null },
            onOpenSyllabus = { fileGuide = null; navigator.navigate(SyllabusScreenDestination(subjectId = subjectId)) },
        )
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

}

/** One action of the selection toolbar: an icon with its name under it. */
@Composable
private fun SelectionAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, destructive: Boolean = false, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tint = if (destructive) colors.error else colors.onSurface
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}
