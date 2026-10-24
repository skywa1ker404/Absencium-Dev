package skywa1ker404.absencium.ui.vault

import android.media.MediaPlayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
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

/**
 * What can be done to a picture or doodle on the page. Each one changes the note text or the file; the page redraws from that.
 * [onDrop] gets where the block was let go after a hold-and-drag: the height on screen and how far across the page (0 to 1).
 */
class EmbedActions(
    val onResize: (widthPercent: Int, align: Align) -> Unit,
    val onMove: (direction: Int) -> Unit,
    val onDrop: (rootY: Float, xFraction: Float) -> Unit,
    val onRemove: () -> Unit,
    val onView: () -> Unit,
    val onCrop: () -> Unit,
    val onDraw: () -> Unit,
)

/** A round handle on a corner of a selected block. Dragging it sideways resizes; [signX] is +1 for a right-hand corner, -1 for a left-hand one. */
@Composable
private fun BoxScope.CornerHandle(
    position: Alignment,
    signX: Int,
    centred: Boolean,
    rowWidth: Int,
    currentPercent: () -> Int,
    onPercent: (Int) -> Unit,
    onEnd: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .align(position)
            .padding(4.dp)
            .size(26.dp)
            .clip(CircleShape)
            .background(colors.primary)
            .border(2.dp, colors.surface, CircleShape)
            .semantics { contentDescription = "Drag to resize" }
            .pointerInput(rowWidth, centred) {
                var moved = 0f
                var start = 100
                detectDragGestures(
                    onDragStart = { moved = 0f; start = currentPercent() },
                    onDragEnd = { onEnd() },
                    onDragCancel = { onCancel() },
                ) { change, amount ->
                    change.consume()
                    moved += amount.x
                    // A centred block grows on both sides, so the edge under the finger moves half as far as the width changes.
                    val growth = signX * moved * (if (centred) 2f else 1f)
                    onPercent((start + growth / rowWidth.coerceAtLeast(1) * 100f).toInt().coerceIn(10, 100))
                }
            },
    )
}

/**
 * A picture, doodle or recording sitting on the page. Tap it for its menu. Hold it and drag to move it anywhere in the note (let go
 * over a line of text and it lands there; let go towards the left or right edge and it aligns that way). While it is selected,
 * round handles on its corners resize it. Size and alignment are kept in the note line, so the page and the PDF show the same thing.
 */
