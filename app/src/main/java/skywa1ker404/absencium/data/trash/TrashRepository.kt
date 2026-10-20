package skywa1ker404.absencium.data.trash

import skywa1ker404.absencium.data.db.AbsenciumDatabase
import skywa1ker404.absencium.data.storage.AttachmentStore
import skywa1ker404.absencium.logging.DebugLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.format.TextStyle
import java.util.Locale

enum class TrashKind(val label: String) { NOTE("Entry"), EXAM("Exam"), HOLIDAY("Holiday"), LECTURE("Lecture") }

/** One thing in the Trash, whatever table it came from. */
data class TrashItem(
    val kind: TrashKind,
    val id: Long,
    val title: String,
    val detail: String,
    /** Epoch milliseconds when it was moved to the Trash. */
    val deletedAt: Long,
) {
    val key: String get() = "${kind.name}-$id"
}

/**
 * The Trash: rows that were deleted but can still be brought back. They stay in their own tables with `deleted_at` set,
 * and every normal query leaves them out. After [RETENTION_DAYS] they are removed for good, and the files that belong to a
 * deleted entry go with them (never before, so a restore always finds its photos).
 */
class TrashRepository(
    private val database: AbsenciumDatabase,
    private val attachmentStore: AttachmentStore,
) {

    val items: Flow<List<TrashItem>> = combine(
        database.noteDao().observeTrashed(),
        database.examDao().observeTrashed(),
        database.holidayDao().observeTrashed(),
        database.timetableDao().observeTrashed(),
        database.subjectDao().observeAll(),
    ) { notes, exams, holidays, slots, subjects ->
        val names = subjects.associate { it.id to it.name }
        val out = ArrayList<TrashItem>(notes.size + exams.size + holidays.size + slots.size)
        notes.forEach {
            out += TrashItem(TrashKind.NOTE, it.id, it.title.ifBlank { "Untitled" }, names[it.subjectId] ?: "", it.deletedAt ?: 0L)
        }
        exams.forEach {
            out += TrashItem(TrashKind.EXAM, it.id, it.title, "${names[it.subjectId] ?: ""} · ${it.date}", it.deletedAt ?: 0L)
        }
        holidays.forEach {
            out += TrashItem(TrashKind.HOLIDAY, it.id, it.title, it.date.toString(), it.deletedAt ?: 0L)
        }
        slots.forEach {
            val day = it.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            out += TrashItem(TrashKind.LECTURE, it.id, names[it.subjectId] ?: "Lecture", "$day ${it.startTime}", it.deletedAt ?: 0L)
        }
        out.sortedByDescending { it.deletedAt }
    }

    suspend fun restore(item: TrashItem) {
        when (item.kind) {
            TrashKind.NOTE -> database.noteDao().restore(item.id)
            TrashKind.EXAM -> database.examDao().restore(item.id)
            TrashKind.HOLIDAY -> database.holidayDao().restore(item.id)
            TrashKind.LECTURE -> database.timetableDao().restore(item.id)
        }
    }

    suspend fun purge(item: TrashItem) {
        when (item.kind) {
            TrashKind.NOTE -> purgeNote(item.id)
            TrashKind.EXAM -> database.examDao().purge(item.id)
            TrashKind.HOLIDAY -> database.holidayDao().purge(item.id)
            TrashKind.LECTURE -> database.timetableDao().purge(item.id)
        }
    }

    suspend fun emptyTrash(all: List<TrashItem>) {
        all.forEach { purge(it) }
    }

    /** Removes everything that has been in the Trash longer than [RETENTION_DAYS]. Returns how many rows went. */
    suspend fun purgeExpired(now: Long = System.currentTimeMillis()): Int {
        val before = now - RETENTION_DAYS * DAY_MS
        var count = 0
        val noteIds = database.noteDao().trashedIdsBefore(before)
        noteIds.forEach { purgeNote(it) }
        count += noteIds.size
        count += database.examDao().purgeBefore(before)
        count += database.holidayDao().purgeBefore(before)
        count += database.timetableDao().purgeBefore(before)
        if (count > 0) DebugLogger.i(TAG, "Purged $count items older than $RETENTION_DAYS days from the Trash")
        return count
    }

    private suspend fun purgeNote(id: Long) {
        val note = database.noteDao().getAnyById(id) ?: return
        if (note.deletedAt == null) return
        val files = database.attachmentDao().getForNote(id).map { it.filePath }
        database.noteDao().delete(note)
        files.forEach { attachmentStore.delete(it) }
    }

    companion object {
        const val RETENTION_DAYS = 30
        const val DAY_MS = 86_400_000L
        private const val TAG = "Trash"

        /** Whole days left before an item deleted at [deletedAt] is removed for good (never below 0). */
        fun daysLeft(deletedAt: Long, now: Long = System.currentTimeMillis()): Int =
            (RETENTION_DAYS - ((now - deletedAt) / DAY_MS).toInt()).coerceAtLeast(0)
    }
}
