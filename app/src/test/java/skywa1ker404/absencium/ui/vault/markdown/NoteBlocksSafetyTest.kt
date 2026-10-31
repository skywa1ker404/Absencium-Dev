package skywa1ker404.absencium.ui.vault.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteBlocksSafetyTest {

    @Test fun aBlockNeverLandsInsideAPictureLine() {
        val text = "intro\n{{image:1|76|center}}\nafter"
        val inside = text.indexOf("cente") + 2
        val out = NoteBlocks.insertEmbed(text, inside, NoteBlocks.newTable(2, 2))
        assertTrue(out.text.contains("\n{{image:1|76|center}}\n"))
        assertTrue(out.text.indexOf("| Column") > out.text.indexOf("{{image:1|76|center}}"))
    }

    @Test fun aBlockInsideBoldGoesAfterTheLineNotThroughIt() {
        val text = "a **bold text** b"
        val out = NoteBlocks.insertEmbed(text, text.indexOf("bold") + 2, "{{image:2|100|center}}")
        assertEquals("a **bold text** b\n{{image:2|100|center}}\n", out.text)
    }

    @Test fun aBlockInsideATableGoesAfterTheTable() {
        val table = NoteBlocks.newTable(2, 2)
        val text = "$table\nend"
        val out = NoteBlocks.insertEmbed(text, table.indexOf("Column") + 2, "{{image:3|100|center}}")
        assertTrue(out.text.startsWith(table + "\n{{image:3|100|center}}\n"))
    }

    @Test fun aBlockInPlainTextStillSplitsTheLine() {
        val out = NoteBlocks.insertEmbed("hello world", 5, "{{image:4|100|center}}")
        assertEquals("hello\n{{image:4|100|center}}\n world", out.text)
    }

    @Test fun aCutPictureLineIsJoinedAgain() {
        val broken = "{{image:1|76|cente\n| Column | Column |\n| --- | --- |\nr}}"
        assertEquals("| Column | Column |\n| --- | --- |\n{{image:1|76|center}}", NoteBlocks.repair(broken))
        assertEquals("plain {{ text", NoteBlocks.repair("plain {{ text"))
    }
}
