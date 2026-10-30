package skywa1ker404.absencium.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Diagonal hatch lines over the whole drawing area, for "nothing here" (a holiday, an empty part of a bar). Clip the area to its
 * shape first; the lines are drawn corner to corner and are not trimmed.
 */
fun DrawScope.drawHatch(color: Color, spacing: Dp = 5.dp, strokeWidth: Dp = 1.dp) {
    val step = spacing.toPx()
    val w = size.width
    val h = size.height
    var x = -h
    while (x < w) {
        drawLine(color, Offset(x, h), Offset(x + h, 0f), strokeWidth.toPx())
        x += step
    }
}
