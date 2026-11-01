package skywa1ker404.absencium.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import skywa1ker404.absencium.ui.components.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import skywa1ker404.absencium.data.db.NoteEntity
import skywa1ker404.absencium.ui.components.AppTextField
import skywa1ker404.absencium.ui.components.ExpressiveChip
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.vault.markdown.Align
import skywa1ker404.absencium.ui.vault.markdown.Block
import skywa1ker404.absencium.ui.vault.markdown.MarkdownParse
import skywa1ker404.absencium.ui.vault.markdown.NoteTags
import skywa1ker404.absencium.ui.vault.markdown.NoteTemplate
import skywa1ker404.absencium.ui.vault.markdown.NoteTemplates
import skywa1ker404.absencium.ui.vault.markdown.Span

/** What a toolbar button does. The editor maps each one to a rule in [skywa1ker404.absencium.ui.vault.markdown.MarkdownEdit]. */
enum class MarkdownAction(val label: String) {
    BOLD("Bold"),
    ITALIC("Italic"),
    STRIKE("Strike"),
    CODE("Code"),
    H1("H1"),
    H2("H2"),
    H3("H3"),
    BULLET("Bullets"),
    NUMBERED("Numbers"),
    CHECK("Checklist"),
    QUOTE("Quote"),
    CODE_BLOCK("Code block"),
    LINK_NOTE("Link note"),
    TEMPLATE("Template"),
}

/** One scrolling row of formatting buttons above the editor. */
@Composable
fun MarkdownToolbar(onAction: (MarkdownAction) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(MarkdownAction.entries.toList()) { action ->
            ExpressiveChip(selected = false, onClick = { onAction(action) }, label = { Text(action.label) })
        }
    }
}

@Composable
private fun spansToText(spans: List<Span>, onLink: (String) -> Unit, strikeAll: Boolean = false): AnnotatedString {
    val colors = MaterialTheme.colorScheme
    val linkStyle = TextLinkStyles(SpanStyle(color = colors.primary, textDecoration = TextDecoration.Underline))
    return buildAnnotatedString {
        spans.forEach { span ->
            val link = span.link
            if (link != null) {
                withLink(LinkAnnotation.Clickable(tag = "note", styles = linkStyle) { onLink(link) }) { append(span.text) }
            } else {
                val lines = buildList {
                    if (span.strike || strikeAll) add(TextDecoration.LineThrough)
                    if (span.underline) add(TextDecoration.Underline)
                }
                val decoration = if (lines.isEmpty()) null else TextDecoration.combine(lines)
                withStyle(
                    SpanStyle(
                        fontWeight = if (span.bold) FontWeight.Bold else null,
                        fontStyle = if (span.italic) FontStyle.Italic else null,
                        textDecoration = decoration,
                        fontFamily = if (span.code) FontFamily.Monospace else null,
                        background = when {
                            span.highlight -> Color(0xFFFFE066).copy(alpha = 0.55f)
                            span.code -> colors.surfaceContainerHighest
                            else -> Color.Unspecified
                        },
                        color = span.color?.let { Color(0xFF000000.toInt() or it) } ?: Color.Unspecified,
                        fontSize = span.sizeSp?.sp ?: TextUnit.Unspecified,
                    ),
                ) { append(span.text) }
            }
        }
    }
}

/** What the reading screen's find bar needs from the page: the words, the active match, and where the page reports back. */
class FindState(
    val query: String,
    val active: Int,
    /** For every match in page order, the index of the block that holds it. */
    val onIndex: (IntArray) -> Unit,
    /** A block's top edge in the window, so the screen can scroll to it. */
    val onTop: (block: Int, windowY: Float) -> Unit,
)

private fun spanText(spans: List<Span>): String = spans.joinToString("") { it.text }

/** The texts of a block that are drawn as text, in the order they are drawn; find only looks in these. */
private fun blockMatchTexts(block: Block): List<String> = when (block) {
    is Block.Heading -> listOf(spanText(block.spans))
    is Block.Paragraph -> listOf(spanText(block.spans))
    is Block.Bullet -> listOf(spanText(block.spans))
    is Block.Numbered -> listOf(spanText(block.spans))
    is Block.Check -> listOf(spanText(block.spans))
    is Block.Quote -> listOf(spanText(block.spans))
    is Block.Table -> {
        val columns = block.rows.maxOfOrNull { it.size } ?: 0
        block.rows.flatMap { row -> (0 until columns).map { c -> spanText(row.getOrNull(c).orEmpty()) } }
    }
    else -> emptyList()
}

