package skywa1ker404.absencium.ui.aihub

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import skywa1ker404.absencium.data.ai.ActionResult
import skywa1ker404.absencium.data.ai.AiAction
import skywa1ker404.absencium.data.ai.AiActionParser
import skywa1ker404.absencium.data.ai.AiScope
import skywa1ker404.absencium.data.ai.ChatMessage
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.AppMenu
import skywa1ker404.absencium.ui.components.AppMenuItem
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.vault.AiMarkdown

/** What the student wrote, on the right. Press and hold for Copy and Edit. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserMessage(message: ChatMessage, onCopy: () -> Unit, onEdit: () -> Unit, enabled: Boolean) {
    val colors = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Text(
            message.text,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onPrimaryContainer,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(24.dp, 24.dp, 6.dp, 24.dp))
                .background(colors.primaryContainer)
                .combinedClickable(onClick = {}, onLongClick = { menu = true })
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
        AppMenu(expanded = menu, onDismiss = { menu = false }) {
            AppMenuItem(AppIcons.ContentCopy, "Copy", { menu = false; onCopy() })
            AppMenuItem(AppIcons.Edit, "Edit", { menu = false; onEdit() }, enabled = enabled)
        }
    }
}

/**
 * The AI's answer: formatted text on the page itself (no bubble), with Copy and, for the last one, a regenerate button. Changes it
 * proposed follow as cards. A failed answer shows only a short line and Retry; the reason is in the bar at the bottom of the screen.
 */
@Composable
fun AssistantMessage(
    message: ChatMessage,
    isLast: Boolean,
    allowed: Set<AiScope>,
    results: Map<String, ActionResult>,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit,
    onApply: (Int, AiAction) -> Unit,
    onApplyAll: (List<Pair<Int, AiAction>>) -> Unit,
    onDismiss: (Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (message.failed) {
            Row(
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(colors.errorContainer).padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("I couldn't answer that.", style = MaterialTheme.typography.bodyMedium, color = colors.onErrorContainer, modifier = Modifier.weight(1f, fill = false))
                if (isLast) PillButton(text = "Retry", onClick = onRegenerate, compact = true, tonal = true)
            }
            return@Column
        }
        SelectionContainer { AiMarkdown(message.text) }
        val actions = remember(message.actions) { message.actions?.let { AiActionParser.parse(it) }.orEmpty() }
        val visible = actions.withIndex().filter { it.value.scope in allowed }
        val pending = visible.filter { it.index !in message.doneActions }
        visible.forEach { (index, action) ->
            ActionCard(
                action = action,
                done = index in message.doneActions,
                result = results["${message.id}:$index"],
                onApply = { onApply(index, action) },
                onDismiss = { onDismiss(index) },
            )
        }
        if (pending.size > 1) {
            PillButton(text = "Apply all ${pending.size}", onClick = { onApplyAll(pending.map { it.index to it.value }) }, compact = true)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
                Icon(AppIcons.ContentCopy, contentDescription = "Copy", tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
            if (isLast) {
                IconButton(onClick = onRegenerate, modifier = Modifier.size(36.dp)) {
                    Icon(AppIcons.Refresh, contentDescription = "Answer again", tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
            if (message.model.isNotBlank()) {
                Text(message.model, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, maxLines = 1, modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

/** One change the AI suggests. Nothing happens until Apply is tapped. */
@Composable
private fun ActionCard(action: AiAction, done: Boolean, result: ActionResult?, onApply: () -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    AppCard(modifier = Modifier.fillMaxWidth(), container = if (done) colors.surfaceContainerLow else colors.secondaryContainer) {
        Text(
            action.describe(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (done) colors.onSurfaceVariant else colors.onSecondaryContainer,
        )
        when {
            done -> Text(result?.message ?: "Handled", style = MaterialTheme.typography.labelMedium, color = colors.primary, modifier = Modifier.padding(top = 4.dp))
            else -> {
                if (result != null && !result.ok) {
                    Text(result.message, style = MaterialTheme.typography.bodySmall, color = colors.error, modifier = Modifier.padding(top = 4.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    PillButton(text = "Apply", onClick = onApply, compact = true)
                    PillButton(text = "Dismiss", onClick = onDismiss, compact = true, tonal = true)
                }
            }
        }
    }
}
