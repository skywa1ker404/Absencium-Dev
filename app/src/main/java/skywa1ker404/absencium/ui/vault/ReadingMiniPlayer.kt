package skywa1ker404.absencium.ui.vault

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import skywa1ker404.absencium.MiniReading
import skywa1ker404.absencium.ui.components.glassPane
import skywa1ker404.absencium.ui.icons.AppIcons
import kotlin.math.roundToInt

/**
 * The minimised reading player, docked like a music mini-player: a bar across the bottom, just above the navigation pill, with
 * the note, where it is filed, a thin progress line for the page reached, an Open button and a Close button. Tap it or swipe it
 * up to open the note again; swipe it down or press Close to put it away. It never floats around the screen.
 */
@Composable
fun ReadingMiniPlayer(mini: MiniReading?, onOpen: (Long) -> Unit, onDismiss: () -> Unit) {
    if (mini == null) return
    val open by rememberUpdatedState(onOpen)
    val dismiss by rememberUpdatedState(onDismiss)
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(20.dp)
    val offsetY = remember(mini.noteId) { Animatable(0f) }
    val snap = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
    val progress = if (mini.pages > 0) (mini.page.toFloat() / mini.pages).coerceIn(0f, 1f) else 0f

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 12.dp, end = 12.dp, bottom = 96.dp)
                .fillMaxWidth()
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .glassPane(shape, colors.surfaceContainerHigh, 0.92f)
                .pointerInput(mini.noteId) {
                    var total = 0f
                    detectVerticalDragGestures(
                        onDragStart = { total = 0f },
                        onVerticalDrag = { change, amount ->
                            change.consume()
                            total += amount
                            scope.launch { offsetY.snapTo(total.coerceAtLeast(-24f)) }
                        },
                        onDragEnd = {
                            when {
                                total > 70f -> dismiss()
                                total < -70f -> open(mini.noteId)
                            }
                            scope.launch { offsetY.animateTo(0f, snap) }
                        },
                        onDragCancel = { scope.launch { offsetY.animateTo(0f, snap) } },
                    )
                }
                .clickable { open(mini.noteId) }
                .semantics { contentDescription = "Reading ${mini.title}. Tap to open." },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(64.dp).padding(start = 10.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(colors.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) { Icon(AppIcons.Book, contentDescription = null, tint = colors.onPrimaryContainer) }
                Column(modifier = Modifier.weight(1f)) {
                    Text(mini.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOf(mini.place, "Page ${mini.page} of ${mini.pages}").filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = { open(mini.noteId) }) { Icon(AppIcons.Book, contentDescription = "Continue reading") }
                IconButton(onClick = { dismiss() }) { Icon(AppIcons.Close, contentDescription = "Close the player") }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = colors.primary,
                trackColor = colors.onSurface.copy(alpha = 0.10f),
                drawStopIndicator = {},
            )
        }
    }
}
