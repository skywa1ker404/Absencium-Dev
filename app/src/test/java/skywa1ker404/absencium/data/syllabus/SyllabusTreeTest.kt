package skywa1ker404.absencium.data.syllabus

import org.junit.Assert.assertEquals
import org.junit.Test
import skywa1ker404.absencium.data.db.SyllabusNodeEntity
import skywa1ker404.absencium.data.db.SyllabusStatus

class SyllabusTreeTest {

    private fun node(id: Long, parent: Long?, status: SyllabusStatus = SyllabusStatus.NOT_STARTED, position: Int = 0) =
        SyllabusNodeEntity(id = id, subjectId = 1, parentId = parent, title = "n$id", status = status, position = position)

    @Test fun rowsFollowSavedOrderAndHideCollapsedChildren() {
        val nodes = listOf(node(1, null, position = 1), node(2, null, position = 0), node(3, 2), node(4, 3))
        assertEquals(listOf(2L, 3L, 4L, 1L), SyllabusTree.rows(nodes, emptySet()).map { it.node.id })
        assertEquals(listOf(2L, 1L), SyllabusTree.rows(nodes, setOf(2L)).map { it.node.id })
        assertEquals(listOf(0, 1, 2, 0), SyllabusTree.rows(nodes, emptySet()).map { it.depth })
    }

    @Test fun progressIsTheAverageOfTheLeaves() {
        val nodes = listOf(
            node(1, null),
            node(2, 1, SyllabusStatus.COMPLETED),
            node(3, 1, SyllabusStatus.IN_PROGRESS),
            node(4, 1, SyllabusStatus.NOT_STARTED),
            node(5, null, SyllabusStatus.COMPLETED),
        )
        assertEquals(0.5f, SyllabusTree.progress(nodes, 1), 0.001f)
        assertEquals(1f, SyllabusTree.progress(nodes, 5), 0.001f)
        assertEquals(0.625f, SyllabusTree.progress(nodes, null), 0.001f)
        assertEquals(0f, SyllabusTree.progress(emptyList(), null), 0.001f)
    }

    @Test fun descendantsIncludeTheNodeItself() {
        val nodes = listOf(node(1, null), node(2, 1), node(3, 2), node(4, null))
        assertEquals(setOf(1L, 2L, 3L), SyllabusTree.withDescendants(nodes, 1).toSet())
    }

    @Test fun statusCyclesRound() {
        assertEquals(SyllabusStatus.IN_PROGRESS, SyllabusTree.nextStatus(SyllabusStatus.NOT_STARTED))
        assertEquals(SyllabusStatus.NOT_STARTED, SyllabusTree.nextStatus(SyllabusStatus.COMPLETED))
    }

    @Test fun outlinesAreReadByIndentationWithMarkersIgnored() {
        val items = SyllabusTree.parseOutline(
            "- Unit 1: Basics\n  - 1.1 First chapter\n    - Topic A\n    - Topic B\n- Unit 2\n        - Too deep at once\n\n1. Numbered unit",
        )
        assertEquals(listOf(0, 1, 2, 2, 0, 1, 0), items.map { it.depth })
        assertEquals("First chapter", items[1].title)
        assertEquals("Unit 1: Basics", items[0].title)
        assertEquals("Numbered unit", items.last().title)
    }
}
