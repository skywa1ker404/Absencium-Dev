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
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ToggleButton
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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
import androidx.compose.ui.graphics.Color
import skywa1ker404.absencium.ui.components.LocalFrostedPopup
import skywa1ker404.absencium.ui.theme.LocalQuality

/** A tonal fill that turns see-through inside a frosted (Liquid Glass) popup, so the glass shows through, like the app's cards. */
@Composable
private fun glassFill(base: Color): Color = if (LocalFrostedPopup.current) base.copy(alpha = base.alpha * 0.62f) else base

/**
 * A group of connected choices (one is always picked), built from the Material 3 Expressive connected button group:
 * the picked toggle button morphs to a fully rounded shape, the others keep squarer connected corners.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun <T> ChoiceBar(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    // Inside a frosted (Liquid Glass) popup the unpicked buttons' tonal fills turn see-through, like the rest of the card.
    val base = MaterialTheme.colorScheme
    val scheme = if (LocalFrostedPopup.current) {
        base.copy(surfaceContainer = glassFill(base.surfaceContainer), surfaceContainerHigh = glassFill(base.surfaceContainerHigh), surfaceContainerHighest = glassFill(base.surfaceContainerHighest))
    } else base
    MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography, shapes = MaterialTheme.shapes) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, option ->
            ToggleButton(
                checked = option == selected,
                onCheckedChange = { onSelect(option) },
                modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                enabled = enabled,
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
            ) {
                Text(label(option), maxLines = 1, textAlign = TextAlign.Center)
            }
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
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(glassFill(colors.surfaceContainerHigh)).padding(18.dp),
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
