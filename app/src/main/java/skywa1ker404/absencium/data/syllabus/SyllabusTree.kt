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

    /** The item above [index] that it sits inside (the nearest earlier item one level up), or null for a top-level item. */
    fun parentIndex(items: List<OutlineItem>, index: Int): Int? {
        val depth = items[index].depth
        if (depth == 0) return null
        for (i in index - 1 downTo 0) if (items[i].depth == depth - 1) return i
        return null
    }

    /** [index] and every item inside it (the lines after it that are deeper). */
    fun subtreeIndices(items: List<OutlineItem>, index: Int): List<Int> {
        val out = arrayListOf(index)
        var i = index + 1
        while (i < items.size && items[i].depth > items[index].depth) { out += i; i++ }
        return out
    }

    /**
     * The items of [items] that would be duplicates once added below [under] (null for the top level): an item whose title
     * (ignoring case and spacing) already sits under the same parent in [existing], or that repeats an earlier item of the list
     * under the same parent. Children of a duplicate are compared with the children of the node it matched.
     */
    fun duplicates(existing: List<SyllabusNodeEntity>, items: List<OutlineItem>, under: Long?): Set<Int> {
        fun norm(t: String) = t.trim().replace(Regex("\\s+"), " ").lowercase()
        val known = HashMap<Pair<String, String>, String>()
        for (n in existing) known[(n.parentId?.let { "e$it" } ?: "root") to norm(n.title)] = "e${n.id}"
        val rootKey = under?.let { "e$it" } ?: "root"
        val trail = arrayOfNulls<String>(MAX_DEPTH + 2)
        val dup = HashSet<Int>()
        items.forEachIndexed { i, item ->
            val d = item.depth.coerceIn(0, MAX_DEPTH)
            val parent = if (d == 0) rootKey else trail[d - 1] ?: rootKey
            val key = parent to norm(item.title)
            val hit = known[key]
            if (hit != null) {
                dup += i
                trail[d] = hit
            } else {
                known[key] = "n$i"
                trail[d] = "n$i"
            }
            for (deeper in d + 1 until trail.size) trail[deeper] = null
        }
        return dup
    }
}
