package skywa1ker404.absencium.ui.vault.markdown

/**
 * The rules for putting an inline style (bold, italic, strike, underline, highlight, code, colour, size) on a selection, and
 * for taking it off again, however many other styles are already around it. Pure: text and selection in, text and selection out.
 *
 * The note is written in plain markers, and the live editor hides them, so the selection the user sees does not always line up
 * with the markers (it can start in front of an opening marker or end in front of a closing one). Every operation therefore
 * first fits the selection to the markers: it steps in over markers at its edges and reaches out to take in the other half of a
 * pair it only half covers. A style is then removed from the pair that really encloses the selection, splitting the pair when
 * only part of it is selected, instead of piling a second pair on top of the first.
 */
internal object InlineStyle {

    /** One styled run: the opening marker, the closing marker, and what they enclose. All offsets are into the whole text. */
    class Wrap(
        val kind: EditKind,
        val openStart: Int,
        val openEnd: Int,
        val closeStart: Int,
        val closeEnd: Int,
    ) {
        val innerStart: Int get() = openEnd
        val innerEnd: Int get() = closeStart
    }

    private val inlineKinds = setOf(
        EditKind.BOLD, EditKind.ITALIC, EditKind.STRIKE, EditKind.UNDERLINE,
        EditKind.HIGHLIGHT, EditKind.COLOR, EditKind.SIZE, EditKind.CODE,
    )
    private val styleKinds = inlineKinds - EditKind.CODE

    fun kindFor(marker: String): EditKind? = when (marker) {
        "**" -> EditKind.BOLD
        "*" -> EditKind.ITALIC
        "~~" -> EditKind.STRIKE
        "++" -> EditKind.UNDERLINE
        "==" -> EditKind.HIGHLIGHT
        "`" -> EditKind.CODE
        else -> null
    }

    fun wraps(text: String): List<Wrap> {
        val ranges = EditStyle.ranges(text)
        val byEnd = HashMap<Int, EditRange>()
        val byStart = HashMap<Int, EditRange>()
        for (r in ranges) {
            if (r.kind == EditKind.MARKER && r.arg == 0) {
                byEnd[r.end] = r
                byStart[r.start] = r
            }
        }
        val out = ArrayList<Wrap>()
        for (r in ranges) {
            if (r.kind !in inlineKinds) continue
            val open = byEnd[r.start] ?: continue
            val close = byStart[r.end] ?: continue
            out += Wrap(r.kind, open.start, open.end, close.start, close.end)
        }
        return out
    }

    /** The selection, stepped in over markers at its edges and widened to hold whole pairs. A cursor stays a cursor. */
    fun fit(start: Int, end: Int, ws: List<Wrap>): Pair<Int, Int> {
        var s = start
        var e = end
        if (s == e) return s to e
        var changed = true
        var guard = 0
        while (changed && guard++ < 40) {
            changed = false
            for (w in ws) {
                if (w.openStart == s && w.openEnd <= e) {
                    s = w.openEnd
                    changed = true
                } else if (w.closeEnd == e && w.closeStart >= s) {
                    e = w.closeStart
                    changed = true
                }
            }
        }
        changed = true
        guard = 0
        while (changed && guard++ < 40) {
            changed = false
            for (w in ws) {
                val openIn = w.openStart >= s && w.openEnd <= e
                val closeIn = w.closeStart >= s && w.closeEnd <= e
                if (openIn && !closeIn && w.closeEnd > e) {
                    e = w.closeEnd
                    changed = true
                } else if (closeIn && !openIn && w.openStart < s) {
                    s = w.openStart
                    changed = true
                }
            }
        }
        return s to e
    }

    /** The smallest run of [kind] that holds the whole selection, or null. */
    fun host(ws: List<Wrap>, kind: EditKind, s: Int, e: Int): Wrap? =
        ws.filter { it.kind == kind && it.innerStart <= s && e <= it.innerEnd }.minByOrNull { it.innerEnd - it.innerStart }

    fun insideCode(ws: List<Wrap>, s: Int, e: Int): Boolean =
        ws.any { it.kind == EditKind.CODE && it.innerStart <= s && e <= it.innerEnd }

