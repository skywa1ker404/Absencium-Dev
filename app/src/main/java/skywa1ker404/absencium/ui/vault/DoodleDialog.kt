package skywa1ker404.absencium.ui.vault

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.vault.entry.ImageTools

/** A pen. Pressure-sensitive nibs get thicker the harder the stylus (or finger) presses; the others keep one thickness. */
private enum class Nib(val description: String, val pressure: Boolean, val widthScale: Float, val alpha: Float, val round: Boolean) {
    PEN("Pen, pressure sensitive", true, 1f, 1f, true),
    LINER("Fine liner", false, 0.5f, 1f, true),
    MARKER("Marker", false, 2.2f, 0.6f, true),
    HIGHLIGHTER("Highlighter", false, 4.5f, 0.35f, false),
}

/** One drawn line. Points and width are fractions of the page width/height, so the doodle can be drawn at any size. */
private data class Ink(
    val points: List<Offset>,
    val pressures: List<Float>,
    val argb: Int,
    val opacity: Float,
    val width: Float,
    val nib: Nib,
    val erase: Boolean,
)

private enum class Paper { PLAIN, LINES, GRID }

/** The page: [background] is null for a transparent page. */
private data class PageSetup(val background: Int?, val width: Int, val height: Int)

private val InkGrid = listOf(
    0xFF000000, 0xFF424242, 0xFF9E9E9E, 0xFFFFFFFF, 0xFFB71C1C, 0xFFE53935, 0xFFFF7043, 0xFFFB8C00, 0xFFFDD835, 0xFFC0CA33,
    0xFF43A047, 0xFF00897B, 0xFF00ACC1, 0xFF1E88E5, 0xFF3949AB, 0xFF8E24AA, 0xFFD81B60, 0xFF6D4C41, 0xFF8D6E63, 0xFFFFCCBC,
).map { it.toInt() }

private val Backgrounds = listOf(0xFFFFFFFF, 0xFFFFF8E1, 0xFFE3F2FD, 0xFFE8F5E9, 0xFF212121, 0xFF000000).map { it.toInt() }

private data class SizePreset(val label: String, val width: Int, val height: Int)

private val Presets = listOf(
    SizePreset("Square", 1600, 1600),
    SizePreset("Landscape 4:3", 1600, 1200),
    SizePreset("Wide 16:9", 1600, 900),
    SizePreset("Portrait 3:4", 1200, 1600),
    SizePreset("A4", 1240, 1754),
    SizePreset("Phone screen", 1080, 1920),
)

private const val RULE = 0x44607D8B
private const val MAX_SIDE = 3000
private const val MIN_SIDE = 100

private fun smooth(points: List<Offset>, w: Float, h: Float): Path {
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

/** Draws one line. The same code paints the screen and the saved picture, so what you see is what is saved. */
private fun DrawScope.drawInk(s: Ink) {
    val w = size.width
    val h = size.height
    val base = s.width * w * s.nib.widthScale * (if (s.erase) 2f else 1f)
    val color = Color(s.argb).copy(alpha = s.opacity * (if (s.erase) 1f else s.nib.alpha))
    if (s.nib.pressure && !s.erase && s.points.size > 1) {
        for (i in 1 until s.points.size) {
            val pressure = (s.pressures[i - 1] + s.pressures[i]) / 2f
            drawLine(
                color = color,
                start = Offset(s.points[i - 1].x * w, s.points[i - 1].y * h),
                end = Offset(s.points[i].x * w, s.points[i].y * h),
                strokeWidth = base * (0.35f + 0.65f * pressure),
                cap = StrokeCap.Round,
            )
        }
    } else {
        drawPath(
            path = smooth(s.points, w, h),
            color = if (s.erase) Color.Black else color,
            style = DrawStroke(width = base, cap = if (s.nib.round || s.erase) StrokeCap.Round else StrokeCap.Butt, join = StrokeJoin.Round),
            blendMode = if (s.erase) BlendMode.Clear else BlendMode.SrcOver,
        )
    }
}

private fun DrawScope.drawRules(paper: Paper) {
    if (paper == Paper.PLAIN) return
    val rule = Color(RULE)
    val thickness = maxOf(1f, size.width / 700f)
    val step = size.height / 22f
    var y = step
    while (y < size.height) {
        drawLine(rule, Offset(0f, y), Offset(size.width, y), strokeWidth = thickness)
        y += step
    }
    if (paper == Paper.GRID) {
        var x = step
        while (x < size.width) {
            drawLine(rule, Offset(x, 0f), Offset(x, size.height), strokeWidth = thickness)
            x += step
        }
    }
}

/** The grey and white squares that stand for "nothing here". */
private fun DrawScope.drawChecker() {
    drawRect(Color(0xFFF2F2F2))
    val cell = maxOf(10f, size.width / 40f)
    var row = 0
    var y = 0f
    while (y < size.height) {
        var col = 0
        var x = 0f
        while (x < size.width) {
            if ((row + col) % 2 == 0) drawRect(Color(0xFFD9D9D9), Offset(x, y), Size(cell, cell))
            x += cell
            col++
        }
        y += cell
        row++
    }
}

/** The picture the doodle becomes: earlier picture and lines on a clear layer (the eraser clears it), then the page, rules and the layer. */
private fun render(base: Bitmap?, strokes: List<Ink>, page: PageSetup, paper: Paper): Bitmap {
    val w = page.width
    val h = page.height
    val size = Size(w.toFloat(), h.toFloat())
    val layer = ImageBitmap(w, h)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, ComposeCanvas(layer), size) {
        base?.let { drawImage(it.asImageBitmap(), dstSize = IntSize(w, h)) }
        strokes.forEach { drawInk(it) }
    }
    val out = ImageBitmap(w, h)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, ComposeCanvas(out), size) {
        page.background?.let { drawRect(Color(it)) }
        drawRules(paper)
        drawImage(layer)
    }
    return out.asAndroidBitmap()
}

