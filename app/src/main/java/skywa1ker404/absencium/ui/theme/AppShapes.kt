package skywa1ker404.absencium.ui.theme

import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Whether the app is in the Pixelated style right now. [AbsenciumTheme] sets it before anything below it is composed, so the shape
 * functions here can be called from anywhere (not only from composables) and still give the right shape.
 */
object StyleFlags {
    @Volatile
    var pixelated: Boolean = false
}

/** One stair step for a corner of [radius]: a small radius gets a fine step, a large one a coarser step, never beyond 3dp. */
private fun stepFor(radius: Dp): Float = (radius.value / 8f).coerceIn(1.5f, 3f)

private fun corner(radius: Dp): Dp = if (radius.value <= 0f) 0.dp else (stepFor(radius) * 2f).dp

/**
 * The app's rounded corners. Use these instead of Compose's own RoundedCornerShape: in the Default and Liquid Glass styles they are
 * exactly Compose's rounded corners, and in Pixelated they become the stair-step corners, so every card, field, chip and tile follows.
 */
fun RoundedCornerShape(size: Dp): Shape =
    if (StyleFlags.pixelated) PixelCornerShape.of(stepFor(size), corner(size), corner(size), corner(size), corner(size))
    else androidx.compose.foundation.shape.RoundedCornerShape(size)

fun RoundedCornerShape(size: Float): Shape =
    if (StyleFlags.pixelated) PixelCornerShape(2f) else androidx.compose.foundation.shape.RoundedCornerShape(size)

fun RoundedCornerShape(percent: Int): Shape =
    if (StyleFlags.pixelated) PixelCornerShape(2f) else androidx.compose.foundation.shape.RoundedCornerShape(percent)

fun RoundedCornerShape(topStart: Dp = 0.dp, topEnd: Dp = 0.dp, bottomEnd: Dp = 0.dp, bottomStart: Dp = 0.dp): Shape =
    if (StyleFlags.pixelated) {
        val biggest = maxOf(topStart.value, topEnd.value, bottomEnd.value, bottomStart.value).dp
        PixelCornerShape.of(stepFor(biggest), corner(topStart), corner(topEnd), corner(bottomEnd), corner(bottomStart))
    } else {
        androidx.compose.foundation.shape.RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)
    }
