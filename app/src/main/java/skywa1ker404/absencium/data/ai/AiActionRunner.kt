package skywa1ker404.absencium.data.ai

import kotlinx.coroutines.flow.first
import skywa1ker404.absencium.data.calendar.CalendarRepository
import skywa1ker404.absencium.data.db.AbsenciumDatabase
import skywa1ker404.absencium.data.db.NoteStatus
import skywa1ker404.absencium.data.db.Priority
import skywa1ker404.absencium.data.db.SubjectEntity
import skywa1ker404.absencium.data.subjects.SubjectRepository
import skywa1ker404.absencium.data.vault.VaultRepository
import skywa1ker404.absencium.logging.DebugLogger

/** What happened when the student applied one proposed change. */
data class ActionResult(val ok: Boolean, val message: String)

/**
 * Applies the changes Absencium AI proposed, one at a time, through the same repositories the screens use, so everything it does can be
 * undone the same way (deleted items go to the Trash). Every lookup is by name and must match exactly one thing; otherwise the change is
 * refused with a plain reason instead of guessing.
 */
class AiActionRunner(
    private val database: AbsenciumDatabase,
    private val calendar: CalendarRepository,
    private val vault: VaultRepository,
    private val subjects: SubjectRepository,
    /** Writes a note from a brief (the app's note writer); returns editor markup, or null when the model could not write it. */
    private val noteWriter: suspend (brief: String, subject: String, length: NoteLength) -> String? = { _, _, _ -> null },
) {
    suspend fun apply(action: AiAction): ActionResult = try {
        run(action)
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        DebugLogger.e("AiActions", "A proposed change failed: ${action.describe()}", e)
        ActionResult(false, "That change could not be made.")
    }

    private suspend fun run(action: AiAction): ActionResult {
        return when (action) {
            is AiAction.AddSubject -> subjects.add(action.name)?.let { ActionResult(false, it) } ?: ActionResult(true, "Added the subject.")

            is AiAction.AddNote -> {
                val subject = subject(action.subject) ?: return missing(action.subject)
                val body = action.body.ifBlank {
                    noteWriter(action.brief.ifBlank { action.title }, subject.name, action.length)
                        ?: return ActionResult(false, "The note could not be written. Check the AI connection and try again.")
                }
                vault.saveNote(0, subject.id, action.title, body, NoteStatus.ACTIVE)
                ActionResult(true, "Added the note to ${subject.name}.")
            }

            is AiAction.AddSlot -> {
                val subject = subject(action.subject) ?: return missing(action.subject)
                if (!action.end.isAfter(action.start)) return ActionResult(false, "The lecture must end after it starts.")
                calendar.addSlot(action.day, subject.id, action.start, action.end, action.room)
                ActionResult(true, "Added to the timetable.")
            }

            is AiAction.UpdateSlot -> {
                val subject = subject(action.subject) ?: return missing(action.subject)
                val slots = database.timetableDao().observeAll().first()
                    .filter { it.subjectId == subject.id && it.dayOfWeek == action.day && (action.matchStart == null || it.startTime == action.matchStart) }
                val slot = slots.singleOrNull()
                    ?: return ActionResult(false, if (slots.isEmpty()) "No such lecture on the timetable." else "Several lectures match; say which time.")
                val changed = slot.copy(
                    dayOfWeek = action.newDay ?: slot.dayOfWeek,
                    startTime = action.newStart ?: slot.startTime,
                    endTime = action.newEnd ?: slot.endTime,
                    roomNumber = action.room ?: slot.roomNumber,
                )
                if (!changed.endTime.isAfter(changed.startTime)) return ActionResult(false, "The lecture must end after it starts.")
                calendar.updateSlot(changed)
                ActionResult(true, "Timetable changed.")
            }

            is AiAction.AddExam -> {
                val subject = subject(action.subject) ?: return missing(action.subject)
                calendar.addExam(subject.id, action.date, action.title, action.weightage, action.room, null, Priority.HIGH)
                ActionResult(true, "Exam added.")
            }

            is AiAction.UpdateExam -> {
                val names = database.subjectDao().observeAll().first().associate { it.id to it.name }
                val matches = database.examDao().observeAll().first().filter {
                    it.title.contains(action.match, ignoreCase = true) || names[it.subjectId].orEmpty().contains(action.match, ignoreCase = true)
                }
                val exam = matches.singleOrNull()
                    ?: return ActionResult(false, if (matches.isEmpty()) "No exam matches \"${action.match}\"." else "Several exams match \"${action.match}\".")
                calendar.updateExam(exam.copy(title = action.title ?: exam.title, date = action.date ?: exam.date, room = action.room ?: exam.room))
                ActionResult(true, "Exam changed.")
            }

            is AiAction.AddHoliday -> {
                if (database.holidayDao().getForDate(action.date) != null) return ActionResult(false, "That day already has a holiday.")
                calendar.addHoliday(action.date, action.title, true, Priority.LOW)
                ActionResult(true, "Holiday added.")
            }

            // Memories are saved by the chat itself and never reach the runner; this keeps the branch total.
            is AiAction.Remember -> ActionResult(true, "Remembered.")

            is AiAction.SetSyllabusStatus -> {
                val subject = subject(action.subject) ?: return missing(action.subject)
                val nodes = database.syllabusDao().observeAll().first().filter { it.subjectId == subject.id }
                val matches = nodes.filter { it.title.equals(action.topic.trim(), ignoreCase = true) }
                    .ifEmpty { nodes.filter { it.title.contains(action.topic.trim(), ignoreCase = true) } }
                val node = matches.singleOrNull()
                    ?: return ActionResult(false, if (matches.isEmpty()) "No syllabus topic matches \"${action.topic}\"." else "Several topics match \"${action.topic}\".")
                database.syllabusDao().update(node.copy(status = action.status, updatedAt = System.currentTimeMillis()))
                ActionResult(true, "Syllabus updated.")
            }

            is AiAction.SetAttendance -> {
                val subject = subject(action.subject) ?: return missing(action.subject)
                if (database.attendanceDao().getFor(subject.id, action.date)?.status == action.status) return ActionResult(true, "Already marked.")
                calendar.setAttendance(subject.id, action.date, action.status)
                ActionResult(true, "Attendance marked.")
            }
        }
    }

    private fun missing(name: String) = ActionResult(false, "There is no subject called \"$name\". Add it first.")

    /** The subject called [name]: an exact match ignoring case, otherwise the only subject whose name contains it. */
    private suspend fun subject(name: String): SubjectEntity? {
        val all = database.subjectDao().observeAll().first()
        return all.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
            ?: all.filter { it.name.contains(name.trim(), ignoreCase = true) }.singleOrNull()
    }
}
