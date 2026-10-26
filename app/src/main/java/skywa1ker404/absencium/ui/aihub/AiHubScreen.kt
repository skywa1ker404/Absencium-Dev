package skywa1ker404.absencium.ui.aihub

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.flow.StateFlow
import skywa1ker404.absencium.AppContainer
import skywa1ker404.absencium.data.ai.AiHistoryEntry
import skywa1ker404.absencium.data.settings.AppSettings
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.bounceIn
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.containerViewModel
import java.text.DateFormat
import java.util.Date

class AiHubViewModel(private val container: AppContainer) : ViewModel() {
    val entries: StateFlow<List<AiHistoryEntry>> = container.aiHistory.entries
    val settings: StateFlow<AppSettings?> = container.settingsState
    fun clear() = container.aiHistory.clear()
    fun remove(time: Long) = container.aiHistory.remove(time)
}

/** Every AI request and its answer, newest first. They stay on this phone and can be removed one by one or all at once. */
@Destination<RootGraph>
@Composable
fun AiHubScreen(navigator: DestinationsNavigator) {
    val viewModel = containerViewModel { AiHubViewModel(it) }
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }
    val format = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }

    ScreenScaffold(title = "AI Hub", onBack = { navigator.navigateUp() }) {
        if (settings?.askAiEnabled == false) {
            HintText("AI Features are off, so nothing new is sent. You can still read and delete the history below. Turn them on in Settings, AI Features.")
        }
        HintText("Each request you made, with the answer. This history never leaves this phone.")
        if (entries.isEmpty()) HintText("No AI requests yet.")
        else PillButton(text = "Clear all history", tonal = true, onClick = { confirmClear = true })

        entries.forEachIndexed { index, entry ->
            androidx.compose.runtime.key(entry.time) {
                var open by remember { mutableStateOf(false) }
                AppCard(modifier = Modifier.fillMaxWidth().bounceIn((index * 45).coerceAtMost(450)).animateContentSize(), onClick = { open = !open }) {
                    Text(
                        text = firstLine(entry.prompt),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = if (open) Int.MAX_VALUE else 2,
                    )
                    Text(
                        text = format.format(Date(entry.time)) + (if (entry.model.isNotBlank()) " · ${entry.model}" else "") +
                            (if (entry.attachments > 0) " · ${entry.attachments} attached" else "") +
                            (if (entry.error != null) " · failed" else ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!open) {
                        Text(
                            text = (entry.result ?: entry.error).orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    } else {
                        SelectionContainer {
                            androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                                Text("Answer", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                Text((entry.result ?: entry.error).orEmpty(), style = MaterialTheme.typography.bodyMedium)
                                Text("What was sent", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                Text(entry.prompt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Row(modifier = Modifier.padding(top = 12.dp)) {
                            PillButton(text = "Delete", compact = true, tonal = true, onClick = { viewModel.remove(entry.time) })
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Clear the AI history?") },
            text = { Text("Every saved request and answer is deleted from this phone. Notes already made from them are not touched.") },
            confirmButton = { TextButton(onClick = { confirmClear = false; viewModel.clear() }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Keep") } },
        )
    }
}

/** Note prompts carry what the student typed between triple quotes; show that, otherwise the first readable line. */
private fun firstLine(prompt: String): String {
    val quote = "\"\"\""
    val start = prompt.indexOf(quote)
    val end = if (start >= 0) prompt.indexOf(quote, start + 3) else -1
    val source = if (start >= 0 && end > start) prompt.substring(start + 3, end) else prompt
    return source.trim().lineSequence().firstOrNull { it.isNotBlank() }?.take(140) ?: "AI request"
}
