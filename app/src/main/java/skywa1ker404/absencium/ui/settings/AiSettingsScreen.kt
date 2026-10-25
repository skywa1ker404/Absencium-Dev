package skywa1ker404.absencium.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import skywa1ker404.absencium.ui.AiKeyViewModel
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.components.SwitchCard
import skywa1ker404.absencium.ui.components.SectionTitle
import skywa1ker404.absencium.ui.containerViewModel

@Destination<RootGraph>
@Composable
fun AiSettingsScreen(navigator: DestinationsNavigator) {
    val settingsViewModel = rememberSettingsViewModel()
    val aiKeyViewModel = containerViewModel { AiKeyViewModel(it) }
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val keyState by aiKeyViewModel.state.collectAsStateWithLifecycle()

    ScreenScaffold(title = "AI & Integrations", onBack = { navigator.navigateUp() }) {
        SectionTitle("AI Features")
        AskAiToggle(settings, settingsViewModel, ready = aiReady(settings, keyState.hasKey))
        AiConnectionSection(settings, settingsViewModel)
        SectionTitle("Images in generated notes")
        SwitchCard(
            title = "Search Images (experimental)",
            subtitle = "Lets Generate Notes look for free pictures online (Wikimedia Commons, Openverse). Only search words leave the phone. Results can be unrelated, so check each one.",
            checked = settings.searchImagesEnabled,
            onCheckedChange = settingsViewModel::setSearchImagesEnabled,
        )
        SwitchCard(
            title = "Generate Images",
            subtitle = "Lets Generate Notes ask your AI to draw pictures. Needs a paid Google Gemini or OpenAI plan; free plans have no image quota.",
            checked = settings.generateImagesEnabled,
            onCheckedChange = settingsViewModel::setGenerateImagesEnabled,
        )
        SectionTitle("API key")
        AiKeySection(aiKeyViewModel)
        HintText("Absencium works fully offline. AI is only used when you turn it on and ask.")

        SectionTitle("Integrations")
        MomentumToggle(settings, settingsViewModel)
    }
}
