package skywa1ker404.absencium.ui.settings

import skywa1ker404.absencium.ui.theme.toComposeShape
import skywa1ker404.absencium.ui.components.SettingSwitchRow
import skywa1ker404.absencium.data.settings.DisplaySize
import skywa1ker404.absencium.data.settings.AvatarShape
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import skywa1ker404.absencium.ui.components.ExpressiveChip
import skywa1ker404.absencium.data.settings.AppStyle
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import skywa1ker404.absencium.ui.theme.DotFamily
import skywa1ker404.absencium.ui.theme.PixelCornerShape
import skywa1ker404.absencium.ui.theme.PixelFamily
import skywa1ker404.absencium.ui.theme.pixelBevel
import skywa1ker404.absencium.ui.components.GreyedOut
import skywa1ker404.absencium.ui.theme.isMonetAndBlurSupported
import skywa1ker404.absencium.ui.theme.palette

@Destination<RootGraph>
@Composable
fun AppearanceSettingsScreen(navigator: DestinationsNavigator) {
    val viewModel = rememberSettingsViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    ScreenScaffold(title = "Appearance", onBack = { navigator.navigateUp() }, largeTitle = true) {
        // Same order as the reference (picture 33): preview, appearance, display size, colour, styles, shape settings, quality.
        AppearancePreview(settings)
        ThemeModeChoices(selected = settings.themeMode, onSelect = viewModel::setThemeMode)
        AppCard(modifier = Modifier.fillMaxWidth()) {
            SettingSwitchRow(
                title = "Pure black in dark mode",
                subtitle = "Black backgrounds when dark is on. Saves power on OLED screens.",
                checked = settings.pureBlack,
                onCheckedChange = viewModel::setPureBlack,
            )
        }
        DisplaySizeChoices(selected = settings.displaySize, onSelect = viewModel::setDisplaySize)
        // Dot matrix is black and white with one red accent, so it has no colour choices; the other styles use the chosen colours.
        if (settings.appStyle != AppStyle.DOT_MATRIX) {
            ColorEngineChoices(
                settings = settings,
                onSelect = viewModel::setColorEngine,
                onPresetSelected = viewModel::setThemePreset,
                onGlassToggle = viewModel::setLiquidGlass,
                onFrostedToggle = viewModel::setFrostedOverlays,
                showGlass = false,
            )
        }

        StyleChoices(selected = settings.appStyle, onSelect = viewModel::setAppStyle)
        // Only the chosen style's own settings are shown.
        if (settings.appStyle == AppStyle.LIQUID_GLASS) {
            if (!isMonetAndBlurSupported) {
                HintText(REQUIRES_ANDROID_12)
            } else {
                GreyedOut(disabled = settings.visualQuality == VisualQuality.PERFORMANCE) {
                    // GreyedOut is a Box, so the two cards need their own column or they draw on top of each other.
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SwitchCard(
                            title = "Frosted popups",
                            subtitle = "Profile, search, Add Entry and dialogs",
                            checked = settings.frostedOverlays,
                            onCheckedChange = viewModel::setFrostedOverlays,
                        )
                        GlassBlurSlider(settings.glassBlurDp) { viewModel.setGlassBlur(it) }
                    }
                }
            }
        }

        if (settings.appStyle != AppStyle.PIXELATED) {
            SliderCard(
                title = "Corner radius",
                subtitle = "How round cards, sheets and buttons are",
                value = settings.cornerRadiusDp,
                range = AppSettings.MIN_CORNER_RADIUS..AppSettings.MAX_CORNER_RADIUS,
                valueLabel = { "${it}dp" },
                onValueChangeFinished = viewModel::setCornerRadius,
            )
        }

        BottomBarChoices(
            anchored = settings.bottomBarAnchored,
            labels = settings.bottomBarLabels,
            onAnchored = viewModel::setBottomBarAnchored,
            onLabels = viewModel::setBottomBarLabels,
        )

        AvatarChoices(shape = settings.avatarShape, frame = settings.avatarFrame, onShape = viewModel::setAvatarShape, onFrame = viewModel::setAvatarFrame)

        VisualQualityChoices(
            selected = settings.visualQuality,
            onSelect = viewModel::setVisualQuality,
        )
    }
}

