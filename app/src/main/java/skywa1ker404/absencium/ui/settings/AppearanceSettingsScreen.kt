package skywa1ker404.absencium.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import skywa1ker404.absencium.ui.theme.DefaultPalette
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
import skywa1ker404.absencium.data.settings.AppSettings
import skywa1ker404.absencium.data.settings.ColorEngine
import skywa1ker404.absencium.data.settings.ThemeMode
import skywa1ker404.absencium.data.settings.ThemePreset
import skywa1ker404.absencium.data.settings.VisualQuality
import skywa1ker404.absencium.ui.REQUIRES_ANDROID_12
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.OptionCard
import skywa1ker404.absencium.ui.components.Reveal
import skywa1ker404.absencium.ui.components.SwitchCard
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.components.SectionTitle
import skywa1ker404.absencium.ui.components.SliderCard
import skywa1ker404.absencium.ui.description
import skywa1ker404.absencium.ui.label
import skywa1ker404.absencium.ui.theme.isDarkTheme
import skywa1ker404.absencium.ui.components.GreyedOut
import skywa1ker404.absencium.ui.theme.isMonetAndBlurSupported
import skywa1ker404.absencium.ui.theme.palette

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
            onFrostedToggle = viewModel::setFrostedOverlays,
        )

        if (settings.liquidGlass && isMonetAndBlurSupported) {
            SectionTitle("Frosted glass intensity")
            GreyedOut(disabled = settings.visualQuality == VisualQuality.PERFORMANCE) {
                GlassBlurSlider(settings.glassBlurDp) { viewModel.setGlassBlur(it) }
            }
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

/** Theme mode as four picture tiles (Light, Dark, OLED, Auto) with one helper line for the chosen one. */
@Composable
fun ThemeModeChoices(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val c = MaterialTheme.colorScheme
    val light = PreviewColors(Color(0xFFF4F4EF), Color(0xFFFFFFFF), Color(0xFF1B1C18), c.primary, c.secondary)
    val dark = PreviewColors(Color(0xFF1E201C), Color(0xFF2E302B), Color(0xFFE4E3DB), c.primary, c.secondary)
    val oled = PreviewColors(Color(0xFF0D0D0D), Color(0xFF1C1C1C), Color(0xFFE4E3DB), c.primary, c.secondary)
    SectionTitle("Theme mode")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ThemeMode.entries.forEach { mode ->
            PreviewTile(
                title = when (mode) {
                    ThemeMode.LIGHT -> "Light"
                    ThemeMode.DARK -> "Dark"
                    ThemeMode.DARK_OLED -> "OLED"
                    ThemeMode.SYSTEM_AUTO -> "Auto"
                },
                selected = selected == mode,
                onClick = { onSelect(mode) },
                modifier = Modifier.weight(1f),
                aspect = 0.72f,
            ) {
                when (mode) {
                    ThemeMode.LIGHT -> MiniScreen(light)
                    ThemeMode.DARK -> MiniScreen(dark)
                    ThemeMode.DARK_OLED -> MiniScreen(oled)
                    ThemeMode.SYSTEM_AUTO -> SplitMiniScreen(light, dark)
                }
            }
        }
    }
    HintText(selected.description())
}

/**
 * Theme choices as three picture tiles (Default, Monet, Presets) with one helper line, then a Liquid Glass switch with its own
 * preview. Presets opens its own page of colour schemes; picking one there applies and saves it at once.
 */
@Composable
fun ColorEngineChoices(
    settings: AppSettings,
    onSelect: (ColorEngine) -> Unit,
    onPresetSelected: (ThemePreset) -> Unit,
    onGlassToggle: (Boolean) -> Unit,
    onFrostedToggle: (Boolean) -> Unit,
) {
    var showPresets by remember { mutableStateOf(false) }
    val isDark = isDarkTheme(settings)
    val monet = monetPreview(isDark)
    SectionTitle("Theme")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PreviewTile(
            title = "Default",
            selected = settings.colorEngine == ColorEngine.DEFAULT_PALETTE,
            onClick = { onSelect(ColorEngine.DEFAULT_PALETTE) },
            modifier = Modifier.weight(1f),
        ) { MiniScreen((if (isDark) DefaultPalette.dark else DefaultPalette.light).previewColors()) }
        PreviewTile(
            title = "Monet",
            selected = settings.colorEngine == ColorEngine.MONET_WALLPAPER,
            enabled = monet != null && isMonetAndBlurSupported,
            onClick = { onSelect(ColorEngine.MONET_WALLPAPER) },
            modifier = Modifier.weight(1f),
        ) { monet?.let { MiniScreen(it) } }
        PreviewTile(
            title = "Presets",
            selected = settings.colorEngine == ColorEngine.PRESET,
            onClick = { showPresets = true },
            modifier = Modifier.weight(1f),
        ) { MiniScreen(settings.themePreset.palette().variantFor(isDark).second.previewColors()) }
    }
    HintText(
        when (settings.colorEngine) {
            ColorEngine.DEFAULT_PALETTE -> ColorEngine.DEFAULT_PALETTE.description()
            ColorEngine.MONET_WALLPAPER ->
                if (isMonetAndBlurSupported) ColorEngine.MONET_WALLPAPER.description() else REQUIRES_ANDROID_12
            ColorEngine.PRESET -> "${settings.themePreset.palette().name}. Tap Presets to change."
        },
    )
    val glassHelper = when {
        !isMonetAndBlurSupported -> "Needs Android 12 or newer"
        settings.visualQuality == VisualQuality.PERFORMANCE -> "Off in Performance mode"
        else -> "Frosted, see-through surfaces over your theme"
    }
    val glassEnabled = isMonetAndBlurSupported && settings.visualQuality != VisualQuality.PERFORMANCE
    AppCard(
        modifier = Modifier.fillMaxWidth().alpha(if (glassEnabled) 1f else 0.5f),
        onClick = if (glassEnabled) { { onGlassToggle(!settings.liquidGlass) } } else null,
        highlighted = settings.liquidGlass,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(60.dp).clip(RoundedCornerShape(18.dp))) { GlassPreview() }
            Column(modifier = Modifier.weight(1f)) {
                Text("Liquid Glass", style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    glassHelper,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            Switch(checked = settings.liquidGlass, onCheckedChange = null, enabled = glassEnabled)
        }
    }
    // Appears with the same springy expansion as the notes list on the Attendance tab, and folds away with Liquid Glass.
    Reveal(visible = settings.liquidGlass && isMonetAndBlurSupported) {
        GreyedOut(disabled = settings.visualQuality == VisualQuality.PERFORMANCE) {
            SwitchCard(
                title = "Frost popups too",
                subtitle = "Profile, search, Add Entry and dialogs. Turn off if they are hard to read.",
                checked = settings.frostedOverlays,
                onCheckedChange = onFrostedToggle,
            )
        }
    }
    if (showPresets) {
        PresetsPage(
            selected = settings.themePreset.takeIf { settings.colorEngine == ColorEngine.PRESET },
            isDark = isDark,
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
