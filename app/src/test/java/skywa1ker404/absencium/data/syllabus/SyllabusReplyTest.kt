package skywa1ker404.absencium.data.syllabus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyllabusReplyTest {

    @Test fun nestedJsonKeepsItsExactTree() {
        val reply = """Sure!
            {"items":[{"title":"Mechanics","children":[{"title":"Laws","children":["Inertia"]},{"title":"Work"}]},{"title":"Optics"}]}"""
        val items = SyllabusTree.parseReply(reply)
        assertEquals(listOf("Mechanics", "Laws", "Inertia", "Work", "Optics"), items.map { it.title })
        assertEquals(listOf(0, 1, 2, 1, 0), items.map { it.depth })
    }

    @Test fun courseDetailsAreDroppedWithEverythingUnderThem() {
        val reply = """{"items":[{"title":"Course Code: PH101"},{"title":"Learning outcomes","children":[{"title":"Understand motion"}]},{"title":"Waves","children":[{"title":"Sound"}]}]}"""
        assertEquals(listOf("Waves", "Sound"), SyllabusTree.parseReply(reply).map { it.title })
    }

    @Test fun plainIndentedTextStillWorksWhenThereIsNoJson() {
        val items = SyllabusTree.parseReply("- Algebra\n  - Groups\n  - Rings\n- Calculus")
        assertEquals(listOf("Algebra", "Groups", "Rings", "Calculus"), items.map { it.title })
        assertEquals(listOf(0, 1, 1, 0), items.map { it.depth })
    }

    @Test fun brokenJsonFallsBackInsteadOfCrashing() {
        // The text reader takes over; the point is that it returns instead of throwing.
        assertTrue(SyllabusTree.parseReply("{ not json").size <= 1)
    }

    @Test fun repliesFromSeveralBatchesMergeIntoOneTree() {
        val a = SyllabusTree.parseReply("""{"items":[{"title":"Mechanics","children":[{"title":"Laws"}]}]}""")
        val b = SyllabusTree.parseReply("""{"items":[{"title":"mechanics","children":[{"title":"Work"}]},{"title":"Optics"}]}""")
        val merged = SyllabusTree.mergeOutlines(listOf(a, b))
        assertEquals(listOf("Mechanics", "Laws", "Work", "Optics"), merged.map { it.title })
        assertEquals(listOf(0, 1, 1, 0), merged.map { it.depth })
    }
}
