package me.rerere.rikkahub.ui.components.dots

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/** True while the app is in its Dot matrix style: every vector icon is then drawn as a grid of round dots instead of a smooth shape. */
val LocalDotIcons = staticCompositionLocalOf { false }

/**
 * The app's Icon. It is Material's Icon, except in the Dot matrix style where a vector icon is drawn as dots (the same grid the style's
 * font uses). Pictures and bitmaps, and icons that keep their own colours (tint unspecified), are drawn as usual.
 */
@Composable
fun Icon(imageVector: ImageVector, contentDescription: String?, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    if (LocalDotIcons.current && tint != Color.Unspecified) {
        val base = rememberVectorPainter(imageVector)
        val dots = remember(base) { DotPainter(imageVector, base) }
        androidx.compose.material3.Icon(painter = dots, contentDescription = contentDescription, modifier = modifier, tint = tint)
    } else {
        androidx.compose.material3.Icon(imageVector = imageVector, contentDescription = contentDescription, modifier = modifier, tint = tint)
    }
}

@Composable
fun Icon(painter: Painter, contentDescription: String?, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    androidx.compose.material3.Icon(painter = painter, contentDescription = contentDescription, modifier = modifier, tint = tint)
}

@Composable
fun Icon(bitmap: ImageBitmap, contentDescription: String?, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    androidx.compose.material3.Icon(bitmap = bitmap, contentDescription = contentDescription, modifier = modifier, tint = tint)
}

/** Dots per side. */
private const val GRID = 14

/** The icon is drawn once at this size and each dot reads a 4 by 4 block of it. */
private const val RESOLUTION = GRID * 4

/** How much of each dot's cell an icon covers, kept per icon so it is only drawn to pixels once. Only touched while drawing. */
private val masks = HashMap<ImageVector, FloatArray>()

private class DotPainter(private val key: ImageVector, private val base: Painter) : Painter() {
    override val intrinsicSize: Size get() = base.intrinsicSize

    override fun DrawScope.onDraw() {
        val mask = masks.getOrPut(key) { rasterize(base) }
        val cellW = size.width / GRID
        val cellH = size.height / GRID
        val radius = minOf(cellW, cellH) * 0.42f
        for (y in 0 until GRID) {
            for (x in 0 until GRID) {
                val coverage = mask[y * GRID + x]
                if (coverage >= 0.25f) {
                    drawCircle(
                        color = Color.Black.copy(alpha = (0.6f + coverage * 0.4f).coerceAtMost(1f)),
                        radius = radius,
                        center = Offset((x + 0.5f) * cellW, (y + 0.5f) * cellH),
                    )
                }
            }
        }
    }
}

/** Draws the icon into a small bitmap and returns, for each dot, how much of its cell the icon covers (0 to 1). */
private fun rasterize(base: Painter): FloatArray {
    val side = RESOLUTION.toFloat()
    val bitmap = ImageBitmap(RESOLUTION, RESOLUTION)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap), Size(side, side)) {
        with(base) { draw(size = Size(side, side)) }
    }
    val pixels = IntArray(RESOLUTION * RESOLUTION)
    bitmap.readPixels(pixels, 0, 0, RESOLUTION, RESOLUTION)
    val block = RESOLUTION / GRID
    val out = FloatArray(GRID * GRID)
    for (gy in 0 until GRID) {
        for (gx in 0 until GRID) {
            var sum = 0f
            for (py in 0 until block) {
                for (px in 0 until block) {
                    sum += ((pixels[(gy * block + py) * RESOLUTION + gx * block + px] ushr 24) and 0xFF) / 255f
                }
            }
            out[gy * GRID + gx] = sum / (block * block)
        }
    }
    return out
}
