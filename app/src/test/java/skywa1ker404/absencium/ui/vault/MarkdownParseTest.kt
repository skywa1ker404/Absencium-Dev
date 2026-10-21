package skywa1ker404.absencium.ui.vault

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import skywa1ker404.absencium.ui.vault.markdown.Block
import skywa1ker404.absencium.ui.vault.markdown.MarkdownParse
import skywa1ker404.absencium.ui.vault.markdown.NoteTags
import skywa1ker404.absencium.ui.vault.markdown.Span

class MarkdownParseTest {

    @Test fun blocksOfEachKind() {
        val b = MarkdownParse.blocks("# T\n\n- a\n1. b\n- [x] c\n> q\n---\n```\ncode\n```\nplain")
        assertTrue(b[0] is Block.Heading && (b[0] as Block.Heading).level == 1)
        assertTrue(b[1] is Block.Bullet)
        assertTrue(b[2] is Block.Numbered)
        assertTrue(b[3] is Block.Check && (b[3] as Block.Check).checked)
        assertTrue(b[4] is Block.Quote)
        assertTrue(b[5] is Block.Rule)
        assertEquals("code", (b[6] as Block.Code).text)
        assertTrue(b[7] is Block.Paragraph)
    }

    @Test fun checkBlockRemembersItsSourceLine() {
        val b = MarkdownParse.blocks("a\n\n- [ ] todo")
        assertEquals(2, (b.last() as Block.Check).line)
    }

    @Test fun unclosedFenceRunsToTheEnd() {
        val b = MarkdownParse.blocks("```\nx\ny")
        assertEquals("x\ny", (b.single() as Block.Code).text)
    }

    @Test fun inlineStyles() {
        val s = MarkdownParse.inline("a **b** *c* `d` ~~e~~ [[F]]")
        assertTrue(s.any { it.text == "b" && it.bold })
        assertTrue(s.any { it.text == "c" && it.italic })
        assertTrue(s.any { it.text == "d" && it.code })
        assertTrue(s.any { it.text == "e" && it.strike })
        assertTrue(s.any { it.text == "F" && it.link == "F" })
    }

    @Test fun unmatchedMarkersStayAsText() {
        assertEquals("a ** b", MarkdownParse.inline("a ** b").joinToString("") { it.text })
        assertEquals("2 * 3 * 4", MarkdownParse.inline("2 * 3 * 4").joinToString("") { it.text })
        assertEquals("[[open", MarkdownParse.inline("[[open").joinToString("") { it.text })
    }

    @Test fun noTextIsEverLost() {
        val samples = listOf("**bold *both* x**", "`a`b`", "~~", "[[a]]]]", "***", "éè 😀 **😀**")
        for (t in samples) {
            val plain = MarkdownParse.inline(t).joinToString("") { it.text }
            // Markers may be consumed as styling, but every non-marker character must survive.
            assertEquals(t.filter { it !in "*`~[]" }, plain.filter { it !in "*`~[]" })
        }
    }

    @Test fun longNoteWithUnmatchedMarkersIsFast() {
        val text = "**".repeat(25_000)
        val start = System.nanoTime()
        MarkdownParse.blocks(text)
        val ms = (System.nanoTime() - start) / 1_000_000
        assertTrue("took $ms ms", ms < 3000)
    }

    @Test fun longLinesOfOtherMarkersAreFastAndDoNotOverflow() {
        for (unit in listOf("x**", "a*b", "[[", "`a", "~~x", "- ", "1. ", "# ")) {
            val text = unit.repeat(20_000)
            val start = System.nanoTime()
            MarkdownParse.blocks(text)
            val ms = (System.nanoTime() - start) / 1_000_000
            assertTrue("'$unit' took $ms ms", ms < 3000)
        }
    }

    @Test fun ruleLinesAreRecognised() {
        for (line in listOf("---", "***", "___", "- - -", "  ***  ")) {
            assertTrue("'$line'", MarkdownParse.blocks(line).single() is Block.Rule)
        }
        assertTrue(MarkdownParse.blocks("--").single() is Block.Paragraph)
        assertTrue(MarkdownParse.blocks("-*-").single() !is Block.Rule)
    }

    @Test fun fiftyThousandCharactersParse() {
        val text = (1..2000).joinToString("\n") { "Line $it with **bold** and a [[Link $it]] and some more words here" }
        assertTrue(text.length > 50_000)
        assertEquals(2000, MarkdownParse.blocks(text.replace("\n", "\n\n")).size)
    }

    @Test fun tagsAreCleanedAndDeduplicated() {
        assertEquals(listOf("Maths", "exam"), NoteTags.parse("#Maths, maths ,exam,, "))
        assertEquals("a,b", NoteTags.join(listOf(" a ", "A", "b")))
        assertEquals(listOf("x"), NoteTags.add(emptyList(), "#x"))
        assertEquals(listOf("x"), NoteTags.add(listOf("x"), "X"))
        assertEquals(32, NoteTags.clean("a".repeat(100)).length)
    }

    @Test fun suggestionsPreferPrefixThenFrequency() {
        val all = listOf("math,exam", "maths", "exam,physics", "mathematics", "exam")
        assertEquals(listOf("exam", "math", "maths", "mathematics", "physics").first(), NoteTags.suggestions("", all, emptyList()).first())
        val m = NoteTags.suggestions("ma", all, emptyList())
        assertTrue(m.all { it.lowercase().contains("ma") })
        assertEquals(emptyList<String>(), NoteTags.suggestions("exam", all, listOf("exam")).filter { it == "exam" })
        assertEquals(Span("x"), Span("x"))
    }
}
