package com.trajectory.vault.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.trajectory.vault.data.settings.AppSettings
import com.trajectory.vault.data.settings.ColorEngine
import com.trajectory.vault.data.settings.ThemeMode
import com.trajectory.vault.data.settings.VisualQuality
import com.trajectory.vault.ui.REQUIRES_ANDROID_12
import com.trajectory.vault.ui.components.AccentColorPicker
import com.trajectory.vault.ui.components.AppCard
import com.trajectory.vault.ui.components.HintText
import com.trajectory.vault.ui.components.OptionCard
import com.trajectory.vault.ui.components.ScreenScaffold
import com.trajectory.vault.ui.components.SectionTitle
import com.trajectory.vault.ui.components.SliderCard
import com.trajectory.vault.ui.description
import com.trajectory.vault.ui.label
import com.trajectory.vault.ui.theme.isMonetAndBlurSupported

@Destination<RootGraph>
@Composable
fun AppearanceSettingsScreen(navigator: DestinationsNavigator) {
    val viewModel = rememberSettingsViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    ScreenScaffold(title = "Appearance", onBack = { navigator.navigateUp() }) {
        VisualQualityChoices(
            selected = settings.visualQuality,
            recommended = viewModel.recommendedQuality,
            onSelect = viewModel::setVisualQuality,
        )
        ThemeModeChoices(selected = settings.themeMode, onSelect = viewModel::setThemeMode)
        ColorEngineChoices(selected = settings.colorEngine, onSelect = viewModel::setColorEngine)

        if (settings.colorEngine == ColorEngine.LIQUID_GLASS && isMonetAndBlurSupported) {
            SectionTitle("Frosted glass intensity")
            GlassBlurSlider(settings.glassBlurDp) { viewModel.setGlassBlur(it) }
        }

        if (settings.colorEngine != ColorEngine.MONET_WALLPAPER) {
            SectionTitle("Accent color")
            AppCard(modifier = Modifier.fillMaxWidth()) {
                AccentColorPicker(
                    selected = Color(settings.accentColor),
                    onSelected = { viewModel.setAccentColor(it.toArgb()) },
                )
            }
        }
    }
}

/** Visual quality choices, shared by Settings and the onboarding wizard. Applies the moment one is tapped. */
@Composable
fun VisualQualityChoices(
    selected: VisualQuality,
    recommended: VisualQuality,
    onSelect: (VisualQuality) -> Unit,
) {
    SectionTitle("Visual quality")
    HintText("Lower levels turn down blur and animation to keep the app smooth. Every feature stays.")
    VisualQuality.entries.forEach { level ->
        OptionCard(
            title = if (level == recommended) "${level.label()} (recommended for this phone)" else level.label(),
            subtitle = level.description(),
            selected = selected == level,
            onClick = { onSelect(level) },
        )
    }
}

/** Theme mode choices, shared by Settings and the onboarding wizard. */
@Composable
fun ThemeModeChoices(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    SectionTitle("Theme mode")
    ThemeMode.entries.forEach { mode ->
        OptionCard(
            title = mode.label(),
            subtitle = mode.description(),
            selected = selected == mode,
            onClick = { onSelect(mode) },
        )
    }
}

/** Color engine choices. Monet and Liquid Glass are greyed out below Android 12. */
@Composable
fun ColorEngineChoices(selected: ColorEngine, onSelect: (ColorEngine) -> Unit) {
    SectionTitle("Color engine")
    ColorEngine.entries.forEach { engine ->
        val available = engine == ColorEngine.SOLID_ACCENT || isMonetAndBlurSupported
        OptionCard(
            title = engine.label(),
            subtitle = if (available) engine.description() else "${engine.description()}\n$REQUIRES_ANDROID_12",
            selected = selected == engine,
            enabled = available,
            onClick = { onSelect(engine) },
        )
    }
}

/** Live frosted glass intensity (5 to 40). The bars on this screen change as the value is saved. */
@Composable
fun GlassBlurSlider(value: Int, onChange: (Int) -> Unit) {
    SliderCard(
        title = "Frosted glass blur",
        subtitle = "How strongly the floating bars blur the content behind them.",
        value = value,
        range = AppSettings.MIN_GLASS_BLUR..AppSettings.MAX_GLASS_BLUR,
        valueLabel = { "$it px" },
        onValueChangeFinished = onChange,
    )
}
