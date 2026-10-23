package skywa1ker404.absencium.ui.vault

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import skywa1ker404.absencium.ui.components.floatingGlass
import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.theme.LocalBackdrop
import skywa1ker404.absencium.ui.vault.markdown.Align
import skywa1ker404.absencium.ui.vault.markdown.NoteTemplate
import skywa1ker404.absencium.ui.vault.markdown.NoteTemplates

/** Which panel is open above the editor's bottom toolbar. */
enum class EditorPanel { NONE, AI, TEXT, LISTS, ADD }

private enum class TextTab(val icon: () -> ImageVector, val description: String) {
    STYLE({ AppIcons.FormatSize }, "Text style"),
    SYMBOL({ AppIcons.Functions }, "Symbols"),
    DIVIDER({ AppIcons.HorizontalRule }, "Dividers"),
    TEMPLATES({ AppIcons.Description }, "Templates"),
}

private val Palette = listOf(0xE53935, 0xFB8C00, 0xFDD835, 0x43A047, 0x00ACC1, 0x1E88E5, 0x8E24AA, 0x757575)
private const val SYMBOLS = "• ◦ ★ ☆ ✓ ✗ → ← ↑ ↓ ↔ ⇒ ± × ÷ ≈ ≠ ≤ ≥ ∑ √ π ∞ ° ² ³ ½ ¼ ¾ © ® ™ § ¶ † ‡ € £ ¥ ₹"

/** A frosted card in the glass style of the floating bars, used for every panel. */
@Composable
internal fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .floatingGlass(LocalBackdrop.current, RoundedCornerShape(26.dp), lens = false, denseVeil = true),
    ) { content() }
}

/** One round tool button: an icon, or a short glyph where no icon says it better (H1, a size sample). [label] is read out by TalkBack. */
@Composable
internal fun Tool(
    label: String,
    onClick: () -> Unit,
    selected: Boolean = false,
    icon: ImageVector? = null,
    glyph: String? = null,
    glyphSize: Int = 16,
    glyphWeight: FontWeight = FontWeight.SemiBold,
) {
    val colors = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    // A small press and a soft colour change; quick enough to stay out of the way of typing.
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, tween(90), label = "toolPress")
    val fill by animateColorAsState(if (selected) colors.primaryContainer else Color.Transparent, tween(140), label = "toolFill")
    Box(
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(fill)
            .clickable(interactionSource = source, indication = LocalIndication.current, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        val tint = if (selected) colors.onPrimaryContainer else colors.onSurface
        if (icon != null) Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        if (glyph != null) Text(glyph, fontSize = glyphSize.sp, fontWeight = glyphWeight, color = tint)
    }
}

/** The row of buttons at the bottom of the editor: text, lists, checklist, pictures and more. [onUse] runs first on every tap (it puts the keyboard away). */
@Composable
fun EditorToolbar(
    panel: EditorPanel,
    onPanel: (EditorPanel) -> Unit,
    onCheck: () -> Unit,
    onImage: () -> Unit,
    onUse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .floatingGlass(LocalBackdrop.current, RoundedCornerShape(32.dp)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tool("AI", { onUse(); onPanel(if (panel == EditorPanel.AI) EditorPanel.NONE else EditorPanel.AI) }, panel == EditorPanel.AI, icon = AppIcons.AutoAwesome)
            Tool("Text style", { onUse(); onPanel(if (panel == EditorPanel.TEXT) EditorPanel.NONE else EditorPanel.TEXT) }, panel == EditorPanel.TEXT, icon = AppIcons.FormatSize)
            Tool("Alignment and lists", { onUse(); onPanel(if (panel == EditorPanel.LISTS) EditorPanel.NONE else EditorPanel.LISTS) }, panel == EditorPanel.LISTS, icon = AppIcons.ListBulleted)
            Tool("Checklist", { onUse(); onCheck() }, icon = AppIcons.CheckBox)
            Tool("Add pictures", { onUse(); onImage() }, icon = AppIcons.Image)
            Tool("Add more", { onUse(); onPanel(if (panel == EditorPanel.ADD) EditorPanel.NONE else EditorPanel.ADD) }, panel == EditorPanel.ADD, icon = AppIcons.Add)
        }
    }
}

