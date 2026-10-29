package skywa1ker404.absencium.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
 * Shows why an AI request failed, on whatever screen the student is on, for ten seconds. The screen that asked may be closed by
 * then (a popup, a background import), and the red line inside it is easy to miss; this bar is not.
 */
@Composable
fun AiErrorHost(failures: Flow<String>, modifier: Modifier = Modifier) {
    val q = LocalQuality.current
    var message by remember { mutableStateOf<String?>(null) }
    var shown by remember { mutableStateOf("") }
    var token by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    LaunchedEffect(failures) {
        failures.collect {
            shown = it
            message = it
            token++
        }
    }
    LaunchedEffect(token) {
        if (token > 0) {
            delay(10_000)
            message = null
        }
    }
    Box(
        modifier = modifier.fillMaxSize().statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 8.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn() + slideInVertically(q.spring(dampingRatio = Spring.DampingRatioMediumBouncy)) { -it },
            exit = fadeOut() + slideOutVertically { -it },
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth(),
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
