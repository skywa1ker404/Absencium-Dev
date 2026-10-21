package skywa1ker404.absencium.ui.vault.markdown

/** A run of inline text with its style. [link] is set for a `[[Note title]]` link and holds the title. */
data class Span(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val code: Boolean = false,
    val strike: Boolean = false,
    val link: String? = null,
)

sealed interface Block {
    data class Heading(val level: Int, val spans: List<Span>) : Block
    data class Paragraph(val spans: List<Span>) : Block
    data class Bullet(val indent: Int, val spans: List<Span>) : Block
    data class Numbered(val indent: Int, val number: String, val spans: List<Span>) : Block
    /** [line] is the 0-based line of the source text, so tapping the box can flip it in the source. */
    data class Check(val indent: Int, val checked: Boolean, val line: Int, val spans: List<Span>) : Block
    data class Quote(val spans: List<Span>) : Block
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
    private val rule = Regex("^\\s*([-*_])(\\s*\\1){2,}\\s*$")

    fun blocks(text: String): List<Block> {
        val out = mutableListOf<Block>()
        val lines = text.split("\n")
        var i = 0
        val paragraph = StringBuilder()
        fun flush() {
            if (paragraph.isNotEmpty()) {
                out += Block.Paragraph(inline(paragraph.toString()))
                paragraph.clear()
            }
        }
        while (i < lines.size) {
            val line = lines[i].trimEnd('\r')
            when {
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
                rule.matches(line) -> { flush(); out += Block.Rule }
                heading.matches(line) -> {
                    flush()
                    val m = heading.find(line)!!
                    out += Block.Heading(m.groupValues[1].length, inline(m.groupValues[2]))
                }
                check.matches(line) -> {
                    flush()
                    val m = check.find(line)!!
                    out += Block.Check(m.groupValues[1].length, m.groupValues[2] != " ", i, inline(m.groupValues[3]))
                }
                bullet.matches(line) -> {
                    flush()
                    val m = bullet.find(line)!!
                    out += Block.Bullet(m.groupValues[1].length, inline(m.groupValues[2]))
                }
                numbered.matches(line) -> {
                    flush()
                    val m = numbered.find(line)!!
                    out += Block.Numbered(m.groupValues[1].length, m.groupValues[2], inline(m.groupValues[3]))
                }
                line.startsWith(">") -> {
                    flush()
                    out += Block.Quote(inline(line.removePrefix(">").trimStart()))
                }
                else -> {
                    if (paragraph.isNotEmpty()) paragraph.append('\n')
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
