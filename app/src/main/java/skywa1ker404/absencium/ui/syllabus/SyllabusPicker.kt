package skywa1ker404.absencium.ui.syllabus

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import skywa1ker404.absencium.ui.components.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import skywa1ker404.absencium.data.ai.SyllabusPath
import skywa1ker404.absencium.data.db.SyllabusNodeEntity

/** What a note or material filed under node [id] is called: its place in the tree, or "Uncategorized". */
fun syllabusLabel(nodes: List<SyllabusNodeEntity>, id: Long?): String =
    if (id == null) "Uncategorized" else SyllabusPath.choices(nodes).firstOrNull { it.id == id }?.label ?: "Uncategorized"

/**
 * Chooses the unit, chapter or topic of a subject that a note or study material belongs to. [onPick] gets null for
 * "Uncategorized". [onOpenSyllabus] (when given) leads to the subject's syllabus, for a subject that has none yet.
 */
@Composable
fun SyllabusPickerDialog(
    nodes: List<SyllabusNodeEntity>,
    current: Long?,
    onPick: (Long?) -> Unit,
    onDismiss: () -> Unit,
    onOpenSyllabus: (() -> Unit)? = null,
    /** What the "no topic" row says: Uncategorized when filing, All topics when filtering. */
    noneLabel: String = "Uncategorized",
    title: String = "File under a topic",
) {
    val choices = remember(nodes) { SyllabusPath.choices(nodes) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (choices.isEmpty()) {
                Text("This subject has no syllabus yet. Build it first, then file notes and study material under its units, chapters and topics.", style = MaterialTheme.typography.bodyMedium)
            } else {
                Box(modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    SyllabusTreeRows(choices, current, noneLabel, onPick)
                }
            }
        },
        confirmButton = {
            if (choices.isEmpty() && onOpenSyllabus != null) TextButton(onClick = onOpenSyllabus) { Text("Open syllabus") }
            else TextButton(onClick = onDismiss) { Text("Close") }
        },
        dismissButton = if (choices.isEmpty() && onOpenSyllabus != null) ({ TextButton(onClick = onDismiss) { Text("Close") } }) else null,
    )
}

@Composable
private fun PickerRow(
    label: String,
    depth: Int,
    selected: Boolean,
    expandable: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
) {
    val turn by androidx.compose.animation.core.animateFloatAsState(if (expanded) 90f else 0f, androidx.compose.animation.core.spring(dampingRatio = 0.7f, stiffness = 500f), label = "arrow")
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = (depth * 16).dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        if (expandable) {
            androidx.compose.material3.IconButton(onClick = onToggle, modifier = Modifier.size(40.dp)) {
                androidx.compose.material3.Icon(
                    skywa1ker404.absencium.ui.icons.AppIcons.ChevronRight,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.graphicsLayer { rotationZ = turn },
                )
            }
        } else {
            Spacer(Modifier.size(40.dp))
        }
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * The rows of a syllabus as a tree, collapsed by default. [choices] come depth first (see [SyllabusPath.choices]); an arrow at the left
 * of a unit, chapter or topic opens what is under it, and any level can be picked. [selectedId] null means the "no topic" row.
 */
@Composable
fun SyllabusTreeRows(
    choices: List<SyllabusPath.Choice>,
    selectedId: Long?,
    noneLabel: String,
    onPick: (Long?) -> Unit,
) {
    // Each row's parent is the nearest earlier row one level up, because the list is depth first.
    val parents = remember(choices) {
        val out = HashMap<Long, Long?>()
        val stack = ArrayList<SyllabusPath.Choice>()
        choices.forEach { c ->
            while (stack.size > c.depth) stack.removeAt(stack.lastIndex)
            out[c.id] = stack.lastOrNull()?.id
            stack.add(c)
        }
        out
    }
    val withChildren = remember(parents) { parents.values.filterNotNull().toSet() }
    val expanded = remember { androidx.compose.runtime.mutableStateListOf<Long>() }
    val visible = choices.filter { c ->
        var parent = parents[c.id]
        var open = true
        while (parent != null) {
            if (parent !in expanded) { open = false; break }
            parent = parents[parent]
        }
        open
    }
    androidx.compose.foundation.layout.Column {
        PickerRow(noneLabel, 0, selectedId == null, false, false, {}) { onPick(null) }
        visible.forEach { c ->
            androidx.compose.runtime.key(c.id) {
                PickerRow(
                    label = c.label.substringAfterLast(" › "),
                    depth = c.depth,
                    selected = selectedId == c.id,
                    expandable = c.id in withChildren,
                    expanded = c.id in expanded,
                    onToggle = { if (c.id in expanded) expanded.remove(c.id) else expanded.add(c.id) },
                    onClick = { onPick(c.id) },
                )
            }
        }
    }
}

/** A field that shows the chosen topic and opens the collapsed syllabus tree under it (used where a dropdown list was). */
@Composable
fun SyllabusTreeField(
    choices: List<SyllabusPath.Choice>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    label: String,
    enabled: Boolean = true,
    noneLabel: String = "Not linked",
) {
    var open by remember { mutableStateOf(false) }
    val chosen = choices.firstOrNull { it.id == selectedId }?.label?.substringAfterLast(" › ") ?: noneLabel
    androidx.compose.foundation.layout.Column(modifier = Modifier.animateContentSize()) {
        androidx.compose.material3.OutlinedCard(
            onClick = { open = !open },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(chosen, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                }
                androidx.compose.material3.Icon(
                    skywa1ker404.absencium.ui.icons.AppIcons.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.graphicsLayer { rotationZ = if (open) 180f else 0f },
                )
            }
        }
        if (open) {
            Box(modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                SyllabusTreeRows(choices, selectedId, noneLabel) { onSelect(it); open = false }
            }
        }
    }
}
