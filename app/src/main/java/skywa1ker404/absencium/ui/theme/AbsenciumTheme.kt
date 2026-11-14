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
import skywa1ker404.absencium.data.settings.AppStyle
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
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
import me.rerere.rikkahub.ui.components.dots.LocalDotIcons
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

/**
 * The theme's own colours with every surface fully opaque. Inside Liquid Glass the surfaces of a screen are translucent (see
 * [glassSurfaces]); windows that cannot show what is behind them (dialogs, menus, popups) switch back to this one.
 */
val LocalSolidColorScheme = staticCompositionLocalOf<ColorScheme?> { null }

/** The same scheme with its surface containers see-through, so a plain Surface or background on a glass screen shows the wallpaper. */
fun ColorScheme.glassSurfaces(alpha: Float = 0.5f): ColorScheme = copy(
    surfaceContainerLowest = surfaceContainerLowest.copy(alpha = alpha * 0.8f),
    surfaceContainerLow = surfaceContainerLow.copy(alpha = alpha),
    surfaceContainer = surfaceContainer.copy(alpha = alpha),
    surfaceContainerHigh = surfaceContainerHigh.copy(alpha = alpha),
    surfaceContainerHighest = surfaceContainerHighest.copy(alpha = alpha),
    surfaceVariant = surfaceVariant.copy(alpha = alpha),
)

/** The corner sizes follow one slider: the large corner is the chosen radius and the others keep the 12 / 20 / 28 / 36 proportions. */
private fun shapesFor(radiusDp: Int): Shapes {
    val r = radiusDp.toFloat()
    return Shapes(
        small = RoundedCornerShape((r * 12f / 28f).dp),
        medium = RoundedCornerShape((r * 20f / 28f).dp),
        large = RoundedCornerShape(r.dp),
        extraLarge = RoundedCornerShape((r * 36f / 28f).dp),
    )
}

/** Pixelated style: corners cut in two stair steps, small on every size. */
private val PixelShapes = Shapes(
    small = PixelCornerShape(1.5f),
    medium = PixelCornerShape(2f),
    large = PixelCornerShape(2.5f),
    extraLarge = PixelCornerShape(3f),
)

/** Dot matrix style: Doto (SIL OFL 1.1), a dot-matrix font, for every text style. */
val DotFamily = FontFamily(Font(R.font.doto, FontWeight.Normal), Font(R.font.doto, FontWeight.Bold))

/**
 * Pixelated style text: a square-pixel font made from Doto by turning each dot into a full square (SIL OFL 1.1, renamed "Absencium Pixel").
 * Both pixel fonts are wider than Inter, so their sizes are scaled down a little and lines stay inside their boxes.
 */
val PixelFamily = FontFamily(Font(R.font.pixel_font, FontWeight.Normal), Font(R.font.pixel_font_bold, FontWeight.Bold))

/** Every text style in [family]: titles and headings bold, the rest regular, all [scale] times the usual size. */
private fun fontTypography(family: FontFamily, scale: Float): Typography = Typography().let { base ->
    fun TextStyle.f(bold: Boolean = false) = copy(
        fontFamily = family,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        letterSpacing = 0.sp,
        fontSize = fontSize * scale,
        lineHeight = lineHeight * scale,
    )
    base.copy(
        displayLarge = base.displayLarge.f(true), displayMedium = base.displayMedium.f(true), displaySmall = base.displaySmall.f(true),
        headlineLarge = base.headlineLarge.f(true), headlineMedium = base.headlineMedium.f(true), headlineSmall = base.headlineSmall.f(true),
        titleLarge = base.titleLarge.f(true), titleMedium = base.titleMedium.f(), titleSmall = base.titleSmall.f(),
        bodyLarge = base.bodyLarge.f(), bodyMedium = base.bodyMedium.f(), bodySmall = base.bodySmall.f(),
        labelLarge = base.labelLarge.f(), labelMedium = base.labelMedium.f(), labelSmall = base.labelSmall.f(),
    )
}

private val DotTypography: Typography = fontTypography(DotFamily, 0.9f)
private val PixelTypography: Typography = fontTypography(PixelFamily, 0.9f)

/** The one accent of the dot matrix style, used only for alerts and errors. */
private val DotRed = Color(0xFFD71921)

