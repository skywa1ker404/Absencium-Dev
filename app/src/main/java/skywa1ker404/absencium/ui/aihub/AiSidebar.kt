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
import androidx.compose.material3.ModalDrawerSheet
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

/**
 * The sidebar of Absencium AI: new chat, a search box, the chats (pinned first), and a closed folder with the prompts the app itself
 * made for notes, the syllabus and homework. That folder is read-only: its entries can be read and copied, never continued or edited.
 * The settings icon leads to the AI options.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AiSidebar(
    chats: List<AiChat>,
    currentId: Long,
    appPrompts: List<AiHistoryEntry>,
    onNew: () -> Unit,
    onOpen: (Long) -> Unit,
    onSettings: () -> Unit,
    onClearPrompts: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var query by rememberSaveable { mutableStateOf("") }
    var folderOpen by rememberSaveable { mutableStateOf(false) }
    var viewing by remember { mutableStateOf<AiHistoryEntry?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    val shown = remember(chats, query) { chats.filter { query.isBlank() || it.label.contains(query, true) || it.messages.any { m -> m.text.contains(query, true) } } }
    val format = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }

    ModalDrawerSheet(drawerContainerColor = colors.surfaceContainerLow, drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Absencium AI", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).padding(start = 8.dp))
                IconButton(onClick = onSettings) { Icon(AppIcons.Settings, contentDescription = "AI settings", tint = colors.onSurfaceVariant) }
            }
            PillButton(text = "New chat", onClick = onNew, modifier = Modifier.fillMaxWidth())
            AppTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search chats", maxLines = 1) },
                leadingIcon = { Icon(AppIcons.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (shown.any { it.pinned }) item(key = "pinned-title") { SidebarLabel("Pinned") }
            items(shown.filter { it.pinned }, key = { it.id }) { chat -> ChatRow(chat, chat.id == currentId, onOpen) }
            item(key = "chats-title") { SidebarLabel(if (shown.isEmpty()) (if (query.isBlank()) "No chats yet" else "No chat matches") else "Chats") }
            items(shown.filter { !it.pinned }, key = { it.id }) { chat -> ChatRow(chat, chat.id == currentId, onOpen) }
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
private fun ChatRow(chat: AiChat, selected: Boolean, onOpen: (Long) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) colors.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent)
            .clickable { onOpen(chat.id) }
            .padding(horizontal = 12.dp, vertical = 12.dp),
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
