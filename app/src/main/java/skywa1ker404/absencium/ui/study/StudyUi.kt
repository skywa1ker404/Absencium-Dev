package skywa1ker404.absencium.ui.study

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.Hue
import skywa1ker404.absencium.ui.components.bounceIn
import skywa1ker404.absencium.ui.components.drawHatch
import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.theme.LocalQuality

/** The Material 3 Expressive shapes the study pages use for their badges. */
enum class BadgeShape { COOKIE, SUNNY, CLOVER, FLOWER, BURST, COOKIE9 }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun badgeShape(shape: BadgeShape): Shape = when (shape) {
    BadgeShape.COOKIE -> MaterialShapes.Cookie12Sided.toShape()
    BadgeShape.SUNNY -> MaterialShapes.Sunny.toShape()
    BadgeShape.CLOVER -> MaterialShapes.Clover4Leaf.toShape()
    BadgeShape.FLOWER -> MaterialShapes.Flower.toShape()
    BadgeShape.BURST -> MaterialShapes.Burst.toShape()
    BadgeShape.COOKIE9 -> MaterialShapes.Cookie9Sided.toShape()
}

/** An icon on a scalloped Material 3 Expressive shape, tinted from the card's [hue]. */
@Composable
fun ShapeBadge(icon: ImageVector, shape: BadgeShape, hue: Hue, modifier: Modifier = Modifier, size: Dp = 48.dp, solid: Boolean = true) {
    Box(
        modifier = modifier
            .size(size)
            .clip(badgeShape(shape))
            .background(if (solid) hue.accent else hue.accent.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon, contentDescription = null,
            tint = if (solid) hue.container else hue.accent,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

/** A big card at the top of a page: a small label, one large figure, a line under it, and a badge on the right. */
@Composable
fun StudyHero(label: String, figure: String, caption: String, icon: ImageVector, hue: Hue, shape: BadgeShape, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier.fillMaxWidth(), container = hue.container) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = hue.onContainer.copy(alpha = 0.75f))
                Text(figure, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = hue.onContainer)
                Text(caption, style = MaterialTheme.typography.bodyMedium, color = hue.onContainer.copy(alpha = 0.8f))
            }
            ShapeBadge(icon, shape, hue, size = 76.dp)
        }
    }
}

/** A pastel quick-start tile: a coloured shape, a bold title and one line of help. */
@Composable
fun PastelTile(title: String, subtitle: String, hue: Hue, shape: BadgeShape, icon: ImageVector, modifier: Modifier = Modifier, delayMs: Int = 0, onClick: () -> Unit) {
    AppCard(modifier = modifier.bounceIn(delayMs), onClick = onClick, container = hue.container, shape = MaterialTheme.shapes.extraLarge) {
        Column(modifier = Modifier.fillMaxWidth().height(116.dp), verticalArrangement = Arrangement.SpaceBetween) {
            ShapeBadge(icon, shape, hue, size = 40.dp)
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = hue.onContainer)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = hue.onContainer.copy(alpha = 0.78f))
            }
        }
    }
}

/** A white list card: a shape badge, a title, what it does, and a chevron. */
@Composable
fun ToolCard(title: String, description: String, icon: ImageVector, hue: Hue, shape: BadgeShape, modifier: Modifier = Modifier, delayMs: Int = 0, meta: String? = null, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    AppCard(modifier = modifier.fillMaxWidth().bounceIn(delayMs), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ShapeBadge(icon, shape, hue, solid = false, size = 52.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                if (meta != null) Text(meta, style = MaterialTheme.typography.labelMedium, color = hue.accent, modifier = Modifier.padding(top = 2.dp))
            }
            Icon(AppIcons.ChevronRight, contentDescription = null, tint = colors.onSurfaceVariant)
        }
    }
}

/** A capsule progress bar: hatched where it is empty, filled where it is done, and it springs to its value. */
@Composable
fun CapsuleProgress(fraction: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 12.dp) {
    val q = LocalQuality.current
    val value by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = q.spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "capsuleProgress",
    )
    val hatch = MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = modifier.fillMaxWidth().height(height).clip(CircleShape)
            .drawBehind { drawHatch(hatch) },
    ) {
        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(value).clip(CircleShape).background(color))
    }
}
