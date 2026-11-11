package skywa1ker404.absencium.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.components.ScreenScaffold

private const val FOCUS_SECONDS = 25 * 60
private const val BREAK_SECONDS = 5 * 60

/** A study round of 25 minutes followed by a 5 minute break. It runs while this page is open. */
@Destination<RootGraph>
@Composable
fun FocusTimerScreen(navigator: DestinationsNavigator) {
    val colors = MaterialTheme.colorScheme
    var focus by remember { mutableStateOf(true) }
    var left by remember { mutableIntStateOf(FOCUS_SECONDS) }
    var running by remember { mutableStateOf(false) }
    var rounds by remember { mutableIntStateOf(0) }

    LaunchedEffect(running, focus) {
        while (running) {
            delay(1000)
            left -= 1
            if (left <= 0) {
                if (focus) rounds++
                focus = !focus
                left = if (focus) FOCUS_SECONDS else BREAK_SECONDS
            }
        }
    }
    val total = if (focus) FOCUS_SECONDS else BREAK_SECONDS
    val progress by animateFloatAsState(
        targetValue = 1f - left.toFloat() / total,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "focusProgress",
    )
    val track = colors.surfaceContainerHighest
    val arc = if (focus) colors.primary else colors.tertiary

    ScreenScaffold(title = "Focus timer", onBack = { navigator.navigateUp() }, largeTitle = true) {
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(220.dp)) {
                        val stroke = 16.dp.toPx()
                        val inset = stroke / 2
                        val box = Size(size.width - stroke, size.height - stroke)
                        drawArc(track, 0f, 360f, false, Offset(inset, inset), box, style = Stroke(stroke, cap = StrokeCap.Round))
                        drawArc(arc, -90f, 360f * progress, false, Offset(inset, inset), box, style = Stroke(stroke, cap = StrokeCap.Round))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("%02d:%02d".format(left / 60, left % 60), style = MaterialTheme.typography.displayMedium)
                        Text(if (focus) "Focus" else "Break", style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton(text = if (running) "Pause" else "Start", onClick = { running = !running })
                    PillButton(
                        text = "Reset", tonal = true,
                        onClick = { running = false; focus = true; left = FOCUS_SECONDS },
                    )
                }
                Text("$rounds rounds finished this session", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
        HintText("Keep this page open while the timer runs.", modifier = Modifier.padding(top = 8.dp))
    }
}
