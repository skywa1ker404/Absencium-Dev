package skywa1ker404.absencium.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import android.content.Intent
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import skywa1ker404.absencium.logging.DebugLogger
import skywa1ker404.absencium.ui.StorageViewModel
import skywa1ker404.absencium.data.backup.SnapshotInfo
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.AppTextField
import skywa1ker404.absencium.ui.components.SwitchCard
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.MessageCard
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.components.SectionTitle
import skywa1ker404.absencium.ui.containerViewModel
import java.time.LocalDate
import kotlin.system.exitProcess

private val RestoreMimeTypes = arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream", "*/*")

private val dateTime = java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")

private fun whenText(epochMs: Long): String =
    java.time.Instant.ofEpochMilli(epochMs).atZone(java.time.ZoneId.systemDefault()).format(dateTime)

@Destination<RootGraph>
@Composable
fun StorageSettingsScreen(navigator: DestinationsNavigator) {
    val settingsViewModel = rememberSettingsViewModel()
    val storageViewModel = containerViewModel { StorageViewModel(it) }
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val storage by storageViewModel.state.collectAsStateWithLifecycle()
    var confirmRestore by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var includeFiles by remember { mutableStateOf(true) }
    var restorePassword by remember { mutableStateOf("") }
    var pendingSnapshot by remember { mutableStateOf<SnapshotInfo?>(null) }

    val encrypted = password.isNotEmpty()
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        if (uri != null) storageViewModel.backupTo(uri, password, includeFiles)
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) storageViewModel.restoreFrom(uri)
    }

    ScreenScaffold(title = "Storage & Data", onBack = { navigator.navigateUp() }) {
        StorageLocationSection(storageViewModel)

        SectionTitle("Backup and restore")
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Text("Back up everything", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "One file with your database (subjects, attendance, exams, notes, timetable), your settings and, if you " +
                    "like, the photos, documents and recordings of your entries. Note files in your notes folder are not inside it. " +
                    "The AI key is never included.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (settings.lastBackupAt > 0L) "Last backup: ${whenText(settings.lastBackupAt)} (${settings.lastBackupNote})" else "No backup made yet.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text("Include photos, documents and recordings", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                androidx.compose.material3.Switch(checked = includeFiles, onCheckedChange = { includeFiles = it })
            }
            AppTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password (optional)") },
                supportingText = {
                    Text(
                        if (encrypted) "The backup is sealed with AES-256. Without this password it cannot be opened, and it cannot be recovered."
                        else "Leave empty for a plain zip anyone with the file can open.",
                    )
                },
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                PillButton(
                    text = "Back up",
                    onClick = { backupLauncher.launch("Absencium-backup-${LocalDate.now()}.${if (encrypted) "absbak" else "zip"}") },
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
        val preview = storage.preview
        if (storage.restoreStaged) {
            AppCard(modifier = Modifier.fillMaxWidth(), highlighted = true) {
                Text("Restore ready", style = MaterialTheme.typography.titleMedium)
                if (preview != null) {
                    val counts = preview.counts.entries.filter { it.value > 0 }.joinToString(", ") { "${it.value} ${it.key}" }
                    Text(
                        text = buildString {
                            if (preview.createdAt > 0L) append("Made ${whenText(preview.createdAt)}. ")
                            if (counts.isNotEmpty()) append("Contains $counts. ")
                            append(if (preview.includesAttachments) "Includes ${preview.attachmentFiles} files." else "Database and settings only; your current photos and files are kept.")
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    text = "The backup is applied when Absencium starts again. Your current data is kept as a safety copy.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
                val context = LocalContext.current
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                    PillButton(
                        text = "Restart app",
                        onClick = {
                            DebugLogger.i("Backup", "Restarting the app so the staged restore can be applied")
                            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
                            if (launch != null) {
                                context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                            }
                            exitProcess(0)
                        },
                    )
                    PillButton(text = "Discard", tonal = true, onClick = { storageViewModel.discardStagedRestore() })
                }
            }
        }

        SectionTitle("Local snapshots")
        SwitchCard(
            title = "Daily snapshot",
            subtitle = "Keeps the 3 newest copies of your database and settings on this phone, at most one a day. Photos are not copied.",
            checked = settings.autoSnapshots,
            onCheckedChange = { settingsViewModel.setAutoSnapshots(it) },
        )
        PillButton(text = "Save a snapshot now", tonal = true, onClick = { storageViewModel.createSnapshotNow() }, enabled = !storage.busy)
        storage.snapshots.forEach { snap ->
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Text(whenText(snap.createdAt), style = MaterialTheme.typography.titleSmall)
                Text("${snap.sizeBytes / 1024} KB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    PillButton(text = "Restore", compact = true, tonal = true, enabled = !storage.busy, onClick = { pendingSnapshot = snap })
                    PillButton(text = "Delete", compact = true, tonal = true, onClick = { storageViewModel.deleteSnapshot(snap) })
                }
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
                    "The backup is checked first (every file against its checksum) and nothing changes until you restart. " +
                        "Restoring then replaces your database, and your photos if the backup has them. " +
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

    if (storage.needsPassword) {
        AlertDialog(
            onDismissRequest = { restorePassword = ""; storageViewModel.cancelPassword() },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Password for this backup") },
            text = {
                Column {
                    if (storage.passwordWasWrong) {
                        Text("That did not open it: the password is wrong, or the file is damaged.", color = MaterialTheme.colorScheme.error)
                    }
                    AppTextField(
                        value = restorePassword,
                        onValueChange = { restorePassword = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = restorePassword.isNotEmpty(),
                    onClick = {
                        val typed = restorePassword
                        restorePassword = ""
                        storageViewModel.submitPassword(typed)
                    },
                ) { Text("Open") }
            },
            dismissButton = {
                TextButton(onClick = { restorePassword = ""; storageViewModel.cancelPassword() }) { Text("Cancel") }
            },
        )
    }

    pendingSnapshot?.let { snap ->
        AlertDialog(
            onDismissRequest = { pendingSnapshot = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Restore this snapshot?") },
            text = { Text("Your database goes back to how it was on ${whenText(snap.createdAt)}. Nothing changes until you restart.") },
            confirmButton = {
                TextButton(onClick = { pendingSnapshot = null; storageViewModel.restoreSnapshot(snap) }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { pendingSnapshot = null }) { Text("Cancel") } },
        )
    }
}
