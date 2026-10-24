package skywa1ker404.absencium.ui.vault

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AiSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.NoteEditorScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import skywa1ker404.absencium.data.db.AttachmentEntity
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
import skywa1ker404.absencium.ui.vault.entry.PhotoCropDialog
import skywa1ker404.absencium.ui.vault.entry.PhotoPreviewDialog
import skywa1ker404.absencium.ui.vault.entry.PhotoThumb
import skywa1ker404.absencium.ui.vault.markdown.EditHistory
import skywa1ker404.absencium.ui.vault.markdown.Edited
import skywa1ker404.absencium.ui.vault.markdown.EditKind
import skywa1ker404.absencium.ui.vault.markdown.EmbedKind
import skywa1ker404.absencium.ui.vault.markdown.LiveFormat
import skywa1ker404.absencium.ui.vault.markdown.MarkdownEdit
import skywa1ker404.absencium.ui.vault.markdown.MarkerHeal
import skywa1ker404.absencium.ui.vault.markdown.NoteBlocks
import skywa1ker404.absencium.ui.vault.markdown.NoteDoc
import skywa1ker404.absencium.ui.vault.markdown.NoteTemplate
import skywa1ker404.absencium.ui.vault.markdown.PartKind
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val UNTITLED = "Untitled note"

/** Where the text on screen stands compared with what is in the database. */
private enum class SaveState { SAVED, UNSAVED, SAVING, FAILED }

