package skywa1ker404.absencium.ui.vault.markdown

/**
 * Editing rules for the blocks that sit on their own lines in a note: pictures, doodles, recordings and tables. The note text
 * stays the only source of truth; a block is moved, resized or removed by changing its lines. Every function is pure.
 */
object NoteBlocks {

    private val embedLine = Regex("^\\{\\{(image|doodle|audio):(\\d{1,18})(?:\\|(\\d{1,3}))?(?:\\|(start|center|end))?\\}\\}$")

    fun token(kind: EmbedKind, id: Long, widthPercent: Int = 100, align: Align = Align.CENTER): String =
        "{{${kind.name.lowercase()}:$id|${widthPercent.coerceIn(10, 100)}|${align.name.lowercase()}}}"

    /** Puts [token] on a line of its own at the cursor, splitting the line it lands in. The cursor ends on the line after it. */
    fun insertEmbed(text: String, cursor: Int, token: String): Edited {
        val at = cursor.coerceIn(0, text.length)
        val before = text.substring(0, at)
        val after = text.substring(at)
        val lead = if (before.isEmpty() || before.endsWith("\n")) "" else "\n"
        val tail = if (after.startsWith("\n")) "" else "\n"
        val out = before + lead + token + tail + after
        val pos = before.length + lead.length + token.length + 1
        return Edited(out, pos.coerceAtMost(out.length), pos.coerceAtMost(out.length))
    }

    private fun linesOf(text: String) = text.split("\n").toMutableList()

    private fun isEmbed(line: String) = embedLine.matches(line.trim())

    /** Changes the width and alignment of the block on [line]. Another kind of line is left alone. */
    fun resize(text: String, line: Int, widthPercent: Int, align: Align): String {
        val lines = linesOf(text)
        val l = lines.getOrNull(line)?.trim() ?: return text
        val m = embedLine.find(l) ?: return text
        val kind = EmbedKind.valueOf(m.groupValues[1].uppercase())
        lines[line] = token(kind, m.groupValues[2].toLong(), widthPercent, align)
        return lines.joinToString("\n")
    }

    /** Removes the block on [line] together with its line break. */
    fun remove(text: String, line: Int): String {
        val lines = linesOf(text)
        if (lines.getOrNull(line)?.let { isEmbed(it) } != true) return text
        lines.removeAt(line)
        return lines.joinToString("\n")
    }

    /**
     * Moves the block on [line] one place up (-1) or down (+1) past the nearest non-blank line, so the text around it flows
     * to the other side. Returns the new text and the line the block is on now.
     */
    fun move(text: String, line: Int, direction: Int): Pair<String, Int> {
        val lines = linesOf(text)
        if (lines.getOrNull(line)?.let { isEmbed(it) } != true || direction == 0) return text to line
        var j = line + direction.coerceIn(-1, 1)
        while (j in lines.indices && lines[j].isBlank()) j += direction.coerceIn(-1, 1)
        if (j !in lines.indices) return text to line
        val moved = lines.removeAt(line)
        lines.add(j, moved)
        return lines.joinToString("\n") to j
    }

    /**
     * Takes the [count] lines that start at [line] (a picture line, or a whole table) and puts them in front of line [before]
     * (numbered as in [text] now; the text's line count means "at the end"). Returns the new text and the line the block starts on.
     */
    fun moveToLine(text: String, line: Int, count: Int, before: Int): Pair<String, Int> {
        val lines = linesOf(text)
        if (count < 1 || line !in lines.indices || line + count > lines.size) return text to line
        val target = before.coerceIn(0, lines.size)
        if (target in line..(line + count)) return text to line
        val block = ArrayList(lines.subList(line, line + count))
        repeat(count) { lines.removeAt(line) }
        val at = if (target > line) target - count else target
        lines.addAll(at, block)
        return lines.joinToString("\n") to at
    }

    /** How many lines the table that starts at [startLine] has, header and separator included; 0 when no table starts there. */
    fun tableLineCount(text: String, startLine: Int): Int {
        val lines = linesOf(text)
        val t = locate(lines, startLine) ?: return 0
        return t.to - t.from + 1
    }

    /**
     * Gives the table that starts at [startLine] exactly [rows] rows (the header counts as one). Growing adds empty rows; shrinking
     * only removes rows from the bottom that hold nothing, so written rows are never lost. Returns [text] when nothing changed.
     */
    fun setTableRows(text: String, startLine: Int, rows: Int): String = edit(text, startLine) { t ->
        val target = rows.coerceIn(1, 50)
        val width = t.cells.maxOfOrNull { it.size } ?: 1
        var changed = false
        while (t.cells.size < target) {
            t.cells.add(MutableList(width) { "" })
            changed = true
        }
        while (t.cells.size > target && t.cells.size > 1 && t.cells.last().all { it.isBlank() }) {
            t.cells.removeAt(t.cells.size - 1)
            changed = true
        }
        changed
    }

