package com.absencium.app.ui.capture

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.absencium.app.AppContainer
import com.absencium.app.data.ai.AiConnection
import com.absencium.app.data.ai.AiException
import com.absencium.app.data.ai.AttachmentReader
import com.absencium.app.data.ai.ExamCandidate
import com.absencium.app.data.ai.ExamImporter
import com.absencium.app.data.ai.HolidayCandidate
import com.absencium.app.data.ai.HolidayImporter
import com.absencium.app.data.ai.ImportAttachment
import com.absencium.app.data.db.Priority
import com.absencium.app.data.db.SubjectEntity
import com.absencium.app.domain.pretty
import com.absencium.app.logging.DebugLogger
import com.absencium.app.ui.components.AppTextField
import com.absencium.app.ui.components.DropdownOption
import com.absencium.app.ui.components.InlineDropdown
import com.absencium.app.ui.components.PillButton
import com.absencium.app.ui.components.PopupShell
import com.absencium.app.ui.containerViewModel
import com.absencium.app.ui.icons.AppIcons
import com.absencium.app.ui.settings.aiReady
import com.absencium.app.ui.theme.LocalQuality
import com.absencium.app.ui.vault.entry.ActionTile
import com.absencium.app.ui.vault.entry.LoadingDots
import com.absencium.app.ui.vault.entry.SquareIconButton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/** What the import popup reads the document for. */
enum class ImportKind(val title: String, val noun: String, val pluralNoun: String) {
    HOLIDAYS("Import holidays", "holiday", "holidays"),
    EXAMS("Import exam timetable", "exam", "exams"),
}

enum class ImportStage { SOURCE, PASTE, WORKING, REVIEW }

data class FoundHoliday(val holiday: HolidayCandidate, val selected: Boolean = true)

/** An exam the AI found, with the subject it was matched to (null until the user picks one). */
data class FoundExam(val exam: ExamCandidate, val subjectId: Long?, val selected: Boolean = true)

data class ImportState(
    val kind: ImportKind = ImportKind.HOLIDAYS,
    val stage: ImportStage = ImportStage.SOURCE,
    val text: String = "",
    val attachments: List<ImportAttachment> = emptyList(),
    val holidays: List<FoundHoliday> = emptyList(),
    val exams: List<FoundExam> = emptyList(),
    val message: String? = null,
    val setupMissing: Boolean = false,
    val added: Boolean = false,
    val busy: Boolean = false,
)

/**
 * The shared import flow: pick a photo, document or camera shot (or paste text), let the user's AI model read it,
 * then review the list and confirm what to add. Holidays and exam timetables differ only in what is read and saved.
 */
class ImportViewModel(private val container: AppContainer) : ViewModel() {

    private val holidayImporter = HolidayImporter(container.aiClient)
    private val examImporter = ExamImporter(container.aiClient)
    private val store = container.attachmentStore

