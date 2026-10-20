package skywa1ker404.absencium.data.trash

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** An offer to take back the last delete: a short message and what to run if the user taps Undo. */
data class UndoRequest(val id: Long, val message: String, val undo: suspend () -> Unit)

/**
 * One place for "Deleted. Undo" offers, so every delete in the app behaves the same. Only the latest offer is kept;
 * the host shows it for a few seconds. The deleted data stays in the Trash either way.
 */
class UndoBus {
    private var next = 0L
    private val _current = MutableStateFlow<UndoRequest?>(null)
    val current: StateFlow<UndoRequest?> = _current.asStateFlow()

    fun offer(message: String, undo: suspend () -> Unit) {
        _current.value = UndoRequest(++next, message, undo)
    }

    /** Clears the offer, but only if it is still [id] (a newer one may have replaced it). */
    fun dismiss(id: Long) {
        if (_current.value?.id == id) _current.value = null
    }
}
