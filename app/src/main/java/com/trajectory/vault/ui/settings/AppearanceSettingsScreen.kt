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
import com.trajectory.vault.data.settings.ColorEngine
import com.trajectory.vault.data.settings.ThemeMode
import com.trajectory.vault.ui.REQUIRES_ANDROID_12
import com.trajectory.vault.ui.components.AccentColorPicker
import com.trajectory.vault.ui.components.AppCard
import com.trajectory.vault.ui.components.OptionCard
import com.trajectory.vault.ui.components.ScreenScaffold
import com.trajectory.vault.ui.components.SectionTitle
import com.trajectory.vault.ui.description
import com.trajectory.vault.ui.label
import com.trajectory.vault.ui.theme.isMonetAndBlurSupported

@Destination<RootGraph>
@Composable
fun AppearanceSettingsScreen(navigator: DestinationsNavigator) {
    val viewModel = rememberSettingsViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    ScreenScaffold(title = "Appearance", onBack = { navigator.navigateUp() }) {
        ThemeModeChoices(selected = settings.themeMode, onSelect = viewModel::setThemeMode)
        ColorEngineChoices(selected = settings.colorEngine, onSelect = viewModel::setColorEngine)

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
