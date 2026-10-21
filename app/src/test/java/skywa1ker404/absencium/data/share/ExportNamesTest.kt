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
}
