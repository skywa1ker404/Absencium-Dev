package skywa1ker404.absencium.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import skywa1ker404.absencium.ui.icons.AppIcons
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Swipe a row to the left to delete it. The row follows the finger, the red layer behind it grows and its bin pops, and past a third of
 * the width the row springs off the screen and [onDelete] runs (the delete goes to the Trash with Undo). Let go earlier and it springs back.
 */
@Composable
fun SwipeToDelete(onDelete: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val offset = remember { Animatable(0f) }
    var width by mutableIntStateOf(1)
    val progress = (abs(offset.value) / (width / 3f)).coerceIn(0f, 1f)
    Box(modifier = modifier.fillMaxWidth().onSizeChanged { width = it.width.coerceAtLeast(1) }) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(MaterialTheme.shapes.large)
                .background(colors.errorContainer.copy(alpha = progress)),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Icon(
                AppIcons.Delete,
                contentDescription = null,
                tint = colors.onErrorContainer,
                modifier = Modifier
                    .padding(end = 20.dp)
                    .size(24.dp)
                    .graphicsLayer {
                        val s = 0.6f + 0.6f * progress
                        scaleX = s
                        scaleY = s
                        alpha = progress
                    },
            )
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    var passed = false
                    detectHorizontalDragGestures(
                        onDragStart = { passed = false },
                        onDragEnd = {
                            scope.launch {
                                if (offset.value < -width / 3f) {
                                    offset.animateTo(-width.toFloat(), spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium))
                                    onDelete()
                                    // The row is gone from the list; if it stays (a failed delete), bring it back.
                                    offset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
                                } else {
                                    offset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
                                }
                            }
                        },
                        onDragCancel = { scope.launch { offset.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) } },
                    ) { change, drag ->
                        change.consume()
                        // Only leftwards, with a little resistance past the point of no return.
                        val next = (offset.value + drag).coerceIn(-width.toFloat(), 0f)
                        scope.launch { offset.snapTo(next) }
                        val now = next < -width / 3f
                        if (now && !passed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        passed = now
                    }
                },
        ) { content() }
    }
}
