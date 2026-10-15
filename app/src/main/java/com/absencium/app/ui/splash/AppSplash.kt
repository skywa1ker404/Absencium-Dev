package com.absencium.app.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.absencium.app.R
import com.absencium.app.ui.components.GlassStage
import com.absencium.app.ui.components.LocalCardBackdrop
import com.absencium.app.ui.components.glassSurface
import com.absencium.app.ui.theme.LocalGlassEnabled
import com.absencium.app.ui.theme.LocalQuality
import kotlinx.coroutines.delay
import kotlin.math.max

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
    val colors = MaterialTheme.colorScheme
    val glass = LocalGlassEnabled.current
    val grow = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        grow.animateTo(1f, q.spring(dampingRatio = 0.55f, stiffness = 90f))
        delay(380)
        fade.animateTo(0f, tween(durationMillis = 380))
        onFinished()
    }

    Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = fade.value }) {
        val stage: @Composable () -> Unit = {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val progress = grow.value
                val radius = (66f - 38f * progress).coerceAtLeast(10f).dp
                val shape = RoundedCornerShape(radius)
                val backdrop = LocalCardBackdrop.current
                Box(
                    modifier = Modifier
                        .size(132.dp)
                        .graphicsLayer {
                            val s = 0.55f + 0.45f * progress
                            scaleX = s
                            scaleY = s
                            alpha = (progress * 2f).coerceIn(0f, 1f)
                        }
                        .clip(shape)
                        .then(
                            if (glass && backdrop != null) {
                                Modifier.glassSurface(backdrop, shape, colors.primaryContainer, 0.5f)
                            } else {
                                Modifier.background(colors.primaryContainer)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_logo_a),
                        contentDescription = "Absencium",
                        colorFilter = ColorFilter.tint(colors.onPrimaryContainer),
                        modifier = Modifier
                            .size(64.dp)
                            .graphicsLayer { rotationZ = (1f - progress) * -22f },
                    )
                }
            }
        }
        if (glass) {
            // Same background as the home screen.
            GlassStage(modifier = Modifier.fillMaxSize()) { stage() }
        } else {
            Box(modifier = Modifier.fillMaxSize().background(colors.background)) { stage() }
        }
    }
}
