package skywa1ker404.absencium.ui.vault.entry

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import skywa1ker404.absencium.logging.DebugLogger
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.icons.AppIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** The crop frame shapes offered in the crop menu. [ratio] is width divided by height; null means free. */
private enum class CropTemplate(val label: String, val ratio: Float?) {
    FREE("Free", null),
    SQUARE("1:1", 1f),
    LANDSCAPE_4_3("4:3", 4f / 3f),
    PORTRAIT_3_4("3:4", 3f / 4f),
    LANDSCAPE_16_9("16:9", 16f / 9f),
    PORTRAIT_9_16("9:16", 9f / 16f),
    A4("A4", 210f / 297f),
}

private enum class Grip { NONE, MOVE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, LEFT, TOP, RIGHT, BOTTOM }

/** Rotation, flips and fine straightening of a photo. Applied in that order: quarter turns, mirror, small angle. */
private data class PhotoTransform(
    val quarterTurns: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val fineDegrees: Float = 0f,
)

/**
 * Builds the matrix that places a [width] x [height] photo, transformed, in a view of [viewWidth] x [viewHeight].
 * The photo is scaled so it fits the view even at the current angle. Returns the matrix and that scale.
 */
private fun placement(width: Int, height: Int, t: PhotoTransform, viewWidth: Float, viewHeight: Float): Pair<Matrix, Float> {
    val odd = t.quarterTurns % 2 != 0
    val dw = if (odd) height.toFloat() else width.toFloat()
    val dh = if (odd) width.toFloat() else height.toFloat()
    val radians = Math.toRadians(t.fineDegrees.toDouble())
    val c = abs(cos(radians)).toFloat()
    val s = abs(sin(radians)).toFloat()
    val boxW = dw * c + dh * s
    val boxH = dw * s + dh * c
    val scale = min(viewWidth / boxW, viewHeight / boxH)
    val matrix = Matrix().apply {
        postTranslate(-width / 2f, -height / 2f)
        postRotate(t.quarterTurns * 90f)
        postScale(if (t.flipHorizontal) -1f else 1f, if (t.flipVertical) -1f else 1f)
        postRotate(t.fineDegrees)
        postScale(scale, scale)
        postTranslate(viewWidth / 2f, viewHeight / 2f)
    }
    return matrix to scale
}

/** The largest rectangle with [ratio] (or the whole of [bounds] when null) centred in [bounds]. */
private fun fitRect(bounds: Rect, ratio: Float?): Rect {
    if (ratio == null) return bounds
    var w = bounds.width
    var h = w / ratio
    if (h > bounds.height) {
        h = bounds.height
        w = h * ratio
    }
    val left = bounds.center.x - w / 2f
    val top = bounds.center.y - h / 2f
    return Rect(left, top, left + w, top + h)
}

