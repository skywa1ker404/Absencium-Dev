package skywa1ker404.absencium.ui.vault

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import skywa1ker404.absencium.MiniReading
import skywa1ker404.absencium.ui.icons.AppIcons
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The minimised reading player. A card above the navigation pill shows the note, where it is filed and the page reached.
 * Swipe it left or right and it becomes a half-hidden bubble on that edge that can be dragged anywhere (drop it on the X to
 * close it); swipe it down to close it; tap it or swipe it up to open the note again.
 */
@Composable
fun ReadingMiniPlayer(mini: MiniReading?, onOpen: (Long) -> Unit, onDismiss: () -> Unit) {
    if (mini == null) return
    val open by rememberUpdatedState(onOpen)
    val dismiss by rememberUpdatedState(onDismiss)
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var bubble by remember(mini.noteId) { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val bubblePx = with(density) { 56.dp.toPx() }
        val cardOffsetY = remember(mini.noteId) { Animatable(0f) }
        val cardOffsetX = remember(mini.noteId) { Animatable(0f) }
        val bubbleX = remember(mini.noteId) { Animatable(widthPx - bubblePx / 2f) }
        val bubbleY = remember(mini.noteId) { Animatable(heightPx * 0.55f) }
        val snap = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)

        if (!bubble) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colors.surfaceContainerHigh,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 96.dp)
                    .offset { IntOffset(cardOffsetX.value.roundToInt(), cardOffsetY.value.roundToInt()) }
                    .pointerInput(mini.noteId) {
                        var total = Offset.Zero
                        detectDragGestures(
                            onDragStart = { total = Offset.Zero },
                            onDrag = { change, amount ->
                                change.consume()
                                total += amount
                                scope.launch { cardOffsetX.snapTo(total.x); cardOffsetY.snapTo(total.y.coerceAtLeast(-40f)) }
                            },
                            onDragEnd = {
                                val horizontal = abs(total.x) > abs(total.y)
                                when {
                                    horizontal && abs(total.x) > 90f -> {
                                        bubble = true
                                        scope.launch {
                                            bubbleX.snapTo(if (total.x > 0) widthPx - bubblePx / 2f else -bubblePx / 2f)
                                            bubbleY.snapTo(heightPx - with(density) { 200.dp.toPx() })
                                        }
                                    }
                                    !horizontal && total.y > 90f -> dismiss()
                                    !horizontal && total.y < -90f -> open(mini.noteId)
                                }
                                scope.launch { cardOffsetX.animateTo(0f, snap) }
                                scope.launch { cardOffsetY.animateTo(0f, snap) }
                            },
                            onDragCancel = {
                                scope.launch { cardOffsetX.animateTo(0f, snap) }
                                scope.launch { cardOffsetY.animateTo(0f, snap) }
                            },
                        )
                    }
                    .clickable { open(mini.noteId) },
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(colors.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) { Icon(AppIcons.Book, contentDescription = null, tint = colors.onPrimaryContainer) }
                    Column(modifier = Modifier.width(220.dp)) {
                        Text(mini.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            listOf(mini.place, "Page ${mini.page} of ${mini.pages}").filter { it.isNotBlank() }.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        } else {
            val targetCentre = Offset(widthPx / 2f, heightPx - with(density) { 120.dp.toPx() })
            val bubbleCentre = Offset(bubbleX.value + bubblePx / 2f, bubbleY.value + bubblePx / 2f)
            val overTarget = dragging && (bubbleCentre - targetCentre).getDistance() < with(density) { 72.dp.toPx() }
            if (dragging) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 92.dp)
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (overTarget) colors.error else colors.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) { Icon(AppIcons.Close, contentDescription = "Drop here to close", tint = if (overTarget) colors.onError else colors.onSurface) }
            }
            Box(
                modifier = Modifier
                    .offset { IntOffset(bubbleX.value.roundToInt(), bubbleY.value.roundToInt()) }
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colors.primaryContainer)
                    .pointerInput(mini.noteId) {
                        detectDragGestures(
                            onDragStart = { dragging = true },
                            onDrag = { change, amount ->
                                change.consume()
                                scope.launch {
                                    bubbleX.snapTo((bubbleX.value + amount.x).coerceIn(-bubblePx / 2f, widthPx - bubblePx / 2f))
                                    bubbleY.snapTo((bubbleY.value + amount.y).coerceIn(0f, heightPx - bubblePx))
                                }
                            },
                            onDragEnd = {
                                dragging = false
                                val centre = Offset(bubbleX.value + bubblePx / 2f, bubbleY.value + bubblePx / 2f)
                                if ((centre - targetCentre).getDistance() < 72.dp.toPx()) {
                                    dismiss()
                                } else {
                                    // Settle half-hidden on the nearer edge.
                                    val edge = if (centre.x < widthPx / 2f) -bubblePx / 2f else widthPx - bubblePx / 2f
                                    scope.launch { bubbleX.animateTo(edge, snap) }
                                }
                            },
                            onDragCancel = { dragging = false },
                        )
                    }
                    .clickable { open(mini.noteId) },
                contentAlignment = Alignment.Center,
            ) { Icon(AppIcons.Book, contentDescription = "Open ${mini.title}", tint = colors.onPrimaryContainer) }
        }
    }
}
