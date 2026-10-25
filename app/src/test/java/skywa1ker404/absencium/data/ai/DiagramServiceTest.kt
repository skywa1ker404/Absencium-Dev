package skywa1ker404.absencium.data.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class DiagramServiceTest {

    @Test
    fun diagramLinesAreFoundAndRemoved() {
        val text = "Intro\n{{diagram: heart}}\nMore\n{{diagram: heart}}\n{{diagram: lungs}}\nEnd"
        assertEquals(listOf("heart", "lungs"), NotePrompts.diagramRequests(text))
        assertEquals("Intro\nMore\nEnd", NotePrompts.withoutDiagramLines(text).replace("\n\n", "\n"))
    }
}
