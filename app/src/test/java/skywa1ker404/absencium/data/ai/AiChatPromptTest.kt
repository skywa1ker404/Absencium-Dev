package skywa1ker404.absencium.data.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import skywa1ker404.absencium.data.calendar.CalendarData
import skywa1ker404.absencium.data.db.NoteEntity
import skywa1ker404.absencium.data.db.SubjectEntity
import skywa1ker404.absencium.data.db.TimetableEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class AiChatPromptTest {
    private val today = LocalDate.of(2026, 10, 31)
    private val data = CalendarData(
        subjects = listOf(SubjectEntity(id = 1, name = "Physics")),
        timetable = listOf(TimetableEntity(dayOfWeek = DayOfWeek.MONDAY, subjectId = 1, startTime = LocalTime.of(9, 0), endTime = LocalTime.of(10, 0), roomNumber = "B12")),
    )
    private val notes = listOf(NoteEntity(subjectId = 1, title = "Secret title", bodyText = "private body text"))

    @Test fun withNothingAllowedTheAiSeesNoAppData() {
        val prompt = AiChatPrompt.build(emptySet(), data, notes, emptyList(), "hi", today)
        assertFalse(prompt.contains("Physics"))
        assertFalse(prompt.contains("absencium-actions holding"))
        assertTrue(prompt.contains("cannot see or change anything"))
    }

    @Test fun onlyAllowedPartsAreSharedAndNoteTextNeverIs() {
        val prompt = AiChatPrompt.build(setOf(AiScope.SUBJECTS, AiScope.TIMETABLE, AiScope.NOTES), data, notes, emptyList(), "hi", today)
        assertTrue(prompt.contains("MONDAY 09:00-10:00 Physics room B12"))
        assertTrue(prompt.contains("Secret title"))
        assertFalse(prompt.contains("private body text"))
        assertTrue(prompt.contains("add_timetable"))
        assertFalse(prompt.contains("add_exam"))
        assertFalse(prompt.contains("set_attendance"))
    }

    @Test fun theConversationAndTheNewQuestionCloseThePrompt() {
        val prompt = AiChatPrompt.build(emptySet(), data, notes, listOf(ChatRole.USER to "What is torque?", ChatRole.ASSISTANT to "A turning force."), "And power?", today)
        assertTrue(prompt.contains("Student: What is torque?"))
        assertTrue(prompt.contains("Assistant: A turning force."))
        assertTrue(prompt.trimEnd().endsWith("Student: And power?"))
    }
}
