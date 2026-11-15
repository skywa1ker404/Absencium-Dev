package skywa1ker404.absencium.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import kotlin.math.roundToInt
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import me.rerere.rikkahub.ui.components.dots.LocalDotIcons
import skywa1ker404.absencium.data.settings.AppStyle
import skywa1ker404.absencium.ui.theme.LocalAppStyle
import skywa1ker404.absencium.ui.theme.LocalQuality
import skywa1ker404.absencium.ui.theme.PixelCornerShape
import skywa1ker404.absencium.ui.theme.pixelBevel
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/*
 * The app's switch, slider, progress bars and spinners. In the Default and Liquid Glass styles they are Material's own; in Pixelated
 * they are square-edged blocks, and in Dot matrix the bars and spinners are rows and grids of round dots.
 */

@Composable
private fun isPixelStyle(): Boolean = LocalAppStyle.current == AppStyle.PIXELATED

@Composable
private fun isDotStyle(): Boolean = LocalDotIcons.current

/** Material's Switch, or a square-thumbed block switch in the Pixelated style. [onCheckedChange] null means the parent handles the tap. */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (!isPixelStyle()) {
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier, enabled = enabled)
        return
    }
    val c = MaterialTheme.colorScheme
    val q = LocalQuality.current
    val position by animateFloatAsState(if (checked) 1f else 0f, q.spring(), label = "pixelSwitch")
    val shape = PixelCornerShape(2f)
    val toggle = if (onCheckedChange != null) {
        Modifier.toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .size(width = 52.dp, height = 28.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.38f }
            .then(toggle)
            .clip(shape)
            .background(if (checked) c.primary else c.surfaceContainerHighest)
            .pixelBevel(c.outline),
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset((4.dp + 24.dp * position).roundToPx(), 4.dp.roundToPx()) }
                .size(20.dp)
                .clip(shape)
                .background(if (checked) c.onPrimary else c.outline)
                .pixelBevel(c.outline),
        )
    }
}

/** Material's Slider, or in the Pixelated style a segmented bar with a square block thumb. */
@Composable
fun AppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    if (!isPixelStyle()) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            valueRange = valueRange,
            steps = steps,
            onValueChangeFinished = onValueChangeFinished,
        )
        return
    }
    // Drawn by hand: a block bar with a square thumb, dragged or tapped. Steps snap to equal stops.
    val c = MaterialTheme.colorScheme
    val shape = PixelCornerShape(2f)
    val start = valueRange.start
    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(1e-6f)
    val fraction = ((value - start) / span).coerceIn(0f, 1f)
    var widthPx by remember { mutableFloatStateOf(1f) }
    val currentChange by rememberUpdatedState(onValueChange)
    val currentFinished by rememberUpdatedState(onValueChangeFinished)
    fun valueAt(x: Float): Float {
        var f = (x / widthPx).coerceIn(0f, 1f)
        if (steps > 0) f = (f * (steps + 1)).roundToInt() / (steps + 1f)
        return start + f * span
    }
    val ink = if (enabled) c.primary else c.onSurface.copy(alpha = 0.38f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(enabled, steps, start, span) {
                if (enabled) detectTapGestures { offset ->
                    currentChange(valueAt(offset.x))
                    currentFinished?.invoke()
                }
            }
            .pointerInput(enabled, steps, start, span) {
                if (enabled) detectHorizontalDragGestures(
                    onDragEnd = { currentFinished?.invoke() },
                    onDragCancel = { currentFinished?.invoke() },
                ) { change, _ ->
                    change.consume()
                    currentChange(valueAt(change.position.x))
                }
            }
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(valueRange), valueRange, steps)
                if (enabled) setProgress { target ->
                    currentChange(target.coerceIn(valueRange.start, valueRange.endInclusive))
                    true
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        StepBar(
            fraction = fraction,
            modifier = Modifier.fillMaxWidth().height(12.dp),
            color = ink,
            trackColor = c.surfaceContainerHighest,
            dotted = false,
        )
        Box(
            modifier = Modifier
                .offset { IntOffset(((widthPx - 14.dp.toPx()) * fraction).roundToInt(), 0) }
                .size(width = 14.dp, height = 28.dp)
                .clip(shape)
                .background(ink)
                .pixelBevel(c.outline),
        )
    }
}

/** Material's linear progress bar, a block bar in Pixelated, a row of dots in Dot matrix. Null [progress] is indeterminate. */
@Composable
fun AppLinearProgress(
    progress: (() -> Float)? = null,
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.linearColor,
    trackColor: Color = ProgressIndicatorDefaults.linearTrackColor,
) {
    if (isPixelStyle() || isDotStyle()) {
        StepBar(progress?.invoke(), modifier.defaultMinSize(minHeight = 6.dp), color, trackColor, dotted = isDotStyle())
    } else if (progress != null) {
        LinearProgressIndicator(progress = progress, modifier = modifier, color = color, trackColor = trackColor, gapSize = 0.dp, drawStopIndicator = {})
    } else {
        LinearProgressIndicator(modifier = modifier, color = color, trackColor = trackColor)
    }
}

