package com.trajectory.vault.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.trajectory.vault.data.settings.AppSettings
import com.trajectory.vault.data.storage.NotesStorageMode
import com.trajectory.vault.ui.AiKeyViewModel
import com.trajectory.vault.ui.StorageViewModel
import com.trajectory.vault.ui.components.GlassCard
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

    GlassCard(modifier = Modifier.fillMaxWidth()) {
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

@Composable
fun AskAiToggle(settings: AppSettings, viewModel: SettingsViewModel) {
    SwitchCard(
        title = "Ask AI",
        subtitle = "Master switch for AI help. Off by default. The Ask AI feature itself arrives in a later update.",
        checked = settings.askAiEnabled,
        onCheckedChange = { viewModel.setAskAiEnabled(it) },
    )
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
