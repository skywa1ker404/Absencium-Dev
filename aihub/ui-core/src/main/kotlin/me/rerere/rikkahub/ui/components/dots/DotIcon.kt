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
        val dots = remember(base) { DotPainter(imageVector, base, square = false, crop = true) }
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

/**
 * Any picture drawn as a grid of dots ([square] false) or of square pixels ([square] true), for the app's own marks such as the
 * header's A. The whole picture is used (no icon padding), and [key] names it so it is only rasterised once.
 */
@Composable
fun rememberMosaicPainter(key: Any, base: Painter, square: Boolean): Painter = remember(key, square) { DotPainter(key, base, square, crop = false) }

/** Dots per side across the icon's live area (Material icons keep their drawing inside the middle 20 of 24 units). */
private const val GRID = 14

/** The whole 24 unit icon is drawn once at 7 pixels per unit; the live area (2..22) is then read in 10 pixel blocks, one per dot. */
private const val UNIT_PX = 7
private const val FULL = 24 * UNIT_PX
private const val EDGE = 2 * UNIT_PX
private const val BLOCK = (20 * UNIT_PX) / GRID

/** How much of each dot's cell an icon covers, kept per icon so it is only drawn to pixels once. Only touched while drawing. */
private val masks = HashMap<Any, FloatArray>()

private class DotPainter(private val key: Any, private val base: Painter, private val square: Boolean, private val crop: Boolean) : Painter() {
    override val intrinsicSize: Size get() = base.intrinsicSize

    override fun DrawScope.onDraw() {
        val mask = masks.getOrPut(Triple(key, crop, square)) { rasterize(base, crop) }
        // The live area is 20/24 of the icon, centred; one cell is its 1/GRID.
        val inset = if (crop) size.width / 12f else 0f
        val cellW = (size.width - inset * 2f) / GRID
        val cellH = (size.height - inset * 2f) / GRID
        val maxRadius = minOf(cellW, cellH) * 0.44f
        for (y in 0 until GRID) {
            for (x in 0 until GRID) {
                val coverage = mask[y * GRID + x]
                if (coverage >= 0.22f) {
                    // Edge dots are a little smaller, which softens the outline the way a real dot display does.
                    val radius = maxRadius * (0.62f + 0.38f * ((coverage - 0.22f) / 0.78f).coerceIn(0f, 1f))
                    if (square) {
                        // Pixelated: each cell is a square pixel (full cells at the core, a touch smaller at the edge).
                        val w = cellW * (0.78f + 0.22f * ((coverage - 0.22f) / 0.78f).coerceIn(0f, 1f))
                        val h = cellH * (0.78f + 0.22f * ((coverage - 0.22f) / 0.78f).coerceIn(0f, 1f))
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(inset + (x + 0.5f) * cellW - w / 2f, inset + (y + 0.5f) * cellH - h / 2f),
                            size = Size(w, h),
                        )
                    } else {
                        drawCircle(
                            color = Color.Black,
                            radius = radius,
                            center = Offset(inset + (x + 0.5f) * cellW, inset + (y + 0.5f) * cellH),
                        )
                    }
                }
            }
        }
    }
}

/** Draws the icon into a bitmap and returns, for each dot of the live area, how much of its cell the icon covers (0 to 1). */
private fun rasterize(base: Painter, crop: Boolean): FloatArray {
    val side = FULL.toFloat()
    val bitmap = ImageBitmap(FULL, FULL)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap), Size(side, side)) {
        with(base) { draw(size = Size(side, side)) }
    }
    val pixels = IntArray(FULL * FULL)
    bitmap.readPixels(pixels, 0, 0, FULL, FULL)
    val edge = if (crop) EDGE else 0
    val block = if (crop) BLOCK else FULL / GRID
    val out = FloatArray(GRID * GRID)
    for (gy in 0 until GRID) {
        for (gx in 0 until GRID) {
            var sum = 0f
            for (py in 0 until block) {
                for (px in 0 until block) {
                    sum += ((pixels[(edge + gy * block + py) * FULL + edge + gx * block + px] ushr 24) and 0xFF) / 255f
                }
            }
            out[gy * GRID + gx] = sum / (block * block)
        }
    }
    return out
}
