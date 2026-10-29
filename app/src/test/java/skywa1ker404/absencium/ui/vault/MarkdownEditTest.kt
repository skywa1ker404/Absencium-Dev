package skywa1ker404.absencium.ui.vault

import org.junit.Assert.assertEquals
import org.junit.Test
import skywa1ker404.absencium.ui.vault.markdown.Align
import skywa1ker404.absencium.ui.vault.markdown.MarkdownEdit

class MarkdownEditTest {

    @Test fun boldWrapsSelection() {
        val r = MarkdownEdit.toggleWrap("hello world", 6, 11, "**", "bold")
        assertEquals("hello **world**", r.text)
        assertEquals("world", r.text.substring(r.start, r.end))
    }

    @Test fun boldOnCursorInsertsPlaceholderSelected() {
        val r = MarkdownEdit.toggleWrap("ab", 1, 1, "**", "bold")
        assertEquals("a**bold**b", r.text)
        assertEquals("bold", r.text.substring(r.start, r.end))
    }

    @Test fun boldTwiceRemovesIt() {
        val once = MarkdownEdit.toggleWrap("hello world", 6, 11, "**", "bold")
        val twice = MarkdownEdit.toggleWrap(once.text, once.start, once.end, "**", "bold")
        assertEquals("hello world", twice.text)
        assertEquals("world", twice.text.substring(twice.start, twice.end))
    }

    @Test fun selectionContainingMarkersIsUnwrapped() {
        val r = MarkdownEdit.toggleWrap("a **b** c", 2, 7, "**", "bold")
        assertEquals("a b c", r.text)
    }

    @Test fun outOfRangeSelectionIsClamped() {
        val r = MarkdownEdit.toggleWrap("abc", 10, -4, "*", "x")
        assertEquals("*abc*", r.text)
    }

    @Test fun headingAppliesAndToggles() {
        val h = MarkdownEdit.heading("Title\nbody", 2, 2, 2)
        assertEquals("## Title\nbody", h.text)
        assertEquals("Title\nbody", MarkdownEdit.heading(h.text, h.start, h.end, 2).text)
        // A different level replaces the old one.
        assertEquals("### Title\nbody", MarkdownEdit.heading(h.text, h.start, h.end, 3).text)
    }

    @Test fun bulletsApplyToEveryTouchedLineAndToggleOff() {
        val text = "one\ntwo\nthree"
        val on = MarkdownEdit.bullet(text, 0, 7)
        assertEquals("- one\n- two\nthree", on.text)
        assertEquals(text, MarkdownEdit.bullet(on.text, on.start, on.end).text)
    }

    @Test fun selectionEndingAtLineStartDoesNotTouchNextLine() {
        val r = MarkdownEdit.bullet("one\ntwo", 0, 4)
        assertEquals("- one\ntwo", r.text)
    }

    @Test fun numberedCountsFromOneAndSkipsBlankLines() {
        val r = MarkdownEdit.numbered("a\n\nb", 0, 4)
        assertEquals("1. a\n\n2. b", r.text)
    }

    @Test fun switchingListKindsDoesNotStack() {
        val bullets = MarkdownEdit.bullet("x", 0, 1)
        assertEquals("- [ ] x", MarkdownEdit.checklist(bullets.text, bullets.start, bullets.end).text)
        val checks = MarkdownEdit.checklist("x", 0, 1)
        assertEquals("1. x", MarkdownEdit.numbered(checks.text, checks.start, checks.end).text)
    }

    @Test fun quoteToggles() {
        val q = MarkdownEdit.quote("hi", 0, 2)
        assertEquals("> hi", q.text)
        assertEquals("hi", MarkdownEdit.quote(q.text, q.start, q.end).text)
    }

    @Test fun cursorStaysOnItsLine() {
        val r = MarkdownEdit.bullet("a\nbcd", 3, 3)
        assertEquals("a\n- bcd", r.text)
        assertEquals(r.start, r.end)
        assertEquals('c', r.text[r.start])
    }

    @Test fun codeBlockAtCursorAndOverSelection() {
        val r = MarkdownEdit.codeBlock("x = 1", 0, 5)
        assertEquals("```\nx = 1\n```\n", r.text)
        assertEquals("x = 1", r.text.substring(r.start, r.end))
        assertEquals("a\n```\ncode\n```\n", MarkdownEdit.codeBlock("a", 1, 1).text)
    }

    @Test fun checkboxToggleOnlyChangesCheckLines() {
        val t = "- [ ] a\n- [x] b\nplain"
        assertEquals("- [x] a\n- [x] b\nplain", MarkdownEdit.toggleCheckboxLine(t, 0))
        assertEquals("- [ ] a\n- [ ] b\nplain", MarkdownEdit.toggleCheckboxLine(t, 1))
        assertEquals(t, MarkdownEdit.toggleCheckboxLine(t, 2))
        assertEquals(t, MarkdownEdit.toggleCheckboxLine(t, 99))
    }

    @Test fun insertReplacesSelection() {
        val r = MarkdownEdit.insert("abcd", 1, 3, "[[X]]")
        assertEquals("a[[X]]d", r.text)
        assertEquals(6, r.start)
    }

    @Test fun boldOffUnderAnotherStyleRemovesTheBoldNotAddsMore() {
        val r = MarkdownEdit.toggleWrap("**~~hi~~**", 4, 6, "**", "bold")
        assertEquals("~~hi~~", r.text)
    }

