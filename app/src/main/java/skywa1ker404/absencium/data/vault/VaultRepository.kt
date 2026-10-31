package skywa1ker404.absencium.data.vault

import skywa1ker404.absencium.data.db.AttachmentEntity
import skywa1ker404.absencium.data.db.GuideEntity
import skywa1ker404.absencium.data.db.GuideType
import skywa1ker404.absencium.data.db.NoteEntity
import skywa1ker404.absencium.data.db.NoteKind
import skywa1ker404.absencium.data.db.NoteStatus
import skywa1ker404.absencium.data.db.SubjectEntity
import skywa1ker404.absencium.data.db.AbsenciumDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/** Everything the Vault screens show, read from Room in one snapshot. */
data class VaultData(
    val subjects: List<SubjectEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val guides: List<GuideEntity> = emptyList(),
    val attachments: List<AttachmentEntity> = emptyList(),
) {
    fun attachmentsOf(noteId: Long): List<AttachmentEntity> = attachments.filter { it.noteId == noteId }

    /** Notes that are filed in a folder. Drafts are kept out until they are saved properly. */
    fun notesOf(subjectId: Long, status: NoteStatus): List<NoteEntity> =
        notes.filter { it.subjectId == subjectId && it.status == status }

    val drafts: List<NoteEntity> get() = notes.filter { it.status == NoteStatus.DRAFT }
}

/** Offline-first source of truth for notes and guides. Room is read as a Flow. */
class VaultRepository(private val database: AbsenciumDatabase) {

    val data: Flow<VaultData> = combine(
        database.subjectDao().observeAll(),
        database.noteDao().observeAll(),
        database.guideDao().observeAll(),
        database.attachmentDao().observeAll(),
    ) { subjects, notes, guides, attachments -> VaultData(subjects, notes, guides, attachments) }

    suspend fun getNote(id: Long): NoteEntity? = database.noteDao().getById(id)

    /** Saves of one note run one at a time, in the order they were asked for, so an older save can never land after a newer one. */
    private val saveLock = Mutex()

    /**
     * Saves a note and returns its id. An [id] of 0, or one that no longer exists, inserts a new
     * note; otherwise the existing row is updated, so saving the same draft twice never makes a copy.
     */
    suspend fun saveNote(id: Long, subjectId: Long, title: String, body: String, status: NoteStatus, tags: String? = null): Long = saveLock.withLock {
        val dao = database.noteDao()
        // A note deleted while its editor was open still exists in the Trash. Saving writes to that row (it stays in the
        // Trash with the new text) rather than inserting a second copy next to it.
        val existing = if (id == 0L) null else dao.getAnyById(id)
        if (existing == null) {
            dao.insert(
                NoteEntity(
                    subjectId = subjectId,
                    title = title,
                    bodyText = body,
                    status = status,
                    tags = tags.orEmpty(),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        } else {
            dao.update(
                existing.copy(
                    subjectId = subjectId,
                    title = title,
                    bodyText = body,
                    status = status,
                    tags = tags ?: existing.tags,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            existing.id
        }
    }

    /** Creates an entry together with its attachments in one transaction and returns the entry's id. */
    suspend fun createEntry(
        subjectId: Long,
        title: String,
        kind: NoteKind,
        body: String,
        absentDate: LocalDate?,
        attachments: List<AttachmentEntity>,
    ): Long {
        val now = System.currentTimeMillis()
        // Notes made for a chosen day are filed under that day (at the time of day they were made), so the choice shows in the list too.
        val filedAt = absentDate?.atTime(java.time.LocalTime.now())?.atZone(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli() ?: now
        return database.entryDao().createWithAttachments(
            NoteEntity(
                subjectId = subjectId,
                title = title,
                bodyText = body,
                status = NoteStatus.ACTIVE,
                createdDate = filedAt,
                kind = kind,
                absentDate = absentDate,
                updatedAt = now,
            ),
            attachments,
        )
    }

    suspend fun attachmentsFor(noteId: Long): List<AttachmentEntity> = database.attachmentDao().getForNote(noteId)

    suspend fun addAttachment(attachment: AttachmentEntity): Long = database.attachmentDao().insert(attachment)

    suspend fun updateAttachment(attachment: AttachmentEntity) = database.attachmentDao().update(attachment)

    suspend fun deleteAttachment(attachment: AttachmentEntity) = database.attachmentDao().delete(attachment)

    suspend fun setNoteStatus(note: NoteEntity, status: NoteStatus) {
        database.noteDao().update(note.copy(status = status))
    }

    /**
     * Moves an entry to the Trash (its photos stay until it is purged, so a restore finds them). Returns true when the
     * entry was a draft, which is thrown away for good instead because nobody wants a draft back.
     */
    suspend fun deleteNote(note: NoteEntity): Boolean {
        if (note.status == NoteStatus.DRAFT) {
            database.noteDao().delete(note)
            return true
        }
        database.noteDao().softDelete(note.id, System.currentTimeMillis())
        return false
    }

    suspend fun restoreNote(id: Long) = database.noteDao().restore(id)

    suspend fun deleteNoteById(id: Long) {
        database.noteDao().getById(id)?.let { database.noteDao().delete(it) }
    }

    suspend fun addGuide(subjectId: Long, fileName: String, localUri: String, type: GuideType): Long =
        database.guideDao().insert(
            GuideEntity(subjectId = subjectId, fileName = fileName, localUri = localUri, guideType = type),
        )

    suspend fun deleteGuide(guide: GuideEntity) = database.guideDao().delete(guide)
}
