package skywa1ker404.absencium.data.backup

/**
 * The parts of the owner's data a backup is made of (docs/BACKUP_DESIGN.md, section 3). A part maps to database tables by name,
 * may hold files, and may need another part: a note needs its subject, a photo needs its note. Entries are listed roots first,
 * so [needs] only ever points at an earlier entry.
 */
enum class BackupPart(
    val title: String,
    val tables: List<String>,
    val needs: Set<BackupPart> = emptySet(),
    /** The rows of this part point at files (photos, documents, recordings) that travel with it. */
    val hasFiles: Boolean = false,
) {
    SETTINGS("Profile and settings", emptyList()),
    SUBJECTS("Subjects", listOf("Subjects_Table")),
    TIMETABLE("Timetable", listOf("Timetable_Table"), setOf(SUBJECTS)),
    ATTENDANCE("Attendance", listOf("Attendance_Table"), setOf(SUBJECTS)),
    EXAMS("Exams", listOf("Exams_Table"), setOf(SUBJECTS)),
    SYLLABUS("Syllabus", listOf("Syllabus_Nodes_Table"), setOf(SUBJECTS)),
    HOLIDAYS_EVENTS("Holidays and events", listOf("Holidays_Table", "Events_Table")),
    FLASHCARDS("Flashcards", listOf("Flashcards_Table")),
    GOALS("Study goals", listOf("Study_Goals_Table")),
    STATS("Study stats and momentum", listOf("User_Stats_Table", "Momentum_Log_Table")),
    NOTES("Notes (text)", listOf("Notes_Table"), setOf(SUBJECTS)),
    NOTE_HISTORY("Note version history", listOf("Note_Versions_Table"), setOf(NOTES)),
    NOTE_FILES("Note photos, files and sketches", listOf("Attachments_Table"), setOf(NOTES), hasFiles = true),
    DOCUMENTS("Documents (Vault)", listOf("Guides_Table"), setOf(SUBJECTS), hasFiles = true),
    RECORDINGS("Recordings", listOf("Audio_Memos_Table"), setOf(NOTES), hasFiles = true),
    RECALL("Recall and quizzes", listOf("Recall_Quiz_Table", "Recall_Attempts_Table"), setOf(NOTES)),
    AI_HISTORY("AI action history", listOf("AI_History_Table"), setOf(NOTES)),

    // The AI chat lives in its own database and files; it travels as one archive made by the AI hub (hub/ai.zip), so these have no tables here.
    AI_CHATS("AI chats and workspaces", emptyList()),
    AI_SETTINGS("AI settings, assistants and providers", emptyList()),
    AI_KEYS("AI keys (only in a password-protected file)", emptyList(), setOf(AI_SETTINGS));

    /** Part of the AI hub's archive rather than of the app's database. */
    val isAi: Boolean get() = this == AI_CHATS || this == AI_SETTINGS || this == AI_KEYS

    companion object {
        /** The three ready-made choices; "My choice" is the last set the owner saved, kept in the settings. */
        // Keys are never part of a preset: the owner switches them on, and only a password-protected file can hold them.
        val everything: Set<BackupPart> get() = entries.toSet() - AI_KEYS
        val lightWithoutMedia: Set<BackupPart> get() = everything - setOf(NOTE_FILES, DOCUMENTS, RECORDINGS, AI_CHATS)
        val dataOnly: Set<BackupPart> get() =
            setOf(SUBJECTS, TIMETABLE, ATTENDANCE, EXAMS, SYLLABUS, HOLIDAYS_EVENTS, NOTES)

        /** Part ids as saved in the settings: names, comma separated. Unknown names (from a newer app) are ignored. */
        fun parse(saved: String?): Set<BackupPart>? {
            if (saved == null) return null
            val names = saved.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            return withNeeds(entries.filter { it.name in names }.toSet())
        }

        fun format(parts: Set<BackupPart>): String = entries.filter { it in parts }.joinToString(",") { it.name }

        /** Every table that belongs to a part. A unit test fails when a table of the database is missing here. */
        val allTables: Set<String> get() = entries.flatMap { it.tables }.toSet()

        /** [selected] plus everything it needs, so switching a part on also switches on what it depends on. */
        fun withNeeds(selected: Set<BackupPart>): Set<BackupPart> {
            val result = selected.toMutableSet()
            var changed = true
            while (changed) {
                changed = false
                for (part in result.toList()) if (result.addAll(part.needs)) changed = true
            }
            return result
        }

        /** [selected] without [part] and without every part that needs it, directly or through another part. */
        fun without(selected: Set<BackupPart>, part: BackupPart): Set<BackupPart> {
            val result = selected.toMutableSet()
            result.remove(part)
            var changed = true
            while (changed) {
                changed = false
                for (other in result.toList()) if (other.needs.any { it !in result }) { result.remove(other); changed = true }
            }
            return result
        }

        /** The parts that need [part], directly or through another part (what switching it off also switches off). */
        fun dependentsOf(part: BackupPart): Set<BackupPart> =
            entries.filter { it != part && part in withNeeds(setOf(it)) }.toSet()
    }
}

/** What one part would add to a backup: rows, files and an approximate size in bytes before compression. */
data class PartEstimate(val part: BackupPart, val rows: Int, val files: Int, val bytes: Long)
