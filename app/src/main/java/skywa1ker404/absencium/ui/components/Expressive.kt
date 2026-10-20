package skywa1ker404.absencium.ui.components

import skywa1ker404.absencium.ui.theme.LocalQuality
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import skywa1ker404.absencium.data.db.AttendanceStatus
import skywa1ker404.absencium.ui.icons.AppIcons
import kotlin.math.roundToInt

/**
 * A filter chip that squashes while pressed, grows a check mark with a spring when it becomes
 * selected, and animates its own width while doing so.
 */
@Composable
fun ExpressiveChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CircleShape,
) {
    val q = LocalQuality.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = q.spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "chipPress",
    )
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .animateContentSize(q.spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)),
        enabled = enabled,
        shape = shape,
        leadingIcon = if (selected) {
            { Icon(AppIcons.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else {
            null
        },
        interactionSource = interaction,
    )
}

/**
 * The Present / Absent switch of the attendance card. One highlight slides between the two halves
 * with a bounce and changes colour (primary for present, error for absent); when nothing is logged
 * yet the highlight is hidden. The highlight also pulses when the choice changes.
 */
@Composable
fun AttendanceToggle(
    selected: AttendanceStatus?,
    enabled: Boolean,
    onPresent: () -> Unit,
    onAbsent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val q = LocalQuality.current
    val colors = MaterialTheme.colorScheme
    val position by animateFloatAsState(
        targetValue = if (selected == AttendanceStatus.ABSENT) 1f else 0f,
        animationSpec = q.spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "toggleSlide",
    )
    val highlightAlpha by animateFloatAsState(
        targetValue = if (selected == null) 0f else 1f,
        animationSpec = q.spring(stiffness = Spring.StiffnessMedium),
        label = "toggleAlpha",
    )
    val highlightColor by animateColorAsState(
        targetValue = if (selected == AttendanceStatus.ABSENT) colors.errorContainer else colors.primaryContainer,
        animationSpec = q.spring(stiffness = Spring.StiffnessMedium),
        label = "toggleColor",
    )
    val presentTint by animateColorAsState(
        targetValue = if (selected == AttendanceStatus.PRESENT) colors.onPrimaryContainer else colors.onSurfaceVariant,
        label = "presentTint",
    )
    val absentTint by animateColorAsState(
        targetValue = if (selected == AttendanceStatus.ABSENT) colors.onErrorContainer else colors.onSurfaceVariant,
        label = "absentTint",
    )
    // A double tap on the same half would log the status and then clear it again, so the second tap is ignored.
    val presentTap = rememberTapGuard(onClick = onPresent)
    val absentTap = rememberTapGuard(onClick = onAbsent)
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        if (selected != null) {
            pulse.snapTo(0.9f)
            pulse.animateTo(1f, q.spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .height(52.dp)
            .glassControl(CircleShape, colors.surfaceContainerHighest, 0.6f)
            .alpha(if (enabled) 1f else 0.5f),
    ) {
        val half = maxWidth / 2
        Box(
            modifier = Modifier
                .offset { IntOffset((half.toPx() * position).roundToInt(), 0) }
                .width(half)
                .fillMaxHeight()
                .padding(4.dp)
                .graphicsLayer {
                    alpha = highlightAlpha
                    scaleX = pulse.value
                    scaleY = pulse.value
                }
                .flatGlassFill(CircleShape, highlightColor, 0.82f),
        )
        Row(modifier = Modifier.fillMaxSize()) {
            ToggleHalf("Present today", presentTint, enabled, presentTap, Modifier.weight(1f))
            ToggleHalf("Absent today", absentTint, enabled, absentTap, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ToggleHalf(
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val q = LocalQuality.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = q.spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "halfPress",
    )
    Box(
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                // No ripple: its half-width pill outlived the slide and showed as a ghost behind the highlight. The press
                // scale and the sliding highlight are the feedback.
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = tint, maxLines = 1)
    }
}