    val subjects: StateFlow<List<SubjectEntity>> = container.database.subjectDao().observeAll()
        .map { list -> list.filter { it.isActive } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _state = MutableStateFlow(ImportState())
    val state: StateFlow<ImportState> = _state.asStateFlow()

    private var job: Job? = null
    private var cameraFile: File? = null

    fun begin(kind: ImportKind) {
        job?.cancel()
        _state.value = ImportState(kind = kind, setupMissing = !aiIsReady())
    }

    fun cancel() {
        job?.cancel()
        cameraFile?.let { store.delete(it.absolutePath) }
        cameraFile = null
        _state.value = ImportState()
    }

    private fun aiIsReady(): Boolean {
        val settings = container.settingsState.value ?: return false
        return aiReady(settings, container.apiKeyVault.hasKey())
    }

    private fun connection(): AiConnection? {
        val s = container.settingsState.value ?: return null
        if (!aiReady(s, container.apiKeyVault.hasKey())) return null
        return AiConnection(s.aiProvider, s.effectiveAiStyle, s.aiBaseUrl, s.aiModel)
    }

    fun toSource() = _state.update { it.copy(stage = ImportStage.SOURCE, message = null, holidays = emptyList(), exams = emptyList(), added = false) }

    fun toPaste() = _state.update { it.copy(stage = ImportStage.PASTE, message = null) }

    fun setText(text: String) = _state.update { it.copy(text = text) }

    /** Reads the chosen photos, PDFs or text files, then sends them to the AI. */
    fun readFiles(resolver: android.content.ContentResolver, uris: List<Uri>) {
        if (uris.isEmpty()) return
        job?.cancel()
        _state.update { it.copy(stage = ImportStage.WORKING, message = null) }
        job = viewModelScope.launch {
            try {
                val read = withContext(Dispatchers.IO) { uris.map { AttachmentReader.read(resolver, it) } }
                _state.update { it.copy(attachments = read) }
                extract()
            } catch (e: CancellationException) {
                throw e
            } catch (e: AiException) {
                _state.update { it.copy(stage = ImportStage.SOURCE, message = e.message) }
            } catch (e: Exception) {
                DebugLogger.e("Import", "Could not read the files", e)
                _state.update { it.copy(stage = ImportStage.SOURCE, message = "Could not read that file. See debug.log for details.") }
            }
        }
    }

    /** A fresh file for the camera to write into. */
    fun prepareCameraFile(): Uri {
        val file = store.newFile("jpg")
        cameraFile = file
        return store.uriFor(file)
    }

    fun cameraFinished(resolver: android.content.ContentResolver, taken: Boolean) {
        val file = cameraFile ?: return
        cameraFile = null
        if (!taken || file.length() == 0L) {
            store.delete(file.absolutePath)
            return
        }
        val uri = store.uriFor(file)
        _state.update { it.copy(stage = ImportStage.WORKING, message = null) }
        job?.cancel()
        job = viewModelScope.launch {
            try {
                val read = withContext(Dispatchers.IO) { listOf(AttachmentReader.read(resolver, uri)) }
                _state.update { it.copy(attachments = read) }
                extract()
            } catch (e: CancellationException) {
                throw e
            } catch (e: AiException) {
                _state.update { it.copy(stage = ImportStage.SOURCE, message = e.message) }
            } catch (e: Exception) {
                DebugLogger.e("Import", "Could not read the camera photo", e)
                _state.update { it.copy(stage = ImportStage.SOURCE, message = "Could not read that photo. See debug.log for details.") }
            } finally {
                // The photo was only needed to be read; it is not kept.
                store.delete(file.absolutePath)
            }
        }
    }

    /** Sends the pasted text to the AI. */
    fun findInText() {
        job?.cancel()
        _state.update { it.copy(stage = ImportStage.WORKING, message = null, attachments = emptyList()) }
        job = viewModelScope.launch { extract() }
    }

    private suspend fun extract() {
        val conn = connection()
        if (conn == null) {
            _state.update { it.copy(stage = ImportStage.SOURCE, setupMissing = true) }
            return
        }
        val current = _state.value
        try {
            _state.update { it.copy(stage = ImportStage.WORKING) }
            if (current.kind == ImportKind.HOLIDAYS) {
                val found = holidayImporter.extract(conn, current.text, current.attachments, LocalDate.now())
                _state.update {
                    if (found.isEmpty()) {
                        it.copy(stage = ImportStage.SOURCE, message = "The AI did not find any holidays in that.")
                    } else {
                        it.copy(stage = ImportStage.REVIEW, holidays = found.map { h -> FoundHoliday(h) })
                    }
                }
            } else {
                val names = subjects.value.map { it.name }
                val found = examImporter.extract(conn, current.text, current.attachments, LocalDate.now(), names)
                _state.update {
                    if (found.isEmpty()) {
                        it.copy(stage = ImportStage.SOURCE, message = "The AI did not find any exams in that.")
                    } else {
                        it.copy(stage = ImportStage.REVIEW, exams = found.map { e -> FoundExam(e, matchSubject(e.subject)) })
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: AiException) {
            _state.update { it.copy(stage = ImportStage.SOURCE, message = e.message) }
        } catch (e: Exception) {
            DebugLogger.e("Import", "AI request failed", e)
            _state.update { it.copy(stage = ImportStage.SOURCE, message = "Something went wrong. See debug.log for details.") }
        }
    }

    /** The subject whose name equals, contains or is contained in the name found in the document. */
    private fun matchSubject(name: String): Long? {
        fun norm(text: String) = text.lowercase().filter { it.isLetterOrDigit() }
        val wanted = norm(name)
        if (wanted.isEmpty()) return null
        val list = subjects.value
        return list.firstOrNull { norm(it.name) == wanted }?.id
            ?: list.firstOrNull { norm(it.name).let { n -> n.isNotEmpty() && (n.contains(wanted) || wanted.contains(n)) } }?.id
    }

    fun toggleHoliday(item: HolidayCandidate) = _state.update { state ->
        state.copy(holidays = state.holidays.map { if (it.holiday == item) it.copy(selected = !it.selected) else it })
    }

    fun toggleExam(index: Int) = _state.update { state ->
        state.copy(exams = state.exams.mapIndexed { i, e -> if (i == index) e.copy(selected = !e.selected) else e })
    }

    fun setExamSubject(index: Int, subjectId: Long) = _state.update { state ->
        state.copy(exams = state.exams.mapIndexed { i, e -> if (i == index) e.copy(subjectId = subjectId) else e })
    }

    /** Adds what the user ticked. */
    fun addSelected() {
        val current = _state.value
        if (current.busy || current.added) return
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            try {
                val message = if (current.kind == ImportKind.HOLIDAYS) {
                    var added = 0
                    var skipped = 0
                    current.holidays.filter { it.selected }.forEach { item ->
                        val (a, s) = container.calendarRepository.addHolidayRange(
                            item.holiday.start, item.holiday.end, item.holiday.title, autoSkip = true, priority = Priority.LOW,
                        )
                        added += a
                        skipped += s
                    }
                    "Added $added ${if (added == 1) "day" else "days"}" +
                        if (skipped > 0) ". $skipped already had a holiday and were left as they are." else "."
                } else {
                    var added = 0
                    current.exams.filter { it.selected }.forEach { item ->
                        val subjectId = item.subjectId ?: return@forEach
                        container.calendarRepository.addExam(
                            subjectId = subjectId,
                            date = item.exam.date,
                            title = item.exam.title,
                            weightage = null,
                            room = item.exam.room,
                            syllabusNotes = null,
                            priority = Priority.HIGH,
                        )
                        added++
                    }
                    "Added $added ${if (added == 1) "exam" else "exams"} to your calendar."
                }
                _state.update { it.copy(busy = false, added = true, message = message) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DebugLogger.e("Import", "Could not add the items", e)
                _state.update { it.copy(busy = false, message = "Could not add them. See debug.log for details.") }
            }
        }
    }
}

/**
 * Import holidays or an exam timetable. It works like the Vault's Add Entry menu: Upload a Pic, Document / File,
 * Camera, then, instead of opening Text Notes, a list to review and confirm.
 */
@Composable
fun ImportPopup(kind: ImportKind, onDismiss: () -> Unit, onOpenAiSettings: () -> Unit) {
    val viewModel = containerViewModel { ImportViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val resolver = LocalContext.current.contentResolver

    LaunchedEffect(kind) { viewModel.begin(kind) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        viewModel.readFiles(resolver, uris)
    }
    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.readFiles(resolver, uris)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        viewModel.cameraFinished(resolver, taken)
    }

    fun close() {
        viewModel.cancel()
        onDismiss()
    }

    PopupShell(
        onDismissRequest = {
            when (state.stage) {
                ImportStage.SOURCE -> close()
                ImportStage.WORKING -> viewModel.cancel().also { viewModel.begin(kind) }
                else -> viewModel.toSource()
            }
        },
    ) {
        AnimatedContent(
            targetState = state.stage,
            transitionSpec = {
                fadeIn(tween(170, delayMillis = 90)).togetherWith(fadeOut(tween(90))) using SizeTransform(clip = false) { _, _ -> snap() }
            },
            label = "importStage",
        ) { stage ->
            when (stage) {
                ImportStage.SOURCE -> SourceStage(
                    state = state,
                    onClose = { close() },
                    onPhoto = { photoPicker.launch("image/*") },
                    onDocument = { documentPicker.launch(arrayOf("application/pdf", "text/plain")) },
                    onCamera = { camera.launch(viewModel.prepareCameraFile()) },
                    onPaste = viewModel::toPaste,
                    onOpenAiSettings = {
                        close()
                        onOpenAiSettings()
                    },
                )

                ImportStage.PASTE -> PasteStage(
                    state = state,
                    onBack = viewModel::toSource,
                    onText = viewModel::setText,
                    onFind = viewModel::findInText,
                )

                ImportStage.WORKING -> WorkingStage(
                    kind = state.kind,
                    onCancel = {
                        viewModel.cancel()
                        viewModel.begin(kind)
                    },
                )

                ImportStage.REVIEW -> ReviewStage(
                    state = state,
                    subjects = subjects,
                    viewModel = viewModel,
                    onBack = viewModel::toSource,
                    onDone = { close() },
                )
            }
        }
    }
}

@Composable
private fun SourceStage(
    state: ImportState,
    onClose: () -> Unit,
    onPhoto: () -> Unit,
    onDocument: () -> Unit,
    onCamera: () -> Unit,
    onPaste: () -> Unit,
    onOpenAiSettings: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SquareIconButton(AppIcons.Close, "Close", onClose)
            Column {
                Text(state.kind.title, style = MaterialTheme.typography.titleLarge)
                Text(
                    "Pick where the ${state.kind.pluralNoun} are written. Your AI model reads it and you confirm what to add.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        if (state.setupMissing) {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(colors.secondaryContainer).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("AI is not set up yet", style = MaterialTheme.typography.titleSmall, color = colors.onSecondaryContainer)
                Text(
                    "Reading a document needs an AI provider, a model (one that accepts images for photos) and your API key. " +
                        "Add them in Settings, AI & Integrations, then come back.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSecondaryContainer,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillButton(text = "Open Settings", compact = true, onClick = onOpenAiSettings, modifier = Modifier.weight(1f))
                    PillButton(text = "Close", compact = true, tonal = true, onClick = onClose, modifier = Modifier.weight(1f))
                }
            }
        } else {
            ActionTile("Upload a Pic", AppIcons.Image, colors.primaryContainer, colors.onPrimaryContainer, 0, onPhoto, Modifier.fillMaxWidth(), 84.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionTile("Document / File", AppIcons.Description, colors.tertiaryContainer, colors.onTertiaryContainer, 2, onDocument, Modifier.weight(1f), 104.dp)
                ActionTile("Camera", AppIcons.PhotoCamera, colors.secondaryContainer, colors.onSecondaryContainer, 1, onCamera, Modifier.weight(1f), 104.dp)
            }
            ActionTile("Paste text", AppIcons.Edit, colors.surfaceContainerHighest, colors.onSurface, 3, onPaste, Modifier.fillMaxWidth(), 72.dp)
        }
        state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.error) }
    }
}

@Composable
private fun PasteStage(state: ImportState, onBack: () -> Unit, onText: (String) -> Unit, onFind: () -> Unit) {
    Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SquareIconButton(AppIcons.ArrowBack, "Back", onBack)
            Text("Paste text", style = MaterialTheme.typography.titleLarge)
        }
        AppTextField(
            value = state.text,
            onValueChange = onText,
            label = { Text("Paste the ${if (state.kind == ImportKind.HOLIDAYS) "holiday list" else "exam timetable"}") },
            minLines = 5,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            PillButton(text = "Back", tonal = true, onClick = onBack, modifier = Modifier.weight(1f))
            PillButton(text = "Find ${state.kind.pluralNoun}", enabled = state.text.isNotBlank(), onClick = onFind, modifier = Modifier.weight(1.4f))
        }
    }
}

