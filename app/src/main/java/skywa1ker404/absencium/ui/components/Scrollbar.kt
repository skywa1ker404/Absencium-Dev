package skywa1ker404.absencium.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable

/**
 * A thin scrollbar for a lazy list. It fades in while the list moves and fades out a moment after it stops. The thumb
 * position follows the first visible item (rows are of similar size), so it is a guide rather than an exact measure.
 */
@Composable
fun Modifier.verticalScrollbar(state: LazyListState, color: Color): Modifier {
    val alpha by animateFloatAsState(
        targetValue = if (state.isScrollInProgress) 0.7f else 0f,
        animationSpec = tween(durationMillis = if (state.isScrollInProgress) 100 else 700, delayMillis = if (state.isScrollInProgress) 0 else 500),
        label = "scrollbarAlpha",
    )
    return drawWithContent {
        drawContent()
        val info = state.layoutInfo
        val total = info.totalItemsCount
        val visible = info.visibleItemsInfo.size
        if (alpha > 0.01f && total > visible && visible > 0) {
            val trackHeight = size.height
            val thumbHeight = (trackHeight * visible / total).coerceAtLeast(36.dp.toPx())
            val first = info.visibleItemsInfo.first()
            val partial = if (first.size > 0) (-first.offset).coerceAtLeast(0).toFloat() / first.size else 0f
            val progress = ((state.firstVisibleItemIndex + partial) / (total - visible).coerceAtLeast(1)).coerceIn(0f, 1f)
            val top = (trackHeight - thumbHeight) * progress
            val width = 4.dp.toPx()
            drawRoundRect(
                color = color.copy(alpha = color.alpha * alpha),
                topLeft = Offset(size.width - width - 2.dp.toPx(), top),
                size = Size(width, thumbHeight),
                cornerRadius = CornerRadius(width / 2f, width / 2f),
            )
        }
    }
}
