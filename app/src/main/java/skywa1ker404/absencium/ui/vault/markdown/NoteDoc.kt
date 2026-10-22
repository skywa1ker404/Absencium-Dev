package skywa1ker404.absencium.ui.vault.markdown

enum class PartKind { TEXT, EMBED, TABLE }

/**
 * One piece of a note laid out top to bottom: a run of text lines, a picture/doodle/recording line, or a table.
 * [text] is exactly the characters of the note from [start], so a part can be replaced by cutting that range out.
 * [line] is the 0-based line of its first line. [synthetic] marks an empty text part added after a last block that has no
 * line break after it, so there is always a place to type below it.
 */
data class DocPart(val kind: PartKind, val text: String, val start: Int, val line: Int, val synthetic: Boolean = false) {
    val end: Int get() = start + text.length
}

/**
 * Cuts a note into parts so each can be drawn and edited on its own (a text field per text part, a picture view per picture)
 * while the note stays one string. Pure.
 */
object NoteDoc {

    private val embedLine = Regex("^\\{\\{(image|doodle|audio):(\\d{1,18})(?:\\|(\\d{1,3}))?(?:\\|(start|center|end))?\\}\\}$")

    fun isEmbedLine(line: String): Boolean = line.trim().startsWith("{{") && embedLine.matches(line.trim())

    private fun isTableLine(line: String) = line.trimStart().startsWith("|")

    fun split(text: String): List<DocPart> {
        val lines = text.split("\n")
        val parts = ArrayList<DocPart>()
        var offset = 0
        var i = 0
        var textFrom = -1
        var textStartOffset = 0
        var textEndOffset = 0

        fun flushText() {
            if (textFrom >= 0) {
                parts += DocPart(PartKind.TEXT, text.substring(textStartOffset, textEndOffset), textStartOffset, textFrom)
                textFrom = -1
            }
        }

        while (i < lines.size) {
            val line = lines[i]
            val lineStart = offset
            val lineEnd = offset + line.length
            when {
                isEmbedLine(line) -> {
                    flushText()
                    parts += DocPart(PartKind.EMBED, line, lineStart, i)
                    offset = lineEnd + 1
                    i++
                }
                isTableLine(line) -> {
                    flushText()
                    var j = i
                    var end = lineEnd
                    var next = lineEnd + 1
                    while (j + 1 < lines.size && isTableLine(lines[j + 1])) {
                        j++
                        end = next + lines[j].length
                        next = end + 1
                    }
                    parts += DocPart(PartKind.TABLE, text.substring(lineStart, end), lineStart, i)
                    offset = end + 1
                    i = j + 1
                }
                else -> {
                    if (textFrom < 0) {
                        textFrom = i
                        textStartOffset = lineStart
                    }
                    textEndOffset = lineEnd
                    offset = lineEnd + 1
                    i++
                }
            }
        }
        flushText()
        if (parts.isEmpty()) {
            parts += DocPart(PartKind.TEXT, "", 0, 0)
        } else if (parts.last().kind != PartKind.TEXT) {
            // A last block with nothing after it: give the cursor a place below it.
            parts += DocPart(PartKind.TEXT, "", text.length, lines.size, synthetic = true)
        }
        return parts
    }

    /** The index of the part that holds [offset] (the part before it when the offset is exactly between two). */
    fun partIndexAt(parts: List<DocPart>, offset: Int): Int {
        for (i in parts.indices.reversed()) if (offset >= parts[i].start) return i
        return 0
    }

    /** The note with part [index] replaced by [newText]. */
    fun replacePart(text: String, parts: List<DocPart>, index: Int, newText: String): String {
        val part = parts.getOrNull(index) ?: return text
        if (part.synthetic) return if (newText.isEmpty()) text else text + "\n" + newText
        return text.substring(0, part.start) + newText + text.substring(part.end.coerceAtMost(text.length))
    }

    /** Global offset of [local] inside part [index]. A synthetic part sits after the line break that [replacePart] adds. */
    fun globalOffset(parts: List<DocPart>, index: Int, local: Int): Int {
        val part = parts.getOrNull(index) ?: return 0
        return part.start + local + if (part.synthetic) 1 else 0
    }

    /** The nearest text part to [index], looking down first, then up. */
    fun nearestText(parts: List<DocPart>, index: Int): Int {
        val i = index.coerceIn(0, parts.lastIndex)
        if (parts[i].kind == PartKind.TEXT) return i
        for (d in 1..parts.size) {
            parts.getOrNull(i + d)?.takeIf { it.kind == PartKind.TEXT }?.let { return i + d }
            parts.getOrNull(i - d)?.takeIf { it.kind == PartKind.TEXT }?.let { return i - d }
        }
        return 0
    }
}
