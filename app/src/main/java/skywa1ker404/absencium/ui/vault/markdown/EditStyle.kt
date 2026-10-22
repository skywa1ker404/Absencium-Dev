package skywa1ker404.absencium.ui.vault.markdown

/** How a stretch of the note is drawn while it is being edited. The text itself is never changed, only coloured and sized. */
enum class EditKind { MARKER, BOLD, ITALIC, STRIKE, UNDERLINE, HIGHLIGHT, COLOR, SIZE, HEADING, CODE, LINK }

/** [arg] is the heading level, the colour (0xRRGGBB) or the size in sp, depending on [kind]. [end] is exclusive. */
data class EditRange(val start: Int, val end: Int, val kind: EditKind, val arg: Int = 0)

/**
 * Finds what to colour in the text of a note while it is edited: the formatting characters are shown dimmed, and the
 * text they style is shown styled, so the page looks like the preview with the markers still in reach. Linear in the length
 * of the text; unmatched markers are left plain. Pure.
 */
object EditStyle {

    private const val LOOK_AHEAD = 2000
    private val listPrefix = Regex("^\\s*(?:[-*+] \\[[ xX]\\] |[-*+] |\\d{1,9}[.)] |[a-z]\\) |> ?)")

    private fun find(text: String, marker: String, from: Int, limit: Int): Int {
        if (from >= limit) return -1
        val end = minOf(limit, from + LOOK_AHEAD)
        val hit = text.substring(from, end).indexOf(marker)
        return if (hit < 0) -1 else from + hit
    }

    fun ranges(text: String): List<EditRange> {
        val out = ArrayList<EditRange>()
        var lineStart = 0
        var inFence = false
        while (lineStart <= text.length) {
            var lineEnd = text.indexOf('\n', lineStart)
            if (lineEnd < 0) lineEnd = text.length
            val trimmed = text.substring(lineStart, lineEnd).trimStart()
            when {
                trimmed.startsWith("```") -> {
                    out += EditRange(lineStart, lineEnd, EditKind.MARKER)
                    inFence = !inFence
                }
                inFence -> out += EditRange(lineStart, lineEnd, EditKind.CODE)
                trimmed.startsWith("{{") && trimmed.trimEnd().endsWith("}}") -> out += EditRange(lineStart, lineEnd, EditKind.MARKER)
                else -> styleLine(text, lineStart, lineEnd, out)
            }
            lineStart = lineEnd + 1
        }
        return out
    }

    private fun styleLine(text: String, from: Int, to: Int, out: MutableList<EditRange>) {
        var bodyStart = from
        if (text.startsWith("{align:", from)) {
            val close = text.indexOf('}', from)
            if (close in from until to) {
                out += EditRange(from, close + 1, EditKind.MARKER)
                bodyStart = close + 1
            }
        }
        var level = 0
        while (bodyStart + level < to && level < 6 && text[bodyStart + level] == '#') level++
        if (level in 1..6 && bodyStart + level < to && text[bodyStart + level] == ' ') {
            out += EditRange(bodyStart, bodyStart + level + 1, EditKind.MARKER)
            out += EditRange(bodyStart + level + 1, to, EditKind.HEADING, level)
        } else {
            val m = listPrefix.find(text.substring(bodyStart, minOf(to, bodyStart + 24)))
            if (m != null) out += EditRange(bodyStart, bodyStart + m.value.length, EditKind.MARKER)
        }
        if (text.substring(bodyStart, minOf(to, bodyStart + 16)).trimStart().startsWith("|")) {
            for (i in bodyStart until to) if (text[i] == '|') out += EditRange(i, i + 1, EditKind.MARKER)
        }
        inline(text, bodyStart, to, out)
    }

