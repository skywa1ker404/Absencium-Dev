package skywa1ker404.absencium.ui.aihub

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import skywa1ker404.absencium.data.ai.AiChat
import skywa1ker404.absencium.data.ai.AssistantProfile
import skywa1ker404.absencium.data.ai.MemoryItem
import skywa1ker404.absencium.ui.components.AlertDialog
import skywa1ker404.absencium.ui.components.AppTextField
import skywa1ker404.absencium.ui.components.ExpressiveChip
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.components.sheetContainerColor
import skywa1ker404.absencium.ui.components.sheetScrimColor
import skywa1ker404.absencium.ui.icons.AppIcons

/** What the "+" button offers: files to send, the modes of this chat, ready-made study prompts, memory and the assistant. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlusSheet(
    chat: AiChat,
    canSearch: Boolean,
    memoryOn: Boolean,
    memoryCount: Int,
    assistantName: String,
    onPhotos: () -> Unit,
    onFiles: () -> Unit,
    onNotes: () -> Unit,
    onWeb: (Boolean) -> Unit,
    onThinking: (Boolean) -> Unit,
    onStudy: (Boolean) -> Unit,
    onLevel: (String) -> Unit,
    onPrompt: (String) -> Unit,
    onMemory: () -> Unit,
    onAssistants: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = sheetContainerColor(),
        scrimColor = sheetScrimColor(),
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Tile(AppIcons.Image, "Photos", Modifier.weight(1f)) { onDismiss(); onPhotos() }
                Tile(AppIcons.Description, "Files", Modifier.weight(1f)) { onDismiss(); onFiles() }
                Tile(AppIcons.Book, "Notes", Modifier.weight(1f)) { onDismiss(); onNotes() }
            }
            HintText("Pictures, PDFs, text files, recordings and short videos (recordings and videos need a Gemini model). Notes and study material you pick here are sent to the AI. It can also ask to read one itself; you approve each read.")
            ToggleRow(AppIcons.Search, "Web search", if (canSearch) "Looks things up and shows its sources (OpenRouter charges credits for it)" else "Needs a Gemini, Claude or OpenRouter model", chat.webSearch && canSearch, canSearch, onWeb)
            ToggleRow(AppIcons.Lightbulb, "Thinking", "Reasons for longer before answering (Gemini, Claude, OpenAI reasoning models)", chat.thinking, true, onThinking)
            ToggleRow(AppIcons.School, "Study mode", "Teaches step by step and checks you understood", chat.study, true, onStudy)
            if (chat.study) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("" to "Auto", "beginner" to "Beginner", "school" to "School", "college" to "College", "expert" to "Expert").forEach { (id, label) ->
                        ExpressiveChip(selected = chat.level == id, onClick = { onLevel(id) }, label = { Text(label) })
                    }
                }
            }
            LinkRow(AppIcons.Person, "Assistant", assistantName.ifBlank { "Default" }) { onDismiss(); onAssistants() }
            LinkRow(AppIcons.Bookmark, "Memory", if (memoryOn) "$memoryCount remembered" else "Off") { onDismiss(); onMemory() }
            Text("Try", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    "Quiz me" to "Make a 10-question quiz with answers and explanations from the attached notes or files: ",
                    "Summarise" to "Summarise the attached note or file clearly, with the key points and what to revise: ",
                    "Improve my note" to "Rewrite and improve the attached note: fix mistakes, organise it with headings, and keep everything correct: ",
                    "Revision plan" to "Make a day-by-day revision plan from my syllabus and exam dates for: ",
                    "Explain simply" to "Explain this simply, with one example: ",
                ).forEach { (label, text) -> ExpressiveChip(selected = false, onClick = { onDismiss(); onPrompt(text) }, label = { Text(label) }) }
            }
        }
    }
}

@Composable
private fun Tile(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.clip(RoundedCornerShape(24.dp)).background(colors.surfaceContainerHigh).clickable(onClick = onClick).padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = colors.onSurface)
        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable(enabled = enabled) { onChange(!checked) }.padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) colors.onSurface else colors.onSurfaceVariant)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = if (enabled) colors.onSurface else colors.onSurfaceVariant)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun LinkRow(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = colors.onSurface)
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        Icon(AppIcons.ChevronRight, contentDescription = null, tint = colors.onSurfaceVariant)
    }
}

/** A note or a study material file the student can hand to the AI. */
data class PickItem(val id: Long, val title: String, val subjectId: Long)

