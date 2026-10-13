package com.trajectory.vault.ui.main

import com.trajectory.vault.ui.theme.LocalQuality
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trajectory.vault.ui.components.floatingGlass
import com.trajectory.vault.ui.icons.AppIcons
import com.kyant.backdrop.backdrops.LayerBackdrop

class MainTab(val label: String, val icon: ImageVector)

private val PillHeight = 64.dp
private val ItemHeight = 48.dp
private val PillPadding = 8.dp

/** Width of every tab slot. The selected tab gets extra room for its label on top of this. */
private val ItemBase = 52.dp
private val LabelGap = 6.dp
private val LabelPadding = 16.dp

/** How much of tab [index] is "open" (shows its label) when the highlight is at [position]. */
private fun openWeight(index: Int, position: Float): Float = (1f - abs(position - index)).coerceAtLeast(0f)

/**
 * The floating navigation pill. Only the highlighted tab shows its label; the highlight springs to
 * the selected tab and can be dragged across the tabs. While dragging, the page switches as soon
 * as the highlight is over another tab, before the finger lifts. While [compact] the pill shrinks
 * to icons only.
 */
@Composable
fun BottomPill(
    tabs: List<MainTab>,
    selectedIndex: Int,
    compact: () -> Boolean,
    backdrop: LayerBackdrop?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val q = LocalQuality.current
    val colors = MaterialTheme.colorScheme
    val isDarkSurface = colors.surface.luminance() < 0.5f
    // Read here, not in the caller, so collapsing the pill only redraws the pill and not the whole screen.
    val isCompact = compact()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val glass = backdrop != null
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelLarge
    val lastIndex = tabs.lastIndex

    val basePx = with(density) { ItemBase.toPx() }
    val paddingPx = with(density) { PillPadding.toPx() }
    val labelExtraPx = with(density) { (LabelGap + LabelPadding).toPx() }
    // How much wider each tab gets when it is the selected one (room for its label).
    val extraPx = remember(tabs, labelStyle, density) {
        tabs.map { textMeasurer.measure(it.label, labelStyle).size.width + labelExtraPx }
    }
    val openFactor by animateFloatAsState(
        targetValue = if (isCompact) 0f else 1f,
        animationSpec = q.spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "pillOpen",
    )
    val open = openFactor.coerceIn(0f, 1f)

    // The highlight's position in tab units (1.5 is halfway between tab 1 and tab 2). It springs to
    // the selected tab, and follows the finger while it is being dragged.
    val indicator = remember { Animatable(selectedIndex.toFloat()) }
    var dragPosition by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(selectedIndex) {
        if (dragPosition == null) {
            indicator.animateTo(
                selectedIndex.toFloat(),
                q.spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
            )
        }
    }
    val position = (dragPosition ?: indicator.value).coerceIn(0f, lastIndex.toFloat())
    // The highlight swells a little while it is held and dragged.
    val zoom by animateFloatAsState(
        targetValue = if (dragPosition != null) 1.22f else 1f,
        animationSpec = q.spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pillZoom",
    )

    // Geometry of the pill for a highlight position, in pixels. Slot widths follow the position, so
    // the label room travels with the highlight.
    fun slotWidth(index: Int, p: Float) = basePx + extraPx[index] * openWeight(index, p) * open
    fun slotLeft(index: Int, p: Float): Float {
        var left = paddingPx
        for (i in 0 until index) left += slotWidth(i, p)
        return left
    }
    fun indicatorLeft(p: Float): Float {
        val k = p.toInt().coerceIn(0, lastIndex)
        val k2 = (k + 1).coerceAtMost(lastIndex)
        val f = if (k == k2) 0f else (p - k).coerceIn(0f, 1f)
        return slotLeft(k, p) + (slotLeft(k2, p) - slotLeft(k, p)) * f
    }
    fun indicatorWidth(p: Float): Float {
        val k = p.toInt().coerceIn(0, lastIndex)
        val k2 = (k + 1).coerceAtMost(lastIndex)
        val f = if (k == k2) 0f else (p - k).coerceIn(0f, 1f)
        return basePx + (extraPx[k] * (1f - f) + extraPx[k2] * f) * open
    }
    fun indicatorCenter(p: Float) = indicatorLeft(p) + indicatorWidth(p) / 2f
    val totalWidthPx = paddingPx * 2 + (0 until tabs.size).sumOf { slotWidth(it, position).toDouble() }.toFloat()

    val currentGeometryOpen by rememberUpdatedState(open)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentSelected by rememberUpdatedState(selectedIndex)
    val currentExtras by rememberUpdatedState(extraPx)

    // Not clipped, so the swollen highlight can reach past the edge of the pill.
    Box(modifier = modifier.width(with(density) { totalWidthPx.toDp() }).height(PillHeight)) {
        Box(modifier = Modifier.matchParentSize().floatingGlass(backdrop, CircleShape))
        Box(
            modifier = Modifier
                .offset { IntOffset(indicatorLeft(position).roundToInt(), ((PillHeight - ItemHeight) / 2).roundToPx()) }
                .width(with(density) { indicatorWidth(position).toDp() })
                .height(ItemHeight)
                .graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                }
                .clip(CircleShape)
                .background(
                    if (glass) {
                        // Plain glass: the selected tab is a faint lighter or darker puck, never a colour.
                        if (isDarkSurface) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.08f)
                    } else {
                        colors.secondaryContainer
                    },
                )
                .border(
                    1.dp,
                    if (glass) {
                        Color.White.copy(alpha = if (isDarkSurface) 0.28f else 0.7f)
                    } else {
                        colors.secondary.copy(alpha = 0.55f)
                    },
                    CircleShape,
                ),
        )
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .pointerInput(tabs.size, basePx, paddingPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val extras = currentExtras
                        val openNow = currentGeometryOpen
                        fun weight(i: Int, p: Float) = (1f - abs(p - i)).coerceAtLeast(0f)
                        fun leftAt(p: Float): Float {
                            val k = p.toInt().coerceIn(0, lastIndex)
                            val k2 = (k + 1).coerceAtMost(lastIndex)
                            val f = if (k == k2) 0f else (p - k).coerceIn(0f, 1f)
                            fun slotLeftAt(index: Int): Float {
                                var l = paddingPx
                                for (i in 0 until index) l += basePx + extras[i] * weight(i, p) * openNow
                                return l
                            }
                            return slotLeftAt(k) + (slotLeftAt(k2) - slotLeftAt(k)) * f
                        }
                        fun widthAt(p: Float): Float {
                            val k = p.toInt().coerceIn(0, lastIndex)
                            val k2 = (k + 1).coerceAtMost(lastIndex)
                            val f = if (k == k2) 0f else (p - k).coerceIn(0f, 1f)
                            return basePx + (extras[k] * (1f - f) + extras[k2] * f) * openNow
                        }
                        fun centerAt(p: Float) = leftAt(p) + widthAt(p) / 2f

                        val start = dragPosition ?: indicator.value
                        val center = centerAt(start)
                        // Only a touch that starts on the highlight drags it; other touches are plain taps.
                        if (abs(down.position.x - center) > widthAt(start) / 2f) return@awaitEachGesture
                        val grab = down.position.x - center

                        // The highlight's position whose centre is under the finger (the centre
                        // grows steadily with the position, so a bisection finds it).
                        fun positionAt(x: Float): Float {
                            val wanted = x - grab
                            var lo = 0f
                            var hi = lastIndex.toFloat()
                            repeat(18) {
                                val mid = (lo + hi) / 2f
                                if (centerAt(mid) < wanted) lo = mid else hi = mid
                            }
                            return ((lo + hi) / 2f).coerceIn(0f, lastIndex.toFloat())
                        }

                        var lastCommitted = currentSelected
                        fun update(x: Float) {
                            val p = positionAt(x)
                            dragPosition = p
                            // The page follows the highlight right away, before the finger lifts.
                            val over = p.roundToInt().coerceIn(0, lastIndex)
                            if (over != lastCommitted) {
                                lastCommitted = over
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentOnSelect(over)
                            }
                        }

                        val slop = awaitHorizontalTouchSlopOrCancellation(down.id) { change, _ ->
                            change.consume()
                        } ?: return@awaitEachGesture
                        update(slop.position.x)
                        horizontalDrag(slop.id) { change ->
                            change.consume()
                            update(change.position.x)
                        }
                        val releasedAt = dragPosition ?: start
                        val target = releasedAt.roundToInt().coerceIn(0, lastIndex)
                        scope.launch {
                            // Hand the finger's position to the spring so the highlight settles smoothly.
                            indicator.snapTo(releasedAt)
                            dragPosition = null
                            indicator.animateTo(
                                target.toFloat(),
                                q.spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
                            )
                        }
                        if (target != lastCommitted) currentOnSelect(target)
                    }
                }
                .padding(start = PillPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, tab ->
                val weight = openWeight(index, position) * open
                PillTab(
                    tab = tab,
                    weight = weight,
                    labelWidth = with(density) { (extraPx[index] - labelExtraPx).toDp() },
                    glass = glass,
                    onClick = { onSelect(index) },
                    modifier = Modifier
                        .width(with(density) { slotWidth(index, position).toDp() })
                        .height(ItemHeight),
                )
            }
        }
    }
}

