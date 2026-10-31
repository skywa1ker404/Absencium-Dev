package skywa1ker404.absencium.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import skywa1ker404.absencium.data.settings.AppSettings
import skywa1ker404.absencium.data.settings.ColorEngine
import skywa1ker404.absencium.data.settings.ThemeMode
import skywa1ker404.absencium.data.settings.VisualQuality
import skywa1ker404.absencium.ui.AiProvidersViewModel
import skywa1ker404.absencium.ui.REQUIRES_ANDROID_12
import skywa1ker404.absencium.ui.StorageViewModel
import skywa1ker404.absencium.ui.components.AnimatedPopup
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.GreyedOut
import skywa1ker404.absencium.ui.components.ExpandChevron
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.OptionCard
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.components.PopupShell
import skywa1ker404.absencium.ui.components.Reveal
import skywa1ker404.absencium.ui.components.SectionTitle
import skywa1ker404.absencium.ui.components.SliderCard
import skywa1ker404.absencium.ui.components.StepIndicator
import skywa1ker404.absencium.ui.components.SwitchCard
import skywa1ker404.absencium.ui.components.floatingGlass
import skywa1ker404.absencium.ui.containerViewModel
import skywa1ker404.absencium.ui.description
import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.label
import skywa1ker404.absencium.ui.settings.AiProviderForm
import skywa1ker404.absencium.ui.settings.AskAiToggle
import skywa1ker404.absencium.ui.settings.GlassBlurSlider
import skywa1ker404.absencium.ui.settings.MomentumToggle
import skywa1ker404.absencium.ui.settings.PresetsPage
import skywa1ker404.absencium.ui.settings.SettingsViewModel
import skywa1ker404.absencium.ui.settings.StorageLocationSection
import skywa1ker404.absencium.ui.settings.StorageModuleToggles
import skywa1ker404.absencium.ui.settings.rememberSettingsViewModel
import skywa1ker404.absencium.ui.splash.AbsenciumMark
import skywa1ker404.absencium.ui.splash.ThemedStage
import skywa1ker404.absencium.ui.theme.LocalBackdrop
import skywa1ker404.absencium.ui.theme.LocalGlassEnabled
import skywa1ker404.absencium.ui.theme.LocalQuality
import skywa1ker404.absencium.ui.theme.isDarkTheme
import skywa1ker404.absencium.ui.theme.isMonetAndBlurSupported
import skywa1ker404.absencium.ui.theme.palette
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Pages 0..2 are essential (look, attendance, notes folder); 3 and 4 are optional and can be skipped. */
private const val PAGE_COUNT = 5
private const val ESSENTIAL_PAGES = 3
private const val STORAGE_PAGE = 2
private const val LAST_PAGE = PAGE_COUNT - 1

private const val PANEL_NONE = -1
private const val PANEL_QUALITY = 0
private const val PANEL_MODE = 1
private const val PANEL_THEME = 2
private const val PANEL_GLASS = 3

/** Pixels of one-way scrolling before the Back and Next buttons fold to icons, and before they open again. */
private const val CompactTravelPx = 90f
private const val ExpandTravelPx = 60f

/**
 * The first-run setup. It opens on the logo and the look of the app (the whole screen, logo included, follows what is
 * chosen), then asks for the essentials, then offers optional steps that can be skipped. The app shows it until it is
 * finished; the feature tour comes after it.
 */
