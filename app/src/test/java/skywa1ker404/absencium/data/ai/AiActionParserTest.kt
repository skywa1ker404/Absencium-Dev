package skywa1ker404.absencium.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import skywa1ker404.absencium.data.db.AttendanceStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class AiActionParserTest {

    @Test
    fun `the block is cut out of the answer and understood`() {
        val reply = """
            Here is a plan.

            ```absencium-actions
            [{"type":"add_exam","subject":"Physics","date":"2026-12-01","title":"Midterm","room":"B12"},
             {"type":"add_timetable","day":"mon","subject":"Physics","start":"9:00","end":"10:30 am"}]
            ```
        """.trimIndent()
        val parsed = AiActionParser.split(reply)
        assertEquals("Here is a plan.", parsed.text)
        assertEquals(2, parsed.actions.size)
        assertEquals(AiAction.AddExam("Physics", LocalDate.of(2026, 12, 1), "Midterm", "B12", null), parsed.actions[0])
        assertEquals(AiAction.AddSlot(DayOfWeek.MONDAY, "Physics", LocalTime.of(9, 0), LocalTime.of(10, 30), null), parsed.actions[1])
    }

    @Test
    fun `an answer without a block has no actions`() {
        val parsed = AiActionParser.split("Just an answer.")
        assertEquals("Just an answer.", parsed.text)
        assertNull(parsed.json)
        assertTrue(parsed.actions.isEmpty())
    }

    @Test
    fun `broken or unknown changes are dropped, not guessed`() {
        val actions = AiActionParser.parse(
            """[{"type":"add_exam","subject":"X","date":"tomorrow","title":"T"},{"type":"format_phone"},{"type":"set_attendance","subject":"X","date":"2026-01-02","status":"maybe"},{"type":"add_holiday","date":"2026-01-02","title":"Break"}]""",
        )
        assertEquals(listOf<AiAction>(AiAction.AddHoliday(LocalDate.of(2026, 1, 2), "Break")), actions)
        assertTrue(AiActionParser.parse("not json").isEmpty())
    }

    @Test
    fun `attendance status and an update without changes`() {
        val actions = AiActionParser.parse(
            """[{"type":"set_attendance","subject":"Math","date":"2026-02-03","status":"absent"},{"type":"update_exam","match":"Midterm"}]""",
        )
        assertEquals(listOf<AiAction>(AiAction.SetAttendance("Math", LocalDate.of(2026, 2, 3), AttendanceStatus.ABSENT)), actions)
    }

    @Test
    fun `times and days are read forgivingly`() {
        assertEquals(LocalTime.of(14, 30), AiActionParser.time("2:30 pm"))
        assertEquals(LocalTime.of(0, 5), AiActionParser.time("12:05 AM"))
        assertEquals(LocalTime.of(9, 0), AiActionParser.time("9"))
        assertNull(AiActionParser.time("25:00"))
        assertEquals(DayOfWeek.THURSDAY, AiActionParser.day("Thu"))
        assertNull(AiActionParser.day("t"))
    }

    @Test
    fun `a label on the first line or a json fence still counts`() {
        val first = AiActionParser.split("Done.\n```\nabsencium-actions\n[{\"type\":\"add_holiday\",\"date\":\"2026-01-02\",\"title\":\"Break\"}]\n```")
        assertEquals("Done.", first.text)
        assertEquals(1, first.actions.size)
        val json = AiActionParser.split("Here.\n```json\n[{\"type\":\"add_subject\",\"name\":\"Chemistry\"}]\n```")
        assertEquals("Here.", json.text)
        assertEquals(listOf<AiAction>(AiAction.AddSubject("Chemistry")), json.actions)
    }

    @Test
    fun `a bare list at the end and a cut off labelled block are handled`() {
        val bare = AiActionParser.split("Adding it.\n[{\"type\":\"add_subject\",\"name\":\"Maths\"}]")
        assertEquals("Adding it.", bare.text)
        assertEquals(1, bare.actions.size)
        val cut = AiActionParser.split("Plan.\n```absencium-actions\n[{\"type\":\"add_note\",\"subject\":\"Phy")
        assertEquals("Plan.", cut.text)
        assertTrue(cut.actions.isEmpty())
    }

    @Test
    fun `a note names its brief and length and memories never become cards`() {
        val parsed = AiActionParser.split(
            "Sure.\n```absencium-actions\n[{\"type\":\"add_note\",\"subject\":\"Physics\",\"title\":\"Optics\",\"brief\":\"All of optics\",\"length\":\"in depth\"},{\"type\":\"remember\",\"text\":\"Studies physics\"},{\"type\":\"set_syllabus_status\",\"subject\":\"Physics\",\"topic\":\"Lenses\",\"status\":\"completed\"}]\n```",
        )
        assertEquals(2, parsed.actions.size)
        assertEquals(AiAction.AddNote("Physics", "Optics", "", "All of optics", NoteLength.IN_DEPTH), parsed.actions[0])
        assertEquals(listOf(AiAction.Remember("Studies physics")), parsed.remembered)
    }
}
