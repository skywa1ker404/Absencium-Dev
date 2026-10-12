package com.trajectory.vault.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import com.trajectory.vault.ui.components.ExpressiveChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.trajectory.vault.data.ai.AiProvider
import com.trajectory.vault.data.ai.ApiStyle
import com.trajectory.vault.data.settings.AppSettings
import com.trajectory.vault.data.storage.NotesStorageMode
import com.trajectory.vault.ui.AiKeyViewModel
import com.trajectory.vault.ui.StorageViewModel
import com.trajectory.vault.ui.components.AppCard
import com.trajectory.vault.ui.components.HintText
import com.trajectory.vault.ui.components.MessageCard
import com.trajectory.vault.ui.components.OptionCard
import com.trajectory.vault.ui.components.PillButton
import com.trajectory.vault.ui.components.SectionTitle
import com.trajectory.vault.ui.components.SwitchCard

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
        subtitle = "Private to Trajectory, no permission needed. It is removed if you uninstall the app.",
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
        OutlinedTextField(
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

/** The master switch is only available once a provider, URL, model and (if required) key are saved. */
@Composable
fun AskAiToggle(settings: AppSettings, viewModel: SettingsViewModel, ready: Boolean) {
    SwitchCard(
        title = "Ask AI",
        subtitle = if (ready || settings.askAiEnabled) {
            "Master switch for AI help. The Ask AI feature itself arrives in a later update."
        } else {
            "Set up a provider, URL, model and key below first."
        },
        checked = settings.askAiEnabled,
        onCheckedChange = { viewModel.setAskAiEnabled(it) },
        enabled = ready || settings.askAiEnabled,
    )
}

/** True when the saved connection is complete: URL and model saved, and a key saved if the provider needs one. */
fun aiReady(settings: AppSettings, hasKey: Boolean): Boolean =
    settings.aiConnectionSaved && (!settings.aiProvider.keyRequired || hasKey)

/**
 * Which AI service Ask AI talks to: the provider, its API URL and the model name. The URL
 * starts from the provider's default and can be edited. The key lives in the vault card below.
 */
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
        OutlinedTextField(
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
        OutlinedTextField(
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                ApiStyle.entries.forEach { option ->
                    ExpressiveChip(
                        selected = style == option,
                        onClick = {
                            style = option
                            saved = false
                        },
                        label = { Text(option.label) },
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