@Composable
private fun WorkingStage(kind: ImportKind, onCancel: () -> Unit) {
    Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        LoadingDots("Reading the ${kind.pluralNoun}", modifier = Modifier.padding(vertical = 18.dp))
        PillButton(text = "Cancel", tonal = true, onClick = onCancel, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ReviewStage(
    state: ImportState,
    subjects: List<SubjectEntity>,
    viewModel: ImportViewModel,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val q = LocalQuality.current
    val selectedCount = if (state.kind == ImportKind.HOLIDAYS) state.holidays.count { it.selected } else state.exams.count { it.selected }
    val missingSubject = state.kind == ImportKind.EXAMS && state.exams.any { it.selected && it.subjectId == null }
    Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SquareIconButton(AppIcons.ArrowBack, "Back", onBack)
            Column {
                val total = if (state.kind == ImportKind.HOLIDAYS) state.holidays.size else state.exams.size
                Text("Found $total", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Check these against your calendar before adding. The AI can misread dates.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }

        if (state.kind == ImportKind.HOLIDAYS) {
            state.holidays.forEach { item ->
                ReviewRow(
                    selected = item.selected,
                    title = item.holiday.title,
                    detail = if (item.holiday.dayCount == 1) {
                        "${item.holiday.start.pretty()} ${item.holiday.start.year}"
                    } else {
                        "${item.holiday.start.pretty()} to ${item.holiday.end.pretty()} ${item.holiday.end.year} (${item.holiday.dayCount} days)"
                    },
                    enabled = !state.added,
                    onToggle = { viewModel.toggleHoliday(item.holiday) },
                )
            }
        } else {
            state.exams.forEachIndexed { index, item ->
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surfaceContainerHighest),
                ) {
                    ReviewRow(
                        selected = item.selected,
                        title = "${item.exam.subject}: ${item.exam.title}",
                        detail = "${item.exam.date.pretty()} ${item.exam.date.year}" + (item.exam.room?.let { "  ·  Room $it" } ?: ""),
                        enabled = !state.added,
                        onToggle = { viewModel.toggleExam(index) },
                        plain = true,
                    )
                    if (item.selected && !state.added) {
                        InlineDropdown(
                            options = subjects.map { DropdownOption(it.id, it.name) },
                            selected = item.subjectId,
                            onSelect = { viewModel.setExamSubject(index, it) },
                            label = "Subject",
                            placeholder = "Choose the subject",
                            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                        )
                    }
                }
            }
            if (subjects.isEmpty()) {
                Text(
                    "Add your subjects first (Settings, Subjects) so each exam can be attached to one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }

        state.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = if (state.added) colors.primary else colors.error) }

        if (state.added) {
            PillButton(text = "Done", onClick = onDone, modifier = Modifier.fillMaxWidth())
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                PillButton(text = "Back", tonal = true, onClick = onBack, modifier = Modifier.weight(1f))
                PillButton(
                    text = "Add $selectedCount ${if (selectedCount == 1) state.kind.noun else state.kind.pluralNoun}",
                    loading = state.busy,
                    enabled = selectedCount > 0 && !missingSubject && !state.busy,
                    onClick = viewModel::addSelected,
                    modifier = Modifier.weight(1.6f),
                )
            }
            if (missingSubject) {
                Text("Choose a subject for every exam you want to add.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ReviewRow(selected: Boolean, title: String, detail: String, enabled: Boolean, onToggle: () -> Unit, plain: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .then(if (plain) Modifier else Modifier.background(colors.surfaceContainerHighest))
            .clickable(enabled = enabled, onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = selected, onCheckedChange = null, enabled = enabled, modifier = Modifier.padding(8.dp))
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}
