package skywa1ker404.absencium.data.ai

import org.junit.Assert.assertEquals
import org.junit.Test
import skywa1ker404.absencium.data.db.SyllabusNodeEntity

class SyllabusPathTest {

    private val nodes = listOf(
        SyllabusNodeEntity(id = 1, subjectId = 1, title = "Waves"),
        SyllabusNodeEntity(id = 2, subjectId = 1, parentId = 1, title = "Interference", details = "Young's double slit"),
        SyllabusNodeEntity(id = 3, subjectId = 1, parentId = 2, title = "Fringe width"),
        SyllabusNodeEntity(id = 4, subjectId = 1, title = "Optics"),
    )

    @Test
    fun choicesAreDepthFirstWithTheirPath() {
        assertEquals(
            listOf("Waves", "Waves › Interference", "Waves › Interference › Fringe width", "Optics"),
            SyllabusPath.choices(nodes).map { it.label },
        )
        assertEquals(listOf(0, 1, 2, 0), SyllabusPath.choices(nodes).map { it.depth })
    }

    @Test
    fun contextNamesUnitChapterAndTopic() {
        val c = SyllabusPath.context(nodes, 3, " BSc Physics, Sem 2 ")
        assertEquals("BSc Physics, Sem 2", c.course)
        assertEquals("Waves", c.unit)
        assertEquals("Interference (Young's double slit)", c.chapter)
        assertEquals("Fringe width", c.topic)
    }

    @Test
    fun noNodeGivesAnEmptyContext() {
        assertEquals(true, SyllabusPath.context(nodes, null, "").isEmpty)
    }
}
