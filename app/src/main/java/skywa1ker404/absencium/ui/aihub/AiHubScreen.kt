package skywa1ker404.absencium.ui.aihub

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AiSettingsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import skywa1ker404.absencium.data.ai.AiScope
import skywa1ker404.absencium.data.ai.ChatMessage
import skywa1ker404.absencium.data.ai.ChatRole
import skywa1ker404.absencium.ui.components.AlertDialog
import skywa1ker404.absencium.ui.components.AppMenu
import skywa1ker404.absencium.ui.components.AppMenuDivider
import skywa1ker404.absencium.ui.components.AppMenuItem
import skywa1ker404.absencium.ui.components.AppTextField
import skywa1ker404.absencium.ui.components.ExpandChevron
import skywa1ker404.absencium.ui.components.ExpressiveChip
import skywa1ker404.absencium.ui.components.ExpressiveEmptyState
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.components.VoiceInputButton
import skywa1ker404.absencium.ui.components.bounceIn
import skywa1ker404.absencium.ui.containerViewModel
import skywa1ker404.absencium.ui.icons.AppIcons

/**
 * Absencium AI: a chat in the style of the Gemini app, drawn with the app's own theme. A sidebar holds the chats (and, closed, the
 * read-only prompts the app made); the model name at the top picks the provider and model; the menu shares, pins, renames or deletes
 * the chat; a long press on your message copies or edits it. With permission it can also read parts of the app and propose changes,
 * which only happen when you tap Apply on a card. Everything stays on this phone.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Destination<RootGraph>
@Composable
fun AiHubScreen(navigator: DestinationsNavigator) {
    val viewModel = containerViewModel { AbsenciumAiViewModel(it) }
    val chat by viewModel.current.collectAsStateWithLifecycle()
    val chats by viewModel.chats.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val pending by viewModel.pending.collectAsStateWithLifecycle()
    val allowed by viewModel.allowed.collectAsStateWithLifecycle()
    val appPrompts by viewModel.appPrompts.collectAsStateWithLifecycle()
    val connection = remember(settings, chat.provider, chat.model, chats) { viewModel.connection(chat) }
    val ready = connection != null
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var input by rememberSaveable { mutableStateOf("") }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var menu by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val saved = chat.messages.isNotEmpty()

    fun copy(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Absencium AI", text))
    }

    fun send() {
        viewModel.send(input, replaceFrom = editingId)
        input = ""
        editingId = null
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AiSidebar(
                chats = chats,
                currentId = chat.id,
                appPrompts = appPrompts,
                onNew = { viewModel.newChat(); input = ""; editingId = null; scope.launch { drawerState.close() } },
                onOpen = { id -> viewModel.open(id); input = ""; editingId = null; scope.launch { drawerState.close() } },
                onSettings = { navigator.navigate(AiSettingsScreenDestination) },
                onClearPrompts = viewModel::clearAppPrompts,
            )
        },
    ) {
        ScreenScaffold(
            title = "Absencium AI",
            onBack = { navigator.navigateUp() },
            scrollable = false,
            customTopBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(0f to colors.background.copy(alpha = 0.9f), 0.7f to colors.background.copy(alpha = 0.55f), 1f to Color.Transparent))
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(AppIcons.Menu, contentDescription = "Chats") }
                    Row(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).clickable { picking = true }.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Column(Modifier.weight(1f, fill = false)) {
                            Text("Absencium AI", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                            Text(connection?.model ?: "Choose a model", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        ExpandChevron(expanded = false)
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
                            AppMenuDivider()
                            AppMenuItem(AppIcons.Delete, "Delete", { menu = false; confirmDelete = true }, destructive = true, enabled = saved)
                        }
                    }
                }
            },
            bottomBar = {
                Column(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 12.dp, vertical = 8.dp),
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
                    editingId?.let {
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.tertiaryContainer).padding(start = 16.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Editing your message. Sending replaces it and what followed.", style = MaterialTheme.typography.labelMedium, color = colors.onTertiaryContainer, modifier = Modifier.weight(1f))
                            IconButton(onClick = { editingId = null; input = "" }) { Icon(AppIcons.Close, contentDescription = "Cancel editing", tint = colors.onTertiaryContainer) }
                        }
                    }
                    AppTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text("Ask Absencium AI", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        enabled = ready && !pending,
                        maxLines = 5,
                        shape = RoundedCornerShape(28.dp),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (input.isBlank() && ready && !pending) VoiceInputButton(onText = { input = (input + " " + it).trim() })
                                if (pending) {
                                    IconButton(onClick = viewModel::cancel) { Icon(AppIcons.Stop, contentDescription = "Stop", tint = colors.primary) }
                                } else {
                                    IconButton(onClick = ::send, enabled = ready && input.isNotBlank()) {
                                        Icon(AppIcons.Send, contentDescription = "Send", tint = if (ready && input.isNotBlank()) colors.primary else colors.onSurfaceVariant)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
        ) {
            Box(Modifier.fillMaxSize()) {
                if (!saved && !pending) {
                    EmptyChat(
                        name = settings?.displayName.orEmpty(),
                        allowed = allowed,
                        onPick = { input = it },
                    )
                } else {
                    val last = chat.messages.lastOrNull()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        reverseLayout = true,
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        if (pending) {
                            item(key = "pending") {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    LoadingIndicator(modifier = Modifier.size(40.dp))
                                    Text("Thinking", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                                }
                            }
                        }
                        items(chat.messages.asReversed(), key = { it.id }) { message ->
                            Box(if (message === last) Modifier.bounceIn(0) else Modifier) {
                                MessageItem(
                                    message = message,
                                    isLast = message === last && !pending,
                                    allowed = allowed,
                                    viewModel = viewModel,
                                    onCopy = ::copy,
                                    onEdit = { input = message.text; editingId = message.id },
                                    canEdit = !pending,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (picking) {
        AiModelDialog(
            providers = remember(settings, picking) { viewModel.usableProviders() },
            current = connection,
            connectionFor = { provider -> viewModel.connectionForProvider(provider) },
            onPick = { provider, model -> viewModel.setModel(provider, model); picking = false },
            onDismiss = { picking = false },
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

@Composable
private fun MessageItem(
    message: ChatMessage,
    isLast: Boolean,
    allowed: Set<AiScope>,
    viewModel: AbsenciumAiViewModel,
    onCopy: (String) -> Unit,
    onEdit: () -> Unit,
    canEdit: Boolean,
) {
    if (message.role == ChatRole.USER) {
        UserMessage(message, onCopy = { onCopy(message.text) }, onEdit = onEdit, enabled = canEdit)
    } else {
        AssistantMessage(
            message = message,
            isLast = isLast,
            allowed = allowed,
            results = viewModel.results,
            onCopy = { onCopy(message.text) },
            onRegenerate = viewModel::regenerate,
            onApply = { index, action -> viewModel.apply(message.id, index, action) },
            onApplyAll = { items -> viewModel.applyAll(message.id, items) },
            onDismiss = { index -> viewModel.dismiss(message.id, index) },
        )
    }
}

/** The empty chat: a greeting with a random morphing shape, and suggestions that fill the box (only ones that fit what is allowed). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyChat(name: String, allowed: Set<AiScope>, onPick: (String) -> Unit) {
    val suggestions = buildList {
        add("Explain simply" to "Explain this simply, with one example: ")
        add("Quiz me" to "Ask me 5 short quiz questions about: ")
        add("Plan revision" to "Help me plan my revision for: ")
        add("Summarise" to "Summarise the key points of: ")
        if (AiScope.TIMETABLE in allowed) add("My timetable" to "What lectures do I have tomorrow?")
        if (AiScope.EXAMS in allowed) add("Add an exam" to "Add an exam: ")
        if (AiScope.ATTENDANCE in allowed) add("My attendance" to "How is my attendance, and which subject needs care?")
        if (AiScope.NOTES in allowed) add("Add a note" to "Add a note about: ")
    }
    ExpressiveEmptyState(
        icon = AppIcons.AutoAwesome,
        title = if (name.isBlank()) "Hello" else "Hi $name",
        text = "How can I help you today?",
        action = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                suggestions.forEach { (label, start) -> ExpressiveChip(selected = false, onClick = { onPick(start) }, label = { Text(label) }) }
            }
        },
    )
}
