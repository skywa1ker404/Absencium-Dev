package skywa1ker404.absencium.ui.vault.markdown

/** A style on the live page, in offsets of the text that is shown ([LivePlan.visible]). */
data class LiveStyle(val start: Int, val end: Int, val kind: EditKind, val arg: Int)

/**
 * What the editor shows for a run of note text: the same page as the preview, with the formatting characters hidden or turned
 * into what they mean (a bullet, a check box, a divider). The note text itself is never changed; this only says what to show
 * and how to move between a position in the note and a position on screen.
 */
class LivePlan(
    val original: String,
    val visible: String,
    /** For each shown character, the position in the note just after the character it comes from. Never decreasing. */
    private val sourceEnd: IntArray,
    val styles: List<LiveStyle>,
    /** Where hidden line prefixes (a heading mark, an alignment tag) end in the note, in increasing order. */
    private val prefixEnds: IntArray = IntArray(0),
) {
    /** Where a position in the note appears on screen. A position among hidden characters appears at the point they were hidden. */
    fun toVisible(original: Int): Int {
        var lo = 0
        var hi = sourceEnd.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (sourceEnd[mid] <= original) lo = mid + 1 else hi = mid
        }
        return lo
    }

    /**
     * The position in the note for a spot on screen: just after the character before it, so typing continues that character's
     * style. At the start of a heading or aligned line it is after the hidden `## ` or tag, so typing stays on that line's style.
     */
    fun toOriginal(visible: Int): Int {
        val before = if (visible <= 0 || sourceEnd.isEmpty()) 0 else sourceEnd[(visible - 1).coerceAtMost(sourceEnd.size - 1)]
        val nextStart = if (visible in sourceEnd.indices) sourceEnd[visible] - 1 else original.length
        var lo = 0
        var hi = prefixEnds.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (prefixEnds[mid] < before) lo = mid + 1 else hi = mid
        }
        // Several prefixes can follow each other; take the last one that still ends before the next shown character.
        var result = before
        var i = lo
        while (i < prefixEnds.size && prefixEnds[i] <= nextStart) {
            result = prefixEnds[i]
            i++
        }
        return result
    }
}

object LiveFormat {

    private fun shown(arg: Int): String = when (arg) {
        1 -> "• "
        2 -> "☐ "
        3 -> "☑ "
        4 -> "┃ "
        5 -> "────────────"
        else -> ""
    }

    fun plan(text: String): LivePlan {
        val ranges = EditStyle.ranges(text)
        val markers = ranges.filter { it.kind == EditKind.MARKER }.sortedBy { it.start }
        val visible = StringBuilder(text.length)
        var sourceEnd = IntArray(text.length + 16)
        var count = 0
        fun push(ch: Char, end: Int) {
            if (count == sourceEnd.size) sourceEnd = sourceEnd.copyOf(sourceEnd.size * 2)
            visible.append(ch)
            sourceEnd[count++] = end
        }
        val replaced = ArrayList<Triple<Int, Int, Int>>() // visible start, visible end, marker arg
        val prefixEnds = ArrayList<Int>()
        var pos = 0
        for (m in markers) {
            if (m.start < pos || m.end > text.length) continue
            for (i in pos until m.start) push(text[i], i + 1)
            val s = shown(m.arg)
            val vStart = count
            for (ch in s) push(ch, m.end)
            if (s.isNotEmpty()) replaced += Triple(vStart, count, m.arg)
            if (m.arg == 6) prefixEnds += m.end
            pos = m.end
        }
        for (i in pos until text.length) push(text[i], i + 1)

        val ends = sourceEnd.copyOf(count)
        val plan = LivePlan(text, visible.toString(), ends, emptyList())
        val styles = ArrayList<LiveStyle>()
        for (r in ranges) {
            if (r.kind == EditKind.MARKER) continue
            val s = plan.toVisible(r.start)
            val e = plan.toVisible(r.end)
            if (e > s) styles += LiveStyle(s, e, r.kind, r.arg)
        }
        for ((s, e, arg) in replaced) styles += LiveStyle(s, e, EditKind.MARKER, arg)
        return LivePlan(text, plan.visible, ends, styles, prefixEnds.toIntArray())
    }
}
