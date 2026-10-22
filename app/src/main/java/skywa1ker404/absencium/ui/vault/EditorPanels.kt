package skywa1ker404.absencium.ui.vault

import androidx.compose.foundation.background
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
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import skywa1ker404.absencium.ui.components.ExpressiveChip
import skywa1ker404.absencium.ui.components.floatingGlass
import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.theme.LocalBackdrop
import skywa1ker404.absencium.ui.vault.markdown.Align
import skywa1ker404.absencium.ui.vault.markdown.NoteTemplate
import skywa1ker404.absencium.ui.vault.markdown.NoteTemplates

/** Which panel is open above the editor's bottom toolbar. */
enum class EditorPanel { NONE, TEXT, LISTS, ADD }

private enum class TextTab(val label: String) {
    STYLE("Text style"),
    SYMBOL("Symbol"),
    DIVIDER("Divider"),
    TEMPLATES("Templates"),
}

private val Palette = listOf(0xE53935, 0xFB8C00, 0xFDD835, 0x43A047, 0x00ACC1, 0x1E88E5, 0x8E24AA, 0x757575)
private const val SYMBOLS = "• ◦ ★ ☆ ✓ ✗ → ← ↑ ↓ ↔ ⇒ ± × ÷ ≈ ≠ ≤ ≥ ∑ √ π ∞ ° ² ³ ½ ¼ ¾ © ® ™ § ¶ † ‡ € £ ¥ ₹"

/** A frosted card in the glass style of the floating bars, used for every panel and the toolbar. */
@Composable
private fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .floatingGlass(LocalBackdrop.current, RoundedCornerShape(26.dp), lens = false, denseVeil = true),
    ) { content() }
}

/** The row of buttons at the bottom of the editor: text, lists, checklist, pictures and more. */
@Composable
fun EditorToolbar(panel: EditorPanel, onPanel: (EditorPanel) -> Unit, onCheck: () -> Unit, onImage: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
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
            val on = colors.primary
            val off = colors.onSurface
            IconButton(onClick = { onPanel(if (panel == EditorPanel.TEXT) EditorPanel.NONE else EditorPanel.TEXT) }) {
                Text("Aa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (panel == EditorPanel.TEXT) on else off)
            }
            IconButton(onClick = { onPanel(if (panel == EditorPanel.LISTS) EditorPanel.NONE else EditorPanel.LISTS) }) {
                Icon(AppIcons.AlignLeft, contentDescription = "Align and lists", tint = if (panel == EditorPanel.LISTS) on else off)
            }
            IconButton(onClick = onCheck) { Icon(AppIcons.CheckBox, contentDescription = "Checklist", tint = off) }
            IconButton(onClick = onImage) { Icon(AppIcons.Image, contentDescription = "Add pictures", tint = off) }
            IconButton(onClick = { onPanel(if (panel == EditorPanel.ADD) EditorPanel.NONE else EditorPanel.ADD) }) {
                Icon(AppIcons.Add, contentDescription = "More to add", tint = if (panel == EditorPanel.ADD) on else off)
            }
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
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableStateOf(TextTab.STYLE) }
    GlassCard(modifier) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextTab.entries.forEach { t ->
                    ExpressiveChip(selected = tab == t, onClick = { tab = t }, label = { Text(t.label, maxLines = 1) })
                }
            }
            Column(modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (tab) {
                    TextTab.STYLE -> {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ExpressiveChip(selected = false, onClick = { onWrap("**", "bold") }, label = { Text("B", fontWeight = FontWeight.Bold) })
                            ExpressiveChip(selected = false, onClick = { onWrap("*", "italic") }, label = { Text("I", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic) })
                            ExpressiveChip(selected = false, onClick = { onWrap("++", "text") }, label = { Text("U", textDecoration = TextDecoration.Underline) })
                            ExpressiveChip(selected = false, onClick = { onWrap("~~", "text") }, label = { Text("S", textDecoration = TextDecoration.LineThrough) })
                            ExpressiveChip(selected = false, onClick = { onWrap("==", "text") }, label = { Text("Highlight") })
                            ExpressiveChip(selected = false, onClick = { onWrap("`", "code") }, label = { Text("Code") })
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ExpressiveChip(selected = false, onClick = { onHeading(1) }, label = { Text("Title") })
                            ExpressiveChip(selected = false, onClick = { onHeading(2) }, label = { Text("Heading") })
                            ExpressiveChip(selected = false, onClick = { onHeading(3) }, label = { Text("Subheading") })
                        }
                        Text("Size", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ExpressiveChip(selected = false, onClick = { onSize(12) }, label = { Text("Small") })
                            ExpressiveChip(selected = false, onClick = { onSize(null) }, label = { Text("Normal") })
                            ExpressiveChip(selected = false, onClick = { onSize(22) }, label = { Text("Large") })
                            ExpressiveChip(selected = false, onClick = { onSize(30) }, label = { Text("Huge") })
                        }
                        Text("Colour", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            ExpressiveChip(selected = false, onClick = { onColor(null) }, label = { Text("None") })
                            Palette.forEach { rgb ->
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF000000.toInt() or rgb))
                                        .clickable { onColor(rgb) },
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
                                    .clickable { onInsert(symbol) },
                                contentAlignment = Alignment.Center,
                            ) { Text(symbol, style = MaterialTheme.typography.titleMedium) }
                        }
                    }
                    TextTab.DIVIDER -> FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ExpressiveChip(selected = false, onClick = { onDivider("---") }, label = { Text("Line") })
                        ExpressiveChip(selected = false, onClick = { onDivider("{align:center}· · ·") }, label = { Text("· · ·") })
                        ExpressiveChip(selected = false, onClick = { onDivider("{align:center}✦ ✦ ✦") }, label = { Text("✦ ✦ ✦") })
                        ExpressiveChip(selected = false, onClick = { onDivider("{align:center}— — —") }, label = { Text("— — —") })
                    }
                    TextTab.TEMPLATES -> NoteTemplates.all.forEach { template ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(14.dp))
                                .clickable { onTemplate(template) }
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
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListsPanel(
    onAlign: (Align) -> Unit,
    onBullet: () -> Unit,
    onNumbered: () -> Unit,
    onLettered: () -> Unit,
    onQuote: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ExpressiveChip(selected = false, onClick = { onAlign(Align.START) }, label = { Text("Left") })
                ExpressiveChip(selected = false, onClick = { onAlign(Align.CENTER) }, label = { Text("Centre") })
                ExpressiveChip(selected = false, onClick = { onAlign(Align.END) }, label = { Text("Right") })
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ExpressiveChip(selected = false, onClick = onBullet, label = { Text("Bullets") })
                ExpressiveChip(selected = false, onClick = onNumbered, label = { Text("Numbers") })
                ExpressiveChip(selected = false, onClick = onLettered, label = { Text("Letters") })
                ExpressiveChip(selected = false, onClick = onQuote, label = { Text("Quote") })
            }
        }
    }
}

/** Things that can be added to the note. Only what works today is listed; doodles and recordings arrive with their own steps. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddPanel(onTable: () -> Unit, onLinkNote: () -> Unit, onCodeBlock: () -> Unit, modifier: Modifier = Modifier) {
    GlassCard(modifier) {
        FlowRow(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ExpressiveChip(selected = false, onClick = onTable, label = { Text("Table") })
            ExpressiveChip(selected = false, onClick = onLinkNote, label = { Text("Link a note") })
            ExpressiveChip(selected = false, onClick = onCodeBlock, label = { Text("Code block") })
        }
    }
}
