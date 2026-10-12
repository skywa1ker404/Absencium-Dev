package com.trajectory.vault.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.trajectory.vault.R
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

/** Dark: near-black neutral surfaces. */
private val DarkBackground = Color(0xFF0D0D0D)

/** OLED Dark: a true black (#000000) background so OLED pixels switch off. */
private val OledBackground = Color(0xFF000000)

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

/** Inter, bundled with the app so the type looks the same offline on every phone. */
val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_extrabold, FontWeight.ExtraBold),
)

private fun TextStyle.inter(weight: FontWeight, tracking: Double = 0.0): TextStyle =
    copy(fontFamily = InterFamily, fontWeight = weight, letterSpacing = tracking.sp)

/**
 * Expressive type scale in Inter: heavy, tightly tracked headings, medium-weight card titles and
 * regular body text so lists stay calm and the hierarchy still reads at a glance.
 */
private val TrajectoryTypography: Typography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.inter(FontWeight.ExtraBold, -1.0),
        displayMedium = base.displayMedium.inter(FontWeight.ExtraBold, -0.75),
        displaySmall = base.displaySmall.inter(FontWeight.Bold, -0.5),
        headlineLarge = base.headlineLarge.inter(FontWeight.ExtraBold, -0.5),
        headlineMedium = base.headlineMedium.inter(FontWeight.Bold, -0.5),
        headlineSmall = base.headlineSmall.inter(FontWeight.Bold, -0.25),
        titleLarge = base.titleLarge.inter(FontWeight.SemiBold),
        titleMedium = base.titleMedium.inter(FontWeight.Medium),
        titleSmall = base.titleSmall.inter(FontWeight.Medium),
        bodyLarge = base.bodyLarge.inter(FontWeight.Normal),
        bodyMedium = base.bodyMedium.inter(FontWeight.Normal),
        bodySmall = base.bodySmall.inter(FontWeight.Normal),
        labelLarge = base.labelLarge.inter(FontWeight.SemiBold),
        labelMedium = base.labelMedium.inter(FontWeight.SemiBold),
        labelSmall = base.labelSmall.inter(FontWeight.Medium),
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
        ThemeMode.DARK, ThemeMode.DARK_OLED -> true
        ThemeMode.SYSTEM_AUTO -> systemDark
    }
    val engine = effectiveColorEngine(settings.colorEngine)
    val seed = Color(settings.accentColor)

    val generated: ColorScheme = when (engine) {
        // Liquid Glass follows the wallpaper colors like Monet; only Solid Accent uses the chosen accent.
        ColorEngine.MONET_WALLPAPER,
        ColorEngine.LIQUID_GLASS,
        -> if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        ColorEngine.SOLID_ACCENT -> rememberDynamicColorScheme(
            seedColor = seed,
            isDark = isDark,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
            style = PaletteStyle.Expressive,
        )
    }

    val colorScheme = when (settings.themeMode) {
        ThemeMode.DARK_OLED -> generated.copy(
            background = OledBackground,
            surface = OledBackground,
            surfaceDim = OledBackground,
            surfaceContainerLowest = OledBackground,
            surfaceContainerLow = Color(0xFF0A0A0A),
            surfaceContainer = Color(0xFF101010),
            surfaceContainerHigh = Color(0xFF161616),
            surfaceContainerHighest = Color(0xFF1D1D1D),
        )

        ThemeMode.DARK, ThemeMode.SYSTEM_AUTO -> if (!isDark) generated else generated.copy(
            background = DarkBackground,
            surface = DarkBackground,
            surfaceDim = DarkBackground,
            surfaceContainerLowest = DarkBackground,
            surfaceContainerLow = Color(0xFF141414),
            surfaceContainer = Color(0xFF191919),
            surfaceContainerHigh = Color(0xFF1E1E1E),
            surfaceContainerHighest = Color(0xFF262626),
        )

        // Light uses the generated scheme as it is.
        ThemeMode.LIGHT -> generated
    }

    MaterialTheme(colorScheme = colorScheme, shapes = TrajectoryShapes, typography = TrajectoryTypography) {
        CompositionLocalProvider(
            LocalGlassEnabled provides (engine == ColorEngine.LIQUID_GLASS),
            LocalGlassBlur provides settings.glassBlurDp.dp,
            // Text outside a Surface would otherwise fall back to black, which is unreadable on dark themes.
            LocalContentColor provides colorScheme.onBackground,
        ) {
            Box(modifier = Modifier.fillMaxSize().background(colorScheme.background)) {
                content()
            }
        }
    }
}

/** Whether the theme in [settings] is dark, resolving System Auto against the phone's setting. */
@Composable
fun isDarkTheme(settings: AppSettings): Boolean = when (settings.themeMode) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK, ThemeMode.DARK_OLED -> true
    ThemeMode.SYSTEM_AUTO -> isSystemInDarkTheme()
}
