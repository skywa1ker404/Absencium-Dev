package skywa1ker404.absencium.ui.vault.markdown

/** A run of inline text with its style. [link] is set for a `[[Note title]]` link and holds the title. */
data class Span(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val code: Boolean = false,
    val strike: Boolean = false,
    val link: String? = null,
    val underline: Boolean = false,
    val highlight: Boolean = false,
    /** Text colour as 0xRRGGBB, or null for the theme's text colour. */
    val color: Int? = null,
    /** Font size in sp, or null for the normal size. */
    val sizeSp: Int? = null,
)

/** Where a block sits across the page. */
enum class Align { START, CENTER, END }

/** What a block embedded in a note shows. [Block.Embed.id] is the attachment id (image, doodle) or audio memo id. */
enum class EmbedKind { IMAGE, DOODLE, AUDIO }

sealed interface Block {
    data class Heading(val level: Int, val spans: List<Span>, val align: Align = Align.START) : Block
    data class Paragraph(val spans: List<Span>, val align: Align = Align.START) : Block
    data class Bullet(val indent: Int, val spans: List<Span>, val align: Align = Align.START) : Block
    /** [number] is `1`, `2`... or a letter for a lettered list. */
    data class Numbered(val indent: Int, val number: String, val spans: List<Span>, val align: Align = Align.START) : Block
    /** [line] is the 0-based line of the source text, so tapping the box can flip it in the source. */
    data class Check(val indent: Int, val checked: Boolean, val line: Int, val spans: List<Span>, val align: Align = Align.START) : Block
    data class Quote(val spans: List<Span>, val align: Align = Align.START) : Block
    /** A picture, doodle or recording on its own line: `{{image:12|60|center}}` = id 12, 60 % of the page width, centred. [line] is its source line. */
    data class Embed(val kind: EmbedKind, val id: Long, val widthPercent: Int, val align: Align, val line: Int) : Block
    /** A table written as `| a | b |` lines. [startLine] is its first source line; [rows] holds the cells as styled text. */
    data class Table(val startLine: Int, val hasHeader: Boolean, val rows: List<List<List<Span>>>) : Block
    data class Code(val text: String) : Block
    data object Rule : Block
}

/**
 * A small Markdown reader for the notes preview: headings, bullet, numbered and check lists, quotes, fenced code, rules, and
 * inline bold, italic, code, strike-through and `[[note links]]`. Anything else is shown as plain text, never dropped.
 * It does not try to be CommonMark; it covers what the toolbar writes. It never throws and never loops on odd input.
 */
object MarkdownParse {

    private val heading = Regex("^(#{1,6}) +(.*)$")
    private val check = Regex("^(\\s*)[-*+] \\[([ xX])\\] ?(.*)$")
    private val bullet = Regex("^(\\s*)[-*+] +(.*)$")
    private val numbered = Regex("^(\\s*)(\\d{1,9})[.)] +(.*)$")
    private val lettered = Regex("^(\\s*)([a-z])\\) +(.*)$")
    private val embed = Regex("^\\{\\{(image|doodle|audio):(\\d{1,18})(?:\\|(\\d{1,3}))?(?:\\|(start|center|end))?\\}\\}$")
    private val alignPrefix = Regex("^\\{align:(center|end)\\}")
    /** Three or more of the same `-`, `*` or `_`, with spaces allowed between. A loop, not a regex: a repeated regex group recurses and overflows on a very long line. */
    private fun isRule(line: String): Boolean {
        var mark = ' '
        var count = 0
        for (ch in line) {
            if (ch.isWhitespace()) continue
            if (ch != '-' && ch != '*' && ch != '_') return false
            if (mark == ' ') mark = ch else if (ch != mark) return false
            count++
        }
        return count >= 3
    }

    private fun alignOf(name: String?): Align = when (name) {
        "center" -> Align.CENTER
        "end" -> Align.END
        else -> Align.START
    }

    private fun isTableLine(line: String) = line.trimStart().startsWith("|")

    private fun cells(line: String): List<String> {
        var body = line.trim()
        if (body.startsWith("|")) body = body.substring(1)
        if (body.endsWith("|")) body = body.substring(0, body.length - 1)
        return body.split("|").map { it.trim() }
    }

