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

    /** The note as plain text: no Markdown marks are removed (they stay readable), only the heading and tags are added. */
    fun plainText(title: String, tags: String, body: String): String = markdown(title, tags, body)
}
