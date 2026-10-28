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
        val (s0, e0) = clamp(text, start, end)
        val ws = InlineStyle.wraps(text)
        val (s, e) = InlineStyle.fit(s0, e0, ws)
        // Code shows its text as typed: nothing else can style what is inside it.
        if (kind != EditKind.CODE && InlineStyle.insideCode(ws, s, e)) return Edited(text, s, e)
        val host = InlineStyle.host(ws, kind, s, e)
        if (host != null) return InlineStyle.release(text, host, s, e, ws)
        return InlineStyle.wrap(text, s, e, kind, marker, marker, placeholder, ws)
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

    private fun setTag(text: String, start: Int, end: Int, kind: EditKind, open: String?, placeholder: String): Edited {
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
