package skywa1ker404.absencium.data.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import skywa1ker404.absencium.data.db.EventEntity
import skywa1ker404.absencium.data.db.EventRepeat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class IcsImportTest {
    private val ist = ZoneId.of("Asia/Kolkata")

    private fun file(vararg events: String) = "BEGIN:VCALENDAR\r\nVERSION:2.0\r\n" + events.joinToString("") { "BEGIN:VEVENT\r\n$it\r\nEND:VEVENT\r\n" } + "END:VCALENDAR\r\n"

    @Test fun readsATimedEventWithLocationAndNotes() {
        val e = IcsImport.parse(file("SUMMARY:Maths test\r\nDTSTART:20261203T093000\r\nDTEND:20261203T110000\r\nLOCATION:Hall 2\r\nDESCRIPTION:Units 1\\, 2\\nBring a calculator"), ist).single()
        assertEquals("Maths test", e.title)
        assertEquals(LocalDate.of(2026, 12, 3), e.date)
        assertEquals(LocalTime.of(9, 30), e.start)
        assertEquals(LocalTime.of(11, 0), e.end)
        assertEquals("Location: Hall 2\nUnits 1, 2\nBring a calculator", e.notes)
        assertEquals(EventRepeat.NONE, e.repeat)
    }

    @Test fun anAllDayEventHasNoTimes() {
        val e = IcsImport.parse(file("SUMMARY:Sports day\r\nDTSTART;VALUE=DATE:20261205"), ist).single()
        assertEquals(LocalDate.of(2026, 12, 5), e.date)
        assertNull(e.start)
        assertNull(e.end)
    }

    @Test fun utcTimesMoveToThePhonesZone() {
        val e = IcsImport.parse(file("SUMMARY:Call\r\nDTSTART:20261203T040000Z"), ist).single()
        assertEquals(LocalTime.of(9, 30), e.start)
    }

    @Test fun utcTimeCanCrossMidnightIntoTheNextDay() {
        val e = IcsImport.parse(file("SUMMARY:Late\r\nDTSTART:20261203T200000Z"), ist).single()
        assertEquals(LocalDate.of(2026, 12, 4), e.date)
        assertEquals(LocalTime.of(1, 30), e.start)
    }

    @Test fun foldedLinesAreJoinedAndRepeatsAreMapped() {
        val e = IcsImport.parse(file("SUMMARY:A very long\r\n  title\r\nDTSTART:20261203T090000\r\nRRULE:FREQ=WEEKLY;BYDAY=TH"), ist).single()
        assertEquals("A very long title", e.title)
        assertEquals(EventRepeat.WEEKLY, e.repeat)
    }

    @Test fun anEndOnAnotherDayIsDropped() {
        val e = IcsImport.parse(file("SUMMARY:Night\r\nDTSTART:20261203T220000\r\nDTEND:20261204T060000"), ist).single()
        assertNull(e.end)
    }

    @Test fun badOrMissingStartsAreSkippedAndTitleDefaults() {
        val found = IcsImport.parse(file("SUMMARY:No start", "SUMMARY:Bad\r\nDTSTART:2026-12-03", "DTSTART:20261203T090000"), ist)
        assertEquals(listOf("Event"), found.map { it.title })
    }

    @Test fun duplicatesOfExistingEventsAreLeftOut() {
        val found = IcsImport.parse(file("SUMMARY:Maths test\r\nDTSTART:20261203T093000", "SUMMARY:Maths test\r\nDTSTART:20261203T093000", "SUMMARY:Viva\r\nDTSTART:20261204T100000"), ist)
        val existing = listOf(EventEntity(title = "maths TEST ", date = LocalDate.of(2026, 12, 3), startTime = LocalTime.of(9, 30)))
        assertEquals(listOf("Viva"), IcsImport.newEvents(found, existing).map { it.title })
        // Without existing events the repeated line inside the file is still added once.
        assertEquals(2, IcsImport.newEvents(found, emptyList()).size)
    }
}
