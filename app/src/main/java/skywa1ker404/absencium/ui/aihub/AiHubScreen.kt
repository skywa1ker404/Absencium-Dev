package skywa1ker404.absencium.ui.aihub

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AiSettingsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import skywa1ker404.absencium.AppContainer
import skywa1ker404.absencium.data.ai.AiHistoryEntry
import skywa1ker404.absencium.data.settings.AppSettings
import skywa1ker404.absencium.ui.components.AlertDialog
import skywa1ker404.absencium.ui.components.AppTextField
import skywa1ker404.absencium.ui.components.ExpressiveChip
import skywa1ker404.absencium.ui.components.ExpressiveEmptyState
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.components.VoiceInputButton
import skywa1ker404.absencium.ui.components.bounceIn
import skywa1ker404.absencium.ui.containerViewModel
import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.vault.AiMarkdown
import java.text.DateFormat
import java.util.Date

private const val CONTEXT_MARK = "Now the student writes:\n"

class AiHubViewModel(private val container: AppContainer) : ViewModel() {
    val entries: StateFlow<List<AiHistoryEntry>> = container.aiHistory.entries
    val settings: StateFlow<AppSettings?> = container.settingsState
    private val _pending = MutableStateFlow<String?>(null)

    /** The message being answered right now, or null. */
    val pending: StateFlow<String?> = _pending.asStateFlow()
    private var job: Job? = null

    fun ready(): Boolean = container.aiConnection() != null
    fun clear() = container.aiHistory.clear()
    fun remove(time: Long) = container.aiHistory.remove(time)

    /** Sends [text] with the last two answers as context, so a follow-up question makes sense. The answer lands in the history. */
    fun send(text: String) {
        val message = text.trim()
        if (message.isEmpty() || _pending.value != null) return
        val connection = container.aiConnection() ?: return
        _pending.value = message
        val earlier = entries.value.filter { it.error == null && !it.result.isNullOrBlank() }.take(2).reversed()
        val prompt = buildString {
            if (earlier.isNotEmpty()) {
                append("Earlier in this chat:\n")
                earlier.forEach { append("Student: ").append(userText(it.prompt).take(400)).append("\nAssistant: ").append(it.result.orEmpty().take(800)).append("\n\n") }
                append(CONTEXT_MARK)
            }
            append(message)
        }
        job = viewModelScope.launch {
            try {
                container.aiClient.generate(connection, prompt)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The failure is kept in the history (shown as a red answer) and announced by the failure bar.
            } finally {
                _pending.value = null
            }
        }
    }

    fun cancel() {
        job?.cancel()
        _pending.value = null
    }
}

private val Starters = listOf(
    "Explain simply" to "Explain this simply, with one example: ",
    "Make a quiz" to "Ask me 5 short quiz questions about: ",
    "Plan revision" to "Help me plan my revision for: ",
    "Summarise" to "Summarise the key points of: ",
)

