package skywa1ker404.absencium.data.ai

import org.junit.Assert.assertEquals
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

    private fun ctx(allowed: Set<AiScope>) = ChatContext(allowed = allowed, calendar = data, notes = notes, today = today)

    @Test fun withNothingAllowedTheAiSeesNoAppDataButStaysGeneral() {
        val prompt = AiChatPrompt.system(ctx(emptySet()))
        assertFalse(prompt.contains("Physics"))
        assertFalse(prompt.contains("holding a JSON array"))
        assertTrue(prompt.contains("cannot see or change anything"))
        assertTrue(prompt.contains("versatile assistant"))
        assertFalse(prompt.contains("{color:"))
        assertTrue(prompt.contains("first sentence"))
    }

    @Test fun onlyAllowedPartsAreSharedAndNoteTextNeverIs() {
        val prompt = AiChatPrompt.system(ctx(setOf(AiScope.SUBJECTS, AiScope.TIMETABLE, AiScope.NOTES)))
        assertTrue(prompt.contains("MONDAY 09:00-10:00 Physics room B12"))
        assertTrue(prompt.contains("Secret title"))
        assertFalse(prompt.contains("private body text"))
        assertTrue(prompt.contains("add_timetable"))
        assertFalse(prompt.contains("add_exam"))
        assertFalse(prompt.contains("set_attendance"))
        assertTrue(prompt.contains("Today (2026-10-31)"))
    }

    @Test fun studyAndWebSearchAreOnlyMentionedWhenOn() {
        assertFalse(AiChatPrompt.system(ctx(emptySet())).contains("STUDY MODE"))
        assertTrue(AiChatPrompt.system(ctx(emptySet()).copy(study = true, webSearch = true)).let { it.contains("STUDY MODE") && it.contains("Web search is on") })
    }

    @Test fun aPlainGreetingKeepsTheInstructionSmall() {
        val all = AiScope.entries.toSet()
        val full = AiChatPrompt.system(ctx(all))
        val hello = AiChatPrompt.system(ctx(all).copy(topics = PromptTopics.of("hello")))
        assertFalse(hello.contains("APP DATA"))
        assertFalse(hello.contains("absencium-actions"))
        assertFalse(hello.contains("QUIZZES"))
        assertFalse(hello.contains("NOTES AND LONG TEXT"))
        assertTrue(hello.contains("HOW YOU TALK"))
        assertTrue(hello.length < full.length / 2)
    }

    @Test fun topicsAreAddedWhenTheMessagesTouchThem() {
        assertTrue(PromptTopics.of("Quiz me on cells").quiz)
        assertTrue(PromptTopics.of("when is my next exam").data)
        assertTrue(PromptTopics.of("make revision notes for optics").notes)
        assertFalse(PromptTopics.of("hello").data)
        assertFalse(PromptTopics.of("hi there").quiz)
    }

    @Test fun attendanceLinesSayHowManyCanBeMissedOrMustBeAttended() {
        assertTrue(AiChatPrompt.attendanceLine("Physics", 18, 2, 75).contains("can miss 4 more"))
        assertTrue(AiChatPrompt.attendanceLine("Maths", 6, 4, 75).contains("must attend the next 6 lectures"))
    }

    @Test fun turnsStartWithTheStudentAndAlternate() {
        val turns = ChatTurns.normalize(
            listOf(
                ChatTurn(ChatRole.ASSISTANT, "stray"), ChatTurn(ChatRole.USER, "a"), ChatTurn(ChatRole.USER, "b"),
                ChatTurn(ChatRole.ASSISTANT, " "), ChatTurn(ChatRole.ASSISTANT, "c"), ChatTurn(ChatRole.USER, "d"),
            ),
        )
        assertEquals(listOf(ChatRole.USER, ChatRole.ASSISTANT, ChatRole.USER), turns.map { it.role })
        assertEquals("a\n\nb", turns[0].text)
    }
}
