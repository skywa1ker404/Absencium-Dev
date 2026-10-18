package skywa1ker404.absencium.ui.components

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
import skywa1ker404.absencium.ui.theme.LocalGlassBlur
import skywa1ker404.absencium.ui.theme.LocalGlassEnabled
import skywa1ker404.absencium.ui.theme.LocalQuality

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
    /** Large surfaces (popups) skip the lens: it would bend the whole screen behind them on every frame of their motion. */
    lens: Boolean = true,
    /** A denser veil for surfaces that carry a lot of text, so it reads over busy content. */
    denseVeil: Boolean = false,
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
    val veil = veilBase.copy(alpha = if (denseVeil) (if (isDark) 0.62f else 0.72f) else if (isDark) 0.42f else 0.5f)
    val blurDp = LocalGlassBlur.current
    // Lens refraction needs rounded corners and Android 13 (a runtime shader); a plain rectangle only blurs.
    val useLens = lens && quality.glassLens && shape is CornerBasedShape && isRuntimeShaderSupported()
    val rim = if (bordered && !quality.glassHighlight) {
        Modifier.border(1.dp, Color.White.copy(alpha = if (isDark) 0.16f else 0.55f), shape)
    } else {
        Modifier
    }
    // Without the lens, only a blur is drawn, and a blur spills past rounded corners as a square patch. Clipping the
    // whole surface to its shape keeps the corners round (the drop shadow is given up with it).
    return (if (useLens) this else this.clip(shape))
        .drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                if (blurDp.value > 0f) blur(blurDp.toPx())
                if (useLens) {
                    lens(refractionHeight = 12.dp.toPx(), refractionAmount = 24.dp.toPx())
                }
            },
            highlight = if (bordered && quality.glassHighlight) ({ Highlight.Default }) else null,
            shadow = if (bordered && quality.glassHighlight && useLens) ({ Shadow.Default }) else null,
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
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    // Light catches the upper-left of the pane and fades across it, like the panes in a glass UI kit: a soft sheen, strongest
    // at the top-left corner, gone by the lower right.
    val sheenStrong = Color.White.copy(alpha = if (isDark) 0.11f else 0.30f)
    val sheenSoft = Color.White.copy(alpha = if (isDark) 0.03f else 0.08f)
    val gloss = skywa1ker404.absencium.ui.theme.LocalGlassGloss.current
    val glossTop = Color.White.copy(alpha = if (isDark) 0.20f else 0.38f)
    val glossGlint = Color.White.copy(alpha = if (isDark) 0.10f else 0.22f)
    // Clipped to its own shape: the blur otherwise spills past rounded corners and shows as a square, pointed patch
    // around every card and button. Cards sit flat on the wallpaper, so they carry no drop shadow.
    return this.clip(shape).drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            if (blurDp.value > 0f) blur(blurDp.toPx())
        },
        highlight = { Highlight.Default },
        shadow = null,
        onDrawSurface = {
            drawRect(tint.copy(alpha = tintAlpha))
            drawRect(
                Brush.linearGradient(
                    0f to sheenStrong,
                    0.4f to sheenSoft,
                    1f to Color.Transparent,
                    start = Offset.Zero,
                    end = Offset(size.width, size.height),
                ),
            )
            if (gloss) {
                // Glossy: a bright band along the top that fades out by the middle, and a narrow glint across the pane.
                drawRect(
                    Brush.verticalGradient(
                        0f to glossTop,
                        0.22f to glossTop.copy(alpha = glossTop.alpha * 0.35f),
                        0.5f to Color.Transparent,
                    ),
                )
                drawRect(
                    Brush.linearGradient(
                        0.28f to Color.Transparent,
                        0.36f to glossGlint,
                        0.44f to Color.Transparent,
                        start = Offset.Zero,
                        end = Offset(size.width, size.height),
                    ),
                )
            }
        },
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
    val streak = Color.White.copy(alpha = if (base.luminance() < 0.5f) 0.07f else 0.22f)
    Box(
        modifier = modifier
            .background(base)
            .drawBehind {
                val w = size.width
                val h = size.height
                fun blob(color: Color, cx: Float, cy: Float, r: Float, peak: Float = 0.95f) = drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = peak), color.copy(alpha = 0f)),
                        center = Offset(cx, cy),
                        radius = r,
                    ),
                    radius = r,
                    center = Offset(cx, cy),
                )
                blob(a, w * 0.15f, h * 0.12f, w * 0.9f)
                blob(b, w * 0.95f, h * 0.45f, w * 0.85f)
                blob(c, w * 0.2f, h * 0.85f, w * 0.9f)
                // A fourth, fainter colour in the middle so the glass has more to bend, and a soft diagonal streak of light
                // across the screen, as in the reference glass designs.
                blob(lerp(a, b, 0.5f), w * 0.55f, h * 0.65f, w * 0.7f, peak = 0.55f)
                drawRect(
                    Brush.linearGradient(
                        0f to Color.Transparent,
                        0.45f to streak,
                        0.6f to Color.Transparent,
                        start = Offset(0f, 0f),
                        end = Offset(w, h * 0.7f),
                    ),
                )
            },
    )
}

/**
 * Wraps a screen's content in glass mode: draws the [GlassWallpaper] into its own layer and lets cards sample it.
 * Without glass it is transparent to its content.
 */
@Composable
fun GlassStage(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val glass = LocalGlassEnabled.current
    val wallpaper = rememberLayerBackdrop()
    // The same layout with Liquid Glass on or off, so switching it never rebuilds the content below (which would reset
    // scroll positions and open sections); only the wallpaper layer and the backdrop appear or go.
    Box(modifier = modifier) {
        if (glass) GlassWallpaper(Modifier.matchParentSize().layerBackdrop(wallpaper))
        CompositionLocalProvider(LocalCardBackdrop provides if (glass) wallpaper else null) { content() }
    }
}

/** Whether overlays (profile, search, Add Entry, import and colour pickers) are frosted: Liquid Glass plus its sub-toggle. */
val LocalFrostedOverlays = androidx.compose.runtime.staticCompositionLocalOf { false }

/**
 * The content area of a popup. The popup itself is the pane of glass (see [PopupShell]), so what sits inside it is flat:
 * cards and controls are plain tonal fills, never a second layer of glass over the first.
 */
@Composable
fun OverlayStage(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalCardBackdrop provides null) { Box(modifier = modifier) { content() } }
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
