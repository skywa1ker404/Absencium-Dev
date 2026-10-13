package com.trajectory.vault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.isRuntimeShaderSupported
import com.kyant.backdrop.shadow.Shadow
import com.trajectory.vault.ui.theme.LocalGlassBlur
import com.trajectory.vault.ui.theme.LocalGlassEnabled
import com.trajectory.vault.ui.theme.LocalQuality

/**
 * The look of a floating element (top bar, floating navigation bar). With a [backdrop] (Liquid Glass
 * in Quality mode on Android 12+) it is plain, colourless glass built on the Backdrop library: it
 * blurs what scrolls behind it and, on Android 13+, bends that content like a lens. Without one it is
 * a solid [tint] surface.
 */
@Composable
fun Modifier.floatingGlass(
    backdrop: LayerBackdrop?,
    shape: Shape,
    tint: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    bordered: Boolean = true,
): Modifier {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    if (backdrop == null) {
        // Solid look (Solid Accent, Monet, or glass switched off): an opaque outline, so it never reads as glass.
        val border = if (bordered) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape) else Modifier
        return this.clip(shape).background(tint).then(border)
    }
    val quality = LocalQuality.current
    val veil = if (isDark) Color(0xFF121212).copy(alpha = 0.32f) else Color(0xFFFAFAFA).copy(alpha = 0.38f)
    val blurDp = LocalGlassBlur.current
    // Lens refraction needs rounded corners and Android 13 (a runtime shader); a plain rectangle only blurs.
    val lens = quality.glassLens && shape is CornerBasedShape && isRuntimeShaderSupported()
    val rim = if (bordered && !quality.glassHighlight) {
        Modifier.border(1.dp, Color.White.copy(alpha = if (isDark) 0.16f else 0.55f), shape)
    } else {
        Modifier
    }
    return this
        .drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(blurDp.toPx())
                if (lens) {
                    lens(refractionHeight = 12.dp.toPx(), refractionAmount = 24.dp.toPx())
                }
            },
            highlight = if (bordered && quality.glassHighlight) ({ Highlight.Default }) else null,
            shadow = if (bordered && quality.glassHighlight) ({ Shadow.Default }) else null,
            // Plain glass: no colour of its own, only a faint light or dark veil so it reads on any content.
            onDrawSurface = { drawRect(veil) },
        )
        .then(rim)
}

/**
 * Background of bottom sheets (search, profile, entry forms). With the Liquid Glass engine the
 * sheet is translucent and tinted by the wallpaper (Monet) colours, so the app shows through it;
 * otherwise it is the plain themed surface.
 */
@Composable
fun sheetContainerColor(): Color {
    val colors = MaterialTheme.colorScheme
    return if (LocalGlassEnabled.current) {
        lerp(colors.surfaceContainerHigh, colors.primaryContainer, 0.22f).copy(alpha = 0.80f)
    } else {
        colors.surfaceContainerLow
    }
}

/** A lighter scrim for glass sheets so the content behind them stays visible. */
@Composable
fun sheetScrimColor(): Color =
    Color.Black.copy(alpha = if (LocalGlassEnabled.current) 0.2f else 0.32f)
