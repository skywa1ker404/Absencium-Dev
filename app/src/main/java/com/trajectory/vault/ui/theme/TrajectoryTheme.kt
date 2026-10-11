package com.trajectory.vault.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.trajectory.vault.data.settings.AppSettings
import com.trajectory.vault.data.settings.ColorEngine
import com.trajectory.vault.data.settings.ThemeMode
import dev.chrisbanes.haze.HazeState

/** Monet (wallpaper colors) and the real blur behind Liquid Glass both need Android 12. */
val isMonetAndBlurSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** The engine actually used: Monet and Liquid Glass fall back to Solid Accent below Android 12. */
fun effectiveColorEngine(requested: ColorEngine): ColorEngine =
    if (requested != ColorEngine.SOLID_ACCENT && !isMonetAndBlurSupported) {
        ColorEngine.SOLID_ACCENT
    } else {
        requested
    }

private val OledBackground = Color(0xFF0D0D0D)

/** Blur radius of the frosted glass bars, from the glass intensity setting. */
val LocalGlassBlur = staticCompositionLocalOf { 20.dp }

/** True while the Liquid Glass engine is active: floating bars become frosted glass. */
val LocalGlassEnabled = staticCompositionLocalOf { false }

/**
 * The haze state of the screen currently being drawn, or null when glass is off. A screen's
 * scrolling content is the blur source; its floating bars blur what scrolls behind them.
 */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

private val TrajectoryShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

/**
 * Expressive type scale: heavier weights on headings, card titles and labels so the hierarchy
 * reads at a glance, with body text left light for long study notes.
 */
private val TrajectoryTypography: Typography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontWeight = FontWeight.ExtraBold),
        displayMedium = base.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold),
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Bold),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.Bold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Bold),
        labelMedium = base.labelMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun TrajectoryTheme(
    settings: AppSettings,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val isDark = when (settings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK_OLED -> true
        ThemeMode.SYSTEM_AUTO -> systemDark
    }
    val engine = effectiveColorEngine(settings.colorEngine)
    val seed = Color(settings.accentColor)

    val generated: ColorScheme = when (engine) {
        ColorEngine.MONET_WALLPAPER ->
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        ColorEngine.SOLID_ACCENT,
        ColorEngine.LIQUID_GLASS,
        -> rememberDynamicColorScheme(
            seedColor = seed,
            isDark = isDark,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
            style = PaletteStyle.Expressive,
        )
    }

    val colorScheme = if (settings.themeMode == ThemeMode.DARK_OLED) {
        generated.copy(
            background = OledBackground,
            surface = OledBackground,
            surfaceDim = OledBackground,
            surfaceContainerLowest = OledBackground,
            surfaceContainerLow = Color(0xFF141414),
            surfaceContainer = Color(0xFF191919),
            surfaceContainerHigh = Color(0xFF1E1E1E),
            surfaceContainerHighest = Color(0xFF262626),
        )
    } else {
        generated
    }

    MaterialTheme(colorScheme = colorScheme, shapes = TrajectoryShapes, typography = TrajectoryTypography) {
        CompositionLocalProvider(
            LocalGlassEnabled provides (engine == ColorEngine.LIQUID_GLASS),
            LocalGlassBlur provides settings.glassBlurDp.dp,
        ) {
            Box(modifier = Modifier.fillMaxSize().background(colorScheme.background)) {
                content()
            }
        }
    }
}
