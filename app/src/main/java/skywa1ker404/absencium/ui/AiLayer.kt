package skywa1ker404.absencium.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.EmbeddedLastChat
import skywa1ker404.absencium.ui.components.GlassStage
import skywa1ker404.absencium.ui.theme.LocalGlassEnabled

/**
 * Absencium AI as a layer of the app's own window. It grows in over the page with a spring and leaves the same way, so going to the
 * chat and back is one continuous motion inside one app, with the same wallpaper, theme and system bars.
 */
@Composable
fun AiLayer(dark: Boolean) {
    val request by AbsenciumAi.request.collectAsStateWithLifecycle()
    val last = remember { arrayOfNulls<AiRequest>(1) }
    if (request != null) last[0] = request
    DisposableEffect(Unit) {
        AbsenciumAi.hostActive = true
        onDispose { AbsenciumAi.hostActive = false }
    }
    BackHandler(enabled = request != null) { AbsenciumAi.close() }
    AnimatedVisibility(
        visible = request != null,
        enter = fadeIn(tween(180)) + scaleIn(spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow), initialScale = 0.93f),
        exit = fadeOut(tween(160)) + scaleOut(tween(200), targetScale = 0.96f),
    ) {
        val shown = request ?: last[0]
        if (shown != null) {
            key(shown.id) {
                // Takes every touch, so nothing of the page underneath can be tapped through the chat.
                Box(Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}) {
                    GlassStage(Modifier.fillMaxSize(), glassSurfaces = false) {
                        EmbeddedLastChat(dark = dark, glass = LocalGlassEnabled.current, settingsPage = shown.destination)
                    }
                }
            }
        }
    }
}