@Composable
fun EmbedBlock(ref: EmbedRef, attachment: AttachmentEntity?, version: Int, actions: EmbedActions, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    var menuOpen by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(false) }
    var rowWidth by remember { mutableIntStateOf(0) }
    var rowLeft by remember { mutableFloatStateOf(0f) }
    var dragPercent by remember { mutableIntStateOf(-1) }
    var drag by remember { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf(false) }
    var centre by remember { mutableStateOf(Offset.Zero) }
    var dragStartCentre by remember { mutableStateOf(Offset.Zero) }
    val percent = if (dragPercent > 0) dragPercent else ref.widthPercent
    val centred = ref.align == Align.CENTER

    // A selected block lets go of its handles after a few quiet seconds.
    LaunchedEffect(selected, menuOpen, dragPercent, dragging) {
        if (selected && !menuOpen && dragPercent < 0 && !dragging) {
            delay(5000)
            selected = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (dragging) 1f else 0f)
            .onSizeChanged { rowWidth = it.width }
            .onGloballyPositioned { rowLeft = it.positionInRoot().x }
            .graphicsLayer {
                translationX = drag.x
                translationY = drag.y
                val lift = if (dragging) 1.03f else 1f
                scaleX = lift
                scaleY = lift
                alpha = if (dragging) 0.92f else 1f
            },
        contentAlignment = when (ref.align) { Align.START -> Alignment.CenterStart; Align.CENTER -> Alignment.Center; Align.END -> Alignment.CenterEnd },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(percent / 100f)
                .onGloballyPositioned { c ->
                    val p = c.positionInRoot()
                    centre = Offset(p.x + c.size.width / 2f, p.y + c.size.height / 2f)
                }
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { dragStartCentre = centre; dragging = true; selected = true },
                        onDrag = { change, amount -> change.consume(); drag += amount },
                        onDragEnd = {
                            val landed = dragStartCentre + drag
                            drag = Offset.Zero
                            dragging = false
                            actions.onDrop(landed.y, ((landed.x - rowLeft) / rowWidth.coerceAtLeast(1)).coerceIn(0f, 1f))
                        },
                        onDragCancel = { drag = Offset.Zero; dragging = false },
                    )
                },
        ) {
            if (ref.kind == EmbedKind.AUDIO) {
                var player by remember { mutableStateOf<MediaPlayer?>(null) }
                var playing by remember { mutableStateOf(false) }
                DisposableEffect(attachment?.filePath) { onDispose { runCatching { player?.release() }; player = null } }
                val seconds = (attachment?.durationMs ?: 0L) / 1000
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surfaceContainerHighest)
                        .border(if (selected) 2.dp else 0.dp, colors.primary, RoundedCornerShape(16.dp))
                        .clickable(enabled = attachment != null) { selected = true; menuOpen = true }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        if (playing) AppIcons.Stop else AppIcons.PlayCircle,
                        contentDescription = if (playing) "Stop" else "Play the recording",
                        modifier = Modifier.clip(CircleShape).clickable(enabled = attachment != null) {
                            if (playing) {
                                runCatching { player?.release() }
                                player = null
                                playing = false
                            } else {
                                val file = attachment?.filePath ?: return@clickable
                                player = runCatching {
                                    MediaPlayer().apply {
                                        setDataSource(file)
                                        prepare()
                                        setOnCompletionListener { mp -> runCatching { mp.release() }; player = null; playing = false }
                                        start()
                                    }
                                }.getOrNull()
                                playing = player != null
                            }
                        },
                    )
                    Text(
                        if (attachment == null) "Recording not found" else "Recording  %d:%02d".format(seconds / 60, seconds % 60),
                        style = MaterialTheme.typography.bodyMedium,
                    )
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
            }
            if (selected && attachment != null && ref.kind != EmbedKind.AUDIO) {
                // The corner on the anchored side stays put, so only the free corners are offered.
                val showRight = ref.align != Align.END
                val showLeft = ref.align != Align.START
                fun handle(position: Alignment, signX: Int): @Composable BoxScope.() -> Unit = {
                    CornerHandle(
                        position = position,
                        signX = signX,
                        centred = centred,
                        rowWidth = rowWidth,
                        currentPercent = { ref.widthPercent },
                        onPercent = { dragPercent = it },
                        onEnd = {
                            if (dragPercent > 0) actions.onResize(dragPercent, ref.align)
                            dragPercent = -1
                        },
                        onCancel = { dragPercent = -1 },
                    )
                }
                if (showRight) {
                    handle(Alignment.TopEnd, 1)()
                    handle(Alignment.BottomEnd, 1)()
                }
                if (showLeft) {
                    handle(Alignment.TopStart, -1)()
                    handle(Alignment.BottomStart, -1)()
                }
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                @Composable
                fun item(label: String, onClick: () -> Unit) =
                    DropdownMenuItem(text = { Text(label) }, onClick = { menuOpen = false; selected = false; onClick() })
                if (ref.kind != EmbedKind.AUDIO) item("View full screen") { actions.onView() }
                item("Move up") { actions.onMove(-1) }
                item("Move down") { actions.onMove(1) }
                if (ref.kind != EmbedKind.AUDIO) {
                    item("Small") { actions.onResize(33, ref.align) }
                    item("Medium") { actions.onResize(60, ref.align) }
                    item("Large") { actions.onResize(85, ref.align) }
                    item("Full width") { actions.onResize(100, ref.align) }
                    item("Align left") { actions.onResize(ref.widthPercent, Align.START) }
                    item("Align centre") { actions.onResize(ref.widthPercent, Align.CENTER) }
                    item("Align right") { actions.onResize(ref.widthPercent, Align.END) }
                }
                if (ref.kind == EmbedKind.IMAGE) item("Edit: crop, rotate, flip") { actions.onCrop() }
                if (ref.kind == EmbedKind.DOODLE) item("Draw on it") { actions.onDraw() }
                item("Remove") { actions.onRemove() }
            }
        }
    }
}

