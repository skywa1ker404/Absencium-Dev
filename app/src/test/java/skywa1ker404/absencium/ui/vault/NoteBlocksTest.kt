package skywa1ker404.absencium.ui.vault

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import skywa1ker404.absencium.ui.vault.markdown.Align
import skywa1ker404.absencium.ui.vault.markdown.Block
import skywa1ker404.absencium.ui.vault.markdown.EmbedKind
import skywa1ker404.absencium.ui.vault.markdown.MarkdownEdit
import skywa1ker404.absencium.ui.vault.markdown.MarkdownParse
import skywa1ker404.absencium.ui.vault.markdown.NoteBlocks

class NoteBlocksTest {

    @Test fun tokenRoundTripsThroughTheParser() {
        val t = NoteBlocks.token(EmbedKind.IMAGE, 12, 60, Align.END)
        assertEquals("{{image:12|60|end}}", t)
        val b = MarkdownParse.blocks("before\n$t\nafter")
        assertEquals(3, b.size)
        val e = b[1] as Block.Embed
        assertEquals(EmbedKind.IMAGE, e.kind); assertEquals(12L, e.id); assertEquals(60, e.widthPercent); assertEquals(Align.END, e.align); assertEquals(1, e.line)
    }

    @Test fun embedDefaultsAndLimits() {
        val e = MarkdownParse.blocks("{{audio:7}}").single() as Block.Embed
        assertEquals(100, e.widthPercent); assertEquals(Align.CENTER, e.align)
        assertEquals(10, (MarkdownParse.blocks("{{doodle:1|3}}").single() as Block.Embed).widthPercent)
        assertEquals("{{image:1|100|center}}", NoteBlocks.token(EmbedKind.IMAGE, 1, 500))
        // Not an embed: stays text.
        assertTrue(MarkdownParse.blocks("{{video:1}}").single() is Block.Paragraph)
        assertTrue(MarkdownParse.blocks("{{image:x}}").single() is Block.Paragraph)
    }

    @Test fun insertSplitsTheLineAndMovesTheCursorOn() {
        val r = NoteBlocks.insertEmbed("hello world", 5, "{{image:1|100|center}}")
        assertEquals("hello\n{{image:1|100|center}}\n world", r.text)
        assertEquals(r.text.indexOf(" world"), r.start)
        val atEnd = NoteBlocks.insertEmbed("abc", 3, "{{audio:2|100|center}}")
        assertEquals("abc\n{{audio:2|100|center}}\n", atEnd.text)
        assertEquals(atEnd.text.length, atEnd.start)
        val empty = NoteBlocks.insertEmbed("", 0, "{{doodle:3|100|center}}")
        assertEquals("{{doodle:3|100|center}}\n", empty.text)
        assertEquals(NoteBlocks.insertEmbed("x", 99, "{{image:1}}").text, "x\n{{image:1}}\n")
    }

    @Test fun resizeMoveAndRemove() {
        val text = "one\n{{image:5|100|center}}\ntwo\n\nthree"
        assertEquals("one\n{{image:5|40|start}}\ntwo\n\nthree", NoteBlocks.resize(text, 1, 40, Align.START))
        assertEquals(text, NoteBlocks.resize(text, 0, 40, Align.START))
        val (down, line) = NoteBlocks.move(text, 1, +1)
        assertEquals("one\ntwo\n{{image:5|100|center}}\n\nthree", down); assertEquals(2, line)
        val (down2, line2) = NoteBlocks.move(down, 2, +1)
        assertEquals("one\ntwo\n\nthree\n{{image:5|100|center}}", down2); assertEquals(4, line2)
        assertEquals(down2 to 4, NoteBlocks.move(down2, 4, +1))
        val (up, upLine) = NoteBlocks.move(text, 1, -1)
        assertEquals("{{image:5|100|center}}\none\ntwo\n\nthree", up); assertEquals(0, upLine)
        assertEquals("one\ntwo\n\nthree", NoteBlocks.remove(text, 1))
        assertEquals(text, NoteBlocks.remove(text, 0))
        assertEquals(text, NoteBlocks.remove(text, 99))
    }

