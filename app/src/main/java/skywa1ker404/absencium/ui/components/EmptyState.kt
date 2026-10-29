package skywa1ker404.absencium.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import skywa1ker404.absencium.ui.theme.LocalQuality

/**
 * An empty page or list in the Material 3 Expressive style: a badge that changes from one Material shape to the next
 * with a springy squash and a light tick, a title and a short helper line. [action] may hold a button.
 * Performance mode keeps a still badge.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveEmptyState(
    icon: ImageVector,
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val q = LocalQuality.current
    val haptic = LocalHapticFeedback.current
    val shapes = remember {
        listOf(
            MaterialShapes.Cookie12Sided,
            MaterialShapes.Clover4Leaf,
            MaterialShapes.Circle,
            MaterialShapes.Cookie9Sided,
            MaterialShapes.SoftBurst,
        )
    }
    var index by remember { mutableIntStateOf(0) }
    var from by remember { mutableIntStateOf(0) }
    val morphProgress = remember { Animatable(1f) }
    val squash = remember { Animatable(1f) }
    // The badge keeps turning slowly anti-clockwise, and every 5 seconds it changes into the next shape with a springy squash.
    val spin = rememberInfiniteTransition(label = "emptySpin").animateFloat(
        initialValue = 0f,
        targetValue = -360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 24_000, easing = LinearEasing)),
        label = "emptySpinAngle",
    )
    LaunchedEffect(q) {
        if (!q.decorativeMotion) return@LaunchedEffect
        while (true) {
            delay(5000)
            from = index
            index = (index + 1) % shapes.size
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            morphProgress.snapTo(0f)
            launch { morphProgress.animateTo(1f, q.spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)) }
            squash.snapTo(0.78f)
            squash.animateTo(1f, q.spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        }
    }
    val morph = remember(from, index) { Morph(shapes[from], shapes[index]) }
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .graphicsLayer {
                    scaleX = squash.value
                    scaleY = 2f - squash.value
                    rotationZ = if (q.decorativeMotion) spin.value else 0f
                }
                .clip(MorphShape(morph, morphProgress.value))
                .background(colors.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.onSecondaryContainer,
                modifier = Modifier.size(40.dp).graphicsLayer { rotationZ = if (q.decorativeMotion) -spin.value else 0f },
            )
        }
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
        if (action != null) Box(modifier = Modifier.padding(top = 6.dp)) { action() }
    }
}


/** The outline of a [Morph] part of the way between its two shapes (0 is the first, 1 the second), scaled to fill the box. */
private class MorphShape(private val morph: Morph, private val progress: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = androidx.compose.ui.graphics.Path()
        val cubics = morph.asCubics(progress.coerceIn(0f, 1f))
        if (cubics.isNotEmpty()) {
            path.moveTo(cubics.first().anchor0X * size.width, cubics.first().anchor0Y * size.height)
            for (c in cubics) {
                path.cubicTo(
                    c.control0X * size.width, c.control0Y * size.height,
                    c.control1X * size.width, c.control1Y * size.height,
                    c.anchor1X * size.width, c.anchor1Y * size.height,
                )
            }
            path.close()
        }
        return Outline.Generic(path)
    }
}
