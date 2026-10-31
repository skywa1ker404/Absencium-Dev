package skywa1ker404.absencium.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import skywa1ker404.absencium.ui.components.ExpressiveChip
import androidx.compose.material3.MaterialTheme
import skywa1ker404.absencium.ui.components.AppTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import skywa1ker404.absencium.data.ai.AiConnection
import skywa1ker404.absencium.data.ai.AiProvider
import skywa1ker404.absencium.data.ai.ApiStyle
import skywa1ker404.absencium.data.settings.AppSettings
import skywa1ker404.absencium.data.storage.NotesStorageMode
import skywa1ker404.absencium.ui.containerViewModel
import skywa1ker404.absencium.ui.StorageViewModel
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.MessageCard
import skywa1ker404.absencium.ui.components.OptionCard
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.components.SectionTitle
import skywa1ker404.absencium.ui.components.SwitchCard

private const val OPTIONAL_NOTE =
    "Optional and off by default. The feature itself arrives in a later update; for now this records your choice."

/** Where notes are stored: the app folder or a folder picked with the system folder picker. */
@Composable
fun StorageLocationSection(viewModel: StorageViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) viewModel.useCustomFolder(uri)
    }

    SectionTitle("Notes location")
    OptionCard(
        title = "App folder",
        subtitle = "Private to Absencium, no permission needed. It is removed if you uninstall the app.",
        selected = state.mode == NotesStorageMode.APP_FILES,
        onClick = { viewModel.useAppFiles() },
    )
    OptionCard(
        title = "Choose a folder",
        subtitle = "Pick any folder, for example in Documents. Your notes stay if you uninstall the app.",
        selected = state.mode == NotesStorageMode.CUSTOM_FOLDER,
        onClick = { folderPicker.launch(null) },
    )
    val path = state.path
    if (path != null) {
        HintText("Current location: $path")
    } else if (state.mode == null) {
        HintText("Choose one of the two options above.")
    }
    val message = state.message
    if (message != null) MessageCard(message)
}

/**
 * The master switch for everything AI. Turning it on first shows a plain consent popup saying exactly what leaves the phone.
 * Tapping it while it is off always opens that popup, even before a provider is set up.
 */
@Composable
fun AskAiToggle(settings: AppSettings, viewModel: SettingsViewModel, ready: Boolean) {
    var asking by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    SwitchCard(
        title = "AI Features",
        subtitle = when {
            !settings.askAiEnabled -> "Off. Nothing is ever sent anywhere. Only reading text on this phone works."
            ready -> "On. AI help, text from files, notes and homework use your own provider."
            else -> "On, but no provider is saved yet. Set up a provider, URL, model and key below."
        },
        checked = settings.askAiEnabled,
        onCheckedChange = { turnOn -> if (turnOn) asking = true else viewModel.setAskAiEnabled(false) },
    )
    if (asking) AiConsentDialog(onAgree = { asking = false; viewModel.setAskAiEnabled(true) }, onDecline = { asking = false })
}

/** What turning AI Features on means, said plainly. The same popup is used by onboarding. */
@Composable
fun AiConsentDialog(onAgree: () -> Unit, onDecline: () -> Unit) {
    skywa1ker404.absencium.ui.components.AlertDialog(
        onDismissRequest = onDecline,
        title = { Text("Turn on AI Features?") },
        text = {
            Text(
                "Absencium has no server and collects nothing. With AI Features on, only when you press an AI button, the text, " +
                    "photos, PDFs or recordings you chose for that action, together with what you typed, are sent from this phone " +
                    "straight to the AI provider you set up, using your own API key. That provider processes them under its own terms and " +
                    "may keep them; Absencium cannot see or control that.\n\n" +
                    "Your key stays in this phone's encrypted storage. Nothing is sent in the background. If you decline, or turn this off " +
                    "later, no AI button sends anything, and reading text from pictures and files still works on this phone.",
            )
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = onAgree) { Text("Agree") } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDecline) { Text("Decline") } },
    )
}

/** True when the saved connection is complete: URL and model saved, and a key saved if the provider needs one. */
fun aiReady(settings: AppSettings, hasKey: Boolean): Boolean =
    settings.aiConnectionSaved && (!settings.aiProvider.keyRequired || hasKey)

/** True when AI may actually be used: the AI Features switch is on and the connection is complete. */
fun aiUsable(settings: AppSettings, hasKey: Boolean): Boolean = settings.askAiEnabled && aiReady(settings, hasKey)

@Composable
fun MomentumToggle(settings: AppSettings, viewModel: SettingsViewModel) {
    SwitchCard(
        title = "Momentum tracker",
        subtitle = "Daily streak and study momentum. $OPTIONAL_NOTE",
        checked = settings.momentumTrackerEnabled,
        onCheckedChange = { viewModel.setMomentumTrackerEnabled(it) },
    )
}

@Composable
fun StorageModuleToggles(settings: AppSettings, viewModel: SettingsViewModel) {
    SwitchCard(
        title = "Semester transition and archive manager",
        subtitle = "Archive a finished semester in one step. $OPTIONAL_NOTE",
        checked = settings.semesterManagerEnabled,
        onCheckedChange = { viewModel.setSemesterManagerEnabled(it) },
    )
    SwitchCard(
        title = "Low storage media compressor",
        subtitle = "Shrink photos and audio when space runs low. $OPTIONAL_NOTE",
        checked = settings.lowStorageCompressorEnabled,
        onCheckedChange = { viewModel.setLowStorageCompressorEnabled(it) },
    )
}
