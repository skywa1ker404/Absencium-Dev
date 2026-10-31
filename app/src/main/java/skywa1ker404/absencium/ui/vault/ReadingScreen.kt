package skywa1ker404.absencium.ui.vault

import skywa1ker404.absencium.ui.components.AppMenu
import skywa1ker404.absencium.ui.components.AppMenuDivider
import skywa1ker404.absencium.ui.components.AppMenuItem
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.MediaPlayer
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ReadingScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import skywa1ker404.absencium.MiniReading
import skywa1ker404.absencium.appContainer
import skywa1ker404.absencium.data.db.AttachmentEntity
import skywa1ker404.absencium.data.db.NoteEntity
import skywa1ker404.absencium.data.db.NoteStatus
import skywa1ker404.absencium.data.share.ShareFormat
import skywa1ker404.absencium.ui.components.AppTextField
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.VoiceInputButton
import skywa1ker404.absencium.ui.containerViewModel
import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.vault.entry.rememberPhoto
import skywa1ker404.absencium.ui.vault.markdown.Block
import skywa1ker404.absencium.ui.vault.markdown.EmbedKind

/** What the AI popup at the bottom is showing. */
private class AiAnswer(val title: String, val loading: Boolean, val text: String?, val error: String?)

/** A text toolbar that, instead of the system one, hands the selection to the reading screen's own pill. */
private class PillToolbar(private val onShow: (Rect, (() -> Unit)?) -> Unit, private val onHide: () -> Unit) : TextToolbar {
    override var status: TextToolbarStatus = TextToolbarStatus.Hidden
        private set

    override fun hide() {
        status = TextToolbarStatus.Hidden
        onHide()
    }

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) {
        status = TextToolbarStatus.Shown
        onShow(rect, onCopyRequested)
    }
}

