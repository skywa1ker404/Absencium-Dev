package skywa1ker404.absencium.ui.aihub

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AiSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ReadingScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SubjectFolderScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import skywa1ker404.absencium.data.ai.AiScope
import skywa1ker404.absencium.data.ai.ChatMessage
import skywa1ker404.absencium.data.ai.ChatRole
import skywa1ker404.absencium.ui.components.AlertDialog
import skywa1ker404.absencium.ui.components.AppMenu
import skywa1ker404.absencium.ui.components.AppMenuDivider
import skywa1ker404.absencium.ui.components.AppMenuItem
import skywa1ker404.absencium.ui.components.AppTextField
import skywa1ker404.absencium.ui.components.ExpressiveChip
import skywa1ker404.absencium.ui.components.ExpressiveEmptyState
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.components.VoiceInputButton
import skywa1ker404.absencium.ui.components.bounceIn
import skywa1ker404.absencium.ui.containerViewModel
import skywa1ker404.absencium.ui.icons.AppIcons

/**
 * Absencium AI: a chat in the style of the reference chat apps, drawn with the app's own theme. The menu button opens the chat list
 * (a full-screen page), the model name picks the provider and model, the pill on the right starts a new chat or opens the chat's menu,
 * and "+" in the box adds files and switches web search, thinking and study mode. Answers are written in the background: leaving the
 * chat or the screen does not stop them. With permission it can read parts of the app and propose changes, which only happen when you
 * tap Apply on a card. Everything stays on this phone.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Destination<RootGraph>
@Composable
fun AiHubScreen(navigator: DestinationsNavigator, chatId: Long? = null) {
    val viewModel = containerViewModel { AbsenciumAiViewModel(it) }
    val chat by viewModel.current.collectAsStateWithLifecycle()
    val chats by viewModel.chats.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val pending by viewModel.pending.collectAsStateWithLifecycle()
    val allowed by viewModel.allowed.collectAsStateWithLifecycle()
    val appPrompts by viewModel.appPrompts.collectAsStateWithLifecycle()
    val busy by viewModel.busyChats.collectAsStateWithLifecycle()
    val unread by viewModel.unread.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val working by viewModel.working.collectAsStateWithLifecycle()
    val files by viewModel.files.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    val assistants by viewModel.assistants.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val noteChoices by viewModel.noteChoices.collectAsStateWithLifecycle()
    val materialChoices by viewModel.materialChoices.collectAsStateWithLifecycle()
    val vault by viewModel.vault.collectAsStateWithLifecycle()
    var viewGuide by remember { mutableStateOf<skywa1ker404.absencium.data.db.GuideEntity?>(null) }
    // A [[name]] in an answer opens that note, study material file or subject.
    val openLink: (String) -> Unit = { name ->
        val key = name.trim()
        val note = vault.notes.firstOrNull { it.deletedAt == null && it.title.trim().equals(key, ignoreCase = true) }
        val guide = vault.guides.firstOrNull { it.fileName.trim().equals(key, ignoreCase = true) || it.fileName.substringBeforeLast('.').trim().equals(key, ignoreCase = true) }
        val subject = vault.subjects.firstOrNull { it.name.trim().equals(key, ignoreCase = true) }
        when {
            note != null -> navigator.navigate(ReadingScreenDestination(noteId = note.id))
            guide != null -> viewGuide = guide
            subject != null -> navigator.navigate(SubjectFolderScreenDestination(subjectId = subject.id))
        }
    }
    viewGuide?.let { skywa1ker404.absencium.ui.vault.GuideViewer(it, onClose = { viewGuide = null }) }
    val connection = remember(settings, chat.provider, chat.model, chats) { viewModel.connection(chat) }
    val ready = connection != null
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val canSearch = remember(connection) { viewModel.canSearch() }

    var input by rememberSaveable { mutableStateOf("") }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var sidebar by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    var plus by remember { mutableStateOf(false) }
    var notePicker by remember { mutableStateOf(false) }
    var memoryDialog by remember { mutableStateOf(false) }
    var assistantDialog by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var otherModelFor by remember { mutableStateOf(false) }
    var savingNote by remember { mutableStateOf<ChatMessage?>(null) }
    val saved = chat.messages.isNotEmpty()

    // The service needs to know when this chat is on screen, so a finished answer elsewhere can be announced.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) viewModel.setVisible(true)
            if (event == Lifecycle.Event.ON_STOP) viewModel.setVisible(false)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) viewModel.setVisible(true)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.setVisible(false)
        }
    }
    LaunchedEffect(chatId) { if (chatId != null) viewModel.open(chatId) }
    LaunchedEffect(notice) {
        if (notice != null) {
            kotlinx.coroutines.delay(3_500)
            viewModel.clearNotice()
        }
    }

    val photos = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris -> if (uris.isNotEmpty()) viewModel.addFiles(uris) }
    val documents = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris -> if (uris.isNotEmpty()) viewModel.addFiles(uris) }

    fun copy(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Absencium AI", text))
    }

    fun send() {
        viewModel.send(input, replaceFrom = editingId)
        input = ""
        editingId = null
    }

    ScreenScaffold(
        title = "Absencium AI",
        onBack = { navigator.navigateUp() },
        scrollable = false,
        contentUnderBars = true,
        overlay = {
            AiSidebar(
                visible = sidebar,
                chats = chats,
                currentId = chat.id,
                busy = busy,
                unread = unread,
                appPrompts = appPrompts,
                studentName = settings?.displayName.orEmpty(),
                onNew = { viewModel.newChat(); input = ""; editingId = null; sidebar = false },
                onOpen = { id -> viewModel.open(id); input = ""; editingId = null; sidebar = false },
                onClose = { sidebar = false },
                onSettings = { navigator.navigate(AiSettingsScreenDestination) },
                onClearPrompts = viewModel::clearAppPrompts,
                onPin = { viewModel.togglePin(it) },
                onArchive = { viewModel.toggleArchive(it) },
                onRename = { id, name -> viewModel.rename(name, id) },
                onDelete = { viewModel.delete(it) },
            )
        },
        customTopBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // A soft fade of the page colour: the chat dissolves under the buttons instead of meeting a hard block.
                    .background(Brush.verticalGradient(0f to colors.background.copy(alpha = 0.92f), 0.75f to colors.background.copy(alpha = 0.6f), 1f to Color.Transparent))
                    .statusBarsPadding()
                    .height(64.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(colors.surfaceContainerHigh).clickable { sidebar = true }, contentAlignment = Alignment.Center) {
                    Icon(AppIcons.Menu, contentDescription = "Chats")
                    if (unread.isNotEmpty()) Box(Modifier.align(Alignment.TopEnd).padding(10.dp).size(9.dp).clip(CircleShape).background(colors.primary))
                }
                Row(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).clickable { picking = true }.padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(connection?.model ?: "Choose a model", style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Icon(AppIcons.ExpandMore, contentDescription = null, tint = colors.onSurfaceVariant)
                }
                Row(
                    modifier = Modifier.height(48.dp).clip(CircleShape).background(colors.surfaceContainerHigh).padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // An empty page offers an incognito chat (not saved, no memory); once there is a conversation the button starts a new chat.
                    IconButton(onClick = {
                        when {
                            !saved && !chat.incognito -> viewModel.setIncognito(true)
                            !saved -> viewModel.setIncognito(false)
                            else -> { viewModel.newChat(); input = ""; editingId = null }
                        }
                    }) {
                        when {
                            chat.incognito -> Icon(AppIcons.Incognito, contentDescription = "Leave the incognito chat", tint = colors.primary)
                            !saved -> Icon(AppIcons.Incognito, contentDescription = "Start an incognito chat")
                            else -> Icon(AppIcons.Edit, contentDescription = "New chat")
                        }
                    }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(AppIcons.MoreVert, contentDescription = "More") }
                        AppMenu(expanded = menu, onDismiss = { menu = false }) {
                            AppMenuItem(AppIcons.Share, "Share conversation", {
                                menu = false
                                val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, viewModel.transcript()) }
                                context.startActivity(Intent.createChooser(send, "Share conversation").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }, enabled = saved)
                            AppMenuItem(AppIcons.PushPin, if (chat.pinned) "Unpin" else "Pin", { menu = false; viewModel.togglePin() }, enabled = saved)
                            AppMenuItem(AppIcons.Edit, "Rename", { menu = false; renaming = true }, enabled = saved)
                            AppMenuItem(AppIcons.Folder, if (chat.archived) "Unarchive" else "Archive", { menu = false; viewModel.toggleArchive() }, enabled = saved)
                            AppMenuDivider()
                            AppMenuItem(AppIcons.Person, "Assistants", { menu = false; assistantDialog = true })
                            AppMenuItem(AppIcons.Bookmark, "Memory", { menu = false; memoryDialog = true })
                            AppMenuDivider()
                            AppMenuItem(AppIcons.Delete, "Delete", { menu = false; confirmDelete = true }, destructive = true, enabled = saved)
                        }
                    }
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(0f to Color.Transparent, 0.4f to colors.background.copy(alpha = 0.7f), 1f to colors.background.copy(alpha = 0.95f)))
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!ready) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.secondaryContainer).padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (settings?.askAiEnabled == true) "Pick a provider and model to start" else "AI Features are off",
                            style = MaterialTheme.typography.bodyMedium, color = colors.onSecondaryContainer, modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { navigator.navigate(AiSettingsScreenDestination) }) { Text("Open settings") }
                    }
                }
                notice?.let {
                    Text(it, style = MaterialTheme.typography.labelLarge, color = colors.primary, modifier = Modifier.padding(horizontal = 16.dp))
                }
                editingId?.let {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.tertiaryContainer).padding(start = 16.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Editing your message. Sending replaces it and what followed.", style = MaterialTheme.typography.labelMedium, color = colors.onTertiaryContainer, modifier = Modifier.weight(1f))
                        IconButton(onClick = { editingId = null; input = "" }) { Icon(AppIcons.Close, contentDescription = "Cancel editing", tint = colors.onTertiaryContainer) }
                    }
                }
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    files.forEach { file ->
                        ExpressiveChip(selected = true, onClick = { viewModel.removeFile(file) }, label = { Text(file.name.take(24) + "  ✕", maxLines = 1) })
                    }
                    if (chat.webSearch && canSearch) ExpressiveChip(selected = true, onClick = { viewModel.setWebSearch(false) }, label = { Text("Web  ✕") })
                    if (chat.thinking) ExpressiveChip(selected = true, onClick = { viewModel.setThinking(false) }, label = { Text("Thinking  ✕") })
                    if (chat.study) ExpressiveChip(selected = true, onClick = { viewModel.setStudy(false) }, label = { Text("Study  ✕") })
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(colors.surfaceContainerHigh).padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    IconButton(onClick = { plus = true }, modifier = Modifier.size(48.dp)) { Icon(AppIcons.Add, contentDescription = "Add files and tools") }
                    Box(Modifier.weight(1f).padding(vertical = 12.dp), contentAlignment = Alignment.CenterStart) {
                        if (input.isEmpty()) Text("Ask Absencium AI", style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, maxLines = 1)
                        BasicTextField(
                            value = input,
                            onValueChange = { input = it },
                            enabled = ready,
                            maxLines = 6,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                            cursorBrush = SolidColor(colors.primary),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (input.isBlank() && files.isEmpty() && ready && !pending) VoiceInputButton(onText = { input = (input + " " + it).trim() })
                    val canSend = ready && !pending && (input.isNotBlank() || files.isNotEmpty())
                    if (pending) {
                        Box(Modifier.size(48.dp).clip(CircleShape).background(colors.primary).clickable(onClick = viewModel::stop), contentAlignment = Alignment.Center) {
                            Icon(AppIcons.Stop, contentDescription = "Stop", tint = colors.onPrimary)
                        }
                    } else if (canSend) {
                        Box(Modifier.size(48.dp).clip(CircleShape).background(colors.primary).clickable(onClick = ::send), contentAlignment = Alignment.Center) {
                            Icon(AppIcons.Send, contentDescription = "Send", tint = colors.onPrimary)
                        }
                    } else {
                        Box(Modifier.size(48.dp))
                    }
                }
            }
        },
    ) {
        val bars = skywa1ker404.absencium.ui.components.LocalScaffoldPadding.current
        Box(Modifier.fillMaxSize()) {
            if (!saved && !pending) {
                Box(Modifier.fillMaxSize().padding(top = bars.calculateTopPadding(), bottom = bars.calculateBottomPadding())) {
                    EmptyChat(name = settings?.displayName.orEmpty(), allowed = allowed, incognito = chat.incognito, onPick = { input = it })
                }
            } else {
                val last = chat.messages.lastOrNull { !it.hidden }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = true,
                    contentPadding = PaddingValues(
                        top = bars.calculateTopPadding() + 8.dp,
                        bottom = bars.calculateBottomPadding() + 8.dp,
                        start = 4.dp,
                        end = 4.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    if (pending) {
                        item(key = "pending") {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                LoadingIndicator(modifier = Modifier.size(40.dp))
                                ShimmerText(if (chat.webSearch) "Searching and thinking" else "Thinking")
                            }
                        }
                    } else if (last != null && last.role == ChatRole.USER) {
                        item(key = "no-answer") { NoAnswerYet(onRetry = viewModel::regenerate) }
                    }
                    items(chat.messages.filter { !it.hidden }.asReversed(), key = { it.id }) { message ->
                        Box(if (message === last) Modifier.bounceIn(0) else Modifier) {
                            if (message.role == ChatRole.USER) {
                                UserMessage(message, onCopy = { copy(message.text) }, onEdit = { input = message.text; editingId = message.id }, enabled = !pending)
                            } else {
                                AssistantMessage(
                                    message = message,
                                    chatId = chat.id,
                                    isLast = message === last && !pending,
                                    allowed = allowed,
                                    results = results,
                                    working = working,
                                    onCopy = { copy(message.text) },
                                    onRegenerate = viewModel::regenerate,
                                    onRetryWithOther = { otherModelFor = true },
                                    onContinue = viewModel::continueAnswer,
                                    onSaveAsNote = { savingNote = message },
                                    onApply = { items -> viewModel.apply(message.id, items) },
                                    onDismiss = { index -> viewModel.dismiss(message.id, index) },
                                    onOpenLink = openLink,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (plus) {
        PlusSheet(
            chat = chat,
            canSearch = canSearch,
            memoryOn = AiScope.MEMORY in allowed,
            memoryCount = memories.size,
            assistantName = assistants.firstOrNull { it.id == chat.assistant }?.name.orEmpty(),
            onPhotos = { photos.launch("image/*") },
            onFiles = { documents.launch(arrayOf("application/pdf", "text/*", "image/*", "audio/*", "video/*", "application/json")) },
            onNotes = { notePicker = true },
            onWeb = viewModel::setWebSearch,
            onThinking = viewModel::setThinking,
            onStudy = viewModel::setStudy,
            onLevel = viewModel::setLevel,
            onPrompt = { input = it },
            onMemory = { memoryDialog = true },
            onAssistants = { assistantDialog = true },
            onDismiss = { plus = false },
        )
    }
    if (notePicker) {
        ContentPickerDialog(
            subjects = subjects.map { it.id to it.name },
            notes = noteChoices,
            materials = materialChoices,
            onAttach = { noteIds, materialIds -> notePicker = false; viewModel.attachNotes(noteIds); viewModel.attachMaterials(materialIds) },
            onDismiss = { notePicker = false },
        )
    }
    if (memoryDialog) {
        MemoryDialog(
            items = memories.filter { it.chatId == null || it.chatId == chat.id },
            enabled = AiScope.MEMORY in allowed,
            chatId = chat.id,
            onEnabled = { viewModel.setScope(AiScope.MEMORY, it) },
            onAdd = viewModel::addMemory,
            onUpdate = viewModel::updateMemory,
            onDelete = viewModel::deleteMemory,
            onClear = viewModel::clearMemory,
            onDismiss = { memoryDialog = false },
        )
    }
    if (assistantDialog) {
        AssistantsDialog(
            profiles = assistants,
            currentId = chat.assistant,
            onPick = { viewModel.setAssistant(it); assistantDialog = false },
            onSave = viewModel::saveAssistant,
            onDelete = viewModel::deleteAssistant,
            onExport = viewModel::exportAssistants,
            onImport = viewModel::importAssistants,
            onDismiss = { assistantDialog = false },
        )
    }
    savingNote?.let { message ->
        SubjectPickerDialog(
            subjects = subjects.map { it.id to it.name },
            onPick = { id -> savingNote = null; viewModel.saveAsNote(message, id) },
            onDismiss = { savingNote = null },
        )
    }
    if (picking || otherModelFor) {
        AiModelDialog(
            providers = remember(settings, picking, otherModelFor) { viewModel.usableProviders() },
            current = connection,
            connectionFor = { provider -> viewModel.connectionForProvider(provider) },
            onPick = { provider, model ->
                if (otherModelFor) viewModel.retryWith(provider, model) else viewModel.setModel(provider, model)
                picking = false
                otherModelFor = false
            },
            onDismiss = { picking = false; otherModelFor = false },
            onOpenSettings = { navigator.navigate(AiSettingsScreenDestination) },
        )
    }
    if (renaming) {
        var name by remember { mutableStateOf(chat.title.ifBlank { chat.label }) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Rename chat") },
            text = { AppTextField(value = name, onValueChange = { name = it }, singleLine = true, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { renaming = false; viewModel.rename(name) }, enabled = name.isNotBlank()) { Text("Save") } },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text("Cancel") } },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Delete this chat?") },
            text = { Text("The conversation is deleted from this phone. Changes you already applied from it stay.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; viewModel.delete() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep") } },
        )
    }
}

/** A line of text with a light sweep passing over it while an answer is being written, in the theme's own colours. */
@Composable
private fun ShimmerText(text: String) {
    val colors = MaterialTheme.colorScheme
    val sweep by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = androidx.compose.animation.core.LinearEasing)),
        label = "shimmerSweep",
    )
    val width = with(androidx.compose.ui.platform.LocalDensity.current) { 220.dp.toPx() }
    val brush = Brush.horizontalGradient(
        colors = listOf(colors.onSurfaceVariant, colors.onSurface, colors.onSurfaceVariant),
        startX = sweep * width * 1.6f - width * 0.6f,
        endX = sweep * width * 1.6f,
    )
    Text("$text...", style = MaterialTheme.typography.titleMedium.copy(brush = brush))
}

