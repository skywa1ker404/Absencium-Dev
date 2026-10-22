package skywa1ker404.absencium.ui.vault

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import skywa1ker404.absencium.ui.components.ExpressiveChip
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.containerViewModel
import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.vault.entry.PhotoPreviewDialog
import skywa1ker404.absencium.ui.vault.entry.PhotoThumb
import skywa1ker404.absencium.ui.vault.markdown.EditHistory
import skywa1ker404.absencium.ui.vault.markdown.EditKind
import skywa1ker404.absencium.ui.vault.markdown.EditStyle
import skywa1ker404.absencium.ui.vault.markdown.Edited
import skywa1ker404.absencium.ui.vault.markdown.MarkdownEdit
import skywa1ker404.absencium.ui.vault.markdown.NoteBlocks
import skywa1ker404.absencium.ui.vault.markdown.NoteTags
import skywa1ker404.absencium.ui.vault.markdown.NoteTemplate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val UNTITLED = "Untitled note"

/** Where the text on screen stands compared with what is in the database. */
private enum class SaveState { SAVED, UNSAVED, SAVING, FAILED }

/**
 * A saved note. It opens as a formatted page; tapping the page starts editing and the check mark in the top bar goes back to
 * the page. A note that was just created opens straight into editing ([startInEdit]). Changes are saved a moment after typing
 * stops, and again when the screen closes.
 */
@Destination<RootGraph>
@Composable
fun NoteEditorScreen(noteId: Long, navigator: DestinationsNavigator, startInEdit: Boolean = false) {
    val viewModel = containerViewModel { VaultViewModel(it) }
    val message by viewModel.message.collectAsStateWithLifecycle()
    var note by remember { mutableStateOf<NoteEntity?>(null) }
    var missing by remember { mutableStateOf(false) }

    LaunchedEffect(noteId) {
        note = viewModel.getNote(noteId)
        if (note == null) missing = true
    }

    val loaded = note
    if (loaded != null) {
        EditorBody(loaded, viewModel, navigator, startInEdit, message)
    } else {
        ScreenScaffold(title = "Note", onBack = { navigator.navigateUp() }) {
            HintText(if (missing) "This note could not be found. It may have been deleted." else "Loading...")
        }
    }
}

/** The note text as it is drawn while editing: formatting characters dimmed, the text they style shown styled. */
private fun buildEditText(text: String, marker: Color, link: Color, codeBackground: Color): AnnotatedString = buildAnnotatedString {
    append(text)
    for (r in EditStyle.ranges(text)) {
        if (r.end <= r.start || r.end > text.length) continue
        val style = when (r.kind) {
            EditKind.MARKER -> SpanStyle(color = marker)
            EditKind.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
            EditKind.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
            EditKind.STRIKE -> SpanStyle(textDecoration = TextDecoration.LineThrough)
            EditKind.UNDERLINE -> SpanStyle(textDecoration = TextDecoration.Underline)
            EditKind.HIGHLIGHT -> SpanStyle(background = Color(0xFFFFE066).copy(alpha = 0.55f))
            EditKind.COLOR -> SpanStyle(color = Color(0xFF000000.toInt() or r.arg))
            EditKind.SIZE -> SpanStyle(fontSize = r.arg.sp)
            EditKind.HEADING -> SpanStyle(
                fontWeight = FontWeight.Bold,
                fontSize = when (r.arg) { 1 -> 28.sp; 2 -> 24.sp; 3 -> 20.sp; else -> 18.sp },
            )
            EditKind.CODE -> SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground)
            EditKind.LINK -> SpanStyle(color = link, textDecoration = TextDecoration.Underline)
        }
        addStyle(style, r.start, r.end)
    }
}

