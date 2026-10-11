package com.trajectory.vault.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.trajectory.vault.logging.DebugLogger
import com.trajectory.vault.ui.StorageViewModel
import com.trajectory.vault.ui.components.GlassCard
import com.trajectory.vault.ui.components.HintText
import com.trajectory.vault.ui.components.MessageCard
import com.trajectory.vault.ui.components.PillButton
import com.trajectory.vault.ui.components.ScreenScaffold
import com.trajectory.vault.ui.components.SectionTitle
import com.trajectory.vault.ui.containerViewModel
import java.time.LocalDate
import kotlin.system.exitProcess

private val RestoreMimeTypes = arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")

@Destination<RootGraph>
@Composable
fun StorageSettingsScreen(navigator: DestinationsNavigator) {
    val settingsViewModel = rememberSettingsViewModel()
    val storageViewModel = containerViewModel { StorageViewModel(it) }
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val storage by storageViewModel.state.collectAsStateWithLifecycle()
    var confirmRestore by remember { mutableStateOf(false) }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        if (uri != null) storageViewModel.backupTo(uri)
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) storageViewModel.restoreFrom(uri)
    }

    ScreenScaffold(title = "Storage & Data", onBack = { navigator.navigateUp() }) {
        StorageLocationSection(storageViewModel)

        SectionTitle("Backup and restore")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("Database backup", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Saves everything in the Trajectory database (subjects, attendance, exams, notes index and more) " +
                    "as one .zip file you can keep anywhere. Note files and appearance settings are not inside it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                PillButton(
                    text = "Back up",
                    onClick = { backupLauncher.launch("Trajectory-backup-${LocalDate.now()}.zip") },
                    loading = storage.busy,
                )
                PillButton(
                    text = "Restore",
                    onClick = { confirmRestore = true },
                    tonal = true,
                    enabled = !storage.busy,
                )
            }
        }
        val message = storage.message
        if (message != null) MessageCard(message)
        if (storage.restoreStaged) {
            GlassCard(modifier = Modifier.fillMaxWidth(), highlighted = true) {
                Text("Restore ready", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "The backup is applied the next time Trajectory starts. Close the app now, then open it again. " +
                        "Your current data is kept as a safety copy.",
                    style = MaterialTheme.typography.bodySmall,
                )
                PillButton(
                    text = "Close app",
                    onClick = {
                        DebugLogger.i("Backup", "Closing the app so the staged restore can be applied")
                        exitProcess(0)
                    },
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        SectionTitle("Optional tools")
        HintText("Both are off by default.")
        StorageModuleToggles(settings, settingsViewModel)
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Restore from a backup?") },
            text = {
                Text(
                    "Restoring replaces everything in the database with the contents of the backup. " +
                        "Your current data is kept as a safety copy, but only one copy is kept.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRestore = false
                        restoreLauncher.launch(RestoreMimeTypes)
                    },
                ) { Text("Choose backup file") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestore = false }) { Text("Cancel") }
            },
        )
    }
}
