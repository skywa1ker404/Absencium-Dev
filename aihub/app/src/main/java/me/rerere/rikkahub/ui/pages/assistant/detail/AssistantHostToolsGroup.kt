package me.rerere.rikkahub.ui.pages.assistant.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.rerere.rikkahub.AiHost
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.ui.components.ui.HapticSwitch
import me.rerere.rikkahub.ui.pages.setting.components.SettingGroupItem
import me.rerere.rikkahub.ui.pages.setting.components.SettingsGroup
import me.rerere.rikkahub.ui.theme.AppShapes

/**
 * Every tool Absencium offers this assistant, in category folders (Vault, Attendance, Syllabus and so on) with a search field.
 * Each tool can be switched off, and can be set to run by itself or to ask first; this replaces the old "Do without asking" switches.
 */
@Composable
fun AssistantHostToolsGroup(assistant: Assistant, onUpdate: (Assistant) -> Unit) {
    val catalog = remember { AiHost.toolCatalog() }
    if (catalog.isEmpty()) return
    var query by rememberSaveable { mutableStateOf("") }
    val shown = catalog.filter { query.isBlank() || it.title.contains(query, true) || it.description.contains(query, true) || it.category.contains(query, true) }
    val folders = shown.groupBy { it.category }

    SettingsGroup(title = "Vault access approval") {
        // One setting for every tool: Manual asks before the AI reads a note's text or changes anything, Auto lets enabled tools run by themselves.
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(false to "Manual", true to "Auto").forEach { (auto, label) ->
                androidx.compose.material3.FilterChip(
                    selected = assistant.vaultApprovalAuto == auto,
                    onClick = { onUpdate(assistant.copy(vaultApprovalAuto = auto)) },
                    label = { Text(label) },
                    shape = androidx.compose.foundation.shape.CircleShape,
                )
            }
        }
        Text(
            if (assistant.vaultApprovalAuto) "Auto: the AI reads and changes your Vault without asking. Deleted items still go to the Trash."
            else "Manual: the AI waits for your approval before reading a note's text or changing anything.",
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    SettingsGroup(title = "Absencium tools") {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Search tools") },
            shape = AppShapes.InputField,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        folders.forEach { (category, tools) ->
            ToolFolder(
                category = category,
                tools = tools,
                total = catalog.count { it.category == category },
                assistant = assistant,
                forceOpen = query.isNotBlank(),
                onUpdate = onUpdate,
            )
        }
        if (folders.isEmpty()) {
            Text("No tool matches", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ToolFolder(
    category: String,
    tools: List<AiHost.ToolInfo>,
    total: Int,
    assistant: Assistant,
    forceOpen: Boolean,
    onUpdate: (Assistant) -> Unit,
) {
    var open by rememberSaveable(category) { mutableStateOf(false) }
    val expanded = open || forceOpen
    val on = tools.count { it.name !in assistant.hostToolsOff }
    Column(modifier = Modifier.animateContentSize(spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow))) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { open = !open }.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(category, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("$on of $total on", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HapticSwitch(
                checked = on > 0,
                onCheckedChange = { enable ->
                    val names = tools.map { it.name }.toSet()
                    onUpdate(assistant.copy(hostToolsOff = if (enable) assistant.hostToolsOff - names else assistant.hostToolsOff + names))
                },
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(spring(0.8f, 380f)) + fadeIn(),
            exit = shrinkVertically(spring(1f, 500f)) + fadeOut(),
        ) {
            Column {
                tools.forEach { tool ->
                    val enabled = tool.name !in assistant.hostToolsOff
                    SettingGroupItem(
                        title = tool.title,
                        subtitle = tool.description,
                        trailing = {
                            HapticSwitch(
                                checked = enabled,
                                onCheckedChange = { v ->
                                    onUpdate(assistant.copy(hostToolsOff = if (v) assistant.hostToolsOff - tool.name else assistant.hostToolsOff + tool.name))
                                },
                            )
                        },
                    )
                }
            }
        }
    }
}
