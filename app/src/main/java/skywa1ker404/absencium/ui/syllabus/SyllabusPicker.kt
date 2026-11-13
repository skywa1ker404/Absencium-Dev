package skywa1ker404.absencium.ui.syllabus

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
    // Everything starts collapsed; an arrow at the left of a unit, chapter or topic opens what is under it, and any level can be picked.
    val parentOf = remember(nodes) { nodes.associate { it.id to it.parentId } }
    val hasChildren = remember(nodes) { nodes.mapNotNull { it.parentId }.toSet() }
    val expanded = remember { androidx.compose.runtime.mutableStateListOf<Long>() }
    val visible = choices.filter { choice ->
        var parent = parentOf[choice.id]
        var open = true
        while (parent != null) {
            if (parent !in expanded) { open = false; break }
            parent = parentOf[parent]
        }
        open
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (choices.isEmpty()) {
                Text("This subject has no syllabus yet. Build it first, then file notes and study material under its units, chapters and topics.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    item(key = "none") { PickerRow(noneLabel, 0, current == null, false, false, {}) { onPick(null) } }
                    items(visible, key = { it.id }) { choice ->
                        // The label holds the whole path; only its last part is shown, indented by its depth.
                        PickerRow(
                            label = choice.label.substringAfterLast(" › "),
                            depth = choice.depth,
                            selected = current == choice.id,
                            expandable = choice.id in hasChildren,
                            expanded = choice.id in expanded,
                            onToggle = { if (choice.id in expanded) expanded.remove(choice.id) else expanded.add(choice.id) },
                            onClick = { onPick(choice.id) },
                        )
                    }
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
