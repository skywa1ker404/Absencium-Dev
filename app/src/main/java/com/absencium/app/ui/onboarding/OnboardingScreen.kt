package com.absencium.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.absencium.app.data.settings.AppSettings
import com.absencium.app.data.settings.ColorEngine
import com.absencium.app.ui.AiKeyViewModel
import com.absencium.app.ui.StorageViewModel
import com.absencium.app.ui.components.AppCard
import com.absencium.app.ui.components.HintText
import com.absencium.app.ui.components.PillButton
import com.absencium.app.ui.components.SectionTitle
import com.absencium.app.ui.components.SwitchCard
import com.absencium.app.ui.components.SliderCard
import com.absencium.app.ui.components.StepIndicator
import com.absencium.app.ui.components.floatingGlass
import com.absencium.app.ui.containerViewModel
import com.absencium.app.ui.settings.AiConnectionSection
import com.absencium.app.ui.settings.AiKeySection
import com.absencium.app.ui.settings.AskAiToggle
import com.absencium.app.ui.settings.ColorEngineChoices
import com.absencium.app.ui.settings.GlassBlurSlider
import com.absencium.app.ui.settings.MomentumToggle
import com.absencium.app.ui.settings.StorageLocationSection
import com.absencium.app.ui.settings.StorageModuleToggles
import com.absencium.app.ui.settings.ThemeModeChoices
import com.absencium.app.ui.settings.VisualQualityChoices
import com.absencium.app.ui.settings.aiReady
import com.absencium.app.ui.settings.rememberSettingsViewModel
import com.absencium.app.ui.theme.LocalGlassEnabled
import com.absencium.app.ui.theme.isMonetAndBlurSupported
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch

private const val STEP_COUNT = 3
private const val LAST_STEP = STEP_COUNT - 1

private val StepTitles = listOf("Theme & Appearance Setup", "Notes Storage Directory", "AI Engine & Optional Modules")
private val StepSubtitles = listOf(
    "Pick a visual quality for your phone, then your theme (Monet, the Default Theme or a preset) and whether to add Liquid Glass on top. Changes apply instantly.",
    "Choose where your notes, PDFs and recordings live, and your minimum safe attendance.",
    "Set up an optional AI connection and the background modules you want. All can be changed later in Settings.",
)

/** The first-run wizard. The app shows it until the last step is finished. */
@Composable
fun OnboardingScreen() {
    val settingsViewModel = rememberSettingsViewModel()
    val storageViewModel = containerViewModel { StorageViewModel(it) }
    val aiKeyViewModel = containerViewModel { AiKeyViewModel(it) }
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val storage by storageViewModel.state.collectAsStateWithLifecycle()
    val keyState by aiKeyViewModel.state.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(pageCount = { STEP_COUNT })
    val scope = rememberCoroutineScope()
    val step = pagerState.currentPage
    var finishing by remember { mutableStateOf(false) }

    fun goTo(page: Int) {
        scope.launch { pagerState.animateScrollToPage(page) }
    }

    BackHandler(enabled = step > 0) { goTo(step - 1) }

    val backdrop = if (LocalGlassEnabled.current) rememberLayerBackdrop() else null
    val background = MaterialTheme.colorScheme.background

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                .background(background)
                .statusBarsPadding(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "FIRST-LAUNCH SETUP",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Absencium.db • Offline-First",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "Step ${step + 1} of $STEP_COUNT",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
                StepIndicator(current = step, total = STEP_COUNT, modifier = Modifier.padding(top = 8.dp))
                Text(text = StepTitles[step], style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = StepSubtitles[step],
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalPager(
                state = pagerState,
                // Steps are changed with the buttons so a required step cannot be swiped past.
                userScrollEnabled = false,
                modifier = Modifier.weight(1f),
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when (page) {
                        0 -> ThemeStep(settings, settingsViewModel)
                        1 -> StorageStep(settings, settingsViewModel, storageViewModel)
                        else -> {
                            SectionTitle("Ask AI")
                            HintText("Optional. You can skip this and set it up later in Settings.")
                            AskAiToggle(settings, settingsViewModel, ready = aiReady(settings, keyState.hasKey))
                            AiConnectionSection(settings, settingsViewModel)
                            SectionTitle("API key")
                            AiKeySection(aiKeyViewModel)

                            SectionTitle("Optional intelligence modules")
                            HintText("All off by default.")
                            MomentumToggle(settings, settingsViewModel)
                            StorageModuleToggles(settings, settingsViewModel)
                        }
                    }
                    // Room so the last card can scroll clear of the floating buttons.
                    Spacer(modifier = Modifier.height(120.dp))
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .floatingGlass(backdrop, RoundedCornerShape(36.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (step > 0) {
                PillButton(text = "Back", onClick = { goTo(step - 1) }, tonal = true)
            } else {
                Spacer(modifier = Modifier)
            }
            if (step < LAST_STEP) {
                PillButton(
                    text = "Next Step →",
                    onClick = { goTo(step + 1) },
                    // The notes folder is mandatory, so step 2 cannot be passed without choosing one.
                    enabled = step != 1 || storage.mode != null,
                )
            } else {
                PillButton(
                    text = "Complete Setup",
                    onClick = {
                        finishing = true
                        settingsViewModel.completeOnboarding()
                    },
                    enabled = storage.mode != null,
                    loading = finishing,
                )
            }
        }
    }
}

@Composable
private fun ThemeStep(settings: AppSettings, viewModel: com.absencium.app.ui.settings.SettingsViewModel) {
    VisualQualityChoices(
        selected = settings.visualQuality,
        onSelect = { viewModel.setVisualQuality(it) },
    )
    ThemeModeChoices(selected = settings.themeMode, onSelect = { viewModel.setThemeMode(it) })
    ColorEngineChoices(
        settings = settings,
        onSelect = { viewModel.setColorEngine(it) },
        onPresetSelected = { viewModel.setThemePreset(it) },
        onGlassToggle = { viewModel.setLiquidGlass(it) },
        onFrostedToggle = { viewModel.setFrostedOverlays(it) },
    )
    if (settings.liquidGlass && isMonetAndBlurSupported) {
        SectionTitle("Frosted glass intensity")
        GlassBlurSlider(settings.glassBlurDp) { viewModel.setGlassBlur(it) }
    }
}

@Composable
private fun StorageStep(
    settings: AppSettings,
    viewModel: com.absencium.app.ui.settings.SettingsViewModel,
    storageViewModel: StorageViewModel,
) {
    StorageLocationSection(storageViewModel)
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
