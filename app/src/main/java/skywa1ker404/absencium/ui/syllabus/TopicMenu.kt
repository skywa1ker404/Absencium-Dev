package skywa1ker404.absencium.ui.syllabus

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.rerere.rikkahub.ui.components.chat.TopicNode
import me.rerere.rikkahub.ui.components.chat.TopicTree
import skywa1ker404.absencium.data.ai.SyllabusPath
import skywa1ker404.absencium.data.db.SyllabusNodeEntity
import skywa1ker404.absencium.ui.components.AlertDialog

/** The syllabus choices as the shared tree rows (the same list the AI's Vault link uses). */
fun topicNodes(choices: List<SyllabusPath.Choice>): List<TopicNode> =
    choices.map { TopicNode(it.id, it.label.substringAfterLast(" › "), it.depth) }

/**
 * THE one sheet for choosing a syllabus unit, chapter or topic: "File under a topic", the note's topic chip, the syllabus pick in
 * Add Entry and every other place a topic is chosen. The rows are the shared collapsed-by-default tree with an arrow at the left of
 * every unit and chapter. The tree area has a fixed height, so opening and closing rows never resizes the sheet.
 */
@Composable
fun TopicSheet(
    choices: List<SyllabusPath.Choice>,
    selectedId: Long?,
    noneLabel: String,
    title: String,
    onPick: (Long?) -> Unit,
    onDismiss: () -> Unit,
    onOpenSyllabus: (() -> Unit)? = null,
) {
    val nodes = remember(choices) { topicNodes(choices) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Box(modifier = Modifier.fillMaxWidth().height(360.dp).verticalScroll(rememberScrollState())) {
                if (nodes.isEmpty() && onOpenSyllabus != null) {
                    Text(
                        "This subject has no syllabus yet. Build it first, then file notes and study material under its units, chapters and topics.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    TopicTree(nodes = nodes, selectedId = selectedId, noneLabel = noneLabel, onPick = onPick)
                }
            }
        },
        confirmButton = {
            if (onOpenSyllabus != null) TextButton(onClick = onOpenSyllabus) { Text(if (nodes.isEmpty()) "Build the syllabus" else "Open the syllabus") }
            else TextButton(onClick = onDismiss) { Text("Close") }
        },
        dismissButton = if (onOpenSyllabus != null) ({ TextButton(onClick = onDismiss) { Text("Close") } }) else null,
    )
}

/** Kept for the note editor's topic chip: it now opens the same [TopicSheet] as everywhere else. */
@Composable
fun TopicMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    nodes: List<SyllabusNodeEntity>,
    selectedId: Long?,
    noneLabel: String,
    onPick: (Long?) -> Unit,
    onOpenSyllabus: () -> Unit,
) {
    if (!expanded) return
    TopicSheet(
        choices = remember(nodes) { SyllabusPath.choices(nodes) },
        selectedId = selectedId,
        noneLabel = noneLabel,
        title = "File under a topic",
        onPick = onPick,
        onDismiss = onDismiss,
        onOpenSyllabus = onOpenSyllabus,
    )
}
