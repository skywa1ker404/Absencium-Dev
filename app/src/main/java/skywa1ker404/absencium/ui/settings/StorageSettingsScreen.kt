package skywa1ker404.absencium.ui.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import skywa1ker404.absencium.data.backup.BackupPart
import skywa1ker404.absencium.ui.components.AppSwitch
import skywa1ker404.absencium.ui.components.ExpressiveChip
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import skywa1ker404.absencium.ui.theme.RoundedCornerShape
import skywa1ker404.absencium.ui.components.AlertDialog
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
    var chosen by remember { mutableStateOf<Set<skywa1ker404.absencium.data.backup.BackupPart>?>(null) }
    val selection = chosen ?: settings.backupParts
    var restorePassword by remember { mutableStateOf("") }

    val encrypted = password.isNotEmpty()
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        if (uri != null) storageViewModel.backupTo(uri, password, selection)
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) storageViewModel.restoreFrom(uri)
    }

    ScreenScaffold(title = "Storage & Data", onBack = { navigator.navigateUp() }, largeTitle = true) {
        StorageLocationSection(storageViewModel)

        SectionTitle("Choose what to back up")
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "Everything" to BackupPart.everything,
                    "Light, no media" to BackupPart.lightWithoutMedia,
                    "Data only" to BackupPart.dataOnly,
                    "My choice" to settings.backupParts,
                ).forEach { (label, set) ->
                    ExpressiveChip(selected = selection == set, onClick = { chosen = set }, label = { Text(label) })
                }
            }
            val estimates = storage.parts.associateBy { it.part }
            val context = androidx.compose.ui.platform.LocalContext.current
            BackupPart.entries.forEach { part ->
                val estimate = estimates[part]
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(part.title, style = MaterialTheme.typography.bodyMedium)
                        val detail = buildList {
                            if (estimate != null && part.tables.isNotEmpty()) {
                                add("${estimate.rows} " + if (estimate.rows == 1) "item" else "items")
                                if (part.hasFiles) add("${estimate.files} " + if (estimate.files == 1) "file" else "files")
                                add("about " + android.text.format.Formatter.formatShortFileSize(context, estimate.bytes))
                            }
                            if (part.needs.isNotEmpty()) add("needs " + part.needs.joinToString { it.title })
                        }.joinToString(" · ")
                        if (detail.isNotEmpty()) Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    AppSwitch(
                        checked = part in selection,
                        onCheckedChange = { on -> chosen = if (on) BackupPart.withNeeds(selection + part) else BackupPart.without(selection, part) },
                    )
                }
            }
            val total = storage.parts.filter { it.part in selection }.sumOf { it.bytes }
            Text(
                text = "${selection.size} of ${BackupPart.entries.size} parts, about " + android.text.format.Formatter.formatShortFileSize(context, total) +
                    " before compression. Switching a part on also switches on what it needs; switching one off also switches off what needs it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        SectionTitle("Backup and restore")
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Text("Back up", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "One file with the parts you chose above. Note files in your notes folder are not inside it. " +
                    "The AI key is never included.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (settings.lastBackupAt > 0L) "Last backup: ${whenText(settings.lastBackupAt)} (${settings.lastBackupNote})" else "No backup made yet.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
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
                            preview.parts?.let { held ->
                                val all = BackupPart.entries
                                append("Holds ${held.size} of ${all.size} parts. ")
                                val missing = all.filter { it !in held }
                                if (missing.isNotEmpty()) {
                                    append("Not in this file: ${missing.joinToString { it.title }}; ")
                                    append(if (preview.partRestorePossible) "they stay as they are on this phone. " else "they will be empty after this restore. ")
                                }
                            }
                            append(if (preview.includesAttachments) "Includes ${preview.attachmentFiles} files." else "Database and settings only; your current photos and files are kept.")
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                var restoreChosen by remember(preview) { mutableStateOf<Set<BackupPart>?>(null) }
                var mergeMode by remember(preview) { mutableStateOf(false) }
                val held = preview?.parts.orEmpty()
                val partMode = preview?.partRestorePossible == true && held.isNotEmpty()
                val restoreSelection = restoreChosen ?: held
                if (partMode) {
                    Text("How to restore", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExpressiveChip(selected = !mergeMode, onClick = { mergeMode = false }, label = { Text("Replace") })
                        ExpressiveChip(selected = mergeMode, onClick = { mergeMode = true }, label = { Text("Merge") })
                    }
                    Text(
                        text = if (mergeMode) {
                            "Merge adds what is missing on this phone and keeps the newer version of what is on both. Nothing is deleted, and settings are not merged."
                        } else {
                            "Replace swaps the chosen parts for the file's. Anything in them that is not in the file is lost."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("Choose what to restore", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExpressiveChip(selected = restoreSelection == held, onClick = { restoreChosen = held }, label = { Text("All in the file") })
                        val dataOnly = BackupPart.dataOnly.filter { it in held }.toSet()
                        ExpressiveChip(selected = restoreSelection == dataOnly, onClick = { restoreChosen = dataOnly }, label = { Text("Data only") })
                    }
                    BackupPart.entries.filter { it in held }.forEach { part ->
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                            Text(part.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            AppSwitch(
                                checked = part in restoreSelection,
                                onCheckedChange = { on ->
                                    restoreChosen = if (on) BackupPart.withNeeds(restoreSelection + part) else BackupPart.without(restoreSelection, part)
                                },
                            )
                        }
                    }
                }
                Text(
                    text = if (partMode && mergeMode) {
                        "Restart to apply. The parts you chose are merged into this phone; nothing is deleted. One safety copy is kept. Discard to keep what you have."
                    } else if (partMode) {
                        "Restart to apply. The parts you chose replace the same parts on this phone; everything else stays. One safety copy is kept. Discard to keep what you have."
                    } else {
                        "Restart to apply. This replaces your current data with the backup; one safety copy is kept. Discard to keep what you have."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
                val context = LocalContext.current
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                    PillButton(
                        text = "Restart app",
                        enabled = !partMode || restoreSelection.isNotEmpty(),
                        onClick = {
                            storageViewModel.chooseRestoreParts(if (partMode) restoreSelection else null, mergeMode)
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

        SectionTitle("Optional tools")
        StorageModuleToggles(settings, settingsViewModel)
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Erase current data and restore?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Warning: restoring erases the data you have now.",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        "Your current subjects, attendance, exams, holidays, timetable and notes are replaced by what is in the backup" +
                            " (and your photos and files too, if the backup has them). Anything you added since that backup was made is lost.",
                    )
                    Text(
                        "The backup is checked first and nothing changes until you restart. Your current data is then kept as one " +
                            "safety copy on this phone, but only one: restoring a second time replaces it. Make a backup of the current " +
                            "data first if you might want it back.",
                    )
                }
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

}