/** A live miniature of the app in the current theme, style, corners and avatar: it changes the moment a choice below changes. */
@Composable
private fun AppearancePreview(settings: AppSettings) {
    val colors = MaterialTheme.colorScheme
    val pill = skywa1ker404.absencium.ui.theme.pillShape()
    SectionTitle("Preview")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(colors.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Absencium", style = MaterialTheme.typography.titleLarge, color = colors.onBackground, maxLines = 1, modifier = Modifier.weight(1f))
            skywa1ker404.absencium.ui.profile.AvatarPicture(fileName = settings.profileAvatar, initials = settings.displayName.ifBlank { "A" }, size = 36)
        }
        Box(
            modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(colors.primaryContainer).padding(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.width(120.dp).height(8.dp).clip(pill).background(colors.onPrimaryContainer.copy(alpha = 0.7f)))
                Box(Modifier.width(76.dp).height(8.dp).clip(pill).background(colors.onPrimaryContainer.copy(alpha = 0.4f)))
                skywa1ker404.absencium.ui.components.AppLinearProgress(
                    progress = { 0.65f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(pill),
                    color = colors.primary,
                    trackColor = colors.onPrimaryContainer.copy(alpha = 0.18f),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(44.dp).clip(MaterialTheme.shapes.medium).background(colors.secondaryContainer))
            Box(Modifier.weight(1f).height(44.dp).clip(MaterialTheme.shapes.medium).background(colors.tertiaryContainer))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            skywa1ker404.absencium.ui.components.AppSwitch(checked = true, onCheckedChange = null)
            skywa1ker404.absencium.ui.components.AppSwitch(checked = false, onCheckedChange = null)
            skywa1ker404.absencium.ui.components.AppSlider(value = 0.4f, onValueChange = {}, enabled = false, modifier = Modifier.weight(1f))
            skywa1ker404.absencium.ui.components.AppLoading(modifier = Modifier.size(32.dp))
        }
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally).clip(pill).background(colors.surfaceContainerHigh).padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(34.dp).height(18.dp).clip(pill).background(colors.primary))
            repeat(3) { Box(Modifier.size(10.dp).clip(pill).background(colors.onSurfaceVariant.copy(alpha = 0.6f))) }
        }
    }
}

/** The bottom bar as two picture tiles, Floating (the pill) or Anchored (fixed to the edge); labels are an Anchored-only choice. */
@Composable
private fun BottomBarChoices(anchored: Boolean, labels: Boolean, onAnchored: (Boolean) -> Unit, onLabels: (Boolean) -> Unit) {
    val c = MaterialTheme.colorScheme
    SectionTitle("Bottom bar")
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(false to "Floating", true to "Anchored").forEach { (isAnchored, name) ->
                PreviewTile(
                    title = name,
                    selected = anchored == isAnchored,
                    onClick = { onAnchored(isAnchored) },
                    modifier = Modifier.weight(1f),
                    aspect = 1.25f,
                ) {
                    val pill = skywa1ker404.absencium.ui.theme.pillShape()
                    Box(Modifier.fillMaxSize().background(c.background)) {
                        Box(Modifier.align(Alignment.TopStart).padding(10.dp).fillMaxWidth(0.4f).height(6.dp).clip(pill).background(c.onBackground.copy(alpha = 0.8f)))
                        Row(
                            modifier = if (isAnchored) {
                                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(22.dp).background(c.surfaceContainerHigh)
                            } else {
                                Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp).fillMaxWidth(0.68f).height(20.dp).clip(pill).background(c.surfaceContainerHigh)
                            },
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.width(18.dp).height(10.dp).clip(pill).background(c.secondaryContainer))
                            repeat(2) { Box(Modifier.size(6.dp).clip(pill).background(c.onSurfaceVariant.copy(alpha = 0.6f))) }
                        }
                    }
                }
            }
        }
    }
    // Labels only exist on the anchored bar; the floating pill shows just the selected tab's label.
    androidx.compose.animation.AnimatedVisibility(visible = anchored) {
        AppCard(modifier = Modifier.fillMaxWidth()) {
            SettingSwitchRow(
                title = "Show labels",
                subtitle = "A name under every icon in the bottom bar",
                checked = labels,
                onCheckedChange = onLabels,
            )
        }
    }
}