/**
 * Picks notes and study material to send with the next question, laid out like the Vault but compact: a search pill, two tabs, and a
 * folder row per subject that opens into one thin row per item. Only what is ticked here is ever sent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentPickerDialog(
    subjects: List<Pair<Long, String>>,
    notes: List<PickItem>,
    materials: List<PickItem>,
    onAttach: (notes: Set<Long>, materials: Set<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val pickedNotes = remember { mutableStateListOf<Long>() }
    val pickedMaterials = remember { mutableStateListOf<Long>() }
    val opened = remember { mutableStateListOf<Long>() }
    var query by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(0) }
    val items = if (tab == 0) notes else materials
    val picked = if (tab == 0) pickedNotes else pickedMaterials
    val q = query.trim()
    val groups = remember(items, subjects, q) {
        subjects.mapNotNull { (id, name) ->
            val list = items.filter { it.subjectId == id && (q.isEmpty() || it.title.contains(q, true) || name.contains(q, true)) }
            if (list.isEmpty()) null else Triple(id, name, list)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Send to the AI", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(40.dp).clip(CircleShape).background(colors.surfaceContainerHighest).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(AppIcons.Search, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) Text("Search", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        androidx.compose.foundation.text.BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.primary),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExpressiveChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("Notes${if (pickedNotes.isEmpty()) "" else " · ${pickedNotes.size}"}", style = MaterialTheme.typography.labelMedium, maxLines = 1) }, shape = CircleShape)
                    ExpressiveChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("Study material${if (pickedMaterials.isEmpty()) "" else " · ${pickedMaterials.size}"}", style = MaterialTheme.typography.labelMedium, maxLines = 1) }, shape = CircleShape)
                }
                if (groups.isEmpty()) {
                    Text(
                        if (q.isNotEmpty()) "Nothing matches." else if (tab == 0) "You have no notes yet." else "You have no study material yet.",
                        style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalMinimumInteractiveComponentSize provides 0.dp) {
                    Column(Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState())) {
                        groups.forEach { (subjectId, name, list) ->
                            val open = q.isNotEmpty() || subjectId in opened
                            val count = list.count { it.id in picked }
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { if (subjectId in opened) opened.remove(subjectId) else opened.add(subjectId) }.padding(horizontal = 4.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                androidx.compose.material3.TriStateCheckbox(
                                    state = when {
                                        count == 0 -> androidx.compose.ui.state.ToggleableState.Off
                                        count == list.size -> androidx.compose.ui.state.ToggleableState.On
                                        else -> androidx.compose.ui.state.ToggleableState.Indeterminate
                                    },
                                    onClick = { if (count == list.size) list.forEach { picked.remove(it.id) } else list.forEach { if (it.id !in picked) picked.add(it.id) } },
                                )
                                Icon(AppIcons.Folder, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Text("$count/${list.size}", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                                skywa1ker404.absencium.ui.components.ExpandChevron(expanded = open)
                            }
                            skywa1ker404.absencium.ui.components.Reveal(visible = open) {
                                Column(Modifier.padding(start = 22.dp)) {
                                    list.forEach { item ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { if (item.id in picked) picked.remove(item.id) else picked.add(item.id) }.padding(horizontal = 4.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Checkbox(checked = item.id in picked, onCheckedChange = { if (it) picked.add(item.id) else picked.remove(item.id) })
                                            Text(item.title.ifBlank { "Untitled" }, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Text("Only what you tick is sent, with your next question.", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
        },
        confirmButton = {
            val total = pickedNotes.size + pickedMaterials.size
            TextButton(onClick = { onAttach(pickedNotes.toSet(), pickedMaterials.toSet()) }, enabled = total > 0) { Text("Attach $total") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Everything Absencium AI remembers across chats: switch it on or off, add, edit and delete. */
