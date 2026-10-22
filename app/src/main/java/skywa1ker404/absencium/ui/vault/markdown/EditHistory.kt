package skywa1ker404.absencium.ui.vault.markdown

/**
 * Undo and redo for the note text. A change is stored as the state before it; changes that come within [mergeMs] of the
 * previous one share a step, so one undo takes back a burst of typing rather than one letter. Pure, so it is tested on the JVM.
 */
class EditHistory(private val max: Int = 200, private val mergeMs: Long = 800) {
    private val undoStack = ArrayDeque<Edited>()
    private val redoStack = ArrayDeque<Edited>()
    private var lastAt = 0L
    private var hasLast = false

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    /** Call with the state as it was *before* a change. [force] starts a new step even inside the merge window (a toolbar action). */
    fun record(before: Edited, now: Long, force: Boolean = false) {
        if (force || undoStack.isEmpty() || !hasLast || now - lastAt > mergeMs) {
            undoStack.addLast(before)
            if (undoStack.size > max) undoStack.removeFirst()
        }
        redoStack.clear()
        lastAt = now
        hasLast = true
    }

    /** The state to go back to, or null when there is nothing to undo. [current] becomes redoable. */
    fun undo(current: Edited): Edited? {
        val previous = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(current)
        hasLast = false
        return previous
    }

    fun redo(current: Edited): Edited? {
        val next = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(current)
        hasLast = false
        return next
    }
}