/** One tab: its icon always, and its label growing in as the highlight arrives ([weight] 0 to 1). */
@Composable
private fun PillTab(
    tab: MainTab,
    weight: Float,
    labelWidth: androidx.compose.ui.unit.Dp,
    glass: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val q = LocalQuality.current
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = q.spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "tabPress",
    )
    val selectedColor = if (glass) colors.onSurface else colors.onSecondaryContainer
    val tint = androidx.compose.ui.graphics.lerp(colors.onSurfaceVariant, selectedColor, weight.coerceIn(0f, 1f))
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
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Box(
            modifier = Modifier
                .width(((labelWidth + LabelGap) * weight.coerceIn(0f, 1f)).coerceAtLeast(0.dp))
                .clipToBounds(),
        ) {
            Text(
                text = tab.label,
                style = MaterialTheme.typography.labelLarge,
                color = tint,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .padding(start = LabelGap)
                    .wrapContentWidth(align = Alignment.Start, unbounded = true)
                    .alpha(weight.coerceIn(0f, 1f)),
            )
        }
    }
}

/** The contextual action button above the bar (Add Entry on Attendance, New Entry in the Vault). */
@Composable
fun ShellFab(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = null,
) {
    val q = LocalQuality.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = q.spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "fabPress",
    )
    if (backdrop != null) {
        // Glass mode: a plain glass pill with no colour of its own.
        Row(
            modifier = modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .floatingGlass(backdrop, CircleShape)
                .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        return
    }
    Button(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
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
fun ShellSearchButton(backdrop: LayerBackdrop?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val q = LocalQuality.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = q.spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "searchPress",
    )
    Box(
        modifier = modifier
            .size(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .floatingGlass(backdrop, CircleShape)
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
    val q = LocalQuality.current
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
            text = "Absencium",
            style = MaterialTheme.typography.titleLarge,
            color = colors.primary,
        )
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (pressed) 0.9f else 1f,
            animationSpec = q.spring(dampingRatio = Spring.DampingRatioMediumBouncy),
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

/**
 * Fades a floating control out and slides it off the right edge while the content scrolls down.
 * [hidden] is read inside this composable, so a change redraws only this control.
 */
@Composable
fun SlideAway(hidden: () -> Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val q = LocalQuality.current
    val isHidden = hidden()
    val offsetX by animateDpAsState(
        targetValue = if (isHidden) 120.dp else 0.dp,
        animationSpec = q.spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "slideAway",
    )
    val alpha by animateFloatAsState(
        targetValue = if (isHidden) 0f else 1f,
        animationSpec = q.spring(stiffness = Spring.StiffnessMediumLow),
        label = "slideAwayAlpha",
    )
    Box(
        modifier = modifier
            .graphicsLayer { translationX = offsetX.toPx() }
            .alpha(alpha),
    ) { content() }
}

val ScrimColor = Color(0x99000000)
