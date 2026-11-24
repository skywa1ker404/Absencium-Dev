package me.rerere.rikkahub.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * What the host app plugs in so the chat's corners follow its Pixelated style. Without a host everything is Compose's own rounded
 * corners. [pixelated] says whether the stair-step style is on right now and [pixelShape] builds a stair-step rectangle (step size,
 * then the four corner sizes: top start, top end, bottom end, bottom start).
 */
object HubStyle {
    @Volatile
    var pixelated: () -> Boolean = { false }

    @Volatile
    var pixelShape: ((Float, Dp, Dp, Dp, Dp) -> Shape)? = null

    /** The host's circle made of square pixels. */
    @Volatile
    var pixelCircle: (() -> Shape)? = null

    /** The host's switch: checked, on change, modifier, enabled. Null means Material's own. */
    @Volatile
    var switch: (@Composable (Boolean, ((Boolean) -> Unit)?, Modifier, Boolean) -> Unit)? = null

    /** The host's slider: value, on change, modifier, enabled, range, steps, on finished. */
    @Volatile
    var slider: (@Composable (Float, (Float) -> Unit, Modifier, Boolean, ClosedFloatingPointRange<Float>, Int, (() -> Unit)?) -> Unit)? = null

    /** The host's waiting indicator (the shape-morphing loader, or its Pixelated / Dot matrix spinner). */
    @Volatile
    var loading: (@Composable (Modifier) -> Unit)? = null

    /** The host's bar: progress (null is indeterminate), modifier, wavy. */
    @Volatile
    var bar: (@Composable ((() -> Float)?, Modifier, Boolean) -> Unit)? = null

    /** The host's button edge for the Pixelated style: modifier and the outline colour. */
    @Volatile
    var bevel: (@Composable (Modifier, androidx.compose.ui.graphics.Color) -> Modifier)? = null

    /** The host's bottom pill (the one on its home screen): tab icons, selected index, select callback, modifier. */
    @Volatile
    var tabPill: (@Composable (List<androidx.compose.ui.graphics.vector.ImageVector>, Int, (Int) -> Unit, Modifier) -> Unit)? = null
}

private fun stepFor(radius: Dp): Float = (radius.value / 8f).coerceIn(1.5f, 3f)

private fun corner(radius: Dp): Dp = if (radius.value <= 0f) 0.dp else (stepFor(radius) * 2f).dp

private fun pixel(step: Float, a: Dp, b: Dp, c: Dp, d: Dp): Shape? =
    if (HubStyle.pixelated()) HubStyle.pixelShape?.invoke(step, a, b, c, d) else null

private fun fixed(): Shape? = pixel(2f, 4.dp, 4.dp, 4.dp, 4.dp)

/** The chat's rounded corners: Compose's own, or the stair-step corners while the host is in its Pixelated style. */
fun RoundedCornerShape(size: Dp): Shape =
    (if (size.value >= 48f && HubStyle.pixelated()) HubStyle.pixelCircle?.invoke() else null) ?: pixel(stepFor(size), corner(size), corner(size), corner(size), corner(size))
        ?: androidx.compose.foundation.shape.RoundedCornerShape(size)

fun RoundedCornerShape(size: Float): Shape = fixed() ?: androidx.compose.foundation.shape.RoundedCornerShape(size)

fun RoundedCornerShape(percent: Int): Shape =
    (if (percent >= 50 && HubStyle.pixelated()) HubStyle.pixelCircle?.invoke() else null) ?: fixed() ?: androidx.compose.foundation.shape.RoundedCornerShape(percent)

fun RoundedCornerShape(topStart: Dp = 0.dp, topEnd: Dp = 0.dp, bottomEnd: Dp = 0.dp, bottomStart: Dp = 0.dp): Shape {
    val biggest = maxOf(topStart.value, topEnd.value, bottomEnd.value, bottomStart.value).dp
    return pixel(stepFor(biggest), corner(topStart), corner(topEnd), corner(bottomEnd), corner(bottomStart))
        ?: androidx.compose.foundation.shape.RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)
}

/** The chat's circle: a true circle, or the stair-step block while the host is in its Pixelated style. */
val CircleShape: Shape
    get() = (if (HubStyle.pixelated()) HubStyle.pixelCircle?.invoke() else null) ?: androidx.compose.foundation.shape.CircleShape
