package skywa1ker404.absencium.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import skywa1ker404.absencium.data.trash.UndoBus
import skywa1ker404.absencium.data.trash.UndoRequest
import skywa1ker404.absencium.logging.DebugLogger

/** The "moved to the Trash. Undo" bar, shown for a few seconds above the navigation pill after any delete. */
@Composable
fun UndoHost(bus: UndoBus, modifier: Modifier = Modifier) {
    val request by bus.current.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    // Kept after the offer is gone so the bar can slide out with its text instead of emptying first.
    var shown by remember { mutableStateOf<UndoRequest?>(null) }
    val current = request
    if (current != null && shown?.id != current.id) shown = current
    LaunchedEffect(current?.id) {
        if (current != null) {
            delay(UNDO_SECONDS * 1000L)
            bus.dismiss(current.id)
        }
    }
    Box(
        modifier = modifier.fillMaxSize().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 96.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = current != null,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
        ) {
            val offer = shown
            if (offer != null) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    shadowElevation = 6.dp,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                    ) {
                        Text(offer.message, style = MaterialTheme.typography.bodyMedium)
                        TextButton(
                            onClick = {
                                bus.dismiss(offer.id)
                                scope.launch {
                                    try {
                                        offer.undo()
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        DebugLogger.e("Undo", "Could not undo", e)
                                    }
                                }
                            },
                        ) { Text("Undo", color = MaterialTheme.colorScheme.inversePrimary) }
                    }
                }
            }
        }
    }
}

private const val UNDO_SECONDS = 6