    private fun isSeparatorRow(cells: List<String>): Boolean =
        cells.isNotEmpty() && cells.all { c -> c.length >= 3 && c.all { it == '-' || it == ':' } && c.contains('-') }

    fun blocks(text: String): List<Block> {
        val out = mutableListOf<Block>()
        val lines = text.split("\n")
        var i = 0
        val paragraph = StringBuilder()
        var paragraphAlign = Align.START
        fun flush() {
            if (paragraph.isNotEmpty()) {
                out += Block.Paragraph(inline(paragraph.toString()), paragraphAlign)
                paragraph.clear()
                paragraphAlign = Align.START
            }
        }
        while (i < lines.size) {
            val rawLine = lines[i].trimEnd('\r')
            var align = Align.START
            var line = rawLine
            if (line.startsWith("{align:")) {
                val m = alignPrefix.find(line)
                if (m != null) { align = alignOf(m.groupValues[1]); line = line.substring(m.value.length) }
            }
            when {
                line.trim().startsWith("{{") && embed.matches(line.trim()) -> {
                    flush()
                    val m = embed.find(line.trim())!!
                    val kind = EmbedKind.valueOf(m.groupValues[1].uppercase())
                    val width = m.groupValues[3].toIntOrNull()?.coerceIn(10, 100) ?: 100
                    out += Block.Embed(kind, m.groupValues[2].toLong(), width, alignOf(m.groupValues[4]).let { if (m.groupValues[4].isEmpty()) Align.CENTER else it }, i)
                }
                isTableLine(line) -> {
                    flush()
                    val start = i
                    val rows = mutableListOf<List<String>>()
                    while (i < lines.size && isTableLine(lines[i].trimEnd('\r'))) { rows += cells(lines[i].trimEnd('\r')); i++ }
                    i-- // the loop below adds one
                    val header = rows.size >= 2 && isSeparatorRow(rows[1])
                    val body = rows.filterIndexed { index, _ -> !(header && index == 1) }
                    out += Block.Table(start, header, body.map { row -> row.map { inline(it) } })
                }
                line.trimStart().startsWith("```") -> {
                line.trimStart().startsWith("```") -> {
                    flush()
                    val code = StringBuilder()
                    i++
                    // An unclosed fence runs to the end of the text.
                    while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                        if (code.isNotEmpty()) code.append('\n')
                        code.append(lines[i].trimEnd('\r'))
                        i++
                    }
                    out += Block.Code(code.toString())
                }
                line.isBlank() -> flush()
                isRule(line) -> { flush(); out += Block.Rule }
                heading.matches(line) -> {
                    flush()
                    val m = heading.find(line)!!
                    out += Block.Heading(m.groupValues[1].length, inline(m.groupValues[2]), align)
                }
                check.matches(line) -> {
                    flush()
                    val m = check.find(line)!!
                    out += Block.Check(m.groupValues[1].length, m.groupValues[2] != " ", i, inline(m.groupValues[3]), align)
                }
                bullet.matches(line) -> {
                    flush()
                    val m = bullet.find(line)!!
                    out += Block.Bullet(m.groupValues[1].length, inline(m.groupValues[2]), align)
                }
                numbered.matches(line) -> {
                    flush()
                    val m = numbered.find(line)!!
                    out += Block.Numbered(m.groupValues[1].length, m.groupValues[2], inline(m.groupValues[3]), align)
                }
                lettered.matches(line) -> {
                    flush()
                    val m = lettered.find(line)!!
                    out += Block.Numbered(m.groupValues[1].length, m.groupValues[2], inline(m.groupValues[3]), align)
                }
                line.startsWith(">") -> {
                    flush()
                    out += Block.Quote(inline(line.removePrefix(">").trimStart()), align)
                }
                else -> {
                    if (paragraph.isNotEmpty()) paragraph.append('\n') else paragraphAlign = align
                    paragraph.append(line)
                }
            }
            i++
        }
        flush()
        return out
    }

