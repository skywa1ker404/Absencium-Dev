package skywa1ker404.absencium.ui.aihub

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.activity.compose.BackHandler
import skywa1ker404.absencium.ui.components.AppMenu
import skywa1ker404.absencium.ui.components.AppMenuItem
import skywa1ker404.absencium.ui.components.sheetContainerColor
import skywa1ker404.absencium.ui.components.sheetScrimColor
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import skywa1ker404.absencium.data.ai.AiChat
import skywa1ker404.absencium.data.ai.AiHistoryEntry
import skywa1ker404.absencium.ui.components.AlertDialog
import skywa1ker404.absencium.ui.components.AppTextField
import skywa1ker404.absencium.ui.components.ExpandChevron
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.components.Reveal
import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.vault.AiMarkdown
import java.text.DateFormat
import java.util.Date

private const val CONTEXT_MARK = "Now the student writes:\n"

/** What the chat list shows. */
enum class ChatFilter(val label: String) { ALL("All"), PINNED("Pinned"), UNREAD("Unread"), ARCHIVED("Archived") }

/**
 * The chat list of Absencium AI, as a full-screen page that opens with a spring (like the reference chat apps): a title with search and
 * close, New chat, a filtered plain list of chats (unread ones have a dot, a pressed-and-held one opens its actions), a closed folder
 * with the prompts the app made itself (read only), and the student at the bottom with the AI settings.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AiSidebar(
    visible: Boolean,
    chats: List<AiChat>,
    currentId: Long,
    busy: Set<Long>,
    unread: Set<Long>,
    appPrompts: List<AiHistoryEntry>,
    studentName: String,
    onNew: () -> Unit,
    onOpen: (Long) -> Unit,
    onClose: () -> Unit,
    onSettings: () -> Unit,
    onClearPrompts: () -> Unit,
    onPin: (Long) -> Unit,
    onArchive: (Long) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf(ChatFilter.ALL) }
    var filterMenu by remember { mutableStateOf(false) }
    var folderOpen by rememberSaveable { mutableStateOf(false) }
    var viewing by remember { mutableStateOf<AiHistoryEntry?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var actionsFor by remember { mutableStateOf<AiChat?>(null) }
    var renaming by remember { mutableStateOf<AiChat?>(null) }
    var deleting by remember { mutableStateOf<AiChat?>(null) }
    val shown = remember(chats, query, filter, unread) {
        chats.filter { chat ->
            val inFilter = when (filter) {
                ChatFilter.ALL -> !chat.archived
                ChatFilter.PINNED -> chat.pinned && !chat.archived
                ChatFilter.UNREAD -> chat.id in unread
                ChatFilter.ARCHIVED -> chat.archived
            }
            inFilter && (query.isBlank() || chat.label.contains(query, true) || chat.messages.any { m -> m.text.contains(query, true) })
        }
    }
    val format = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }

    BackHandler(enabled = visible) { if (searching) { searching = false; query = "" } else onClose() }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(spring(stiffness = 600f)) + scaleIn(spring(dampingRatio = 0.75f, stiffness = 380f), initialScale = 0.9f),
        exit = fadeOut(spring(stiffness = 700f)) + scaleOut(spring(stiffness = 600f), targetScale = 0.94f),
    ) {
        Column(Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (searching) {
                    AppTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search chats", maxLines = 1) },
                        leadingIcon = { Icon(AppIcons.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { searching = false; query = "" }) { Icon(AppIcons.Close, contentDescription = "Close search") }
                } else {
                    Text("Absencium AI", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f).padding(start = 8.dp))
                    IconButton(onClick = { searching = true }) { Icon(AppIcons.Search, contentDescription = "Search chats") }
                    IconButton(onClick = onClose) { Icon(AppIcons.Close, contentDescription = "Close") }
                }
            }
            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                item(key = "new") {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).clickable(onClick = onNew).padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Icon(AppIcons.Add, contentDescription = null, tint = colors.onSurface)
                        Text("New chat", style = MaterialTheme.typography.titleMedium)
                    }
                }
                item(key = "filter") {
                    Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(filter.label, style = MaterialTheme.typography.titleSmall, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Box {
                            IconButton(onClick = { filterMenu = true }) { Icon(AppIcons.Tune, contentDescription = "Filter chats", tint = colors.onSurfaceVariant) }
                            AppMenu(expanded = filterMenu, onDismiss = { filterMenu = false }) {
                                ChatFilter.entries.forEach { option -> AppMenuItem(AppIcons.Check, option.label, { filterMenu = false; filter = option }) }
                            }
                        }
                    }
                }
                if (shown.isEmpty()) {
                    item(key = "none") {
                        Text(
                            if (query.isBlank()) "Nothing here yet." else "No chat matches.",
                            style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(12.dp),
                        )
                    }
                }
                items(shown, key = { it.id }) { chat ->
                    ChatRow(chat, chat.id == currentId, chat.id in busy, chat.id in unread, onOpen, onHold = { actionsFor = chat })
                }
                item(key = "prompts") {
                    // Closed by default. Prompts the app made itself, kept apart so they never mix with real chats.
                    Column(Modifier.padding(top = 10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable { folderOpen = !folderOpen }.padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(AppIcons.Folder, contentDescription = null, tint = colors.onSurfaceVariant)
                            Column(Modifier.weight(1f)) {
                                Text("App prompts", style = MaterialTheme.typography.titleSmall)
                                Text("${appPrompts.size} made by notes, syllabus and homework · read only", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                            }
                            ExpandChevron(expanded = folderOpen)
                        }
                        Reveal(visible = folderOpen) {
                            Column(Modifier.padding(start = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                if (appPrompts.isEmpty()) {
                                    Text("Nothing yet.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(12.dp))
                                }
                                appPrompts.take(60).forEach { entry ->
                                    Column(
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { viewing = entry }.padding(horizontal = 12.dp, vertical = 8.dp),
                                    ) {
                                        Text(promptTitle(entry.prompt), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (entry.error != null) colors.error else colors.onSurface)
                                        Text(format.format(Date(entry.time)), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                                    }
                                }
                                if (appPrompts.isNotEmpty()) TextButton(onClick = { confirmClear = true }) { Text("Clear app prompts") }
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(colors.primaryContainer), contentAlignment = Alignment.Center) {
                    Text(studentName.trim().firstOrNull()?.uppercase() ?: "A", style = MaterialTheme.typography.titleMedium, color = colors.onPrimaryContainer)
                }
                Text(studentName.ifBlank { "Absencium" }, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                IconButton(onClick = onSettings) { Icon(AppIcons.Settings, contentDescription = "AI settings", tint = colors.onSurfaceVariant) }
            }
        }
    }

    actionsFor?.let { chat ->
        ModalBottomSheet(
            onDismissRequest = { actionsFor = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = sheetContainerColor(),
            scrimColor = sheetScrimColor(),
        ) {
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(chat.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
                SheetRow(AppIcons.PushPin, if (chat.pinned) "Unpin" else "Pin") { actionsFor = null; onPin(chat.id) }
                SheetRow(AppIcons.Edit, "Rename") { actionsFor = null; renaming = chat }
                SheetRow(AppIcons.Folder, if (chat.archived) "Move out of Archived" else "Archive") { actionsFor = null; onArchive(chat.id) }
                SheetRow(AppIcons.Delete, "Delete", destructive = true) { actionsFor = null; deleting = chat }
            }
        }
    }
    renaming?.let { chat ->
        var name by remember(chat.id) { mutableStateOf(chat.title.ifBlank { chat.label }) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Rename chat") },
            text = { AppTextField(value = name, onValueChange = { name = it }, singleLine = true, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { renaming = null; onRename(chat.id, name) }, enabled = name.isNotBlank()) { Text("Save") } },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("Cancel") } },
        )
    }
    deleting?.let { chat ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Delete this chat?") },
            text = { Text("The conversation is deleted from this phone. Changes you already applied from it stay.") },
            confirmButton = { TextButton(onClick = { deleting = null; onDelete(chat.id) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Keep") } },
        )
    }
    viewing?.let { entry ->
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = { viewing = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text("App prompt", maxLines = 1) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Made by the app on ${format.format(Date(entry.time))}${if (entry.model.isNotBlank()) " with ${entry.model}" else ""}. It cannot be continued.", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    Text(promptTitle(entry.prompt, 600), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.background(colors.surfaceContainerHigh, RoundedCornerShape(16.dp)).padding(12.dp))
                    when {
                        entry.error != null -> Text(entry.error, style = MaterialTheme.typography.bodyMedium, color = colors.error)
                        !entry.result.isNullOrBlank() -> SelectionContainer { AiMarkdown(entry.result) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { viewing = null }) { Text("Close") } },
            dismissButton = {
                TextButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("AI answer", (entry.result ?: entry.error).orEmpty()))
                }) { Text("Copy answer") }
            },
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Clear app prompts?") },
            text = { Text("The list of prompts the app made is deleted from this phone. Notes already made from them are not touched, and your chats stay.") },
            confirmButton = { TextButton(onClick = { confirmClear = false; onClearPrompts() }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Keep") } },
        )
    }
}

@Composable
private fun SidebarLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp))
}

@Composable
private fun SheetRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, destructive: Boolean = false, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, contentDescription = null, tint = if (destructive) colors.error else colors.onSurface)
        Text(label, style = MaterialTheme.typography.bodyLarge, color = if (destructive) colors.error else colors.onSurface)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatRow(chat: AiChat, selected: Boolean, busy: Boolean, unread: Boolean, onOpen: (Long) -> Unit, onHold: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) colors.secondaryContainer else Color.Transparent)
            .combinedClickable(onClick = { onOpen(chat.id) }, onLongClick = onHold)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            chat.label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) colors.onSecondaryContainer else colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (busy) Text("Writing", style = MaterialTheme.typography.labelSmall, color = colors.primary)
        if (unread && !busy) Box(Modifier.size(10.dp).clip(CircleShape).background(colors.primary))
        if (chat.pinned) Icon(AppIcons.PushPin, contentDescription = "Pinned", tint = colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
    }
}

/** What the app asked, for the list: the text between triple quotes of a note prompt, or the start of the prompt. */
internal fun promptTitle(prompt: String, max: Int = 80): String {
    val mark = prompt.indexOf(CONTEXT_MARK)
    if (mark >= 0) return prompt.substring(mark + CONTEXT_MARK.length).trim().take(max)
    val quote = "\"\"\""
    val start = prompt.indexOf(quote)
    val end = if (start >= 0) prompt.indexOf(quote, start + 3) else -1
    val source = if (start >= 0 && end > start) prompt.substring(start + 3, end) else prompt
    return source.trim().replace('\n', ' ').take(max).ifBlank { "AI request" }
}
