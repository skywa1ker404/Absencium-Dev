package skywa1ker404.absencium.data.ai

import skywa1ker404.absencium.data.db.SyllabusNodeEntity

/** Turns a subject's syllabus tree into choices for a dropdown, and into the context the AI is told to stay inside. */
object SyllabusPath {

    /** One node in a dropdown: [label] shows its place in the tree, e.g. "Unit 2 › Waves › Interference". */
    data class Choice(val id: Long, val label: String, val depth: Int)

    /** The nodes depth-first in their saved order. */
    fun choices(nodes: List<SyllabusNodeEntity>): List<Choice> {
        val byParent = nodes.groupBy { it.parentId }
        val out = ArrayList<Choice>()
        fun walk(parent: Long?, trail: List<String>) {
            byParent[parent].orEmpty().forEach { node ->
                val path = trail + node.title
                out += Choice(node.id, path.joinToString(" › "), trail.size)
                walk(node.id, path)
            }
        }
        walk(null, emptyList())
        return out
    }

    /** The unit, chapter and topic of node [id]; the first three levels are named, deeper ones join the topic. */
    fun context(nodes: List<SyllabusNodeEntity>, id: Long?, course: String, practical: Boolean = false): SyllabusContext {
        val byId = nodes.associateBy { it.id }
        val chain = ArrayList<SyllabusNodeEntity>()
        var current = id?.let { byId[it] }
        while (current != null && chain.size < 12) {
            chain.add(0, current)
            current = current.parentId?.let { byId[it] }
        }
        fun label(n: SyllabusNodeEntity) = n.details?.takeIf { it.isNotBlank() }?.let { "${n.title} (${it.take(300)})" } ?: n.title
        return SyllabusContext(
            course = course.trim(),
            unit = chain.getOrNull(0)?.let(::label).orEmpty(),
            chapter = chain.getOrNull(1)?.let(::label).orEmpty(),
            topic = chain.drop(2).joinToString(" › ") { label(it) },
            practical = practical,
        )
    }
}
