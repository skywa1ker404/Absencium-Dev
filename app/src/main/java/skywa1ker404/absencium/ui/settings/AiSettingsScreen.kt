package skywa1ker404.absencium.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
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
    val primary = androidx.compose.material3.MaterialTheme.colorScheme.primary
    var advanced by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }

    ScreenScaffold(title = "Absencium AI", onBack = { navigator.navigateUp() }, largeTitle = true) {
        AskAiToggle(settings, settingsViewModel, ready = true)

        // What AI needs to work: the chat, the model, where it comes from, and who answers.
        SettingGroup {
            SettingRow(
                title = "Open Absencium AI",
                subtitle = "Chat, attach files, ask it to look things up or change your notes, timetable and more",
                onClick = { AbsenciumAi.open(context) },
                leading = { androidx.compose.material3.Icon(AppIcons.AutoAwesome, contentDescription = null, tint = primary) },
            )
            SettingDivider()
            SettingRow(
                title = "Default models",
                subtitle = "The model that answers, writes titles and reads text",
                onClick = { AbsenciumAi.open(context, destination = "models") },
                leading = { androidx.compose.material3.Icon(AppIcons.AutoAwesome, contentDescription = null, tint = primary) },
            )
            SettingDivider()
            SettingRow(
                title = "Providers and keys",
                subtitle = "Gemini, OpenAI, OpenRouter, Claude and others; your key stays on this phone",
                onClick = { AbsenciumAi.open(context, destination = "providers") },
                leading = { androidx.compose.material3.Icon(AppIcons.Edit, contentDescription = null, tint = primary) },
            )
            SettingDivider()
            SettingRow(
                title = "Assistants",
                subtitle = "Study, exam coach, writing and your own",
                onClick = { AbsenciumAi.open(context, destination = "assistants") },
                leading = { androidx.compose.material3.Icon(AppIcons.Person, contentDescription = null, tint = primary) },
            )
            SettingDivider()
            SettingRow(
                title = "Web search",
                subtitle = "Which search the AI uses when you ask it to look something up",
                onClick = { AbsenciumAi.open(context, destination = "search") },
                leading = { androidx.compose.material3.Icon(AppIcons.Search, contentDescription = null, tint = primary) },
            )
        }

        // Everything else, closed by default.
        SettingGroup {
            SettingRow(
                title = "Advanced",
                subtitle = "Plugins, skills, voice, storage, pictures and approvals",
                onClick = { advanced = !advanced },
            )
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = advanced,
            enter = androidx.compose.animation.expandVertically(androidx.compose.animation.core.spring(0.8f, 380f)) + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.shrinkVertically(androidx.compose.animation.core.spring(1f, 500f)) + androidx.compose.animation.fadeOut(),
        ) {
            androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingGroup {
                    SettingRow(title = "Plugins (MCP)", subtitle = "Tool servers the AI can call", onClick = { AbsenciumAi.open(context, destination = "mcp") })
                    SettingDivider()
                    SettingRow(title = "Skills", subtitle = "Saved know-how the AI can load when needed", onClick = { AbsenciumAi.open(context, destination = "skills") })
                    SettingDivider()
                    SettingRow(title = "Voice", subtitle = "Read answers aloud and dictate with your voice", onClick = { AbsenciumAi.open(context, destination = "tts") })
                    SettingDivider()
                    SettingRow(title = "Android integration", subtitle = "Assistant overlay and quick ways to ask", onClick = { AbsenciumAi.open(context, destination = "android") })
                    SettingDivider()
                    SettingRow(title = "Chat storage and backup", subtitle = "See what chats take up, clear them, back them up", onClick = { AbsenciumAi.open(context, destination = "storage") })
                }

                AutoApproveSection(settings.aiAutoApprove, settingsViewModel::setAiAutoApprove)

                SettingCaption("Pictures")
                SettingGroup {
                    SettingSwitchRow(
                        title = "Search images (experimental)",
                        subtitle = "Looks for free pictures on Wikimedia Commons and Openverse. Only search words leave the phone; check each result.",
                        checked = settings.searchImagesEnabled,
                        onCheckedChange = settingsViewModel::setSearchImagesEnabled,
                    )
                    SettingDivider()
                    SettingSwitchRow(
                        title = "Generate images (Imagine)",
                        subtitle = "Shows Imagine in the chat and lets your AI draw pictures. Off by default; needs a paid Google Gemini or OpenAI plan.",
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
            }
        }
        HintText("Absencium works fully offline. AI is only used when you turn it on and ask.")
    }
}

/** The kinds of change Absencium AI can make without asking. Closed by default; deleting always asks. */
@Composable
private fun AutoApproveSection(auto: Set<String>, onChange: (Set<String>) -> Unit) {
    var open by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    SettingGroup {
        SettingRow(
            title = "Do without asking",
            subtitle = if (auto.isEmpty()) "Everything it changes waits for your approval" else "${auto.size} kinds of change run on their own",
            onClick = { open = !open },
        )
        androidx.compose.animation.AnimatedVisibility(
            visible = open,
            enter = androidx.compose.animation.expandVertically(androidx.compose.animation.core.spring(0.8f, 380f)) + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.shrinkVertically(androidx.compose.animation.core.spring(1f, 500f)) + androidx.compose.animation.fadeOut(),
        ) {
            androidx.compose.foundation.layout.Column {
                AUTO_KINDS.forEach { (key, label) ->
                    SettingDivider()
                    SettingSwitchRow(
                        title = label,
                        subtitle = "Adding and changing. Deleting still asks.",
                        checked = key in auto,
                        onCheckedChange = { on -> onChange(if (on) auto + key else auto - key) },
                    )
                }
            }
        }
    }
    HintText("A plan with several changes is shown once and approved once. Anything you switch on here skips even that.")
}

private val AUTO_KINDS = listOf(
    "subjects" to "Subjects",
    "notes" to "Notes",
    "timetable" to "Timetable",
    "exams" to "Exams",
    "holidays" to "Holidays",
    "attendance" to "Attendance",
    "syllabus" to "Syllabus",
)
