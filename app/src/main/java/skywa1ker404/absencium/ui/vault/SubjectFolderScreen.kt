package skywa1ker404.absencium.ui.vault

import android.widget.Toast
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AiSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.NewEntryScreenDestination
import com.ramcosta.composedestinations.generated.destinations.NoteEditorScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import skywa1ker404.absencium.data.db.GuideEntity
import skywa1ker404.absencium.data.db.NoteStatus
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.vault.entry.AddEntryPopup
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.MessageCard
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.components.SectionTitle
import skywa1ker404.absencium.ui.containerViewModel

/** One subject's folder: its notes, the Finished / Written Notes archive, and Guides and Question Banks. */
@Destination<RootGraph>
@Composable
fun SubjectFolderScreen(subjectId: Long, navigator: DestinationsNavigator) {
    val viewModel = containerViewModel { VaultViewModel(it) }
    val data by viewModel.data.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var finishedOpen by rememberSaveable { mutableStateOf(false) }
    var addEntry by remember { mutableStateOf(false) }
    var guideToDelete by remember { mutableStateOf<GuideEntity?>(null) }
    val colors = MaterialTheme.colorScheme

    val loaded = data
    val subject = loaded?.subjects?.firstOrNull { it.id == subjectId }

    ScreenScaffold(title = subject?.name ?: "Folder", onBack = { navigator.navigateUp() }) {
        if (loaded == null) {
            HintText("Loading...")
            return@ScreenScaffold
        }
        if (subject == null) {
            HintText("This subject no longer exists.")
            return@ScreenScaffold
        }
        if (message != null) {
            AppCard(modifier = Modifier.fillMaxWidth(), highlighted = true, onClick = { viewModel.clearMessage() }) {
                Text(message.orEmpty(), style = MaterialTheme.typography.bodyMedium)
            }
        }

        PillButton(
            text = "New entry in this folder",
            onClick = { addEntry = true },
        )

        val active = loaded.notesOf(subjectId, NoteStatus.ACTIVE)
        val finished = loaded.notesOf(subjectId, NoteStatus.FINISHED)
        val guides = loaded.guides.filter { it.subjectId == subjectId }

        SectionTitle("Notes")
        if (active.isEmpty()) HintText("No notes yet. Notes you mark as finished move to the archive below.")
        active.forEach { note ->
            NoteCard(note, onClick = { navigator.navigate(NoteEditorScreenDestination(noteId = note.id)) })
        }

        SectionTitle("Finished / Written Notes")
        AppCard(modifier = Modifier.fillMaxWidth(), onClick = { finishedOpen = !finishedOpen }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${finished.size} ${if (finished.size == 1) "note" else "notes"} archived",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = if (finishedOpen) "Hide" else "Show",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary,
                )
            }
        }
        if (finishedOpen) {
            finished.forEach { note ->
                NoteCard(note, onClick = { navigator.navigate(NoteEditorScreenDestination(noteId = note.id)) })
            }
        }

        SectionTitle("Guides & Question Banks")
        if (guides.isEmpty()) HintText("No PDFs yet. Add one from a new entry with the Guide / Answer PDF format.")
        guides.forEach { guide ->
            GuideCard(
                guide = guide,
                onClick = {
                    openGuide(context, guide)?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                },
                onDelete = { guideToDelete = guide },
            )
        }
    }

    val pending = guideToDelete
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { guideToDelete = null },
            title = { Text("Remove this PDF?") },
            text = { Text("${pending.fileName} will be removed from the vault and from the notes folder.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteGuide(pending)
                    guideToDelete = null
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { guideToDelete = null }) { Text("Keep") } },
        )
    }

    if (addEntry) {
        AddEntryPopup(
            onDismiss = { addEntry = false },
            onSaved = { id ->
                addEntry = false
                navigator.navigate(NoteEditorScreenDestination(noteId = id))
            },
            onOpenAiSettings = {
                addEntry = false
                navigator.navigate(AiSettingsScreenDestination)
            },
            initialSubjectId = subjectId,
        )
    }
}
