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

/** The timers of the Study Tools page: Pomodoro rounds, an open stopwatch, and two plain break countdowns. */
object FocusMode {
    const val POMODORO = "pomodoro"
    const val STOPWATCH = "stopwatch"
    const val BREAK_10 = "break10"
    const val BREAK_15 = "break15"
}

/** Runs while this page is open. [mode] is one of [FocusMode]. */
@Destination<RootGraph>
@Composable
fun FocusTimerScreen(navigator: DestinationsNavigator, mode: String = FocusMode.POMODORO) {
    val colors = MaterialTheme.colorScheme
    val stopwatch = mode == FocusMode.STOPWATCH
    val breakOnly = mode == FocusMode.BREAK_10 || mode == FocusMode.BREAK_15
    val start = when (mode) {
        FocusMode.BREAK_10 -> 10 * 60
        FocusMode.BREAK_15 -> 15 * 60
        FocusMode.STOPWATCH -> 0
        else -> FOCUS_SECONDS
    }
    var focus by remember { mutableStateOf(!breakOnly) }
    var left by remember { mutableIntStateOf(start) }
    var running by remember { mutableStateOf(false) }
    var rounds by remember { mutableIntStateOf(0) }

    LaunchedEffect(running, focus) {
        while (running) {
            delay(1000)
            if (stopwatch) {
                left += 1
                continue
            }
            left -= 1
            if (left <= 0) {
                if (breakOnly) {
                    left = 0
                    running = false
                } else {
                    if (focus) rounds++
                    focus = !focus
                    left = if (focus) FOCUS_SECONDS else BREAK_SECONDS
                }
            }
        }
    }
    val total = when {
        stopwatch -> 60 * 60
        breakOnly -> start
        focus -> FOCUS_SECONDS
        else -> BREAK_SECONDS
    }
    val progress by animateFloatAsState(
        targetValue = if (stopwatch) (left % 3600).toFloat() / total else 1f - left.toFloat() / total,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "focusProgress",
    )
    val hue = skywa1ker404.absencium.ui.components.hueFor(
        when (mode) { FocusMode.STOPWATCH -> 1L; FocusMode.BREAK_10 -> 2L; FocusMode.BREAK_15 -> 3L; else -> if (focus) 0L else 2L },
    )
    val track = hue.accent.copy(alpha = 0.2f)
    val arc = hue.accent

    ScreenScaffold(title = when (mode) { FocusMode.STOPWATCH -> "Stopwatch"; FocusMode.BREAK_10 -> "10-minute break"; FocusMode.BREAK_15 -> "15-minute break"; else -> "Pomodoro" }, onBack = { navigator.navigateUp() }, largeTitle = true) {
        AppCard(modifier = Modifier.fillMaxWidth(), container = hue.container, shape = MaterialTheme.shapes.extraLarge) {
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
                        Text(if (left >= 3600) "%d:%02d:%02d".format(left / 3600, left % 3600 / 60, left % 60) else "%02d:%02d".format(left / 60, left % 60), style = MaterialTheme.typography.displayMedium, color = hue.onContainer)
                        Text(if (stopwatch) "Studying" else if (focus) "Focus" else "Break", style = MaterialTheme.typography.titleMedium, color = hue.onContainer.copy(alpha = 0.8f))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton(text = if (running) "Pause" else "Start", onClick = { running = !running })
                    PillButton(
                        text = "Reset", tonal = true,
                        onClick = { running = false; focus = !breakOnly; left = start },
                    )
                }
                if (mode == FocusMode.POMODORO) Text("$rounds rounds finished this session", style = MaterialTheme.typography.bodySmall, color = hue.onContainer.copy(alpha = 0.8f))
            }
        }
        HintText("Keep this page open while the timer runs.", modifier = Modifier.padding(top = 8.dp))
    }
}
