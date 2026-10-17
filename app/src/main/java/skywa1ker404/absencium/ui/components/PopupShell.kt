package skywa1ker404.absencium.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.theme.LocalBackdrop
import skywa1ker404.absencium.ui.theme.LocalQuality

/** The open and close state of the popup being shown, so the card and its dim can leave with an animation too. */
private val LocalPopupTransition = staticCompositionLocalOf<MutableTransitionState<Boolean>?> { null }

/** True inside a frosted popup: cards there are see-through tonal fills so the glass shows through them. */
val LocalFrostedPopup = staticCompositionLocalOf { false }

private class Held<T : Any> { var value: T? = null }

/**
 * Shows a popup while [item] is not null and keeps it on screen while it animates away after [item] goes back to null,
 * using the last value it had. Every popup in the app is placed through this, so each one opens and closes the same way.
 * It must be placed in the full-screen layer of the screen (inside its root Box), above the page it floats over.
 */
@Composable
fun <T : Any> AnimatedPopup(item: T?, content: @Composable (T) -> Unit) {
    val transition = remember { MutableTransitionState(false) }
    val held = remember { Held<T>() }
    if (item != null) held.value = item
    transition.targetState = item != null
    val shown = held.value
    if (shown != null && (transition.currentState || transition.targetState)) {
        CompositionLocalProvider(LocalPopupTransition provides transition) { content(shown) }
    }
}

/**
 * The card every popup of the app is built on (Add Entry, Import, Search, Profile, calendar entries, Timetable, Drafts).
 *
 * - It lives in the screen's own window, so with Liquid Glass and "Apply frosted glass on other elements" on it is real
 *   glass: it blurs the page behind it exactly like the navigation pill does, with the same blur strength setting.
 *   Otherwise it is a plain solid card.
 * - It starts at the bottom of the screen and grows upwards, so the bottom edge stays put while its content changes.
 * - The card owns the one size animation. Everything inside only fades and changes size at once
 *   ([LocalHostAnimatesSize]); two competing size animations make the layout stutter.
 *
 * [centered] shows it in the middle of the screen (growing from the centre) instead of at the bottom.
 * [scrollable] wraps the content in a scroll; pass false when the content scrolls itself (a list, or its own column).
 * Tapping the dimmed area calls [onDismissRequest] only when [dismissOnOutsideTap] is set (forms keep it off).
 */
@Composable
fun PopupShell(
    onDismissRequest: () -> Unit,
    scrollable: Boolean = true,
    dismissOnOutsideTap: Boolean = false,
    centered: Boolean = false,
    content: @Composable () -> Unit,
) {
    val q = LocalQuality.current
    val colors = MaterialTheme.colorScheme
    val supplied = LocalPopupTransition.current
    val own = remember { MutableTransitionState(false).also { it.targetState = true } }
    val transition = supplied ?: own
    val backdrop = LocalBackdrop.current
    val frosted = LocalFrostedOverlays.current && backdrop != null
    val shape = RoundedCornerShape(34.dp)

    BackHandler(enabled = transition.targetState) { onDismissRequest() }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(visibleState = transition, enter = fadeIn(tween(220)), exit = fadeOut(tween(200))) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.scrim.copy(alpha = if (frosted) 0.28f else 0.4f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        if (dismissOnOutsideTap) onDismissRequest()
                    },
            )
        }
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = if (centered) Alignment.Center else Alignment.BottomCenter,
        ) {
            val maxCardHeight = maxHeight
            AnimatedVisibility(
                visibleState = transition,
                enter = fadeIn(tween(160)) + expandVertically(
                    animationSpec = q.spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow),
                    expandFrom = if (centered) Alignment.CenterVertically else Alignment.Bottom,
                ),
                exit = fadeOut(tween(140)) + shrinkVertically(tween(200), shrinkTowards = if (centered) Alignment.CenterVertically else Alignment.Bottom),
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .animateContentSize(q.spring(dampingRatio = 0.92f, stiffness = Spring.StiffnessMediumLow))
                        .heightIn(max = maxCardHeight)
                        .then(
                            if (frosted) {
                                Modifier.floatingGlass(backdrop, shape, colors.surfaceContainerHigh, lens = false, denseVeil = true)
                            } else {
                                Modifier.clip(shape).background(colors.surfaceContainerHigh)
                            },
                        ),
                ) {
                    CompositionLocalProvider(
                        LocalCardBackdrop provides null,
                        LocalHostAnimatesSize provides true,
                        LocalFrostedPopup provides frosted,
                    ) {
                        if (scrollable) {
                            Box(modifier = Modifier.verticalScroll(rememberScrollState())) { content() }
                        } else {
                            content()
                        }
                    }
                }
            }
        }
    }
}

/** The header of a popup: a round-cornered close button and the title, with room for a short line under it. */
@Composable
fun PopupHeader(title: String, onClose: () -> Unit, subtitle: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Icon(AppIcons.Close, contentDescription = "Close")
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** A popup's usual body: padded, with a header, and the content spaced like the cards of a screen. */
@Composable
fun PopupBody(title: String, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PopupHeader(title = title, onClose = onClose)
        content()
    }
}
