package skywa1ker404.absencium.ui.vault

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import skywa1ker404.absencium.data.db.AttachmentEntity
import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.vault.entry.rememberPhoto
import skywa1ker404.absencium.ui.vault.markdown.Align
import skywa1ker404.absencium.ui.vault.markdown.Block
import skywa1ker404.absencium.ui.vault.markdown.EmbedKind
import skywa1ker404.absencium.ui.vault.markdown.TableGrid

/** What an embed line says: `{{image:12|60|center}}`. */
data class EmbedRef(val kind: EmbedKind, val id: Long, val widthPercent: Int, val align: Align) {
    companion object {
        private val pattern = Regex("^\\{\\{(image|doodle|audio):(\\d{1,18})(?:\\|(\\d{1,3}))?(?:\\|(start|center|end))?\\}\\}$")

        fun parse(line: String): EmbedRef? {
            val m = pattern.find(line.trim()) ?: return null
            return EmbedRef(
                kind = EmbedKind.valueOf(m.groupValues[1].uppercase()),
                id = m.groupValues[2].toLongOrNull() ?: return null,
                widthPercent = m.groupValues[3].toIntOrNull()?.coerceIn(10, 100) ?: 100,
                align = when (m.groupValues[4]) { "start" -> Align.START; "end" -> Align.END; else -> Align.CENTER },
            )
        }
    }
}

/** What can be done to a picture or doodle on the page. Each one changes the note text or the file; the page redraws from that. */
class EmbedActions(
    val onResize: (widthPercent: Int, align: Align) -> Unit,
    val onMove: (direction: Int) -> Unit,
    val onRemove: () -> Unit,
    val onView: () -> Unit,
    val onCrop: () -> Unit,
    val onDraw: () -> Unit,
)

/**
 * A picture or doodle sitting on the page. Tapping it opens its menu; while it is selected a round handle at its corner
 * resizes it by dragging. Size and alignment are kept in the note line, so the page and the PDF show the same thing.
 */
@Composable
fun EmbedBlock(ref: EmbedRef, attachment: AttachmentEntity?, version: Int, actions: EmbedActions) {
    val colors = MaterialTheme.colorScheme
    var menuOpen by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(false) }
    var rowWidth by remember { mutableIntStateOf(0) }
    var dragPercent by remember { mutableIntStateOf(-1) }
    val density = LocalDensity.current
    val percent = if (dragPercent > 0) dragPercent else ref.widthPercent

    Box(
        modifier = Modifier.fillMaxWidth().onSizeChanged { rowWidth = it.width },
        contentAlignment = when (ref.align) { Align.START -> Alignment.CenterStart; Align.CENTER -> Alignment.Center; Align.END -> Alignment.CenterEnd },
    ) {
        Box(modifier = Modifier.fillMaxWidth(percent / 100f)) {
            if (ref.kind == EmbedKind.AUDIO) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surfaceContainerHighest).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(AppIcons.Mic, contentDescription = null)
                    Text("Recording", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                val bitmap by rememberPhoto(attachment?.filePath ?: "", 1600, version)
                val shape = RoundedCornerShape(14.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(colors.surfaceContainerHighest)
                        .border(if (selected) 2.dp else 0.dp, colors.primary, shape)
                        .clickable { selected = true; menuOpen = true },
                ) {
                    val image = bitmap
                    if (image != null) {
                        Image(bitmap = image, contentDescription = if (ref.kind == EmbedKind.DOODLE) "Doodle" else "Picture", modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
                    } else {
                        Text(
                            if (attachment == null) "This picture is no longer in the note." else "Loading…",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp),
                        )
                    }
                }
                if (selected && attachment != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(colors.primary)
                            .semantics { contentDescription = "Drag to resize" }
                            .pointerInput(rowWidth, ref.widthPercent) {
                                var start = ref.widthPercent
                                var moved = 0f
                                detectDragGestures(
                                    onDragStart = { start = ref.widthPercent; moved = 0f },
                                    onDragEnd = {
                                        if (dragPercent > 0) actions.onResize(dragPercent, ref.align)
                                        dragPercent = -1
                                    },
                                    onDragCancel = { dragPercent = -1 },
                                ) { change, amount ->
                                    change.consume()
                                    moved += amount.x
                                    val whole = rowWidth.coerceAtLeast(1)
                                    // The handle sits on the picture's far edge: dragging towards the middle makes it smaller.
                                    val sign = if (ref.align == Align.END) -1 else 1
                                    dragPercent = (start + sign * moved / whole * 100f).toInt().coerceIn(10, 100)
                                }
                            },
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false; selected = false }) {
                    fun item(label: String, onClick: () -> Unit) =
                        DropdownMenuItem(text = { Text(label) }, onClick = { menuOpen = false; selected = false; onClick() })
                    item("View full screen") { actions.onView() }
                    item("Move up") { actions.onMove(-1) }
                    item("Move down") { actions.onMove(1) }
                    item("Small") { actions.onResize(33, ref.align) }
                    item("Medium") { actions.onResize(60, ref.align) }
                    item("Large") { actions.onResize(85, ref.align) }
                    item("Full width") { actions.onResize(100, ref.align) }
                    item("Align left") { actions.onResize(ref.widthPercent, Align.START) }
                    item("Align centre") { actions.onResize(ref.widthPercent, Align.CENTER) }
                    item("Align right") { actions.onResize(ref.widthPercent, Align.END) }
                    if (ref.kind == EmbedKind.IMAGE) item("Crop and rotate") { actions.onCrop() }
                    if (ref.kind == EmbedKind.DOODLE) item("Draw on it") { actions.onDraw() }
                    item("Remove") { actions.onRemove() }
                }
            }
        }
    }
}

/** A table on the page. Tapping it opens the table editor. */
@Composable
fun TableBlock(text: String, onEdit: () -> Unit) {
    val block = remember(text) { skywa1ker404.absencium.ui.vault.markdown.MarkdownParse.blocks(text).firstOrNull() as? Block.Table }
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onEdit)) {
        if (block != null) PreviewTable(block) { } else Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

/** Edits a table cell by cell, with rows and columns added or removed. Done gives the new table lines to [onDone]. */
@Composable
fun TableEditorDialog(text: String, onCancel: () -> Unit, onDone: (String) -> Unit, onDeleteTable: () -> Unit) {
    var rows by remember { mutableStateOf(TableGrid.parse(text)) }
    val width = rows.maxOf { it.size }

    AlertDialog(
        onDismissRequest = onCancel,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Table") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                rows.forEachIndexed { r, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEachIndexed { c, cell ->
                            OutlinedTextField(
                                value = cell,
                                onValueChange = { v ->
                                    rows = rows.mapIndexed { ri, rr -> if (ri == r) rr.mapIndexed { ci, cc -> if (ci == c) v else cc } else rr }
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f).widthIn(min = 72.dp),
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { rows = rows + listOf(List(width) { "" }) }) { Text("Add row") }
                    TextButton(onClick = { rows = rows.map { it + "" } }) { Text("Add column") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { if (rows.size > 1) rows = rows.dropLast(1) }, enabled = rows.size > 1) { Text("Remove last row") }
                    TextButton(onClick = { if (width > 1) rows = rows.map { it.dropLast(1) } }, enabled = width > 1) { Text("Remove last column") }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onDone(TableGrid.build(rows)) }) { Text("Done") } },
        dismissButton = {
            Row {
                TextButton(onClick = onDeleteTable) { Text("Delete table") }
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
        },
    )
}