    @Test fun tablesParseWithAndWithoutHeader() {
        val t = MarkdownParse.blocks("| A | B |\n| --- | --- |\n| 1 | 2 |\ntext").first() as Block.Table
        assertTrue(t.hasHeader); assertEquals(2, t.rows.size); assertEquals("1", t.rows[1][0].single().text); assertEquals(0, t.startLine)
        val plain = MarkdownParse.blocks("intro\n\n| x | y |\n| 1 | 2 |") .last() as Block.Table
        assertTrue(!plain.hasHeader); assertEquals(2, plain.rows.size); assertEquals(2, plain.startLine)
    }

    @Test fun tableEditing() {
        val start = NoteBlocks.newTable(3, 2)
        assertEquals("| Column | Column |\n| --- | --- |\n|  |  |\n|  |  |", start)
        val t1 = NoteBlocks.setCell(start, 0, 2, 1, "a|b\nc")
        assertTrue(t1.contains("|  | a b c |"))
        val t2 = NoteBlocks.addRow(t1, 0, -1)
        assertEquals(5, t2.lines().size)
        assertEquals("| Column | Column |", t2.lines()[0]); assertEquals("| --- | --- |", t2.lines()[1])
        val t3 = NoteBlocks.addColumn(t2, 0, 0)
        assertTrue(t3.lines()[0] == "| Column |  | Column |")
        val t4 = NoteBlocks.deleteColumn(t3, 0, 1)
        assertEquals(t2, t4)
        // The header row and the last column or row are protected.
        assertEquals(t2, NoteBlocks.deleteRow(t2, 0, 0))
        assertEquals(4, NoteBlocks.deleteRow(t2, 0, 1).lines().size)
        val one = NoteBlocks.newTable(1, 1)
        assertEquals(one, NoteBlocks.deleteColumn(one, 0, 0)); assertEquals(one, NoteBlocks.deleteRow(one, 0, 0))
        // Text before and after the table is kept; a start line that is not a table changes nothing.
        val doc = "top\n$start\nbottom"
        assertEquals("top\n${NoteBlocks.addColumn(start, 0, 1)}\nbottom", NoteBlocks.addColumn(doc, 1, 1))
        assertEquals(doc, NoteBlocks.addColumn(doc, 0, 1))
    }

    @Test fun richInlineStyles() {
        val s = MarkdownParse.inline("a ++under++ ==mark== {color:FF0000|red|} {size:24|big|} **b**")
        assertTrue(s.any { it.text == "under" && it.underline })
        assertTrue(s.any { it.text == "mark" && it.highlight })
        assertTrue(s.any { it.text == "red" && it.color == 0xFF0000 })
        assertTrue(s.any { it.text == "big" && it.sizeSp == 24 })
        assertTrue(s.any { it.text == "b" && it.bold })
        // Nested styles combine; bad arguments stay as literal text.
        assertTrue(MarkdownParse.inline("{color:00FF00|**x**|}").single().let { it.bold && it.color == 0x00FF00 })
        assertEquals("{color:zzzzzz|x|}", MarkdownParse.inline("{color:zzzzzz|x|}").joinToString("") { it.text })
        assertEquals("{size:200|x|}", MarkdownParse.inline("{size:200|x|}").joinToString("") { it.text })
        assertEquals("a == b == c", MarkdownParse.inline("a == b == c").joinToString("") { it.text })
        assertEquals("++ x", MarkdownParse.inline("++ x").joinToString("") { it.text })
    }

    @Test fun alignmentAndLetters() {
        val b = MarkdownParse.blocks("{align:center}# Title\n{align:end}plain\nb) item\nc) next")
        assertEquals(Align.CENTER, (b[0] as Block.Heading).align)
        assertEquals(Align.END, (b[1] as Block.Paragraph).align)
        assertEquals("b", (b[2] as Block.Numbered).number)
        val e = MarkdownEdit.setAlign("one\ntwo", 0, 7, Align.CENTER)
        assertEquals("{align:center}one\n{align:center}two", e.text)
        assertEquals("one\n{align:end}two", MarkdownEdit.setAlign(e.text, 0, e.text.length, Align.END).text.replace("{align:end}one", "one"))
        assertEquals("one\ntwo", MarkdownEdit.setAlign(e.text, 0, e.text.length, Align.START).text)
        assertEquals("a) x\nb) y", MarkdownEdit.lettered("x\ny", 0, 3).text)
        assertEquals("x\ny", MarkdownEdit.lettered("a) x\nb) y", 0, 10).text)
    }

