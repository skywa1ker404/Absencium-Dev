package skywa1ker404.absencium.ui.settings

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.theme.LocalQuality
import skywa1ker404.absencium.ui.theme.PaletteVariant

/** The colours a preview draws with: the page, a card on it, the text, and two accents. */
class PreviewColors(
    val background: Color,
    val card: Color,
    val text: Color,
    val primary: Color,
    val secondary: Color,
)

fun PaletteVariant.previewColors() = PreviewColors(
    background = background,
    card = lerp(background, text, 0.10f),
    text = text,
    primary = primary,
    secondary = secondary,
)

fun androidx.compose.material3.ColorScheme.previewColors() = PreviewColors(
    background = background,
    card = surfaceContainerHigh,
    text = onBackground,
    primary = primary,
    secondary = secondary,
)

/** The wallpaper colours Monet would use, or null where the phone has no Material You (before Android 12). */
@Composable
fun monetPreview(isDark: Boolean): PreviewColors? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val context = LocalContext.current
    return remember(isDark, context) {
        (if (isDark) androidx.compose.material3.dynamicDarkColorScheme(context) else androidx.compose.material3.dynamicLightColorScheme(context))
            .previewColors()
    }
}

/** A tiny drawing of the app in [colors]: a title bar, two cards and a floating pill. Purely a picture, it takes no touches. */
@Composable
fun MiniScreen(colors: PreviewColors, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.fillMaxWidth(0.4f).height(6.dp).clip(CircleShape).background(colors.text.copy(alpha = 0.85f)))
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(10.dp)).background(colors.primary))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(colors.card))
                Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(colors.secondary))
            }
        }
        Box(
            Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(0.6f).height(9.dp)
                .clip(CircleShape).background(colors.card),
        )
    }
}

/** A light/dark split preview for System Auto. */
@Composable
fun SplitMiniScreen(light: PreviewColors, dark: PreviewColors) {
    Box(Modifier.fillMaxSize()) {
        MiniScreen(light)
        // The dark screen is drawn again on top, clipped to the right half.
        Box(Modifier.fillMaxSize().clip(HalfShape)) { MiniScreen(dark) }
    }
}

/** The right half of a box. */
private val HalfShape = object : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density,
    ): androidx.compose.ui.graphics.Outline = androidx.compose.ui.graphics.Outline.Rectangle(
        androidx.compose.ui.geometry.Rect(size.width / 2f, 0f, size.width, size.height),
    )
}

/**
 * See-through glass over two soft colour blobs: a translucent pane with a thin rim and a faint sheen, no glow.
 * Drawn from the active theme so it matches what the real surfaces will look like.
 */
@Composable
fun GlassPreview() {
    val c = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(c.primaryContainer, c.tertiaryContainer, c.secondaryContainer))),
    ) {
        Box(Modifier.align(Alignment.TopStart).padding(10.dp).size(26.dp).clip(CircleShape).background(c.primary.copy(alpha = 0.8f)))
        Box(Modifier.align(Alignment.BottomEnd).padding(14.dp).size(30.dp).clip(CircleShape).background(c.tertiary.copy(alpha = 0.7f)))
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.74f)
                .fillMaxSize(0.52f)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.42f), Color.White.copy(alpha = 0.16f))))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)), RoundedCornerShape(14.dp)),
        )
    }
}

/**
 * A choice shown as a picture with a one-line name under it. The chosen tile gets a rim and a check badge and grows slightly;
 * every tile springs when pressed. [preview] fills the picture area.
 */
@Composable
fun PreviewTile(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    aspect: Float = 0.9f,
    preview: @Composable BoxScope.() -> Unit,
) {
    val q = LocalQuality.current
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.94f
            selected -> 1.03f
            else -> 1f
        },
        animationSpec = q.spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "previewTile",
    )
    val rim by animateColorAsState(if (selected) colors.primary else colors.outlineVariant, label = "previewRim")
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .semantics {
                this.selected = selected
                contentDescription = title
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspect)
                .clip(shape)
                .border(BorderStroke(if (selected) 2.5.dp else 1.dp, rim), shape)
                .clickable(enabled = enabled, interactionSource = interaction, indication = androidx.compose.foundation.LocalIndication.current, onClick = onClick),
        ) {
            preview()
            if (selected) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp).clip(CircleShape).background(colors.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(AppIcons.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(14.dp))
                }
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colors.primary else colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
