package com.trajectory.vault.data.vault

import com.trajectory.vault.data.db.GuideEntity
import com.trajectory.vault.data.db.GuideType
import com.trajectory.vault.data.db.NoteEntity
import com.trajectory.vault.data.db.NoteStatus
import com.trajectory.vault.data.db.SubjectEntity
import com.trajectory.vault.data.db.TrajectoryDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Everything the Vault screens show, read from Room in one snapshot. */
data class VaultData(
    val subjects: List<SubjectEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val guides: List<GuideEntity> = emptyList(),
) {
    /** Notes that are filed in a folder. Drafts are kept out until they are saved properly. */
    fun notesOf(subjectId: Long, status: NoteStatus): List<NoteEntity> =
        notes.filter { it.subjectId == subjectId && it.status == status }

    val drafts: List<NoteEntity> get() = notes.filter { it.status == NoteStatus.DRAFT }
}

/** Offline-first source of truth for notes and guides. Room is read as a Flow. */
class VaultRepository(private val database: TrajectoryDatabase) {

    val data: Flow<VaultData> = combine(
        database.subjectDao().observeAll(),
        database.noteDao().observeAll(),
        database.guideDao().observeAll(),
    ) { subjects, notes, guides -> VaultData(subjects, notes, guides) }

    suspend fun getNote(id: Long): NoteEntity? = database.noteDao().getById(id)

    /**
     * Saves a note and returns its id. An [id] of 0, or one that no longer exists, inserts a new
     * note; otherwise the existing row is updated, so saving the same draft twice never makes a copy.
     */
    suspend fun saveNote(id: Long, subjectId: Long, title: String, body: String, status: NoteStatus): Long {
        val dao = database.noteDao()
        val existing = if (id == 0L) null else dao.getById(id)
        return if (existing == null) {
            dao.insert(NoteEntity(subjectId = subjectId, title = title, bodyText = body, status = status))
        } else {
            dao.update(existing.copy(subjectId = subjectId, title = title, bodyText = body, status = status))
            existing.id
        }
    }

    suspend fun setNoteStatus(note: NoteEntity, status: NoteStatus) {
        database.noteDao().update(note.copy(status = status))
    }

    suspend fun deleteNote(note: NoteEntity) = database.noteDao().delete(note)

    suspend fun deleteNoteById(id: Long) {
        database.noteDao().getById(id)?.let { database.noteDao().delete(it) }
    }

    suspend fun addGuide(subjectId: Long, fileName: String, localUri: String, type: GuideType): Long =
        database.guideDao().insert(
            GuideEntity(subjectId = subjectId, fileName = fileName, localUri = localUri, guideType = type),
        )

    suspend fun deleteGuide(guide: GuideEntity) = database.guideDao().delete(guide)
}