private fun editTransformation(source: String, styled: AnnotatedString) = VisualTransformation { shown ->
    if (shown.text == source) TransformedText(styled, OffsetMapping.Identity) else TransformedText(shown, OffsetMapping.Identity)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditorBody(note: NoteEntity, viewModel: VaultViewModel, navigator: DestinationsNavigator, startInEdit: Boolean, message: String?) {
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val bodyFocus = remember { FocusRequester() }
    val titleFocus = remember { FocusRequester() }
    var title by remember(note.id) { mutableStateOf(note.title) }
    var body by remember(note.id) { mutableStateOf(TextFieldValue(note.bodyText)) }
    var tags by remember(note.id) { mutableStateOf(NoteTags.parse(note.tags)) }
    var savedTitle by remember(note.id) { mutableStateOf(note.title) }
    var savedBody by remember(note.id) { mutableStateOf(note.bodyText) }
    var savedTags by remember(note.id) { mutableStateOf(NoteTags.join(NoteTags.parse(note.tags))) }
    var saveState by remember(note.id) { mutableStateOf(SaveState.SAVED) }
    var status by remember(note.id) { mutableStateOf(note.status) }
    var editing by remember(note.id) { mutableStateOf(startInEdit || note.bodyText.isBlank()) }
    var panel by remember(note.id) { mutableStateOf(EditorPanel.NONE) }
    var lastPanel by remember { mutableStateOf(EditorPanel.TEXT) }
    var menuOpen by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var showLinkPicker by remember { mutableStateOf(false) }
    var showShare by remember { mutableStateOf(false) }
    var linkProblem by remember { mutableStateOf<String?>(null) }
    // Set once the note is deleted or filed, so closing the screen does not save it again.
    var skipFlush by remember(note.id) { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val history = remember(note.id) { EditHistory() }
    var historyVersion by remember(note.id) { mutableIntStateOf(0) }

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

    // Typing records history (bursts merge); a toolbar action always starts a new step.
    fun change(next: TextFieldValue, force: Boolean = false) {
        if (next.text != body.text) {
            history.record(Edited(body.text, body.selection.min, body.selection.max), System.currentTimeMillis(), force)
            historyVersion++
        }
        body = next
    }

    fun apply(edit: Edited) = change(TextFieldValue(edit.text, TextRange(edit.start, edit.end)), force = true)

    fun undo() {
        history.undo(Edited(body.text, body.selection.min, body.selection.max))?.let {
            body = TextFieldValue(it.text, TextRange(it.start, it.end))
            historyVersion++
        }
    }

    fun redo() {
        history.redo(Edited(body.text, body.selection.min, body.selection.max))?.let {
            body = TextFieldValue(it.text, TextRange(it.start, it.end))
            historyVersion++
        }
    }

    fun enterEdit() {
        if (!editing) {
            body = body.copy(selection = TextRange(body.text.length))
            editing = true
        }
    }

    fun leaveEdit() {
        panel = EditorPanel.NONE
        editing = false
    }

    LaunchedEffect(editing) {
        if (editing) {
            delay(60)
            runCatching { bodyFocus.requestFocus() }
        } else {
            focusManager.clearFocus()
        }
    }
    if (panel != EditorPanel.NONE) lastPanel = panel
    BackHandler(enabled = editing && panel != EditorPanel.NONE) { panel = EditorPanel.NONE }

    fun openLink(linkTitle: String) {
        val target = otherNotes.firstOrNull { it.title.trim().equals(linkTitle.trim(), ignoreCase = true) }
        if (target != null) {
            linkProblem = null
            navigator.navigate(NoteEditorScreenDestination(noteId = target.id))
        } else {
            linkProblem = "There is no note called \"$linkTitle\"."
        }
    }

    fun insertTemplate(template: NoteTemplate) {
        val text = body.text
        if (text.isBlank()) {
            apply(Edited(template.body, template.body.length, template.body.length))
        } else {
            val at = body.selection.max
            val lead = if (at == 0 || text[at - 1] == '\n') "" else "\n"
            apply(MarkdownEdit.insert(text, at, at, lead + template.body))
        }
    }

    val photos = vault?.attachmentsOf(note.id).orEmpty().filter { it.kind == AttachmentKind.IMAGE }
    var previewIndex by remember { mutableStateOf<Int?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        viewModel.addPhotos(note.id, uris)
    }

    val saveText = when (saveState) {
        SaveState.SAVED -> "Saved"
        SaveState.UNSAVED -> "Not saved yet…"
        SaveState.SAVING -> "Saving…"
        SaveState.FAILED -> "Could not save. Your text is still here. Tap to try again."
    }
    val dateText = remember(note.createdDate) {
        DateTimeFormatter.ofPattern("d MMM yyyy").format(Instant.ofEpochMilli(note.createdDate).atZone(ZoneId.systemDefault()))
    }

    ScreenScaffold(
        title = "Note",
        onBack = { navigator.navigateUp() },
        customTopBar = {
            // Read so the buttons refresh when the history changes.
            @Suppress("UNUSED_VARIABLE") val version = historyVersion
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            0f to colors.background.copy(alpha = 0.9f),
                            0.7f to colors.background.copy(alpha = 0.55f),
                            1f to Color.Transparent,
                        ),
                    )
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { navigator.navigateUp() }) { Icon(AppIcons.ArrowBack, contentDescription = "Back") }
                Spacer(modifier = Modifier.weight(1f))
                if (editing) {
                    IconButton(onClick = { undo() }, enabled = history.canUndo) { Icon(AppIcons.Undo, contentDescription = "Undo") }
                    IconButton(onClick = { redo() }, enabled = history.canRedo) { Icon(AppIcons.Redo, contentDescription = "Redo") }
                    IconButton(onClick = { leaveEdit() }) { Icon(AppIcons.Check, contentDescription = "Done editing") }
                } else {
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(AppIcons.MoreVert, contentDescription = "More") }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text("Notes info") }, onClick = { menuOpen = false; showInfo = true })
                            DropdownMenuItem(text = { Text("Add photos") }, onClick = { menuOpen = false; photoPicker.launch("image/*") })
                            DropdownMenuItem(text = { Text("Share or export") }, onClick = { menuOpen = false; showShare = true })
                            DropdownMenuItem(
                                text = { Text(if (status == NoteStatus.FINISHED) "Reopen" else "Mark as finished") },
                                onClick = {
                                    menuOpen = false
                                    skipFlush = true
                                    val next = if (status == NoteStatus.FINISHED) NoteStatus.ACTIVE else NoteStatus.FINISHED
                                    scope.launch {
                                        // Finished notes move to the subject's Finished / Written Notes archive.
                                        viewModel.saveNote(note.id, note.subjectId, cleanTitle(title), body.text, next, NoteTags.join(tags))
                                        navigator.navigateUp()
                                    }
                                },
                            )
                            DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; confirmDelete = true })
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (editing) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AnimatedVisibility(
                        visible = panel != EditorPanel.NONE,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        val s = body.selection.min
                        val e = body.selection.max
                        when (lastPanel) {
                            EditorPanel.TEXT, EditorPanel.NONE -> TextPanel(
                                onWrap = { marker, placeholder -> apply(MarkdownEdit.toggleWrap(body.text, s, e, marker, placeholder)) },
                                onHeading = { apply(MarkdownEdit.heading(body.text, s, e, it)) },
                                onColor = { apply(MarkdownEdit.setColor(body.text, s, e, it)) },
                                onSize = { apply(MarkdownEdit.setSize(body.text, s, e, it)) },
                                onInsert = { apply(MarkdownEdit.insert(body.text, s, e, it)) },
                                onDivider = { apply(NoteBlocks.insertEmbed(body.text, e, it)) },
                                onTemplate = { insertTemplate(it) },
                            )
                            EditorPanel.LISTS -> ListsPanel(
                                onAlign = { apply(MarkdownEdit.setAlign(body.text, s, e, it)) },
                                onBullet = { apply(MarkdownEdit.bullet(body.text, s, e)) },
                                onNumbered = { apply(MarkdownEdit.numbered(body.text, s, e)) },
                                onLettered = { apply(MarkdownEdit.lettered(body.text, s, e)) },
                                onQuote = { apply(MarkdownEdit.quote(body.text, s, e)) },
                            )
                            EditorPanel.ADD -> AddPanel(
                                onTable = { panel = EditorPanel.NONE; apply(NoteBlocks.insertEmbed(body.text, e, NoteBlocks.newTable(3, 3))) },
                                onLinkNote = { panel = EditorPanel.NONE; showLinkPicker = true },
                                onCodeBlock = { panel = EditorPanel.NONE; apply(MarkdownEdit.codeBlock(body.text, s, e)) },
                            )
                        }
                    }
                    EditorToolbar(
                        panel = panel,
                        onPanel = { panel = it },
                        onCheck = { apply(MarkdownEdit.checklist(body.text, body.selection.min, body.selection.max)) },
                        onImage = { photoPicker.launch("image/*") },
                    )
                }
            }
        },
    ) {
        if (message != null) {
            AppCard(modifier = Modifier.fillMaxWidth(), highlighted = true, onClick = { viewModel.clearMessage() }) {
                Text(message, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Title: a text field while editing, plain text on the page.
        if (editing) {
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineMedium.copy(color = colors.onSurface, fontWeight = FontWeight.Bold),
                cursorBrush = SolidColor(colors.primary),
                modifier = Modifier.fillMaxWidth().focusRequester(titleFocus),
                decorationBox = { inner ->
                    Box {
                        if (title.isEmpty()) Text("Title", style = MaterialTheme.typography.headlineMedium, color = colors.onSurfaceVariant.copy(alpha = 0.6f))
                        inner()
                    }
                },
            )
        } else {
            Text(
                text = title.ifBlank { UNTITLED },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (title.isBlank()) colors.onSurfaceVariant else colors.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { enterEdit() },
            )
        }

        // Date, size and where the note belongs (a syllabus topic once the syllabus exists), and the save state.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$dateText  |  ${"%,d".format(body.text.length)} characters  |  Uncategorized",
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceVariant,
            )
        }
        Text(
            text = saveText,
            style = MaterialTheme.typography.labelMedium,
            color = if (saveState == SaveState.FAILED) colors.error else colors.onSurfaceVariant,
            modifier = if (saveState == SaveState.FAILED) Modifier.clickable { scope.launch { saveNow() } } else Modifier,
        )

        TagEditor(
            tags = tags,
            otherNotesTags = otherNotes.map { it.tags },
            onChange = { tags = it },
        )

        if (editing) {
            val marker = colors.onSurfaceVariant.copy(alpha = 0.45f)
            val link = colors.primary
            val codeBackground = colors.surfaceContainerHighest
            val source = body.text
            val styled = remember(source, marker, link, codeBackground) { buildEditText(source, marker, link, codeBackground) }
            val transformation = remember(source, styled) { editTransformation(source, styled) }
            BasicTextField(
                value = body,
                onValueChange = { change(it) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface, lineHeight = TextUnit.Unspecified),
                cursorBrush = SolidColor(colors.primary),
                visualTransformation = transformation,
                modifier = Modifier.fillMaxWidth().heightIn(min = 320.dp).focusRequester(bodyFocus),
                decorationBox = { inner ->
                    Box {
                        if (body.text.isEmpty()) Text("Start writing…", style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant.copy(alpha = 0.6f))
                        inner()
                    }
                },
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 320.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { enterEdit() },
            ) {
                MarkdownPreview(
                    text = body.text,
                    onToggleCheck = { line -> body = body.copy(text = MarkdownEdit.toggleCheckboxLine(body.text, line)) },
                    onOpenLink = { openLink(it) },
                )
            }
            linkProblem?.let { HintText(it) }
        }

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
        // Room for the toolbar and keyboard so the last line is never hidden behind them.
        if (editing) Spacer(modifier = Modifier.height(24.dp))

        previewIndex?.let { index ->
            PhotoPreviewDialog(
                paths = photos.map { it.filePath },
                startIndex = index.coerceIn(0, (photos.size - 1).coerceAtLeast(0)),
                onBack = { previewIndex = null },
            )
        }

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
                    apply(MarkdownEdit.insert(body.text, body.selection.min, body.selection.max, "[[$picked]]"))
                },
                onDismiss = { showLinkPicker = false },
            )
        }

        if (showInfo) {
            val subjectName = vault?.subjects.orEmpty().firstOrNull { it.id == note.subjectId }?.name ?: "Unknown"
            val words = body.text.split(Regex("\\s+")).count { it.isNotEmpty() }
            val edited = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm").format(Instant.ofEpochMilli(maxOf(note.updatedAt, note.createdDate)).atZone(ZoneId.systemDefault()))
            AlertDialog(
                onDismissRequest = { showInfo = false },
                shape = RoundedCornerShape(28.dp),
                title = { Text("Notes info") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        InfoLine("Subject", subjectName)
                        InfoLine("Category", "Uncategorized")
                        InfoLine("Created", dateText)
                        InfoLine("Last edited", edited)
                        InfoLine("Characters", "%,d".format(body.text.length))
                        InfoLine("Words", "%,d".format(words))
                        InfoLine("Status", status.name.lowercase().replaceFirstChar { it.uppercase() })
                        InfoLine("Tags", if (tags.isEmpty()) "None" else tags.joinToString(", ") { "#$it" })
                        InfoLine("Photos", photos.size.toString())
                    }
                },
                confirmButton = { TextButton(onClick = { showInfo = false }) { Text("Close") } },
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
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 16.dp))
    }
}