/** An earlier doodle that was saved on a plain colour keeps that colour under the eraser; a transparent one stays transparent. */
private fun backgroundOf(bitmap: Bitmap): Int? {
    val corner = bitmap.getPixel(0, 0)
    return if ((corner ushr 24) == 0xFF) corner else null
}

/**
 * Full-screen drawing page. A new doodle starts by choosing its background (transparent or a colour) and size (a preset or your own).
 * Then: pen, fine liner, marker, highlighter, eraser, thickness, opacity, a colour grid and spectrum, undo and redo, and plain,
 * ruled or squared paper. An earlier doodle ([existingPath]) is shown underneath so it can be drawn over. Done gives the picture to [onDone].
 */
@Composable
fun DoodleDialog(existingPath: String?, onCancel: () -> Unit, onDone: (Bitmap) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val base by produceState<Bitmap?>(null, existingPath) {
        value = if (existingPath == null) null else withContext(Dispatchers.IO) { ImageTools.decode(existingPath, 2048) }
    }
    var chosen by remember { mutableStateOf<PageSetup?>(null) }
    val page: PageSetup? = chosen ?: if (existingPath != null) base?.let { PageSetup(backgroundOf(it), it.width.coerceAtLeast(1), it.height.coerceAtLeast(1)) } else null

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        if (page == null) {
            if (existingPath == null) {
                DoodleSetup(onStart = { chosen = it }, onCancel = onCancel)
            } else {
                Box(modifier = Modifier.fillMaxSize().background(colors.surface), contentAlignment = Alignment.Center) {
                    IconButton(onClick = onCancel) { Icon(AppIcons.Close, contentDescription = "Close") }
                }
            }
        } else {
            DoodlePage(base = base, page = page, onCancel = onCancel, onDone = onDone)
        }
    }
}

private enum class PageTab { BRUSH, COLOUR, PAPER }

