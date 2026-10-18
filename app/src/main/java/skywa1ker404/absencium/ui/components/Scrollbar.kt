package skywa1ker404.absencium.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/** How wide the strip at the right edge is that grabs the scrollbar. */
private val GrabWidth = 28.dp

/**
 * A thin scrollbar for a lazy list. It fades in while the list moves and fades out a moment after it stops, and it can be
 * dragged: touch the strip along the right edge and move up or down to scroll quickly. The thumb position follows the
 * first visible item (rows are of similar size), so it is a guide rather than an exact measure.
 */
@Composable
fun Modifier.verticalScrollbar(state: LazyListState, color: Color): Modifier {
    var dragging by remember { mutableStateOf(false) }
    val visible = state.isScrollInProgress || dragging
    val alpha by animateFloatAsState(
        targetValue = if (visible) (if (dragging) 0.9f else 0.7f) else 0f,
        animationSpec = tween(durationMillis = if (visible) 100 else 700, delayMillis = if (visible) 0 else 500),
        label = "scrollbarAlpha",
    )
    return this
        .pointerInput(state) {
            // Looks at the touch before the list does, so a drag that starts on the edge strip moves the scrollbar and not the text.
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (down.position.x < size.width - GrabWidth.toPx()) return@awaitEachGesture
                val info = state.layoutInfo
                val total = info.totalItemsCount
                val shown = info.visibleItemsInfo
                if (total <= shown.size || shown.isEmpty()) return@awaitEachGesture
                // One pixel of finger travel moves the list by (content height / track height) pixels.
                val averageItem = shown.sumOf { it.size }.toFloat() / shown.size
                val ratio = (averageItem * total / size.height.toFloat()).coerceAtLeast(1f)
                down.consume()
                dragging = true
                var last = down.position.y
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    val dy = change.position.y - last
                    last = change.position.y
                    if (dy != 0f) {
                        state.dispatchRawDelta(dy * ratio)
                        change.consume()
                    }
                }
                dragging = false
            }
        }
        .drawWithContent {
            drawContent()
            val info = state.layoutInfo
            val total = info.totalItemsCount
            val shown = info.visibleItemsInfo.size
            if (alpha > 0.01f && total > shown && shown > 0) {
                val trackHeight = size.height
                val thumbHeight = (trackHeight * shown / total).coerceAtLeast(36.dp.toPx())
                val first = info.visibleItemsInfo.first()
                val partial = if (first.size > 0) (-first.offset).coerceAtLeast(0).toFloat() / first.size else 0f
                val progress = ((state.firstVisibleItemIndex + partial) / (total - shown).coerceAtLeast(1)).coerceIn(0f, 1f)
                val top = (trackHeight - thumbHeight) * progress
                val width = if (dragging) 8.dp.toPx() else 4.dp.toPx()
                drawRoundRect(
                    color = color.copy(alpha = color.alpha * alpha),
                    topLeft = Offset(size.width - width - 2.dp.toPx(), top),
                    size = Size(width, thumbHeight),
                    cornerRadius = CornerRadius(width / 2f, width / 2f),
                )
            }
        }
}
