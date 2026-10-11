package com.trajectory.vault.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trajectory.vault.data.settings.ColorEngine
import com.trajectory.vault.ui.AiKeyViewModel
import com.trajectory.vault.ui.StorageViewModel
import com.trajectory.vault.ui.components.AccentColorPicker
import com.trajectory.vault.ui.components.GlassCard
import com.trajectory.vault.ui.components.HintText
import com.trajectory.vault.ui.components.PillButton
import com.trajectory.vault.ui.components.SectionTitle
import com.trajectory.vault.ui.components.StepIndicator
import com.trajectory.vault.ui.containerViewModel
import com.trajectory.vault.ui.settings.AiKeySection
import com.trajectory.vault.ui.settings.AskAiToggle
import com.trajectory.vault.ui.settings.ColorEngineChoices
import com.trajectory.vault.ui.settings.MomentumToggle
import com.trajectory.vault.ui.settings.StorageLocationSection
import com.trajectory.vault.ui.settings.StorageModuleToggles
import com.trajectory.vault.ui.settings.ThemeModeChoices
import com.trajectory.vault.ui.settings.rememberSettingsViewModel

private const val STEP_COUNT = 3
private const val LAST_STEP = STEP_COUNT - 1

private val StepTitles = listOf("Choose your theme", "Pick your colors", "Where should notes live?")
private val StepSubtitles = listOf(
    "Light, pitch-black for OLED screens, or follow your phone. Changes apply instantly.",
    "Choose how Trajectory gets its colors. You can change this later in Settings.",
    "Pick a folder for your notes. Everything else on this page is optional.",
)

/** The first-run wizard. The app shows it until the last step is finished. */
@Composable
fun OnboardingScreen() {
    val settingsViewModel = rememberSettingsViewModel()
    val storageViewModel = containerViewModel { StorageViewModel(it) }
    val aiKeyViewModel = containerViewModel { AiKeyViewModel(it) }
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val storage by storageViewModel.state.collectAsStateWithLifecycle()

    var step by rememberSaveable { mutableStateOf(0) }
    var finishing by remember { mutableStateOf(false) }

    BackHandler(enabled = step > 0) { step -= 1 }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StepIndicator(current = step, total = STEP_COUNT)
            Text(
                text = "Step ${step + 1} of $STEP_COUNT",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(text = StepTitles[step], style = MaterialTheme.typography.headlineMedium)
            Text(
                text = StepSubtitles[step],
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AnimatedContent(
            targetState = step,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                val forward = targetState > initialState
                val enter = slideInHorizontally { full -> if (forward) full else -full } + fadeIn()
                val exit = slideOutHorizontally { full -> if (forward) -full else full } + fadeOut()
                enter togetherWith exit
            },
            label = "wizardStep",
        ) { page ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (page) {
                    0 -> ThemeModeChoices(
                        selected = settings.themeMode,
                        onSelect = { settingsViewModel.setThemeMode(it) },
                    )

                    1 -> {
                        ColorEngineChoices(
                            selected = settings.colorEngine,
                            onSelect = { settingsViewModel.setColorEngine(it) },
                        )
                        if (settings.colorEngine != ColorEngine.MONET_WALLPAPER) {
                            SectionTitle("Accent color")
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                AccentColorPicker(
                                    selected = Color(settings.accentColor),
                                    onSelected = { settingsViewModel.setAccentColor(it.toArgb()) },
                                )
                            }
                        }
                    }

                    else -> {
                        StorageLocationSection(storageViewModel)

                        SectionTitle("Optional: Ask AI")
                        HintText("You can skip this and add a key later in Settings.")
                        AskAiToggle(settings, settingsViewModel)
                        AiKeySection(aiKeyViewModel)

                        SectionTitle("Optional modules")
                        HintText("All off by default.")
                        MomentumToggle(settings, settingsViewModel)
                        StorageModuleToggles(settings, settingsViewModel)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (step > 0) {
                    PillButton(text = "Back", onClick = { step -= 1 }, tonal = true)
                } else {
                    Spacer(modifier = Modifier)
                }
                if (step < LAST_STEP) {
                    PillButton(text = "Next", onClick = { step += 1 })
                } else {
                    PillButton(
                        text = "Finish",
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
}