internal fun countMatches(text: String, query: String): Int {
    if (query.isEmpty()) return 0
    var n = 0
    var i = text.indexOf(query, 0, ignoreCase = true)
    while (i >= 0) {
        n++
        i = text.indexOf(query, i + query.length, ignoreCase = true)
    }
    return n
}

/** [text] with every match of [query] marked yellow and match number [active] marked orange. */
private fun withFind(text: AnnotatedString, query: String, active: Int): AnnotatedString {
    val plain = text.text
    val builder = AnnotatedString.Builder(text)
    var i = plain.indexOf(query, 0, ignoreCase = true)
    var n = 0
    while (i >= 0) {
        builder.addStyle(SpanStyle(background = if (n == active) Color(0xFFFF9800).copy(alpha = 0.85f) else Color(0xFFFFE066).copy(alpha = 0.6f)), i, i + query.length)
        n++
        i = plain.indexOf(query, i + query.length, ignoreCase = true)
    }
    return builder.toAnnotatedString()
}

/**
 * The note as formatted text. Tapping a check box flips it in the note itself ([onToggleCheck] gets the source line), and
 * tapping a `[[note link]]` calls [onOpenLink] with the title.
 */
@Composable
fun MarkdownPreview(
    text: String,
    onToggleCheck: (line: Int) -> Unit,
    onOpenLink: (title: String) -> Unit,
    modifier: Modifier = Modifier,
    /** Draws a picture, doodle or recording block; the editor supplies it because it knows where the files are. */
    embed: @Composable (Block.Embed) -> Unit = {},
    /** Find in the page: matches are highlighted and the screen is told where each one is (see [FindState]). */
    find: FindState? = null,
) {
    val colors = MaterialTheme.colorScheme
    // A colour or size tag that lost its `|}` closer would show as raw text; it is closed here for older notes and AI answers.
    val blocks = remember(text) { MarkdownParse.blocks(skywa1ker404.absencium.data.ai.AiNoteCleaner.repairTags(text)) }
    val query = find?.query.orEmpty()
    val perBlock = remember(blocks, query) {
        blocks.map { b -> if (query.isEmpty()) 0 else blockMatchTexts(b).sumOf { countMatches(it, query) } }
    }
    val starts = remember(perBlock) {
        var acc = 0
        perBlock.map { n -> acc.also { acc += n } }
    }
    if (find != null) {
        androidx.compose.runtime.LaunchedEffect(perBlock) {
            val owner = ArrayList<Int>()
            perBlock.forEachIndexed { b, n -> repeat(n) { owner += b } }
            find.onIndex(owner.toIntArray())
        }
    }
    val hl: (AnnotatedString, Int) -> AnnotatedString = { a, bi -> if (query.isEmpty()) a else withFind(a, query, (find?.active ?: -1) - starts[bi]) }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (blocks.isEmpty()) HintText("Tap here to start writing.")
        blocks.forEachIndexed { bi, block ->
          Box(
              modifier = if (find == null) Modifier.fillMaxWidth() else Modifier.fillMaxWidth().onGloballyPositioned { find.onTop(bi, it.positionInWindow().y) },
          ) {
            when (block) {
                is Block.Heading -> Text(
                    text = hl(spansToText(block.spans, onOpenLink), bi),
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.headlineSmall
                        2 -> MaterialTheme.typography.titleLarge
                        3 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    },
                    textAlign = block.align.toTextAlign(),
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
                is Block.Paragraph -> Text(
                    hl(spansToText(block.spans, onOpenLink), bi),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = block.align.toTextAlign(),
                    modifier = Modifier.fillMaxWidth(),
                )
                is Block.Embed -> Box(modifier = Modifier.fillMaxWidth(), contentAlignment = block.align.toBoxAlignment()) {
                    Box(modifier = Modifier.fillMaxWidth(block.widthPercent / 100f)) { embed(block) }
                }
                is Block.Table -> PreviewTable(block, onOpenLink, query, (find?.active ?: -1) - starts[bi])
                is Block.Bullet -> Row(modifier = Modifier.padding(start = (block.indent * 6).dp)) {
                    Text("•  ", style = MaterialTheme.typography.bodyLarge)
                    Text(hl(spansToText(block.spans, onOpenLink), bi), style = MaterialTheme.typography.bodyLarge)
                }
                is Block.Numbered -> Row(modifier = Modifier.padding(start = (block.indent * 6).dp)) {
                    Text("${block.number}.  ", style = MaterialTheme.typography.bodyLarge)
                    Text(hl(spansToText(block.spans, onOpenLink), bi), style = MaterialTheme.typography.bodyLarge)
                }
                is Block.Check -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(start = (block.indent * 6).dp)
                        .clickable { onToggleCheck(block.line) },
                ) {
                    Text(if (block.checked) "☑  " else "☐  ", style = MaterialTheme.typography.titleMedium)
                    Text(
                        hl(spansToText(block.spans, onOpenLink, strikeAll = block.checked), bi),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (block.checked) colors.onSurfaceVariant else colors.onSurface,
                    )
                }
                is Block.Quote -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceContainerHigh, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text(
                        hl(spansToText(block.spans, onOpenLink), bi),
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = FontStyle.Italic,
                        color = colors.onSurfaceVariant,
                    )
                }
                is Block.Code -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceContainerHighest, RoundedCornerShape(12.dp))
                        .horizontalScroll(rememberScrollState())
                        .padding(12.dp),
                ) {
                    Text(block.text, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                }
                Block.Rule -> HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
          }
        }
    }
}

