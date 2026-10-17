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
import skywa1ker404.absencium.ui.components.SectionTitle
import skywa1ker404.absencium.ui.components.SwitchCard
import skywa1ker404.absencium.ui.containerViewModel

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
        SwitchCard(
            title = "Read YouTube captions directly",
            subtitle = "Lets the app fetch a video's captions through YouTube's own app interface, which is not an official API. " +
                "YouTube's terms may not allow it, and it can stop working at any time. Off by default. " +
                "With it off, Gemini can still watch a public link; other providers need this switch.",
            checked = settings.youtubeCaptionsDirect,
            onCheckedChange = settingsViewModel::setYoutubeCaptionsDirect,
        )
    }
}
