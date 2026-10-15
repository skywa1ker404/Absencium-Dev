package com.absencium.app.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.absencium.app.R
import com.absencium.app.ui.components.GlassStage
import com.absencium.app.ui.components.LocalCardBackdrop
import com.absencium.app.ui.components.glassSurface
import com.absencium.app.ui.theme.LocalGlassEnabled

/**
 * The Absencium logo in the current theme: a rounded square in the theme's accent container with the A on it. With
 * Liquid Glass on, the square is a pane of glass over the wallpaper instead. The splash, the first-run setup and the
 * tour all draw it, so it changes with the theme options chosen.
 *
 * [progress] runs from 0 to 1 while the splash grows it out of a circle; 1 is the finished logo.
 */
@Composable
fun AbsenciumMark(modifier: Modifier = Modifier, size: Dp = 132.dp, progress: Float = 1f) {
    val colors = MaterialTheme.colorScheme
    val glass = LocalGlassEnabled.current
    val backdrop = LocalCardBackdrop.current
    val radius = (size.value * 0.5f - size.value * 0.288f * progress).coerceAtLeast(size.value * 0.075f).dp
    val shape = RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                val s = 0.55f + 0.45f * progress
                scaleX = s
                scaleY = s
                alpha = (progress * 2f).coerceIn(0f, 1f)
            }
            .clip(shape)
            .then(
                if (glass && backdrop != null) {
                    Modifier.glassSurface(backdrop, shape, colors.primaryContainer, 0.5f)
                } else {
                    Modifier.background(colors.primaryContainer)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_logo_a),
            contentDescription = "Absencium",
            colorFilter = ColorFilter.tint(colors.onPrimaryContainer),
            modifier = Modifier
                .size(size / 2.06f)
                .graphicsLayer { rotationZ = (1f - progress) * -22f },
        )
    }
}

/**
 * The themed backdrop of a full screen outside the main app (splash, setup, tour): the wallpaper-like glass stage when
 * Liquid Glass is on, the plain themed background otherwise.
 */
@Composable
fun ThemedStage(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val glass = LocalGlassEnabled.current
    val background = MaterialTheme.colorScheme.background
    // Always the same stage: with glass it supplies the wallpaper, without it a plain themed background.
    GlassStage(modifier = if (glass) modifier else modifier.background(background)) { content() }
}
