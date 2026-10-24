package skywa1ker404.absencium.data.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportNamesTest {

    @Test fun unsafeCharactersAreRemoved() {
        assertEquals("a b c d", ExportNames.safeBase("a/b\\c:d"))
        assertEquals("What", ExportNames.safeBase("What?*\"<>|"))
        assertEquals("hidden", ExportNames.safeBase("...hidden..."))
    }

    @Test fun emptyOrBlankFallsBack() {
        assertEquals("Untitled note", ExportNames.safeBase(""))
        assertEquals("Untitled note", ExportNames.safeBase("  /// "))
        assertEquals("x", ExportNames.safeBase("", fallback = "x"))
    }

    @Test fun longNamesAreCutWithoutSplittingAnEmoji() {
        val name = "a".repeat(79) + "😀" + "tail"
        val out = ExportNames.safeBase(name)
        assertTrue(out.length <= 80)
        assertFalse(Character.isHighSurrogate(out.last()))
    }

    @Test fun fileNameAddsExtension() {
        assertEquals("Notes.pdf", ExportNames.fileName("Notes", "pdf"))
        assertEquals("Notes.md", ExportNames.fileName("Notes", ".md"))
    }

    @Test fun duplicateNamesGetNumbersInOrder() {
        assertEquals(
            listOf("a.md", "a (2).md", "B.md", "b (2).md", "a (3).md"),
            ExportNames.unique(listOf("a.md", "a.md", "B.md", "b.md", "a.md")),
        )
        assertEquals(listOf("x", "x (2)"), ExportNames.unique(listOf("x", "x")))
    }

    @Test fun markdownHasHeadingTagsAndBody() {
        assertEquals("# Title\n\n#maths #two-words\n\nbody\n", ExportNames.markdown(" Title ", "maths,two words", "body\n\n"))
        assertEquals("body\n", ExportNames.markdown("", "", "body"))
    }

    @Test fun pictureLinesBecomeMarkdownLinks() {
        val body = "before\n{{image:7|100|center}}\n{{doodle:8|50|start}}\n{{audio:9|100|center}}\nafter"
        val out = ExportNames.portableMarkdown(body) { kind, id -> "files/$kind$id.x" }
        assertEquals("before\n![Picture](files/image7.x)\n![Doodle](files/doodle8.x)\n[Recording](files/audio9.x)\nafter", out)
    }

    @Test fun missingFilesLeaveAShortNote() {
        val out = ExportNames.portableMarkdown("{{image:1|100|center}}") { _, _ -> null }
        assertEquals("*[Picture not included in this file. Export as ZIP to include it.]*", out)
    }

    @Test fun stylesWithoutMarkdownMarksBecomeHtml() {
        assertEquals("<span style=\"color:#E53935\">red</span> and <u>under</u> and <mark>hi</mark>", ExportNames.portableMarkdown("{color:E53935|red|} and ++under++ and ==hi==") { _, _ -> null })
        assertEquals("<p align=\"center\">mid</p>", ExportNames.portableMarkdown("{align:center}mid") { _, _ -> null })
        assertEquals("<span style=\"font-size:30px\">big</span> see other", ExportNames.portableMarkdown("{size:30|big|} see [[other]]") { _, _ -> null })
    }
}