    /** Where [marker] next occurs at or after [from], looking at most this far ahead, so unmatched markers cannot make a long note slow. */
    private const val LOOK_AHEAD = 2000

    private fun find(text: String, marker: String, from: Int): Int {
        if (from >= text.length) return -1
        val end = minOf(text.length, from + LOOK_AHEAD)
        val hit = text.substring(from, end).indexOf(marker)
        return if (hit < 0) -1 else from + hit
    }

    /** Splits [text] into styled runs. Unmatched markers stay as literal characters. Linear in the length of the text. */
    fun inline(text: String): List<Span> {
        val spans = mutableListOf<Span>()
        val plain = StringBuilder()
        fun flushPlain() {
            if (plain.isNotEmpty()) {
                spans += Span(plain.toString())
                plain.clear()
            }
        }
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("[[", i) -> {
                    val close = find(text, "]]", i + 2)
                    if (close > i + 2 && text.substring(i, close).indexOf('\n') < 0) {
                        flushPlain()
                        val title = text.substring(i + 2, close)
                        spans += Span(title, link = title)
                        i = close + 2
                    } else { plain.append(text[i]); i++ }
                }
                text[i] == '`' -> {
                    val close = find(text, "`", i + 1)
                    if (close > i + 1) {
                        flushPlain()
                        spans += Span(text.substring(i + 1, close), code = true)
                        i = close + 1
                    } else { plain.append(text[i]); i++ }
                }
                text.startsWith("**", i) -> {
                    val close = find(text, "**", i + 2)
                    if (close > i + 2) {
                        flushPlain()
                        inline(text.substring(i + 2, close)).forEach { spans += it.copy(bold = true) }
                        i = close + 2
                    } else { plain.append("**"); i += 2 }
                }
                text.startsWith("~~", i) -> {
                    val close = find(text, "~~", i + 2)
                    if (close > i + 2) {
                        flushPlain()
                        inline(text.substring(i + 2, close)).forEach { spans += it.copy(strike = true) }
                        i = close + 2
                    } else { plain.append("~~"); i += 2 }
                }
                text.startsWith("++", i) || text.startsWith("==", i) -> {
                    val marker = text.substring(i, i + 2)
                    val close = if (i + 2 < text.length && !text[i + 2].isWhitespace()) find(text, marker, i + 2) else -1
                    if (close > i + 2 && !text[close - 1].isWhitespace()) {
                        flushPlain()
                        val inner = inline(text.substring(i + 2, close))
                        inner.forEach { spans += if (marker == "++") it.copy(underline = true) else it.copy(highlight = true) }
                        i = close + 2
                    } else { plain.append(marker); i += 2 }
                }
                text.startsWith("{color:", i) || text.startsWith("{size:", i) -> {
                    val bar = find(text, "|", i)
                    val isColor = text.startsWith("{color:", i)
                    val argStart = i + if (isColor) 7 else 6
                    val close = if (bar in (argStart + 1)..(argStart + 7)) find(text, "|}", bar + 1) else -1
                    val arg = if (close > 0) text.substring(argStart, bar) else ""
                    val colorValue = if (isColor && arg.length == 6) arg.toIntOrNull(16) else null
                    val sizeValue = if (!isColor) arg.toIntOrNull()?.takeIf { it in 8..64 } else null
                    if (close > bar && (colorValue != null || sizeValue != null)) {
                        flushPlain()
                        inline(text.substring(bar + 1, close)).forEach {
                            spans += if (colorValue != null) it.copy(color = colorValue) else it.copy(sizeSp = sizeValue)
                        }
                        i = close + 2
                    } else { plain.append(text[i]); i++ }
                }
                text[i] == '*' && i + 1 < text.length && !text[i + 1].isWhitespace() -> {
                    val close = find(text, "*", i + 1)
                    if (close > i + 1 && !text[close - 1].isWhitespace()) {
                        flushPlain()
                        inline(text.substring(i + 1, close)).forEach { spans += it.copy(italic = true) }
                        i = close + 1
                    } else { plain.append(text[i]); i++ }
                }
                else -> { plain.append(text[i]); i++ }
            }
        }
        flushPlain()
        return spans
    }
}
