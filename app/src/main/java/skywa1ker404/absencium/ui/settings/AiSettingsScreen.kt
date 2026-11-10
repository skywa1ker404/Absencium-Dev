package skywa1ker404.absencium.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.CardStack
import skywa1ker404.absencium.ui.components.GroupPosition
import skywa1ker404.absencium.ui.components.NavCard
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.components.SectionTitle
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.setValue
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
    var advanced by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }

    ScreenScaffold(title = "Absencium AI", onBack = { navigator.navigateUp() }, largeTitle = true) {
        AiHero(enabled = settings.askAiEnabled, onOpen = { AbsenciumAi.open(context) })
        AskAiToggle(settings, settingsViewModel, ready = true)

        SectionTitle("Set up")
        CardStack {
            NavCard(
                icon = AppIcons.Tune, title = "Models", subtitle = "Choose which model answers",
                onClick = { AbsenciumAi.open(context, destination = "models") }, position = GroupPosition.First,
            )
            NavCard(
                icon = AppIcons.Storage, title = "Providers and keys", subtitle = "Your key stays on this phone",
                onClick = { AbsenciumAi.open(context, destination = "providers") }, position = GroupPosition.Middle,
            )
            NavCard(
                icon = AppIcons.Person, title = "Assistants", subtitle = "Study coach and your own",
                onClick = { AbsenciumAi.open(context, destination = "assistants") }, position = GroupPosition.Middle,
            )
            NavCard(
                icon = AppIcons.Search, title = "Web search", subtitle = null,
                onClick = { AbsenciumAi.open(context, destination = "search") }, position = GroupPosition.Last,
            )
        }

        AdvancedHeader(expanded = advanced, onClick = { advanced = !advanced })
        androidx.compose.animation.AnimatedVisibility(
            visible = advanced,
            enter = androidx.compose.animation.expandVertically(androidx.compose.animation.core.spring(0.8f, 380f)) + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.shrinkVertically(androidx.compose.animation.core.spring(1f, 500f)) + androidx.compose.animation.fadeOut(),
        ) {
            androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CardStack {
                    NavCard(
                        icon = AppIcons.Code, title = "Plugins", subtitle = "Connect tool servers",
                        onClick = { AbsenciumAi.open(context, destination = "mcp") }, position = GroupPosition.First,
                    )
                    NavCard(
                        icon = AppIcons.Book, title = "Skills", subtitle = null,
                        onClick = { AbsenciumAi.open(context, destination = "skills") }, position = GroupPosition.Middle,
                    )
                    NavCard(
                        icon = AppIcons.Mic, title = "Voice", subtitle = "Read aloud and dictation",
                        onClick = { AbsenciumAi.open(context, destination = "tts") }, position = GroupPosition.Middle,
                    )
                    NavCard(
                        icon = AppIcons.OpenInNew, title = "Android integration", subtitle = null,
                        onClick = { AbsenciumAi.open(context, destination = "android") }, position = GroupPosition.Middle,
                    )
                    NavCard(
                        icon = AppIcons.Folder, title = "Chat storage and backup", subtitle = null,
                        onClick = { AbsenciumAi.open(context, destination = "storage") }, position = GroupPosition.Last,
                    )
                }

                SectionTitle("Pictures")
                SettingGroup {
                    SettingSwitchRow(
                        title = "Search images",
                        subtitle = "Wikimedia and Openverse. Only search words leave the phone.",
                        checked = settings.searchImagesEnabled,
                        onCheckedChange = settingsViewModel::setSearchImagesEnabled,
                    )
                    SettingDivider()
                    SettingSwitchRow(
                        title = "Generate images",
                        subtitle = "Adds Imagine to the chat. Requires a Gemini or OpenAI plan.",
                        checked = settings.generateImagesEnabled,
                        onCheckedChange = settingsViewModel::setGenerateImagesEnabled,
                    )
                }

                SectionTitle("Study")
                SettingGroup {
                    SettingSwitchRow(
                        title = "Momentum tracker",
                        subtitle = "Daily study streak. $OPTIONAL_NOTE",
                        checked = settings.momentumTrackerEnabled,
                        onCheckedChange = settingsViewModel::setMomentumTrackerEnabled,
                    )
                }
            }
        }
    }
}

/** The top of the page: the Absencium AI mark, whether it is on, and one button into the chat. */
@Composable
private fun AiHero(enabled: Boolean, onOpen: () -> Unit) {
    val colors = androidx.compose.material3.MaterialTheme.colorScheme
    AppCard(modifier = Modifier.fillMaxWidth(), container = colors.primaryContainer.copy(alpha = 0.55f)) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(22.dp)).background(colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Icon(AppIcons.AutoAwesome, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(32.dp))
            }
            androidx.compose.material3.Text(
                "Absencium AI",
                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 6.dp),
            )
            androidx.compose.material3.Text(
                if (enabled) "Ready when you are" else "Switched off",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            PillButton(text = "Open chat", onClick = onOpen, enabled = enabled, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

/** A quiet header that opens the less common settings. The arrow turns with a spring. */
@Composable
private fun AdvancedHeader(expanded: Boolean, onClick: () -> Unit) {
    val colors = androidx.compose.material3.MaterialTheme.colorScheme
    val turn by androidx.compose.animation.core.animateFloatAsState(
        if (expanded) 180f else 0f, androidx.compose.animation.core.spring(0.7f, 400f), label = "advanced",
    )
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Text(
                "Advanced",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            androidx.compose.material3.Icon(
                AppIcons.ExpandMore, contentDescription = null, tint = colors.onSurfaceVariant,
                modifier = Modifier.rotate(turn),
            )
        }
    }
}