@Composable
fun OnboardingScreen() {
    val settingsViewModel = rememberSettingsViewModel()
    val storageViewModel = containerViewModel { StorageViewModel(it) }
    val aiProvidersViewModel = containerViewModel { AiProvidersViewModel(it) }
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val storage by storageViewModel.state.collectAsStateWithLifecycle()
    val providerRows by aiProvidersViewModel.rows.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val page = pagerState.currentPage
    val storageChosen = storage.mode != null
    var finishing by remember { mutableStateOf(false) }
    var showSkipDialog by rememberSaveable { mutableStateOf(false) }
    // Which of the four look options is open on the first page. Kept here so changing the theme cannot close it.
    var panel by rememberSaveable { mutableIntStateOf(PANEL_NONE) }
    // Held as State and read only by the buttons, so scrolling does not recompose the pages.
    val compactState = remember { mutableStateOf(false) }

    fun goTo(target: Int) {
        scope.launch { pagerState.animateScrollToPage(target) }
    }

    fun finish() {
        if (finishing) return
        finishing = true
        settingsViewModel.completeOnboarding()
    }

    BackHandler(enabled = page > 0 && !finishing) {
        compactState.value = false
        goTo(page - 1)
    }

    // The notes folder is mandatory: a swipe past it settles back on it.
    val limit = if (storageChosen) LAST_PAGE else STORAGE_PAGE
    LaunchedEffect(pagerState.settledPage, limit) {
        if (pagerState.settledPage > limit) pagerState.animateScrollToPage(limit)
    }
    // Swiping sideways works like Next and Back and folds both buttons to icons.
    val dragged by pagerState.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(dragged) { if (dragged) compactState.value = true }

    val scrollConnection = remember {
        object : NestedScrollConnection {
            // One-way distance; it restarts when the direction changes so small wobbles never flip the buttons.
            private var travelled = 0f

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y
                if (dy == 0f) return Offset.Zero
                if (travelled * dy < 0f) travelled = 0f
                travelled += dy
                if (travelled < -CompactTravelPx && !compactState.value) {
                    compactState.value = true
                } else if (travelled > ExpandTravelPx && compactState.value) {
                    compactState.value = false
                }
                return Offset.Zero
            }
        }
    }

    val glass = LocalGlassEnabled.current
    val backdrop = if (glass) rememberLayerBackdrop() else null

    Box(modifier = Modifier.fillMaxSize().nestedScroll(scrollConnection)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier),
        ) {
            ThemedStage(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StepIndicator(current = page, total = PAGE_COUNT)
                        Crossfade(targetState = page >= ESSENTIAL_PAGES, label = "stage") { optional ->
                            Text(
                                text = if (optional) "Optional" else "Essential",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                            )
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f),
                        key = { it },
                    ) { index ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            when (index) {
                                0 -> LookPage(settings, settingsViewModel, panel) { panel = it }
                                1 -> {
                                    PageHeader("Attendance rules", "Set the attendance you want to stay above, and which weekend days are holidays.")
                                    AttendanceStep(settings, settingsViewModel)
                                }
                                2 -> {
                                    PageHeader("Where your notes live", "Choose a folder for your notes, PDFs and recordings. This one is required.")
                                    StorageLocationSection(storageViewModel)
                                    if (!storageChosen) HintText("Choose a folder to continue.")
                                }
                                3 -> {
                                    PageHeader("AI Features", "Optional. Connect an AI to explain notes and read photos. You can set this up later in Settings.")
                                    AskAiToggle(settings, settingsViewModel, ready = providerRows.any { it.active && it.complete })
                                    AiProviderForm(
                                        initial = remember { providerRows.firstOrNull { it.active }?.provider },
                                        viewModel = aiProvidersViewModel,
                                        onDone = {},
                                        saveLabel = "Save provider",
                                    )
                                }
                                else -> {
                                    PageHeader("Optional modules", "All off by default. Turn on what you want; each can be changed later in Settings.")
                                    MomentumToggle(settings, settingsViewModel)
                                    StorageModuleToggles(settings, settingsViewModel)
                                }
                            }
                            // Room so the last card can scroll clear of the floating buttons.
                            Spacer(modifier = Modifier.height(130.dp))
                        }
                    }
                }
            }
        }

        OnboardingBar(
            page = page,
            compactState = compactState,
            nextEnabled = (page != STORAGE_PAGE || storageChosen) && !finishing,
            backdrop = backdrop,
            onBack = {
                compactState.value = false
                goTo(page - 1)
            },
            onNext = {
                compactState.value = false
                if (page == LAST_PAGE) finish() else goTo(page + 1)
            },
            onSkip = { showSkipDialog = true },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        // The skip question is a popup in this screen's own layer, so it can frost the page behind it.
        CompositionLocalProvider(LocalBackdrop provides backdrop) {
        AnimatedPopup(if (showSkipDialog) true else null) {
        PopupShell(onDismissRequest = { showSkipDialog = false }) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Skip the optional steps?", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Every setting, the essential ones and the optional ones, is in the app's Settings, so you can set up anything later. Do you want to skip?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    PillButton(text = "No", onClick = { showSkipDialog = false }, tonal = true)
                    PillButton(
                        text = "Yes, Skip",
                        onClick = {
                            showSkipDialog = false
                            finish()
                        },
                    )
                }
            }
        }
        }
        }
    }
}

@Composable
private fun PageHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// Page 1: the logo and the four look options
// ---------------------------------------------------------------------------------------------------------------------