@Composable
private fun DoodlePage(base: Bitmap?, page: PageSetup, onCancel: () -> Unit, onDone: (Bitmap) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val strokes = remember { mutableStateListOf<Ink>() }
    val undone = remember { mutableStateListOf<Ink>() }
    var live by remember { mutableStateOf<Ink?>(null) }
    var erasing by remember { mutableStateOf(false) }
    var nib by remember { mutableStateOf(Nib.PEN) }
    var argb by remember { mutableStateOf(InkGrid[0]) }
    var width by remember { mutableFloatStateOf(0.008f) }
    var opacity by remember { mutableFloatStateOf(1f) }
    var hue by remember { mutableFloatStateOf(0f) }
    var shade by remember { mutableFloatStateOf(0.5f) }
    var paper by remember { mutableStateOf(Paper.PLAIN) }
    var tab by remember { mutableStateOf(PageTab.BRUSH) }
    val ratio = page.width.toFloat() / page.height.toFloat()

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
            IconButton(onClick = { onDone(render(base, strokes.toList(), page, paper)) }) { Icon(AppIcons.Check, contentDescription = "Save the doodle") }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .aspectRatio(ratio, matchHeightConstraintsFirst = ratio < 1f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, colors.outlineVariant, RoundedCornerShape(16.dp)),
            ) {
                // The page under the lines: checkerboard for a transparent page, or its colour, then the paper rules.
                Canvas(modifier = Modifier.matchParentSize()) {
                    val bg = page.background
                    if (bg == null) drawChecker() else drawRect(Color(bg))
                    drawRules(paper)
                }
                // The lines on their own layer, so the eraser really clears.
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .pointerInput(erasing, argb, width, opacity, nib) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                val w = size.width.toFloat().coerceAtLeast(1f)
                                val h = size.height.toFloat().coerceAtLeast(1f)
                                val points = ArrayList<Offset>()
                                val pressures = ArrayList<Float>()
                                fun add(change: PointerInputChange) {
                                    points += Offset(change.position.x / w, change.position.y / h)
                                    pressures += change.pressure.coerceIn(0.05f, 1f)
                                }
                                fun make() = Ink(points.toList(), pressures.toList(), argb, opacity, width, nib, erasing)
                                add(down)
                                live = make()
                                do {
                                    val event = awaitPointerEvent()
                                    for (change in event.changes) {
                                        if (change.pressed) {
                                            add(change)
                                            change.consume()
                                        }
                                    }
                                    live = make()
                                } while (event.changes.any { it.pressed })
                                strokes += make()
                                undone.clear()
                                live = null
                            }
                        },
                ) {
                    base?.let { drawImage(it.asImageBitmap(), dstSize = IntSize(size.width.toInt(), size.height.toInt())) }
                    for (s in strokes) drawInk(s)
                    live?.let { drawInk(it) }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            PageTab.entries.forEach { t ->
                RoundToolIcon(
                    when (t) { PageTab.BRUSH -> "Pens"; PageTab.COLOUR -> "Colours"; PageTab.PAPER -> "Paper" },
                    tab == t,
                    { tab = t },
                ) {
                    val tint = if (tab == t) colors.onPrimaryContainer else colors.onSurface
                    when (t) {
                        PageTab.BRUSH -> Icon(AppIcons.Brush, contentDescription = null, tint = tint)
                        PageTab.COLOUR -> Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(Color(argb)).border(2.dp, tint, CircleShape))
                        PageTab.PAPER -> Icon(AppIcons.Table, contentDescription = null, tint = tint)
                    }
                }
            }
        }

        when (tab) {
            PageTab.BRUSH -> Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    Nib.entries.forEach { n ->
                        val selected = !erasing && nib == n
                        RoundToolIcon(n.description, selected, { erasing = false; nib = n }) {
                            Icon(
                                when (n) { Nib.PEN -> AppIcons.Brush; Nib.LINER -> AppIcons.PenLine; Nib.MARKER -> AppIcons.Edit; Nib.HIGHLIGHTER -> AppIcons.Highlight },
                                contentDescription = null,
                                tint = if (selected) colors.onPrimaryContainer else colors.onSurface,
                            )
                        }
                    }
                    RoundToolIcon("Eraser", erasing, { erasing = true }) {
                        Icon(AppIcons.Eraser, contentDescription = null, tint = if (erasing) colors.onPrimaryContainer else colors.onSurface)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.Brush, contentDescription = null, modifier = Modifier.size(20.dp), tint = colors.onSurfaceVariant)
                    Slider(value = width, onValueChange = { width = it }, valueRange = 0.003f..0.05f, modifier = Modifier.weight(1f).semantics { contentDescription = "Line thickness" })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.Opacity, contentDescription = null, modifier = Modifier.size(20.dp), tint = colors.onSurfaceVariant)
                    Slider(value = opacity, onValueChange = { opacity = it }, valueRange = 0.1f..1f, modifier = Modifier.weight(1f).semantics { contentDescription = "Opacity" })
                }
            }
            PageTab.COLOUR -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InkGrid.chunked(10).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        row.forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(c))
                                    .border(if (c == argb && !erasing) 3.dp else 1.dp, if (c == argb && !erasing) colors.primary else colors.outlineVariant, CircleShape)
                                    .clickable { argb = c; erasing = false }
                                    .semantics { contentDescription = "Colour" },
                            )
                        }
                    }
                }
                SpectrumBar(
                    brush = Brush.horizontalGradient(List(7) { Color.hsv(it * 60f, 1f, 1f) }),
                    fraction = hue / 360f,
                    label = "Colour spectrum",
                    thumb = Color.hsv(hue, 1f, 1f),
                ) { f ->
                    hue = (f * 360f).coerceIn(0f, 359.9f)
                    argb = shaded(hue, shade)
                    erasing = false
                }
                SpectrumBar(
                    brush = Brush.horizontalGradient(listOf(Color.Black, Color.hsv(hue, 1f, 1f), Color.White)),
                    fraction = shade,
                    label = "Light and dark",
                    thumb = Color(shaded(hue, shade)),
                ) { f ->
                    shade = f.coerceIn(0f, 1f)
                    argb = shaded(hue, shade)
                    erasing = false
                }
            }
            PageTab.PAPER -> Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Paper.entries.forEach { p ->
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surfaceContainerHigh)
                            .border(if (paper == p) 3.dp else 1.dp, if (paper == p) colors.primary else colors.outlineVariant, RoundedCornerShape(12.dp))
                            .clickable { paper = p }
                            .semantics { contentDescription = when (p) { Paper.PLAIN -> "Plain paper"; Paper.LINES -> "Ruled paper"; Paper.GRID -> "Squared paper" } },
                    ) {
                        Canvas(modifier = Modifier.matchParentSize().padding(6.dp)) {
                            drawRect(Color.White)
                            val line = Color(0xFF90A4AE)
                            if (p != Paper.PLAIN) {
                                for (i in 1..4) drawLine(line, Offset(0f, size.height * i / 5f), Offset(size.width, size.height * i / 5f), strokeWidth = 1.5f)
                                if (p == Paper.GRID) for (i in 1..4) drawLine(line, Offset(size.width * i / 5f, 0f), Offset(size.width * i / 5f, size.height), strokeWidth = 1.5f)
                            }
                        }
                    }
                }
                RoundToolIcon("Clear all lines", false, { if (strokes.isNotEmpty()) { undone.clear(); undone.addAll(strokes.reversed()); strokes.clear() } }) {
                    Icon(AppIcons.Delete, contentDescription = null, tint = colors.onSurface)
                }
            }
        }
    }
}

