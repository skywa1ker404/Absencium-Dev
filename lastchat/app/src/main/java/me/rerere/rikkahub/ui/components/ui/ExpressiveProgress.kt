package me.rerere.rikkahub.ui.components.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp

/**
 * Absencium's waiting indicator inside the chat: the Material 3 Expressive loader whose shape morphs from one to the next, in place
 * of the plain spinning ring. Calls without a `progress` land here; the ones with a real progress value keep the Material ring below.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
@Suppress("UNUSED_PARAMETER")
fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    strokeWidth: Dp = Dp.Unspecified,
    trackColor: Color = Color.Unspecified,
    strokeCap: StrokeCap = StrokeCap.Round,
) {
    if (color == Color.Unspecified) LoadingIndicator(modifier = modifier) else LoadingIndicator(modifier = modifier, color = color)
}

/** A real progress value: the determinate Material ring. */
@Composable
fun CircularProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = androidx.compose.material3.ProgressIndicatorDefaults.circularColor,
    strokeWidth: Dp = androidx.compose.material3.ProgressIndicatorDefaults.CircularStrokeWidth,
    trackColor: Color = androidx.compose.material3.ProgressIndicatorDefaults.circularDeterminateTrackColor,
    strokeCap: StrokeCap = androidx.compose.material3.ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
) {
    androidx.compose.material3.CircularProgressIndicator(progress = progress, modifier = modifier, color = color, strokeWidth = strokeWidth, trackColor = trackColor, strokeCap = strokeCap)
}
