package com.absencium.app.data.subjects

import com.absencium.app.data.db.SubjectCascadeDao
import com.absencium.app.data.db.SubjectDao
import com.absencium.app.data.db.SubjectEntity
import com.absencium.app.logging.DebugLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

/** Offline-first source of truth for subjects: Room is read through a Flow, writes go straight to Room. */
class SubjectRepository(private val dao: SubjectDao, private val cascade: SubjectCascadeDao) {

    val subjects: Flow<List<SubjectEntity>> = dao.observeAll()

    /** Returns null on success or a message for the user. */
    suspend fun add(name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return "Enter a subject name."
        return attempt("add \"$trimmed\"", "That subject already exists.") { dao.insert(SubjectEntity(name = trimmed)) }
    }

    /** Renames a subject. Returns null on success or a message for the user. */
    suspend fun rename(subject: SubjectEntity, name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return "Enter a subject name."
        if (trimmed == subject.name) return null
        return attempt("rename ${subject.name}", "Another subject already has that name.") {
            dao.update(subject.copy(name = trimmed))
        }
    }

    suspend fun setActive(subject: SubjectEntity, active: Boolean): String? =
        attempt("update ${subject.name}", "Could not update the subject.") { dao.update(subject.copy(isActive = active)) }

    suspend fun setMuted(subject: SubjectEntity, muted: Boolean): String? =
        attempt("update ${subject.name}", "Could not update the subject.") { dao.update(subject.copy(isMuted = muted)) }

    suspend fun delete(subject: SubjectEntity): String? =
        attempt(
            "delete ${subject.name}",
            "\"${subject.name}\" still has attendance, exams, notes or other records, so it cannot be deleted. " +
                "Mark it inactive instead.",
        ) { dao.delete(subject) }

    /** How many attendance records, exams, lectures, notes and guides are filed under [subject]. */
    suspend fun entryCount(subject: SubjectEntity): Int = cascade.entryCount(subject.id)

    /** Deletes the subject and everything under it in one transaction. Returns null on success or a message. */
    suspend fun deleteWithEntries(subject: SubjectEntity): String? =
        attempt("delete ${subject.name} with its entries", "Could not delete \"${subject.name}\". See debug.log for details.") {
            cascade.deleteWithEntries(subject.id)
        }

    private suspend fun attempt(action: String, failureMessage: String, block: suspend () -> Unit): String? =
        try {
            block()
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Could not $action", e)
            failureMessage
        }

    private companion object {
        const val TAG = "Subjects"
    }
}
