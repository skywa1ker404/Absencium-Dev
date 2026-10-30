package skywa1ker404.absencium.ui.vault

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import skywa1ker404.absencium.ui.icons.AppIcons

/** What the AI panel is showing besides the actions: nothing, a running request, an answer, an error, or the "not set up" notice. */
data class AiPanelState(
    val running: Boolean = false,
    val result: String? = null,
    val resultReplaces: Boolean = false,
    val error: String? = null,
    val setupMissing: Boolean = false,
    /** What the actions work on: "the selection" or "the whole note", with its length. */
    val scopeLabel: String = "",
    val sourceLength: Int = 0,
    /** False when the answer would replace a whole note that holds pictures or tables; only Insert below is offered then. */
    val canReplace: Boolean = true,
)

/**
 * The AI panel of the editor. A prompt bar for your own instruction, then the actions (each says how much text it needs), then
 * the answer with Replace / Insert below / Discard. The history of this note's requests is behind the ⋮ button.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiPanel(
    state: AiPanelState,
    prompt: String,
    onPrompt: (String) -> Unit,
    onSend: () -> Unit,
    history: List<AiHistoryItem>,
    onAction: (AiAction) -> Unit,
    onReplace: () -> Unit,
    onInsert: () -> Unit,
    onDiscard: () -> Unit,
    onCancel: () -> Unit,
    onOpenHistory: (AiHistoryItem) -> Unit,
    onOpenSettings: () -> Unit,
    onUse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    var showHistory by remember { mutableStateOf(false) }
    GlassCard(modifier) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp).animateContentSize(tween(180)),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.AutoAwesome, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                Text(
                    text = if (showHistory) "History" else "AI",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp).weight(1f),
                )
                if (history.isNotEmpty()) Tool("History", { onUse(); showHistory = !showHistory }, showHistory, icon = AppIcons.History)
            }

            AnimatedContent(
                targetState = showHistory,
                transitionSpec = { fadeIn(tween(140)).togetherWith(fadeOut(tween(90))) },
                label = "aiHistory",
            ) { historyShown ->
                if (historyShown) {
                    Column(
                        modifier = Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        history.asReversed().forEach { item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.surfaceContainerHigh)
                                    .clickable { onUse(); showHistory = false; onOpenHistory(item) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            ) {
                                Text(item.title, style = MaterialTheme.typography.labelLarge)
                                Text(item.result.take(110), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 2)
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Your own instruction.
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CircleShape)
                                .background(colors.surfaceContainerHigh)
                                .padding(start = 14.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(modifier = Modifier.weight(1f).padding(vertical = 10.dp)) {
                                if (prompt.isEmpty()) Text("Ask the AI to change or write…", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                BasicTextField(
                                    value = prompt,
                                    onValueChange = onPrompt,
                                    singleLine = true,
                                    enabled = !state.running,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface),
                                    cursorBrush = SolidColor(colors.primary),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            if (prompt.isEmpty()) skywa1ker404.absencium.ui.components.VoiceInputButton(onText = { onPrompt(it) }, enabled = !state.running)
                            Tool("Send", { onSend() }, selected = prompt.isNotBlank() && !state.running, icon = AppIcons.SkipNext)
                        }

                        if (state.scopeLabel.isNotEmpty()) {
                            Text(
                                "Works on ${state.scopeLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                            )
                        }

                        // While an answer waits for Replace / Insert / Discard the action chips fold away, so the panel stays short.
                        if (state.result == null) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            AiAction.entries.forEach { action ->
                                val short = action.tooShort(state.sourceLength)
                                Row(
                                    modifier = Modifier
                                        .alpha(if (short || state.running) 0.45f else 1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(colors.surfaceContainerHigh)
                                        .clickable(enabled = !state.running) { onUse(); onAction(action) }
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                        .semantics { contentDescription = if (short) "${action.label}, needs ${action.minChars} characters" else action.label },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(action.icon(), contentDescription = null, modifier = Modifier.size(18.dp), tint = colors.onSurface)
                                    Text(action.label, style = MaterialTheme.typography.labelLarge)
                                    if (action.minChars > 0) {
                                        Text("${action.minChars}+", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (state.running) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Working…", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Tool("Stop", { onCancel() }, icon = AppIcons.Close)
                }
            }
            state.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.error) }
            if (state.setupMissing) {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.secondaryContainer).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("AI is off or not set up", style = MaterialTheme.typography.titleSmall, color = colors.onSecondaryContainer)
                    Text(
                        "Add a provider, a model and your API key in Settings, AI & Integrations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSecondaryContainer,
                    )
                    Text(
                        "Open Settings",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primary,
                        modifier = Modifier.clip(CircleShape).clickable { onOpenSettings() }.padding(vertical = 4.dp),
                    )
                }
            }
            state.result?.let { result ->
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surfaceContainerHighest).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(modifier = Modifier.heightIn(max = 200.dp).verticalScroll(rememberScrollState())) {
                        AiMarkdown(result)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (state.canReplace) ResultButton("Replace", state.resultReplaces) { onUse(); onReplace() }
                        ResultButton("Insert below", !state.resultReplaces || !state.canReplace) { onUse(); onInsert() }
                        ResultButton("Discard", false) { onUse(); onDiscard() }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultButton(label: String, primary: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (primary) colors.onPrimary else colors.onSurface,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (primary) colors.primary else colors.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}
