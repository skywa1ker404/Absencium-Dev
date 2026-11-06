package skywa1ker404.absencium.data.ai

import java.util.concurrent.TimeUnit

/** The note the student last had open, so "add this to the note" in Absencium AI can mean it. It is forgotten after half an hour. */
object AiContext {
    @Volatile private var noteId: Long? = null
    @Volatile private var at: Long = 0L

    fun noteViewed(id: Long) {
        noteId = id
        at = System.currentTimeMillis()
    }

    fun recentNoteId(): Long? = noteId?.takeIf { System.currentTimeMillis() - at < TimeUnit.MINUTES.toMillis(30) }
}
