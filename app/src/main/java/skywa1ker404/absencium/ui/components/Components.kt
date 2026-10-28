package skywa1ker404.absencium.ui.components

import skywa1ker404.absencium.ui.theme.LocalQuality
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
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.theme.LocalGlassEnabled
import skywa1ker404.absencium.ui.theme.LocalBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
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
    /** A tinted tile colour (for example a theme container). Defaults to the plain card surface. */
    container: Color? = null,
    /** A long press, for selecting the card. A card with only this still shows the press animation. */
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val q = LocalQuality.current
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = q.spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "cardPress",
    )
    val container by animateColorAsState(
        targetValue = when {
            // A chosen card is tinted toward the theme's accent container instead of being outlined.
            container != null -> container
            highlighted -> androidx.compose.ui.graphics.lerp(colors.surfaceContainerHigh, colors.primaryContainer, 0.55f)
            pressed -> colors.surfaceContainerHigh
            else -> colors.surfaceContainerLow
        },
        animationSpec = q.spring(),
        label = "cardColor",
    )
    val clickModifier = if (onClick != null || onLongClick != null) {
        Modifier.combinedClickable(
            interactionSource = interaction,
            indication = LocalIndication.current,
            onClick = { onClick?.invoke() },
            onLongClick = onLongClick,
        )
    } else {
        Modifier
    }
    val cardBackdrop = LocalCardBackdrop.current
    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .animateContentSize(animationSpec = q.spring(dampingRatio = Spring.DampingRatioLowBouncy))
            // Liquid Glass: the card is a pane of glass over the wallpaper, tinted with its own tonal colour.
            .then(
                when {
                    cardBackdrop != null -> Modifier.glassSurface(cardBackdrop, shape, container)
                    // In a frosted popup the card is a see-through tinted pane, so the popup's glass shows through it.
                    LocalFrostedPopup.current -> Modifier.flatPane(shape, container, 0.4f)
                    else -> Modifier
                },
            ),
        shape = shape,
        color = if (cardBackdrop != null || LocalFrostedPopup.current) Color.Transparent else container,
    ) {
        HostAnimatesSize {
            Column(modifier = Modifier.then(clickModifier).padding(20.dp), content = content)
        }
    }
}

/**
 * A themed screen: a top bar, a scrolling column of content and an optional floating [bottomBar].
 * The content scrolls underneath the bars. With the Liquid Glass engine the bars are frosted
 * glass that blur the content passing behind them. With [scrollable] false the content is not put in a scroll, for
 * screens whose content is a lazy list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {},
    scrollable: Boolean = true,
    /** Replaces the standard title bar (the note editor draws its own, with different buttons in edit and preview). */
    customTopBar: (@Composable () -> Unit)? = null,
    /** Popups of the screen. They sit above the page inside its glass layer, so with Liquid Glass they blur the page like the pill. */
    overlay: @Composable () -> Unit = {},
    /** Replaces the plain title text (the folder screen turns its title into a menu button). */
    titleContent: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val glass = LocalGlassEnabled.current
    val backdrop = if (glass) rememberLayerBackdrop() else null
    val colors = MaterialTheme.colorScheme

    CompositionLocalProvider(LocalBackdrop provides backdrop) {
      Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = modifier,
            containerColor = colors.background,
            topBar = {
                if (customTopBar != null) customTopBar() else TopAppBar(
                    title = { if (titleContent != null) titleContent() else Text(title, style = MaterialTheme.typography.headlineSmall) },
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.semantics { contentDescription = "Back" },
                            ) {
                                Icon(AppIcons.ArrowBack, contentDescription = null)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    // No solid band: a soft fade of the page colour, so scrolling content dissolves under the title
                    // instead of meeting a hard edge. The same fade as the Home, Calendar and Vault header.
                    modifier = Modifier.background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            0f to colors.background.copy(alpha = 0.9f),
                            0.7f to colors.background.copy(alpha = 0.55f),
                            1f to Color.Transparent,
                        ),
                    ),
                )
            },
            bottomBar = bottomBar,
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                    .background(colors.background),
            ) {
                GlassStage(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            // [scrollable] false: the content scrolls itself (a lazy list), so it must not sit in a scroll.
                            .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                            .padding(padding)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        content = content,
                    )
                }
            }
        }
        overlay()
      }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(top = 14.dp, start = 4.dp),
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
    icon: ImageVector,
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
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(26.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
    compact: Boolean = false,
) {
    val q = LocalQuality.current
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = q.spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pillPress",
    )
    val active = enabled && !loading
    val tap = rememberTapGuard(onClick = onClick)
    // With Liquid Glass the button is a pane of glass tinted in its usual colour; its own fill becomes clear.
    val glass = LocalCardBackdrop.current != null || LocalFrostedPopup.current
    val tint = if (tonal) colors.secondaryContainer else colors.primary
    val glassModifier = if (glass) Modifier.glassControl(CircleShape, tint, if (active) 0.78f else 0.3f) else Modifier
    val pillModifier = modifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(glassModifier)
    val padding = if (compact) PaddingValues(horizontal = 16.dp, vertical = 10.dp) else PaddingValues(horizontal = 28.dp, vertical = 14.dp)
    val label: @Composable RowScope.() -> Unit = {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
    if (tonal) {
        FilledTonalButton(
            onClick = tap,
            modifier = pillModifier,
            enabled = active,
            shape = CircleShape,
            contentPadding = padding,
            interactionSource = interaction,
            colors = if (glass) {
                ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                )
            } else {
                ButtonDefaults.filledTonalButtonColors()
            },
            content = label,
        )
    } else {
        Button(
            onClick = tap,
            modifier = pillModifier,
            enabled = active,
            shape = CircleShape,
            contentPadding = padding,
            interactionSource = interaction,
            colors = if (glass) {
                ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent)
            } else {
                ButtonDefaults.buttonColors()
            },
            content = label,
        )
    }
}

/**
 * Wraps a click so a second tap within [windowMs] of the first is ignored. A double tap on Save, Delete or Mark present
 * then does the thing once instead of twice (or, for a toggle, does it and undoes it).
 */
@Composable
fun rememberTapGuard(windowMs: Long = 450L, onClick: () -> Unit): () -> Unit {
    val latest by androidx.compose.runtime.rememberUpdatedState(onClick)
    val last = remember { longArrayOf(0L) }
    return remember(windowMs) {
        {
            val now = android.os.SystemClock.uptimeMillis()
            if (now - last[0] >= windowMs) {
                last[0] = now
                latest()
            }
        }
    }
}


/** Progress dots for the wizard: the current step stretches into a pill. */
@Composable
fun StepIndicator(current: Int, total: Int, modifier: Modifier = Modifier) {
    val q = LocalQuality.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { index ->
            val barWidth by animateDpAsState(
                targetValue = if (index == current) 40.dp else 12.dp,
                animationSpec = q.spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "stepWidth",
            )
            val barColor by animateColorAsState(
                targetValue = if (index <= current) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                animationSpec = q.spring(),
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

/** Greys out [content] and swallows every touch on it while [disabled], without changing its layout. */
@Composable
fun GreyedOut(disabled: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .alpha(if (disabled) 0.5f else 1f)
            .pointerInput(disabled) {
                if (disabled) {
                    awaitPointerEventScope {
                        while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                    }
                }
            },
    ) { content() }
}
