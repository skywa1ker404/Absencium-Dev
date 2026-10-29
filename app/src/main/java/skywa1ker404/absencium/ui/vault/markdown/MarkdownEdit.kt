package skywa1ker404.absencium.ui.vault.markdown

/** A text with a selection (start..end, start <= end; equal means a cursor). Plain data, so the editing rules are unit-tested on the JVM. */
data class Edited(val text: String, val start: Int, val end: Int)

/** What the formatting toolbar does to the text. Every function is pure: text and selection in, text and selection out. */
object MarkdownEdit {

    private fun clamp(text: String, start: Int, end: Int): Pair<Int, Int> {
        val a = start.coerceIn(0, text.length)
        val b = end.coerceIn(0, text.length)
        return minOf(a, b) to maxOf(a, b)
    }

    /**
     * Wraps the selection in [marker] (bold `**`, italic `*`, code `` ` ``, strike `~~`), or removes the marker when the selection
     * is already wrapped. With only a cursor, inserts the pair around [placeholder] and selects the placeholder.
     */
    fun toggleWrap(text: String, start: Int, end: Int, marker: String, placeholder: String): Edited {
        val kind = InlineStyle.kindFor(marker) ?: return plainWrap(text, start, end, marker, placeholder)
        return perLine(text, start, end, mixedSkips = true, isOn = { t, a, b ->
            val ws = InlineStyle.wraps(t)
            val (fa, fb) = InlineStyle.fit(a, b, ws)
            InlineStyle.host(ws, kind, fa, fb) != null
        }) { t, a, b -> toggleOne(t, a, b, kind, marker, placeholder) }
    }

    private fun toggleOne(text: String, s0: Int, e0: Int, kind: EditKind, marker: String, placeholder: String): Edited {
        val ws = InlineStyle.wraps(text)
        val (s, e) = InlineStyle.fit(s0, e0, ws)
        // Code shows its text as typed: nothing else can style what is inside it.
        if (kind != EditKind.CODE && InlineStyle.insideCode(ws, s, e)) return Edited(text, s, e)
        val host = InlineStyle.host(ws, kind, s, e)
        if (host != null) return InlineStyle.release(text, host, s, e, ws)
        return InlineStyle.wrap(text, s, e, kind, marker, marker, placeholder, ws)
    }

    /** Where the words of the line holding [index] start: after its alignment tag, indent, heading mark, list marker or quote mark. -1 for a line with no words to style. */
    private fun bodyStartOf(text: String, index: Int): Int {
        val ls = lineStart(text, index)
        val le = lineEnd(text, index)
        val line = text.substring(ls, le)
        if (line.trimStart().startsWith("```")) return -1
        val p = parts(line)
        if (isRuleText(p.body)) return -1
        return ls + line.length - p.body.length
    }

    /**
     * Runs [op] on each line the selection touches, on the words of the line only: a style must sit after the line's block prefix
     * (or the live page would show the `## ` it hides), and a pair never spans a line break (the page reads one line at a time).
     * Works from the last line to the first so the offsets of the lines still to do hold. With [mixedSkips], lines that are already
     * in the style are left alone unless every line is.
     */
    private fun perLine(
        text: String,
        start: Int,
        end: Int,
        mixedSkips: Boolean = false,
        isOn: (String, Int, Int) -> Boolean = { _, _, _ -> false },
        op: (String, Int, Int) -> Edited,
    ): Edited {
        val (s, e) = clamp(text, start, end)
        if (s == e) {
            val body = bodyStartOf(text, s)
            return op(text, if (body >= 0) maxOf(s, body) else s, if (body >= 0) maxOf(s, body) else e)
        }
        val ranges = ArrayList<Pair<Int, Int>>()
        var pos = s
        while (pos <= e) {
            val le = lineEnd(text, pos)
            val body = bodyStartOf(text, pos)
            val a = if (body >= 0) maxOf(pos, body) else -1
            val b = minOf(e, le)
            if (a in 0 until b) ranges += a to b
            if (le >= e) break
            pos = le + 1
        }
        if (ranges.isEmpty()) return Edited(text, s, e)
        if (ranges.size == 1) return op(text, ranges[0].first, ranges[0].second)
        val allOn = ranges.all { isOn(text, it.first, it.second) }
        var cur = text
        var first = 0
        var last = 0
        var shift = 0
        for ((i, r) in ranges.asReversed().withIndex()) {
            if (mixedSkips && !allOn && isOn(cur, r.first, r.second)) {
                if (i == 0) last = r.second
                if (i == ranges.lastIndex) first = r.first
                continue
            }
            val before = cur.length
            val out = op(cur, r.first, r.second)
            cur = out.text
            if (i == 0) last = out.end
            else shift += cur.length - before
            if (i == ranges.lastIndex) first = out.start
        }
        return Edited(cur, first, last + shift)
    }

