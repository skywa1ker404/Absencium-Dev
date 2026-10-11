package com.trajectory.vault.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trajectory.vault.ui.theme.LocalGlassBlur
import com.trajectory.vault.ui.theme.LocalHazeState
import dev.chrisbanes.haze.*
import dev.chrisbanes.haze.blur.*

/**
 * The look of a floating element (top bar, floating navigation bar). With a [hazeState] (Liquid Glass
 * on Android 12+) it is frosted glass that blurs whatever scrolls behind it; otherwise it is a
 * solid [tint] surface.
 */
@Composable
fun Modifier.floatingGlass(
    hazeState: HazeState?,
    shape: Shape,
    tint: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    bordered: Boolean = true,
): Modifier {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val borderModifier = if (bordered) {
        Modifier.border(1.dp, Color.White.copy(alpha = if (isDark) 0.16f else 0.55f), shape)
    } else {
        Modifier
    }
    if (hazeState == null) {
        return this.clip(shape).background(tint).then(borderModifier)
    }
    val blur = LocalGlassBlur.current
    val style = remember(tint, blur) {
        HazeBlurStyle {
            blurRadius(blur)
            noiseFactor(0.06f)
            colorEffects(listOf(HazeColorEffect.tint(tint.copy(alpha = 0.55f))))
            fallbackColorEffect(HazeColorEffect.tint(tint.copy(alpha = 0.92f)))
        }
    }
    return this
        .clip(shape)
        .hazeBlur(input = HazeInput.Backdrop(hazeState), style = style)
        .then(borderModifier)
}

class NavTab(val label: String, val emoji: String)

/**
 * A pill-shaped navigation bar floating above the content. The selected tab grows into a
 * pill with its label; the others show only their icon.
 */
@Composable
fun FloatingNavBar(
    tabs: List<NavTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hazeState = LocalHazeState.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .floatingGlass(hazeState, CircleShape)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, tab ->
                NavItem(tab = tab, selected = index == selectedIndex, onClick = { onSelect(index) })
            }
        }
    }
}

@Composable
private fun NavItem(tab: NavTab, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "navPress",
    )
    val container by animateColorAsState(
        targetValue = if (selected) colors.primaryContainer else Color.Transparent,
        animationSpec = spring(),
        label = "navContainer",
    )
    val contentColor = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant
    Row(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(container)
            .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
            .semantics { contentDescription = tab.label },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = tab.emoji, fontSize = 20.sp)
        if (selected) {
            Text(
                text = tab.label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
