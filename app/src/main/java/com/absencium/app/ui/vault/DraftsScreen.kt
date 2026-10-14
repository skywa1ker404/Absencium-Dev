package com.absencium.app.ui.vault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.NewEntryScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.absencium.app.data.db.NoteEntity
import com.absencium.app.ui.components.AppCard
import com.absencium.app.ui.components.HintText
import com.absencium.app.ui.components.ScreenScaffold
import com.absencium.app.ui.containerViewModel

/** Entries that were saved with Save to Drafts. Tap one to keep editing it. */
@Destination<RootGraph>
@Composable
fun DraftsScreen(navigator: DestinationsNavigator) {
    val viewModel = containerViewModel { VaultViewModel(it) }
    val data by viewModel.data.collectAsStateWithLifecycle()
    var toDelete by remember { mutableStateOf<NoteEntity?>(null) }
    val colors = MaterialTheme.colorScheme

    ScreenScaffold(title = "Drafts", onBack = { navigator.navigateUp() }) {
        val loaded = data
        if (loaded == null) {
            HintText("Loading...")
            return@ScreenScaffold
        }
        val names = loaded.subjects.associate { it.id to it.name }
        if (loaded.drafts.isEmpty()) {
            HintText("No drafts. When you leave a new entry unfinished, choose Save to Drafts to keep it here.")
        }
        loaded.drafts.forEach { draft ->
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    navigator.navigate(NewEntryScreenDestination(subjectId = draft.subjectId, draftId = draft.id))
                },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                        Text(draft.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        Text(
                            text = "${names[draft.subjectId] ?: "Unknown subject"} · ${formatNoteDate(draft.createdDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                        val preview = draft.snippet()
                        if (preview.isNotEmpty()) {
                            Text(preview, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                        }
                    }
                    TextButton(onClick = { toDelete = draft }) { Text("Delete") }
                }
            }
        }
    }

    val pending = toDelete
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete this draft?") },
            text = { Text("${pending.title} will be deleted for good.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteNote(pending)
                    toDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Keep") } },
        )
    }
}