/** What can be done to a table on the page. */
class TableActions(
    val onEdit: () -> Unit,
    /** Gives the table this many rows (the header counts as one); only empty rows are ever removed. */
    val onSetRows: (Int) -> Unit,
    val onAddColumn: () -> Unit,
    val onRemoveColumn: () -> Unit,
    val onMove: (direction: Int) -> Unit,
    /** Where a hold-and-drag was let go: the height on screen. */
    val onDrop: (rootY: Float) -> Unit,
    val onRemove: () -> Unit,
)

/**
 * A table on the page. Tap it for its menu (edit, add or remove rows and columns, move, remove). Hold it and drag to move it. The
 * round handle at its bottom corner adds rows when dragged down and removes the empty rows at the bottom when dragged up.
 */
@Composable
fun TableBlock(text: String, actions: TableActions, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val block = remember(text) { skywa1ker404.absencium.ui.vault.markdown.MarkdownParse.blocks(text).firstOrNull() as? Block.Table }
    val rowCount = remember(text) { TableGrid.parse(text).size }
    val rowCountNow by rememberUpdatedState(rowCount)
    var menuOpen by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(false) }
    var heightPx by remember { mutableIntStateOf(0) }
    var drag by remember { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf(false) }
    var centreY by remember { mutableFloatStateOf(0f) }
    var startCentreY by remember { mutableFloatStateOf(0f) }
    val shape = RoundedCornerShape(8.dp)

    LaunchedEffect(selected, menuOpen, dragging) {
        if (selected && !menuOpen && !dragging) {
            delay(5000)
            selected = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (dragging) 1f else 0f)
            .graphicsLayer {
                translationX = drag.x
                translationY = drag.y
                alpha = if (dragging) 0.92f else 1f
            }
            .onSizeChanged { heightPx = it.height }
            .onGloballyPositioned { c -> centreY = c.positionInRoot().y + c.size.height / 2f }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { startCentreY = centreY; dragging = true; selected = true },
                    onDrag = { change, amount -> change.consume(); drag += amount },
                    onDragEnd = {
                        val landed = startCentreY + drag.y
                        drag = Offset.Zero
                        dragging = false
                        actions.onDrop(landed)
                    },
                    onDragCancel = { drag = Offset.Zero; dragging = false },
                )
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .border(if (selected) 2.dp else 0.dp, colors.primary, shape)
                .clickable { selected = true; menuOpen = true },
        ) {
            if (block != null) PreviewTable(block) { } else Text(text, style = MaterialTheme.typography.bodySmall)
        }
        if (selected) {
            // Dragging the corner down by one row's height adds a row; up removes an empty bottom row.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.primary)
                    .border(2.dp, colors.surface, CircleShape)
                    .semantics { contentDescription = "Drag down to add rows, up to remove empty rows" }
                    .pointerInput(Unit) {
                        var moved = 0f
                        var start = 1
                        detectDragGestures(
                            onDragStart = { moved = 0f; start = rowCountNow },
                            onDragEnd = { },
                            onDragCancel = { },
                        ) { change, amount ->
                            change.consume()
                            moved += amount.y
                            val rowHeight = (heightPx.toFloat() / rowCountNow.coerceAtLeast(1)).coerceAtLeast(1f)
                            actions.onSetRows((start + (moved / rowHeight).roundToInt()).coerceIn(1, 50))
                        }
                    },
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            @Composable
            fun item(label: String, onClick: () -> Unit) =
                DropdownMenuItem(text = { Text(label) }, onClick = { menuOpen = false; selected = false; onClick() })
            item("Edit the table") { actions.onEdit() }
            item("Add a row") { actions.onSetRows(rowCount + 1) }
            item("Remove the last row if empty") { actions.onSetRows(rowCount - 1) }
            item("Add a column") { actions.onAddColumn() }
            item("Remove the last column") { actions.onRemoveColumn() }
            item("Move up") { actions.onMove(-1) }
            item("Move down") { actions.onMove(1) }
            item("Remove the table") { actions.onRemove() }
        }
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