/** Text style, symbols, dividers and templates, in four tabs. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextPanel(
    onWrap: (marker: String, placeholder: String) -> Unit,
    onHeading: (Int) -> Unit,
    onColor: (Int?) -> Unit,
    onSize: (Int?) -> Unit,
    onInsert: (String) -> Unit,
    onDivider: (String) -> Unit,
    onTemplate: (NoteTemplate) -> Unit,
    onUse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableStateOf(TextTab.STYLE) }
    GlassCard(modifier) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextTab.entries.forEach { t -> Tool(t.description, { onUse(); tab = t }, tab == t, icon = t.icon()) }
            }
            Column(modifier = Modifier.heightIn(max = 230.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                when (tab) {
                    TextTab.STYLE -> {
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Tool("Bold", { onUse(); onWrap("**", "bold") }, icon = AppIcons.FormatBold)
                            Tool("Italic", { onUse(); onWrap("*", "italic") }, icon = AppIcons.FormatItalic)
                            Tool("Underline", { onUse(); onWrap("++", "text") }, icon = AppIcons.FormatUnderlined)
                            Tool("Strikethrough", { onUse(); onWrap("~~", "text") }, icon = AppIcons.FormatStrikethrough)
                            Tool("Highlight", { onUse(); onWrap("==", "text") }, icon = AppIcons.Highlight)
                            Tool("Code", { onUse(); onWrap("`", "code") }, icon = AppIcons.Code)
                        }
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Tool("Title", { onUse(); onHeading(1) }, glyph = "H1", glyphSize = 17)
                            Tool("Heading", { onUse(); onHeading(2) }, glyph = "H2", glyphSize = 16)
                            Tool("Subheading", { onUse(); onHeading(3) }, glyph = "H3", glyphSize = 15)
                            Tool("Small text", { onUse(); onSize(12) }, glyph = "A", glyphSize = 12, glyphWeight = FontWeight.Normal)
                            Tool("Normal text size", { onUse(); onSize(null) }, glyph = "A", glyphSize = 17, glyphWeight = FontWeight.Normal)
                            Tool("Large text", { onUse(); onSize(22) }, glyph = "A", glyphSize = 22, glyphWeight = FontWeight.Normal)
                            Tool("Huge text", { onUse(); onSize(30) }, glyph = "A", glyphSize = 28, glyphWeight = FontWeight.Normal)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    .clickable { onUse(); onColor(null) }
                                    .semantics { contentDescription = "Default text colour" },
                                contentAlignment = Alignment.Center,
                            ) { Icon(AppIcons.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Palette.forEach { rgb ->
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF000000.toInt() or rgb))
                                        .clickable { onUse(); onColor(rgb) }
                                        .semantics { contentDescription = "Text colour" },
                                )
                            }
                        }
                    }
                    TextTab.SYMBOL -> FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SYMBOLS.split(' ').forEach { symbol ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .clickable { onUse(); onInsert(symbol) },
                                contentAlignment = Alignment.Center,
                            ) { Text(symbol, style = MaterialTheme.typography.titleMedium) }
                        }
                    }
                    TextTab.DIVIDER -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("---" to "────────", "{align:center}· · ·" to "· · ·", "{align:center}✦ ✦ ✦" to "✦ ✦ ✦", "{align:center}— — —" to "— — —").forEach { (source, sample) ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .clickable { onUse(); onDivider(source) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) { Text(sample, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                    TextTab.TEMPLATES -> NoteTemplates.all.forEach { template ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(14.dp))
                                .clickable { onUse(); onTemplate(template) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            Text(template.name, style = MaterialTheme.typography.titleSmall)
                            Text(template.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

/** Alignment and list styles for the lines the cursor or selection touches. */
@Composable
fun ListsPanel(
    onAlign: (Align) -> Unit,
    onBullet: () -> Unit,
    onNumbered: () -> Unit,
    onLettered: () -> Unit,
    onQuote: () -> Unit,
    onUse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Tool("Align left", { onUse(); onAlign(Align.START) }, icon = AppIcons.AlignLeft)
                Tool("Align centre", { onUse(); onAlign(Align.CENTER) }, icon = AppIcons.FormatAlignCenter)
                Tool("Align right", { onUse(); onAlign(Align.END) }, icon = AppIcons.FormatAlignRight)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Tool("Bulleted list", { onUse(); onBullet() }, icon = AppIcons.ListBulleted)
                Tool("Numbered list", { onUse(); onNumbered() }, icon = AppIcons.FormatListNumbered)
                Tool("Lettered list", { onUse(); onLettered() }, glyph = "a)", glyphSize = 17)
                Tool("Quote", { onUse(); onQuote() }, icon = AppIcons.FormatQuote)
            }
        }
    }
}

/** Things that can be added to the note. Only what works today is listed; recordings arrive with their own step. */
@Composable
fun AddPanel(
    onTable: () -> Unit,
    onDoodle: () -> Unit,
    onLinkNote: () -> Unit,
    onCodeBlock: () -> Unit,
    onUse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier) {
        Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Tool("Table", { onUse(); onTable() }, icon = AppIcons.Table)
            Tool("Doodle", { onUse(); onDoodle() }, icon = AppIcons.Brush)
            Tool("Link a note", { onUse(); onLinkNote() }, icon = AppIcons.Link)
            Tool("Code block", { onUse(); onCodeBlock() }, icon = AppIcons.Code)
        }
    }
}
