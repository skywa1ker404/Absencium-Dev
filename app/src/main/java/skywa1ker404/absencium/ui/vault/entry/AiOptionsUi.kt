package skywa1ker404.absencium.ui.vault.entry

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import skywa1ker404.absencium.ui.theme.LocalQuality

/**
 * A group of connected choices (one is always picked). The picked one is a filled, rounder pill that springs between
 * segments; the others stay square-ish, like the connected button groups of Material 3 Expressive.
 */
@Composable
internal fun <T> ChoiceBar(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val q = LocalQuality.current
    Row(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(colors.surfaceContainerHighest).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val corner by animateDpAsState(
                targetValue = if (pressed) 10.dp else if (isSelected) 26.dp else 14.dp,
                animationSpec = q.spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "choiceCorner",
            )
            val fill by animateColorAsState(if (isSelected) colors.primary else colors.surfaceContainerHighest, q.spring(stiffness = Spring.StiffnessMedium), label = "choiceFill")
            val ink by animateColorAsState(if (isSelected) colors.onPrimary else colors.onSurfaceVariant, q.spring(stiffness = Spring.StiffnessMedium), label = "choiceInk")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(corner))
                    .background(fill)
                    .clickable(enabled = enabled, interactionSource = interaction, indication = androidx.compose.foundation.LocalIndication.current) { onSelect(option) }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label(option), style = MaterialTheme.typography.labelLarge, color = ink, textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
}

/** A rounded group with an icon, a title and a line of help, holding related options. */
@Composable
internal fun OptionGroup(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    help: String? = null,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(colors.surfaceContainerHigh).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(colors.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = null, tint = colors.onSecondaryContainer, modifier = Modifier.size(22.dp)) }
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (help != null) Text(help, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
        content()
    }
}
