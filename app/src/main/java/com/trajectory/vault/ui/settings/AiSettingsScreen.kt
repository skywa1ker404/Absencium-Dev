package com.trajectory.vault.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.trajectory.vault.ui.AiKeyViewModel
import com.trajectory.vault.ui.components.HintText
import com.trajectory.vault.ui.components.ScreenScaffold
import com.trajectory.vault.ui.components.SectionTitle
import com.trajectory.vault.ui.containerViewModel

@Destination<RootGraph>
@Composable
fun AiSettingsScreen(navigator: DestinationsNavigator) {
    val settingsViewModel = rememberSettingsViewModel()
    val aiKeyViewModel = containerViewModel { AiKeyViewModel(it) }
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val keyState by aiKeyViewModel.state.collectAsStateWithLifecycle()

    ScreenScaffold(title = "AI & Integrations", onBack = { navigator.navigateUp() }) {
        SectionTitle("Ask AI")
        AskAiToggle(settings, settingsViewModel, ready = aiReady(settings, keyState.hasKey))
        AiConnectionSection(settings, settingsViewModel)
        SectionTitle("API key")
        AiKeySection(aiKeyViewModel)
        HintText("Absencium works fully offline. AI is only used when you turn it on and ask.")

        SectionTitle("Integrations")
        MomentumToggle(settings, settingsViewModel)
    }
}
