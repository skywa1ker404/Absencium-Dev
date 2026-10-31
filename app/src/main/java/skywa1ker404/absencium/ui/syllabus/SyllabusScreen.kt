package skywa1ker404.absencium.ui.syllabus

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import skywa1ker404.absencium.ui.main.ShellFab
import skywa1ker404.absencium.ui.theme.LocalBackdrop
import skywa1ker404.absencium.ui.components.AppMenu
import skywa1ker404.absencium.ui.components.AppMenuDivider
import skywa1ker404.absencium.ui.components.AppMenuItem
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import skywa1ker404.absencium.ui.components.Hue
import skywa1ker404.absencium.ui.components.hueFor
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import skywa1ker404.absencium.ui.components.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
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
fun SyllabusScreen(subjectId: Long, navigator: DestinationsNavigator, rootId: Long? = null) {
    val viewModel = containerViewModel { SyllabusViewModel(it, subjectId) }
    val nodes by viewModel.nodes.collectAsStateWithLifecycle()
    val counts by viewModel.linkCounts.collectAsStateWithLifecycle()
    val subjectName by viewModel.subjectName.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()

    var collapsed by remember { mutableStateOf(emptySet<Long>()) }
    var editing by remember { mutableStateOf<Editing?>(null) }
    var deleting by remember { mutableStateOf<SyllabusNodeEntity?>(null) }
    var showImport by remember { mutableStateOf(false) }

    // Opened from a unit card, the screen shows that unit's own tree and nothing else; the unit becomes the top of the tree.
    val scoped = remember(nodes, rootId) {
        if (rootId == null) {
            nodes
        } else {
            val inside = SyllabusTree.withDescendants(nodes, rootId).toSet()
            nodes.filter { it.id in inside }.map { if (it.id == rootId) it.copy(parentId = null) else it }
        }
    }
    LaunchedEffect(rootId, nodes.isEmpty(), scoped.isEmpty()) {
        // The unit was deleted from here: go back to the list.
        if (rootId != null && nodes.isNotEmpty() && scoped.isEmpty()) navigator.navigateUp()
    }
    val rows = remember(scoped, collapsed) { SyllabusTree.rows(scoped, collapsed) }
    val overall = remember(scoped) { SyllabusTree.progress(scoped, null) }
    val animatedOverall by animateFloatAsState(
        targetValue = overall,
        animationSpec = LocalQuality.current.spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "overall",
    )

    val pillScroll = skywa1ker404.absencium.ui.main.rememberPillScroll()

    ScreenScaffold(
        title = if (rootId == null) "Syllabus" else "Unit",
        onBack = { navigator.navigateUp() },
        modifier = Modifier.nestedScroll(pillScroll.connection),
        overlay = {
            // The two ways to add to the syllabus are buttons floating at the bottom right, like the main screen's.
            Box(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (rootId == null) {
                        ShellFab(
                            icon = AppIcons.AutoAwesome,
                            label = "Import outline",
                            onClick = { viewModel.dismissImport(); showImport = true },
                            backdrop = LocalBackdrop.current,
                            expanded = !pillScroll.compact.value,
                        )
                    }
                    ShellFab(
                        icon = AppIcons.Add,
                        label = if (rootId == null) "Add unit" else "Add chapter",
                        onClick = { editing = Editing.Add(rootId, SyllabusTree.levelName(if (rootId == null) 0 else 1)) },
                        backdrop = LocalBackdrop.current,
                        expanded = !pillScroll.compact.value,
                    )
                }
            }
        },
    ) {
        val headHue = hueFor(0)
        AppCard(modifier = Modifier.fillMaxWidth().bounceIn(0), container = headHue.container.copy(alpha = 0.75f)) {
            Text(
                if (rootId == null) subjectName.ifEmpty { "Subject" } else scoped.firstOrNull { it.id == rootId }?.title.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = headHue.onContainer,
            )
            if (scoped.isEmpty()) {
                Text("No syllabus yet", style = MaterialTheme.typography.bodyMedium, color = headHue.onContainer.copy(alpha = 0.8f))
            } else {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${(overall * 100).toInt()}%", style = MaterialTheme.typography.displaySmall, color = headHue.onContainer)
                    Text(
                        if (rootId == null) "of the syllabus studied" else "of this unit studied",
                        style = MaterialTheme.typography.titleMedium,
                        color = headHue.onContainer.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                LinearWavyProgressIndicator(
                    progress = { animatedOverall },
                    modifier = Modifier.fillMaxWidth(),
                    color = headHue.accent,
                    trackColor = headHue.accent.copy(alpha = 0.18f),
                )
            }
        }

        if (scoped.isEmpty()) {
            HintText(
                "Add the units of this subject, then the chapters and topics inside them. Notes and study material can be filed under " +
                    "any of them. You can also paste an outline or pick a file and let your AI turn it into a tree.",
            )
        } else {
            SectionTitle("Units, chapters and topics")
            HintText("Tap a circle to move a status on. On a unit or chapter it sets everything inside.")
        }

        // One card per unit; its chapters, topics and subtopics are rows inside it.
        val groups = remember(rows) {
            val out = ArrayList<MutableList<SyllabusTree.Row>>()
            rows.forEach { r -> if (r.depth == 0) out += mutableListOf(r) else out.lastOrNull()?.add(r) }
            out
        }
        groups.forEachIndexed { groupIndex, group ->
            androidx.compose.runtime.key(group.first().node.id) {
                val hue = hueFor((groupIndex + 1).toLong())
                AppCard(
                    modifier = Modifier.fillMaxWidth().bounceIn((groupIndex * 45).coerceAtMost(300)),
                    container = hue.container.copy(alpha = 0.75f),
                ) {
                    group.forEach { row ->
                        androidx.compose.runtime.key(row.node.id) {
                            SyllabusRow(
                                row = row,
                                hue = hue,
                                progress = SyllabusTree.progress(scoped, row.node.id),
                                linked = counts[row.node.id] ?: 0,
                                expanded = row.node.id !in collapsed,
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
            }
        }
        Spacer(Modifier.height(168.dp))
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
            onSources = viewModel::importSources,
            existing = nodes,
            subjectName = subjectName,
            onAccept = { items, chosen -> viewModel.insertOutline(items, chosen); showImport = false },
            onBack = viewModel::dismissImport,
            onClose = { viewModel.dismissImport(); showImport = false },
        )
    }
}

/**
 * One line of a unit's card: a round status mark, the title with its level and progress, a chevron when it has children, and a
 * menu. Deeper lines are indented and joined to their parent by a thin guide line.
 */
@Composable
private fun SyllabusRow(
    row: SyllabusTree.Row,
    hue: Hue,
    progress: Float,
    linked: Int,
    expanded: Boolean,
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
    val guide = hue.onContainer.copy(alpha = 0.2f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (row.depth * 12).dp)
            .then(
                if (row.depth > 0) {
                    Modifier.drawBehind {
                        drawLine(guide, Offset(1.dp.toPx(), 4.dp.toPx()), Offset(1.dp.toPx(), size.height - 4.dp.toPx()), strokeWidth = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    }
                } else {
                    Modifier
                },
            )
            .padding(start = if (row.depth > 0) 6.dp else 0.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = row.hasChildren, onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StatusMark(status = node.status, accent = hue.accent, onClick = onCycle)
        Column(modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
            // A long title stops at three lines; the whole of it is in Rename. Units are medium-sized, never headline-sized.
            Text(
                node.title,
                style = if (row.depth == 0) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                fontWeight = if (row.depth == 0) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = hue.onContainer,
            )
            val line = buildString {
                append(SyllabusTree.levelName(row.depth))
                if (row.hasChildren) append(" \u00b7 ${(progress * 100).toInt()}%")
                if (linked > 0) append(" \u00b7 $linked filed here")
            }
            Text(line, style = MaterialTheme.typography.labelMedium, color = hue.onContainer.copy(alpha = 0.7f))
            if (row.hasChildren) {
                val shown by animateFloatAsState(progress, LocalQuality.current.spring(), label = "rowProgress")
                Box(
                    Modifier.padding(top = 5.dp).fillMaxWidth().height(4.dp).clip(CircleShape).background(hue.accent.copy(alpha = 0.18f)),
                ) { Box(Modifier.fillMaxWidth(shown.coerceIn(0f, 1f)).height(4.dp).clip(CircleShape).background(hue.accent)) }
            }
            node.details?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis, color = hue.onContainer.copy(alpha = 0.75f), modifier = Modifier.padding(top = 2.dp))
            }
        }
        if (row.hasChildren) {
            val turn by animateFloatAsState(
                targetValue = if (expanded) 1f else 0f,
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                label = "chevron",
            )
            Icon(
                AppIcons.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = hue.onContainer,
                modifier = Modifier.size(20.dp).rotate(-90f * (1f - turn)),
            )
        }
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.size(32.dp)) { Icon(AppIcons.MoreVert, contentDescription = "More", tint = hue.onContainer, modifier = Modifier.size(20.dp)) }
            AppMenu(expanded = menu, onDismiss = { menu = false }) {
                if (row.depth < SyllabusTree.MAX_DEPTH) {
                    AppMenuItem(AppIcons.Add, "Add ${SyllabusTree.levelName(row.depth + 1).lowercase()}", { menu = false; onAddInside() })
                }
                AppMenuItem(AppIcons.Edit, "Rename", { menu = false; onRename() })
                AppMenuItem(AppIcons.Description, "Details", { menu = false; onDetails() })
                AppMenuDivider()
                AppMenuItem(AppIcons.ArrowUp, "Move up", { menu = false; onMoveUp() })
                AppMenuItem(AppIcons.ArrowDown, "Move down", { menu = false; onMoveDown() })
                AppMenuDivider()
                AppMenuItem(AppIcons.Delete, "Delete", { menu = false; onDelete() }, destructive = true)
            }
        }
    }
}

