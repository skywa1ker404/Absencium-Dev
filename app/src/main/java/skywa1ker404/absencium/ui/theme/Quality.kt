package skywa1ker404.absencium.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import skywa1ker404.absencium.data.settings.VisualQuality

/**
 * What each [VisualQuality] level changes. Nothing here removes a feature or a control; it only
 * decides how much is drawn and how long animations run.
 */
@Immutable
class QualityProfile private constructor(
    val level: VisualQuality,
    /** Real glass on the floating bars. Off means they are solid surfaces. */
    val blurEnabled: Boolean,
    /** Bends the content behind the glass like a lens (Android 13+). */
    val glassLens: Boolean,
    /** A lit specular edge and a soft shadow around the glass. */
    val glassHighlight: Boolean,
    /** Animations that exist only for delight, such as the slide when switching tabs. */
    val decorativeMotion: Boolean,
    private val minDamping: Float,
    private val stiffnessScale: Float,
) {
    /**
     * A spring tuned for this mode. Performance mode damps the overshoot and stiffens the spring, so an
     * animation settles in fewer frames and the screen redraws less while it runs.
     */
    fun <T> spring(
        dampingRatio: Float = Spring.DampingRatioNoBouncy,
        stiffness: Float = Spring.StiffnessMedium,
    ): SpringSpec<T> = SpringSpec(
        dampingRatio = maxOf(dampingRatio, minDamping),
        stiffness = (stiffness * stiffnessScale).coerceAtMost(Spring.StiffnessHigh),
    )

    companion object {
        private val Performance = QualityProfile(
            VisualQuality.PERFORMANCE, blurEnabled = false, glassLens = false, glassHighlight = false,
            decorativeMotion = false, minDamping = 1f, stiffnessScale = 1.6f,
        )
        private val Low = QualityProfile(
            VisualQuality.LOW, blurEnabled = true, glassLens = false, glassHighlight = true,
            decorativeMotion = true, minDamping = 0.8f, stiffnessScale = 1.25f,
        )
        private val Quality = QualityProfile(
            VisualQuality.QUALITY, blurEnabled = true, glassLens = true, glassHighlight = true,
            decorativeMotion = true, minDamping = 0f, stiffnessScale = 1f,
        )

        fun of(level: VisualQuality): QualityProfile = when (level) {
            VisualQuality.PERFORMANCE -> Performance
            VisualQuality.LOW -> Low
            VisualQuality.QUALITY -> Quality
        }
    }
}

/** The quality level in effect. Changing it in Settings updates the whole app at once. */
val LocalQuality = staticCompositionLocalOf { QualityProfile.of(VisualQuality.QUALITY) }
