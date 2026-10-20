package skywa1ker404.absencium.ui.trash

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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import skywa1ker404.absencium.AppContainer
import skywa1ker404.absencium.data.trash.TrashItem
import skywa1ker404.absencium.data.trash.TrashRepository
import skywa1ker404.absencium.logging.DebugLogger
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.MessageCard
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.containerViewModel

class TrashViewModel(container: AppContainer) : ViewModel() {

    private val repository = container.trashRepository

    /** Null until the first read finishes. */
    val items: StateFlow<List<TrashItem>?> = repository.items
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun restore(item: TrashItem) = run("restore it") {
        repository.restore(item)
        _message.value = "Restored \"${item.title}\"."
    }

    fun purge(item: TrashItem) = run("delete it") {
        repository.purge(item)
        _message.value = "Deleted \"${item.title}\" for good."
    }

    fun emptyTrash() = run("empty the Trash") {
        repository.emptyTrash(items.value.orEmpty())
        _message.value = "The Trash is empty."
    }

    private fun run(action: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DebugLogger.e("Trash", "Could not $action", e)
                _message.value = "Could not $action. See debug.log for details."
            }
        }
    }
}

/** Deleted entries, exams, holidays and lectures. They can be restored for 30 days, then they are removed for good. */
@Destination<RootGraph>
@Composable
fun TrashScreen(navigator: DestinationsNavigator) {
    val viewModel = containerViewModel { TrashViewModel(it) }
    val items by viewModel.items.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var confirmEmpty by remember { mutableStateOf(false) }
    var confirmPurge by remember { mutableStateOf<TrashItem?>(null) }
    val list = items

    ScreenScaffold(title = "Trash", onBack = { navigator.navigateUp() }) {
        HintText("Deleted items stay here for ${TrashRepository.RETENTION_DAYS} days and can be restored. After that they are removed for good.")
        message?.let { MessageCard(it) }
        if (list != null && list.isEmpty()) HintText("The Trash is empty.")
        if (!list.isNullOrEmpty()) {
            PillButton(text = "Empty the Trash", tonal = true, onClick = { confirmEmpty = true })
        }
        list.orEmpty().forEach { item ->
            androidx.compose.runtime.key(item.key) {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    val left = TrashRepository.daysLeft(item.deletedAt)
                    Text(
                        text = listOf(item.kind.label, item.detail).filter { it.isNotBlank() }.joinToString(" · ") +
                            " · $left ${if (left == 1) "day" else "days"} left",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                        PillButton(text = "Restore", compact = true, onClick = { viewModel.restore(item) })
                        PillButton(text = "Delete for good", compact = true, tonal = true, onClick = { confirmPurge = item })
                    }
                }
            }
        }
    }

    if (confirmEmpty) {
        AlertDialog(
            onDismissRequest = { confirmEmpty = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Empty the Trash?") },
            text = { Text("Everything in the Trash is deleted for good, with the photos and files of the deleted entries. This cannot be undone.") },
            confirmButton = { TextButton(onClick = { confirmEmpty = false; viewModel.emptyTrash() }) { Text("Empty") } },
            dismissButton = { TextButton(onClick = { confirmEmpty = false }) { Text("Cancel") } },
        )
    }
    confirmPurge?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmPurge = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Delete \"${item.title}\" for good?") },
            text = { Text("It cannot be restored after this.") },
            confirmButton = { TextButton(onClick = { confirmPurge = null; viewModel.purge(item) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmPurge = null }) { Text("Cancel") } },
        )
    }
}
