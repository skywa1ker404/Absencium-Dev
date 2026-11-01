package skywa1ker404.absencium.ui.aihub

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
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

/** What the student wrote, on the right in a quiet bubble, with the files sent. Press and hold for Copy and Edit. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserMessage(message: ChatMessage, onCopy: () -> Unit, onEdit: () -> Unit, enabled: Boolean) {
    val colors = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        message.attachments.forEach { file ->
            Row(
                modifier = Modifier.widthIn(max = 280.dp).clip(RoundedCornerShape(16.dp)).background(colors.surfaceContainerHigh).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(fileIcon(file.mime), contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Text(file.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Box {
            Text(
                message.text,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurface,
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .clip(RoundedCornerShape(26.dp, 26.dp, 8.dp, 26.dp))
                    .background(colors.surfaceContainerHigh)
                    .combinedClickable(onClick = {}, onLongClick = { menu = true })
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            )
            AppMenu(expanded = menu, onDismiss = { menu = false }) {
                AppMenuItem(AppIcons.ContentCopy, "Copy", { menu = false; onCopy() })
                AppMenuItem(AppIcons.Edit, "Edit", { menu = false; onEdit() }, enabled = enabled)
            }
        }
    }
}

internal fun fileIcon(mime: String) = when {
    mime.startsWith("image/") -> AppIcons.Image
    mime.startsWith("audio/") -> AppIcons.AudioFile
    mime.startsWith("video/") -> AppIcons.PlayCircle
    else -> AppIcons.Description
}

/** A question that has no answer (stopped, or the app was closed before it came): says so and offers Retry. */
@Composable
fun NoAnswerYet(onRetry: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("No answer yet", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        PillButton(text = "Retry", onClick = onRetry, compact = true, tonal = true)
    }
}

/**
 * The AI's answer: formatted text on the page itself (no bubble), the web pages it used, change cards it proposed, and an action row
 * (copy, answer again, share, more). The last answer also carries the "can make mistakes" line, like the reference chat apps. A failed
 * answer shows only a short line and Retry; the reason is in the bar at the bottom of the screen.
 */
@Composable
fun AssistantMessage(
    message: ChatMessage,
    chatId: Long,
    isLast: Boolean,
    allowed: Set<AiScope>,
    results: Map<String, ActionResult>,
    working: Set<String>,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit,
    onRetryWithOther: () -> Unit,
    onContinue: () -> Unit,
    onSaveAsNote: () -> Unit,
    onApply: (List<Pair<Int, AiAction>>) -> Unit,
    onDismiss: (Int) -> Unit,
    onOpenLink: (String) -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    var more by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (message.failed) {
            Row(
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(colors.errorContainer).padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("I couldn't answer that.", style = MaterialTheme.typography.bodyMedium, color = colors.onErrorContainer, modifier = Modifier.weight(1f, fill = false))
                if (isLast) {
                    PillButton(text = "Retry", onClick = onRegenerate, compact = true, tonal = true)
                    PillButton(text = "Another model", onClick = onRetryWithOther, compact = true, tonal = true)
                }
            }
            return@Column
        }
        SelectionContainer { AiMarkdown(message.text, onOpenLink = onOpenLink) }
        message.quiz?.let { QuizCard(it) }
        if (message.sources.isNotEmpty()) Sources(message)
        val actions = remember(message.actions) { message.actions?.let { AiActionParser.parse(it) }.orEmpty() }
        val visible = actions.withIndex().filter { it.value.scope in allowed }
        val pending = visible.filter { it.index !in message.doneActions }
        visible.forEach { (index, action) ->
            val key = "$chatId:${message.id}:$index"
            ActionCard(
                action = action,
                done = index in message.doneActions,
                busy = key in working,
                result = results[key],
                onApply = { onApply(listOf(index to action)) },
                onDismiss = { onDismiss(index) },
            )
        }
        if (pending.size > 1) {
            PillButton(text = "Apply all ${pending.size}", onClick = { onApply(pending.map { it.index to it.value }) }, compact = true)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            ActionIcon(AppIcons.ContentCopy, "Copy", onCopy)
            if (isLast) ActionIcon(AppIcons.Refresh, "Answer again", onRegenerate)
            ActionIcon(AppIcons.Share, "Share") {
                val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, message.text) }
                context.startActivity(Intent.createChooser(send, "Share answer").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            Box {
                ActionIcon(AppIcons.MoreVert, "More") { more = true }
                AppMenu(expanded = more, onDismiss = { more = false }) {
                    AppMenuItem(AppIcons.Draft, "Save as note", { more = false; onSaveAsNote() })
                    if (isLast) {
                        AppMenuItem(AppIcons.Refresh, "Retry with another model", { more = false; onRetryWithOther() })
                        AppMenuItem(AppIcons.ArrowDown, "Continue", { more = false; onContinue() })
                    }
                }
            }
        }
        if (isLast) {
            Text(
                (if (message.model.isNotBlank()) "${message.model} · " else "") + "Absencium AI can make mistakes. Check important facts.",
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ActionIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

/** The web pages the answer was based on, one per line; a tap opens the page. */
@Composable
private fun Sources(message: ChatMessage) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.clip(CircleShape).clickable { open = !open }.background(colors.surfaceContainerHigh).padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(AppIcons.Link, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.onSurfaceVariant)
            Text("${message.sources.size} sources", style = MaterialTheme.typography.labelLarge)
        }
        if (open) {
            message.sources.forEachIndexed { i, source ->
                Text(
                    "${i + 1}. ${source.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(source.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    }.padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }
    }
}

/** One change the AI suggests. Nothing happens until Apply is tapped. */
@Composable
private fun ActionCard(action: AiAction, done: Boolean, busy: Boolean, result: ActionResult?, onApply: () -> Unit, onDismiss: () -> Unit) {
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
                    PillButton(text = if (busy) "Working" else "Apply", onClick = onApply, compact = true, loading = busy, enabled = !busy)
                    PillButton(text = "Dismiss", onClick = onDismiss, compact = true, tonal = true, enabled = !busy)
                }
            }
        }
    }
}
