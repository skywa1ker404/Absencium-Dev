package me.rerere.rikkahub.ui.pages.menu

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import me.rerere.rikkahub.AiHost
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.data.model.Conversation
import me.rerere.rikkahub.data.repository.ConversationRepository
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.theme.AppShapes
import org.koin.compose.koinInject
import java.text.DateFormat
import java.util.Date

/** The folder on the Statistics page that opens the list of what the AI did. */
@Composable
internal fun AiActionsFolder() {
    if (AiHost.actionTargets == null) return
    val navController = LocalNavController.current
    val repo: ConversationRepository = koinInject()
    val chats by repo.getConversationsOfAssistant(AiHost.ACTIONS_ASSISTANT_ID).collectAsStateWithLifecycle(initialValue = emptyList())
    Card(
        onClick = { navController.navigate(Screen.AiActions) },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        shape = AppShapes.CardMedium,
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier.size(40.dp).clip(AppShapes.Chip).background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(22.dp)) }
            Column(modifier = Modifier.weight(1f)) {
                Text("AI actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (chats.isEmpty()) "Notes, homework and changes made by AI appear here" else "${chats.size} chats",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/** Every chat the app's AI had for an action, like the normal chat list: open one to read it, nothing can be sent. */
@Composable
fun AiActionsPage() {
    val repo: ConversationRepository = koinInject()
    val chats by repo.getConversationsOfAssistant(AiHost.ACTIONS_ASSISTANT_ID).collectAsStateWithLifecycle(initialValue = emptyList())
    val withTarget by (AiHost.actionTargets ?: flowOf(emptySet())).collectAsStateWithLifecycle(initialValue = emptySet())
    val navController = LocalNavController.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton() },
                title = { Text("AI actions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
            )
        },
    ) { innerPadding ->
        if (chats.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Nothing yet", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Notes and homework made on Add Entry, and everything else the app asks the AI to do, are listed here as chats you can read.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = innerPadding.calculateTopPadding() + 8.dp, bottom = innerPadding.calculateBottomPadding() + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(chats, key = { it.id.toString() }) { chat ->
                    ActionChatCard(
                        chat = chat,
                        canOpen = chat.id.toString() in withTarget,
                        onRead = { navController.navigate(Screen.Chat(id = chat.id.toString())) },
                        onOpen = { AiHost.openActionTarget(context, chat.id.toString()) },
                        onDelete = {
                            scope.launch {
                                repo.deleteConversation(chat)
                                AiHost.forgetAction(chat.id.toString())
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionChatCard(chat: Conversation, canOpen: Boolean, onRead: () -> Unit, onOpen: () -> Unit, onDelete: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(
        onClick = onRead,
        modifier = Modifier.fillMaxWidth().animateContentSize(spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer, contentColor = colors.onSurface),
        shape = AppShapes.CardMedium,
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(chat.title.ifBlank { "AI action" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(chat.createAt.toEpochMilli())),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            if (canOpen) FilledTonalButton(onClick = onOpen) { Text("Open") }
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, contentDescription = "Delete this chat") }
        }
    }
}