/** Whole-app size as four "Aa" tiles. */
@Composable
private fun DisplaySizeChoices(selected: DisplaySize, onSelect: (DisplaySize) -> Unit) {
    SectionTitle("Display size")
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DisplaySize.entries.forEach { size ->
                ChoiceTile(selected = selected == size, onClick = { onSelect(size) }, label = size.label, modifier = Modifier.weight(1f)) {
                    Text("Aa", fontSize = (18 * (0.85f + size.ordinal * 0.15f)).sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
    HintText("Scales text, icons and spacing everywhere in Absencium. Your phone's own display size and font size still apply.")
}

/** Avatar shapes as tiles, then the frame switch. */
@Composable
private fun AvatarChoices(shape: AvatarShape, frame: Boolean, onShape: (AvatarShape) -> Unit, onFrame: (Boolean) -> Unit) {
    SectionTitle("Avatars")
    // Pixelated and Dot matrix fix the avatar shape (stair-step square, full circle), so the choice is hidden there.
    val fixedShape = skywa1ker404.absencium.ui.theme.LocalAppStyle.current == AppStyle.PIXELATED ||
        skywa1ker404.absencium.ui.theme.LocalAppStyle.current == AppStyle.DOT_MATRIX
    if (!fixedShape) AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AvatarShape.entries.forEach { option ->
                ChoiceTile(selected = shape == option, onClick = { onShape(option) }, label = option.label, modifier = Modifier.weight(1f)) {
                    Box(Modifier.size(40.dp).clip(option.toComposeShape()).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)))
                }
            }
        }
    }
    AppCard(modifier = Modifier.fillMaxWidth()) {
        SettingSwitchRow(
            title = "Show frame",
            subtitle = "A thin line along the avatar's border",
            checked = frame,
            onCheckedChange = onFrame,
        )
    }
}

/** A rounded tile with something in the middle and a label under it; the chosen one gets a ring in the primary colour. */
@Composable
private fun ChoiceTile(selected: Boolean, onClick: () -> Unit, label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val rim by androidx.compose.animation.animateColorAsState(if (selected) colors.primary else Color.Transparent, label = "tileRim")
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) colors.secondaryContainer else colors.surfaceContainerHighest.copy(alpha = 0.6f))
            .border(2.dp, rim, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.height(44.dp), contentAlignment = Alignment.Center) { content() }
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurface, maxLines = 1)
    }
}