/** The empty chat: a morphing badge and a big greeting, with the suggestions as plain rows below when there is room (never over the greeting). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyChat(name: String, allowed: Set<AiScope>, incognito: Boolean, onPick: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val suggestions = buildList<Triple<ImageVector, String, String>> {
        add(Triple(AppIcons.Lightbulb, "Explain a topic simply", "Explain this simply, with one example: "))
        add(Triple(AppIcons.Quiz, "Quiz me on my notes", "Quiz me on what I should revise next. "))
        add(Triple(AppIcons.Draft, "Write notes for a chapter", "Write lengthy, well formatted notes on: "))
        if (AiScope.EXAMS in allowed) add(Triple(AppIcons.Schedule, "Plan my revision", "Make a revision plan from my syllabus and exam dates."))
        else add(Triple(AppIcons.Summarize, "Summarise something", "Summarise the key points of: "))
        if (AiScope.ATTENDANCE in allowed) {
            add(Triple(AppIcons.Tasks, "How is my attendance?", "Show my attendance for each subject: the percentage and whether it is safe or below the required percentage. Only call a subject a concern if it is below the requirement."))
        }
    }
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        val imeOpen = WindowInsets.isImeVisible
        // The suggestions only show when the page is tall enough for them and the keyboard is closed.
        val room = maxHeight > 560.dp && !imeOpen
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                ExpressiveEmptyState(
                    icon = if (incognito) AppIcons.Incognito else AppIcons.AutoAwesome,
                    title = when {
                        incognito -> "Incognito chat"
                        name.isBlank() -> "What's on your mind?"
                        else -> "Hi $name, what's on your mind?"
                    },
                    text = if (incognito) "Not saved to your chats, and nothing is remembered from it." else "",
                    titleStyle = MaterialTheme.typography.headlineMedium,
                )
            }
            if (room) {
                Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    suggestions.forEach { (icon, label, start) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable { onPick(start) }.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            Icon(icon, contentDescription = null, tint = colors.onSurface)
                            Text(label, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}
