package me.rerere.rikkahub.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat
import kotlinx.serialization.Serializable

import me.rerere.rikkahub.ui.hooks.rememberColorMode
import me.rerere.rikkahub.ui.hooks.rememberUserSettingsState

private val ExtendLightColors = lightExtendColors()
private val ExtendDarkColors = darkExtendColors()
val LocalExtendColors = compositionLocalOf { ExtendLightColors }

val LocalDarkMode = compositionLocalOf { false }

@Serializable
enum class ColorMode {
    SYSTEM,
    LIGHT,
    DARK
}

@Composable
fun RikkahubTheme(
    content: @Composable () -> Unit
) {
    // Inside Absencium the host's theme engine (Default, Monet, Liquid Glass, presets) decides every colour, shape and font.
    val host = me.rerere.rikkahub.LastChatHost.theme
    if (host != null) {
        host { dark -> HostedChatTheme(dark, content) }
        return
    }
    val settings by rememberUserSettingsState()

    val colorMode by rememberColorMode()
    val darkTheme = when (colorMode) {
        ColorMode.SYSTEM -> isSystemInDarkTheme()
        ColorMode.LIGHT -> false
        ColorMode.DARK -> true
    }


    val colorScheme = when {
        settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> findPresetTheme(settings.themeId).getColorScheme(dark = true)
        else -> findPresetTheme(settings.themeId).getColorScheme(dark = false)
    }
    val colorSchemeConverted = remember(darkTheme, colorScheme) {
        colorScheme.withLastChatAmoledSurface(darkTheme)
    }
    val extendColors = if (darkTheme) ExtendDarkColors else ExtendLightColors
    val statusBarColor = colorSchemeConverted.background

    // 更新状态栏图标颜色
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = statusBarColor.luminance() > 0.5f
                isAppearanceLightNavigationBars = !darkTheme
            }
            view.layoutDirection = android.view.View.LAYOUT_DIRECTION_LTR
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.statusBarColor = statusBarColor.toArgb()
        }
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Ltr,
        LocalDarkMode provides darkTheme,
        LocalExtendColors provides extendColors
    ) {
        // Create typography from font settings
        val fontSettings = me.rerere.rikkahub.ui.hooks.rememberFontSettings()
        val typography = rememberTypographyFromFontSettings(fontSettings)
        
        MaterialTheme(
            colorScheme = colorSchemeConverted,
            typography = typography,
            shapes = Shapes,
            content = content
        )
    }
}

/** The chat's own extra colours and the system bars, on top of whatever Material theme the host already set. */
@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
private fun HostedChatTheme(dark: Boolean, content: @Composable () -> Unit) {
    val barColor = MaterialTheme.colorScheme.background
    val glass = me.rerere.rikkahub.LocalHostGlass.current
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = barColor.luminance() > 0.5f
                isAppearanceLightNavigationBars = !dark
            }
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.statusBarColor = barColor.toArgb()
        }
    }
    val scheme = MaterialTheme.colorScheme
    val glassScheme = remember(scheme, glass) {
        // Liquid Glass: the page is see-through so the wallpaper shows, and the panes on it (input bar, cards, bubbles) are tinted
        // glass. Dialogs, menus and sheets keep their solid colours so they stay readable.
        if (glass) scheme.copy(
            // Same colour at zero alpha, not Color.Transparent: fades drawn from the background colour then blend to the wallpaper, not to black.
            background = scheme.background.copy(alpha = 0f),
            surfaceContainer = scheme.surfaceContainer.copy(alpha = 0.55f),
            surfaceContainerHighest = scheme.surfaceContainerHighest.copy(alpha = 0.55f),
            surfaceContainerLowest = scheme.surfaceContainerLowest.copy(alpha = 0.45f),
        ) else scheme
    }
    CompositionLocalProvider(
        LocalDarkMode provides dark,
        LocalExtendColors provides (if (dark) ExtendDarkColors else ExtendLightColors),
    ) {
        androidx.compose.material3.MaterialExpressiveTheme(
            colorScheme = glassScheme,
            motionScheme = androidx.compose.material3.MotionScheme.expressive(),
            shapes = MaterialTheme.shapes,
            typography = MaterialTheme.typography,
            content = content,
        )
    }
}

val MaterialTheme.extendColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendColors.current

/** A settings card colour: under Liquid Glass the card is a tinted see-through pane over the wallpaper (the same look as the app's cards), otherwise solid. */
@Composable
@ReadOnlyComposable
fun glassCard(base: androidx.compose.ui.graphics.Color): androidx.compose.ui.graphics.Color =
    if (me.rerere.rikkahub.LocalHostGlass.current) base.copy(alpha = 0.42f) else base

/** A menu's fill: the theme's container colour, never see-through, because a menu floats above text and must stay readable under glass. */
@Composable @ReadOnlyComposable
fun solidMenuColor(): androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 1f)