/** An AI answer as formatted text (bold, lists, headings), read-only. The model writes Markdown; showing its stars and dashes raw looks broken. */
@Composable
fun AiMarkdown(text: String, modifier: Modifier = Modifier) =
    MarkdownPreview(text = text, onToggleCheck = {}, onOpenLink = {}, modifier = modifier)

private fun Align.toTextAlign(): TextAlign = when (this) {
    Align.START -> TextAlign.Start
    Align.CENTER -> TextAlign.Center
    Align.END -> TextAlign.End
}

internal fun Align.toBoxAlignment(): Alignment = when (this) {
    Align.START -> Alignment.CenterStart
    Align.CENTER -> Alignment.Center
    Align.END -> Alignment.CenterEnd
}

/** A table drawn as equal-width cells with a thin line around each; the first row is bold when the table has a header. */
@Composable
internal fun PreviewTable(table: Block.Table, onOpenLink: (String) -> Unit, query: String = "", active: Int = -1) {
    val colors = MaterialTheme.colorScheme
    val columns = table.rows.maxOfOrNull { it.size } ?: 0
    var seen = 0
    Column(modifier = Modifier.fillMaxWidth().border(1.dp, colors.outlineVariant, RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp))) {
        table.rows.forEachIndexed { r, row ->
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                for (c in 0 until columns) {
                    val cell = row.getOrNull(c).orEmpty()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(0.5.dp, colors.outlineVariant)
                            .background(if (table.hasHeader && r == 0) colors.surfaceContainerHigh else Color.Transparent)
                            .padding(8.dp),
                    ) {
                        val cellText = spansToText(cell, onOpenLink)
                        val shown = if (query.isEmpty()) cellText else withFind(cellText, query, active - seen)
                        if (query.isNotEmpty()) seen += countMatches(cellText.text, query)
                        Text(
                            shown,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (table.hasHeader && r == 0) FontWeight.SemiBold else null,
                        )
                    }
                }
            }
        }
    }
}

/** Picks another note to link to; the editor writes `[[Title]]` at the cursor. */
@Composable
fun LinkNoteDialog(notes: List<NoteEntity>, onPick: (title: String) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val shown = remember(notes, query) {
        notes.filter { it.title.isNotBlank() && (query.isBlank() || it.title.contains(query, ignoreCase = true)) }.take(50)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Link to a note") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search notes") },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (shown.isEmpty()) HintText("No notes match.")
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(shown) { note ->
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(14.dp))
                                .clickable { onPick(note.title) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun TemplateDialog(onPick: (NoteTemplate) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Start from a template") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NoteTemplates.all.forEach { template ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(14.dp))
                            .clickable { onPick(template) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Text(template.name, style = MaterialTheme.typography.titleSmall)
                        Text(template.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
