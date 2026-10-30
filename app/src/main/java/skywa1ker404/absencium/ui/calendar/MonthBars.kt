package skywa1ker404.absencium.ui.calendar

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import skywa1ker404.absencium.ui.components.drawHatch
import skywa1ker404.absencium.ui.theme.LocalQuality
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/** One capsule of the chart: a month and the share of its marked days that were Present, or null when nothing was marked. */
class MonthBar(val month: YearMonth, val percent: Double?)

/**
 * The last months as tall capsules (image 08): the empty part is hatched, the filled part rises with the attendance, the viewed month is
 * solid with its percentage in a bubble above it, and a month under the [threshold] is filled in the error colour. Tapping one opens that month.
 */
@Composable
internal fun MonthBars(bars: List<MonthBar>, selected: YearMonth, threshold: Int, onPick: (YearMonth) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val q = LocalQuality.current
    // Six months fit the width; earlier and later ones scroll sideways, and the viewed month is brought to the middle.
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
    val slot = maxWidth / 6
    val scroll = androidx.compose.foundation.rememberScrollState()
    val slotPx = with(androidx.compose.ui.platform.LocalDensity.current) { slot.roundToPx() }
    androidx.compose.runtime.LaunchedEffect(selected, bars.size) {
        val index = bars.indexOfFirst { it.month == selected }.coerceAtLeast(0)
        scroll.animateScrollTo((index * slotPx - slotPx * 5 / 2).coerceAtLeast(0))
    }
    Row(modifier = Modifier.horizontalScroll(scroll), horizontalArrangement = Arrangement.Start) {
        bars.forEach { bar ->
            val on = bar.month == selected
            val fraction by animateFloatAsState(
                targetValue = ((bar.percent ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f),
                animationSpec = q.spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "monthBar",
            )
            val low = bar.percent != null && bar.percent < threshold
            val fill = when {
                low -> if (on) colors.error else colors.errorContainer
                on -> colors.primary
                else -> colors.primaryContainer
            }
            val label = bar.month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(slot)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onPick(bar.month) }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .semantics { contentDescription = "$label, ${bar.percent?.roundToInt()?.let { "$it percent present" } ?: "nothing marked"}" },
            ) {
                // The bubble keeps its height on every bar so the capsules line up.
                Box(modifier = Modifier.height(24.dp), contentAlignment = Alignment.Center) {
                    if (on && bar.percent != null) {
                        Text(
                            "${bar.percent.roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.inverseOnSurface,
                            modifier = Modifier.clip(CircleShape).background(colors.inverseSurface).padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
                val track = colors.onSurfaceVariant.copy(alpha = 0.28f)
                Box(
                    modifier = Modifier
                        .width(26.dp)
                        .height(92.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(colors.surfaceContainerHighest.copy(alpha = 0.6f))
                        .drawBehind { drawHatch(track, spacing = 5.dp) },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction)
                            .clip(RoundedCornerShape(13.dp))
                            .background(fill),
                    )
                }
                Text(
                    label,
                    style = if (on) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
                    color = if (on) colors.primary else colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                    maxLines = 1,
                )
            }
        }
    }
    }
}

/** A tinted tile with a big number and its name under it; three of them sit in a row. */
@Composable
internal fun StatTile(label: String, value: String, container: androidx.compose.ui.graphics.Color, content: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(container)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, color = content, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelMedium, color = content.copy(alpha = 0.8f), maxLines = 1)
    }
}
