package skywa1ker404.absencium.ui.vault

import skywa1ker404.absencium.ui.vault.markdown.Block
import skywa1ker404.absencium.ui.vault.markdown.MarkdownParse
import skywa1ker404.absencium.ui.vault.markdown.Span

private fun List<Span>.plain(): String = joinToString("") { it.text }.replace(Regex("\\s+"), " ").trim()

/**
 * What a note card shows of its body: the first few lines as plain text, read with the same parser as the preview page,
 * so style markers (`==`, `{size:30|`, `**`) never show up as raw characters. A checklist keeps its boxes; pictures,
 * doodles and recordings are left out. Never throws.
 */
fun notePreviewLines(body: String, maxLines: Int = 4, maxChars: Int = 90): List<String> {
    if (body.isBlank()) return emptyList()
    val out = ArrayList<String>()
    for (block in MarkdownParse.blocks(body.take(4_000))) {
        if (out.size >= maxLines) break
        val line = when (block) {
            is Block.Heading -> block.spans.plain()
            is Block.Paragraph -> block.spans.plain()
            is Block.Bullet -> "• " + block.spans.plain()
            is Block.Numbered -> block.number + ". " + block.spans.plain()
            is Block.Check -> (if (block.checked) "☑ " else "☐ ") + block.spans.plain()
            is Block.Quote -> block.spans.plain()
            is Block.Table -> block.rows.firstOrNull()?.joinToString(" · ") { it.plain() }.orEmpty()
            is Block.Code -> block.text.lineSequence().firstOrNull().orEmpty().trim()
            is Block.Embed, Block.Rule -> ""
        }
        if (line.isNotBlank()) out += if (line.length > maxChars) line.take(maxChars).trimEnd() + "…" else line
    }
    return out
}
