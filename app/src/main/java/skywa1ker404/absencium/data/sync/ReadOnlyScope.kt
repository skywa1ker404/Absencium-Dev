package skywa1ker404.absencium.data.sync

/**
 * What a read-only share may show (Step 17). Sharing never hands out the database: it passes one of these to a view that
 * can only read, and anything not named here is invisible. Empty by default, so forgetting to fill it in shares nothing.
 */
data class ReadOnlyScope(
    val subjectUuids: Set<String> = emptySet(),
    val includeAttendance: Boolean = false,
    val includeNotes: Boolean = false,
    val includeExams: Boolean = false,
    val includeTimetable: Boolean = false,
) {
    fun allows(subjectUuid: String): Boolean = subjectUuid in subjectUuids
    val isEmpty: Boolean get() = subjectUuids.isEmpty()
}
