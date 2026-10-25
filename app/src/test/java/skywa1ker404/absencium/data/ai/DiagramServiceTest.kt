package skywa1ker404.absencium.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DiagramServiceTest {

    private val reply = """
        {"query":{"pages":{
          "11":{"title":"File:Heart.svg.png","index":2,"imageinfo":[{"mime":"image/svg+xml","thumburl":"https://x/a.png"}]},
          "12":{"title":"File:Human heart diagram.jpg","index":1,"imageinfo":[{"mime":"image/jpeg","thumburl":"https://x/b.jpg",
            "extmetadata":{"LicenseShortName":{"value":"CC BY-SA 4.0"},"Artist":{"value":"<a href=\"u\">Jane Doe</a>"}}}]}
        }}}
    """.trimIndent()

    @Test
    fun picksTheBestRankedJpegOrPngAndCreditsIt() {
        val pick = DiagramService.pickBest(reply)
        assertNotNull(pick)
        assertEquals("https://x/b.jpg", pick!!.imageUrl)
        assertEquals("Picture: Human heart diagram, Jane Doe, CC BY-SA 4.0 (Wikimedia Commons)", pick.caption)
    }

    @Test
    fun noResultsGivesNull() {
        assertNull(DiagramService.pickBest("""{"batchcomplete":""}"""))
    }

    @Test
    fun diagramLinesAreFoundAndRemoved() {
        val text = "Intro\n{{diagram: heart}}\nMore\n{{diagram: heart}}\n{{diagram: lungs}}\nEnd"
        assertEquals(listOf("heart", "lungs"), NotePrompts.diagramRequests(text))
        assertEquals("Intro\nMore\nEnd", NotePrompts.withoutDiagramLines(text).replace("\n\n", "\n"))
    }
}
