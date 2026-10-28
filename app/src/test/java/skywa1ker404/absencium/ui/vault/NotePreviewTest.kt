package skywa1ker404.absencium.ui.vault

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotePreviewTest {
    @Test
    fun readsLinesAsPlainText() {
        val lines = notePreviewLines("# Title\n**bold** text\n- [ ] one\n- [x] two")
        assertEquals(listOf("Title", "bold text", "☐ one", "☑ two"), lines)
    }

    @Test
    fun styleMarkersNeverShow() {
        val lines = notePreviewLines("==~~++{size:30|usiiwiwijw|}++~~==")
        assertTrue(lines.isNotEmpty())
        assertTrue(lines.none { it.contains("{size") || it.contains("==") || it.contains("|}") })
    }

    @Test
    fun emptyAndLongBodiesAreSafe() {
        assertTrue(notePreviewLines("").isEmpty())
        assertEquals(4, notePreviewLines((1..50).joinToString("\n\n") { "line $it" }).size)
    }
}
