package me.rerere.rikkahub.ui.components.chat

/** A link the student attached from the host's Vault: kind is NOTE, SYLLABUS or MATERIAL; [token] is the text kept in the message. */
data class VaultRef(val kind: String, val label: String, val token: String, val id: Long? = null)

private val refPattern = Regex("""\[Vault (note|syllabus|material): ([^\]]*)]""")

/** Splits the Vault link tokens out of a message text: the links, and the text without them. */
fun splitVaultRefs(text: String): Pair<List<VaultRef>, String> {
    val refs = refPattern.findAll(text).map {
        val label = it.groupValues[2].substringBefore(" (id ").trim()
        VaultRef(it.groupValues[1].uppercase(), label, it.value, Regex("""\(id (\d+)\)""").find(it.groupValues[2])?.groupValues?.get(1)?.toLongOrNull())
    }.toList()
    if (refs.isEmpty()) return emptyList<VaultRef>() to text
    return refs to text.replace(refPattern, "").lines().joinToString("\n") { it.trimEnd() }.trim()
}
