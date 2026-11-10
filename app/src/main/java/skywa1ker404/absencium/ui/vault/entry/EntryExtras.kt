package skywa1ker404.absencium.ui.vault.entry

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import skywa1ker404.absencium.ui.components.AppMenu
import skywa1ker404.absencium.ui.components.AppTextField
import skywa1ker404.absencium.ui.components.DropdownOption
import skywa1ker404.absencium.ui.components.ExpressiveChip
import skywa1ker404.absencium.ui.components.InlineDropdown
import skywa1ker404.absencium.ui.components.SwitchCard
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.icons.AppIcons

private fun clock(ms: Long): String {
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}

/** Audio Note: record, listen back is in the note itself; this page records and saves. */
@Composable
internal fun AudioStage(viewModel: AddEntryViewModel, subjectName: String, onBack: () -> Unit, onSaved: (Long) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    var elapsed by remember { mutableLongStateOf(0L) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.startRecording() else viewModel.micDenied()
    }
    LaunchedEffect(state.recording) {
        while (state.recording) {
            elapsed = viewModel.recordingElapsed()
            delay(250)
        }
    }

    Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SquareIconButton(AppIcons.ArrowBack, "Back", onClick = {
                viewModel.discardRecording()
                onBack()
            })
            Text("Audio Note", style = MaterialTheme.typography.titleLarge)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(colors.tertiaryContainer)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                clock(if (state.recording) elapsed else state.recordedMs),
                style = MaterialTheme.typography.displaySmall,
                maxLines = 1,
                color = colors.onTertiaryContainer,
            )
            Text(
                when {
                    state.recording -> "Recording…"
                    state.recordedPath != null -> "Ready to save"
                    else -> "Tap Record and start talking"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onTertiaryContainer,
                maxLines = 1,
            )
        }
        AppTextField(
            value = state.title,
            onValueChange = viewModel::setTitle,
            label = { Text("Title (optional)") },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        SwitchCard(
            "Turn it into text with AI",
            "Adds the words to the note under the recording (needs a Gemini model)",
            state.transcribe,
            viewModel::setTranscribe,
            enabled = !state.recording,
        )
        state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.error) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            if (state.recording) {
                PillButton(text = "Stop", onClick = { viewModel.stopRecording() }, modifier = Modifier.weight(1f))
            } else {
                PillButton(
                    text = if (state.recordedPath != null) "Redo" else "Record",
                    tonal = state.recordedPath != null,
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            viewModel.startRecording()
                        } else {
                            permission.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
                PillButton(
                    text = "Save",
                    enabled = state.recordedPath != null,
                    loading = state.busy,
                    onClick = { viewModel.saveRecording(subjectName, onSaved) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Solve Homework (type the question and/or add photos) and Generate AI Notes (type a topic). The AI answer becomes a new entry. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AiTaskStage(
    viewModel: AddEntryViewModel,
    solve: Boolean,
    subjectName: String,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    onOpenAiSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris -> viewModel.addPictures(uris) }

    Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SquareIconButton(AppIcons.ArrowBack, "Back", onClick = {
                if (!state.aiRunning) onBack()
            })
            Text(if (solve) "Solve Homework" else "Generate AI Notes", style = MaterialTheme.typography.titleLarge)
        }
        AppTextField(
            value = state.aiPrompt,
            onValueChange = viewModel::setAiPrompt,
            label = { Text(if (solve) "Type the question (or add a photo of it)" else "What should the notes be about?") },
            enabled = !state.aiRunning,
            trailingIcon = { skywa1ker404.absencium.ui.components.VoiceInputButton(onText = { viewModel.setAiPrompt((state.aiPrompt + " " + it).trim()) }, enabled = !state.aiRunning) },
            minLines = 3,
            maxLines = 8,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        val choices by viewModel.syllabusChoices.collectAsStateWithLifecycle()
        val idle = !state.aiRunning
        if (solve) {
            PillButton(text = "Add photos of the homework", tonal = true, enabled = idle, onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth())
            if (state.pictures.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.pictures, key = { it.id }) { picture ->
                        PhotoThumb(path = picture.path, onClick = {}, onRemove = { viewModel.removePicture(picture.id) })
                    }
                }
            }
        }
        if (!solve) {
            // One size setting, and its words change with the kind of notes: length for a full note, detail for key
            // details, the time you have for last-minute preparation, and so on. What goes into the notes is decided
            // per topic by the planning step, so there are no switches for tables, comparisons or questions.
            val template = state.noteTemplate
            OptionGroup(title = "Kind of notes", icon = AppIcons.Description, help = template.guide) {
                InlineDropdown(
                    options = skywa1ker404.absencium.data.ai.NoteTemplate.entries.map { DropdownOption(it, it.label) },
                    selected = template,
                    onSelect = viewModel::setNoteTemplate,
                    label = "Kind of notes",
                    enabled = idle,
                )
                val depths = template.depths
                val picked = depths.getOrElse(state.noteDepth) { depths.first() }
                Text(template.depthTitle, style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                ChoiceBar(
                    options = depths.indices.toList(),
                    selected = depths.indexOf(picked),
                    label = { depths[it].label },
                    onSelect = viewModel::setNoteDepth,
                    enabled = idle,
                )
                Text(picked.guide.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
        val sources by viewModel.imageSources.collectAsStateWithLifecycle()
        // Hidden until at least one source is switched on in Settings, then Absencium AI.
        if (sources.search || sources.generate) {
            OptionGroup(
                title = "Images",
                icon = AppIcons.Image,
                help = when {
                    state.genSearchDiagrams && state.genDrawDiagrams -> "Searches first, then generates what it cannot find"
                    state.genSearchDiagrams -> "Free pictures found online (experimental); only search words are sent"
                    state.genDrawDiagrams -> "Generated by your AI plan (paid Gemini or OpenAI)"
                    else -> "None"
                },
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (sources.search) {
                        ExpressiveChip(selected = state.genSearchDiagrams, onClick = { viewModel.setGenSearchDiagrams(!state.genSearchDiagrams) }, label = { Text("Search online") }, enabled = idle)
                    }
                    if (sources.generate) {
                        ExpressiveChip(selected = state.genDrawDiagrams, onClick = { viewModel.setGenDrawDiagrams(!state.genDrawDiagrams) }, label = { Text("Generate with AI") }, enabled = idle)
                    }
                }
            }
        }
        val courseOptions by viewModel.courseOptions.collectAsStateWithLifecycle()
        OptionGroup(title = "Your course", icon = AppIcons.Edit, help = "Keeps the answer inside what you are taught") {
            if (choices.isNotEmpty()) {
                InlineDropdown(
                    options = listOf(DropdownOption(-1L, "Not linked")) + choices.map { DropdownOption(it.id, it.label) },
                    selected = state.syllabusNodeId ?: -1L,
                    onSelect = { viewModel.setSyllabusNode(it.takeIf { id -> id >= 0 }) },
                    label = "Syllabus topic",
                    enabled = idle,
                    maxListHeight = 320.dp,
                )
            }
            CourseField(
                value = state.courseInfo,
                options = courseOptions,
                onChange = viewModel::setCourseInfo,
                enabled = idle,
            )
        }
        if (state.aiRunning) LoadingDots(state.aiStep.ifBlank { if (solve) "Working on the answer" else "Writing your notes" })
        state.aiError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.error) }
        if (state.aiSetupMissing2) {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(colors.secondaryContainer).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("AI is off or not set up", style = MaterialTheme.typography.titleSmall, color = colors.onSecondaryContainer)
                Text(
                    "Add an AI provider, a model and your API key in Settings, then Absencium AI, then come back.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSecondaryContainer,
                )
                PillButton(text = "Open Settings", compact = true, onClick = onOpenAiSettings)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            if (state.aiRunning) {
                PillButton(text = "Cancel", tonal = true, onClick = { viewModel.cancelAiTask() }, modifier = Modifier.weight(1f))
            } else {
                PillButton(text = "Back", tonal = true, onClick = onBack, modifier = Modifier.weight(1f))
                PillButton(
                    text = if (solve) "Solve" else "Generate",
                    onClick = { viewModel.runAiTask(solve, subjectName, onSaved) },
                    modifier = Modifier.weight(2f),
                )
            }
        }
    }
}


/**
 * The course, year or semester. It starts as the course and year from the Profile; the arrow lists the Profile's values to pick
 * from, the cross clears it, and anything can be typed instead. Left blank, the notes do not assume a course.
 */
@Composable
private fun CourseField(value: String, options: List<String>, onChange: (String) -> Unit, enabled: Boolean) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        AppTextField(
            value = value,
            onValueChange = onChange,
            label = { Text("Course, year or semester") },
            placeholder = { Text("Optional") },
            enabled = enabled,
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (value.isNotEmpty()) {
                        IconButton(onClick = { onChange("") }, enabled = enabled) { Icon(AppIcons.Close, contentDescription = "Clear") }
                    }
                    if (options.isNotEmpty()) {
                        IconButton(onClick = { open = true }, enabled = enabled) { Icon(AppIcons.ExpandMore, contentDescription = "Choose from your profile") }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        AppMenu(expanded = open, onDismiss = { open = false }, solid = true) {
            Text("From your profile", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp))
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onChange(option)
                        open = false
                    },
                )
            }
        }
    }
}