/** Draws the photo with the transform, cropped to [crop] (in view coordinates), at the photo's own resolution. */
private fun renderCrop(source: Bitmap, t: PhotoTransform, viewWidth: Float, viewHeight: Float, crop: Rect): Bitmap {
    val (matrix, scale) = placement(source.width, source.height, t, viewWidth, viewHeight)
    var factor = 1f / scale
    val longest = max(crop.width, crop.height) * factor
    if (longest > MAX_OUTPUT_EDGE) factor *= MAX_OUTPUT_EDGE / longest
    val outWidth = (crop.width * factor).roundToInt().coerceAtLeast(1)
    val outHeight = (crop.height * factor).roundToInt().coerceAtLeast(1)
    val out = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(out)
    canvas.drawColor(android.graphics.Color.BLACK)
    matrix.postTranslate(-crop.left, -crop.top)
    matrix.postScale(outWidth / crop.width, outHeight / crop.height)
    canvas.drawBitmap(source, matrix, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
    return out
}

private const val MAX_OUTPUT_EDGE = 4096
private const val SOURCE_EDGE = 2800
private const val MIN_CROP_PX = 64f

/**
 * Full-screen crop editor. The frame can be dragged and resized, the photo can be turned in quarter steps, mirrored and
 * straightened with a slider, and the frame can snap to a template. Done writes a new JPEG with [newFile] and gives it
 * to [onDone]; the original is left for the caller to replace. Cancel changes nothing.
 */
@Composable
fun PhotoCropDialog(
    path: String,
    newFile: () -> File,
    onCancel: () -> Unit,
    onDone: (File) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val source by produceState<Bitmap?>(null, path) {
        value = withContext(Dispatchers.IO) { ImageTools.decode(path, SOURCE_EDGE) }
    }

    var transform by remember { mutableStateOf(PhotoTransform()) }
    var template by remember { mutableStateOf(CropTemplate.FREE) }
    var viewSize by remember { mutableStateOf(Size.Zero) }
    var crop by remember { mutableStateOf(Rect.Zero) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    /** The part of the view the transformed photo covers, clipped to the view. */
    fun imageBounds(): Rect {
        val bmp = source ?: return Rect(Offset.Zero, viewSize)
        val (matrix, _) = placement(bmp.width, bmp.height, transform, viewSize.width, viewSize.height)
        val mapped = RectF(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat())
        matrix.mapRect(mapped)
        return Rect(
            max(mapped.left, 0f), max(mapped.top, 0f),
            min(mapped.right, viewSize.width), min(mapped.bottom, viewSize.height),
        )
    }

    fun resetFrame(newTemplate: CropTemplate = template) {
        if (viewSize.width <= 0f || source == null) return
        crop = fitRect(imageBounds(), newTemplate.ratio)
    }

    // The frame starts on the whole photo as soon as the photo and the view are both measured.
    LaunchedEffect(source, viewSize, crop) { if (crop == Rect.Zero) resetFrame() }

    Dialog(
        onDismissRequest = { if (!saving) onCancel() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnClickOutside = false),
    ) {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0B0B)).statusBarsPadding().navigationBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SquareIconButton(AppIcons.ArrowBack, "Cancel crop", onClick = { if (!saving) onCancel() })
                Text("Crop photo", style = MaterialTheme.typography.titleLarge, color = Color.White)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .onSizeChanged { size ->
                        val newSize = Size(size.width.toFloat(), size.height.toFloat())
                        if (newSize != viewSize) {
                            viewSize = newSize
                            crop = Rect.Zero
                        }
                    },
            ) {
                val currentCrop by rememberUpdatedState(crop)
                val currentTemplate by rememberUpdatedState(template)
                val currentViewSize by rememberUpdatedState(viewSize)
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            var grip = Grip.NONE
                            var anchor = Offset.Zero
                            val hit = 40.dp.toPx()
                            detectDragGestures(
                                onDragStart = { start ->
                                    val r = currentCrop
                                    fun near(p: Offset) = (start - p).getDistance() <= hit
                                    grip = when {
                                        near(Offset(r.left, r.top)) -> Grip.TOP_LEFT
                                        near(Offset(r.right, r.top)) -> Grip.TOP_RIGHT
                                        near(Offset(r.left, r.bottom)) -> Grip.BOTTOM_LEFT
                                        near(Offset(r.right, r.bottom)) -> Grip.BOTTOM_RIGHT
                                        currentTemplate.ratio == null && abs(start.x - r.left) <= hit / 2 &&
                                            start.y in r.top..r.bottom -> Grip.LEFT
                                        currentTemplate.ratio == null && abs(start.x - r.right) <= hit / 2 &&
                                            start.y in r.top..r.bottom -> Grip.RIGHT
                                        currentTemplate.ratio == null && abs(start.y - r.top) <= hit / 2 &&
                                            start.x in r.left..r.right -> Grip.TOP
                                        currentTemplate.ratio == null && abs(start.y - r.bottom) <= hit / 2 &&
                                            start.x in r.left..r.right -> Grip.BOTTOM
                                        r.contains(start) -> Grip.MOVE
                                        else -> Grip.NONE
                                    }
                                    anchor = when (grip) {
                                        Grip.TOP_LEFT -> Offset(r.right, r.bottom)
                                        Grip.TOP_RIGHT -> Offset(r.left, r.bottom)
                                        Grip.BOTTOM_LEFT -> Offset(r.right, r.top)
                                        Grip.BOTTOM_RIGHT -> Offset(r.left, r.top)
                                        else -> Offset.Zero
                                    }
                                },
                                onDragEnd = { grip = Grip.NONE },
                                onDragCancel = { grip = Grip.NONE },
                            ) { change, drag ->
                                change.consume()
                                val view = currentViewSize
                                val r = currentCrop
                                val ratio = currentTemplate.ratio
                                crop = when (grip) {
                                    Grip.NONE -> r
                                    Grip.MOVE -> {
                                        val dx = drag.x.coerceIn(-r.left, view.width - r.right)
                                        val dy = drag.y.coerceIn(-r.top, view.height - r.bottom)
                                        r.translate(dx, dy)
                                    }
                                    Grip.LEFT -> Rect(change.position.x.coerceIn(0f, r.right - MIN_CROP_PX), r.top, r.right, r.bottom)
                                    Grip.RIGHT -> Rect(r.left, r.top, change.position.x.coerceIn(r.left + MIN_CROP_PX, view.width), r.bottom)
                                    Grip.TOP -> Rect(r.left, change.position.y.coerceIn(0f, r.bottom - MIN_CROP_PX), r.right, r.bottom)
                                    Grip.BOTTOM -> Rect(r.left, r.top, r.right, change.position.y.coerceIn(r.top + MIN_CROP_PX, view.height))
                                    else -> {
                                        val p = Offset(change.position.x.coerceIn(0f, view.width), change.position.y.coerceIn(0f, view.height))
                                        if (ratio == null) {
                                            val left = min(anchor.x, p.x)
                                            val top = min(anchor.y, p.y)
                                            Rect(left, top, max(left + MIN_CROP_PX, max(anchor.x, p.x)), max(top + MIN_CROP_PX, max(anchor.y, p.y)))
                                        } else {
                                            val sx = if (p.x >= anchor.x) 1f else -1f
                                            val sy = if (p.y >= anchor.y) 1f else -1f
                                            val maxW = min(if (sx > 0) view.width - anchor.x else anchor.x, (if (sy > 0) view.height - anchor.y else anchor.y) * ratio)
                                            val w = max(abs(p.x - anchor.x), abs(p.y - anchor.y) * ratio).coerceIn(MIN_CROP_PX, max(maxW, MIN_CROP_PX))
                                            val h = w / ratio
                                            Rect(
                                                min(anchor.x, anchor.x + sx * w), min(anchor.y, anchor.y + sy * h),
                                                max(anchor.x, anchor.x + sx * w), max(anchor.y, anchor.y + sy * h),
                                            )
                                        }
                                    }
                                }
                            }
                        },
                ) {
                    val bmp = source
                    if (bmp != null && viewSize.width > 0f) {
                        val (matrix, _) = placement(bmp.width, bmp.height, transform, size.width, size.height)
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawBitmap(bmp, matrix, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
                        }
                        val frame = crop
                        if (frame != Rect.Zero) {
                            val dim = Color.Black.copy(alpha = 0.58f)
                            drawRect(dim, Offset.Zero, Size(size.width, frame.top))
                            drawRect(dim, Offset(0f, frame.bottom), Size(size.width, size.height - frame.bottom))
                            drawRect(dim, Offset(0f, frame.top), Size(frame.left, frame.height))
                            drawRect(dim, Offset(frame.right, frame.top), Size(size.width - frame.right, frame.height))
                            val grid = Color.White.copy(alpha = 0.35f)
                            for (i in 1..2) {
                                val x = frame.left + frame.width * i / 3f
                                val y = frame.top + frame.height * i / 3f
                                drawLine(grid, Offset(x, frame.top), Offset(x, frame.bottom), 1.dp.toPx())
                                drawLine(grid, Offset(frame.left, y), Offset(frame.right, y), 1.dp.toPx())
                            }
                            drawRect(Color.White, frame.topLeft, frame.size, style = Stroke(2.dp.toPx()))
                            val arm = 22.dp.toPx()
                            val thick = 5.dp.toPx()
                            fun corner(x: Float, y: Float, dx: Float, dy: Float) {
                                drawLine(Color.White, Offset(x, y), Offset(x + dx * arm, y), thick)
                                drawLine(Color.White, Offset(x, y), Offset(x, y + dy * arm), thick)
                            }
                            corner(frame.left, frame.top, 1f, 1f)
                            corner(frame.right, frame.top, -1f, 1f)
                            corner(frame.left, frame.bottom, 1f, -1f)
                            corner(frame.right, frame.bottom, -1f, -1f)
                        }
                    }
                }
                if (source == null) {
                    LoadingDots("Opening the photo", modifier = Modifier.align(Alignment.Center), color = Color.White)
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CropTemplate.entries.forEach { option ->
                        FilterChip(
                            selected = template == option,
                            onClick = {
                                template = option
                                resetFrame(option)
                            },
                            label = { Text(option.label) },
                        )
                    }
                }
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CropIcon("Rotate left", mirrored = true, icon = AppIcons.RotateRight) {
                        transform = transform.copy(quarterTurns = (transform.quarterTurns + 3) % 4)
                        crop = Rect.Zero
                    }
                    CropIcon("Rotate right", icon = AppIcons.RotateRight) {
                        transform = transform.copy(quarterTurns = (transform.quarterTurns + 1) % 4)
                        crop = Rect.Zero
                    }
                    CropIcon("Flip horizontal", icon = AppIcons.FlipHorizontal, selected = transform.flipHorizontal) {
                        transform = transform.copy(flipHorizontal = !transform.flipHorizontal)
                    }
                    CropIcon("Flip vertical", icon = AppIcons.FlipVertical, selected = transform.flipVertical) {
                        transform = transform.copy(flipVertical = !transform.flipVertical)
                    }
                    // Puts the photo back as it was opened: no turns, no flips, no tilt.
                    CropIcon("Reset rotation", icon = AppIcons.Refresh) {
                        transform = PhotoTransform()
                        crop = Rect.Zero
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Angle ${"%.1f".format(transform.fineDegrees)}°",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    Slider(
                        value = transform.fineDegrees,
                        onValueChange = { transform = transform.copy(fineDegrees = (it * 2f).roundToInt() / 2f) },
                        onValueChangeFinished = { crop = Rect.Zero },
                        valueRange = -45f..45f,
                        modifier = Modifier.weight(1f),
                    )
                }
                error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.error) }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    PillButton(text = "Cancel", tonal = true, enabled = !saving, onClick = onCancel, modifier = Modifier.weight(1f))
                    PillButton(
                        text = "Done",
                        loading = saving,
                        enabled = source != null && crop != Rect.Zero && crop.width >= MIN_CROP_PX && crop.height >= MIN_CROP_PX,
                        onClick = {
                            val bmp = source ?: return@PillButton
                            val frame = crop
                            val spec = transform
                            val view = viewSize
                            saving = true
                            error = null
                            scope.launch {
                                try {
                                    val file = withContext(Dispatchers.IO) {
                                        val rendered = renderCrop(bmp, spec, view.width, view.height, frame)
                                        val target = newFile()
                                        try {
                                            target.outputStream().use { rendered.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                                        } finally {
                                            rendered.recycle()
                                        }
                                        target
                                    }
                                    onDone(file)
                                } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    DebugLogger.e("Crop", "Could not save the cropped photo", e)
                                    error = "Could not save the cropped photo: ${e.message ?: e.javaClass.simpleName}"
                                    saving = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** A round icon button of the photo editor, light on its dark page. [mirrored] flips the icon sideways (rotate left). */
@Composable
private fun CropIcon(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, mirrored: Boolean = false, selected: Boolean = false, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(46.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(if (selected) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Icon(
            icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp).graphicsLayer { scaleX = if (mirrored) -1f else 1f },
        )
    }
}