    @Test fun colourAndSizeWrapAndReplace() {
        val c = MarkdownEdit.setColor("hello", 0, 5, 0xFF0000)
        assertEquals("{color:FF0000|hello|}", c.text); assertEquals("hello", c.text.substring(c.start, c.end))
        val c2 = MarkdownEdit.setColor(c.text, c.start, c.end, 0x0000FF)
        assertEquals("{color:0000FF|hello|}", c2.text)
        assertEquals("hello", MarkdownEdit.setColor(c2.text, c2.start, c2.end, null).text)
        assertEquals("{size:30|hi|}", MarkdownEdit.setSize("hi", 0, 2, 30).text)
        assertEquals("{size:64|text|}", MarkdownEdit.setSize("", 0, 0, 999).text)
        assertEquals("hi", MarkdownEdit.setColor("hi", 0, 2, null).text)
        assertEquals("++x++", MarkdownEdit.toggleWrap("x", 0, 1, "++", "t").text)
        assertEquals("==x==", MarkdownEdit.toggleWrap("x", 0, 1, "==", "t").text)
    }

    @Test fun longInputStaysFast() {
        val long = "{color:FF0000|".repeat(20_000) + "++".repeat(20_000) + "==".repeat(20_000) + "|".repeat(20_000)
        val started = System.nanoTime()
        MarkdownParse.blocks(long)
        assertTrue("took ${(System.nanoTime() - started) / 1_000_000} ms", (System.nanoTime() - started) / 1_000_000 < 3000)
    }

    @Test fun aBlockCanBeDroppedBetweenTextLines() {
        val text = "one\n{{image:1|100|center}}\ntwo\nthree"
        // Drop the picture in front of line 3 ("three"): it ends up between "two" and "three" on line 2.
        val (down, at) = NoteBlocks.moveToLine(text, 1, 1, 4)
        assertEquals("one\ntwo\nthree\n{{image:1|100|center}}", down)
        assertEquals(3, at)
        val (up, atUp) = NoteBlocks.moveToLine(down, 3, 1, 0)
        assertEquals("{{image:1|100|center}}\none\ntwo\nthree", up)
        assertEquals(0, atUp)
        // Dropping where it already is changes nothing.
        assertEquals(text to 1, NoteBlocks.moveToLine(text, 1, 1, 1))
        assertEquals(text to 1, NoteBlocks.moveToLine(text, 1, 1, 2))
    }

    @Test fun aWholeTableMovesAsOneBlock() {
        val table = "| a | b |\n| --- | --- |\n| 1 | 2 |"
        val text = "top\n$table\nbottom"
        val count = NoteBlocks.tableLineCount(text, 1)
        assertEquals(3, count)
        val (moved, at) = NoteBlocks.moveToLine(text, 1, count, 5)
        assertEquals("top\nbottom\n$table", moved)
        assertEquals(2, at)
    }

    @Test fun tableRowsGrowAndOnlyEmptyRowsShrink() {
        val table = "| a | b |\n| --- | --- |\n| 1 | 2 |"
        val grown = NoteBlocks.setTableRows(table, 0, 4)
        assertEquals(5, NoteBlocks.tableLineCount(grown, 0))   // header, separator, three body rows
        // The two new rows are empty, so they can go again; the written row stays.
        assertEquals(table, NoteBlocks.setTableRows(grown, 0, 2))
        assertEquals(table, NoteBlocks.setTableRows(table, 0, 1))
        // Nothing changes when there is nothing to remove.
        assertEquals(table, NoteBlocks.setTableRows(table, 0, 2))
    }
}
