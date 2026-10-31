package skywa1ker404.absencium.data.share

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import android.graphics.Bitmap
import android.graphics.RectF
import skywa1ker404.absencium.ui.vault.markdown.Align
import skywa1ker404.absencium.ui.vault.markdown.Block
import skywa1ker404.absencium.ui.vault.markdown.EmbedKind
import skywa1ker404.absencium.ui.vault.markdown.Span
import java.io.OutputStream

/**
 * Draws a note onto A4 pages with the platform's own [PdfDocument]: no library, nothing leaves the phone. Text is real text
 * (selectable, searchable in a viewer), wrapped to the page, and a paragraph that does not fit is continued on the next page
 * line by line instead of being cut. The page follows the note: headings, lists, alignment, colours, sizes, highlights, tables
 * with borders, and pictures and doodles at their size and alignment. Recordings are left out (a PDF cannot play them).
 */
/**
 * The colours of a themed PDF, taken from the app's current theme (so light or dark and the chosen colours carry over). [Plain] is the
 * ordinary white page with black text.
 */
class PdfTheme(val background: Int, val text: Int, val shade: Int, val line: Int, val highlight: Int) {
    companion object {
        val Plain = PdfTheme(0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFFEDEDED.toInt(), 0xFF9E9E9E.toInt(), 0xFFFFE066.toInt())
    }
}

object PdfNoteRenderer {
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 48

    /** [picture] gives the bitmap of a picture or doodle block, or null when its file is gone (the block is then skipped). */
    @Synchronized
    fun render(title: String, blocks: List<Block>, out: OutputStream, theme: PdfTheme = PdfTheme.Plain, picture: (Block.Embed) -> Bitmap? = { null }) {
        val doc = PdfDocument()
        highlightColor = theme.highlight
        try {
            val pager = Pager(doc, theme)
            val heading = title.trim()
            if (heading.isNotEmpty()) pager.text(SpannableStringBuilder(heading).also { it.setSpan(StyleSpan(Typeface.BOLD), 0, it.length, 0) }, 24f, 0, 10f)
            for (block in blocks) {
                when (block) {
                    is Block.Heading -> {
                        val size = when (block.level) { 1 -> 20f; 2 -> 17f; 3 -> 15f; else -> 13f }
                        pager.text(styled(block.spans, size, bold = true), size, 0, 6f, topGap = 6f, align = block.align)
                    }
                    is Block.Paragraph -> pager.text(styled(block.spans, 12f), 12f, 0, 8f, align = block.align)
                    is Block.Bullet -> pager.text(prefixed("•  ", block.spans), 12f, block.indent * 6, 4f, align = block.align)
                    is Block.Numbered -> pager.text(prefixed("${block.number}.  ", block.spans), 12f, block.indent * 6, 4f, align = block.align)
                    is Block.Check -> pager.text(prefixed(if (block.checked) "\u2611  " else "\u2610  ", block.spans), 12f, block.indent * 6, 4f, align = block.align)
                    is Block.Quote -> pager.quote(styled(block.spans, 12f, italic = true), block.align)
                    is Block.Code -> pager.text(SpannableStringBuilder(block.text).also { it.setSpan(TypefaceSpan("monospace"), 0, it.length, 0) }, 10.5f, 8, 8f, shade = true)
                    Block.Rule -> pager.rule()
                    is Block.Embed -> if (block.kind != EmbedKind.AUDIO) picture(block)?.let { pager.picture(it, block.widthPercent, block.align) }
                    is Block.Table -> pager.table(block.rows.map { row -> row.map { cell -> styled(cell, 11f, bold = false) } }, block.hasHeader) { cell, bold ->
                        if (bold) SpannableStringBuilder(cell).also { it.setSpan(StyleSpan(Typeface.BOLD), 0, it.length, 0) } else cell
                    }
                }
            }
            pager.finish()
            doc.writeTo(out)
        } finally {
            doc.close()
        }
    }

