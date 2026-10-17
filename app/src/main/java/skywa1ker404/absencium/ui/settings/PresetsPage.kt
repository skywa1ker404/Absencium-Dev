package skywa1ker404.absencium.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import skywa1ker404.absencium.data.settings.ThemePreset
import skywa1ker404.absencium.ui.theme.LocalGlassEnabled
import skywa1ker404.absencium.ui.theme.ThemePalette
import skywa1ker404.absencium.ui.theme.palette

/**
 * The Presets page: a full-screen page over the settings with every colour scheme, its name and four swatches.
 * Tapping one applies it straight away (the page itself changes colour) and saves it.
 */
@Composable
fun PresetsPage(
    selected: ThemePreset?,
    isDark: Boolean,
    onSelect: (ThemePreset) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Presets", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = "Tap one to use it everywhere. Light and dark follow your theme mode where a scheme has both.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onDismiss) { Text("Done") }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // "No colour" is offered only while Liquid Glass is on, and is listed first.
                val glassOn = LocalGlassEnabled.current
                val presets = (if (glassOn) listOf(ThemePreset.NO_COLOUR) else emptyList()) +
                    ThemePreset.entries.filter { it != ThemePreset.NO_COLOUR }
                items(presets, key = { it.name }) { preset ->
                    PresetRow(
                        palette = preset.palette(),
                        isDark = isDark,
                        selected = preset == selected,
                        onClick = { onSelect(preset) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetRow(palette: ThemePalette, isDark: Boolean, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val (_, variant) = palette.variantFor(isDark)
    // The swatches show the scheme's own colours: its background, then its three accents.
    val swatches = listOf(variant.background, variant.primary, variant.secondary, variant.tertiary)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(if (selected) colors.primaryContainer else colors.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .semantics {
                this.selected = selected
                contentDescription = "${palette.name}, ${palette.variants}"
            }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = palette.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) colors.onPrimaryContainer else colors.onSurface,
            )
            Text(
                text = palette.variants,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            swatches.forEachIndexed { index, color ->
                if (index > 0) Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(color))
            }
        }
        if (selected) {
            Text(
                text = "✓",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onPrimaryContainer,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}