/**
 * A saved note. The page is always editable and always shows the note as it will look: headings, lists, colours, pictures,
 * doodles and tables appear as they are, and the formatting characters stay out of sight. Changes are saved a moment after
 * typing stops, and again when the screen closes. [startInEdit] is kept for the callers that open a new note; the page is the
 * same either way.
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

private val usedAudio = Regex("\\{\\{audio:(\\d{1,18})")
private val usedEmbeds = Regex("\\{\\{(?:image|doodle):(\\d{1,18})")

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditorBody(note: NoteEntity, viewModel: VaultViewModel, navigator: DestinationsNavigator, startInEdit: Boolean, message: String?) {
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val keyboard = LocalSoftwareKeyboardController.current
    val titleFocus = remember { FocusRequester() }
    val focusers = remember(note.id) { HashMap<Int, FocusRequester>() }
    fun focuserFor(i: Int) = focusers.getOrPut(i) { FocusRequester() }

    var title by remember(note.id) { mutableStateOf(note.title) }
    var text by remember(note.id) { mutableStateOf(note.bodyText) }
    var selStart by remember(note.id) { mutableIntStateOf(note.bodyText.length) }
    var selEnd by remember(note.id) { mutableIntStateOf(note.bodyText.length) }
    var focusPart by remember(note.id) { mutableIntStateOf(-1) }
    var focusValue by remember(note.id) { mutableStateOf(TextFieldValue("")) }
    var savedTitle by remember(note.id) { mutableStateOf(note.title) }
    var savedBody by remember(note.id) { mutableStateOf(note.bodyText) }
    var saveState by remember(note.id) { mutableStateOf(SaveState.SAVED) }
    var status by remember(note.id) { mutableStateOf(note.status) }
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
    val latestBody by rememberUpdatedState(text)
    val latestStatus by rememberUpdatedState(status)

    fun cleanTitle(value: String) = value.trim().ifEmpty { UNTITLED }

    fun isDirty() = title != savedTitle || text != savedBody

    /** Writes what is on screen now. The save runs to the end even if the screen moves on, and the state says what happened. */
    suspend fun saveNow() {
        val t = title
        val b = text
        saveState = SaveState.SAVING
        val id = withContext(NonCancellable) { viewModel.saveNote(note.id, note.subjectId, cleanTitle(t), b, status, note.tags) }
        if (id != null) {
            savedTitle = t
            savedBody = b
            // Typing may have continued during the save; then it is not saved yet, and the next pass writes it.
            saveState = if (isDirty()) SaveState.UNSAVED else SaveState.SAVED
        } else {
            saveState = SaveState.FAILED
        }
    }

    LaunchedEffect(title, text) {
        if (isDirty()) {
            saveState = SaveState.UNSAVED
            delay(600)
            saveNow()
        }
    }
    DisposableEffect(note.id) {
        onDispose {
            val dirty = latestTitle != savedTitle || latestBody != savedBody
            if (!skipFlush && dirty) {
                viewModel.saveInBackground(note.id, note.subjectId, cleanTitle(latestTitle), latestBody, latestStatus, note.tags)
            }
        }
    }

    val vault by viewModel.data.collectAsStateWithLifecycle()
    val otherNotes = remember(vault, note.id) { vault?.notes.orEmpty().filter { it.id != note.id } }
    val attachments = vault?.attachmentsOf(note.id).orEmpty()

    val parts = remember(text) { NoteDoc.split(text) }

    /** Puts the cursor (and the keyboard focus target) in the text part that holds [start]; the fields redraw from [text]. */
    fun refocus(next: String, start: Int, end: Int) {
        val ps = NoteDoc.split(next)
        val i = NoteDoc.nearestText(ps, NoteDoc.partIndexAt(ps, start))
        val p = ps[i]
        val shift = if (p.synthetic) 1 else 0
        val ls = (start - p.start - shift).coerceIn(0, p.text.length)
        val le = (end - p.start - shift).coerceIn(ls, p.text.length)
        focusPart = i
        focusValue = TextFieldValue(p.text, TextRange(ls, le))
    }

    /** Replaces the note text. Typing records history (bursts merge); a toolbar action always starts a new step. */
    fun commit(next: String, start: Int = selStart, end: Int = selEnd, force: Boolean = true) {
        if (next != text) {
            history.record(Edited(text, selStart, selEnd), System.currentTimeMillis(), force)
            historyVersion++
        }
        text = next
        selStart = start.coerceIn(0, next.length)
        selEnd = end.coerceIn(selStart, next.length)
        refocus(next, selStart, selEnd)
    }

    fun apply(edit: Edited) = commit(edit.text, edit.start, edit.end, force = true)

    fun undo() {
        history.undo(Edited(text, selStart, selEnd))?.let {
            text = it.text
            selStart = it.start.coerceIn(0, it.text.length)
            selEnd = it.end.coerceIn(selStart, it.text.length)
            refocus(text, selStart, selEnd)
            historyVersion++
        }
    }

    fun redo() {
        history.redo(Edited(text, selStart, selEnd))?.let {
            text = it.text
            selStart = it.start.coerceIn(0, it.text.length)
            selEnd = it.end.coerceIn(selStart, it.text.length)
            refocus(text, selStart, selEnd)
            historyVersion++
        }
    }

    fun hideKeyboard() {
        keyboard?.hide()
    }

    LaunchedEffect(note.id) {
        // A new, empty note opens with the cursor ready; an older one opens as a page to read.
        if (startInEdit || note.bodyText.isBlank()) {
            delay(80)
            val ps = NoteDoc.split(text)
            val i = if (note.bodyText.isBlank()) 0 else NoteDoc.nearestText(ps, ps.lastIndex)
            refocus(text, ps[i].end, ps[i].end)
            runCatching { focuserFor(i).requestFocus() }
        }
    }
    if (panel != EditorPanel.NONE) lastPanel = panel
    BackHandler(enabled = panel != EditorPanel.NONE) { panel = EditorPanel.NONE }

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
        if (text.isBlank()) {
            apply(Edited(template.body, template.body.length, template.body.length))
        } else {
            val at = selEnd
            val lead = if (at == 0 || text[at - 1] == '\n') "" else "\n"
            apply(MarkdownEdit.insert(text, at, at, lead + template.body))
        }
    }

    /** Puts [token] lines on the page at the cursor. */
    fun insertBlock(token: String) = apply(NoteBlocks.insertEmbed(text, selEnd, token))

    var viewing by remember { mutableStateOf<AttachmentEntity?>(null) }
    var cropping by remember { mutableStateOf<AttachmentEntity?>(null) }
    var doodling by remember { mutableStateOf(false) }
    var doodleTarget by remember { mutableStateOf<AttachmentEntity?>(null) }
    var tableStart by remember { mutableStateOf<Int?>(null) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addPhotos(note.id, uris) { ids ->
                insertBlock(ids.joinToString("\n") { NoteBlocks.token(EmbedKind.IMAGE, it, 100) })
            }
        }
    }

    // A photo taken for "Scan a document" opens the crop editor as soon as it is on the page.
    var scanning by remember { mutableStateOf(false) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        val scan = scanning
        scanning = false
        viewModel.cameraFinished(note.id, taken) { photo ->
            insertBlock(NoteBlocks.token(EmbedKind.IMAGE, photo.id, 100))
            if (scan) cropping = photo
        }
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addAudioFiles(note.id, uris) { ids ->
                insertBlock(ids.joinToString("\n") { NoteBlocks.token(EmbedKind.AUDIO, it, 100) })
            }
        }
    }
    val recorderState by viewModel.recorderState.collectAsStateWithLifecycle()
    var recordClock by remember { mutableStateOf(0L) }
    LaunchedEffect(recorderState.recording) {
        while (recorderState.recording) {
            recordClock = viewModel.recordingElapsed()
            delay(200)
        }
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.startRecording() else viewModel.micDenied()
    }
    val appContext = androidx.compose.ui.platform.LocalContext.current
    fun startRecord() {
        val allowed = androidx.core.content.ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (allowed) viewModel.startRecording() else micPermission.launch(android.Manifest.permission.RECORD_AUDIO)
    }
    DisposableEffect(note.id) { onDispose { viewModel.discardRecording() } }

    val saveText = when (saveState) {
        SaveState.SAVED -> "Saved"
        SaveState.UNSAVED -> "Not saved yet"
        SaveState.SAVING -> "Saving…"
        SaveState.FAILED -> "Could not save. Tap to try again"
    }
    val dateText = remember(note.createdDate) {
        DateTimeFormatter.ofPattern("d MMM yyyy").format(Instant.ofEpochMilli(note.createdDate).atZone(ZoneId.systemDefault()))
    }
    // ---- AI panel ----
    var aiState by remember(note.id) { mutableStateOf(AiPanelState()) }
    var aiPrompt by remember(note.id) { mutableStateOf("") }
    val aiHistory = remember(note.id) { mutableStateListOf<AiHistoryItem>() }
    var aiJob by remember(note.id) { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    // The selection the request was made for (null = the whole note), so the answer lands where the text was.
    var aiRange by remember(note.id) { mutableStateOf<Pair<Int, Int>?>(null) }
    val hasSelection = selStart != selEnd
    val sourceText = if (hasSelection) text.substring(selStart.coerceIn(0, text.length), selEnd.coerceIn(0, text.length)) else text

    fun startAi(title: String, request: String, replaces: Boolean) {
        aiRange = if (hasSelection) selStart to selEnd else null
        aiState = aiState.copy(running = true, result = null, error = null, setupMissing = false)
        aiJob = viewModel.askAi(request) { out ->
            aiState = aiState.copy(running = false, result = out.text, resultReplaces = replaces, error = out.error, setupMissing = out.setupMissing)
            out.text?.let {
                aiHistory += AiHistoryItem(title, it, replaces)
                while (aiHistory.size > 20) aiHistory.removeAt(0)
            }
        }
    }

    fun runAiAction(action: AiAction) {
        val trimmed = sourceText.trim()
        if (action.tooShort(trimmed.length)) {
            aiState = aiState.copy(error = "${action.label} needs at least ${action.minChars} characters. Select some text or write a little more.", result = null, setupMissing = false)
            return
        }
        startAi(action.label, action.request(AiAction.excerpt(sourceText, action == AiAction.CONTINUE)), action.replaces)
    }

    fun sendCustomAi() {
        val ask = aiPrompt.trim()
        if (ask.isEmpty() || aiState.running) return
        // An instruction given with text selected edits that text; without a selection it writes new material below.
        startAi(ask.take(40), AiAction.custom(ask, AiAction.excerpt(sourceText, false)), replaces = hasSelection)
        aiPrompt = ""
    }

    fun applyAiResult(replace: Boolean) {
        val result = aiState.result ?: return
        val range = aiRange
        if (replace) {
            if (range != null) {
                apply(MarkdownEdit.insert(text, range.first.coerceIn(0, text.length), range.second.coerceIn(0, text.length), result))
            } else {
                commit(result, result.length, result.length)
            }
        } else {
            val at = (range?.second ?: text.length).coerceIn(0, text.length)
            var lineEnd = at
            while (lineEnd < text.length && text[lineEnd] != '\n') lineEnd++
            val lead = if (text.isEmpty()) "" else "\n\n"
            apply(MarkdownEdit.insert(text, lineEnd, lineEnd, lead + result))
        }
        aiState = aiState.copy(result = null, error = null)
    }

    // Where each part of the page is on screen, so a block let go after a drag can find the line it landed on.
    val partBounds = remember(note.id) { mutableStateMapOf<Int, Pair<Float, Float>>() }

    /** Puts the block that starts at [part] (it covers [lineCount] lines) where it was let go, and aligns a picture by how far across it landed. */
    fun dropBlock(part: skywa1ker404.absencium.ui.vault.markdown.DocPart, lineCount: Int, rootY: Float, xFraction: Float?, ref: EmbedRef?) {
        val ps = NoteDoc.split(text)
        var before = -1
        for ((j, p) in ps.withIndex()) {
            if (p.kind == part.kind && p.line == part.line) continue
            val b = partBounds[j] ?: continue
            if (rootY >= b.first && rootY <= b.second) {
                val f = ((rootY - b.first) / (b.second - b.first).coerceAtLeast(1f)).coerceIn(0f, 1f)
                before = when {
                    p.kind == PartKind.TEXT -> {
                        val n = p.text.split("\n").size
                        p.line + (f * n).toInt().coerceIn(0, n)
                    }
                    f < 0.5f -> p.line
                    p.kind == PartKind.TABLE -> p.line + NoteBlocks.tableLineCount(text, p.line)
                    else -> p.line + 1
                }
                break
            }
        }
        if (before < 0) {
            val firstTop = partBounds[0]?.first ?: 0f
            before = if (rootY < firstTop) 0 else text.split("\n").size
        }
        val (moved, at) = NoteBlocks.moveToLine(text, part.line, lineCount, before)
        var next = moved
        if (ref != null && xFraction != null && ref.widthPercent < 100) {
            val align = when {
                xFraction < 0.33f -> skywa1ker404.absencium.ui.vault.markdown.Align.START
                xFraction > 0.66f -> skywa1ker404.absencium.ui.vault.markdown.Align.END
                else -> skywa1ker404.absencium.ui.vault.markdown.Align.CENTER
            }
            next = NoteBlocks.resize(next, at, ref.widthPercent, align)
        }
        if (next != text) commit(next)
    }

    val palette = LivePalette(accent = colors.primary, muted = colors.onSurfaceVariant, codeBackground = colors.surfaceContainerHighest)

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
                    .height(48.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { navigator.navigateUp() }) { Icon(AppIcons.ArrowBack, contentDescription = "Back") }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { undo() }, enabled = history.canUndo) { Icon(AppIcons.Undo, contentDescription = "Undo") }
                IconButton(onClick = { redo() }, enabled = history.canRedo) { Icon(AppIcons.Redo, contentDescription = "Redo") }
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(AppIcons.MoreVert, contentDescription = "More") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Notes info") }, onClick = { menuOpen = false; showInfo = true })
                        DropdownMenuItem(text = { Text("Share or export") }, onClick = { menuOpen = false; showShare = true })
                        DropdownMenuItem(
                            text = { Text(if (status == NoteStatus.FINISHED) "Reopen" else "Mark as finished") },
                            onClick = {
                                menuOpen = false
                                skipFlush = true
                                val next = if (status == NoteStatus.FINISHED) NoteStatus.ACTIVE else NoteStatus.FINISHED
                                scope.launch {
                                    // Finished notes move to the subject's Finished / Written Notes archive.
                                    viewModel.saveNote(note.id, note.subjectId, cleanTitle(title), text, next, note.tags)
                                    navigator.navigateUp()
                                }
                            },
                        )
                        DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; confirmDelete = true })
                    }
                }
            }
        },
        bottomBar = {
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
                    val s = selStart
                    val e = selEnd
                    AnimatedContent(
                        targetState = lastPanel,
                        transitionSpec = { fadeIn(tween(130)).togetherWith(fadeOut(tween(80))) },
                        label = "editorPanel",
                    ) { shown ->
                    when (shown) {
                        EditorPanel.AI -> AiPanel(
                            state = aiState.copy(
                                scopeLabel = if (hasSelection) "the selected text" else "the whole note",
                                sourceLength = sourceText.trim().length,
                                canReplace = hasSelection || parts.all { it.kind == PartKind.TEXT },
                            ),
                            prompt = aiPrompt,
                            onPrompt = { aiPrompt = it },
                            onSend = { sendCustomAi() },
                            history = aiHistory,
                            onAction = { runAiAction(it) },
                            onReplace = { applyAiResult(replace = true) },
                            onInsert = { applyAiResult(replace = false) },
                            onDiscard = { aiState = aiState.copy(result = null, error = null) },
                            onCancel = { aiJob?.cancel(); aiState = aiState.copy(running = false) },
                            onOpenHistory = { aiState = aiState.copy(result = it.result, resultReplaces = it.replaces, error = null) },
                            onOpenSettings = { navigator.navigate(AiSettingsScreenDestination) },
                            onUse = ::hideKeyboard,
                        )
                        EditorPanel.TEXT, EditorPanel.NONE -> TextPanel(
                            onWrap = { marker, placeholder -> apply(MarkdownEdit.toggleWrap(text, s, e, marker, placeholder)) },
                            onHeading = { apply(MarkdownEdit.heading(text, s, e, it)) },
                            onColor = { apply(MarkdownEdit.setColor(text, s, e, it)) },
                            onSize = { apply(MarkdownEdit.setSize(text, s, e, it)) },
                            onInsert = { apply(MarkdownEdit.insert(text, s, e, it)) },
                            onDivider = { apply(NoteBlocks.insertEmbed(text, e, it)) },
                            onTemplate = { insertTemplate(it) },
                            onUse = ::hideKeyboard,
                        )
                        EditorPanel.LISTS -> ListsPanel(
                            onAlign = { apply(MarkdownEdit.setAlign(text, s, e, it)) },
                            onBullet = { apply(MarkdownEdit.bullet(text, s, e)) },
                            onNumbered = { apply(MarkdownEdit.numbered(text, s, e)) },
                            onLettered = { apply(MarkdownEdit.lettered(text, s, e)) },
                            onQuote = { apply(MarkdownEdit.quote(text, s, e)) },
                            onUse = ::hideKeyboard,
                        )
                        EditorPanel.MEDIA -> MediaPanel(
                            onGallery = { panel = EditorPanel.NONE; photoPicker.launch("image/*") },
                            onCamera = { panel = EditorPanel.NONE; scanning = false; camera.launch(viewModel.prepareCameraFile()) },
                            onScan = { panel = EditorPanel.NONE; scanning = true; camera.launch(viewModel.prepareCameraFile()) },
                            onUse = ::hideKeyboard,
                        )
                        EditorPanel.RECORD -> RecordPanel(
                            recording = recorderState.recording,
                            takeReady = recorderState.takePath != null,
                            clockText = run {
                                val ms = if (recorderState.recording) recordClock else recorderState.takeMs
                                val sec = ms / 1000
                                "%d:%02d".format(sec / 60, sec % 60)
                            },
                            onRecord = { startRecord() },
                            onStop = { viewModel.stopRecording() },
                            onAdd = {
                                panel = EditorPanel.NONE
                                viewModel.addRecording(note.id) { id -> insertBlock(NoteBlocks.token(EmbedKind.AUDIO, id, 100)) }
                            },
                            onDiscard = { viewModel.discardRecording() },
                            onUse = ::hideKeyboard,
                        )
                        EditorPanel.ADD -> AddPanel(
                            onTable = { panel = EditorPanel.NONE; insertBlock(NoteBlocks.newTable(3, 3)) },
                            onDoodle = { panel = EditorPanel.NONE; doodleTarget = null; doodling = true },
                            onRecord = { panel = EditorPanel.RECORD },
                            onAudioFiles = { panel = EditorPanel.NONE; audioPicker.launch("audio/*") },
                            onLinkNote = { panel = EditorPanel.NONE; showLinkPicker = true },
                            onCodeBlock = { panel = EditorPanel.NONE; apply(MarkdownEdit.codeBlock(text, s, e)) },
                            onUse = ::hideKeyboard,
                        )
                    }
                    }
                }
                EditorToolbar(
                    panel = panel,
                    onPanel = { panel = it },
                    onCheck = { apply(MarkdownEdit.checklist(text, selStart, selEnd)) },
                    onUse = ::hideKeyboard,
                )
            }
        },
    ) {
        if (message != null) {
            AppCard(modifier = Modifier.fillMaxWidth(), highlighted = true, onClick = { viewModel.clearMessage() }) {
                Text(message, style = MaterialTheme.typography.bodyMedium)
            }
        }

        BasicTextField(
            value = title,
            onValueChange = { title = it.replace("\n", " ") },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleLarge.copy(color = colors.onSurface, fontWeight = FontWeight.Bold),
            cursorBrush = SolidColor(colors.primary),
            modifier = Modifier.fillMaxWidth().focusRequester(titleFocus),
            decorationBox = { inner ->
                Box {
                    if (title.isEmpty()) Text("Title", style = MaterialTheme.typography.titleLarge, color = colors.onSurfaceVariant.copy(alpha = 0.6f))
                    inner()
                }
            },
        )

        // Date, size, where the note belongs, and the save state, in one small line.
        Text(
            text = "$dateText  |  ${"%,d".format(text.length)} characters  |  Uncategorized  |  $saveText",
            style = MaterialTheme.typography.labelSmall,
            color = if (saveState == SaveState.FAILED) colors.error else colors.onSurfaceVariant,
            modifier = if (saveState == SaveState.FAILED) Modifier.clickable { scope.launch { saveNow() } } else Modifier,
        )

        // The page: text parts are live text fields, pictures, doodles and tables are blocks between them.
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            parts.forEachIndexed { i, part ->
                when (part.kind) {
                    PartKind.TEXT -> {
                        val plan = remember(part.text) { LiveFormat.plan(part.text) }
                        val styled = remember(plan, palette.accent, palette.muted, palette.codeBackground) { buildLiveText(plan, palette) }
                        val transformation = remember(plan, styled) { liveTransformation(plan, styled) }
                        var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
                        val value = when {
                            i != focusPart -> TextFieldValue(part.text)
                            focusValue.text == part.text -> focusValue
                            // Keep the cursor where it was instead of sending it to the start when the two texts briefly differ.
                            else -> TextFieldValue(part.text, TextRange(focusValue.selection.start.coerceIn(0, part.text.length), focusValue.selection.end.coerceIn(0, part.text.length)))
                        }
                        BasicTextField(
                            value = value,
                            onValueChange = { incoming ->
                                // A keyboard can send two events for one key (the text, then the cursor). Always compare with the
                                // note as it is now, never with what this field was drawn from, or the second event would be applied twice.
                                val nowParts = NoteDoc.split(text)
                                val oldPart = nowParts.getOrNull(i)?.text ?: part.text
                                var nv = incoming
                                if (nv.text != oldPart) {
                                    // A backspace that cut into a hidden marker removes the whole marker, never half of it.
                                    MarkerHeal.heal(oldPart, nv.text)?.let { nv = TextFieldValue(it.text, TextRange(it.cursor)) }
                                    // A line break inside styled text must not split the style's markers across two lines.
                                    MarkerHeal.keepStylesWhole(oldPart, incoming.text)?.let { nv = TextFieldValue(it.text, TextRange(it.cursor)) }
                                    val before = Edited(text, selStart, selEnd)
                                    text = NoteDoc.replacePart(text, nowParts, i, nv.text)
                                    history.record(before, System.currentTimeMillis(), false)
                                    historyVersion++
                                }
                                focusPart = i
                                focusValue = nv
                                val afterParts = NoteDoc.split(text)
                                selStart = NoteDoc.globalOffset(afterParts, i, nv.selection.min.coerceAtMost(nv.text.length))
                                selEnd = NoteDoc.globalOffset(afterParts, i, nv.selection.max.coerceAtMost(nv.text.length))
                            },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface, lineHeight = TextUnit.Unspecified),
                            cursorBrush = SolidColor(colors.primary),
                            visualTransformation = transformation,
                            onTextLayout = { layout = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = if (parts.size == 1) 240.dp else 24.dp)
                                .focusRequester(focuserFor(i))
                                .onGloballyPositioned { c ->
                                    val y = c.positionInRoot().y
                                    partBounds[i] = y to y + c.size.height
                                }
                                .onPreviewKeyEvent { ev ->
                                    // Backspace at the very start of a line that follows a picture, doodle, recording or table removes that block.
                                    if (ev.type == KeyEventType.KeyDown && ev.key == Key.Backspace && value.selection.collapsed && value.selection.start == 0 && i > 0) {
                                        val nowParts = NoteDoc.split(text)
                                        val prev = nowParts.getOrNull(i - 1)
                                        if (prev != null && prev.kind != PartKind.TEXT) {
                                            val next = if (prev.kind == PartKind.EMBED) NoteBlocks.remove(text, prev.line)
                                            else text.substring(0, prev.start) + text.substring((prev.end + 1).coerceAtMost(text.length))
                                            commit(next, prev.start, prev.start)
                                            true
                                        } else false
                                    } else false
                                }
                                .onFocusChanged {
                                    if (it.isFocused && focusPart != i) {
                                        focusPart = i
                                        focusValue = TextFieldValue(part.text, TextRange(0))
                                        selStart = NoteDoc.globalOffset(parts, i, 0)
                                        selEnd = selStart
                                    }
                                }
                                .pointerInput(plan) {
                                    // A tap on a check box flips it; a tap on a [[link]] opens that note. Everything else is left to the field.
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                        val up = waitForUpOrCancellation(PointerEventPass.Initial) ?: return@awaitEachGesture
                                        if (up.uptimeMillis - down.uptimeMillis > 350) return@awaitEachGesture
                                        val result = layout ?: return@awaitEachGesture
                                        val at = result.getOffsetForPosition(up.position).coerceIn(0, plan.visible.length)
                                        val check = plan.styles.firstOrNull { it.kind == EditKind.MARKER && (it.arg == 2 || it.arg == 3) && at >= it.start && at < it.end }
                                        if (check != null) {
                                            val line = plan.visible.substring(0, at).count { it == '\n' }
                                            commit(NoteDoc.replacePart(text, parts, i, MarkdownEdit.toggleCheckboxLine(part.text, line)), selStart, selEnd, force = true)
                                            return@awaitEachGesture
                                        }
                                        val link = plan.styles.firstOrNull { it.kind == EditKind.LINK && at > it.start && at < it.end }
                                        if (link != null) openLink(plan.visible.substring(link.start, link.end))
                                    }
                                },
                            decorationBox = { inner ->
                                Box {
                                    if (text.isEmpty() && i == 0) Text("Start writing…", style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant.copy(alpha = 0.6f))
                                    inner()
                                }
                            },
                        )
                    }

                    PartKind.EMBED -> {
                        val ref = EmbedRef.parse(part.text)
                        if (ref != null) {
                            val attachment = attachments.firstOrNull { it.id == ref.id }
                            EmbedBlock(
                                ref = ref,
                                attachment = attachment,
                                version = 0,
                                actions = EmbedActions(
                                    onResize = { w, a -> commit(NoteBlocks.resize(text, part.line, w, a)) },
                                    onMove = { d -> commit(NoteBlocks.move(text, part.line, d).first) },
                                    onDrop = { y, xf -> dropBlock(part, 1, y, xf, ref) },
                                    onRemove = { commit(NoteBlocks.remove(text, part.line)) },
                                    onView = { viewing = attachment },
                                    onCrop = { cropping = attachment },
                                    onDraw = { doodleTarget = attachment; doodling = true },
                                ),
                            )
                        }
                    }

                    PartKind.TABLE -> {
                        val tableLines = part.text.split("\n").size
                        TableBlock(
                            text = part.text,
                            modifier = Modifier.onGloballyPositioned { c ->
                                val y = c.positionInRoot().y
                                partBounds[i] = y to y + c.size.height
                            },
                            actions = TableActions(
                                onEdit = { tableStart = part.start },
                                onSetRows = { rows -> commit(NoteBlocks.setTableRows(text, part.line, rows)) },
                                onAddColumn = { commit(NoteBlocks.addColumn(text, part.line, 99)) },
                                onRemoveColumn = {
                                    val width = skywa1ker404.absencium.ui.vault.markdown.TableGrid.parse(part.text).maxOf { it.size }
                                    commit(NoteBlocks.deleteColumn(text, part.line, width - 1))
                                },
                                onMove = { d ->
                                    val before = if (d < 0) (part.line - 1).coerceAtLeast(0) else part.line + tableLines + 1
                                    commit(NoteBlocks.moveToLine(text, part.line, tableLines, before).first)
                                },
                                onDrop = { y -> dropBlock(part, tableLines, y, null, null) },
                                onRemove = {
                                    val cut = (part.end + 1).coerceAtMost(text.length)
                                    commit(text.substring(0, part.start) + text.substring(cut))
                                },
                            ),
                        )
                    }
                }
                // Between two blocks there is no line to type on; a thin strip adds one.
                val next = parts.getOrNull(i + 1)
                if (part.kind != PartKind.TEXT && next != null && next.kind != PartKind.TEXT) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                commit(text.substring(0, part.end) + "\n" + text.substring(part.end), part.end + 1, part.end + 1)
                            },
                    )
                }
            }
        }
        linkProblem?.let { HintText(it) }

        // Tapping the empty space under the page puts the cursor at the end.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    val ps = NoteDoc.split(text)
                    val i = NoteDoc.nearestText(ps, ps.lastIndex)
                    refocus(text, ps[i].end + if (ps[i].synthetic) 1 else 0, ps[i].end + if (ps[i].synthetic) 1 else 0)
                    runCatching { focuserFor(i).requestFocus() }
                },
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

        // Photos that belong to the note but are not on the page (for example those added when the note was made).
        val onPage = remember(text) { usedEmbeds.findAll(text).mapNotNull { it.groupValues[1].toLongOrNull() }.toSet() }
        val onPageAudio = remember(text) { usedAudio.findAll(text).mapNotNull { it.groupValues[1].toLongOrNull() }.toSet() }
        val loose = attachments.filter { (it.kind == AttachmentKind.IMAGE || it.kind == AttachmentKind.SKETCH) && it.id !in onPage }
        if (loose.isNotEmpty()) {
            Text("Not on the page yet. Tap a picture to put it at the cursor.", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(loose, key = { it.id }) { photo ->
                    PhotoThumb(
                        path = photo.filePath,
                        onClick = {
                            val kind = if (photo.kind == AttachmentKind.SKETCH) EmbedKind.DOODLE else EmbedKind.IMAGE
                            insertBlock(NoteBlocks.token(kind, photo.id, 100))
                        },
                        onRemove = { viewModel.deleteAttachment(photo) },
                    )
                }
            }
        }

        // Recordings that are not on the page yet, and files attached to the note.
        val looseAudio = attachments.filter { it.kind == AttachmentKind.AUDIO && it.id !in onPageAudio }
        if (looseAudio.isNotEmpty()) {
            Text("Recordings. Tap to put one on the page.", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                looseAudio.forEach { a ->
                    ExpressiveChip(selected = false, onClick = { insertBlock(NoteBlocks.token(EmbedKind.AUDIO, a.id, 100)) }, label = { Text(a.displayName) })
                }
            }
        }
        val files = attachments.filter { it.kind == AttachmentKind.DOCUMENT }
        if (files.isNotEmpty()) {
            Text("Files", style = MaterialTheme.typography.titleMedium)
            val context = androidx.compose.ui.platform.LocalContext.current
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                files.forEach { f ->
                    ExpressiveChip(selected = false, onClick = { linkProblem = openAttachment(context, f) }, label = { Text(f.displayName) })
                }
            }
        }

        viewing?.let { a ->
            PhotoPreviewDialog(paths = listOf(a.filePath), startIndex = 0, onBack = { viewing = null })
        }

        cropping?.let { a ->
            PhotoCropDialog(
                path = a.filePath,
                newFile = viewModel::newPictureFile,
                onCancel = { cropping = null },
                onDone = { file ->
                    viewModel.replaceAttachmentFile(a, file)
                    cropping = null
                },
            )
        }

        if (doodling) {
            val target = doodleTarget
            DoodleDialog(
                existingPath = target?.filePath,
                onCancel = { doodling = false; doodleTarget = null },
                onDone = { bitmap ->
                    doodling = false
                    doodleTarget = null
                    viewModel.saveDoodle(note.id, target, bitmap) { id ->
                        if (target == null) insertBlock(NoteBlocks.token(EmbedKind.DOODLE, id, 100))
                    }
                },
            )
        }

        tableStart?.let { start ->
            val part = parts.firstOrNull { it.kind == PartKind.TABLE && it.start == start }
            if (part == null) {
                tableStart = null
            } else {
                TableEditorDialog(
                    text = part.text,
                    onCancel = { tableStart = null },
                    onDone = { built ->
                        tableStart = null
                        commit(text.substring(0, part.start) + built + text.substring(part.end))
                    },
                    onDeleteTable = {
                        tableStart = null
                        val cut = (part.end + 1).coerceAtMost(text.length)
                        commit(text.substring(0, part.start) + text.substring(cut))
                    },
                )
            }
        }

        ShareHost(viewModel)
        if (showShare) {
            ShareFormatDialog(
                formats = listOf(ShareFormat.PDF, ShareFormat.MARKDOWN, ShareFormat.TEXT, ShareFormat.ZIP),
                onPick = { format ->
                    showShare = false
                    // What is on screen, even if the automatic save has not run yet.
                    val current = note.copy(title = cleanTitle(title), bodyText = text)
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
                    apply(MarkdownEdit.insert(text, selStart, selEnd, "[[$picked]]"))
                },
                onDismiss = { showLinkPicker = false },
            )
        }

        if (showInfo) {
            val subjectName = vault?.subjects.orEmpty().firstOrNull { it.id == note.subjectId }?.name ?: "Unknown"
            val words = text.split(Regex("\\s+")).count { it.isNotEmpty() }
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
                        InfoLine("Characters", "%,d".format(text.length))
                        InfoLine("Words", "%,d".format(words))
                        InfoLine("Status", status.name.lowercase().replaceFirstChar { it.uppercase() })
                        InfoLine("Pictures", attachments.count { it.kind == AttachmentKind.IMAGE || it.kind == AttachmentKind.SKETCH }.toString())
                    }
                },
                confirmButton = { TextButton(onClick = { showInfo = false }) { Text("Close") } },
            )
        }

        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text("Delete this note?") },
                text = { Text("${cleanTitle(title)} moves to the Trash in the Vault. You can restore it from there for 30 days.") },
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
