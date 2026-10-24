package skywa1ker404.absencium.ui.vault.markdown

/** A text after a deletion was repaired, with where the cursor belongs. */
data class Healed(val text: String, val cursor: Int)

/**
 * The editor hides formatting characters, so a backspace can land inside one (the `## ` of a heading, the `**` of bold, the
 * `{color:...|` of a colour). Removing only part of it would leave junk like `{color:00AC` on the page. This repairs such a
 * deletion: the whole marker goes, and for a pair (bold, colour, size...) its partner goes too, so the text is simply plain again.
 * Pure.
 */
object MarkerHeal {

    private val paired = setOf(EditKind.BOLD, EditKind.ITALIC, EditKind.STRIKE, EditKind.UNDERLINE, EditKind.HIGHLIGHT, EditKind.COLOR, EditKind.SIZE, EditKind.LINK, EditKind.CODE)

    /**
     * A line break typed inside styled text (bold, a colour, a link...) would split its markers across two lines and break the
     * style. At the end of the styled text the break goes after the closing marker; in the middle the style is closed before the
     * break and opened again after it. Returns null when [after] is not [before] plus one line break, or the break is not in a style.
     */
    fun keepStylesWhole(before: String, after: String): Healed? {
        if (after.length != before.length + 1) return null
        var p = 0
        while (p < before.length && before[p] == after[p]) p++
        if (after[p] != '\n' || after.substring(p + 1) != before.substring(p)) return null

        val ranges = EditStyle.ranges(before)
        val markers = ranges.filter { it.kind == EditKind.MARKER }
        val enclosing = ranges.filter { it.kind in paired && it.start <= p && p <= it.end }
        if (enclosing.isEmpty()) return null
        fun opener(r: EditRange) = markers.firstOrNull { it.end == r.start }
        fun closer(r: EditRange) = markers.firstOrNull { it.start == r.end }

        val middle = enclosing.filter { it.start < p && p < it.end && opener(it) != null && closer(it) != null }
        if (middle.isNotEmpty()) {
            // Innermost first when closing, outermost first when opening again.
            val inner = middle.sortedBy { it.end - it.start }
            val closers = inner.joinToString("") { before.substring(closer(it)!!.start, closer(it)!!.end) }
            val openers = inner.reversed().joinToString("") { before.substring(opener(it)!!.start, opener(it)!!.end) }
            val text = before.substring(0, p) + closers + "\n" + openers + before.substring(p)
            return Healed(text, p + closers.length + 1 + openers.length)
        }
        val atEnd = enclosing.filter { it.end == p }.mapNotNull { closer(it) }
        if (atEnd.isNotEmpty()) {
            val pos = atEnd.maxOf { it.end }
            return Healed(before.substring(0, pos) + "\n" + before.substring(pos), pos + 1)
        }
        val atStart = enclosing.filter { it.start == p }.mapNotNull { opener(it) }
        if (atStart.isNotEmpty()) {
            val pos = atStart.minOf { it.start }
            return Healed(before.substring(0, pos) + "\n" + before.substring(pos), pos + 1)
        }
        return null
    }

    /** Returns the repaired text when [after] is [before] with a stretch deleted that cut into a marker; null when nothing needs repair. */
    fun heal(before: String, after: String): Healed? {
        if (after.length >= before.length) return null
        var p = 0
        val limit = minOf(before.length, after.length)
        while (p < limit && before[p] == after[p]) p++
        var q = 0
        while (q < limit - p && before[before.length - 1 - q] == after[after.length - 1 - q]) q++
        val delStart = p
        val delEnd = before.length - q
        // Only a pure deletion is repaired; a replacement is the user typing over something.
        if (after.length != before.length - (delEnd - delStart)) return null
        if (delEnd <= delStart) return null

        val ranges = EditStyle.ranges(before)
        val markers = ranges.filter { it.kind == EditKind.MARKER && (it.arg in 0..6) }
        val cut = ArrayList<IntArray>()
        cut += intArrayOf(delStart, delEnd)
        var touched = false
        for (m in markers) {
            val partial = m.start < delEnd && m.end > delStart && !(delStart <= m.start && delEnd >= m.end)
            if (!partial) continue
            touched = true
            cut += intArrayOf(m.start, m.end)
            // A pair: the marker just before or just after the styled text has a partner on the other side.
            for (r in ranges) {
                if (r.kind !in paired) continue
                if (m.end == r.start) markers.firstOrNull { it.start == r.end }?.let { cut += intArrayOf(it.start, it.end) }
                if (m.start == r.end) markers.firstOrNull { it.end == r.start }?.let { cut += intArrayOf(it.start, it.end) }
            }
        }
        if (!touched) return null
        // Remove the union of the deleted stretch and the markers it cut into; the styled text between a pair stays.
        cut.sortBy { it[0] }
        val merged = ArrayList<IntArray>()
        for (c in cut) {
            val last = merged.lastOrNull()
            if (last != null && c[0] <= last[1]) last[1] = maxOf(last[1], c[1]) else merged += intArrayOf(c[0], c[1])
        }
        val out = StringBuilder()
        var pos = 0
        for (c in merged) {
            out.append(before, pos, c[0])
            pos = c[1]
        }
        out.append(before, pos, before.length)
        return Healed(out.toString(), merged.first()[0].coerceIn(0, out.length))
    }
}
