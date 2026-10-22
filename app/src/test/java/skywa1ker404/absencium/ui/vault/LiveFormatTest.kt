package skywa1ker404.absencium.ui.vault

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import skywa1ker404.absencium.ui.vault.markdown.EditKind
import skywa1ker404.absencium.ui.vault.markdown.LiveFormat
import skywa1ker404.absencium.ui.vault.markdown.NoteDoc
import skywa1ker404.absencium.ui.vault.markdown.PartKind

class LiveFormatTest {

    @Test fun markersAreHiddenAndTextIsStyled() {
        val p = LiveFormat.plan("a **bold** b")
        assertEquals("a bold b", p.visible)
        val bold = p.styles.single { it.kind == EditKind.BOLD }
        assertEquals("bold", p.visible.substring(bold.start, bold.end))
    }

    @Test fun listsQuotesChecksAndRulesBecomeWhatTheyMean() {
        assertEquals("• item", LiveFormat.plan("- item").visible)
        assertEquals("☐ todo", LiveFormat.plan("- [ ] todo").visible)
        assertEquals("☑ done", LiveFormat.plan("- [x] done").visible)
        assertEquals("┃ said", LiveFormat.plan("> said").visible)
        assertEquals("Title", LiveFormat.plan("## Title").visible)
        assertEquals("1. one", LiveFormat.plan("1. one").visible)
        assertTrue(LiveFormat.plan("---").visible.startsWith("───"))
        assertEquals("one\n• two\nthree", LiveFormat.plan("one\n- two\nthree").visible)
    }

    @Test fun positionsMapBothWays() {
        val text = "a **bold** b"
        val p = LiveFormat.plan(text)
        // Before and after the whole word, and inside it.
        assertEquals(2, p.toVisible(2))
        assertEquals(2, p.toVisible(4))      // after the opening ** it is still at the start of "bold"
        assertEquals(6, p.toVisible(10))     // after the closing **
        assertEquals(4, p.toVisible(6))      // inside the word: "bo|ld"
        assertEquals(6, p.toVisible(8))      // the end of "bold", before the closing **
        // Back again: typing at the end of the bold word continues the bold word (inside the markers).
        assertEquals(8, p.toOriginal(6))
        for (v in 0..p.visible.length) {
            val o = p.toOriginal(v)
            assertTrue("original $o in range", o in 0..text.length)
            assertEquals("round trip of visible $v", v, p.toVisible(o))
        }
    }

    @Test fun cursorAtTheStartOfAHeadingLandsAfterTheHiddenMark() {
        val p = LiveFormat.plan("## Title")
        assertEquals(3, p.toOriginal(0))
        assertEquals(0, p.toVisible(0))
        assertEquals(0, p.toVisible(3))
        val tagged = LiveFormat.plan("{align:center}## Title")
        assertEquals(17, tagged.toOriginal(0))
        // A line that is not a heading is not moved.
        assertEquals(0, LiveFormat.plan("Title").toOriginal(0))
        val second = LiveFormat.plan("one\n## Two")
        assertEquals(7, second.toOriginal(4))
        assertEquals(3, second.toOriginal(3))
    }

    @Test fun everyShapeOfInputGivesAConsistentPlan() {
        val inputs = listOf("", "\n", "---\n---", "- [ ] \n> \n# ", "**", "{color:FF0000|x|}", "```\ncode\n```", "{align:end}\n", "***")
        for (t in inputs) {
            val p = LiveFormat.plan(t)
            for (v in 0..p.visible.length) {
                val o = p.toOriginal(v)
                assertTrue("[$t] original $o", o in 0..t.length)
            }
            for (o in 0..t.length) assertTrue("[$t] visible ${p.toVisible(o)}", p.toVisible(o) in 0..p.visible.length)
            assertTrue(p.styles.all { it.start in 0..p.visible.length && it.end in it.start..p.visible.length })
        }
    }

    @Test fun manyDividersAreFast() {
        val t = "---\n".repeat(20_000)
        val started = System.nanoTime()
        val p = LiveFormat.plan(t)
        val ms = (System.nanoTime() - started) / 1_000_000
        assertTrue("took $ms ms", ms < 4000)
        assertTrue(p.visible.length > t.length)
    }

    @Test fun noteIsCutIntoPartsAndPutBackTogether() {
        val text = "intro\nmore\n{{image:5|100|center}}\nafter\n| a | b |\n| --- | --- |\n| 1 | 2 |\nend"
        val parts = NoteDoc.split(text)
        assertEquals(listOf(PartKind.TEXT, PartKind.EMBED, PartKind.TEXT, PartKind.TABLE, PartKind.TEXT), parts.map { it.kind })
        assertEquals("intro\nmore", parts[0].text)
        assertEquals("{{image:5|100|center}}", parts[1].text)
        assertEquals(2, parts[1].line)
        assertEquals("after", parts[2].text)
        assertEquals("| a | b |\n| --- | --- |\n| 1 | 2 |", parts[3].text)
        assertEquals(4, parts[3].line)
        assertEquals("end", parts[4].text)
        for (part in parts) assertEquals(part.text, text.substring(part.start, part.end))
        assertEquals(text.replace("after", "AFTER"), NoteDoc.replacePart(text, parts, 2, "AFTER"))
        assertEquals(2, NoteDoc.partIndexAt(parts, parts[2].start + 2))
        assertEquals(parts[2].start + 3, NoteDoc.globalOffset(parts, 2, 3))
    }

    @Test fun aLastBlockGetsATextPartBelowIt() {
        val text = "a\n{{doodle:2}}"
        val parts = NoteDoc.split(text)
        assertEquals(PartKind.TEXT, parts.last().kind)
        assertTrue(parts.last().synthetic)
        assertEquals(text + "\nhello", NoteDoc.replacePart(text, parts, parts.lastIndex, "hello"))
        assertEquals(text, NoteDoc.replacePart(text, parts, parts.lastIndex, ""))
        assertEquals(text.length + 1 + 2, NoteDoc.globalOffset(parts, parts.lastIndex, 2))
        // With a line break after the block the empty line is a real part.
        val broken = NoteDoc.split("a\n{{doodle:2}}\n")
        assertTrue(!broken.last().synthetic && broken.last().kind == PartKind.TEXT)
    }

    @Test fun emptyAndBlockOnlyNotes() {
        assertEquals(1, NoteDoc.split("").size)
        val two = NoteDoc.split("{{image:1}}\n{{image:2}}")
        assertEquals(listOf(PartKind.EMBED, PartKind.EMBED, PartKind.TEXT), two.map { it.kind })
        assertEquals(2, NoteDoc.nearestText(two, 0))
        assertEquals(0, NoteDoc.nearestText(NoteDoc.split("x"), 5))
    }
}
