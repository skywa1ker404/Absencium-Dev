package skywa1ker404.absencium.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
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
import kotlinx.coroutines.delay
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
    val squash = remember { Animatable(1f) }
    val turn = remember { Animatable(0f) }
    LaunchedEffect(q) {
        if (!q.decorativeMotion) return@LaunchedEffect
        while (true) {
            delay(2600)
            index = (index + 1) % shapes.size
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            squash.snapTo(0.82f)
            squash.animateTo(1f, q.spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
        }
    }
    LaunchedEffect(index) {
        if (q.decorativeMotion) turn.animateTo(index * 30f, q.spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
    }
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
                    rotationZ = turn.value
                }
                .clip(shapes[index].toShape())
                .background(colors.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.onSecondaryContainer,
                modifier = Modifier.size(40.dp).graphicsLayer { rotationZ = -turn.value },
            )
        }
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
        if (action != null) Box(modifier = Modifier.padding(top = 6.dp)) { action() }
    }
}
