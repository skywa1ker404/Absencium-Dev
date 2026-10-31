package skywa1ker404.absencium.ui.vault

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.theme.LocalQuality

/**
 * Pull-down-at-the-top to open Drafts, for any list (the Vault grid has its own copy of the same rule). Put [connection] on the
 * list's parent with nestedScroll, move the list down by [offset], and draw [DraftPullBadge] on top. Releasing past the threshold
 * calls [onOpen]; releasing earlier springs back. Only drag the list could not use is counted, so ordinary scrolling is untouched.
 */
class DraftPull internal constructor(
    val connection: NestedScrollConnection,
    private val pullOf: () -> Float,
    val thresholdPx: Float,
) {
    val pull: Float get() = pullOf()
    val progress: Float get() = (pull / thresholdPx).coerceIn(0f, 1f)
    val reached: Boolean get() = pull >= thresholdPx
}

@Composable
fun rememberDraftPull(atTop: () -> Boolean, onOpen: () -> Unit): DraftPull {
    val q = LocalQuality.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val thresholdPx = with(density) { 96.dp.toPx() }
    val maxPx = with(density) { 160.dp.toPx() }
    var pull by remember { mutableFloatStateOf(0f) }
    val currentOpen by rememberUpdatedState(onOpen)
    val currentAtTop by rememberUpdatedState(atTop)
    return remember(thresholdPx, maxPx) {
        DraftPull(
            connection = object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (source == NestedScrollSource.UserInput && pull > 0f && available.y < 0f) {
                        val used = maxOf(available.y, -pull)
                        pull += used
                        return Offset(0f, used)
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    if (source == NestedScrollSource.UserInput && available.y > 0f && currentAtTop()) {
                        pull = (pull + available.y * 0.5f).coerceAtMost(maxPx)
                        return Offset(0f, available.y)
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (pull <= 0f) return Velocity.Zero
                    val open = pull >= thresholdPx
                    scope.launch {
                        animate(pull, 0f, animationSpec = q.spring(dampingRatio = Spring.DampingRatioMediumBouncy)) { value, _ -> pull = value }
                    }
                    if (open) currentOpen()
                    return Velocity(0f, available.y)
                }
            },
            pullOf = { pull },
            thresholdPx = thresholdPx,
        )
    }
}

/** The draft icon that slides down from the top edge while pulling, and fills in when the pull is far enough. */
@Composable
fun BoxScope.DraftPullBadge(draftPull: DraftPull, topPadding: Dp = 0.dp) {
    if (draftPull.pull <= 0f) return
    val colors = MaterialTheme.colorScheme
    val reached = draftPull.reached
    val progress = draftPull.progress
    val pull = draftPull.pull
    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = topPadding)
            .graphicsLayer {
                translationY = pull * 0.6f - 56.dp.toPx()
                alpha = progress
                scaleX = 0.6f + 0.4f * progress + if (reached) 0.12f else 0f
                scaleY = scaleX
            }
            .size(48.dp)
            .clip(CircleShape)
            .background(if (reached) colors.primary else colors.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Icon(AppIcons.Draft, contentDescription = "Release to open Drafts", tint = if (reached) colors.onPrimary else colors.onSurface)
    }
}
