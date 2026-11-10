package me.rerere.rikkahub.ui.pages.menu

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.AiHost
import me.rerere.rikkahub.AiHost.HostAction
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.theme.AppShapes
import java.text.DateFormat
import java.util.Date

/** The folder on the Statistics page that opens the list of what the AI did. */
@Composable
internal fun AiActionsFolder() {
    val navController = LocalNavController.current
    val actions by (AiHost.actions ?: return).collectAsStateWithLifecycleSafe()
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
                    if (actions.isEmpty()) "Notes, homework and changes made by AI appear here" else "${actions.size} recorded",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun kotlinx.coroutines.flow.Flow<List<HostAction>>.collectAsStateWithLifecycleSafe() =
    this.collectAsStateWithLifecycle(initialValue = emptyList())

/** Everything the AI made or changed: what was asked, a preview of the result, and a button to open it where it lives. */
@Composable
fun AiActionsPage() {
    val actions by (AiHost.actions ?: kotlinx.coroutines.flow.flowOf(emptyList())).collectAsStateWithLifecycleSafe()
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton() },
                title = { Text("AI actions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                actions = {
                    if (actions.isNotEmpty()) TextButton(onClick = { AiHost.removeAction(null) }) { Text("Clear all") }
                },
            )
        },
    ) { innerPadding ->
        if (actions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Nothing yet", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Notes and homework made on Add Entry, and changes approved in the chat, are listed here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, top = innerPadding.calculateTopPadding() + 8.dp, bottom = innerPadding.calculateBottomPadding() + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(actions, key = { it.id }) { action -> ActionCard(action, onOpen = { AiHost.openAction(context, action.id) }) }
            }
        }
    }
}

@Composable
private fun ActionCard(action: HostAction, onOpen: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer, contentColor = colors.onSurface),
        shape = AppShapes.CardMedium,
    ) {
        Column(modifier = Modifier.clickable { expanded = !expanded }.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    if (action.ok) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                    contentDescription = if (action.ok) "Done" else "Failed",
                    tint = if (action.ok) colors.primary else colors.error,
                    modifier = Modifier.size(24.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(action.action, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (action.title.isNotBlank()) {
                        Text(action.title, style = MaterialTheme.typography.bodyMedium, maxLines = if (expanded) 4 else 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(
                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(action.time)),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            if (expanded) {
                action.fields.forEach { (label, value) ->
                    Column {
                        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                        Text(value, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                action.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.error) }
                action.preview?.let { preview ->
                    Text("Result", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    Text(
                        preview,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        maxLines = 14,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surfaceContainerHighest).padding(12.dp),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (action.canOpen) FilledTonalButton(onClick = onOpen) { Text("Open") }
                    Box(modifier = Modifier.weight(1f))
                    IconButton(onClick = { AiHost.removeAction(action.id) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Remove from the list")
                    }
                }
            } else if (action.canOpen) {
                Row { FilledTonalButton(onClick = onOpen) { Text("Open") } }
            }
        }
    }
}
