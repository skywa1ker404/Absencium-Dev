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
        val (s, e) = clamp(text, start, end)
        val m = marker.length
        // Already wrapped just outside the selection: take the markers off.
        if (s >= m && e + m <= text.length && text.substring(s - m, s) == marker && text.substring(e, e + m) == marker) {
            val out = text.substring(0, s - m) + text.substring(s, e) + text.substring(e + m)
            return Edited(out, s - m, e - m)
        }
        // Selection includes the markers itself.
        val selected = text.substring(s, e)
        if (selected.length >= 2 * m && selected.startsWith(marker) && selected.endsWith(marker)) {
            val inner = selected.substring(m, selected.length - m)
            return Edited(text.substring(0, s) + inner + text.substring(e), s, s + inner.length)
        }
        if (s == e) {
            val out = text.substring(0, s) + marker + placeholder + marker + text.substring(e)
            return Edited(out, s + m, s + m + placeholder.length)
        }
        val out = text.substring(0, s) + marker + selected + marker + text.substring(e)
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

    private val headingPrefix = Regex("^#{1,6} ")
    private val bulletPrefix = Regex("^(\\s*)[-*+] (?!\\[[ xX]\\] )")
    private val checkPrefix = Regex("^(\\s*)[-*+] \\[[ xX]\\] ")
    private val numberPrefix = Regex("^(\\s*)\\d+\\. ")
    private val quotePrefix = Regex("^> ?")

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

    /** `# ` to `###### `. Choosing the level a line already has removes the heading. */
    fun heading(text: String, start: Int, end: Int, level: Int): Edited {
        val wanted = "#".repeat(level.coerceIn(1, 6)) + " "
        return mapLines(text, start, end) { lines ->
            val allSame = lines.all { it.startsWith(wanted) }
            lines.map { line ->
                val bare = line.replace(headingPrefix, "")
                if (allSame) bare else wanted + bare
            }
        }
    }

    fun bullet(text: String, start: Int, end: Int): Edited = mapLines(text, start, end) { lines ->
        val all = lines.filter { it.isNotBlank() }.all { bulletPrefix.containsMatchIn(it) }
        lines.map { line ->
            when {
                line.isBlank() -> line
                all -> line.replace(bulletPrefix) { it.groupValues[1] }
                else -> "- " + line.replace(checkPrefix) { it.groupValues[1] }.replace(numberPrefix) { it.groupValues[1] }.replace(bulletPrefix) { it.groupValues[1] }
            }
        }
    }

    fun numbered(text: String, start: Int, end: Int): Edited = mapLines(text, start, end) { lines ->
        val all = lines.filter { it.isNotBlank() }.all { numberPrefix.containsMatchIn(it) }
        var n = 0
        lines.map { line ->
            when {
                line.isBlank() -> line
                all -> line.replace(numberPrefix) { it.groupValues[1] }
                else -> { n++; "$n. " + line.replace(bulletPrefix) { it.groupValues[1] }.replace(checkPrefix) { it.groupValues[1] }.replace(numberPrefix) { it.groupValues[1] } }
            }
        }
    }

    fun checklist(text: String, start: Int, end: Int): Edited = mapLines(text, start, end) { lines ->
        val all = lines.filter { it.isNotBlank() }.all { checkPrefix.containsMatchIn(it) }
        lines.map { line ->
            when {
                line.isBlank() -> line
                all -> line.replace(checkPrefix) { it.groupValues[1] }
                else -> "- [ ] " + line.replace(checkPrefix) { it.groupValues[1] }.replace(bulletPrefix) { it.groupValues[1] }.replace(numberPrefix) { it.groupValues[1] }
            }
        }
    }

    fun quote(text: String, start: Int, end: Int): Edited = mapLines(text, start, end) { lines ->
        val all = lines.filter { it.isNotBlank() }.all { quotePrefix.containsMatchIn(it) }
        lines.map { line ->
            when {
                line.isBlank() -> line
                all -> line.replace(quotePrefix, "")
                else -> "> $line"
            }
        }
    }

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

    private val colorOpen = Regex("\\{color:[0-9A-Fa-f]{6}\\|$")
    private val sizeOpen = Regex("\\{size:\\d{1,2}\\|$")
    private val alignTag = Regex("^\\{align:(center|end)\\}")

    /**
     * Gives the selection a colour (`{color:RRGGBB|text|}`), replacing a colour it already has; null removes it. With only a
     * cursor it inserts the placeholder in that colour.
     */
    fun setColor(text: String, start: Int, end: Int, rgb: Int?, placeholder: String = "text"): Edited =
        setTag(text, start, end, colorOpen, rgb?.let { "{color:%06X|".format(it and 0xFFFFFF) }, placeholder)

    /** Same for the font size in sp (8 to 64); null removes it. */
    fun setSize(text: String, start: Int, end: Int, sp: Int?, placeholder: String = "text"): Edited =
        setTag(text, start, end, sizeOpen, sp?.coerceIn(8, 64)?.let { "{size:$it|" }, placeholder)

    private fun setTag(text: String, start: Int, end: Int, existing: Regex, open: String?, placeholder: String): Edited {
        val (s, e) = clamp(text, start, end)
        val before = text.substring(maxOf(0, s - 16), s)
        val old = existing.find(before)
        if (old != null && text.startsWith("|}", e)) {
            val tagStart = s - old.value.length
            val body = text.substring(s, e)
            return if (open == null) {
                Edited(text.substring(0, tagStart) + body + text.substring(e + 2), tagStart, tagStart + body.length)
            } else {
                Edited(text.substring(0, tagStart) + open + body + text.substring(e), tagStart + open.length, tagStart + open.length + body.length)
            }
        }
        if (open == null) return Edited(text, s, e)
        val body = if (s == e) placeholder else text.substring(s, e)
        val out = text.substring(0, s) + open + body + "|}" + text.substring(e)
        return Edited(out, s + open.length, s + open.length + body.length)
    }

    /** Aligns every line the selection touches: start (no tag), centre or end. */
    fun setAlign(text: String, start: Int, end: Int, align: Align): Edited = mapLines(text, start, end) { lines ->
        lines.map { line ->
            val bare = line.replace(alignTag, "")
            when {
                bare.isBlank() -> line
                align == Align.CENTER -> "{align:center}$bare"
                align == Align.END -> "{align:end}$bare"
                else -> bare
            }
        }
    }

    private val letterPrefix = Regex("^(\\s*)[a-z]\\) ")

    /** A lettered list: `a) `, `b) `... Choosing it again on a lettered selection removes it. */
    fun lettered(text: String, start: Int, end: Int): Edited = mapLines(text, start, end) { lines ->
        val all = lines.filter { it.isNotBlank() }.all { letterPrefix.containsMatchIn(it) }
        var n = 0
        lines.map { line ->
            when {
                line.isBlank() -> line
                all -> line.replace(letterPrefix) { it.groupValues[1] }
                else -> {
                    val letter = 'a' + (n % 26)
                    n++
                    "$letter) " + line.replace(bulletPrefix) { it.groupValues[1] }.replace(checkPrefix) { it.groupValues[1] }.replace(numberPrefix) { it.groupValues[1] }
                }
            }
        }
    }
}