    @Test fun selectionStartingInFrontOfAnOpeningMarkerStillToggles() {
        // What the live editor reports when the shown word "bar" is selected: from before the hidden ** to before the closing **.
        val off = MarkdownEdit.toggleWrap("foo **bar** baz", 4, 9, "**", "bold")
        assertEquals("foo bar baz", off.text)
        assertEquals("bar", off.text.substring(off.start, off.end))
    }

    @Test fun halfCoveredPairIsWidenedInsteadOfBroken() {
        val r = MarkdownEdit.toggleWrap("foo **bar** baz", 0, 9, "*", "i")
        assertEquals("*foo **bar*** baz", r.text)
    }

    @Test fun partOfABoldRunSplitsIt() {
        val r = MarkdownEdit.toggleWrap("**abcd**", 4, 6, "**", "bold")
        assertEquals("**ab**cd", r.text)
        assertEquals("cd", r.text.substring(r.start, r.end))
    }

    @Test fun boldInsideBoldDoesNotNest() {
        val r = MarkdownEdit.toggleWrap("a **b** c", 0, 9, "**", "bold")
        assertEquals("**a b c**", r.text)
    }

    @Test fun cursorInsideAStyledRunRemovesTheStyle() {
        assertEquals("ab", MarkdownEdit.toggleWrap("**ab**", 3, 3, "**", "bold").text)
    }

    @Test fun codeOverStyledTextDropsTheMarkersInsideIt() {
        assertEquals("`a b c`", MarkdownEdit.toggleWrap("a **b** c", 0, 9, "`", "code").text)
    }

    @Test fun boldItalicLosesOnlyTheBoldPart() {
        assertEquals("*hi*", MarkdownEdit.toggleWrap("***hi***", 3, 5, "**", "bold").text)
    }

    @Test fun sizeAppliedTwiceReplacesTheFirst() {
        val a = MarkdownEdit.setSize("hi", 0, 2, 12)
        assertEquals("{size:12|hi|}", a.text)
        val b = MarkdownEdit.setSize(a.text, a.start, a.end, 20)
        assertEquals("{size:20|hi|}", b.text)
        assertEquals("hi", b.text.substring(b.start, b.end))
    }

    @Test fun sizeReplacedThroughOtherStyles() {
        val a = MarkdownEdit.setSize("**hi**", 2, 4, 12)
        assertEquals("**{size:12|hi|}**", a.text)
        val b = MarkdownEdit.setSize(a.text, a.start, a.end, 20)
        assertEquals("**{size:20|hi|}**", b.text)
        assertEquals("**hi**", MarkdownEdit.setSize(b.text, b.start, b.end, null).text)
    }

    @Test fun colourOnAColouredWordReplacesIt() {
        val a = MarkdownEdit.setColor("hi", 0, 2, 0xFF0000)
        val b = MarkdownEdit.setColor(a.text, a.start, a.end, 0x00FF00)
        assertEquals("{color:00FF00|hi|}", b.text)
    }

    @Test fun listKindsReplaceEachOtherInsteadOfStacking() {
        var e = MarkdownEdit.checklist("x", 0, 1)
        e = MarkdownEdit.lettered(e.text, e.start, e.end)
        assertEquals("a) x", e.text)
        e = MarkdownEdit.quote(e.text, e.start, e.end)
        assertEquals("> x", e.text)
        e = MarkdownEdit.lettered(e.text, e.start, e.end)
        e = MarkdownEdit.bullet(e.text, e.start, e.end)
        assertEquals("- x", e.text)
    }

    @Test fun anAlreadyStackedLineIsCleanedUp() {
        assertEquals("- x", MarkdownEdit.bullet("- [ ] a) > a) x", 0, 0).text)
    }

    @Test fun alignmentTagStaysInFrontOfTheListMarker() {
        val c = MarkdownEdit.setAlign("x", 0, 1, Align.CENTER)
        assertEquals("{align:center}- x", MarkdownEdit.bullet(c.text, c.start, c.end).text)
    }

    @Test fun headingKeepsNoStackedPrefix() {
        assertEquals("## x", MarkdownEdit.heading("> - x", 0, 5, 2).text)
    }

    @Test fun colourOnAWholeHeadingLineGoesAfterTheHeadingMark() {
        val r = MarkdownEdit.setColor("## Title", 0, 8, 0x43A047)
        assertEquals("## {color:43A047|Title|}", r.text)
    }

    @Test fun boldOnAWholeBulletLineGoesAfterTheMarker() {
        assertEquals("- **item**", MarkdownEdit.toggleWrap("- item", 0, 6, "**", "bold").text)
    }

    @Test fun aCursorInsideTheHeadingMarkIsMovedToTheWords() {
        assertEquals("# **bold**", MarkdownEdit.toggleWrap("# ", 0, 0, "**", "bold").text)
    }

    @Test fun aStyleOverTwoLinesIsAPairOnEachLine() {
        val r = MarkdownEdit.toggleWrap("one\ntwo", 0, 7, "**", "bold")
        assertEquals("**one**\n**two**", r.text)
        assertEquals("one\ntwo", r.text.substring(r.start, r.end).replace("**", ""))
    }

    @Test fun theSameStyleOverTwoStyledLinesTakesItOffBoth() {
        assertEquals("one\ntwo", MarkdownEdit.toggleWrap("**one**\n**two**", 0, 15, "**", "bold").text)
    }

    @Test fun aMixedSelectionStylesOnlyTheLinesThatLackIt() {
        assertEquals("**one**\n**two**", MarkdownEdit.toggleWrap("**one**\ntwo", 0, 11, "**", "bold").text)
    }
}
