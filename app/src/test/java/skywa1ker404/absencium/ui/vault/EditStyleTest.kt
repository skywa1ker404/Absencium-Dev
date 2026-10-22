package skywa1ker404.absencium.ui.vault

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import skywa1ker404.absencium.ui.vault.markdown.EditHistory
import skywa1ker404.absencium.ui.vault.markdown.EditKind
import skywa1ker404.absencium.ui.vault.markdown.EditStyle
import skywa1ker404.absencium.ui.vault.markdown.Edited

class EditStyleTest {
    private fun kinds(text: String, kind: EditKind) = EditStyle.ranges(text).filter { it.kind == kind }.map { text.substring(it.start, it.end) }

    @Test fun boldItalicAndMarkers() {
        val t = "a **b** *i* z"
        assertEquals(listOf("b"), kinds(t, EditKind.BOLD))
        assertEquals(listOf("i"), kinds(t, EditKind.ITALIC))
        assertEquals(4, kinds(t, EditKind.MARKER).size)
    }

    @Test fun headingListAndAlignPrefixes() {
        val h = "## Title"
        assertEquals(listOf("## "), kinds(h, EditKind.MARKER))
        assertEquals(listOf("Title"), kinds(h, EditKind.HEADING))
        assertEquals(2, EditStyle.ranges(h).first { it.kind == EditKind.HEADING }.arg)
        assertEquals(listOf("- [ ] "), kinds("- [ ] todo", EditKind.MARKER))
        assertEquals(listOf("{align:center}", "- "), kinds("{align:center}- x", EditKind.MARKER))
        assertEquals(listOf("b) "), kinds("b) item", EditKind.MARKER))
    }

    @Test fun colourSizeUnderlineHighlightAndLinks() {
        val t = "{color:FF0000|red|} {size:24|big|} ++u++ ==h== [[note]]"
        assertEquals(listOf("red"), kinds(t, EditKind.COLOR))
        assertEquals(0xFF0000, EditStyle.ranges(t).first { it.kind == EditKind.COLOR }.arg)
        assertEquals(listOf("big"), kinds(t, EditKind.SIZE))
        assertEquals(listOf("u"), kinds(t, EditKind.UNDERLINE))
        assertEquals(listOf("h"), kinds(t, EditKind.HIGHLIGHT))
        assertEquals(listOf("note"), kinds(t, EditKind.LINK))
    }

    @Test fun nestedStylesAndUnmatchedMarkers() {
        assertEquals(listOf("x"), kinds("{color:00FF00|**x**|}", EditKind.BOLD))
        assertTrue(kinds("an **unmatched marker", EditKind.BOLD).isEmpty())
        assertTrue(kinds("a == b == c", EditKind.HIGHLIGHT).isEmpty())
        assertTrue(kinds("{color:zzz|x|}", EditKind.COLOR).isEmpty())
    }

    @Test fun codeFencesAndEmbedLines() {
        val t = "```\n**not bold**\n```\n{{image:1|100|center}}\n**yes**"
        assertEquals(listOf("yes"), kinds(t, EditKind.BOLD))
        assertEquals(listOf("**not bold**"), kinds(t, EditKind.CODE))
        assertTrue(kinds(t, EditKind.MARKER).contains("{{image:1|100|center}}"))
    }

    @Test fun rangesStayInsideTheTextAndAreFastOnLongInput() {
        val long = "**".repeat(30_000) + "\n" + "{color:FF0000|".repeat(10_000) + "\n" + "*a ".repeat(30_000)
        val started = System.nanoTime()
        val r = EditStyle.ranges(long)
        val ms = (System.nanoTime() - started) / 1_000_000
        assertTrue("took $ms ms", ms < 4000)
        assertTrue(r.all { it.start >= 0 && it.end <= long.length && it.start <= it.end })
        assertTrue(EditStyle.ranges("").isEmpty())
    }

    @Test fun historyMergesBurstsAndRedoes() {
        val h = EditHistory(mergeMs = 800)
        assertFalse(h.canUndo)
        h.record(Edited("", 0, 0), now = 0)
        h.record(Edited("a", 1, 1), now = 100) // same burst: not a new step
        h.record(Edited("ab", 2, 2), now = 2000)
        val u1 = h.undo(Edited("abc", 3, 3))!!
        assertEquals("ab", u1.text)
        val u2 = h.undo(u1)!!
        assertEquals("", u2.text)
        assertNull(h.undo(u2))
        assertEquals("ab", h.redo(u2)!!.text)
        assertEquals("abc", h.redo(Edited("ab", 2, 2))!!.text)
        assertNull(h.redo(Edited("abc", 3, 3)))
        h.record(Edited("abc", 3, 3), now = 10_000)
        assertFalse(h.canRedo)
    }

    @Test fun historyForceAndLimit() {
        val h = EditHistory(max = 3, mergeMs = 10_000)
        h.record(Edited("1", 0, 0), now = 0)
        h.record(Edited("2", 0, 0), now = 1, force = true)
        h.record(Edited("3", 0, 0), now = 2, force = true)
        h.record(Edited("4", 0, 0), now = 3, force = true)
        assertEquals("4", h.undo(Edited("5", 0, 0))!!.text)
        assertEquals("3", h.undo(Edited("4", 0, 0))!!.text)
        assertEquals("2", h.undo(Edited("3", 0, 0))!!.text)
        assertNull(h.undo(Edited("2", 0, 0)))
    }
}
