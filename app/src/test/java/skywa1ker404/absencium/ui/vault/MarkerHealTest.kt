package skywa1ker404.absencium.ui.vault

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import skywa1ker404.absencium.ui.vault.markdown.MarkdownEdit
import skywa1ker404.absencium.ui.vault.markdown.MarkerHeal

class MarkerHealTest {

    @Test fun plainDeletionsAreLeftAlone() {
        assertNull(MarkerHeal.heal("hello", "hell"))
        assertNull(MarkerHeal.heal("a **b** c", "a **b** "))
    }

    @Test fun backspaceInsideAHeadingMarkRemovesTheWholeMark() {
        val h = MarkerHeal.heal("## Title", "# Title")
        assertNotNull(h)
        assertEquals("Title", h!!.text)
        assertEquals(0, h.cursor)
    }

    @Test fun halfABoldPairTakesBothHalves() {
        val h = MarkerHeal.heal("a **bold** b", "a *bold** b")
        assertEquals("a bold b", h!!.text)
        val tail = MarkerHeal.heal("a **bold** b", "a **bold* b")
        assertEquals("a bold b", tail!!.text)
    }

    @Test fun halfAColourTagLeavesNoJunk() {
        val text = "x {color:E53935|red|} y"
        val damaged = text.removeRange(9, 10)
        assertEquals("x red y", MarkerHeal.heal(text, damaged)!!.text)
    }

    @Test fun halfACheckBoxPrefixIsRemovedWhole() {
        assertEquals("todo", MarkerHeal.heal("- [ ] todo", "- [ ]todo")!!.text)
    }

    @Test fun deletingAWholeMarkerOrWholeLineNeedsNoRepair() {
        assertNull(MarkerHeal.heal("## Title", "Title"))
        assertNull(MarkerHeal.heal("a\n## Title\nb", "a\nb"))
    }

    @Test fun aLineBreakAtTheEndOfStyledTextGoesAfterTheStyle() {
        val before = "a **bold** b"
        val typed = before.substring(0, 8) + "\n" + before.substring(8)
        val r = MarkerHeal.keepStylesWhole(before, typed)!!
        assertEquals("a **bold**\n b", r.text)
        assertEquals(11, r.cursor)
        val colour = "{color:E53935|red|}"
        val c = MarkerHeal.keepStylesWhole(colour, colour.substring(0, 17) + "\n" + colour.substring(17))!!
        assertEquals("{color:E53935|red|}\n", c.text)
    }

    @Test fun aLineBreakInTheMiddleOfStyledTextClosesAndReopensIt() {
        val before = "**ab**"
        val r = MarkerHeal.keepStylesWhole(before, "**a\nb**")!!
        assertEquals("**a**\n**b**", r.text)
        assertEquals(8, r.cursor)
    }

    @Test fun aLineBreakOutsideStylesIsNotTouched() {
        assertNull(MarkerHeal.keepStylesWhole("plain", "pla\nin"))
        assertNull(MarkerHeal.keepStylesWhole("a **b** c", "a **b** c\n"))
        assertNull(MarkerHeal.keepStylesWhole("ab", "abc"))
    }

    @Test fun checklistOnAnEmptyLineAddsABox() {
        val r = MarkdownEdit.checklist("", 0, 0)
        assertEquals("- [ ] ", r.text)
        assertEquals(6, r.start)
        assertEquals("a\n- [ ] ", MarkdownEdit.checklist("a\n", 2, 2).text)
        assertEquals("- ", MarkdownEdit.bullet("", 0, 0).text)
        assertEquals("1. ", MarkdownEdit.numbered("", 0, 0).text)
        assertEquals("", MarkdownEdit.checklist("- [ ] ", 6, 6).text)
    }
}
