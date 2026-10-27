package skywa1ker404.absencium.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import skywa1ker404.absencium.ui.theme.LocalQuality
import kotlin.math.roundToInt

/**
 * A pill of tabs: a capsule of glass (a plain tonal capsule without Liquid Glass) with a bead that slides under the chosen tab
 * on a spring, the chosen tab showing its icon, and a light tick when it changes. Used for the tabs inside a screen; the
 * main 3-button navigation pill is a separate component and is not touched by this one.
 */
@Composable
fun <T> TabPill(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    icon: (T) -> ImageVector,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val q = LocalQuality.current
    val haptic = LocalHapticFeedback.current
    val index = options.indexOf(selected).coerceAtLeast(0)
    val position by animateFloatAsState(
        targetValue = index.toFloat(),
        animationSpec = q.spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "tabPill",
    )
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .glassPane(CircleShape, colors.surfaceContainerHigh, 0.6f)
            .padding(4.dp),
    ) {
        val tabWidth = maxWidth / options.size
        val tabWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { tabWidth.toPx() }
        Box(
            modifier = Modifier
                .offset { IntOffset((position * tabWidthPx).roundToInt(), 0) }
                .width(tabWidth)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(colors.primaryContainer),
        )
        Row(modifier = Modifier.fillMaxSize()) {
            options.forEach { option ->
                val chosen = option == selected
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Tab) {
                            if (!chosen) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(option)
                            }
                        },
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val tint = if (chosen) colors.onPrimaryContainer else colors.onSurfaceVariant
                    if (chosen) Icon(icon(option), contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                    Text(
                        text = label(option),
                        style = MaterialTheme.typography.labelLarge,
                        color = tint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