@Composable
private fun LookPage(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    panel: Int,
    onPanel: (Int) -> Unit,
) {
    var showPresets by remember { mutableStateOf(false) }
    // Liquid Glass off closes its own panel.
    LaunchedEffect(settings.liquidGlass) { if (!settings.liquidGlass && panel == PANEL_GLASS) onPanel(PANEL_NONE) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // The logo is drawn in the theme being chosen below, so it changes as the options do.
        AbsenciumMark(size = 112.dp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Absencium",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Make it yours first. Everything here can be changed later in Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }

    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OptionTile(
            icon = AppIcons.Tune,
            title = "Visual quality",
            value = settings.visualQuality.label(),
            open = panel == PANEL_QUALITY,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            onClick = { onPanel(if (panel == PANEL_QUALITY) PANEL_NONE else PANEL_QUALITY) },
        )
        OptionTile(
            icon = AppIcons.Contrast,
            title = "Theme mode",
            value = settings.themeMode.label(),
            open = panel == PANEL_MODE,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            onClick = { onPanel(if (panel == PANEL_MODE) PANEL_NONE else PANEL_MODE) },
        )
    }
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OptionTile(
            icon = AppIcons.Palette,
            title = "Themes",
            value = if (settings.colorEngine == ColorEngine.PRESET) settings.themePreset.palette().name else settings.colorEngine.label(),
            open = panel == PANEL_THEME,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            onClick = { onPanel(if (panel == PANEL_THEME) PANEL_NONE else PANEL_THEME) },
        )
        OptionTile(
            icon = AppIcons.AutoAwesome,
            title = "Liquid Glass",
            value = when {
                !isMonetAndBlurSupported -> "Not supported by your Android version"
                settings.visualQuality == VisualQuality.PERFORMANCE -> "Not in Performance mode"
                settings.liquidGlass -> "On"
                else -> "Off"
            },
            open = panel == PANEL_GLASS,
            active = settings.liquidGlass,
            enabled = isMonetAndBlurSupported && settings.visualQuality != VisualQuality.PERFORMANCE,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            onClick = {
                // One tap turns glass on and opens its options; once it is on, the tile just opens and closes them.
                if (!settings.liquidGlass) {
                    viewModel.setLiquidGlass(true)
                    onPanel(PANEL_GLASS)
                } else {
                    onPanel(if (panel == PANEL_GLASS) PANEL_NONE else PANEL_GLASS)
                }
            },
        )
    }

    // Only one panel is open at a time; each opens below the grid with the app's springy expansion.
    Column(modifier = Modifier.fillMaxWidth()) {
        Reveal(visible = panel == PANEL_QUALITY) {
            PanelColumn {
                HintText("Pick what suits your phone. Every feature stays in all three.")
                VisualQuality.entries.forEach { level ->
                    OptionCard(
                        title = level.label(),
                        subtitle = level.description(),
                        selected = settings.visualQuality == level,
                        onClick = { viewModel.setVisualQuality(level) },
                    )
                }
            }
        }
        Reveal(visible = panel == PANEL_MODE) {
            PanelColumn {
                ThemeMode.entries.forEach { mode ->
                    OptionCard(
                        title = mode.label(),
                        subtitle = mode.description(),
                        selected = settings.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                    )
                }
            }
        }
        Reveal(visible = panel == PANEL_THEME) {
            PanelColumn {
                listOf(ColorEngine.DEFAULT_PALETTE, ColorEngine.MONET_WALLPAPER).forEach { engine ->
                    val available = engine == ColorEngine.DEFAULT_PALETTE || isMonetAndBlurSupported
                    OptionCard(
                        title = engine.label(),
                        subtitle = if (available) engine.description() else "${engine.description()}\n$REQUIRES_ANDROID_12",
                        selected = settings.colorEngine == engine,
                        enabled = available,
                        onClick = { viewModel.setColorEngine(engine) },
                    )
                }
                OptionCard(
                    title = ColorEngine.PRESET.label(),
                    subtitle = if (settings.colorEngine == ColorEngine.PRESET) {
                        "${settings.themePreset.palette().name} • tap to change"
                    } else {
                        ColorEngine.PRESET.description()
                    },
                    selected = settings.colorEngine == ColorEngine.PRESET,
                    onClick = { showPresets = true },
                )
            }
        }
        Reveal(visible = panel == PANEL_GLASS) {
            GreyedOut(disabled = settings.visualQuality == VisualQuality.PERFORMANCE) {
            PanelColumn {
                SwitchCard(
                    title = "Liquid Glass",
                    subtitle = "Frosted, see-through glass on every surface, in the colours of your theme. Shown in Quality mode.",
                    checked = settings.liquidGlass,
                    onCheckedChange = { viewModel.setLiquidGlass(it) },
                )
                Reveal(visible = settings.liquidGlass) {
                    PanelColumn(topPadding = 0.dp) {
                        GlassBlurSlider(settings.glassBlurDp) { viewModel.setGlassBlur(it) }
                        SwitchCard(
                            title = "Apply frosted glass on all elements",
                            subtitle = "Also frost the profile, search, Add Entry, import and colour dialogs. Turn it off to keep them solid.",
                            checked = settings.frostedOverlays,
                            onCheckedChange = { viewModel.setFrostedOverlays(it) },
                        )
                    }
                }
            }
            }
        }
    }

    if (showPresets) {
        PresetsPage(
            selected = settings.themePreset.takeIf { settings.colorEngine == ColorEngine.PRESET },
            isDark = isDarkTheme(settings),
            onSelect = { viewModel.setThemePreset(it) },
            onDismiss = { showPresets = false },
        )
    }
}

