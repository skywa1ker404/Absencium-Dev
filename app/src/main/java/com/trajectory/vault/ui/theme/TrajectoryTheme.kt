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
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.trajectory.vault.data.settings.AppSettings
import com.trajectory.vault.data.settings.ColorEngine
import com.trajectory.vault.data.settings.ThemeMode
import dev.chrisbanes.haze.*

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

/**
 * Non-null while the Liquid Glass engine is active. Glass surfaces blur whatever is drawn
 * inside the matching `hazeSource`, which is the themed backdrop drawn by [TrajectoryTheme].
 */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

private val TrajectoryShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

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
            background = Color.Black,
            surface = Color.Black,
            surfaceDim = Color.Black,
            surfaceContainerLowest = Color.Black,
        )
    } else {
        generated
    }

    val hazeState = rememberHazeState()
    val glassState = if (engine == ColorEngine.LIQUID_GLASS) hazeState else null

    MaterialTheme(colorScheme = colorScheme, shapes = TrajectoryShapes) {
        CompositionLocalProvider(LocalHazeState provides glassState) {
            Box(modifier = Modifier.fillMaxSize()) {
                Backdrop(glassState)
                content()
            }
        }
    }
}

/** Plain background normally; a soft color field for glass surfaces to blur in Liquid Glass. */
@Composable
private fun Backdrop(hazeState: HazeState?) {
    val colors = MaterialTheme.colorScheme
    if (hazeState == null) {
        Box(modifier = Modifier.fillMaxSize().background(colors.background))
        return
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .hazeSource(hazeState)
            .drawBehind {
                drawRect(
                    Brush.linearGradient(
                        listOf(colors.primaryContainer, colors.surface, colors.tertiaryContainer),
                    ),
                )
                val radius = size.minDimension * 0.75f
                val spots = listOf(
                    Triple(Offset(size.width * 0.12f, size.height * 0.18f), colors.primary, 0.55f),
                    Triple(Offset(size.width * 0.9f, size.height * 0.5f), colors.tertiary, 0.45f),
                    Triple(Offset(size.width * 0.3f, size.height * 0.92f), colors.secondary, 0.5f),
                )
                spots.forEach { (center, color, alpha) ->
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                            center = center,
                            radius = radius,
                        ),
                        radius = radius,
                        center = center,
                    )
                }
            },
    )
}
