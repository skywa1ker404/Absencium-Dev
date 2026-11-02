package skywa1ker404.absencium.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** A small coloured caption above a group of settings. */
@Composable
fun SettingCaption(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 14.dp, start = 6.dp),
    )
}

/** One card holding several compact rows, with a thin line between them. */
@Composable
fun SettingGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    AppCard(modifier = modifier.fillMaxWidth(), content = content)
}

@Composable
fun SettingDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

/** A compact row: a title, one short line under it, and anything at the end. The whole row is tappable when [onClick] is set. */
@Composable
fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .clip(RoundedCornerShape(14.dp))
            .then(if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 52.dp)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        trailing?.invoke()
    }
}

/** A [SettingRow] with a switch; tapping anywhere on the row flips it. */
@Composable
fun SettingSwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    SettingRow(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        enabled = enabled,
        onClick = { onCheckedChange(!checked) },
        trailing = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
    )
}
