package com.trajectory.vault.data.subjects

import com.trajectory.vault.data.db.SubjectDao
import com.trajectory.vault.data.db.SubjectEntity
import com.trajectory.vault.logging.DebugLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

/** Offline-first source of truth for subjects: Room is read through a Flow, writes go straight to Room. */
class SubjectRepository(private val dao: SubjectDao) {

    val subjects: Flow<List<SubjectEntity>> = dao.observeAll()

    /** Returns null on success or a message for the user. */
    suspend fun add(name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return "Enter a subject name."
        return attempt("add \"$trimmed\"", "That subject already exists.") { dao.insert(SubjectEntity(name = trimmed)) }
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
