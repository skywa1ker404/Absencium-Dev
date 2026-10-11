package com.trajectory.vault.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trajectory.vault.ui.components.floatingGlass
import com.trajectory.vault.ui.icons.AppIcons
import dev.chrisbanes.haze.HazeState

class MainTab(val label: String, val icon: ImageVector)

private val PillHeight = 64.dp
private val ItemHeight = 48.dp
private val PillPadding = 8.dp

/** Width of one tab in the expanded (icon and label) and compact (icon only) pill. */
private val ExpandedItemWidth = 84.dp
private val CompactItemWidth = 48.dp

/** Width of the whole pill for [count] tabs. */
fun pillWidth(count: Int, compact: Boolean) =
    (if (compact) CompactItemWidth else ExpandedItemWidth) * count + PillPadding * 2

/**
 * The floating navigation pill. A sliding indicator springs to the selected tab, and while
 * [compact] the pill narrows to icons only.
 */
@Composable
fun BottomPill(
    tabs: List<MainTab>,
    selectedIndex: Int,
    compact: Boolean,
    hazeState: HazeState?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val width by animateDpAsState(
        targetValue = pillWidth(tabs.size, compact),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "pillWidth",
    )
    val position by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "pillIndicator",
    )
    val itemWidth = (width - PillPadding * 2) / tabs.size

    Box(
        modifier = modifier
            .width(width)
            .height(PillHeight)
            .floatingGlass(hazeState, CircleShape),
    ) {
        Box(
            modifier = Modifier
                .offset(x = PillPadding + itemWidth * position, y = (PillHeight - ItemHeight) / 2)
                .width(itemWidth)
                .height(ItemHeight)
                .clip(CircleShape)
                .background(colors.primaryContainer)
                .border(1.dp, colors.primary.copy(alpha = 0.4f), CircleShape),
        )
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = PillPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, tab ->
                PillTab(
                    tab = tab,
                    selected = index == selectedIndex,
                    compact = compact,
                    onClick = { onSelect(index) },
                    modifier = Modifier.width(itemWidth).height(ItemHeight),
                )
            }
        }
    }
}

@Composable
private fun PillTab(
    tab: MainTab,
    selected: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "tabPress",
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
        animationSpec = spring(),
        label = "tabLabel",
    )
    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick)
            .semantics { contentDescription = tab.label },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = null,
            tint = labelColor,
            modifier = Modifier.size(22.dp),
        )
        AnimatedVisibility(
            visible = !compact,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally(),
        ) {
            Text(
                text = tab.label,
                style = MaterialTheme.typography.labelLarge,
                color = labelColor,
                maxLines = 1,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

/** The contextual action button above the bar (Add Exam on Attends, New Note in the Vault). */
@Composable
fun ShellFab(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "fabPress",
    )
    Button(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        interactionSource = interaction,
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Text(text = label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 8.dp))
    }
}

/** The round floating search button that sits above the navigation pill. */
@Composable
fun ShellSearchButton(hazeState: HazeState?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "searchPress",
    )
    Box(
        modifier = modifier
            .size(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .floatingGlass(hazeState, CircleShape)
            .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick)
            .semantics { contentDescription = "Search" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = AppIcons.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** The top bar: the app name on the left and the profile avatar (initials) on the right. */
@Composable
fun ShellTopBar(displayName: String, onProfile: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Trajectory",
            style = MaterialTheme.typography.titleLarge,
            color = colors.primary,
        )
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (pressed) 0.9f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "avatarPress",
        )
        Box(
            modifier = Modifier
                .size(44.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .background(colors.primaryContainer)
                .border(1.dp, colors.outlineVariant, CircleShape)
                .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onProfile)
                .semantics { contentDescription = "Profile" },
            contentAlignment = Alignment.Center,
        ) {
            val initials = initialsOf(displayName)
            if (initials == null) {
                Icon(AppIcons.Person, contentDescription = null, tint = colors.onPrimaryContainer)
            } else {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onPrimaryContainer,
                )
            }
        }
    }
}

/** Up to two initials from a name, or null when no name is set. */
fun initialsOf(name: String): String? {
    val parts = name.trim().split(" ").filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> null
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}

/** Fades a floating control out and slides it off the right edge while the content scrolls down. */
@Composable
fun Modifier.slideAwayWhen(hidden: Boolean): Modifier {
    val offsetX by animateDpAsState(
        targetValue = if (hidden) 120.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "slideAway",
    )
    val alpha by animateFloatAsState(
        targetValue = if (hidden) 0f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "slideAwayAlpha",
    )
    return this.offset(x = offsetX).alpha(alpha)
}

val ScrimColor = Color(0x99000000)
