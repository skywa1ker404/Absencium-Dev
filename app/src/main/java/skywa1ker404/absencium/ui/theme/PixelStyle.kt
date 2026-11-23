package skywa1ker404.absencium.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.math.sqrt
import skywa1ker404.absencium.data.settings.AppStyle

/** The style the app is drawn in right now, so shared components can switch to their pixel look. */
val LocalAppStyle = staticCompositionLocalOf { AppStyle.DEFAULT }

/** The bottom bar: [anchored] fixed to the bottom edge (false is the floating pill), and [labels] a label under every icon (anchored only). */
class BottomBarStyle(val anchored: Boolean, val labels: Boolean)

val LocalBottomBar = staticCompositionLocalOf { BottomBarStyle(anchored = false, labels = true) }

/**
 * A rectangle whose corners are cut in two stair steps of [stepDp] each, like the buttons of an old game. It is a corner-based shape
 * (each corner is twice a step), so Material's Shapes accept it and a sheet can still square off some of its corners.
 */
class PixelCornerShape private constructor(
    private val stepDp: Float,
    topStart: CornerSize,
    topEnd: CornerSize,
    bottomEnd: CornerSize,
    bottomStart: CornerSize,
) : CornerBasedShape(topStart, topEnd, bottomEnd, bottomStart) {
    constructor(stepDp: Float = 2f) : this(stepDp, CornerSize((stepDp * 2).dp), CornerSize((stepDp * 2).dp), CornerSize((stepDp * 2).dp), CornerSize((stepDp * 2).dp))

    override fun copy(topStart: CornerSize, topEnd: CornerSize, bottomEnd: CornerSize, bottomStart: CornerSize): CornerBasedShape =
        PixelCornerShape(stepDp, topStart, topEnd, bottomEnd, bottomStart)

    companion object {
        /** A pixel shape with its own size for each corner (a zero corner stays square). */
        fun of(stepDp: Float, topStart: androidx.compose.ui.unit.Dp, topEnd: androidx.compose.ui.unit.Dp, bottomEnd: androidx.compose.ui.unit.Dp, bottomStart: androidx.compose.ui.unit.Dp): PixelCornerShape =
            PixelCornerShape(stepDp, CornerSize(topStart), CornerSize(topEnd), CornerSize(bottomEnd), CornerSize(bottomStart))
    }

    override fun createOutline(size: Size, topStart: Float, topEnd: Float, bottomEnd: Float, bottomStart: Float, layoutDirection: LayoutDirection): Outline {
        val w = size.width
        val h = size.height
        // One step is half a corner.
        val a = topStart / 2f
        val b = topEnd / 2f
        val c = bottomEnd / 2f
        val d = bottomStart / 2f
        val path = Path().apply {
            moveTo(2 * a, 0f)
            lineTo(w - 2 * b, 0f); lineTo(w - 2 * b, b); lineTo(w - b, b); lineTo(w - b, 2 * b); lineTo(w, 2 * b)
            lineTo(w, h - 2 * c); lineTo(w - c, h - 2 * c); lineTo(w - c, h - c); lineTo(w - 2 * c, h - c); lineTo(w - 2 * c, h)
            lineTo(2 * d, h); lineTo(2 * d, h - d); lineTo(d, h - d); lineTo(d, h - 2 * d); lineTo(0f, h - 2 * d)
            lineTo(0f, 2 * a); lineTo(a, 2 * a); lineTo(a, a); lineTo(2 * a, a)
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * A circle (or, in a wide box, a pill) drawn in square pixels: the round ends are built from stair steps cut from a real circle, so a
 * round button, an avatar or a dot still looks round in the Pixelated style, only made of blocks. The pixel size follows the shape:
 * 1.25dp to 2dp (a sixteenth of the radius), fine enough that the edge reads as a smooth circle.
 */
class PixelCircleShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val r = minOf(w, h) / 2f
        if (r <= 0f) return Outline.Rectangle(Rect(0f, 0f, w, h))
        val cells = (r / (r / 16f).coerceIn(1.25f * density.density, 2f * density.density)).roundToInt().coerceAtLeast(1)
        val u = r / cells
        // How far each row of the corner is cut in from the side, in whole pixels.
        val x = FloatArray(cells) { i ->
            val dy = r - (i + 0.5f) * u
            val inset = r - sqrt((r * r - dy * dy).coerceAtLeast(0f))
            (inset / u).roundToInt() * u
        }
        val n = cells
        val path = Path().apply {
            moveTo(x[0], 0f)
            lineTo(w - x[0], 0f)
            for (i in 0 until n) {
                lineTo(w - x[i], (i + 1) * u)
                if (i < n - 1) lineTo(w - x[i + 1], (i + 1) * u)
            }
            lineTo(w - x[n - 1], h - n * u)
            for (j in n - 1 downTo 0) {
                lineTo(w - x[j], h - j * u)
                if (j > 0) lineTo(w - x[j - 1], h - j * u)
            }
            lineTo(x[0], h)
            for (j in 0 until n) {
                lineTo(x[j], h - (j + 1) * u)
                if (j < n - 1) lineTo(x[j + 1], h - (j + 1) * u)
            }
            lineTo(x[n - 1], n * u)
            for (j in n - 1 downTo 0) {
                lineTo(x[j], j * u)
                if (j > 0) lineTo(x[j - 1], j * u)
            }
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Draws the Pixelated button edge over the content: a dark outline that follows the stepped corners, a light line along the top and left,
 * and a darker band along the bottom and right, like the keys in the reference sheet. Round buttons ([circle]) get the outline only.
 */
fun Modifier.pixelBevel(outline: Color, stepDp: Float = 2f, circle: Boolean = false): Modifier = drawWithContent {
    drawContent()
    val u = stepDp.dp.toPx().coerceAtMost(minOf(size.width, size.height) / 6f)
    val shape = if (circle) PixelCircleShape().createOutline(size, LayoutDirection.Ltr, this) else PixelCornerShape(stepDp).createOutline(size, LayoutDirection.Ltr, this)
    val path = (shape as Outline.Generic).path
    clipPath(path) {
        // A round button has only its outline: the straight highlight and shade bands do not follow the round ends and drew a second ring.
        if (circle) {
            drawPath(path, outline, style = Stroke(width = 2 * u))
            return@clipPath
        }
        drawRect(Color.White.copy(alpha = 0.30f), Offset(u, u), Size((size.width - 2 * u).coerceAtLeast(0f), u))
        drawRect(Color.White.copy(alpha = 0.30f), Offset(u, u), Size(u, (size.height - 2 * u).coerceAtLeast(0f)))
        drawRect(Color.Black.copy(alpha = 0.26f), Offset(u, size.height - 3 * u), Size((size.width - 2 * u).coerceAtLeast(0f), 2 * u))
        drawRect(Color.Black.copy(alpha = 0.26f), Offset(size.width - 2 * u, u), Size(u, (size.height - 2 * u).coerceAtLeast(0f)))
        drawPath(path, outline, style = Stroke(width = 2 * u))
    }
}

/** The shape of pills, bars and round buttons: a circle, or the same circle made of square pixels in the Pixelated style. */
@Composable
fun pillShape(): Shape = if (LocalAppStyle.current == AppStyle.PIXELATED) PixelCircleShape() else CircleShape

/** A pill-shaped coloured background: [pillShape] clip plus [color]. One place for every small pill and chip. */
@Composable
fun Modifier.pillBackground(color: Color): Modifier = this.clip(pillShape()).background(color)

/** The same with a [brush] (a gradient). */
@Composable
fun Modifier.pillBackground(brush: androidx.compose.ui.graphics.Brush): Modifier = this.clip(pillShape()).background(brush)

/** [default] normally, or the stair-step rectangle in the Pixelated style (one step is [stepDp]). */
@Composable
fun pixelOr(default: Shape, stepDp: Float = 2f): Shape = if (LocalAppStyle.current == AppStyle.PIXELATED) PixelCornerShape(stepDp) else default
