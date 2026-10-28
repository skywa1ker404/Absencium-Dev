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
import skywa1ker404.absencium.ui.AiKeyViewModel
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

/** Encrypted storage for the AI API key. The key is optional and can be added or removed at any time. */
@Composable
fun AiKeySection(viewModel: AiKeyViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Not rememberSaveable: the key must not be written into saved instance state.
    var input by remember { mutableStateOf("") }

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text("API key vault", style = MaterialTheme.typography.titleMedium)
        Text(
            text = if (state.hasKey) {
                "A key ending in ${state.lastFour ?: "????"} is saved, encrypted with a key held in the Android Keystore."
            } else {
                "No key saved. Your key is encrypted on this device and never written to the debug log."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AppTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text(if (state.hasKey) "Replace key" else "API key") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton(
                text = "Save key",
                onClick = {
                    viewModel.save(input)
                    input = ""
                },
                enabled = input.isNotBlank(),
            )
            if (state.hasKey) {
                PillButton(text = "Remove", onClick = { viewModel.clear() }, tonal = true)
            }
        }
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

/**
 * Which AI service Ask AI talks to: the provider, its API URL and the model name. The URL
 * starts from the provider's default and can be edited. The key lives in the vault card below.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiConnectionSection(settings: AppSettings, viewModel: SettingsViewModel) {
    var provider by remember(settings.aiProvider) { mutableStateOf(settings.aiProvider) }
    var baseUrl by remember(settings.aiBaseUrl, settings.aiProvider) {
        mutableStateOf(settings.aiBaseUrl.ifBlank { settings.aiProvider.defaultBaseUrl })
    }
    var model by remember(settings.aiModel) { mutableStateOf(settings.aiModel) }
    var style by remember(settings.aiApiStyle) { mutableStateOf(settings.aiApiStyle) }
    var error by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf(false) }
    val modelsViewModel = containerViewModel { AiModelsViewModel(it) }
    val modelsState by modelsViewModel.state.collectAsStateWithLifecycle()

    SectionTitle("AI provider")
    AiProvider.entries.forEach { option ->
        OptionCard(
            title = option.label,
            subtitle = option.defaultBaseUrl.ifBlank { "Any server that speaks one of the supported API styles" },
            selected = provider == option,
            onClick = {
                provider = option
                baseUrl = option.defaultBaseUrl
                error = null
                saved = false
                modelsViewModel.reset()
            },
        )
    }

    SectionTitle("Connection")
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text("API URL", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "The default for ${provider.label} is filled in. Change it if your provider or server uses a different address.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AppTextField(
            value = baseUrl,
            onValueChange = {
                baseUrl = it
                saved = false
            },
            label = { Text("API URL") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        AppTextField(
            value = model,
            onValueChange = {
                model = it
                saved = false
            },
            label = { Text("Model name") },
            supportingText = { Text("Exactly as your provider lists it, for example in its model list or dashboard.") },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        ModelPickerCard(
            provider = provider,
            state = modelsState,
            selectedId = model.trim(),
            onFetch = {
                val chosenStyle = if (provider == AiProvider.CUSTOM) style else provider.style
                modelsViewModel.load(AiConnection(provider, chosenStyle, baseUrl.trim().trimEnd('/'), model.trim()))
            },
            onPick = {
                model = it
                saved = false
            },
        )
        if (provider == AiProvider.CUSTOM) {
            Text(
                text = "API style",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 12.dp),
            )
            // Wraps onto a second line instead of squeezing the last chip into a column of single letters.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                ApiStyle.entries.forEach { option ->
                    ExpressiveChip(
                        selected = style == option,
                        onClick = {
                            style = option
                            saved = false
                        },
                        label = { Text(option.label, maxLines = 1, softWrap = false) },
                        shape = CircleShape,
                    )
                }
            }
        }
        if (baseUrl.trim().startsWith("http://")) {
            Text(
                text = "Plain http is only meant for a server on your own network.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        val currentError = error
        if (currentError != null) {
            Text(
                text = currentError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        PillButton(
            text = if (saved) "Saved" else "Save connection",
            onClick = {
                val cleanUrl = baseUrl.trim().trimEnd('/')
                val cleanModel = model.trim()
                when {
                    !(cleanUrl.startsWith("https://") || cleanUrl.startsWith("http://")) ->
                        error = "The API URL must start with https:// or http://"

                    cleanUrl.substringAfter("://").isBlank() -> error = "Enter the full API URL."
                    cleanModel.isEmpty() -> error = "Enter the model name."
                    else -> {
                        error = null
                        val chosenStyle = if (provider == AiProvider.CUSTOM) style else provider.style
                        viewModel.setAiConnection(provider, cleanUrl, cleanModel, chosenStyle)
                        saved = true
                    }
                }
            },
            enabled = !saved,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

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