    private fun plainWrap(text: String, start: Int, end: Int, marker: String, placeholder: String): Edited {
        val (s, e) = clamp(text, start, end)
        val m = marker.length
        if (s >= m && e + m <= text.length && text.substring(s - m, s) == marker && text.substring(e, e + m) == marker) {
            val out = text.substring(0, s - m) + text.substring(s, e) + text.substring(e + m)
            return Edited(out, s - m, e - m)
        }
        if (s == e) {
            val out = text.substring(0, s) + marker + placeholder + marker + text.substring(e)
            return Edited(out, s + m, s + m + placeholder.length)
        }
        val out = text.substring(0, s) + marker + text.substring(s, e) + marker + text.substring(e)
        return Edited(out, s + m, e + m)
    }

    private fun lineStart(text: String, index: Int): Int {
        var i = index
        while (i > 0 && text[i - 1] != '\n') i--
        return i
    }

    private fun lineEnd(text: String, index: Int): Int {
        var i = index
        while (i < text.length && text[i] != '\n') i++
        return i
    }

    /** Applies [transform] to every line the selection touches and keeps the selection over the same lines. */
    private fun mapLines(text: String, start: Int, end: Int, transform: (List<String>) -> List<String>): Edited {
        val (s, e) = clamp(text, start, end)
        val from = lineStart(text, s)
        // A selection that ends exactly at the start of a line does not include that line.
        val to = lineEnd(text, if (e > s && e > 0 && text[e - 1] == '\n') e - 1 else e)
        val lines = text.substring(from, to).split("\n")
        val out = transform(lines).joinToString("\n")
        val result = text.substring(0, from) + out + text.substring(to)
        val delta = out.length - (to - from)
        return if (s == e) {
            // A cursor stays on its line, moved by what changed in front of it.
            val firstDelta = transform(listOf(lines.first())).first().length - lines.first().length
            val pos = (s + firstDelta).coerceIn(from, from + out.length)
            Edited(result, pos, pos)
        } else {
            Edited(result, from, to + delta)
        }
    }

    private enum class LineKind { NONE, HEADING, CHECK, BULLET, NUMBER, LETTER, QUOTE }

    /** A line taken apart: its alignment tag, its indent, what kind of line it is, and the text after every block prefix. */
    private class Parts(val head: String, val indent: String, val kind: LineKind, val level: Int, val body: String)

    private val alignHead = Regex("^\\{align:(?:center|end)\\}")
    private val anyPrefix = Regex("^(?:(#{1,6}) +|[-*+] \\[[ xX]\\] ?|[-*+] +|\\d{1,9}[.)] +|[a-z]\\) +|> ?)")

    private fun isRuleText(s: String): Boolean {
        val t = s.filter { !it.isWhitespace() }
        return t.length >= 3 && (t.all { it == '-' } || t.all { it == '*' } || t.all { it == '_' })
    }

    /**
     * A line has one block prefix (a heading mark, a list marker or a quote mark) after its alignment tag: that is all the page
     * reads. Every prefix found is taken off, so a line that already holds a stack of them is cleaned up when a new one is chosen.
     */
    private fun parts(line: String): Parts {
        val head = alignHead.find(line)?.value ?: ""
        val rest = line.substring(head.length)
        val indent = rest.takeWhile { it == ' ' || it == '\t' }
        var body = rest.substring(indent.length)
        if (isRuleText(body)) return Parts(head, indent, LineKind.NONE, 0, body)
        var kind = LineKind.NONE
        var level = 0
        var guard = 0
        while (guard++ < 8) {
            val m = anyPrefix.find(body) ?: break
            if (kind == LineKind.NONE) {
                val v = m.value
                kind = when {
                    v.startsWith("#") -> { level = m.groupValues[1].length; LineKind.HEADING }
                    v.startsWith(">") -> LineKind.QUOTE
                    v[0] in "-*+" && v.contains('[') -> LineKind.CHECK
                    v[0] in "-*+" -> LineKind.BULLET
                    v[0].isDigit() -> LineKind.NUMBER
                    else -> LineKind.LETTER
                }
            }
            body = body.substring(m.value.length)
        }
        return Parts(head, indent, kind, level, body)
    }

    /** Gives every touched line the prefix [prefixFor] makes, or takes that kind of prefix off when every line already has it. */
    private fun prefixLines(text: String, start: Int, end: Int, kind: LineKind, level: Int = 0, prefixFor: (Int) -> String): Edited =
        mapLines(text, start, end) { lines ->
            val blankOnly = lines.all { it.isBlank() }
            val parsed = lines.map { parts(it) }
            val all = !blankOnly && lines.indices.filter { lines[it].isNotBlank() }
                .all { parsed[it].kind == kind && (kind != LineKind.HEADING || parsed[it].level == level) }
            var n = 0
            lines.mapIndexed { i, line ->
                val p = parsed[i]
                when {
                    line.isBlank() && !blankOnly -> line
                    all -> p.head + p.indent + p.body
                    else -> p.head + (if (kind == LineKind.HEADING) "" else p.indent) + prefixFor(n++) + p.body
                }
            }
        }