/** Black and white with one red accent. Surfaces step through greys; no colour presets and no Monet. */
private fun dotMatrixScheme(dark: Boolean): ColorScheme = if (dark) {
    darkColorScheme(
        primary = Color.White, onPrimary = Color.Black, primaryContainer = Color(0xFF262626), onPrimaryContainer = Color.White,
        secondary = Color(0xFFD0D0D0), onSecondary = Color.Black, secondaryContainer = Color(0xFF1C1C1C), onSecondaryContainer = Color.White,
        tertiary = Color(0xFFB0B0B0), onTertiary = Color.Black, tertiaryContainer = Color(0xFF303030), onTertiaryContainer = Color.White,
        background = Color.Black, onBackground = Color.White, surface = Color.Black, onSurface = Color.White,
        surfaceVariant = Color(0xFF1C1C1C), onSurfaceVariant = Color(0xFFB8B8B8), outline = Color(0xFF6E6E6E), outlineVariant = Color(0xFF333333),
        surfaceContainerLowest = Color.Black, surfaceContainerLow = Color(0xFF0C0C0C), surfaceContainer = Color(0xFF121212),
        surfaceContainerHigh = Color(0xFF1A1A1A), surfaceContainerHighest = Color(0xFF242424),
        error = DotRed, onError = Color.White, errorContainer = Color(0xFF3A0D10), onErrorContainer = Color(0xFFFFB4B6),
    )
} else {
    lightColorScheme(
        primary = Color.Black, onPrimary = Color.White, primaryContainer = Color(0xFFE6E6E6), onPrimaryContainer = Color.Black,
        secondary = Color(0xFF2E2E2E), onSecondary = Color.White, secondaryContainer = Color(0xFFEDEDED), onSecondaryContainer = Color.Black,
        tertiary = Color(0xFF4A4A4A), onTertiary = Color.White, tertiaryContainer = Color(0xFFD9D9D9), onTertiaryContainer = Color.Black,
        background = Color.White, onBackground = Color.Black, surface = Color.White, onSurface = Color.Black,
        surfaceVariant = Color(0xFFEDEDED), onSurfaceVariant = Color(0xFF4A4A4A), outline = Color(0xFF8A8A8A), outlineVariant = Color(0xFFCFCFCF),
        surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF6F6F6), surfaceContainer = Color(0xFFF0F0F0),
        surfaceContainerHigh = Color(0xFFE9E9E9), surfaceContainerHighest = Color(0xFFE0E0E0),
        error = DotRed, onError = Color.White, errorContainer = Color(0xFFFFDAD9), onErrorContainer = Color(0xFF410004),
    )
}

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

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
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

    val colorScheme: ColorScheme = remember(engine, isDark, settings.themePreset, settings.themeMode, settings.pureBlack, settings.appStyle, glassActive, context, LocalConfiguration.current) {
        val generated = if (settings.appStyle == AppStyle.DOT_MATRIX) dotMatrixScheme(isDark) else when (engine) {
            // The system's own scheme: the wallpaper colour picked in Wallpaper & style, light or dark.
            ColorEngine.MONET_WALLPAPER -> if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            ColorEngine.DEFAULT_PALETTE -> (if (isDark) DefaultPalette.dark else DefaultPalette.light).toColorScheme(isDark)
            ColorEngine.PRESET -> {
                val (variantIsDark, variant) = settings.themePreset.palette().variantFor(isDark)
                variant.toColorScheme(variantIsDark)
            }
        }
        if (settings.pureBlack && isDark) generated.oledBlack() else generated
    }

    // Material 3 Expressive: the springy, bouncy motion scheme drives every Material component (switches, sliders, toggle buttons).
    androidx.compose.material3.MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = androidx.compose.material3.MotionScheme.expressive(),
        shapes = if (settings.appStyle == AppStyle.PIXELATED) PixelShapes else shapesFor(settings.cornerRadiusDp),
        typography = when (settings.appStyle) {
            AppStyle.DOT_MATRIX -> DotTypography
            AppStyle.PIXELATED -> PixelTypography
            else -> AbsenciumTypography
        },
    ) {
        CompositionLocalProvider(
            LocalQuality provides quality,
            LocalAppStyle provides settings.appStyle,
            LocalDotIcons provides (settings.appStyle == AppStyle.DOT_MATRIX),
            LocalAvatarStyle provides AvatarStyle(settings.avatarShape, settings.avatarFrame),
            androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(
                density = androidx.compose.ui.platform.LocalDensity.current.density * settings.displaySize.scale,
                fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale,
            ),
            LocalGlassEnabled provides glassActive,
            LocalSolidColorScheme provides colorScheme,
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
