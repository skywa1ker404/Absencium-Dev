package skywa1ker404.absencium.data.syllabus

import skywa1ker404.absencium.data.db.SyllabusNodeEntity
import skywa1ker404.absencium.data.db.SyllabusStatus

/** Pure helpers for a subject's syllabus tree: what is shown, how far along it is, and reading an outline written as text. */
object SyllabusTree {

    /** One visible line of the tree. */
    data class Row(val node: SyllabusNodeEntity, val depth: Int, val hasChildren: Boolean)

    /** One line of an outline read from text: how deep it sits (0 is a unit) and its title. */
    data class OutlineItem(val depth: Int, val title: String)

    const val MAX_DEPTH = 3

    /** What a node at [depth] is called: unit, chapter, topic, then subtopic for everything deeper. */
    fun levelName(depth: Int): String = when (depth) {
        0 -> "Unit"
        1 -> "Chapter"
        2 -> "Topic"
        else -> "Subtopic"
    }

    /** The tree depth-first in saved order, leaving out everything under a node in [collapsed]. */
    fun rows(nodes: List<SyllabusNodeEntity>, collapsed: Set<Long>): List<Row> {
        val byParent = nodes.groupBy { it.parentId }
        val out = ArrayList<Row>()
        fun walk(parent: Long?, depth: Int) {
            byParent[parent].orEmpty().sortedWith(compareBy({ it.position }, { it.id })).forEach { node ->
                val kids = byParent[node.id].orEmpty()
                out += Row(node, depth, kids.isNotEmpty())
                if (node.id !in collapsed && depth < 20) walk(node.id, depth + 1)
            }
        }
        walk(null, 0)
        return out
    }

    /** [id] and everything below it. */
    fun withDescendants(nodes: List<SyllabusNodeEntity>, id: Long): List<Long> {
        val byParent = nodes.groupBy { it.parentId }
        val out = ArrayList<Long>()
        fun walk(current: Long, guard: Int) {
            out += current
            if (guard < 30) byParent[current].orEmpty().forEach { walk(it.id, guard + 1) }
        }
        walk(id, 0)
        return out
    }

    private fun value(status: SyllabusStatus): Float = when (status) {
        SyllabusStatus.COMPLETED -> 1f
        SyllabusStatus.IN_PROGRESS -> 0.5f
        SyllabusStatus.NOT_STARTED -> 0f
    }

    /**
     * How far along node [id] is, from 0 to 1: the average of the finished-ness of the leaves below it (a completed leaf is 1,
     * one in progress is half). A node with no children counts as its own status. [id] null means the whole subject.
     */
    fun progress(nodes: List<SyllabusNodeEntity>, id: Long?): Float {
        if (nodes.isEmpty()) return 0f
        val byParent = nodes.groupBy { it.parentId }
        val leaves = ArrayList<SyllabusNodeEntity>()
        fun walk(node: SyllabusNodeEntity, guard: Int) {
            val kids = byParent[node.id].orEmpty()
            if (kids.isEmpty() || guard > 30) leaves += node else kids.forEach { walk(it, guard + 1) }
        }
        if (id == null) byParent[null].orEmpty().forEach { walk(it, 0) } else nodes.firstOrNull { it.id == id }?.let { walk(it, 0) }
        if (leaves.isEmpty()) return 0f
        return leaves.sumOf { value(it.status).toDouble() }.toFloat() / leaves.size
    }

    /** Not started, then in progress, then completed, then round again. */
    fun nextStatus(status: SyllabusStatus): SyllabusStatus = when (status) {
        SyllabusStatus.NOT_STARTED -> SyllabusStatus.IN_PROGRESS
        SyllabusStatus.IN_PROGRESS -> SyllabusStatus.COMPLETED
        SyllabusStatus.COMPLETED -> SyllabusStatus.NOT_STARTED
    }

    private val marker = Regex("^(?:[-*•◦]|#{1,6}|\\d+(?:\\.\\d+)*[.)]?|[a-zA-Z][.)])\\s+")

    /**
     * Reads an outline: one title per line, nested by indentation (two spaces or a tab per level), with bullets, numbers and
     * heading marks ignored. A line can only sit one level deeper than the one above it, and nothing goes deeper than
     * [MAX_DEPTH].
     */
    fun parseOutline(text: String): List<OutlineItem> {
        val out = ArrayList<OutlineItem>()
        var previous = -1
        text.lineSequence().forEach { raw ->
            if (raw.isBlank()) return@forEach
            val expanded = raw.replace("\t", "  ")
            val indent = expanded.length - expanded.trimStart().length
            var title = expanded.trim()
            val first = marker.find(title)
            if (first != null) {
                title = title.removePrefix(first.value).trim()
                // A bullet can be followed by a number ("- 1.1 Name"); drop that too.
                val bullet = first.value.trim()
                if (bullet.length == 1 && bullet[0] in "-*\u2022\u25E6" || bullet.startsWith("#")) title = title.replace(marker, "").trim()
            }
            title = title.take(120)
            if (title.isEmpty() || title.startsWith("```")) return@forEach
            val depth = (indent / 2).coerceAtMost(previous + 1).coerceIn(0, MAX_DEPTH)
            out += OutlineItem(depth, title)
            previous = depth
        }
        return out
    }
}
