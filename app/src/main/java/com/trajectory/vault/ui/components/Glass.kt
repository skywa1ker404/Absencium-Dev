package com.trajectory.vault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.trajectory.vault.ui.theme.LocalGlassBlur
import dev.chrisbanes.haze.*
import dev.chrisbanes.haze.blur.*

/**
 * The look of a floating element (top bar, floating navigation bar). With a [hazeState] (Liquid Glass
 * on Android 12+) it is frosted glass that blurs whatever scrolls behind it; otherwise it is a
 * solid [tint] surface.
 */
@Composable
fun Modifier.floatingGlass(
    hazeState: HazeState?,
    shape: Shape,
    tint: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    bordered: Boolean = true,
): Modifier {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val borderModifier = if (bordered) {
        Modifier.border(1.dp, Color.White.copy(alpha = if (isDark) 0.16f else 0.55f), shape)
    } else {
        Modifier
    }
    if (hazeState == null) {
        return this.clip(shape).background(tint).then(borderModifier)
    }
    val blur = LocalGlassBlur.current
    val style = remember(tint, blur) {
        HazeBlurStyle {
            blurRadius(blur)
            noiseFactor(0.06f)
            colorEffects(listOf(HazeColorEffect.tint(tint.copy(alpha = 0.55f))))
            fallbackColorEffect(HazeColorEffect.tint(tint.copy(alpha = 0.92f)))
        }
    }
    return this
        .clip(shape)
        .hazeBlur(input = HazeInput.Backdrop(hazeState), style = style)
        .then(borderModifier)
}
