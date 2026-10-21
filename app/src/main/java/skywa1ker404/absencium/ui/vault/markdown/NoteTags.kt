package skywa1ker404.absencium.ui.vault.markdown

/** Tags are stored as one comma separated string. These rules keep them tidy: trimmed, no `#`, no repeats, case-insensitive. */
object NoteTags {
    const val MAX_LENGTH = 32
    const val MAX_TAGS = 20

    fun clean(raw: String): String =
        raw.trim().removePrefix("#").replace(',', ' ').replace(Regex("\\s+"), " ").trim().take(MAX_LENGTH)

    fun parse(stored: String): List<String> = stored.split(',').map { clean(it) }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }

    fun join(tags: List<String>): String = tags.map { clean(it) }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }.take(MAX_TAGS).joinToString(",")

    /** Adds [raw] unless it is empty or already there (ignoring case). */
    fun add(tags: List<String>, raw: String): List<String> {
        val tag = clean(raw)
        if (tag.isEmpty() || tags.any { it.equals(tag, ignoreCase = true) } || tags.size >= MAX_TAGS) return tags
        return tags + tag
    }

    /** Known tags that start with what was typed (or contain it, after those), most used first, without those already chosen. */
    fun suggestions(typed: String, allNotesTags: List<String>, chosen: List<String>, limit: Int = 6): List<String> {
        val query = clean(typed).lowercase()
        val counts = LinkedHashMap<String, Int>()
        val display = HashMap<String, String>()
        allNotesTags.flatMap { parse(it) }.forEach {
            val k = it.lowercase()
            counts[k] = (counts[k] ?: 0) + 1
            display.putIfAbsent(k, it)
        }
        val taken = chosen.map { it.lowercase() }.toSet()
        return counts.entries
            .filter { it.key !in taken && (query.isEmpty() || it.key.contains(query)) }
            .sortedWith(compareBy<Map.Entry<String, Int>> { if (query.isNotEmpty() && it.key.startsWith(query)) 0 else 1 }.thenByDescending { it.value }.thenBy { it.key })
            .take(limit)
            .map { display.getValue(it.key) }
    }
}