    private fun inline(text: String, from: Int, to: Int, out: MutableList<EditRange>) {
        val skip = HashSet<Int>()
        var i = from
        while (i < to) {
            if (i in skip) { i += 1; continue }
            val c = text[i]
            when {
                c == '[' && text.startsWith("[[", i) -> {
                    val close = find(text, "]]", i + 2, to)
                    if (close > i + 2) {
                        out += EditRange(i, i + 2, EditKind.MARKER)
                        out += EditRange(i + 2, close, EditKind.LINK)
                        out += EditRange(close, close + 2, EditKind.MARKER)
                        i = close + 2
                    } else {
                        i++
                    }
                }
                c == '`' -> {
                    val close = find(text, "`", i + 1, to)
                    if (close > i + 1) {
                        out += EditRange(i, i + 1, EditKind.MARKER)
                        out += EditRange(i + 1, close, EditKind.CODE)
                        out += EditRange(close, close + 1, EditKind.MARKER)
                        i = close + 1
                    } else {
                        i++
                    }
                }
                text.startsWith("**", i) -> i = pair(text, i, to, "**", EditKind.BOLD, out, skip, spaced = false)
                text.startsWith("~~", i) -> i = pair(text, i, to, "~~", EditKind.STRIKE, out, skip, spaced = false)
                text.startsWith("++", i) -> i = pair(text, i, to, "++", EditKind.UNDERLINE, out, skip, spaced = true)
                text.startsWith("==", i) -> i = pair(text, i, to, "==", EditKind.HIGHLIGHT, out, skip, spaced = true)
                c == '*' && i + 1 < to && !text[i + 1].isWhitespace() -> {
                    val close = find(text, "*", i + 1, to)
                    if (close > i + 1 && !text[close - 1].isWhitespace() && !text.startsWith("**", close)) {
                        out += EditRange(i, i + 1, EditKind.MARKER)
                        out += EditRange(i + 1, close, EditKind.ITALIC)
                        out += EditRange(close, close + 1, EditKind.MARKER)
                        skip += close
                    }
                    i++
                }
                c == '{' && (text.startsWith("{color:", i) || text.startsWith("{size:", i)) -> i = tagged(text, i, to, out, skip)
                else -> i++
            }
        }
    }

    /** A two-character marker pair. The closing marker is remembered so it is not read as an opening one. Returns where to continue. */
    private fun pair(
        text: String,
        i: Int,
        to: Int,
        marker: String,
        kind: EditKind,
        out: MutableList<EditRange>,
        skip: MutableSet<Int>,
        spaced: Boolean,
    ): Int {
        val opens = !spaced || (i + 2 < to && !text[i + 2].isWhitespace())
        val close = if (opens) find(text, marker, i + 2, to) else -1
        if (close > i + 2 && !(spaced && text[close - 1].isWhitespace())) {
            out += EditRange(i, i + 2, EditKind.MARKER)
            out += EditRange(i + 2, close, kind)
            out += EditRange(close, close + 2, EditKind.MARKER)
            skip += close
            skip += close + 1
        }
        return i + 2
    }

    private fun tagged(text: String, i: Int, to: Int, out: MutableList<EditRange>, skip: MutableSet<Int>): Int {
        val isColor = text.startsWith("{color:", i)
        val argStart = i + if (isColor) 7 else 6
        val bar = find(text, "|", i, to)
        if (bar !in (argStart + 1)..(argStart + 7)) return i + 1
        val close = find(text, "|}", bar + 1, to)
        if (close < 0) return i + 1
        val arg = text.substring(argStart, bar)
        val value = if (isColor) (if (arg.length == 6) arg.toIntOrNull(16) else null) else arg.toIntOrNull()?.takeIf { it in 8..64 }
        if (value == null) return i + 1
        out += EditRange(i, bar + 1, EditKind.MARKER)
        out += EditRange(bar + 1, close, if (isColor) EditKind.COLOR else EditKind.SIZE, value)
        out += EditRange(close, close + 2, EditKind.MARKER)
        skip += close
        skip += close + 1
        return bar + 1
    }
}
