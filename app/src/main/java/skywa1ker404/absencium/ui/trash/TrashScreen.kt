package skywa1ker404.absencium.ui.trash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import skywa1ker404.absencium.data.trash.TrashKind
import skywa1ker404.absencium.ui.components.ExpressiveEmptyState
import skywa1ker404.absencium.ui.components.bounceIn
import skywa1ker404.absencium.ui.components.hueFor
import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.vault.CardChip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import skywa1ker404.absencium.ui.components.AlertDialog
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
        if (list != null && list.isEmpty()) {
            ExpressiveEmptyState(
                icon = AppIcons.Delete,
                title = "The Trash is empty",
                text = "Entries, exams, holidays and lectures you delete wait here for ${TrashRepository.RETENTION_DAYS} days.",
            )
        }
        if (!list.isNullOrEmpty()) {
            PillButton(text = "Empty the Trash", tonal = true, onClick = { confirmEmpty = true })
        }
        list.orEmpty().forEachIndexed { index, item ->
            androidx.compose.runtime.key(item.key) {
                val hue = hueFor(item.kind.ordinal.toLong())
                val left = TrashRepository.daysLeft(item.deletedAt)
                val colors = MaterialTheme.colorScheme
                AppCard(
                    modifier = Modifier.fillMaxWidth().bounceIn((index * 40).coerceAtMost(240)),
                    container = hue.container.copy(alpha = 0.7f),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(colors.surface.copy(alpha = 0.9f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = when (item.kind) {
                                    TrashKind.NOTE -> AppIcons.Description
                                    TrashKind.EXAM -> AppIcons.Quiz
                                    TrashKind.HOLIDAY -> AppIcons.CalendarMonth
                                    TrashKind.LECTURE -> AppIcons.Schedule
                                },
                                contentDescription = null,
                                tint = hue.accent,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium, color = hue.onContainer, maxLines = 2)
                            Text(
                                text = listOf(item.kind.label, item.detail).filter { it.isNotBlank() }.joinToString(" \u00b7 "),
                                style = MaterialTheme.typography.bodySmall,
                                color = hue.onContainer.copy(alpha = 0.75f),
                                maxLines = 1,
                            )
                        }
                        // Few days left is the one thing worth a warning colour.
                        CardChip(
                            text = "$left ${if (left == 1) "day" else "days"} left",
                            color = if (left <= 3) colors.error else hue.onContainer,
                        )
                    }
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
