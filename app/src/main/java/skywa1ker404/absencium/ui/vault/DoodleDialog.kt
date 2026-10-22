package skywa1ker404.absencium.ui.vault

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.vault.entry.ImageTools

/** One drawn line. Points and width are fractions of the page width/height, so the same doodle can be drawn at any size. */
private data class Ink(val points: List<Offset>, val color: Int, val width: Float, val erase: Boolean)

private enum class Paper { PLAIN, LINES, GRID }

private val InkColors = listOf(0xFF212121.toInt(), 0xFFE53935.toInt(), 0xFFFB8C00.toInt(), 0xFF43A047.toInt(), 0xFF1E88E5.toInt(), 0xFF8E24AA.toInt())
private const val PAGE = 0xFFFFFFFF.toInt()
private const val RULE = 0x33607D8B

/** A line from a fraction of the page to a pixel size, as a smooth path. */
private fun smooth(points: List<Offset>, w: Float, h: Float): AndroidPath {
    val path = AndroidPath()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x * w, points[0].y * h)
    if (points.size == 1) {
        path.lineTo(points[0].x * w + 0.01f, points[0].y * h)
        return path
    }
    for (i in 1 until points.size) {
        val a = points[i - 1]
        val b = points[i]
        path.quadTo(a.x * w, a.y * h, (a.x + b.x) / 2f * w, (a.y + b.y) / 2f * h)
    }
    path.lineTo(points.last().x * w, points.last().y * h)
    return path
}

private fun smoothCompose(points: List<Offset>, w: Float, h: Float): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x * w, points[0].y * h)
    if (points.size == 1) {
        path.lineTo(points[0].x * w + 0.01f, points[0].y * h)
        return path
    }
    for (i in 1 until points.size) {
        val a = points[i - 1]
        val b = points[i]
        path.quadraticTo(a.x * w, a.y * h, (a.x + b.x) / 2f * w, (a.y + b.y) / 2f * h)
    }
    path.lineTo(points.last().x * w, points.last().y * h)
    return path
}

/** The picture the doodle becomes: the page, the earlier picture, every line, then the paper rules. */
private fun render(base: Bitmap?, strokes: List<Ink>, paper: Paper, width: Int, height: Int): Bitmap {
    val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(out)
    canvas.drawColor(PAGE)
    if (base != null) canvas.drawBitmap(base, null, android.graphics.RectF(0f, 0f, width.toFloat(), height.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))
    val w = width.toFloat()
    val h = height.toFloat()
    for (s in strokes) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = s.width * w
            color = if (s.erase) PAGE else s.color
        }
        canvas.drawPath(smooth(s.points, w, h), paint)
    }
    if (paper != Paper.PLAIN) {
        val rule = Paint().apply { color = RULE; strokeWidth = 2f }
        val step = h / 22f
        var y = step
        while (y < h) { canvas.drawLine(0f, y, w, y, rule); y += step }
        if (paper == Paper.GRID) {
            var x = step
            while (x < w) { canvas.drawLine(x, 0f, x, h, rule); x += step }
        }
    }
    return out
}

/**
 * Full-screen drawing page. Pen and eraser, six colours, a size slider, undo and redo, and plain, lined or squared paper.
 * An earlier doodle ([existingPath]) is shown underneath so it can be drawn over. Done gives the finished picture to [onDone].
 */