    /** The highlight colour of the page being drawn; render calls are serialised, so one value is enough. */
    private var highlightColor = 0xFFFFE066.toInt()

    private fun prefixed(prefix: String, spans: List<Span>): SpannableStringBuilder =
        SpannableStringBuilder(prefix).also { it.append(styled(spans, 12f)) }

    /** A size the note sets in sp is scaled to the page (the page is about 0.75 of a phone screen at the same look). */
    private fun styled(spans: List<Span>, base: Float, bold: Boolean = false, italic: Boolean = false): SpannableStringBuilder {
        val sb = SpannableStringBuilder()
        for (span in spans) {
            val start = sb.length
            sb.append(span.text)
            val end = sb.length
            if (end == start) continue
            val isBold = bold || span.bold
            val isItalic = italic || span.italic
            val style = when {
                isBold && isItalic -> Typeface.BOLD_ITALIC
                isBold -> Typeface.BOLD
                isItalic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }
            if (style != Typeface.NORMAL) sb.setSpan(StyleSpan(style), start, end, 0)
            if (span.code) sb.setSpan(TypefaceSpan("monospace"), start, end, 0)
            if (span.strike) sb.setSpan(StrikethroughSpan(), start, end, 0)
            if (span.link != null || span.underline) sb.setSpan(UnderlineSpan(), start, end, 0)
            if (span.highlight) sb.setSpan(android.text.style.BackgroundColorSpan(highlightColor), start, end, 0)
            span.sizeSp?.let { sb.setSpan(android.text.style.RelativeSizeSpan((it * 0.75f / base).coerceIn(0.4f, 6f)), start, end, 0) }
            span.color?.let { sb.setSpan(android.text.style.ForegroundColorSpan(0xFF000000.toInt() or it), start, end, 0) }
        }
        return sb
    }

    private class Pager(private val doc: PdfDocument, private val theme: PdfTheme) {
        private val width = PAGE_W - 2 * MARGIN
        private val contentHeight = (PAGE_H - 2 * MARGIN).toFloat()
        private var pageNumber = 0
        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var y = 0f

