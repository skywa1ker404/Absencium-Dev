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
import skywa1ker404.absencium.ui.vault.markdown.Block
import skywa1ker404.absencium.ui.vault.markdown.Span
import java.io.OutputStream

/**
 * Draws a note onto A4 pages with the platform's own [PdfDocument]: no library, nothing leaves the phone. Text is real text
 * (selectable, searchable in a viewer), wrapped to the page, and a paragraph that does not fit is continued on the next page
 * line by line instead of being cut. Pictures, doodles and recordings are marked by a short label; drawing them comes with the picture blocks.
 */
object PdfNoteRenderer {
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 48

    fun render(title: String, blocks: List<Block>, out: OutputStream) {
        val doc = PdfDocument()
        try {
            val pager = Pager(doc)
            val heading = title.trim()
            if (heading.isNotEmpty()) pager.text(SpannableStringBuilder(heading).also { it.setSpan(StyleSpan(Typeface.BOLD), 0, it.length, 0) }, 24f, 0, 10f)
            for (block in blocks) {
                when (block) {
                    is Block.Heading -> pager.text(styled(block.spans, bold = true), when (block.level) { 1 -> 20f; 2 -> 17f; 3 -> 15f; else -> 13f }, 0, 6f, topGap = 6f)
                    is Block.Paragraph -> pager.text(styled(block.spans), 12f, 0, 8f)
                    is Block.Bullet -> pager.text(prefixed("•  ", block.spans), 12f, block.indent * 6, 4f)
                    is Block.Numbered -> pager.text(prefixed("${block.number}.  ", block.spans), 12f, block.indent * 6, 4f)
                    is Block.Check -> pager.text(prefixed(if (block.checked) "[x]  " else "[ ]  ", block.spans), 12f, block.indent * 6, 4f)
                    is Block.Quote -> pager.text(styled(block.spans, italic = true), 12f, 18, 8f)
                    is Block.Code -> pager.text(SpannableStringBuilder(block.text).also { it.setSpan(TypefaceSpan("monospace"), 0, it.length, 0) }, 10.5f, 8, 8f)
                    Block.Rule -> pager.rule()
                    is Block.Embed -> pager.text(
                        SpannableStringBuilder("[${block.kind.name.lowercase().replaceFirstChar { it.uppercase() }}]").also { it.setSpan(StyleSpan(Typeface.ITALIC), 0, it.length, 0) },
                        11f, 0, 8f,
                    )
                    is Block.Table -> for (row in block.rows) {
                        pager.text(styled(row.flatMapIndexed { i, cell -> if (i == 0) cell else listOf(Span("   |   ")) + cell }, bold = block.hasHeader && row === block.rows.first()), 12f, 0, 3f)
                    }
                }
            }
            pager.finish()
            doc.writeTo(out)
        } finally {
            doc.close()
        }
    }

    private fun prefixed(prefix: String, spans: List<Span>): SpannableStringBuilder =
        SpannableStringBuilder(prefix).also { it.append(styled(spans)) }

    private fun styled(spans: List<Span>, bold: Boolean = false, italic: Boolean = false): SpannableStringBuilder {
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
            if (span.highlight) sb.setSpan(android.text.style.BackgroundColorSpan(0xFFFFE066.toInt()), start, end, 0)
            span.color?.let { sb.setSpan(android.text.style.ForegroundColorSpan(0xFF000000.toInt() or it), start, end, 0) }
        }
        return sb
    }

    private class Pager(private val doc: PdfDocument) {
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
            y = 0f
        }

        fun text(text: CharSequence, size: Float, indent: Int, gapAfter: Float, topGap: Float = 0f) {
            if (text.isEmpty()) return
            if (page == null) newPage()
            if (y > 0f) y += topGap
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size }
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, (width - indent).coerceAtLeast(60))
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
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

        fun rule() {
            if (page == null) newPage()
            if (y + 16f > contentHeight) newPage()
            val c = canvas ?: return
            val paint = Paint().apply { strokeWidth = 0.8f; color = 0xFF888888.toInt() }
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
