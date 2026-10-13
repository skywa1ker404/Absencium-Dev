package com.trajectory.vault.ui.calendar

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AiSettingsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.trajectory.vault.AppContainer
import com.trajectory.vault.data.ai.AiConnection
import com.trajectory.vault.data.ai.AiException
import com.trajectory.vault.data.ai.AttachmentReader
import com.trajectory.vault.data.ai.HolidayCandidate
import com.trajectory.vault.data.ai.HolidayImporter
import com.trajectory.vault.data.ai.ImportAttachment
import com.trajectory.vault.data.db.Priority
import com.trajectory.vault.domain.pretty
import com.trajectory.vault.logging.DebugLogger
import com.trajectory.vault.ui.components.AppCard
import com.trajectory.vault.ui.components.HintText
import com.trajectory.vault.ui.components.MessageCard
import com.trajectory.vault.ui.components.PillButton
import com.trajectory.vault.ui.components.ScreenScaffold
import com.trajectory.vault.ui.components.SectionTitle
import com.trajectory.vault.ui.containerViewModel
import com.trajectory.vault.ui.settings.aiReady
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** A holiday the AI found, with whether the user wants to add it. */
data class FoundHoliday(val holiday: HolidayCandidate, val selected: Boolean = true)

data class HolidayImportState(
    val text: String = "",
    val attachments: List<ImportAttachment> = emptyList(),
    val busy: Boolean = false,
    val found: List<FoundHoliday>? = null,
    val message: String? = null,
    val added: Boolean = false,
)

class HolidayImportViewModel(private val container: AppContainer) : ViewModel() {

    private val importer = HolidayImporter(container.aiClient)

    /** The saved AI connection, or null while Ask AI has no provider, model or key. */
    val connection: StateFlow<AiConnection?> = container.settingsRepository.settings
        .map { s ->
            if (aiReady(s, container.apiKeyVault.hasKey())) {
                AiConnection(s.aiProvider, s.effectiveAiStyle, s.aiBaseUrl, s.aiModel)
            } else {
                null
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _state = MutableStateFlow(HolidayImportState())
    val state: StateFlow<HolidayImportState> = _state.asStateFlow()

    fun setText(text: String) = _state.update { it.copy(text = text, found = null) }

    fun addFiles(resolver: android.content.ContentResolver, uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            try {
                val read = withContext(Dispatchers.IO) { uris.map { AttachmentReader.read(resolver, it) } }
                _state.update { it.copy(attachments = it.attachments + read, found = null, busy = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: AiException) {
                _state.update { it.copy(busy = false, message = e.message) }
            } catch (e: Exception) {
                DebugLogger.e("HolidayImport", "Could not read the files", e)
                _state.update { it.copy(busy = false, message = "Could not read that file. See debug.log for details.") }
            }
        }
    }

    fun removeAttachment(attachment: ImportAttachment) =
        _state.update { it.copy(attachments = it.attachments - attachment, found = null) }

    fun find() {
        val conn = connection.value ?: return
        val current = _state.value
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null, found = null) }
            try {
                val result = importer.extract(conn, current.text, current.attachments, LocalDate.now())
                _state.update {
                    it.copy(
                        busy = false,
                        found = result.map { holiday -> FoundHoliday(holiday) },
                        message = if (result.isEmpty()) "The AI did not find any holidays in that." else null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: AiException) {
                _state.update { it.copy(busy = false, message = e.message) }
            } catch (e: Exception) {
                DebugLogger.e("HolidayImport", "AI request failed", e)
                _state.update { it.copy(busy = false, message = "Something went wrong. See debug.log for details.") }
            }
        }
    }

    fun toggle(holiday: HolidayCandidate) = _state.update { state ->
        state.copy(found = state.found?.map { if (it.holiday == holiday) it.copy(selected = !it.selected) else it })
    }

    fun addSelected() {
        val chosen = _state.value.found.orEmpty().filter { it.selected }.map { it.holiday }
        if (chosen.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            try {
                var added = 0
                var skipped = 0
                chosen.forEach { holiday ->
                    val (a, s) = container.calendarRepository.addHolidayRange(
                        holiday.start, holiday.end, holiday.title, autoSkip = true, priority = Priority.LOW,
                    )
                    added += a
                    skipped += s
                }
                _state.update {
                    it.copy(
                        busy = false,
                        added = true,
                        message = "Added $added ${if (added == 1) "day" else "days"}" +
                            if (skipped > 0) ". $skipped already had a holiday and were left as they are." else ".",
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DebugLogger.e("HolidayImport", "Could not add the holidays", e)
                _state.update { it.copy(busy = false, message = "Could not add the holidays. See debug.log for details.") }
            }
        }
    }
}

/** Import holidays from pasted text, a photo, a PDF or a text file, read by the user's own AI model. */
@Destination<RootGraph>
@Composable
fun HolidayImportScreen(navigator: DestinationsNavigator) {
    val viewModel = containerViewModel { HolidayImportViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val connection by viewModel.connection.collectAsStateWithLifecycle()
    val resolver = LocalContext.current.contentResolver
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.addFiles(resolver, uris)
    }

    ScreenScaffold(title = "Import holidays with AI", onBack = { navigator.navigateUp() }) {
        val conn = connection
        if (conn == null) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Text("Set up AI first", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Pick a provider and model and save your API key in Settings, AI & Integrations.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                PillButton(text = "Open AI settings", onClick = { navigator.navigate(AiSettingsScreenDestination) })
            }
            return@ScreenScaffold
        }

        HintText(
            "What you add here is sent to your AI provider (${conn.provider.label}, model ${conn.model}) " +
                "to be read. It is not stored by Absencium.",
        )
        OutlinedTextField(
            value = state.text,
            onValueChange = viewModel::setText,
            label = { Text("Paste the holiday list") },
            minLines = 4,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton(
                text = "Add photo, PDF or text file",
                tonal = true,
                onClick = { picker.launch(arrayOf("image/*", "application/pdf", "text/plain")) },
                enabled = !state.busy,
            )
        }
        state.attachments.forEach { attachment ->
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = attachment.name + if (attachment.images.size > 1) " (${attachment.images.size} pages)" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { viewModel.removeAttachment(attachment) }) { Text("Remove") }
                }
            }
        }

        val feedback = state.message
        if (feedback != null) MessageCard(feedback)

        PillButton(
            text = "Find holidays",
            onClick = viewModel::find,
            enabled = !state.busy && (state.text.isNotBlank() || state.attachments.isNotEmpty()),
            loading = state.busy && state.found == null,
            modifier = Modifier.fillMaxWidth(),
        )

        val found = state.found
        if (!found.isNullOrEmpty()) {
            SectionTitle("Found ${found.size}")
            HintText("Check these against your calendar before adding. The AI can misread dates.")
            found.forEach { item ->
                AppCard(modifier = Modifier.fillMaxWidth(), onClick = { viewModel.toggle(item.holiday) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = item.selected, onCheckedChange = { viewModel.toggle(item.holiday) })
                        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                            Text(item.holiday.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (item.holiday.dayCount == 1) {
                                    "${item.holiday.start.pretty()} ${item.holiday.start.year}"
                                } else {
                                    "${item.holiday.start.pretty()} to ${item.holiday.end.pretty()} ${item.holiday.end.year}" +
                                        " (${item.holiday.dayCount} days)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            val count = found.count { it.selected }
            PillButton(
                text = if (state.added) "Added" else "Add $count ${if (count == 1) "holiday" else "holidays"}",
                onClick = viewModel::addSelected,
                enabled = count > 0 && !state.busy && !state.added,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