@Composable
private fun PanelColumn(topPadding: Dp = 12.dp, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = topPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) { content() }
}

/** A button-shaped option of the first page: icon, what it controls, what is chosen, and a chevron that turns when open. */
@Composable
private fun OptionTile(
    icon: ImageVector,
    title: String,
    value: String,
    open: Boolean,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    AppCard(
        modifier = modifier.graphicsLayer { alpha = if (enabled) 1f else 0.5f },
        onClick = if (enabled) onClick else null,
        highlighted = open || active,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.weight(1f))
                if (enabled) ExpandChevron(expanded = open)
            }
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.titleSmall, maxLines = 2)
        }
    }
}

@Composable
private fun AttendanceStep(settings: AppSettings, viewModel: SettingsViewModel) {
    SectionTitle("Target attendance threshold")
    SliderCard(
        title = "Minimum safe attendance",
        subtitle = "Absencium warns you when a subject approaches this limit. You can change it later.",
        value = settings.attendanceThresholdPercent,
        range = AppSettings.MIN_ATTENDANCE_THRESHOLD..AppSettings.MAX_ATTENDANCE_THRESHOLD,
        valueLabel = { "$it%" },
        onValueChangeFinished = { viewModel.setAttendanceThreshold(it) },
    )
    SectionTitle("Weekends")
    SwitchCard(
        title = "Sunday is always holiday",
        subtitle = "Turn off if you have lectures on Sunday. You can change it later in Settings.",
        checked = settings.sundayHoliday,
        onCheckedChange = { viewModel.setSundayHoliday(it) },
    )
    SwitchCard(
        title = "Saturday is always holiday",
        subtitle = "Turn off if you have lectures on Saturday. You can change it later in Settings.",
        checked = settings.saturdayHoliday,
        onCheckedChange = { viewModel.setSaturdayHoliday(it) },
    )
}

// ---------------------------------------------------------------------------------------------------------------------
// The bottom buttons: Back, Skip and Next
// ---------------------------------------------------------------------------------------------------------------------

private val BarHeight = 56.dp
private val BackWidth = 100.dp
private val NextWidth = 116.dp
private val SkipSize = 52.dp
private val BarGap = 10.dp

/**
 * Next, and once the setup has moved on, Back beside it, like the navigation pill: each button is a pane of glass (or a
 * plain tonal pill) that shows its label while the page is still and folds to just its icon while the page is scrolled
 * or swiped. Tapping either opens both again.
 *
 * Back springs in when the first Next is tapped, pushing Next to the right. On the optional pages a round Skip springs
 * in between them; it never grows a label. Widths animate with the Expressive spring, so everything slides and bounces
 * instead of jumping.
 */
