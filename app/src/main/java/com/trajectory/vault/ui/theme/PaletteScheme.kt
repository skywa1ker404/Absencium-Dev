package com.trajectory.vault.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

/**
 * The few colours a theme is made of. [toColorScheme] maps them onto every Material 3 colour role, so
 * a theme is described once and the whole app follows it: surfaces step from [background] toward [text],
 * accents become tinted containers, and anything used as text or an icon is nudged until it is readable.
 */
class PaletteVariant(
    val background: Color,
    val text: Color,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val error: Color,
    /** Secondary text. Derived from [text] and [background] when not given. */
    val textMuted: Color? = null,
    val outline: Color? = null,
    /** Optional hand-picked containers; otherwise a tint of the accent over the background. */
    val primaryContainer: Color? = null,
    val secondaryContainer: Color? = null,
    /** Optional hand-picked surface steps (low, container, high, highest). */
    val surfaces: List<Color>? = null,
)

/** WCAG contrast ratio between two opaque colours. */
internal fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return max(la, lb) / min(la, lb)
}

/** Moves [color] toward [toward] in small steps until it reaches [minimum] contrast against [against]. */
internal fun ensureContrast(color: Color, against: Color, toward: Color, minimum: Float): Color {
    if (contrastRatio(color, against) >= minimum) return color
    var amount = 0f
    var result = color
    while (amount < 1f && contrastRatio(result, against) < minimum) {
        amount += 0.05f
        result = lerp(color, toward, amount.coerceAtMost(1f))
    }
    return result
}

fun PaletteVariant.toColorScheme(isDark: Boolean): ColorScheme {
    val bg = background
    val fg = text
    // Text on an accent uses whichever of the theme's two ends reads better, so it stays in the theme.
    val darkInk = if (bg.luminance() < fg.luminance()) bg else fg
    val lightInk = if (bg.luminance() < fg.luminance()) fg else bg
    fun on(color: Color) = if (contrastRatio(color, darkInk) >= contrastRatio(color, lightInk)) darkInk else lightInk

    val muted = ensureContrast(textMuted ?: lerp(fg, bg, 0.3f), bg, fg, 4.5f)
    val edge = ensureContrast(outline ?: lerp(fg, bg, 0.55f), bg, fg, 3f)
    val p = ensureContrast(primary, bg, fg, 4.5f)
    val s = ensureContrast(secondary, bg, fg, 4.5f)
    val t = ensureContrast(tertiary, bg, fg, 4.5f)
    val e = ensureContrast(error, bg, fg, 4.5f)

    val low = surfaces?.get(0) ?: lerp(bg, fg, 0.04f)
    val container = surfaces?.get(1) ?: lerp(bg, fg, 0.07f)
    val high = surfaces?.get(2) ?: lerp(bg, fg, 0.10f)
    val highest = surfaces?.get(3) ?: lerp(bg, fg, 0.14f)
    val lowest = if (isDark) lerp(bg, Color.Black, 0.3f) else lerp(bg, Color.White, 0.5f)
    val dim = if (isDark) lerp(bg, Color.Black, 0.2f) else lerp(bg, fg, 0.1f)
    val bright = if (isDark) lerp(bg, fg, 0.12f) else lerp(bg, Color.White, 0.5f)

    val tint = if (isDark) 0.30f else 0.22f
    val pc = primaryContainer ?: lerp(bg, primary, tint)
    val sc = secondaryContainer ?: lerp(bg, secondary, tint)
    val tc = lerp(bg, tertiary, tint)
    val ec = lerp(bg, error, tint)

    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = p,
        onPrimary = on(p),
        primaryContainer = pc,
        onPrimaryContainer = ensureContrast(fg, pc, if (fg.luminance() > pc.luminance()) Color.White else Color.Black, 4.5f),
        inversePrimary = lerp(p, bg, 0.4f),
        secondary = s,
        onSecondary = on(s),
        secondaryContainer = sc,
        onSecondaryContainer = ensureContrast(fg, sc, if (fg.luminance() > sc.luminance()) Color.White else Color.Black, 4.5f),
        tertiary = t,
        onTertiary = on(t),
        tertiaryContainer = tc,
        onTertiaryContainer = ensureContrast(fg, tc, if (fg.luminance() > tc.luminance()) Color.White else Color.Black, 4.5f),
        error = e,
        onError = on(e),
        errorContainer = ec,
        onErrorContainer = ensureContrast(fg, ec, if (fg.luminance() > ec.luminance()) Color.White else Color.Black, 4.5f),
        background = bg,
        onBackground = fg,
        surface = bg,
        onSurface = fg,
        surfaceVariant = highest,
        onSurfaceVariant = muted,
        surfaceTint = p,
        inverseSurface = fg,
        inverseOnSurface = bg,
        outline = edge,
        outlineVariant = lerp(bg, edge, 0.5f),
        scrim = Color.Black,
        surfaceDim = dim,
        surfaceBright = bright,
        surfaceContainerLowest = lowest,
        surfaceContainerLow = low,
        surfaceContainer = container,
        surfaceContainerHigh = high,
        surfaceContainerHighest = highest,
    )
}

/** Pure black for the OLED mode. The raised surfaces keep their tint, only darker, so each theme stays itself. */
fun ColorScheme.oledBlack(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = lerp(Color.Black, surfaceContainerLow, 0.55f),
    surfaceContainer = lerp(Color.Black, surfaceContainer, 0.6f),
    surfaceContainerHigh = lerp(Color.Black, surfaceContainerHigh, 0.65f),
    surfaceContainerHighest = lerp(Color.Black, surfaceContainerHighest, 0.7f),
)