/**
 * Reading mode: the note as plain finished text that cannot be edited by accident. Pinch to zoom, scroll, and select words to
 * explain, translate or look them up with AI. The Edit button goes back to the editor.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Destination<RootGraph>
@Composable
fun ReadingScreen(noteId: Long, navigator: DestinationsNavigator) {
    val viewModel = containerViewModel { VaultViewModel(it) }
    val context = LocalContext.current
    val container = context.appContainer
    var note by remember { mutableStateOf<NoteEntity?>(null) }
    var missing by remember { mutableStateOf(false) }
    LaunchedEffect(noteId) {
        container.readingMini.value = null
        note = viewModel.getNote(noteId)
        if (note == null) missing = true
    }
    val loaded = note
    if (loaded == null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().padding(20.dp)) {
            HintText(if (missing) "This note could not be found. It may have been deleted." else "Loading...")
        }
        return
    }
    ReadingBody(loaded, viewModel, navigator)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ReadingBody(note: NoteEntity, viewModel: VaultViewModel, navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val container = context.appContainer
    val scope = rememberCoroutineScope()
    val vault by viewModel.data.collectAsStateWithLifecycle()
    val attachments = vault?.attachmentsOf(note.id).orEmpty()
    val subjectName = vault?.subjects?.firstOrNull { it.id == note.subjectId }?.name.orEmpty()

    var zoom by remember { mutableFloatStateOf(1f) }
    var focus by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var showShare by remember { mutableStateOf(false) }
    var showLanguages by remember { mutableStateOf(false) }
    var askOpen by remember { mutableStateOf(false) }
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf<AiAnswer?>(null) }
    var answerMinimised by remember { mutableStateOf(false) }
    var aiJob by remember { mutableStateOf<Job?>(null) }
    var language by remember { mutableStateOf("English") }
    var pillRect by remember { mutableStateOf<Rect?>(null) }
    var pillCopy by remember { mutableStateOf<(() -> Unit)?>(null) }
    var lastSelection by remember { mutableStateOf("") }
    var rootTop by remember { mutableFloatStateOf(0f) }
    var viewportHeight by remember { mutableIntStateOf(1) }
    val scroll = rememberScrollState()

    // Continue where reading stopped, once the page is tall enough.
    var restored by remember(note.id) { mutableStateOf(false) }
    LaunchedEffect(scroll.maxValue) {
        if (!restored && scroll.maxValue > 0) {
            restored = true
            scroll.scrollTo(container.readingPosition(note.id).coerceIn(0, scroll.maxValue))
        }
    }
    val pages = ((scroll.maxValue + viewportHeight).toFloat() / viewportHeight).let { kotlin.math.ceil(it.toDouble()).toInt() }.coerceAtLeast(1)
    val page = (scroll.value / viewportHeight + 1).coerceIn(1, pages)
    DisposableEffect(note.id) {
        onDispose { container.saveReadingPosition(note.id, scroll.value) }
    }
    BackHandler(enabled = focus) { focus = false }

    val toolbar = remember {
        PillToolbar(
            onShow = { rect, copy -> pillRect = rect; pillCopy = copy },
            onHide = { pillRect = null; pillCopy = null },
        )
    }

    /** Reads the selected words by asking the system to copy them, then puts the old clipboard back. */
    fun selectedText(): String {
        val copy = pillCopy ?: return lastSelection
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val before = clipboard.primaryClip
        copy()
        val text = clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
        if (before != null) clipboard.setPrimaryClip(before) else clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
        if (text.isNotBlank()) lastSelection = text.trim()
        return lastSelection
    }

    fun ask(title: String, request: String) {
        aiJob?.cancel()
        answerMinimised = false
        answer = AiAnswer(title, loading = true, text = null, error = null)
        aiJob = viewModel.askAi(request) { outcome ->
            answer = when {
                outcome.setupMissing -> AiAnswer(title, false, null, "AI is disabled or not set up. Enable it in Settings, AI Features, or ignore this.")
                outcome.error != null -> AiAnswer(title, false, null, outcome.error)
                else -> AiAnswer(title, false, outcome.text.orEmpty(), null)
            }
        }
    }

    val context1 = "The passage is from a student's note titled \"${note.title}\"${if (subjectName.isNotBlank()) " in the subject $subjectName" else ""}."
    fun explain() {
        val text = selectedText()
        if (text.isBlank()) return
        pillRect = null
        ask("Explain", "$context1 Explain the selected words below clearly and simply for a student, with one short example if it helps. Answer with the explanation only.\n\nSelected:\n$text")
    }
    fun meaning() {
        val text = selectedText()
        if (text.isBlank()) return
        pillRect = null
        ask("Meaning", "$context1 Give the meaning of the selected word or phrase below as it is used in this context: a short definition, its type of word, and one example sentence. Answer with that only.\n\nSelected:\n$text")
    }
    fun translate(into: String) {
        val text = lastSelection.ifBlank { selectedText() }
        if (text.isBlank()) return
        pillRect = null
        language = into
        ask("Translate to $into", "Translate the text below into $into. Keep its meaning and formatting. Answer with the translation only.\n\n$text")
    }
    fun askQuestion() {
        val q = question.trim()
        if (q.isEmpty()) return
        val selected = lastSelection
        val body = note.bodyText.take(14_000)
        val request = if (selected.isNotBlank()) {
            "$context1 Answer the student's question about the selected words. Be clear and brief.\n\nQuestion: $q\n\nSelected:\n$selected\n\nWhole note, for context:\n$body"
        } else {
            "$context1 Answer the student's question using the note. If the note does not say, say so and answer from general knowledge. Be clear and brief.\n\nQuestion: $q\n\nNote:\n$body"
        }
        ask("Your question", request)
        question = ""
    }

    val base = MaterialTheme.colorScheme
    val scheme = base
    val density = LocalDensity.current

    MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography, shapes = MaterialTheme.shapes) {
        val colors = MaterialTheme.colorScheme
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .onGloballyPositioned { rootTop = it.positionInWindow().y },
        ) {
            CompositionLocalProvider(
                LocalTextToolbar provides toolbar,
                LocalDensity provides Density(density.density, density.fontScale * zoom),
            ) {
                SelectionContainer {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { viewportHeight = it.height.coerceAtLeast(1) }
                            .pointerInput(Unit) {
                                // Two fingers zoom the text; one finger scrolls and selects as usual.
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
                                    do {
                                        val event = awaitPointerEvent(PointerEventPass.Final)
                                        if (event.changes.size >= 2) {
                                            val z = event.calculateZoom()
                                            if (z != 1f) {
                                                zoom = (zoom * z).coerceIn(0.7f, 2.6f)
                                                event.changes.forEach { it.consume() }
                                            }
                                        }
                                    } while (event.changes.any { it.pressed })
                                }
                            }
                            .verticalScroll(scroll)
                            .statusBarsPadding()
                            .padding(start = 20.dp, end = 20.dp, top = if (focus) 12.dp else 64.dp, bottom = 180.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(note.title, style = MaterialTheme.typography.headlineMedium, color = colors.onBackground)
                        MarkdownPreview(
                            text = note.bodyText,
                            onToggleCheck = {},
                            onOpenLink = { title ->
                                vault?.notes?.firstOrNull { it.title.equals(title, ignoreCase = true) }?.let {
                                    navigator.navigate(ReadingScreenDestination(noteId = it.id))
                                }
                            },
                            embed = { block -> ReadOnlyEmbed(block, attachments) },
                        )
                    }
                }
            }

            // Top bar: back, the title, minimise and the three-dot menu.
            AnimatedVisibility(visible = !focus, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.TopCenter)) {
                Surface(color = colors.background.copy(alpha = 0.92f), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { navigator.navigateUp() }) { Icon(AppIcons.ArrowBack, contentDescription = "Back", tint = colors.onBackground) }
                        Text(
                            "Page $page of $pages",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.weight(1f).padding(start = 4.dp),
                        )
                        IconButton(onClick = { askOpen = !askOpen }) { Icon(AppIcons.Search, contentDescription = "Ask about this note", tint = colors.onBackground) }
                        IconButton(onClick = {
                            container.saveReadingPosition(note.id, scroll.value)
                            container.readingMini.value = MiniReading(note.id, note.title, subjectName, page, pages)
                            navigator.navigateUp()
                        }) { Icon(AppIcons.ExpandMore, contentDescription = "Minimise to the reading player", tint = colors.onBackground) }
                        Box {
                            IconButton(onClick = { menuOpen = true }) { Icon(AppIcons.MoreVert, contentDescription = "More", tint = colors.onBackground) }
                            AppMenu(expanded = menuOpen, onDismiss = { menuOpen = false }) {
                                AppMenuItem(AppIcons.Share, "Share or export", { menuOpen = false; showShare = true })
                                AppMenuItem(AppIcons.Fullscreen, "Focus mode", { menuOpen = false; focus = true })
                                AppMenuDivider()
                                AppMenuItem(AppIcons.Bookmark, "Bookmark this page", { menuOpen = false; container.saveBookmark(note.id, scroll.value) })
                                if (container.bookmark(note.id) >= 0) {
                                    AppMenuItem(AppIcons.ArrowDown, "Go to the bookmark", { menuOpen = false; scope.launch { scroll.animateScrollTo(container.bookmark(note.id).coerceIn(0, scroll.maxValue)) } })
                                    AppMenuItem(AppIcons.Remove, "Remove the bookmark", { menuOpen = false; container.saveBookmark(note.id, null) })
                                }
                                AppMenuDivider()
                                AppMenuItem(AppIcons.Check, "Mark as finished", {
                                    menuOpen = false
                                    scope.launch {
                                        viewModel.saveNote(note.id, note.subjectId, note.title, note.bodyText, NoteStatus.FINISHED, note.tags)
                                        navigator.navigateUp()
                                    }
                                })
                            }
                        }
                    }
                }
            }
            if (focus) {
                Text(
                    "Exit focus",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceContainerHigh.copy(alpha = 0.8f))
                        .clickable { focus = false }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }

            // The selection pill: Material 3 Expressive floating toolbar above the selected words.
            val rect = pillRect
            if (rect != null) {
                HorizontalFloatingToolbar(
                    expanded = true,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset { IntOffset(0, (rect.top - rootTop - 72.dp.toPx()).toInt().coerceAtLeast(72.dp.roundToPx())) },
                    content = {
                        TextButton(onClick = { explain() }) { Text("Explain") }
                        TextButton(onClick = { selectedText(); showLanguages = true }) { Text("Translate") }
                        TextButton(onClick = { meaning() }) { Text("Meaning") }
                        TextButton(onClick = { selectedText(); askOpen = true; pillRect = null }) { Text("Ask") }
                    },
                )
            }

            // Bottom: the answer popup, the question bar and the Edit button.
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().imePadding().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val shown = answer
                androidx.compose.animation.AnimatedVisibility(
                    visible = shown != null,
                    enter = androidx.compose.animation.slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
                    ) + fadeIn(),
                    exit = androidx.compose.animation.slideOutVertically { it } + fadeOut(),
                ) {
                  val lastShown = remember { androidx.compose.runtime.mutableStateOf(shown) }
                  if (shown != null) lastShown.value = shown
                  val card = lastShown.value
                  if (card != null) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = colors.surfaceContainerHigh,
                        tonalElevation = 3.dp,
                        modifier = Modifier.fillMaxWidth().animateContentSize(),
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (card.loading) LoadingIndicator(modifier = Modifier.size(32.dp))
                                Text(
                                    card.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.onSurface,
                                    modifier = Modifier.weight(1f).padding(start = if (card.loading) 8.dp else 0.dp).clickable { answerMinimised = !answerMinimised },
                                )
                                IconButton(onClick = { answerMinimised = !answerMinimised }) {
                                    Icon(
                                        if (answerMinimised) AppIcons.ArrowUp else AppIcons.ExpandMore,
                                        contentDescription = if (answerMinimised) "Open the answer" else "Minimise the answer",
                                        tint = colors.onSurface,
                                    )
                                }
                                IconButton(onClick = { aiJob?.cancel(); answer = null }) { Icon(AppIcons.Close, contentDescription = "Close", tint = colors.onSurface) }
                            }
                            if (!answerMinimised) {
                                Column(modifier = Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState()).padding(bottom = 8.dp)) {
                                    when {
                                        card.loading -> Text("Thinking...", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                                        card.error != null -> Text(card.error, style = MaterialTheme.typography.bodyMedium, color = colors.error)
                                        else -> SelectionContainer { AiMarkdown(card.text.orEmpty()) }
                                    }
                                }
                            }
                        }
                    }
                }
                }

                if (askOpen && !focus) {
                    // A solid pill: the note text scrolling underneath must not show through the field.
                    Surface(shape = RoundedCornerShape(28.dp), color = colors.surfaceContainerHigh, tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                    AppTextField(
                        value = question,
                        onValueChange = { question = it },
                        placeholder = { Text(if (lastSelection.isNotBlank()) "Ask about the selected words" else "Ask about this note") },
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                VoiceInputButton(onText = { question = (question + " " + it).trim() })
                                IconButton(onClick = { askQuestion() }, enabled = question.isNotBlank()) {
                                    Icon(AppIcons.SkipNext, contentDescription = "Ask", tint = colors.primary)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    }
                }
                if (!focus) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        FloatingActionButton(onClick = {
                            container.saveReadingPosition(note.id, scroll.value)
                            navigator.navigate(com.ramcosta.composedestinations.generated.destinations.NoteEditorScreenDestination(noteId = note.id))
                        }) { Icon(AppIcons.Edit, contentDescription = "Edit this note") }
                    }
                }
            }
        }
    }

    if (showLanguages) {
        LanguagePickerDialog(current = language, onPick = { showLanguages = false; translate(it) }, onDismiss = { showLanguages = false })
    }
    ShareHost(viewModel)
    if (showShare) {
        ShareFormatDialog(
            onPick = { format, theme -> showShare = false; viewModel.share(listOf(note), format, theme) },
            onDismiss = { showShare = false },
        )
    }
}