/** Material's wavy bar, or the same block / dot bar as [AppLinearProgress] in Pixelated and Dot matrix. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppLinearWavy(
    progress: (() -> Float)? = null,
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.linearColor,
    trackColor: Color = ProgressIndicatorDefaults.linearTrackColor,
) {
    if (isPixelStyle() || isDotStyle()) {
        StepBar(progress?.invoke(), modifier.height(10.dp), color, trackColor, dotted = isDotStyle())
    } else if (progress != null) {
        LinearWavyProgressIndicator(progress = progress, modifier = modifier, color = color, trackColor = trackColor)
    } else {
        LinearWavyProgressIndicator(modifier = modifier, color = color, trackColor = trackColor)
    }
}

/** Material's circular spinner, or a chasing ring of blocks (Pixelated) / dots (Dot matrix). */
@Composable
fun AppCircularProgress(modifier: Modifier = Modifier, strokeWidth: androidx.compose.ui.unit.Dp = 2.dp) {
    if (isPixelStyle() || isDotStyle()) {
        StepSpinner(modifier, LocalContentColor.current, dotted = isDotStyle())
    } else {
        CircularProgressIndicator(modifier = modifier, strokeWidth = strokeWidth)
    }
}

/** Material's shape-morphing loader, or the same chasing block / dot spinner in Pixelated and Dot matrix. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppLoading(modifier: Modifier = Modifier) {
    if (isPixelStyle() || isDotStyle()) {
        StepSpinner(modifier, MaterialTheme.colorScheme.primary, dotted = isDotStyle())
    } else {
        LoadingIndicator(modifier = modifier)
    }
}

/**
 * A bar made of separate cells. [fraction] null is indeterminate: a short run of cells sweeps along. Cells are squares in Pixelated and
 * round dots ([dotted]) in Dot matrix; a cell is as tall as the bar.
 */
@Composable
internal fun StepBar(fraction: Float?, modifier: Modifier, color: Color, trackColor: Color, dotted: Boolean) {
    val q = LocalQuality.current
    val sweep = if (fraction == null && q.decorativeMotion) {
        val t = rememberInfiniteTransition(label = "stepBar")
        val v by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "sweep")
        v
    } else {
        0.3f
    }
    Canvas(modifier = modifier) {
        val h = size.height
        val cell = max(h, 1f)
        val gap = if (dotted) cell * 0.5f else cell * 0.34f
        val pitch = cell + gap
        val count = max(1, floor((size.width + gap) / pitch).toInt())
        val used = count * pitch - gap
        val startX = (size.width - used) / 2f
        val filled = if (fraction != null) fraction * count else 0f
        for (i in 0 until count) {
            val on = if (fraction != null) {
                // The cell the bar is part-way through counts as full once more than half of it is covered.
                i < floor(filled).toInt() || (i == floor(filled).toInt() && filled - floor(filled) >= 0.5f)
            } else {
                val d = ((sweep * count) - i + count) % count
                d < 4f
            }
            val x = startX + i * pitch
            val col = if (on) color else trackColor
            if (dotted) {
                drawCircle(color = col, radius = cell / 2f, center = Offset(x + cell / 2f, h / 2f))
            } else {
                drawRect(color = col, topLeft = Offset(x, 0f), size = Size(cell, h))
            }
        }
    }
}

/**
 * Eight cells round a square with a bright head that chases round it, leaving a fading tail. Square in Pixelated, round in Dot matrix.
 * With reduced motion the head stays put.
 */
@Composable
internal fun StepSpinner(modifier: Modifier, color: Color, dotted: Boolean) {
    val q = LocalQuality.current
    val head = if (q.decorativeMotion) {
        val t = rememberInfiniteTransition(label = "stepSpinner")
        val v by t.animateFloat(0f, 8f, infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart), label = "head")
        v
    } else {
        0f
    }
    Canvas(modifier = modifier.defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)) {
        val side = min(size.width, size.height)
        val pitch = side / 3f
        val cell = pitch * 0.72f
        val ox = (size.width - side) / 2f
        val oy = (size.height - side) / 2f
        // Clockwise from the top-left corner.
        val ring = listOf(0 to 0, 1 to 0, 2 to 0, 2 to 1, 2 to 2, 1 to 2, 0 to 2, 0 to 1)
        for ((i, p) in ring.withIndex()) {
            val behind = ((head - i) % 8f + 8f) % 8f
            val alpha = (1f - behind / 5f).coerceIn(0.18f, 1f)
            val cx = ox + p.first * pitch + pitch / 2f
            val cy = oy + p.second * pitch + pitch / 2f
            if (dotted) {
                drawCircle(color = color.copy(alpha = alpha), radius = cell / 2f, center = Offset(cx, cy))
            } else {
                drawRect(color = color.copy(alpha = alpha), topLeft = Offset(cx - cell / 2f, cy - cell / 2f), size = Size(cell, cell))
            }
        }
    }
}
