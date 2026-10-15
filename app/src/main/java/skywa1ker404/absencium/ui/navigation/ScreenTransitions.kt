package skywa1ker404.absencium.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.navigation.NavBackStackEntry
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle

/**
 * How every screen opens and closes (Settings, its pages, the note editor and the rest). The screen that leaves fades
 * out first, then the new one fades in while rising a few pixels into place, the same fade-through the Home, Calendar
 * and Vault tabs use, so the two screens are never seen on top of each other. Going back plays the same motion.
 */
object ScreenTransitions : NavHostAnimatedDestinationStyle() {
    override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(tween(durationMillis = 220, delayMillis = 90, easing = LinearOutSlowInEasing)) +
            slideInVertically(tween(durationMillis = 300, delayMillis = 60, easing = FastOutSlowInEasing)) { full -> full / 28 }
    }

    override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(tween(durationMillis = 100))
    }
}
