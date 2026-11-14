package skywa1ker404.absencium.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import skywa1ker404.absencium.data.settings.AppStyle

/** The style the app is drawn in right now, so shared components can switch to their pixel look. */
val LocalAppStyle = staticCompositionLocalOf { AppStyle.DEFAULT }

/** A rectangle whose four corners are cut in two stair steps of [stepDp] each, like the buttons of an old game. */
class PixelCornerShape(private val stepDp: Float = 2f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        // Never more than a quarter of the shortest side, so a tiny box is still a box.
        val u = minOf(with(density) { stepDp.dp.toPx() }, minOf(w, h) / 6f).coerceAtLeast(0f)
        val path = Path().apply {
            moveTo(2 * u, 0f)
            lineTo(w - 2 * u, 0f); lineTo(w - 2 * u, u); lineTo(w - u, u); lineTo(w - u, 2 * u); lineTo(w, 2 * u)
            lineTo(w, h - 2 * u); lineTo(w - u, h - 2 * u); lineTo(w - u, h - u); lineTo(w - 2 * u, h - u); lineTo(w - 2 * u, h)
            lineTo(2 * u, h); lineTo(2 * u, h - u); lineTo(u, h - u); lineTo(u, h - 2 * u); lineTo(0f, h - 2 * u)
            lineTo(0f, 2 * u); lineTo(u, 2 * u); lineTo(u, u); lineTo(2 * u, u)
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Draws the Pixelated button edge over the content: a dark outline that follows the stepped corners, a light line along the top and left,
 * and a darker band along the bottom and right, like the keys in the reference sheet.
 */
fun Modifier.pixelBevel(outline: Color, stepDp: Float = 2f): Modifier = drawWithContent {
    drawContent()
    val u = stepDp.dp.toPx().coerceAtMost(minOf(size.width, size.height) / 6f)
    val shape = PixelCornerShape(stepDp).createOutline(size, LayoutDirection.Ltr, this)
    val path = (shape as Outline.Generic).path
    clipPath(path) {
        drawRect(Color.White.copy(alpha = 0.30f), Offset(u, u), Size((size.width - 2 * u).coerceAtLeast(0f), u))
        drawRect(Color.White.copy(alpha = 0.30f), Offset(u, u), Size(u, (size.height - 2 * u).coerceAtLeast(0f)))
        drawRect(Color.Black.copy(alpha = 0.26f), Offset(u, size.height - 3 * u), Size((size.width - 2 * u).coerceAtLeast(0f), 2 * u))
        drawRect(Color.Black.copy(alpha = 0.26f), Offset(size.width - 2 * u, u), Size(u, (size.height - 2 * u).coerceAtLeast(0f)))
        drawPath(path, outline, style = Stroke(width = 2 * u))
    }
}
