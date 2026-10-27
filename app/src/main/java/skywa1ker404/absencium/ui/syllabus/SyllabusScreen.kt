package skywa1ker404.absencium.ui.syllabus

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import skywa1ker404.absencium.data.db.SyllabusNodeEntity
import skywa1ker404.absencium.data.db.SyllabusStatus
import skywa1ker404.absencium.data.syllabus.SyllabusTree
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.AppTextField
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.components.SectionTitle
import skywa1ker404.absencium.ui.components.bounceIn
import skywa1ker404.absencium.ui.containerViewModel
import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.theme.LocalQuality

/** What a text dialog is for: adding a node under a parent, renaming one, or writing its details. */
private sealed interface Editing {
    data class Add(val parentId: Long?, val level: String) : Editing
    data class Rename(val node: SyllabusNodeEntity) : Editing
    data class Details(val node: SyllabusNodeEntity) : Editing
}

private fun statusLabel(status: SyllabusStatus) = when (status) {
    SyllabusStatus.NOT_STARTED -> "Not started"
    SyllabusStatus.IN_PROGRESS -> "In progress"
    SyllabusStatus.COMPLETED -> "Done"
}

/** One subject's syllabus as a tree of units, chapters, topics and subtopics, with a status on each and progress above. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Destination<RootGraph>
@Composable
fun SyllabusScreen(subjectId: Long, navigator: DestinationsNavigator) {
    val viewModel = containerViewModel { SyllabusViewModel(it, subjectId) }
    val nodes by viewModel.nodes.collectAsStateWithLifecycle()
    val counts by viewModel.linkCounts.collectAsStateWithLifecycle()
    val subjectName by viewModel.subjectName.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()

    var collapsed by remember { mutableStateOf(emptySet<Long>()) }
    var editing by remember { mutableStateOf<Editing?>(null) }
    var deleting by remember { mutableStateOf<SyllabusNodeEntity?>(null) }
    var showImport by remember { mutableStateOf(false) }

    val rows = remember(nodes, collapsed) { SyllabusTree.rows(nodes, collapsed) }
    val overall = remember(nodes) { SyllabusTree.progress(nodes, null) }
    val animatedOverall by animateFloatAsState(
        targetValue = overall,
        animationSpec = LocalQuality.current.spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "overall",
    )

    ScreenScaffold(title = "Syllabus", onBack = { navigator.navigateUp() }) {
        AppCard(modifier = Modifier.fillMaxWidth().bounceIn(0)) {
            Text(subjectName.ifEmpty { "Subject" }, style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (nodes.isEmpty()) "No syllabus yet" else "${(overall * 100).toInt()}% of the syllabus studied",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (nodes.isNotEmpty()) {
                Spacer(Modifier.size(12.dp))
                LinearWavyProgressIndicator(progress = { animatedOverall }, modifier = Modifier.fillMaxWidth())
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton(text = "Add unit", onClick = { editing = Editing.Add(null, SyllabusTree.levelName(0)) })
            PillButton(text = "Import outline", tonal = true, onClick = { viewModel.dismissImport(); showImport = true })
        }

        if (nodes.isEmpty()) {
            HintText(
                "Add the units of this subject, then the chapters and topics inside them. Notes and study material can be filed under " +
                    "any of them. You can also paste an outline or pick a file and let your AI turn it into a tree.",
            )
        } else {
            SectionTitle("Units, chapters and topics")
            HintText("Tap a status to move it on. Tapping it on a unit or chapter sets everything inside it.")
        }

        rows.forEachIndexed { index, row ->
            androidx.compose.runtime.key(row.node.id) {
                SyllabusRow(
                    row = row,
                    progress = SyllabusTree.progress(nodes, row.node.id),
                    linked = counts[row.node.id] ?: 0,
                    expanded = row.node.id !in collapsed,
                    delayMs = (index * 25).coerceAtMost(300),
                    onToggle = { collapsed = if (row.node.id in collapsed) collapsed - row.node.id else collapsed + row.node.id },
                    onCycle = { viewModel.cycleStatus(row.node.id) },
                    onAddInside = { editing = Editing.Add(row.node.id, SyllabusTree.levelName(row.depth + 1)) },
                    onRename = { editing = Editing.Rename(row.node) },
                    onDetails = { editing = Editing.Details(row.node) },
                    onMoveUp = { viewModel.move(row.node.id, up = true) },
                    onMoveDown = { viewModel.move(row.node.id, up = false) },
                    onDelete = { deleting = row.node },
                )
            }
        }
    }

    editing?.let { current ->
        TextDialog(
            title = when (current) {
                is Editing.Add -> "Add ${current.level.lowercase()}"
                is Editing.Rename -> "Rename"
                is Editing.Details -> "Details"
            },
            initial = when (current) {
                is Editing.Add -> ""
                is Editing.Rename -> current.node.title
                is Editing.Details -> current.node.details.orEmpty()
            },
            label = if (current is Editing.Details) "What this covers (optional)" else "Title",
            multiline = current is Editing.Details,
            allowBlank = current is Editing.Details,
            onDismiss = { editing = null },
            onSave = { text ->
                when (current) {
                    is Editing.Add -> viewModel.add(current.parentId, text)
                    is Editing.Rename -> viewModel.rename(current.node.id, text)
                    is Editing.Details -> viewModel.setDetails(current.node.id, text)
                }
                if (current is Editing.Add) current.parentId?.let { collapsed = collapsed - it }
                editing = null
            },
        )
    }

    deleting?.let { node ->
        val inside = SyllabusTree.withDescendants(nodes, node.id).size - 1
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete \"${node.title}\"?") },
            text = {
                Text(
                    (if (inside > 0) "Everything inside it ($inside ${if (inside == 1) "item" else "items"}) goes too. " else "") +
                        "Notes and study material filed there are kept and go back to Uncategorized.",
                )
            },
            confirmButton = { TextButton(onClick = { viewModel.delete(node.id); deleting = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Keep") } },
        )
    }

    if (showImport) {
        ImportDialog(
            state = importState,
            aiReady = viewModel.aiReady(),
            onTyped = viewModel::importTyped,
            onWithAi = viewModel::importWithAi,
            onFile = viewModel::importFile,
            onAccept = { items -> viewModel.insertOutline(items); showImport = false },
            onBack = viewModel::dismissImport,
            onClose = { viewModel.dismissImport(); showImport = false },
        )
    }
}

@Composable
private fun SyllabusRow(
    row: SyllabusTree.Row,
    progress: Float,
    linked: Int,
    expanded: Boolean,
    delayMs: Int,
    onToggle: () -> Unit,
    onCycle: () -> Unit,
    onAddInside: () -> Unit,
    onRename: () -> Unit,
    onDetails: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    val node = row.node
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (row.depth * 14).dp)
            .bounceIn(delayMs),
    ) {
        AppCard(modifier = Modifier.weight(1f), onClick = if (row.hasChildren) onToggle else null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (row.hasChildren) {
                    val turn by animateFloatAsState(
                        targetValue = if (expanded) 1f else 0f,
                        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                        label = "chevron",
                    )
                    Icon(
                        AppIcons.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.size(22.dp).rotate(-90f * (1f - turn)),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(node.title, style = if (row.depth == 0) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall)
                    val line = buildString {
                        append(SyllabusTree.levelName(row.depth))
                        if (row.hasChildren) append(" · ${(progress * 100).toInt()}%")
                        if (linked > 0) append(" · $linked filed here")
                    }
                    Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    node.details?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 3, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                StatusPill(status = node.status, onClick = onCycle)
                Box {
                    IconButton(onClick = { menu = true }) { Icon(AppIcons.MoreVert, contentDescription = "More") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        if (row.depth < SyllabusTree.MAX_DEPTH) {
                            DropdownMenuItem(text = { Text("Add ${SyllabusTree.levelName(row.depth + 1).lowercase()}") }, onClick = { menu = false; onAddInside() })
                        }
                        DropdownMenuItem(text = { Text("Rename") }, onClick = { menu = false; onRename() })
                        DropdownMenuItem(text = { Text("Details") }, onClick = { menu = false; onDetails() })
                        DropdownMenuItem(text = { Text("Move up") }, onClick = { menu = false; onMoveUp() })
                        DropdownMenuItem(text = { Text("Move down") }, onClick = { menu = false; onMoveDown() })
                        DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
                    }
                }
            }
        }
    }
}

/** The status of a node. Tapping it moves it on; the colour and size spring to the new state. */
@Composable
private fun StatusPill(status: SyllabusStatus, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val target = when (status) {
        SyllabusStatus.NOT_STARTED -> colors.surfaceContainerHighest
        SyllabusStatus.IN_PROGRESS -> colors.tertiaryContainer
        SyllabusStatus.COMPLETED -> colors.primaryContainer
    }
    val container by animateColorAsState(target, animationSpec = LocalQuality.current.spring(), label = "status")
    // A small spring pop each time the status changes.
    val pop = remember { Animatable(1f) }
    LaunchedEffect(status) {
        pop.snapTo(0.8f)
        pop.animateTo(1f, spring(Spring.DampingRatioHighBouncy, Spring.StiffnessMedium))
    }
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = container,
        modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value },
    ) {
        Text(
            text = statusLabel(status),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun TextDialog(
    title: String,
    initial: String,
    label: String,
    multiline: Boolean,
    allowBlank: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            AppTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = !multiline,
                minLines = if (multiline) 3 else 1,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(enabled = allowBlank || text.isNotBlank(), onClick = { onSave(text) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Brings in a whole outline at once: typed or pasted text, or a document or photo read with the student's AI. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ImportDialog(
    state: OutlineImport,
    aiReady: Boolean,
    onTyped: (String) -> Unit,
    onWithAi: (String) -> Unit,
    onFile: (android.net.Uri) -> Unit,
    onAccept: (List<SyllabusTree.OutlineItem>) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) onFile(uri) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Import an outline") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (state) {
                    OutlineImport.Idle -> {
                        Text(
                            "Write or paste one title per line. Indent a line (two spaces) to put it inside the one above. " +
                                "Or let your AI tidy a messy page, or read a PDF, photo or document.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        AppTextField(
                            value = text,
                            onValueChange = { text = it },
                            label = { Text("Syllabus text") },
                            minLines = 5,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (!aiReady) HintText("AI Features are off or not set up, so only \"Use as typed\" works. Turn them on in Settings, AI Features.")
                    }
                    is OutlineImport.Working -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LoadingIndicator(modifier = Modifier.size(40.dp))
                        Text(state.step, style = MaterialTheme.typography.bodyMedium)
                    }
                    is OutlineImport.Ready -> {
                        Text("${state.items.size} items found:", style = MaterialTheme.typography.titleSmall)
                        state.items.take(14).forEach { item ->
                            Text(
                                item.title,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = (item.depth * 14).dp),
                                maxLines = 1,
                            )
                        }
                        if (state.items.size > 14) HintText("and ${state.items.size - 14} more")
                    }
                    is OutlineImport.Problem -> Text(state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            when (state) {
                OutlineImport.Idle -> Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(enabled = aiReady, onClick = { picker.launch(arrayOf("application/pdf", "text/*", "image/*", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) }) { Text("File") }
                    TextButton(enabled = aiReady && text.isNotBlank(), onClick = { onWithAi(text) }) { Text("Tidy with AI") }
                    TextButton(enabled = text.isNotBlank(), onClick = { onTyped(text) }) { Text("Use as typed") }
                }
                is OutlineImport.Ready -> TextButton(onClick = { onAccept(state.items) }) { Text("Add ${state.items.size}") }
                is OutlineImport.Problem -> TextButton(onClick = onBack) { Text("Back") }
                is OutlineImport.Working -> Spacer(Modifier.width(1.dp))
            }
        },
        dismissButton = { TextButton(onClick = if (state is OutlineImport.Ready) onBack else onClose) { Text(if (state is OutlineImport.Ready) "Back" else "Cancel") } },
    )
}
