package skywa1ker404.absencium.ui.vault

import org.junit.Assert.assertEquals
import org.junit.Test
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
}
