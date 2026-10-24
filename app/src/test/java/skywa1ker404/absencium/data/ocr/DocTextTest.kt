package skywa1ker404.absencium.data.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class DocTextTest {

    @Test fun paragraphsBecomeLinesAndTagsDisappear() {
        val xml = "<w:body><w:p><w:r><w:t>First &amp; second</w:t></w:r></w:p><w:p><w:r><w:t>Next</w:t></w:r><w:tab/><w:r><w:t>cell</w:t></w:r></w:p></w:body>"
        assertEquals("First & second\nNext\tcell", DocText.fromDocxXml(xml))
    }

    @Test fun manyEmptyParagraphsCollapse() {
        assertEquals("a\n\nb", DocText.fromDocxXml("<w:p>a</w:p><w:p></w:p><w:p></w:p><w:p></w:p><w:p>b</w:p>"))
    }

    @Test fun fileKindsFollowMimeThenName() {
        assertEquals(FileKind.IMAGE, FileKind.of("image/png", "x"))
        assertEquals(FileKind.PDF, FileKind.of("", "Notes.PDF"))
        assertEquals(FileKind.AUDIO, FileKind.of("audio/mp4", "Recording.m4a"))
        assertEquals(FileKind.WORD, FileKind.of("", "essay.docx"))
        assertEquals(FileKind.PLAIN, FileKind.of("text/markdown", "n"))
        assertEquals(FileKind.OTHER, FileKind.of("application/zip", "a.zip"))
    }
}
