package me.rerere.rikkahub.ui.components.ui

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import me.rerere.rikkahub.ui.theme.HubStyle

/*
 * The chat's slider, bars and loader. Inside Absencium they are the app's own controls, so Pixelated and Dot matrix reach the chat;
 * without a host they are Material's. Same names as Material's so a call site only changes its import.
 */

@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val slot = HubStyle.slider
    if (slot != null) {
        slot(value, onValueChange, modifier, enabled, valueRange, steps, onValueChangeFinished)
    } else {
        androidx.compose.material3.Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            valueRange = valueRange,
            steps = steps,
            onValueChangeFinished = onValueChangeFinished,
        )
    }
}

@Composable
fun LinearProgressIndicator(
    progress: (() -> Float)? = null,
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.linearColor,
    trackColor: Color = ProgressIndicatorDefaults.linearTrackColor,
) {
    val slot = HubStyle.bar
    if (slot != null) {
        slot(progress, modifier, false)
    } else if (progress != null) {
        androidx.compose.material3.LinearProgressIndicator(progress = progress, modifier = modifier, color = color, trackColor = trackColor)
    } else {
        androidx.compose.material3.LinearProgressIndicator(modifier = modifier, color = color, trackColor = trackColor)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LinearWavyProgressIndicator(modifier: Modifier = Modifier) {
    val slot = HubStyle.bar
    if (slot != null) slot(null, modifier, true) else androidx.compose.material3.LinearWavyProgressIndicator(modifier = modifier)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    val slot = HubStyle.loading
    if (slot != null) slot(modifier) else androidx.compose.material3.LoadingIndicator(modifier = modifier)
}