    /** `# ` to `###### `. Choosing the level a line already has removes the heading. */
    fun heading(text: String, start: Int, end: Int, level: Int): Edited {
        val l = level.coerceIn(1, 6)
        return prefixLines(text, start, end, LineKind.HEADING, l) { "#".repeat(l) + " " }
    }

    fun bullet(text: String, start: Int, end: Int): Edited = prefixLines(text, start, end, LineKind.BULLET) { "- " }

    fun numbered(text: String, start: Int, end: Int): Edited = prefixLines(text, start, end, LineKind.NUMBER) { "${it + 1}. " }

    fun checklist(text: String, start: Int, end: Int): Edited = prefixLines(text, start, end, LineKind.CHECK) { "- [ ] " }

    fun quote(text: String, start: Int, end: Int): Edited = prefixLines(text, start, end, LineKind.QUOTE) { "> " }

    /** A lettered list: `a) `, `b) `... Choosing it again on a lettered selection removes it. */
    fun lettered(text: String, start: Int, end: Int): Edited = prefixLines(text, start, end, LineKind.LETTER) { "${'a' + (it % 26)}) " }

    /** Inserts [snippet] at the selection, replacing it, with the cursor after it. */
    fun insert(text: String, start: Int, end: Int, snippet: String): Edited {
        val (s, e) = clamp(text, start, end)
        return Edited(text.substring(0, s) + snippet + text.substring(e), s + snippet.length, s + snippet.length)
    }

    /** A fenced code block around the selection (or an empty one at the cursor). */
    fun codeBlock(text: String, start: Int, end: Int): Edited {
        val (s, e) = clamp(text, start, end)
        val before = if (s == 0 || text[s - 1] == '\n') "" else "\n"
        val selected = text.substring(s, e)
        val inner = selected.ifEmpty { "code" }
        val block = "$before```\n$inner\n```\n"
        val out = text.substring(0, s) + block + text.substring(e)
        val innerStart = s + before.length + 4
        return Edited(out, innerStart, innerStart + inner.length)
    }

    /** Flips the checkbox of the [line]th line (0-based) of a rendered checklist. Returns the text unchanged for any other line. */
    fun toggleCheckboxLine(text: String, line: Int): String {
        val lines = text.split("\n").toMutableList()
        if (line !in lines.indices) return text
        val l = lines[line]
        val m = Regex("^(\\s*[-*+] )\\[([ xX])\\]( .*|)$").find(l) ?: return text
        lines[line] = m.groupValues[1] + (if (m.groupValues[2] == " ") "[x]" else "[ ]") + m.groupValues[3]
        return lines.joinToString("\n")
    }

    private val alignTag = Regex("^\\{align:(center|end)\\}")

    /**
     * Gives the selection a colour (`{color:RRGGBB|text|}`), replacing the colour it already has (whatever other styles sit between);
     * null removes it. With only a cursor it inserts the placeholder in that colour, or recolours the run the cursor is in.
     */
    fun setColor(text: String, start: Int, end: Int, rgb: Int?, placeholder: String = "text"): Edited =
        setTag(text, start, end, EditKind.COLOR, rgb?.let { "{color:%06X|".format(it and 0xFFFFFF) }, placeholder)

    /** Same for the font size in sp (8 to 64); null removes it. */
    fun setSize(text: String, start: Int, end: Int, sp: Int?, placeholder: String = "text"): Edited =
        setTag(text, start, end, EditKind.SIZE, sp?.coerceIn(8, 64)?.let { "{size:$it|" }, placeholder)

    private fun setTag(text: String, start: Int, end: Int, kind: EditKind, open: String?, placeholder: String): Edited =
        perLine(text, start, end) { t, a, b -> setTagOne(t, a, b, kind, open, placeholder) }

    private fun setTagOne(text: String, start: Int, end: Int, kind: EditKind, open: String?, placeholder: String): Edited {
        val (s0, e0) = clamp(text, start, end)
        val ws = InlineStyle.wraps(text)
        val (s, e) = InlineStyle.fit(s0, e0, ws)
        if (InlineStyle.insideCode(ws, s, e)) return Edited(text, s, e)
        val host = InlineStyle.host(ws, kind, s, e)
        if (host != null) {
            return if (open == null) InlineStyle.release(text, host, s, e, ws) else InlineStyle.release(text, host, s, e, ws, open, "|}")
        }
        if (open == null) {
            if (s == e) return Edited(text, s, e)
            // Only colours or sizes inside the selection: take those off.
            return InlineStyle.wrap(text, s, e, kind, "", "", placeholder, ws)
        }
        return InlineStyle.wrap(text, s, e, kind, open, "|}", placeholder, ws)
    }

    /** Aligns every line the selection touches: start (no tag), centre or end. */
    fun setAlign(text: String, start: Int, end: Int, align: Align): Edited = mapLines(text, start, end) { lines ->
        val blankOnly = lines.all { it.isBlank() }
        lines.map { line ->
            val bare = line.replace(alignTag, "")
            when {
                bare.isBlank() && !blankOnly -> line
                align == Align.CENTER -> "{align:center}$bare"
                align == Align.END -> "{align:end}$bare"
                else -> bare
            }
        }
    }
}
