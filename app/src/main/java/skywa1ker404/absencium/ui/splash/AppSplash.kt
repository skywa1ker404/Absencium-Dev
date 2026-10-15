package skywa1ker404.absencium.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import skywa1ker404.absencium.ui.theme.LocalQuality
import kotlinx.coroutines.delay

/**
 * The in-app splash, shown after the system splash while the app is ready underneath. It follows the chosen theme like
 * the rest of the app (light, dark, OLED, Monet, presets, every quality level). With Liquid Glass on, the background is
 * the same wallpaper as the home screen and the icon becomes a pane of glass.
 *
 * About a second and a half in total: the icon grows out of a circle into its rounded square with the Expressive spring
 * while the A turns into place, holds for a moment, then the whole splash fades into the app.
 */
@Composable
fun AppSplash(onFinished: () -> Unit) {
    val q = LocalQuality.current
    val grow = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        grow.animateTo(1f, q.spring(dampingRatio = 0.55f, stiffness = 90f))
        delay(380)
        fade.animateTo(0f, tween(durationMillis = 380))
        onFinished()
    }

    Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = fade.value }) {
        // Same background as the home screen; the logo follows the theme (and becomes glass with Liquid Glass).
        ThemedStage(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AbsenciumMark(size = 132.dp, progress = grow.value)
            }
        }
    }
}