    // ---- tables ----

    /** An empty table of [rows] by [columns] with a header row. */
    fun newTable(rows: Int, columns: Int): String {
        val r = rows.coerceIn(1, 50)
        val c = columns.coerceIn(1, 12)
        fun row(cell: String) = "|" + (" $cell |").repeat(c)
        val header = "|" + " Column |".repeat(c)
        val separator = "|" + " --- |".repeat(c)
        return buildString {
            append(header).append('\n').append(separator)
            repeat(r - 1) { append('\n').append(row("")) }
        }
    }

    /** The table that starts at [startLine]: its line range and its cells, header separator row removed. */
    private class Located(val from: Int, val to: Int, val header: Boolean, val cells: MutableList<MutableList<String>>)

    private fun cellsOf(line: String): MutableList<String> {
        var body = line.trim()
        if (body.startsWith("|")) body = body.substring(1)
        if (body.endsWith("|")) body = body.substring(0, body.length - 1)
        return body.split("|").map { it.trim() }.toMutableList()
    }

    private fun locate(lines: List<String>, startLine: Int): Located? {
        if (lines.getOrNull(startLine)?.trimStart()?.startsWith("|") != true) return null
        var end = startLine
        while (end + 1 < lines.size && lines[end + 1].trimStart().startsWith("|")) end++
        val rows = (startLine..end).map { cellsOf(lines[it]) }
        val header = rows.size >= 2 && rows[1].all { c -> c.length >= 3 && c.all { it == '-' || it == ':' } && c.contains('-') }
        val body = rows.filterIndexed { i, _ -> !(header && i == 1) }.map { it.toMutableList() }.toMutableList()
        return Located(startLine, end, header, body)
    }

    private fun write(lines: MutableList<String>, t: Located): String {
        val width = t.cells.maxOfOrNull { it.size } ?: 1
        fun render(row: List<String>) = "|" + (0 until width).joinToString("") { " ${row.getOrElse(it) { "" }} |" }
        val out = mutableListOf<String>()
        t.cells.forEachIndexed { i, row ->
            out += render(row)
            if (t.header && i == 0) out += "|" + " --- |".repeat(width)
        }
        repeat(t.to - t.from + 1) { lines.removeAt(t.from) }
        lines.addAll(t.from, out)
        return lines.joinToString("\n")
    }

    private fun edit(text: String, startLine: Int, change: (Located) -> Boolean): String {
        val lines = linesOf(text)
        val t = locate(lines, startLine) ?: return text
        if (!change(t)) return text
        return write(lines, t)
    }

    /** Adds an empty row after row [after] (0-based; -1 adds at the top, below a header when there is one). */
    fun addRow(text: String, startLine: Int, after: Int): String = edit(text, startLine) { t ->
        val width = t.cells.maxOfOrNull { it.size } ?: 1
        val index = (after + 1).coerceIn(if (t.header) 1 else 0, t.cells.size)
        t.cells.add(index, MutableList(width) { "" })
        true
    }

    /** Removes row [row]. The last row of a table is never removed, and neither is the header row. */
    fun deleteRow(text: String, startLine: Int, row: Int): String = edit(text, startLine) { t ->
        if (t.cells.size <= 1 || row !in t.cells.indices || (t.header && row == 0)) false else { t.cells.removeAt(row); true }
    }

    /** Adds an empty column after column [after] (-1 adds at the left). */
    fun addColumn(text: String, startLine: Int, after: Int): String = edit(text, startLine) { t ->
        val width = t.cells.maxOfOrNull { it.size } ?: 1
        if (width >= 12) return@edit false
        t.cells.forEach { row ->
            while (row.size < width) row.add("")
            row.add((after + 1).coerceIn(0, row.size), "")
        }
        true
    }

    /** Removes column [column]. The last column is never removed. */
    fun deleteColumn(text: String, startLine: Int, column: Int): String = edit(text, startLine) { t ->
        val width = t.cells.maxOfOrNull { it.size } ?: 1
        if (width <= 1 || column !in 0 until width) false else { t.cells.forEach { if (column < it.size) it.removeAt(column) }; true }
    }

    /** Sets the text of one cell. A `|` or line break in [value] is replaced by a space, because either would break the table. */
    fun setCell(text: String, startLine: Int, row: Int, column: Int, value: String): String = edit(text, startLine) { t ->
        if (row !in t.cells.indices || column < 0 || column >= 12) return@edit false
        val cells = t.cells[row]
        while (cells.size <= column) cells.add("")
        cells[column] = value.replace('|', ' ').replace('\n', ' ').trim()
        true
    }
}