@Composable
private fun OnboardingBar(
    page: Int,
    compactState: State<Boolean>,
    nextEnabled: Boolean,
    backdrop: LayerBackdrop?,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val q = LocalQuality.current
    val compact = compactState.value
    val hasBack = page > 0
    val hasSkip = page >= ESSENTIAL_PAGES
    val spring = q.spring<Dp>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)

    val backWidth by animateDpAsState(if (!hasBack) 0.dp else if (compact) BarHeight else BackWidth, spring, label = "backWidth")
    val nextWidth by animateDpAsState(if (compact) BarHeight else NextWidth, spring, label = "nextWidth")
    val skipWidth by animateDpAsState(if (hasSkip) SkipSize else 0.dp, spring, label = "skipWidth")
    val backGap by animateDpAsState(if (hasBack) BarGap else 0.dp, spring, label = "backGap")
    val skipGap by animateDpAsState(if (hasSkip) BarGap else 0.dp, spring, label = "skipGap")
    val last = page == LAST_PAGE

    Row(
        modifier = modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (backWidth > 0.5.dp) {
            BarButton(
                width = backWidth,
                compactWidth = BarHeight,
                expandedWidth = BackWidth,
                label = "Back",
                icon = AppIcons.ChevronLeft,
                iconFirst = true,
                primary = false,
                enabled = true,
                backdrop = backdrop,
                onClick = onBack,
            )
        }
        Spacer(modifier = Modifier.width(backGap.coerceAtLeast(0.dp)))
        if (skipWidth > 0.5.dp) {
            // The circle pops: its scale follows the bouncy width, so it overshoots a little when it arrives.
            val pop = (skipWidth.value / SkipSize.value).coerceAtLeast(0f)
            Box(
                modifier = Modifier
                    .width(skipWidth.coerceAtLeast(0.dp))
                    .height(SkipSize)
                    .graphicsLayer {
                        scaleX = pop
                        scaleY = pop
                        alpha = pop.coerceIn(0f, 1f)
                    },
                contentAlignment = Alignment.Center,
            ) {
                BarButton(
                    width = SkipSize,
                    compactWidth = SkipSize,
                    expandedWidth = SkipSize,
                    label = null,
                    icon = AppIcons.SkipNext,
                    iconFirst = true,
                    primary = false,
                    enabled = true,
                    backdrop = backdrop,
                    description = "Skip the optional steps",
                    height = SkipSize,
                    onClick = onSkip,
                )
            }
        }
        Spacer(modifier = Modifier.width(skipGap.coerceAtLeast(0.dp)))
        BarButton(
            width = nextWidth,
            compactWidth = BarHeight,
            expandedWidth = NextWidth,
            label = if (last) "Finish" else "Next",
            icon = if (last) AppIcons.Check else AppIcons.ChevronRight,
            iconFirst = false,
            primary = true,
            enabled = nextEnabled,
            backdrop = backdrop,
            onClick = onNext,
        )
    }
}

@Composable
private fun BarButton(
    width: Dp,
    compactWidth: Dp,
    expandedWidth: Dp,
    label: String?,
    icon: ImageVector,
    iconFirst: Boolean,
    primary: Boolean,
    enabled: Boolean,
    backdrop: LayerBackdrop?,
    onClick: () -> Unit,
    description: String? = null,
    height: Dp = BarHeight,
) {
    val q = LocalQuality.current
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = q.spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "barPress",
    )
    val tint = if (primary) colors.primaryContainer else colors.surfaceContainerHigh
    val content = if (primary) colors.onPrimaryContainer else colors.onSurface
    // 0 while folded to the icon, 1 when fully open; the label's width follows it so nothing is cut mid-morph.
    val span = (expandedWidth.value - compactWidth.value).coerceAtLeast(1f)
    val openness = ((width.value - compactWidth.value) / span).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .width(width.coerceAtLeast(0.dp))
            .height(height)
            .graphicsLayer {
                scaleX = press
                scaleY = press
                alpha = if (enabled) 1f else 0.5f
            }
            .floatingGlass(backdrop, CircleShape, tint)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.wrapContentWidth(Alignment.CenterHorizontally, unbounded = true),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (iconFirst) Icon(icon, contentDescription = description ?: label, tint = content, modifier = Modifier.size(24.dp))
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = content,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .collapseWidth { openness }
                        .padding(start = if (iconFirst) 6.dp else 0.dp, end = if (iconFirst) 0.dp else 6.dp)
                        .graphicsLayer { alpha = openness },
                )
            }
            if (!iconFirst) Icon(icon, contentDescription = description ?: label, tint = content, modifier = Modifier.size(24.dp))
        }
    }
}

/** Lays its content out at full size but reports only [fraction] of its width, so a label can fold away smoothly. */
private fun Modifier.collapseWidth(fraction: () -> Float): Modifier =
    this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity))
        val shown = (placeable.width * fraction().coerceIn(0f, 1f)).roundToInt()
        layout(shown, placeable.height) { placeable.place(0, 0) }
    }.clipToBounds()