/** A picture, doodle or recording on the page, shown without any editing handles. */
@Composable
private fun ReadOnlyEmbed(block: Block.Embed, attachments: List<AttachmentEntity>) {
    val colors = MaterialTheme.colorScheme
    val attachment = attachments.firstOrNull { it.id == block.id }
    if (block.kind == EmbedKind.AUDIO) {
        var player by remember { mutableStateOf<MediaPlayer?>(null) }
        var playing by remember { mutableStateOf(false) }
        DisposableEffect(attachment?.filePath) { onDispose { runCatching { player?.release() }; player = null } }
        val seconds = (attachment?.durationMs ?: 0L) / 1000
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surfaceContainerHighest).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                if (playing) AppIcons.Stop else AppIcons.PlayCircle,
                contentDescription = if (playing) "Stop" else "Play the recording",
                tint = colors.onSurface,
                modifier = Modifier.clip(CircleShape).clickable(enabled = attachment != null) {
                    if (playing) {
                        runCatching { player?.release() }
                        player = null
                        playing = false
                    } else {
                        val file = attachment?.filePath ?: return@clickable
                        player = runCatching {
                            MediaPlayer().apply {
                                setDataSource(file)
                                prepare()
                                setOnCompletionListener { mp -> runCatching { mp.release() }; player = null; playing = false }
                                start()
                            }
                        }.getOrNull()
                        playing = player != null
                    }
                },
            )
            Text(
                if (attachment == null) "Recording not found" else "Recording  %d:%02d".format(seconds / 60, seconds % 60),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
            )
        }
    } else {
        val bitmap by rememberPhoto(attachment?.filePath ?: "", 1600, 0)
        val image = bitmap
        if (image != null) {
            Image(bitmap = image, contentDescription = null, contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)))
        } else {
            Box(Modifier.fillMaxWidth().heightIn(min = 80.dp).clip(RoundedCornerShape(14.dp)).background(colors.surfaceContainerHighest))
        }
    }
}
