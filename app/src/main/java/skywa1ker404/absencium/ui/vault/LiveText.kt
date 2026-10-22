package skywa1ker404.absencium.ui.vault

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import skywa1ker404.absencium.ui.vault.markdown.EditKind
import skywa1ker404.absencium.ui.vault.markdown.LivePlan

/** The colours the live page needs from the theme. */
class LivePalette(val accent: Color, val muted: Color, val codeBackground: Color)

/** The text as the page shows it: formatting characters gone, styles applied. Offsets are those of [LivePlan.visible]. */
fun buildLiveText(plan: LivePlan, palette: LivePalette): AnnotatedString = buildAnnotatedString {
    append(plan.visible)
    for (r in plan.styles) {
        if (r.end <= r.start || r.end > plan.visible.length) continue
        when (r.kind) {
            EditKind.MARKER -> addStyle(
                SpanStyle(color = if (r.arg == 5) palette.muted else palette.accent, fontWeight = if (r.arg == 1 || r.arg == 4) FontWeight.Bold else null),
                r.start, r.end,
            )
            EditKind.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), r.start, r.end)
            EditKind.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), r.start, r.end)
            EditKind.STRIKE -> addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), r.start, r.end)
            EditKind.UNDERLINE -> addStyle(SpanStyle(textDecoration = TextDecoration.Underline), r.start, r.end)
            EditKind.HIGHLIGHT -> addStyle(SpanStyle(background = Color(0xFFFFE066).copy(alpha = 0.55f)), r.start, r.end)
            EditKind.COLOR -> addStyle(SpanStyle(color = Color(0xFF000000.toInt() or r.arg)), r.start, r.end)
            EditKind.SIZE -> addStyle(SpanStyle(fontSize = r.arg.sp), r.start, r.end)
            EditKind.HEADING -> addStyle(
                SpanStyle(fontWeight = FontWeight.Bold, fontSize = when (r.arg) { 1 -> 28.sp; 2 -> 24.sp; 3 -> 20.sp; else -> 18.sp }),
                r.start, r.end,
            )
            EditKind.CODE -> addStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = palette.codeBackground), r.start, r.end)
            EditKind.LINK -> addStyle(SpanStyle(color = palette.accent, textDecoration = TextDecoration.Underline), r.start, r.end)
            EditKind.ALIGN -> addStyle(ParagraphStyle(textAlign = if (r.arg == 1) TextAlign.Center else TextAlign.End), r.start, r.end)
            EditKind.CHECKED -> addStyle(SpanStyle(color = palette.muted, textDecoration = TextDecoration.LineThrough), r.start, r.end)
            EditKind.QUOTE -> addStyle(SpanStyle(fontStyle = FontStyle.Italic, color = palette.muted), r.start, r.end)
        }
    }
}

/** Shows [plan]'s page for the note text it was made from, and maps the cursor between the two. Any other text is shown as it is. */
fun liveTransformation(plan: LivePlan, styled: AnnotatedString) = VisualTransformation { shown ->
    if (shown.text != plan.original) {
        TransformedText(shown, OffsetMapping.Identity)
    } else {
        TransformedText(
            styled,
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = plan.toVisible(offset.coerceIn(0, plan.original.length)).coerceIn(0, plan.visible.length)
                override fun transformedToOriginal(offset: Int): Int = plan.toOriginal(offset.coerceIn(0, plan.visible.length)).coerceIn(0, plan.original.length)
            },
        )
    }
}
