package skywa1ker404.absencium.data.share

import skywa1ker404.absencium.ui.vault.markdown.NoteTags

/** File names and the Markdown text of an exported note. Pure, so the rules are unit-tested without Android. */
object ExportNames {

    private const val MAX_BASE = 80
    private val forbidden = Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]")

    /** A name safe on every file system: no path characters, no control characters, no leading dots, never empty, not too long. */
    fun safeBase(title: String, fallback: String = "Untitled note"): String {
        val cleaned = title.replace(forbidden, " ").replace(Regex("\\s+"), " ").trim().trimStart('.').trim().trimEnd('.')
        val base = cleaned.ifEmpty { fallback }
        // Cut on a code point boundary so an emoji is never split in half.
        return if (base.length <= MAX_BASE) base else base.substring(0, if (Character.isHighSurrogate(base[MAX_BASE - 1])) MAX_BASE - 1 else MAX_BASE).trim()
    }

    fun fileName(title: String, extension: String): String = safeBase(title) + "." + extension.trimStart('.')

    /** Makes every name in [names] unique by adding " (2)", " (3)" before the extension, keeping the order. Case-insensitive. */
    fun unique(names: List<String>): List<String> {
        val seen = HashSet<String>()
        return names.map { name ->
            var candidate = name
            var n = 2
            val dot = name.lastIndexOf('.').takeIf { it > 0 }
            while (!seen.add(candidate.lowercase())) {
                candidate = if (dot == null) "$name ($n)" else name.substring(0, dot) + " ($n)" + name.substring(dot)
                n++
            }
            candidate
        }
    }

    /** The note as a Markdown file: its title as the heading, its tags, then the text. */
    fun markdown(title: String, tags: String, body: String): String = buildString {
        val heading = title.trim()
        if (heading.isNotEmpty()) append("# ").append(heading).append("\n\n")
        val list = NoteTags.parse(tags)
        if (list.isNotEmpty()) append(list.joinToString(" ") { "#" + it.replace(' ', '-') }).append("\n\n")
        append(body.trimEnd()).append('\n')
    }

    private val embedLine = Regex("^\\{\\{(image|doodle|audio):(\\d{1,18})(?:\\|\\d{1,3})?(?:\\|(?:start|center|end))?\\}\\}$")
    private val alignPrefix = Regex("^\\{align:(center|end)\\}")
    private val colourSpan = Regex("\\{color:([0-9A-Fa-f]{6})\\|(.*?)\\|\\}")
    private val sizeSpan = Regex("\\{size:(\\d{1,2})\\|(.*?)\\|\\}")
    private val underlineSpan = Regex("\\+\\+(.+?)\\+\\+")
    private val highlightSpan = Regex("==(.+?)==")
    private val noteLink = Regex("\\[\\[(.+?)\\]\\]")

    /**
     * The note body in Markdown that other apps can read. Picture, doodle and recording lines become image and file links through
     * [resolve] (null when the file is not part of this export; a short italic note stands in). Colours, sizes, underline, highlight
     * and alignment, which Markdown has no marks for, become the small HTML tags most Markdown viewers understand. Pure.
     */
    fun portableMarkdown(body: String, resolve: (kind: String, id: Long) -> String?): String =
        body.lines().joinToString("\n") { raw ->
            val embed = embedLine.matchEntire(raw.trim())
            if (embed != null) {
                val kind = embed.groupValues[1]
                val target = resolve(kind, embed.groupValues[2].toLong())
                val label = when (kind) { "doodle" -> "Doodle"; "audio" -> "Recording"; else -> "Picture" }
                return@joinToString when {
                    target == null -> "*[$label not included in this file. Export as ZIP to include it.]*"
                    kind == "audio" -> "[$label]($target)"
                    else -> "![$label]($target)"
                }
            }
            var line = raw
            var align: String? = null
            alignPrefix.find(line)?.let {
                align = if (it.groupValues[1] == "end") "right" else "center"
                line = line.removeRange(it.range)
            }
            line = line
                .replace(colourSpan) { "<span style=\"color:#${it.groupValues[1]}\">${it.groupValues[2]}</span>" }
                .replace(sizeSpan) { "<span style=\"font-size:${it.groupValues[1]}px\">${it.groupValues[2]}</span>" }
                .replace(underlineSpan) { "<u>${it.groupValues[1]}</u>" }
                .replace(highlightSpan) { "<mark>${it.groupValues[1]}</mark>" }
                .replace(noteLink) { it.groupValues[1] }
            val side = align
            if (side != null && line.isNotBlank()) "<p align=\"$side\">$line</p>" else line
        }

    /** The note as plain text: no Markdown marks are removed (they stay readable), only the heading and tags are added. */
    fun plainText(title: String, tags: String, body: String): String = markdown(title, tags, body)
}
