package skywa1ker404.absencium.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import skywa1ker404.absencium.ui.theme.LocalGlassEnabled

/**
 * The app's pop-up menu: big rounded corners, a hairline edge and, with Liquid Glass, a see-through tint in the theme's colours.
 * (A pop-up is its own window, so it cannot blur what is behind it; the tint is kept dense enough to read.)
 */
@Composable
fun AppMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    /** A fully solid container, for long menus whose text must not show what is behind them. */
    solid: Boolean = false,
    offset: androidx.compose.ui.unit.DpOffset = androidx.compose.ui.unit.DpOffset(0.dp, 0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val glass = LocalGlassEnabled.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier,
        offset = offset,
        shape = RoundedCornerShape(28.dp),
        containerColor = if (glass && !solid) colors.surfaceContainerHigh.copy(alpha = 0.94f) else colors.surfaceContainerHigh,
        tonalElevation = 0.dp,
        shadowElevation = 10.dp,
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = if (glass) 0.18f else 0.08f)),
        content = content,
    )
}

/** One row of an [AppMenu]: an icon on a small rounded chip, then the label. [destructive] colours it as a warning. */
@Composable
fun AppMenuItem(icon: ImageVector, label: String, onClick: () -> Unit, destructive: Boolean = false, enabled: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    val chip = if (destructive) colors.errorContainer else colors.secondaryContainer
    val on = if (destructive) colors.onErrorContainer else colors.onSecondaryContainer
    DropdownMenuItem(
        text = { Text(label, style = MaterialTheme.typography.bodyLarge, color = if (destructive) colors.error else colors.onSurface) },
        onClick = onClick,
        enabled = enabled,
        leadingIcon = {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(chip),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = null, tint = on, modifier = Modifier.size(20.dp)) }
        },
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp).clip(RoundedCornerShape(18.dp)),
        colors = MenuDefaults.itemColors(),
    )
}

/** A thin line between groups of rows in an [AppMenu]. */
@Composable
fun AppMenuDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}
