package skywa1ker404.absencium.data.sync

/** The kinds of rows that carry a `uuid`, `updated_at` and, where it applies, `deleted_at` (schema version 2). */
enum class SyncTable(val tableName: String) {
    SUBJECTS("Subjects"),
    ATTENDANCE("Attendance"),
    HOLIDAYS("Holidays"),
    EXAMS("Exams"),
    TIMETABLE("Timetable"),
    NOTES("Notes"),
    ATTACHMENTS("Attachments"),
    GUIDES("Guides"),
}

/** One row that changed: its stable id, when it last changed, and whether it was deleted (a tombstone) rather than edited. */
data class RowChange(
    val table: SyncTable,
    val uuid: String,
    val updatedAt: Long,
    val deleted: Boolean,
)

/** What changed since [sinceMillis] (exclusive). The unit that a later sync would send, merge and apply. */
data class ChangeSet(val sinceMillis: Long, val rows: List<RowChange>)

/**
 * Builds a [ChangeSet] from the database. Only the contract exists today; the Room-backed builder arrives with sync
 * (Step 18) together with the merge rules, because the row-level queries and conflict handling should be designed once
 * with a real transport in front of them.
 */
interface ChangeSetBuilder {
    suspend fun changesSince(sinceMillis: Long): ChangeSet
}
