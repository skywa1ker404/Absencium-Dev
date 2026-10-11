package com.trajectory.vault.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trajectory.vault.ui.theme.LocalGlassEnabled
import com.trajectory.vault.ui.theme.LocalHazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlin.math.roundToInt

/**
 * The standard content card: a rounded tonal surface (low layer, higher layer when [highlighted]
 * or pressed). It squashes slightly while pressed and springs when its content changes size.
 * Glass is used only on floating elements (top bar, floating bars), never on content,
 * so text on cards always has solid contrast.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    highlighted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "cardPress",
    )
    val container by animateColorAsState(
        targetValue = if (highlighted || pressed) colors.surfaceContainerHigh else colors.surfaceContainerLow,
        animationSpec = spring(),
        label = "cardColor",
    )
    val clickModifier = if (onClick != null) {
        Modifier.clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick)
    } else {
        Modifier
    }
    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)),
        shape = shape,
        color = container,
        border = if (highlighted) BorderStroke(2.dp, colors.primary) else null,
    ) {
        Column(modifier = Modifier.then(clickModifier).padding(20.dp), content = content)
    }
}

/**
 * A themed screen: a top bar, a scrolling column of content and an optional floating [bottomBar].
 * The content scrolls underneath the bars. With the Liquid Glass engine the bars are frosted
 * glass that blur the content passing behind them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val glass = LocalGlassEnabled.current
    val hazeState = if (glass) rememberHazeState() else null
    val colors = MaterialTheme.colorScheme

    CompositionLocalProvider(LocalHazeState provides hazeState) {
        Scaffold(
            modifier = modifier,
            containerColor = colors.background,
            topBar = {
                TopAppBar(
                    title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.semantics { contentDescription = "Back" },
                            ) {
                                Text("←", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    modifier = Modifier.floatingGlass(
                        hazeState = hazeState,
                        shape = RectangleShape,
                        tint = colors.background,
                        bordered = false,
                    ),
                )
            },
            bottomBar = bottomBar,
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (hazeState != null) Modifier.hazeSource(hazeState) else Modifier)
                    .background(colors.background),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(padding)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content,
                )
            }
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 12.dp, start = 4.dp),
    )
}

/** A selectable card with a radio button. A disabled option is greyed out and explains why. */
@Composable
fun OptionCard(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f),
        onClick = if (enabled) onClick else null,
        highlighted = selected,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RadioButton(selected = selected, onClick = null, enabled = enabled)
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private val AccentPresets = listOf(
    Color(0xFF6750A4),
    Color(0xFF3F51B5),
    Color(0xFF1E6FD9),
    Color(0xFF00897B),
    Color(0xFF43A047),
    Color(0xFFF57C00),
    Color(0xFFD32F2F),
    Color(0xFFD81B60),
)

/** Preset accent swatches. The chosen one gets a ring and a check mark, and springs when tapped. */
@Composable
fun AccentColorPicker(
    selected: Color,
    onSelected: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AccentPresets.chunked(4).forEach { rowColors ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                rowColors.forEach { preset ->
                    val isSelected = preset == selected
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.08f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "swatchScale",
                    )
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clip(CircleShape)
                            .background(preset)
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape,
                            )
                            .clickable { onSelected(preset) }
                            .semantics { contentDescription = "Accent color" },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Text("\u2713", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}

/** A card with a title, an optional description and a pill switch. Tapping anywhere on the card toggles it. */
@Composable
fun SwitchCard(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f),
        onClick = if (enabled) {
            { onCheckedChange(!checked) }
        } else {
            null
        },
        highlighted = checked,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(checked = checked, onCheckedChange = null, enabled = enabled)
        }
    }
}

/** A settings row that opens another screen: an emoji tile, a bold title and a short description. */
@Composable
fun NavCard(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = emoji, fontSize = 24.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "›",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A slider card for a whole-number setting. The value is saved once the finger lifts. */
@Composable
fun SliderCard(
    title: String,
    subtitle: String?,
    value: Int,
    range: IntRange,
    valueLabel: (Int) -> String,
    onValueChangeFinished: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember(value) { mutableFloatStateOf(value.toFloat()) }
    AppCard(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valueLabel(draft.roundToInt()),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = draft,
            onValueChange = { draft = it },
            onValueChangeFinished = { onValueChangeFinished(draft.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = range.last - range.first - 1,
        )
    }
}

/** A fully rounded button that springs when pressed and shows a spinner while [loading]. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    tonal: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pillPress",
    )
    val pillModifier = modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
    val padding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)
    val label: @Composable RowScope.() -> Unit = {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
    if (tonal) {
        FilledTonalButton(
            onClick = onClick,
            modifier = pillModifier,
            enabled = enabled && !loading,
            shape = CircleShape,
            contentPadding = padding,
            interactionSource = interaction,
            content = label,
        )
    } else {
        Button(
            onClick = onClick,
            modifier = pillModifier,
            enabled = enabled && !loading,
            shape = CircleShape,
            contentPadding = padding,
            interactionSource = interaction,
            content = label,
        )
    }
}

/** Progress dots for the wizard: the current step stretches into a pill. */
@Composable
fun StepIndicator(current: Int, total: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { index ->
            val barWidth by animateDpAsState(
                targetValue = if (index == current) 40.dp else 12.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "stepWidth",
            )
            val barColor by animateColorAsState(
                targetValue = if (index <= current) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                animationSpec = spring(),
                label = "stepColor",
            )
            Box(
                modifier = Modifier
                    .height(12.dp)
                    .width(barWidth)
                    .clip(CircleShape)
                    .background(barColor),
            )
        }
    }
}

/** A highlighted card for the result of an action ("Backup saved", "Could not use that folder"). */
@Composable
fun MessageCard(text: String, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier.fillMaxWidth(), highlighted = true) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Small explanatory text under a section. */
@Composable
fun HintText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 4.dp),
    )
}