/**
 * The status of a node as a round mark: an empty ring (not started), a half-filled ring (in progress) or a filled ring with a
 * tick (done). Tapping it moves it on; the fill sweeps round and the mark pops with a spring.
 */
@Composable
private fun StatusMark(status: SyllabusStatus, accent: Color, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val fill by animateFloatAsState(
        targetValue = when (status) {
            SyllabusStatus.NOT_STARTED -> 0f
            SyllabusStatus.IN_PROGRESS -> 0.5f
            SyllabusStatus.COMPLETED -> 1f
        },
        animationSpec = LocalQuality.current.spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "statusFill",
    )
    val pop = remember { Animatable(1f) }
    LaunchedEffect(status) {
        pop.snapTo(0.78f)
        pop.animateTo(1f, spring(Spring.DampingRatioHighBouncy, Spring.StiffnessMedium))
    }
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .clickable(role = Role.Checkbox, onClickLabel = statusLabel(status), onClick = onClick)
            .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
            .semantics { contentDescription = statusLabel(status) },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(22.dp)) {
            val stroke = 2.5.dp.toPx()
            drawCircle(accent, radius = size.minDimension / 2f - stroke / 2f, style = Stroke(stroke))
            if (fill > 0f) {
                val inset = 4.dp.toPx()
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f * fill,
                    useCenter = true,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - 2 * inset, size.height - 2 * inset),
                )
            }
        }
        if (status == SyllabusStatus.COMPLETED) {
            Icon(AppIcons.Check, contentDescription = null, tint = colors.surface, modifier = Modifier.size(14.dp))
        }
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
    onSources: (android.net.Uri?, List<android.net.Uri>, String) -> Unit,
    existing: List<SyllabusNodeEntity>,
    subjectName: String,
    onAccept: (List<SyllabusTree.OutlineItem>, Set<Int>) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    // What was picked: one document and up to five pictures; either, or both, are read together.
    var doc by remember { mutableStateOf<android.net.Uri?>(null) }
    var pictures by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    val context = androidx.compose.ui.platform.LocalContext.current
    fun nameOf(uri: android.net.Uri): String = try {
        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    } catch (e: Exception) {
        null
    } ?: "Document"
    // After the outline is read the student picks what to add. What is already in the syllabus is never offered twice.
    val found = (state as? OutlineImport.Ready)?.items.orEmpty()
    val dups = remember(state, existing) { SyllabusTree.duplicates(existing, found, null) }
    var chosen by remember(state) { mutableStateOf(found.indices.filter { it !in dups }.toSet()) }
    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) doc = uri }
    val picturePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        pictures = (pictures + uris).distinct().take(SyllabusViewModel.MAX_PICTURES)
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Import an outline") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (state) {
                    OutlineImport.Idle -> {
                        Text(
                            "Write or paste one title per line. Indent a line (two spaces) to put it inside the one above. " +
                                "Or let your AI read one document and/or up to five pictures, with or without text.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        AppTextField(
                            value = text,
                            onValueChange = { text = it },
                            label = { Text("Syllabus text") },
                            minLines = 5,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        // One document and/or up to five pictures, read together by the AI.
                        val picked = doc
                        if (picked != null) {
                            PickedChip(icon = AppIcons.Description, text = nameOf(picked), onRemove = { doc = null })
                        } else {
                            PillButton(
                                text = "Choose a document",
                                tonal = true,
                                compact = true,
                                enabled = aiReady,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { docPicker.launch(arrayOf("application/pdf", "text/*", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) },
                            )
                        }
                        pictures.forEachIndexed { i, uri ->
                            PickedChip(icon = AppIcons.Image, text = "Picture ${i + 1}", onRemove = { pictures = pictures - uri })
                        }
                        if (pictures.size < SyllabusViewModel.MAX_PICTURES) {
                            PillButton(
                                text = if (pictures.isEmpty()) "Choose pictures (up to ${SyllabusViewModel.MAX_PICTURES})" else "Add more (${pictures.size} of ${SyllabusViewModel.MAX_PICTURES})",
                                tonal = true,
                                compact = true,
                                enabled = aiReady,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { picturePicker.launch("image/*") },
                            )
                        }
                        PillButton(
                            text = if (doc != null || pictures.isNotEmpty()) "Read with AI" else "Tidy the text with AI",
                            compact = true,
                            enabled = aiReady && (doc != null || pictures.isNotEmpty() || text.isNotBlank()),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onSources(doc, pictures, text) },
                        )
                        if (!aiReady) HintText("AI Features are off or not set up, so only \"Use as typed\" works. Turn them on in Settings, AI Features.")
                    }
                    is OutlineImport.Working -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(state.step, style = MaterialTheme.typography.bodyMedium)
                        // The upload is a wavy line that fills up; when nothing can be measured it waves on its own.
                        if (state.progress != null) {
                            LinearWavyProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                            Text("${(state.progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        Text(
                            "A large file can take a minute or two. If your phone cannot reach the AI service this ends with a message; Cancel stops it now.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    is OutlineImport.Ready -> {
                        val fresh = state.items.size - dups.size
                        Text(
                            "${state.items.size} items found for ${subjectName.ifEmpty { "this subject" }}" +
                                if (dups.isEmpty()) "." else ", $fresh new and ${dups.size} already in the syllabus.",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { chosen = state.items.indices.filter { it !in dups }.toSet() }) { Text("Select all") }
                            TextButton(onClick = { chosen = emptySet() }) { Text("Select none") }
                        }
                        Column(modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                            state.items.forEachIndexed { index, item ->
                                val isDup = index in dups
                                val on = isDup || index in chosen
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(enabled = !isDup) {
                                            val family = SyllabusTree.subtreeIndices(state.items, index).filter { it !in dups }
                                            chosen = if (index in chosen) {
                                                // Taking an item off takes everything inside it off too.
                                                chosen - family.toSet()
                                            } else {
                                                // Putting one on also puts on what it sits inside, so it has a place to go.
                                                var next = chosen + index
                                                var up = SyllabusTree.parentIndex(state.items, index)
                                                while (up != null) {
                                                    if (up !in dups) next = next + up
                                                    up = SyllabusTree.parentIndex(state.items, up)
                                                }
                                                next
                                            }
                                        }
                                        .padding(start = (item.depth * 16).dp, top = 2.dp, bottom = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    androidx.compose.material3.Checkbox(checked = on, onCheckedChange = null, enabled = !isDup)
                                    Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                                        Text(
                                            item.title,
                                            style = if (item.depth == 0) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                                            maxLines = 2,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        )
                                        if (isDup) Text("Already in the syllabus", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                    is OutlineImport.Problem -> Text(state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            when (state) {
                OutlineImport.Idle -> TextButton(enabled = text.isNotBlank(), onClick = { onTyped(text) }) { Text("Use as typed") }
                is OutlineImport.Ready -> TextButton(enabled = chosen.isNotEmpty(), onClick = { onAccept(state.items, chosen) }) { Text("Add ${chosen.size}") }
                is OutlineImport.Problem -> TextButton(onClick = onBack) { Text("Back") }
                is OutlineImport.Working -> Spacer(Modifier.width(1.dp))
            }
        },
        dismissButton = { TextButton(onClick = if (state is OutlineImport.Ready) onBack else onClose) { Text(if (state is OutlineImport.Ready) "Back" else "Cancel") } },
    )
}

/** A chosen document or picture with a way to take it off again. */
@Composable
private fun PickedChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onRemove: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().clip(CircleShape).background(colors.secondaryContainer).padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = colors.onSecondaryContainer, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = colors.onSecondaryContainer, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) { Icon(AppIcons.Close, contentDescription = "Remove", tint = colors.onSecondaryContainer, modifier = Modifier.size(18.dp)) }
    }
}