@Composable
fun DoodleDialog(existingPath: String?, onCancel: () -> Unit, onDone: (Bitmap) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val base by produceState<Bitmap?>(null, existingPath) {
        value = if (existingPath == null) null else withContext(Dispatchers.IO) { ImageTools.decode(existingPath, 2048) }
    }
    val strokes = remember { mutableStateListOf<Ink>() }
    val undone = remember { mutableStateListOf<Ink>() }
    var live by remember { mutableStateOf<Ink?>(null) }
    var erasing by remember { mutableStateOf(false) }
    var color by remember { mutableStateOf(InkColors[0]) }
    var width by remember { mutableFloatStateOf(0.008f) }
    var paper by remember { mutableStateOf(Paper.PLAIN) }
    val ratio = base?.let { it.width.toFloat() / it.height.toFloat() } ?: (4f / 3f)

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().background(colors.surface).statusBarsPadding().navigationBarsPadding().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCancel) { Icon(AppIcons.Close, contentDescription = "Discard the doodle") }
                Box(modifier = Modifier.weight(1f))
                IconButton(onClick = { strokes.removeLastOrNull()?.let { undone += it } }, enabled = strokes.isNotEmpty()) {
                    Icon(AppIcons.Undo, contentDescription = "Undo")
                }
                IconButton(onClick = { undone.removeLastOrNull()?.let { strokes += it } }, enabled = undone.isNotEmpty()) {
                    Icon(AppIcons.Redo, contentDescription = "Redo")
                }
                IconButton(onClick = {
                    val longSide = 1600
                    val w = if (ratio >= 1f) longSide else (longSide * ratio).toInt().coerceAtLeast(200)
                    val h = if (ratio >= 1f) (longSide / ratio).toInt().coerceAtLeast(200) else longSide
                    onDone(render(base, strokes.toList(), paper, w, h))
                }) { Icon(AppIcons.Check, contentDescription = "Save the doodle") }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(ratio)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, colors.outlineVariant, RoundedCornerShape(16.dp))
                        .pointerInput(erasing, color, width) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                val w = size.width.toFloat().coerceAtLeast(1f)
                                val h = size.height.toFloat().coerceAtLeast(1f)
                                val points = ArrayList<Offset>()
                                points += Offset(down.position.x / w, down.position.y / h)
                                live = Ink(points.toList(), color, width, erasing)
                                do {
                                    val event = awaitPointerEvent()
                                    for (change in event.changes) {
                                        if (change.pressed) {
                                            points += Offset(change.position.x / w, change.position.y / h)
                                            change.consume()
                                        }
                                    }
                                    live = Ink(points.toList(), color, width, erasing)
                                } while (event.changes.any { it.pressed })
                                strokes += Ink(points.toList(), color, width, erasing)
                                undone.clear()
                                live = null
                            }
                        },
                ) {
                    base?.let { drawImage(it.asImageBitmap(), dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())) }
                    for (s in strokes) drawInk(s)
                    live?.let { drawInk(it) }
                    if (paper != Paper.PLAIN) {
                        val rule = Color(RULE)
                        val step = size.height / 22f
                        var y = step
                        while (y < size.height) { drawLine(rule, Offset(0f, y), Offset(size.width, y), strokeWidth = 2f); y += step }
                        if (paper == Paper.GRID) {
                            var x = step
                            while (x < size.width) { drawLine(rule, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2f); x += step }
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                RoundToolIcon("Pen", !erasing, { erasing = false }) { Icon(AppIcons.Brush, contentDescription = null, tint = if (!erasing) colors.onPrimaryContainer else colors.onSurface) }
                RoundToolIcon("Eraser", erasing, { erasing = true }) { EraserGlyph(if (erasing) colors.onPrimaryContainer else colors.onSurface) }
                RoundToolIcon("Paper", paper != Paper.PLAIN, {
                    paper = when (paper) { Paper.PLAIN -> Paper.LINES; Paper.LINES -> Paper.GRID; Paper.GRID -> Paper.PLAIN }
                }) { Icon(AppIcons.Table, contentDescription = null, tint = if (paper != Paper.PLAIN) colors.onPrimaryContainer else colors.onSurface) }
                RoundToolIcon("Clear", false, { if (strokes.isNotEmpty()) { undone.clear(); undone.addAll(strokes.reversed()); strokes.clear() } }) {
                    Icon(AppIcons.Delete, contentDescription = null, tint = colors.onSurface)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                InkColors.forEach { c ->
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(c))
                            .border(if (c == color && !erasing) 3.dp else 0.dp, colors.primary, CircleShape)
                            .clickable { color = c; erasing = false }
                            .semantics { contentDescription = "Colour" },
                    )
                }
            }
            Slider(value = width, onValueChange = { width = it }, valueRange = 0.003f..0.04f, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Line thickness" })
        }
    }
}

private fun DrawScope.drawInk(s: Ink) {
    val path = smoothCompose(s.points, size.width, size.height)
    drawPath(
        path = path,
        color = if (s.erase) Color.White else Color(s.color),
        style = DrawStroke(width = s.width * size.width, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

@Composable
private fun RoundToolIcon(label: String, selected: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(if (selected) colors.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** A small slanted rubber, drawn in place because the icon set has none. */
@Composable
private fun EraserGlyph(tint: Color) {
    Canvas(modifier = Modifier.size(24.dp)) {
        rotate(-35f) {
            val w = size.width * 0.62f
            val h = size.height * 0.42f
            val left = (size.width - w) / 2f
            val top = (size.height - h) / 2f
            drawRoundRect(tint, topLeft = Offset(left, top), size = androidx.compose.ui.geometry.Size(w, h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f), style = DrawStroke(width = 3f))
            drawLine(tint, Offset(left + w * 0.45f, top), Offset(left + w * 0.45f, top + h), strokeWidth = 3f)
        }
    }
}
