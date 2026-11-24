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
fun LoadingIndicator(modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    val slot = HubStyle.loading
    if (slot != null) slot(modifier)
    else if (color == Color.Unspecified) androidx.compose.material3.LoadingIndicator(modifier = modifier)
    else androidx.compose.material3.LoadingIndicator(modifier = modifier, color = color)
}

/*
 * Buttons: Material's, but in the Pixelated style they are pixel circles with the host's single outline. Same names as Material's.
 */

@Composable
private fun buttonShape(shape: androidx.compose.ui.graphics.Shape): androidx.compose.ui.graphics.Shape =
    if (HubStyle.pixelated() && shape == androidx.compose.material3.ButtonDefaults.shape) HubStyle.pixelCircle?.invoke() ?: shape else shape

@Composable
private fun buttonModifier(modifier: Modifier): Modifier =
    if (HubStyle.pixelated()) HubStyle.bevel?.invoke(modifier, androidx.compose.material3.MaterialTheme.colorScheme.outline) ?: modifier else modifier

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.material3.ButtonDefaults.shape,
    colors: androidx.compose.material3.ButtonColors = androidx.compose.material3.ButtonDefaults.buttonColors(),
    elevation: androidx.compose.material3.ButtonElevation? = androidx.compose.material3.ButtonDefaults.buttonElevation(),
    border: androidx.compose.foundation.BorderStroke? = null,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.material3.ButtonDefaults.ContentPadding,
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource? = null,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) = androidx.compose.material3.Button(onClick, buttonModifier(modifier), enabled, buttonShape(shape), colors, elevation, border, contentPadding, interactionSource, content)

@Composable
fun FilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.material3.ButtonDefaults.filledTonalShape,
    colors: androidx.compose.material3.ButtonColors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(),
    elevation: androidx.compose.material3.ButtonElevation? = androidx.compose.material3.ButtonDefaults.filledTonalButtonElevation(),
    border: androidx.compose.foundation.BorderStroke? = null,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.material3.ButtonDefaults.ContentPadding,
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource? = null,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) = androidx.compose.material3.FilledTonalButton(onClick, buttonModifier(modifier), enabled, buttonShape(shape), colors, elevation, border, contentPadding, interactionSource, content)

@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.material3.ButtonDefaults.outlinedShape,
    colors: androidx.compose.material3.ButtonColors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(),
    elevation: androidx.compose.material3.ButtonElevation? = null,
    border: androidx.compose.foundation.BorderStroke? = androidx.compose.material3.ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.material3.ButtonDefaults.ContentPadding,
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource? = null,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) = androidx.compose.material3.OutlinedButton(onClick, buttonModifier(modifier), enabled, buttonShape(shape), colors, elevation, border, contentPadding, interactionSource, content)
