package skywa1ker404.absencium.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import skywa1ker404.absencium.ui.AbsenciumAi
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.components.SettingCaption
import skywa1ker404.absencium.ui.components.SettingDivider
import skywa1ker404.absencium.ui.components.SettingGroup
import skywa1ker404.absencium.ui.components.SettingRow
import skywa1ker404.absencium.ui.components.SettingSwitchRow
import skywa1ker404.absencium.ui.icons.AppIcons

/**
 * The gateway to Absencium AI. The chat, its providers, models, assistants and tools live in the embedded Absencium AI itself; this
 * screen keeps only the app-wide AI Features switch (and its consent), the two doors into the chat, and the settings of the app's own
 * note and momentum features that are not part of the chat.
 */
@Destination<RootGraph>
@Composable
fun AiSettingsScreen(navigator: DestinationsNavigator) {
    val settingsViewModel = rememberSettingsViewModel()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ScreenScaffold(title = "Absencium AI", onBack = { navigator.navigateUp() }) {
        AskAiToggle(settings, settingsViewModel, ready = true)

        SettingGroup {
            SettingRow(
                title = "Open Absencium AI",
                subtitle = "Chat, attach files, ask it to look things up or change your notes, timetable and more",
                onClick = { AbsenciumAi.open(context) },
                leading = { androidx.compose.material3.Icon(AppIcons.AutoAwesome, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.primary) },
            )
            SettingRow(
                title = "Providers, models and assistants",
                subtitle = "API keys, the model to use, search and everything else about the AI",
                onClick = { AbsenciumAi.open(context, settings = true) },
                leading = { androidx.compose.material3.Icon(AppIcons.Edit, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.primary) },
            )
        }

        SettingCaption("Images in generated notes")
        SettingGroup {
            SettingSwitchRow(
                title = "Search images (experimental)",
                subtitle = "Looks for free pictures on Wikimedia Commons and Openverse. Only search words leave the phone; check each result.",
                checked = settings.searchImagesEnabled,
                onCheckedChange = settingsViewModel::setSearchImagesEnabled,
            )
            SettingDivider()
            SettingSwitchRow(
                title = "Generate images",
                subtitle = "Asks your AI to draw pictures. Needs a paid Google Gemini or OpenAI plan.",
                checked = settings.generateImagesEnabled,
                onCheckedChange = settingsViewModel::setGenerateImagesEnabled,
            )
        }

        SettingCaption("Integrations")
        SettingGroup {
            SettingSwitchRow(
                title = "Momentum tracker",
                subtitle = "Daily streak and study momentum. $OPTIONAL_NOTE",
                checked = settings.momentumTrackerEnabled,
                onCheckedChange = settingsViewModel::setMomentumTrackerEnabled,
            )
        }
        HintText("Absencium works fully offline. AI is only used when you turn it on and ask.")
    }
}
