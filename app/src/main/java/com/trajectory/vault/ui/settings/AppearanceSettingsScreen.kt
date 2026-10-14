package com.trajectory.vault.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.trajectory.vault.data.settings.AppSettings
import com.trajectory.vault.data.settings.ColorEngine
import com.trajectory.vault.data.settings.ThemeMode
import com.trajectory.vault.data.settings.ThemePreset
import com.trajectory.vault.data.settings.VisualQuality
import com.trajectory.vault.ui.REQUIRES_ANDROID_12
import com.trajectory.vault.ui.components.AppCard
import com.trajectory.vault.ui.components.HintText
import com.trajectory.vault.ui.components.OptionCard
import com.trajectory.vault.ui.components.SwitchCard
import com.trajectory.vault.ui.components.ScreenScaffold
import com.trajectory.vault.ui.components.SectionTitle
import com.trajectory.vault.ui.components.SliderCard
import com.trajectory.vault.ui.description
import com.trajectory.vault.ui.label
import com.trajectory.vault.ui.theme.isDarkTheme
import com.trajectory.vault.ui.theme.isMonetAndBlurSupported
import com.trajectory.vault.ui.theme.palette

@Destination<RootGraph>
@Composable
fun AppearanceSettingsScreen(navigator: DestinationsNavigator) {
    val viewModel = rememberSettingsViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    ScreenScaffold(title = "Appearance", onBack = { navigator.navigateUp() }) {
        VisualQualityChoices(
            selected = settings.visualQuality,
            onSelect = viewModel::setVisualQuality,
        )
        ThemeModeChoices(selected = settings.themeMode, onSelect = viewModel::setThemeMode)
        ColorEngineChoices(
            settings = settings,
            onSelect = viewModel::setColorEngine,
            onPresetSelected = viewModel::setThemePreset,
            onGlassToggle = viewModel::setLiquidGlass,
        )

        if (settings.liquidGlass && isMonetAndBlurSupported) {
            SectionTitle("Frosted glass intensity")
            GlassBlurSlider(settings.glassBlurDp) { viewModel.setGlassBlur(it) }
        }
    }
}

/** Visual quality choices, shared by Settings and the onboarding wizard. Applies the moment one is tapped. */
@Composable
fun VisualQualityChoices(
    selected: VisualQuality,
    onSelect: (VisualQuality) -> Unit,
) {
    SectionTitle("Visual quality")
    HintText("Pick what suits your phone. Every feature stays in all three.")
    VisualQuality.entries.forEach { level ->
        OptionCard(
            title = level.label(),
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

/**
 * Theme choices: Default Theme, Monet, Liquid Glass and Presets. Presets opens its own page of colour schemes;
 * picking one there applies and saves it at once.
 */
@Composable
fun ColorEngineChoices(
    settings: AppSettings,
    onSelect: (ColorEngine) -> Unit,
    onPresetSelected: (ThemePreset) -> Unit,
    onGlassToggle: (Boolean) -> Unit,
) {
    var showPresets by remember { mutableStateOf(false) }
    SectionTitle("Theme")
    listOf(ColorEngine.DEFAULT_PALETTE, ColorEngine.MONET_WALLPAPER).forEach { engine ->
        val available = engine == ColorEngine.DEFAULT_PALETTE || isMonetAndBlurSupported
        OptionCard(
            title = engine.label(),
            subtitle = if (available) engine.description() else "${engine.description()}\n$REQUIRES_ANDROID_12",
            selected = settings.colorEngine == engine,
            enabled = available,
            onClick = { onSelect(engine) },
        )
    }
    OptionCard(
        title = ColorEngine.PRESET.label(),
        subtitle = if (settings.colorEngine == ColorEngine.PRESET) {
            "${settings.themePreset.palette().name} \u2022 tap to change"
        } else {
            ColorEngine.PRESET.description()
        },
        selected = settings.colorEngine == ColorEngine.PRESET,
        onClick = { showPresets = true },
    )
    SwitchCard(
        title = "Liquid Glass",
        subtitle = if (isMonetAndBlurSupported) {
            "Frosted, see-through glass on every surface except the wallpaper, in the colours of the theme you picked. " +
                "Shown in Quality mode; Performance mode keeps plain surfaces so the app stays smooth."
        } else {
            "Frosted glass over any theme.\n$REQUIRES_ANDROID_12"
        },
        checked = settings.liquidGlass,
        enabled = isMonetAndBlurSupported,
        onCheckedChange = onGlassToggle,
    )
    if (showPresets) {
        PresetsPage(
            selected = settings.themePreset.takeIf { settings.colorEngine == ColorEngine.PRESET },
            isDark = isDarkTheme(settings),
            onSelect = onPresetSelected,
            onDismiss = { showPresets = false },
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
