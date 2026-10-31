package skywa1ker404.absencium.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import skywa1ker404.absencium.ui.theme.LocalQuality

/**
 * Shows why an AI request failed, on whatever screen the student is on, at the bottom above the navigation pill, for five seconds.
 * Swipe it left or right to dismiss it sooner. The screen that asked may be closed by then (a popup, a background import), and a
 * red line inside it is easy to miss; this card is not. It is the only place an AI failure is spelled out.
 */
@Composable
fun AiErrorHost(failures: Flow<String>, modifier: Modifier = Modifier) {
    val q = LocalQuality.current
    var message by remember { mutableStateOf<String?>(null) }
    var shown by remember { mutableStateOf("") }
    var token by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(failures) {
        failures.collect {
            shown = it
            message = it
            token++
        }
    }
    LaunchedEffect(token) {
        if (token > 0) {
            delay(AUTO_CLOSE_MS)
            message = null
        }
    }
    Box(
        modifier = modifier.fillMaxSize().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 104.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn() + slideInVertically(q.spring(dampingRatio = Spring.DampingRatioMediumBouncy)) { it },
            exit = fadeOut() + slideOutVertically { it },
        ) {
            // A fresh swipe state for every new failure, so a card that was swiped away does not come back already moved.
            val offset = remember(token) { Animatable(0f) }
            var width by remember { mutableIntStateOf(1) }
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { width = it.width.coerceAtLeast(1) }
                    .offset { IntOffset(offset.value.roundToInt(), 0) }
                    .graphicsLayer { alpha = (1f - abs(offset.value) / width).coerceIn(0f, 1f) }
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta -> scope.launch { offset.snapTo(offset.value + delta) } },
                        onDragStopped = { velocity ->
                            val away = abs(offset.value) > width * 0.3f || abs(velocity) > 1_800f
                            if (away) {
                                val target = (if (offset.value != 0f) offset.value else velocity).let { if (it < 0f) -width * 1.2f else width * 1.2f }
                                offset.animateTo(target, spring(stiffness = Spring.StiffnessMedium))
                                message = null
                            } else {
                                offset.animateTo(0f, q.spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                            }
                        },
                    ),
            ) {
                Column(modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 4.dp)) {
                    Text("The AI request failed", style = MaterialTheme.typography.titleSmall)
                    Text(
                        shown,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager)
                                .setPrimaryClip(android.content.ClipData.newPlainText("AI error", shown)) }) { Text("Copy") }
                        TextButton(onClick = { message = null }) { Text("Close") }
                    }
                }
            }
        }
    }
}

private const val AUTO_CLOSE_MS = 5_000L