/** A colour from a hue and a light-to-dark position: left half goes black to the pure colour, right half goes to white. */
private fun shaded(hue: Float, shade: Float): Int =
    if (shade < 0.5f) Color.hsv(hue, 1f, (shade * 2f).coerceIn(0f, 1f)).toArgb()
    else Color.hsv(hue, (1f - (shade - 0.5f) * 2f).coerceIn(0f, 1f), 1f).toArgb()

/** A gradient bar you tap or drag; [onChange] gets a position from 0 to 1. */
@Composable
private fun SpectrumBar(brush: Brush, fraction: Float, label: String, thumb: Color, onChange: (Float) -> Unit) {
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(30.dp)) {
        val barWidth = maxWidth
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(brush)
                .semantics { contentDescription = label }
                .pointerInput(Unit) { detectTapGestures { onChange((it.x / size.width).coerceIn(0f, 1f)) } }
                .pointerInput(Unit) { detectHorizontalDragGestures { change, _ -> onChange((change.position.x / size.width).coerceIn(0f, 1f)) } },
        )
        val thumbSize = 26.dp
        Box(
            modifier = Modifier
                .offset(x = (barWidth - thumbSize) * fraction.coerceIn(0f, 1f) + 0.dp, y = 2.dp)
                .size(thumbSize)
                .clip(CircleShape)
                .background(thumb)
                .border(3.dp, colors.surface, CircleShape),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DoodleSetup(onStart: (PageSetup) -> Unit, onCancel: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var background by remember { mutableStateOf<Int?>(null) }
    var preset by remember { mutableStateOf<Int>(1) }
    var customWidth by remember { mutableStateOf("1600") }
    var customHeight by remember { mutableStateOf("1200") }
    val custom = preset == Presets.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancel) { Icon(AppIcons.Close, contentDescription = "Cancel") }
            Text("New doodle", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = {
                val size = if (custom) {
                    (customWidth.toIntOrNull() ?: 1600).coerceIn(MIN_SIDE, MAX_SIDE) to (customHeight.toIntOrNull() ?: 1200).coerceIn(MIN_SIDE, MAX_SIDE)
                } else {
                    Presets[preset].width to Presets[preset].height
                }
                onStart(PageSetup(background, size.first, size.second))
            }) { Icon(AppIcons.Check, contentDescription = "Start drawing") }
        }

        Text("Background", style = MaterialTheme.typography.titleSmall)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .border(if (background == null) 3.dp else 1.dp, if (background == null) colors.primary else colors.outlineVariant, CircleShape)
                    .clickable { background = null }
                    .semantics { contentDescription = "Transparent background" },
            ) { Canvas(modifier = Modifier.matchParentSize()) { drawChecker() } }
            Backgrounds.forEach { c ->
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(c))
                        .border(if (background == c) 3.dp else 1.dp, if (background == c) colors.primary else colors.outlineVariant, CircleShape)
                        .clickable { background = c }
                        .semantics { contentDescription = "Solid background" },
                )
            }
        }
        Text(
            if (background == null) "Transparent: the doodle can sit on top of the note's page." else "Solid: the doodle is a filled picture.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )

        Text("Size", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Presets.forEachIndexed { i, p ->
                FilterChip(selected = preset == i, onClick = { preset = i }, label = { Text(p.label) })
            }
            FilterChip(selected = custom, onClick = { preset = Presets.size }, label = { Text("Custom") })
        }
        if (custom) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = customWidth,
                    onValueChange = { customWidth = it.filter(Char::isDigit).take(4) },
                    label = { Text("Width (px)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = customHeight,
                    onValueChange = { customHeight = it.filter(Char::isDigit).take(4) },
                    label = { Text("Height (px)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
            Text("Between $MIN_SIDE and $MAX_SIDE pixels on each side.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
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