/** The app's look, one at a time, as four picture tiles: Default, Liquid Glass, Pixelated or Dot matrix (black and white, Nothing-inspired). */
@Composable
fun StyleChoices(selected: AppStyle, onSelect: (AppStyle) -> Unit) {
    SectionTitle("Styles")
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text("Themes style", style = MaterialTheme.typography.titleMedium, maxLines = 1)
        Text(
            "currently selected: ${styleName(selected)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppStyle.entries.chunked(2).forEach { pair ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { style ->
                        PreviewTile(
                            title = styleName(style),
                            selected = selected == style,
                            onClick = { onSelect(style) },
                            modifier = Modifier.weight(1f),
                            aspect = 1.25f,
                        ) { StyleMini(style) }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
    HintText(
        when (selected) {
            AppStyle.DEFAULT -> "The standard Absencium look."
            AppStyle.LIQUID_GLASS -> "Translucent frosted glass on your colour theme."
            AppStyle.PIXELATED -> "Stepped, square corners on your colour theme."
            AppStyle.DOT_MATRIX -> "Black and white with one red accent and a dot-matrix font. No colour themes."
        },
    )
}

private fun styleName(style: AppStyle) = when (style) {
    AppStyle.DEFAULT -> "Default"
    AppStyle.LIQUID_GLASS -> "Liquid Glass"
    AppStyle.PIXELATED -> "Pixelated"
    AppStyle.DOT_MATRIX -> "Dot matrix"
}

/** Visual quality choices, shared by Settings and the onboarding wizard. Applies the moment one is tapped. */
@Composable
fun VisualQualityChoices(
    selected: VisualQuality,
    onSelect: (VisualQuality) -> Unit,
) {
    SectionTitle("Visual quality")
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
    SectionTitle("Appearance")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ThemeMode.entries.filter { it != ThemeMode.DARK_OLED }.forEach { mode ->
            PreviewTile(
                title = when (mode) {
                    ThemeMode.LIGHT -> "Light"
                    ThemeMode.DARK -> "Dark"
                    ThemeMode.DARK_OLED -> "OLED"
                    ThemeMode.SYSTEM_AUTO -> "System"
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
    showGlass: Boolean = true,
) {
    var showPresets by remember { mutableStateOf(false) }
    val isDark = isDarkTheme(settings)
    val monet = monetPreview(isDark)
    SectionTitle("Color")
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
    if (showGlass) {
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
                skywa1ker404.absencium.ui.components.AppSwitch(checked = settings.liquidGlass, onCheckedChange = null, enabled = glassEnabled)
            }
        }
        // Appears with the same springy expansion as the notes list on the Attendance tab, and folds away with Liquid Glass.
        Reveal(visible = settings.liquidGlass && isMonetAndBlurSupported) {
            GreyedOut(disabled = settings.visualQuality == VisualQuality.PERFORMANCE) {
                SwitchCard(
                    title = "Frosted popups",
                    subtitle = "Profile, search, Add Entry and dialogs",
                    checked = settings.frostedOverlays,
                    onCheckedChange = onFrostedToggle,
                )
            }
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
        subtitle = "Blur strength of floating bars",
        value = value,
        range = AppSettings.MIN_GLASS_BLUR..AppSettings.MAX_GLASS_BLUR,
        valueLabel = { "$it px" },
        onValueChangeFinished = onChange,
    )
}

/** A small picture of one style: what a card, a button and the text look like in it. */
@Composable
private fun StyleMini(style: AppStyle) {
    val c = MaterialTheme.colorScheme
    when (style) {
        AppStyle.DEFAULT -> MiniScreen(PreviewColors(c.background, c.surfaceContainerHigh, c.onBackground, c.primary, c.secondary))
        AppStyle.LIQUID_GLASS -> GlassPreview()
        AppStyle.PIXELATED -> {
            val face = TextStyle(fontFamily = PixelFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            Column(
                modifier = Modifier.fillMaxSize().background(c.background).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(Modifier.fillMaxWidth(0.4f).height(8.dp).clip(PixelCornerShape(1f)).background(c.onBackground.copy(alpha = 0.85f)))
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier.weight(1f).fillMaxSize().clip(PixelCornerShape(2f)).background(c.primary).pixelBevel(c.outline, 1.5f),
                        contentAlignment = Alignment.Center,
                    ) { Text("PLAY", style = face, color = c.onPrimary, maxLines = 1) }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            Modifier.weight(1f).fillMaxWidth().clip(PixelCornerShape(1.5f)).background(c.secondaryContainer).pixelBevel(c.outline, 1f),
                            contentAlignment = Alignment.Center,
                        ) { Text("OK", style = face.copy(fontSize = 8.sp), color = c.onSecondaryContainer, maxLines = 1) }
                        // A block switch (on) and a block bar.
                        Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(width = 24.dp, height = 13.dp).clip(PixelCornerShape(1f)).background(c.primary).pixelBevel(c.outline, 1f)) {
                                Box(Modifier.align(Alignment.CenterEnd).padding(2.dp).size(9.dp).clip(PixelCornerShape(1f)).background(c.onPrimary))
                            }
                            skywa1ker404.absencium.ui.components.StepBar(0.6f, Modifier.weight(1f).height(6.dp), c.primary, c.surfaceContainerHighest, dotted = false)
                        }
                    }
                }
            }
        }
        AppStyle.DOT_MATRIX -> Box(Modifier.fillMaxSize().background(Color(0xFF0B0B0B)).padding(10.dp)) {
            Text("Aa", style = TextStyle(fontFamily = DotFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp), color = Color.White, maxLines = 1, modifier = Modifier.align(Alignment.TopStart))
            Box(Modifier.align(Alignment.TopEnd).size(8.dp).clip(CircleShape).background(Color(0xFFD71921)))
            // A dotted progress bar and the dotted loader.
            Row(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                skywa1ker404.absencium.ui.components.StepBar(0.6f, Modifier.weight(1f).height(5.dp), Color.White, Color.White.copy(alpha = 0.2f), dotted = true)
                skywa1ker404.absencium.ui.components.StepSpinner(Modifier.size(18.dp), Color(0xFFD71921), dotted = true)
            }
        }
    }
}