        private fun newPage() {
            page?.let { doc.finishPage(it) }
            pageNumber++
            val p = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNumber).create())
            page = p
            canvas = p.canvas
            // A themed page is painted edge to edge in the theme's background.
            if (theme.background != PdfTheme.Plain.background) p.canvas.drawColor(theme.background)
            y = 0f
        }

        fun text(text: CharSequence, size: Float, indent: Int, gapAfter: Float, topGap: Float = 0f, align: Align = Align.START, shade: Boolean = false, bar: Boolean = false) {
            if (text.isEmpty()) return
            if (page == null) newPage()
            if (y > 0f) y += topGap
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size; color = theme.text }
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, (width - indent).coerceAtLeast(60))
                .setAlignment(when (align) { Align.START -> Layout.Alignment.ALIGN_NORMAL; Align.CENTER -> Layout.Alignment.ALIGN_CENTER; Align.END -> Layout.Alignment.ALIGN_OPPOSITE })
                .setLineSpacing(0f, 1.15f)
                .build()
            var consumed = 0f
            val total = layout.height.toFloat()
            while (consumed < total) {
                val available = contentHeight - y
                var first = 0
                while (first < layout.lineCount && layout.getLineTop(first) < consumed - 0.5f) first++
                var line = first
                var endBottom = consumed
                while (line < layout.lineCount && layout.getLineBottom(line) - consumed <= available) {
                    endBottom = layout.getLineBottom(line).toFloat()
                    line++
                }
                if (endBottom <= consumed) {
                    if (y > 0f) {
                        newPage()
                        continue
                    }
                    // A single line taller than a whole page cannot be split; draw it and carry on.
                    endBottom = layout.getLineBottom(first.coerceAtMost(layout.lineCount - 1)).toFloat()
                }
                val c = canvas ?: return
                if (shade || bar) {
                    val back = Paint().apply { color = if (shade) theme.shade else theme.line }
                    val top = MARGIN + y
                    if (shade) c.drawRect((MARGIN + indent - 4).toFloat(), top, (MARGIN + width).toFloat(), top + endBottom - consumed, back)
                    else c.drawRect((MARGIN + indent - 10).toFloat(), top, (MARGIN + indent - 7).toFloat(), top + endBottom - consumed, back)
                }
                c.save()
                c.translate((MARGIN + indent).toFloat(), MARGIN + y)
                c.clipRect(0f, 0f, width.toFloat(), endBottom - consumed)
                c.translate(0f, -consumed)
                layout.draw(c)
                c.restore()
                y += endBottom - consumed
                consumed = endBottom
                if (consumed < total) newPage()
            }
            y += gapAfter
        }

        fun quote(content: CharSequence, align: Align) = text(content, 12f, 18, 8f, align = align, bar = true)

        /** A picture or doodle at [widthPercent] of the page width, placed by [align]; scaled down if it is taller than a page. */
        fun picture(bitmap: Bitmap, widthPercent: Int, align: Align) {
            if (bitmap.width <= 0 || bitmap.height <= 0) return
            if (page == null) newPage()
            var w = width * widthPercent.coerceIn(10, 100) / 100f
            var h = w * bitmap.height / bitmap.width
            if (h > contentHeight) {
                h = contentHeight
                w = h * bitmap.width / bitmap.height
            }
            if (y + h > contentHeight) newPage()
            val c = canvas ?: return
            val left = MARGIN + when (align) { Align.START -> 0f; Align.CENTER -> (width - w) / 2f; Align.END -> width - w }
            val top = MARGIN + y
            c.drawBitmap(bitmap, null, RectF(left, top, left + w, top + h), Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
            y += h + 8f
        }

        /** A table with a border round every cell; the header row is shaded. A row that does not fit moves to the next page. */
        fun table(rows: List<List<SpannableStringBuilder>>, hasHeader: Boolean, header: (SpannableStringBuilder, Boolean) -> CharSequence) {
            val columns = rows.maxOfOrNull { it.size } ?: return
            if (columns == 0) return
            if (page == null) newPage()
            val cellW = width.toFloat() / columns
            val pad = 4f
            val line = Paint().apply { color = theme.line; style = Paint.Style.STROKE; strokeWidth = 0.6f }
            val shade = Paint().apply { color = theme.shade }
            rows.forEachIndexed { r, row ->
                val bold = hasHeader && r == 0
                val layouts = (0 until columns).map { col ->
                    val content = header(row.getOrNull(col) ?: SpannableStringBuilder(), bold)
                    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; color = theme.text }
                    StaticLayout.Builder.obtain(content, 0, content.length, paint, (cellW - 2 * pad).toInt().coerceAtLeast(20)).build()
                }
                val rowH = (layouts.maxOf { it.height } + 2 * pad).coerceAtMost(contentHeight)
                if (y + rowH > contentHeight) newPage()
                val c = canvas ?: return
                val top = MARGIN + y
                layouts.forEachIndexed { col, layout ->
                    val left = MARGIN + col * cellW
                    if (bold) c.drawRect(left, top, left + cellW, top + rowH, shade)
                    c.drawRect(left, top, left + cellW, top + rowH, line)
                    c.save()
                    c.translate(left + pad, top + pad)
                    layout.draw(c)
                    c.restore()
                }
                y += rowH
            }
            y += 8f
        }

        fun rule() {
            if (page == null) newPage()
            if (y + 16f > contentHeight) newPage()
            val c = canvas ?: return
            val paint = Paint().apply { strokeWidth = 0.8f; color = theme.line }
            c.drawLine(MARGIN.toFloat(), MARGIN + y + 8f, (MARGIN + width).toFloat(), MARGIN + y + 8f, paint)
            y += 16f
        }

        fun finish() {
            if (page == null) newPage()
            page?.let { doc.finishPage(it) }
            page = null
        }
    }
}
