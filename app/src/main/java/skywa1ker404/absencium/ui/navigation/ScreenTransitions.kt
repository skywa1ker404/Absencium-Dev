package skywa1ker404.absencium.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle

/**
 * How every screen opens and closes, identical to Absencium AI's own screens (same distances, durations and curve) so moving between
 * the two never changes the motion. A new screen slides in from the right while the old one drifts left and fades; Back is the exact
 * reverse.
 */
object ScreenTransitions : NavHostAnimatedDestinationStyle() {
    private const val SLIDE_MS = 200
    private const val FADE_IN_MS = 150
    private const val FADE_OUT_MS = 100

    override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(tween(SLIDE_MS, easing = FastOutSlowInEasing)) { it / 2 } + fadeIn(tween(FADE_IN_MS))
    }

    override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(tween(SLIDE_MS, easing = FastOutSlowInEasing)) { -it / 4 } + fadeOut(tween(FADE_OUT_MS))
    }

    override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(tween(SLIDE_MS, easing = FastOutSlowInEasing)) { -it / 4 } + fadeIn(tween(FADE_IN_MS))
    }

    override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(tween(SLIDE_MS, easing = FastOutSlowInEasing)) { it / 2 } + fadeOut(tween(FADE_OUT_MS))
    }
}
