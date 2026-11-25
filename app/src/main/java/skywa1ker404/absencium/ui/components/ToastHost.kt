package skywa1ker404.absencium.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import skywa1ker404.absencium.ui.theme.RoundedCornerShape

/** One short message for the whole app, in place of the system toast. Anything can post to it; [ToastHost] shows it. */
object ToastBus {
    class Message(val id: Long, val text: String)

    private val _current = MutableStateFlow<Message?>(null)
    val current: StateFlow<Message?> = _current
    private var next = 0L

    fun show(text: String) {
        _current.value = Message(++next, text)
    }

    fun dismiss(id: Long) {
        if (_current.value?.id == id) _current.value = null
    }
}

/** The app's one message bar: the same look as the Undo bar, a few seconds above the navigation pill. */
@Composable
fun ToastHost(modifier: Modifier = Modifier) {
    val message by ToastBus.current.collectAsStateWithLifecycle()
    var shown by remember { mutableStateOf<ToastBus.Message?>(null) }
    val current = message
    if (current != null && shown?.id != current.id) shown = current
    LaunchedEffect(current?.id) {
        if (current != null) {
            delay(3500L)
            ToastBus.dismiss(current.id)
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
            val text = shown?.text
            if (text != null) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    shadowElevation = 0.dp,
                ) {
                    Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp))
                }
            }
        }
    }
}
