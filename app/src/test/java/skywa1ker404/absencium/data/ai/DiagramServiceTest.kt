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

    @Test
    fun longDescriptionsAreShortenedForSearch() {
        val v = DiagramService.queryVariants("detailed cross-section of the human eyeball showing cornea, iris, lens, retina, and optic nerve")
        assertEquals("detailed cross-section of the human eyeball", v.first())
        assertEquals(true, v.contains("cross-section eyeball diagram"))
        assertEquals(true, v.size >= 3)
    }
}