@Composable
fun MemoryDialog(
    items: List<MemoryItem>,
    enabled: Boolean,
    chatId: Long,
    onEnabled: (Boolean) -> Unit,
    onAdd: (String, Boolean) -> Unit,
    onUpdate: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var editing by remember { mutableStateOf<MemoryItem?>(null) }
    var adding by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Memory") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).heightIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Remember across chats", style = MaterialTheme.typography.titleSmall)
                        Text("Optional. Kept on this phone; sent to your AI provider only as part of a question.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                    Switch(checked = enabled, onCheckedChange = onEnabled)
                }
                if (items.isEmpty()) HintText("Nothing remembered yet. With memory on, the AI saves lasting facts such as your course and how you like answers, and you can add your own.")
                items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surfaceContainerHigh).padding(start = 14.dp, top = 4.dp, bottom = 4.dp, end = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f).clickable { editing = item }) {
                            Text(item.text, style = MaterialTheme.typography.bodyMedium)
                            Text(if (item.chatId == null) "All chats" else if (item.chatId == chatId) "This chat only" else "One chat only", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                        }
                        IconButton(onClick = { onDelete(item.id) }) { Icon(AppIcons.Delete, contentDescription = "Forget", tint = colors.onSurfaceVariant) }
                    }
                }
                PillButton(text = "Add a memory", onClick = { adding = true }, tonal = true, compact = true)
                if (items.isNotEmpty()) TextButton(onClick = { confirmClear = true }) { Text("Forget everything") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
    if (adding) {
        var text by remember { mutableStateOf("") }
        var chatOnly by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { adding = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Add a memory") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppTextField(value = text, onValueChange = { text = it.take(400) }, placeholder = { Text("e.g. I study BSc Physics, second year") }, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Only for this chat", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = chatOnly, onCheckedChange = { chatOnly = it })
                    }
                }
            },
            confirmButton = { TextButton(onClick = { adding = false; onAdd(text, chatOnly) }, enabled = text.isNotBlank()) { Text("Save") } },
            dismissButton = { TextButton(onClick = { adding = false }) { Text("Cancel") } },
        )
    }
    editing?.let { item ->
        var text by remember(item.id) { mutableStateOf(item.text) }
        AlertDialog(
            onDismissRequest = { editing = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Edit memory") },
            text = { AppTextField(value = text, onValueChange = { text = it.take(400) }, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { editing = null; onUpdate(item.id, text) }, enabled = text.isNotBlank()) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Forget everything?") },
            text = { Text("All memories are deleted from this phone.") },
            confirmButton = { TextButton(onClick = { confirmClear = false; onClear() }) { Text("Forget") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Keep") } },
        )
    }
}

/** Assistants: reusable custom instructions (with study, research and writing presets), importable and exportable. */
@Composable
fun AssistantsDialog(
    profiles: List<AssistantProfile>,
    currentId: String,
    onPick: (AssistantProfile?) -> Unit,
    onSave: (AssistantProfile) -> Unit,
    onDelete: (String) -> Unit,
    onExport: () -> String,
    onImport: (String) -> Int,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    var editing by remember { mutableStateOf<AssistantProfile?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val text = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } }.getOrNull()
            val count = if (text == null) -1 else onImport(text)
            message = if (count < 0) "That file could not be read." else if (count == 0) "No assistants found in that file." else "Added $count."
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Assistants") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).heightIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HintText("An assistant gives this chat its own instructions (and optionally a model and temperature).")
                AssistantRow("Default", "Plain Absencium AI", currentId.isBlank(), onClick = { onPick(null) })
                profiles.forEach { profile ->
                    AssistantRow(
                        profile.name, profile.instructions, profile.id == currentId,
                        onClick = { onPick(profile) },
                        onEdit = { editing = if (profile.builtIn) profile.copy(id = java.util.UUID.randomUUID().toString(), name = profile.name + " copy", builtIn = false) else profile },
                        editLabel = if (profile.builtIn) "Copy to edit" else "Edit",
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillButton(text = "New", onClick = { editing = AssistantProfile(java.util.UUID.randomUUID().toString(), "", "") }, tonal = true, compact = true)
                    PillButton(text = "Import", onClick = { importer.launch(arrayOf("application/json", "text/*", "application/octet-stream")) }, tonal = true, compact = true)
                    PillButton(
                        text = "Export",
                        onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply { type = "application/json"; putExtra(Intent.EXTRA_TEXT, onExport()); putExtra(Intent.EXTRA_SUBJECT, "Absencium assistants") }
                            context.startActivity(Intent.createChooser(send, "Export assistants").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        },
                        tonal = true, compact = true,
                    )
                }
                message?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = colors.primary) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
    editing?.let { profile ->
        var name by remember(profile.id) { mutableStateOf(profile.name) }
        var instructions by remember(profile.id) { mutableStateOf(profile.instructions) }
        var temperature by remember(profile.id) { mutableStateOf(profile.temperature?.toString().orEmpty()) }
        val isNew = profiles.none { it.id == profile.id }
        AlertDialog(
            onDismissRequest = { editing = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text(if (isNew) "New assistant" else "Edit assistant") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppTextField(value = name, onValueChange = { name = it.take(60) }, label = { Text("Name") }, singleLine = true, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth())
                    AppTextField(value = instructions, onValueChange = { instructions = it.take(4000) }, label = { Text("Instructions") }, minLines = 4, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth())
                    AppTextField(value = temperature, onValueChange = { temperature = it.take(4) }, label = { Text("Temperature 0 to 2 (optional)") }, singleLine = true, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth())
                    if (!isNew) TextButton(onClick = { editing = null; onDelete(profile.id) }) { Text("Delete this assistant", color = colors.error) }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { editing = null; onSave(profile.copy(name = name.trim(), instructions = instructions.trim(), temperature = temperature.toDoubleOrNull()?.coerceIn(0.0, 2.0))) },
                    enabled = name.isNotBlank() && instructions.isNotBlank(),
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun AssistantRow(name: String, text: String, selected: Boolean, onClick: () -> Unit, onEdit: (() -> Unit)? = null, editLabel: String = "Edit") {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if (selected) colors.secondaryContainer else colors.surfaceContainerHigh).clickable(onClick = onClick).padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleSmall, color = if (selected) colors.onSecondaryContainer else colors.onSurface)
            Text(text, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis, color = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant)
        }
        if (onEdit != null) TextButton(onClick = onEdit) { Text(editLabel) }
    }
}

/** Where to save an answer as a note: the subjects to choose from. */
@Composable
fun SubjectPickerDialog(subjects: List<Pair<Long, String>>, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Save as a note in") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).heightIn(max = 360.dp)) {
                if (subjects.isEmpty()) HintText("Add a subject first (Home, Subjects).")
                subjects.forEach { (id, name) ->
                    Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onPick(id) }.padding(horizontal = 12.dp, vertical = 14.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
