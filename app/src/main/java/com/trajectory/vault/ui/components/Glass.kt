package com.trajectory.vault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
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
        // Solid look (Monet, Default, a preset, or glass switched off): a plain tonal surface, no outline.
        return this.clip(shape).background(tint)
    }
    val quality = LocalQuality.current
    // The glass takes the colours of the active theme: a veil of the surface colour, nudged toward its accent container.
    val scheme = MaterialTheme.colorScheme
    val veilBase = lerp(scheme.surface, scheme.primaryContainer, 0.18f)
    val veil = veilBase.copy(alpha = if (isDark) 0.42f else 0.5f)
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
                if (blurDp.value > 0f) blur(blurDp.toPx())
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

/** The backdrop that cards and tiles sample: only the wallpaper behind the content, never the content itself. */
val LocalCardBackdrop = androidx.compose.runtime.staticCompositionLocalOf<LayerBackdrop?> { null }

/**
 * Glass for content surfaces (cards, tiles). It blurs only the wallpaper layer, so a card never samples itself
 * or the cards around it, and it is lighter than the floating bars: blur and a soft edge, no refraction.
 */
@Composable
fun Modifier.glassSurface(backdrop: LayerBackdrop, shape: Shape, tint: Color, tintAlpha: Float = 0.55f): Modifier {
    val blurDp = LocalGlassBlur.current
    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            if (blurDp.value > 0f) blur(blurDp.toPx())
        },
        highlight = { Highlight.Default },
        onDrawSurface = { drawRect(tint.copy(alpha = tintAlpha)) },
    )
}

/**
 * A control (button, switch, segmented toggle) in the theme's [tint]. With Liquid Glass it is a pane of glass over
 * the wallpaper, tinted with [tint] at [tintAlpha]; otherwise a plain tonal fill.
 */
@Composable
fun Modifier.glassControl(shape: Shape, tint: Color, tintAlpha: Float = 0.7f): Modifier {
    val backdrop = LocalCardBackdrop.current
    return if (backdrop != null) glassSurface(backdrop, shape, tint, tintAlpha) else this.clip(shape).background(tint)
}

/** The wallpaper behind everything in glass mode: soft blobs of the theme's own colours for the glass to blur. */
@Composable
fun GlassWallpaper(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val a = scheme.primaryContainer
    val b = scheme.tertiaryContainer
    val c = scheme.secondaryContainer
    val base = scheme.background
    Box(
        modifier = modifier
            .background(base)
            .drawBehind {
                val w = size.width
                val h = size.height
                fun blob(color: Color, cx: Float, cy: Float, r: Float) = drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.95f), color.copy(alpha = 0f)),
                        center = Offset(cx, cy),
                        radius = r,
                    ),
                    radius = r,
                    center = Offset(cx, cy),
                )
                blob(a, w * 0.15f, h * 0.12f, w * 0.9f)
                blob(b, w * 0.95f, h * 0.45f, w * 0.85f)
                blob(c, w * 0.2f, h * 0.85f, w * 0.9f)
            },
    )
}

/**
 * Wraps a screen's content in glass mode: draws the [GlassWallpaper] into its own layer and lets cards sample it.
 * Without glass it is transparent to its content.
 */
@Composable
fun GlassStage(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    if (!LocalGlassEnabled.current) {
        Box(modifier = modifier) { content() }
        return
    }
    val wallpaper = rememberLayerBackdrop()
    Box(modifier = modifier) {
        GlassWallpaper(Modifier.matchParentSize().layerBackdrop(wallpaper))
        CompositionLocalProvider(LocalCardBackdrop provides wallpaper) { content() }
    }
}

/**
 * The selected tab of the glass navigation pill. In Quality mode it is a clear lens: it does not blur, it bends
 * what is behind it, so the pill looks like glass with a bead of clearer glass sliding along it. In Performance
 * mode, or without runtime shaders (below Android 13), it is a plain faint puck. Never coloured.
 */
@Composable
fun Modifier.glassPuck(backdrop: LayerBackdrop?, shape: Shape, isDark: Boolean): Modifier {
    val quality = LocalQuality.current
    val faint = if (isDark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.08f)
    if (backdrop == null || !quality.glassLens || !isRuntimeShaderSupported() || shape !is CornerBasedShape) {
        return this.clip(shape).background(faint)
    }
    val sheen = if (isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.22f)
    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            lens(refractionHeight = 8.dp.toPx(), refractionAmount = 16.dp.toPx())
        },
        highlight = { Highlight.Default },
        onDrawSurface = { drawRect(sheen) },
    )
}

/**
 * Background of bottom sheets (search, profile, entry forms). With Liquid Glass it is the theme's tinted background,
 * and the sheet's cards become glass over its own wallpaper (see [GlassStage]); otherwise the plain themed surface.
 */
@Composable
fun sheetContainerColor(): Color {
    val colors = MaterialTheme.colorScheme
    return if (LocalGlassEnabled.current) {
        // Opaque: a sheet is its own window and cannot blur the app behind it, so it must not let that app show through.
        lerp(colors.background, colors.primaryContainer, 0.2f)
    } else {
        colors.surfaceContainerLow
    }
}

/** The dim behind a sheet. */
@Composable
fun sheetScrimColor(): Color =
    Color.Black.copy(alpha = 0.4f)
