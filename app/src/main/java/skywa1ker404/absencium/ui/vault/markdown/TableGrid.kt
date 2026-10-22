package skywa1ker404.absencium.ui.vault.markdown

/** A table as plain cells, for the table editor: read from the `| a | b |` lines of a note and written back as the same lines. Pure. */
object TableGrid {

    private val separatorCell = Regex("^:?-{1,}:?$")

    /** The cells of every row except the `| --- |` line, padded to the same width. Always at least one row and one column. */
    fun parse(text: String): List<List<String>> {
        val rows = ArrayList<List<String>>()
        for (raw in text.split("\n")) {
            val line = raw.trim()
            if (!line.startsWith("|")) continue
            var inner = line.removePrefix("|")
            if (inner.endsWith("|")) inner = inner.removeSuffix("|")
            val cells = inner.split("|").map { it.trim() }
            if (cells.all { it.matches(separatorCell) }) continue
            rows += cells
        }
        val width = (rows.maxOfOrNull { it.size } ?: 1).coerceAtLeast(1)
        val padded = rows.map { r -> r + List(width - r.size) { "" } }
        return padded.ifEmpty { listOf(List(width) { "" }) }
    }

    /** The table lines for [rows]: the first row is the header, followed by the separator line. A `|` or line break inside a cell becomes a space-safe character. */
    fun build(rows: List<List<String>>): String {
        val width = (rows.maxOfOrNull { it.size } ?: 1).coerceAtLeast(1)
        fun line(row: List<String>) = "|" + (0 until width).joinToString("") { " ${clean(row.getOrElse(it) { "" })} |" }
        val body = rows.ifEmpty { listOf(List(width) { "" }) }
        return buildList {
            add(line(body[0]))
            add("|" + " --- |".repeat(width))
            for (i in 1 until body.size) add(line(body[i]))
        }.joinToString("\n")
    }

    private fun clean(cell: String) = cell.replace("\r", " ").replace("\n", " ").replace("|", "/").trim()
}