    /** Widens [s]..[e] to whole runs of the other styles inside [host], so a split never cuts one of them in two. */
    private fun widenWithin(ws: List<Wrap>, host: Wrap, s: Int, e: Int): Pair<Int, Int> {
        var a = s
        var b = e
        var changed = true
        var guard = 0
        while (changed && guard++ < 40) {
            changed = false
            for (w in ws) {
                if (w === host || w.openStart == host.openStart) continue
                if (w.openStart < host.innerStart || w.closeEnd > host.innerEnd) continue
                val touches = if (a == b) a > w.openStart && a < w.closeEnd else a < w.closeEnd && b > w.openStart
                if (!touches) continue
                if (w.openStart < a) { a = w.openStart; changed = true }
                if (w.closeEnd > b) { b = w.closeEnd; changed = true }
            }
        }
        return a to b
    }

    private fun piece(x: String, open: String, close: String): String =
        if (x.isEmpty()) "" else if (open.isEmpty() && close.isEmpty()) x else open + x + close

    /**
     * Takes [host] off the selection. A selection that is the whole run (or a cursor in it) removes the pair; a selection of
     * part of it splits the pair around the selected part. [midOpen] and [midClose] wrap the selected part afterwards; they are
     * what is left of a `***` pair, or the new tag when a colour or size is replaced.
     */
    fun release(
        text: String,
        host: Wrap,
        start: Int,
        end: Int,
        ws: List<Wrap>,
        midOpen: String? = null,
        midClose: String? = null,
    ): Edited {
        val whole = start == end || (start <= host.innerStart && end >= host.innerEnd)
        val (s, e) = if (whole) host.innerStart to host.innerEnd else widenWithin(ws, host, start, end)
        val shared = host.closeEnd - host.closeStart == 3
        val rest = if (shared) (if (host.kind == EditKind.BOLD) "*" else "**") else ""
        val mo = midOpen ?: rest
        val mc = midClose ?: rest
        val open = text.substring(host.openStart, host.openEnd)
        val close = text.substring(host.closeStart, host.closeEnd)
        val left = text.substring(host.innerStart, s)
        val mid = text.substring(s, e)
        val right = text.substring(e, host.innerEnd)
        val sb = StringBuilder()
        sb.append(piece(left, open, close))
        val midStart = host.openStart + sb.length + (if (mid.isEmpty()) 0 else mo.length)
        sb.append(piece(mid, mo, mc))
        sb.append(piece(right, open, close))
        val out = text.substring(0, host.openStart) + sb + text.substring(host.closeEnd)
        return Edited(out, midStart, midStart + mid.length)
    }

    /**
     * Wraps the selection in [open] and [close], first removing the same style where it already sits inside the selection (for code:
     * every style, since code shows its text as typed). A cursor gets [placeholder].
     */
    fun wrap(text: String, s: Int, e: Int, kind: EditKind, open: String, close: String, placeholder: String, ws: List<Wrap>): Edited {
        val removing = if (kind == EditKind.CODE) styleKinds else setOf(kind)
        val targets = ws.filter { it.kind in removing && it.openStart >= s && it.closeEnd <= e }
        val body = if (s == e) placeholder else cleaned(text, s, e, targets)
        val out = text.substring(0, s) + open + body + close + text.substring(e)
        return Edited(out, s + open.length, s + open.length + body.length)
    }

    /** `text[s, e)` without the markers of [targets]; a `***` pair keeps the half that is not removed. */
    private fun cleaned(text: String, s: Int, e: Int, targets: List<Wrap>): String {
        if (targets.isEmpty()) return text.substring(s, e)
        val edits = java.util.TreeMap<Int, Pair<Int, String>>()
        for (w in targets) {
            for ((a, b) in listOf(w.openStart to w.openEnd, w.closeStart to w.closeEnd)) {
                val replacement = if (b - a == 3) {
                    val boldGone = targets.any { it.kind == EditKind.BOLD && (it.openStart == w.openStart || it.closeStart == w.closeStart) }
                    val italicGone = targets.any { it.kind == EditKind.ITALIC && (it.openStart == w.openStart || it.closeStart == w.closeStart) }
                    (if (boldGone) "" else "**") + (if (italicGone) "" else "*")
                } else {
                    ""
                }
                edits[a] = b to replacement
            }
        }
        val sb = StringBuilder()
        var pos = s
        for ((a, v) in edits) {
            if (a < pos) continue
            sb.append(text, pos, a)
            sb.append(v.second)
            pos = v.first
        }
        sb.append(text, pos, e)
        return sb.toString()
    }
}
