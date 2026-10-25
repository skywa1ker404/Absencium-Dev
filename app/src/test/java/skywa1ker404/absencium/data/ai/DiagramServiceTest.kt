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
        assertEquals(true, v.contains("cross-section human eyeball diagram"))
        assertEquals(true, v.size >= 3)
    }

    @Test
    fun foreignAndUnrelatedTitlesAreRejected() {
        val k = DiagramService.significantWords("female reproductive system diagram")
        assertEquals(false, DiagramService.suitable("Scheme female reproductive system-sr", k))
        assertEquals(false, DiagramService.suitable("Plutonium hexafluoride phase diagram-cs", k))
        assertEquals(false, DiagramService.suitable("Женская система", k))
        assertEquals(true, DiagramService.suitable("Female reproductive system", k))
    }

    @Test
    fun englishRelatedTitlesPass() {
        val k = DiagramService.significantWords("human kidney nephron structure")
        assertEquals(true, DiagramService.suitable("Nephron diagram", k))
        assertEquals(true, DiagramService.suitable("Kidney anatomy", k))
    }
}