/**
 * The AI Hub as a chat (images 05 and 13): your messages on the right, the AI's answers on the left as formatted text, suggestion chips and
 * an input pill with a mic and a send button at the bottom. Everything stays on this phone; requests made elsewhere in the app (notes,
 * syllabus, homework) appear here as well, and any exchange can be copied or deleted.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Destination<RootGraph>
@Composable
fun AiHubScreen(navigator: DestinationsNavigator) {
    val viewModel = containerViewModel { AiHubViewModel(it) }
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val pending by viewModel.pending.collectAsStateWithLifecycle()
    val ready = remember(settings) { viewModel.ready() }
    var confirmClear by remember { mutableStateOf(false) }
    var input by rememberSaveable { mutableStateOf("") }
    val format = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current

    fun send() {
        viewModel.send(input)
        input = ""
    }

    ScreenScaffold(
        title = "AI Hub",
        onBack = { navigator.navigateUp() },
        scrollable = false,
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
                        Text("AI is off or not set up", style = MaterialTheme.typography.bodyMedium, color = colors.onSecondaryContainer, modifier = Modifier.weight(1f))
                        TextButton(onClick = { navigator.navigate(AiSettingsScreenDestination) }) { Text("Open settings") }
                    }
                } else if (input.isBlank() && pending == null) {
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Starters.forEach { (label, start) ->
                            ExpressiveChip(selected = false, onClick = { input = start }, label = { Text(label) })
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (entries.isNotEmpty()) {
                        IconButton(onClick = { confirmClear = true }) { Icon(AppIcons.Delete, contentDescription = "Clear the chat", tint = colors.onSurfaceVariant) }
                    }
                    AppTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text("Ask anything", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        enabled = ready && pending == null,
                        maxLines = 4,
                        shape = RoundedCornerShape(28.dp),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (input.isBlank() && ready) VoiceInputButton(onText = { input = (input + " " + it).trim() }, enabled = pending == null)
                                if (pending != null) {
                                    IconButton(onClick = viewModel::cancel) { Icon(AppIcons.Close, contentDescription = "Stop", tint = colors.primary) }
                                } else {
                                    IconButton(onClick = ::send, enabled = ready && input.isNotBlank()) {
                                        Icon(AppIcons.SkipNext, contentDescription = "Send", tint = if (ready && input.isNotBlank()) colors.primary else colors.onSurfaceVariant)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
    ) {
        Box(Modifier.fillMaxSize()) {
            if (entries.isEmpty() && pending == null) {
                ExpressiveEmptyState(AppIcons.AutoAwesome, "Ask me anything", "Chats and every AI answer in the app are kept here, on your phone only.")
            } else {
                // Newest at the bottom: the list is laid out from the bottom up.
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = true,
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    pending?.let { message ->
                        item(key = "pending") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                UserBubble(message)
                                Row(
                                    modifier = Modifier.clip(RoundedCornerShape(24.dp, 24.dp, 24.dp, 6.dp)).background(colors.surfaceContainerHigh).padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    LoadingIndicator(modifier = Modifier.size(36.dp))
                                    Text("Thinking", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    items(entries, key = { it.time }) { entry ->
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = if (entry === entries.first()) Modifier.bounceIn(0) else Modifier) {
                            UserBubble(userText(entry.prompt))
                            AnswerBubble(
                                entry = entry,
                                meta = format.format(Date(entry.time)) + (if (entry.model.isNotBlank()) " \u00b7 ${entry.model}" else "") +
                                    (if (entry.attachments > 0) " \u00b7 ${entry.attachments} attached" else ""),
                                onCopy = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("AI answer", (entry.result ?: entry.error).orEmpty()))
                                },
                                onDelete = { viewModel.remove(entry.time) },
                            )
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
            title = { Text("Clear the chat?") },
            text = { Text("Every saved request and answer is deleted from this phone. Notes already made from them are not touched.") },
            confirmButton = { TextButton(onClick = { confirmClear = false; viewModel.clear() }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Keep") } },
        )
    }
}

@Composable
private fun UserBubble(text: String) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onPrimary,
            maxLines = 8,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(24.dp, 24.dp, 6.dp, 24.dp))
                .background(colors.primary)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun AnswerBubble(entry: AiHistoryEntry, meta: String, onCopy: () -> Unit, onDelete: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth(0.94f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier
                .clip(RoundedCornerShape(24.dp, 24.dp, 24.dp, 6.dp))
                .background(if (entry.error != null) colors.errorContainer else colors.surfaceContainerHigh)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            if (entry.error != null) {
                Text(entry.error, style = MaterialTheme.typography.bodyMedium, color = colors.onErrorContainer)
            } else {
                SelectionContainer { AiMarkdown(entry.result.orEmpty()) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(start = 6.dp)) {
            Text(meta, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            Text("Copy", style = MaterialTheme.typography.labelMedium, color = colors.primary, modifier = Modifier.clip(CircleShape).clickable(onClick = onCopy).padding(horizontal = 8.dp, vertical = 4.dp))
            Text("Delete", style = MaterialTheme.typography.labelMedium, color = colors.primary, modifier = Modifier.clip(CircleShape).clickable(onClick = onDelete).padding(horizontal = 8.dp, vertical = 4.dp))
        }
    }
}

/**
 * What the student wrote: the text after the chat context, or what sits between triple quotes in a note prompt, otherwise the start of the
 * prompt itself.
 */
private fun userText(prompt: String): String {
    val mark = prompt.indexOf(CONTEXT_MARK)
    if (mark >= 0) return prompt.substring(mark + CONTEXT_MARK.length).trim()
    val quote = "\"\"\""
    val start = prompt.indexOf(quote)
    val end = if (start >= 0) prompt.indexOf(quote, start + 3) else -1
    val source = if (start >= 0 && end > start) prompt.substring(start + 3, end) else prompt
    return source.trim().take(400).ifBlank { "AI request" }
}
