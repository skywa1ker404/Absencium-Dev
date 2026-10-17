package skywa1ker404.absencium.ui.theme

import skywa1ker404.absencium.ui.components.LocalFrostedOverlays
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import skywa1ker404.absencium.R
import skywa1ker404.absencium.data.settings.AppSettings
import skywa1ker404.absencium.data.settings.ColorEngine
import skywa1ker404.absencium.data.settings.ThemeMode
import skywa1ker404.absencium.data.settings.ThemePreset
import com.kyant.backdrop.backdrops.LayerBackdrop

/** Monet (wallpaper colors) and the real blur behind Liquid Glass both need Android 12. */
val isMonetAndBlurSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** The engine actually used: Monet needs Android 12, so older phones use the Default Theme. */
fun effectiveColorEngine(requested: ColorEngine): ColorEngine =
    if (requested == ColorEngine.MONET_WALLPAPER && !isMonetAndBlurSupported) {
        ColorEngine.DEFAULT_PALETTE
    } else {
        requested
    }

/**
 * Whether the app is drawn dark. A scheme with no light variant (Dracula, Poimandres) is always dark,
 * whatever the phone or the theme mode says.
 */
fun resolveDark(settings: AppSettings, systemDark: Boolean): Boolean {
    val wantsDark = when (settings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.DARK_OLED -> true
        ThemeMode.SYSTEM_AUTO -> systemDark
    }
    val darkOnly = effectiveColorEngine(settings.colorEngine) == ColorEngine.PRESET && !settings.themePreset.palette().hasLight
    return wantsDark || darkOnly
}

/** Blur radius of the frosted glass bars, from the glass intensity setting. */
val LocalGlassBlur = staticCompositionLocalOf { 20.dp }

/** True while the Liquid Glass engine is active: floating bars become frosted glass. */
val LocalGlassEnabled = staticCompositionLocalOf { false }

/**
 * The backdrop of the screen currently being drawn, or null when glass is off. A screen's
 * scrolling content is recorded into it; its floating bars blur and refract what scrolls behind them.
 */
val LocalBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

private val AbsenciumShapes = Shapes(
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

/** Outfit Bold, used only for the "Absencium" wordmark. */
val OutfitFamily = FontFamily(Font(R.font.outfit_bold, FontWeight.Bold))

private fun TextStyle.inter(weight: FontWeight, tracking: Double = 0.0): TextStyle =
    copy(fontFamily = InterFamily, fontWeight = weight, letterSpacing = tracking.sp)

/**
 * Expressive type scale in Inter: heavy, tightly tracked headings, medium-weight card titles and
 * regular body text so lists stay calm and the hierarchy still reads at a glance.
 */
private val AbsenciumTypography: Typography = Typography().let { base ->
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
fun AbsenciumTheme(
    settings: AppSettings,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val isDark = resolveDark(settings, systemDark)
    val engine = effectiveColorEngine(settings.colorEngine)

    val quality = QualityProfile.of(settings.visualQuality)
    // The Liquid Glass colours stay; only the blur itself is switched off when the quality level asks for it.
    // Liquid Glass is a layer on top of the active theme; it needs Android 12 for the blur and Quality mode for the cost.
    val glassActive = settings.liquidGlass && quality.blurEnabled && isMonetAndBlurSupported

    val colorScheme: ColorScheme = remember(engine, isDark, settings.themePreset, settings.themeMode, glassActive, context, LocalConfiguration.current) {
        val generated = when (engine) {
            // The system's own scheme: the wallpaper colour picked in Wallpaper & style, light or dark.
            ColorEngine.MONET_WALLPAPER -> if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            ColorEngine.DEFAULT_PALETTE -> (if (isDark) DefaultPalette.dark else DefaultPalette.light).toColorScheme(isDark)
            ColorEngine.PRESET -> {
                if (settings.themePreset == ThemePreset.NO_COLOUR && !glassActive) {
                    // "No colour" exists only for Liquid Glass; with glass off the default theme is used instead.
                    (if (isDark) DefaultPalette.dark else DefaultPalette.light).toColorScheme(isDark)
                } else {
                    val (variantIsDark, variant) = settings.themePreset.palette().variantFor(isDark)
                    variant.toColorScheme(variantIsDark)
                }
            }
        }
        if (settings.themeMode == ThemeMode.DARK_OLED && isDark) generated.oledBlack() else generated
    }

    MaterialTheme(colorScheme = colorScheme, shapes = AbsenciumShapes, typography = AbsenciumTypography) {
        CompositionLocalProvider(
            LocalQuality provides quality,
            LocalGlassEnabled provides glassActive,
            LocalFrostedOverlays provides (glassActive && settings.frostedOverlays),
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
fun isDarkTheme(settings: AppSettings): Boolean = resolveDark(settings, isSystemInDarkTheme())
