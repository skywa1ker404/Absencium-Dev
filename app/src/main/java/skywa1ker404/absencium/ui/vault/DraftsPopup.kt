package skywa1ker404.absencium.ui.vault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import skywa1ker404.absencium.ui.components.AlertDialog
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
import skywa1ker404.absencium.data.db.NoteEntity
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.PopupBody
import skywa1ker404.absencium.ui.components.PopupShell
import skywa1ker404.absencium.ui.containerViewModel

/** Entries that were saved with Save to Drafts, as a popup. Tap one to keep editing it. */
@Composable
fun DraftsPopup(onDismiss: () -> Unit, onOpenDraft: (NoteEntity) -> Unit, subjectId: Long? = null) {
    val viewModel = containerViewModel { VaultViewModel(it) }
    val data by viewModel.data.collectAsStateWithLifecycle()
    var toDelete by remember { mutableStateOf<NoteEntity?>(null) }
    val colors = MaterialTheme.colorScheme

    PopupShell(onDismissRequest = onDismiss, dismissOnOutsideTap = true) {
    PopupBody(title = "Drafts", onClose = onDismiss) {
        val loaded = data
        if (loaded == null) {
            HintText("Loading...")
            return@PopupBody
        }
        val names = loaded.subjects.associate { it.id to it.name }
        // Opened from a subject folder, only that subject's drafts are listed.
        val shown = if (subjectId == null) loaded.drafts else loaded.drafts.filter { it.subjectId == subjectId }
        if (shown.isEmpty()) {
            HintText("No drafts. When you leave a new entry unfinished, choose Save to Drafts to keep it here.")
        }
        shown.forEach { draft ->
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onOpenDraft(draft) },
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
