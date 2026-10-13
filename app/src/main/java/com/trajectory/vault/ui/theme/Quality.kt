package com.trajectory.vault.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.trajectory.vault.data.settings.VisualQuality

/**
 * What each [VisualQuality] level changes. Nothing here removes a feature or a control; it only
 * decides how much is drawn and how long animations run.
 */
@Immutable
class QualityProfile private constructor(
    val level: VisualQuality,
    /** Frosted glass blur on the floating bars. Off means they are solid surfaces. */
    val blurEnabled: Boolean,
    /** Multiplier on the user's blur intensity. */
    val blurScale: Float,
    /** Bends the content behind the glass like a lens (Android 13+). */
    val glassLens: Boolean,
    /** A lit specular edge around the glass. */
    val glassHighlight: Boolean,
    /** Colour fringing, depth and a soft drop shadow on the glass. The most expensive look. */
    val glassPremium: Boolean,
    /** Animations that exist only for delight, such as the slide when switching tabs. */
    val decorativeMotion: Boolean,
    private val minDamping: Float,
    private val stiffnessScale: Float,
) {
    /**
     * A spring tuned for this level. Low and Mid damp the overshoot and stiffen the spring, so an
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
        private val Low = QualityProfile(
            VisualQuality.LOW, blurEnabled = false, blurScale = 0f, glassLens = false, glassHighlight = false, glassPremium = false,
            decorativeMotion = false, minDamping = 1f, stiffnessScale = 1.6f,
        )
        private val Mid = QualityProfile(
            VisualQuality.MID, blurEnabled = true, blurScale = 0.6f, glassLens = false, glassHighlight = false, glassPremium = false,
            decorativeMotion = false, minDamping = 0.75f, stiffnessScale = 1.2f,
        )
        private val High = QualityProfile(
            VisualQuality.HIGH, blurEnabled = true, blurScale = 1f, glassLens = true, glassHighlight = true, glassPremium = false,
            decorativeMotion = true, minDamping = 0f, stiffnessScale = 1f,
        )
        private val Ultra = QualityProfile(
            VisualQuality.ULTRA, blurEnabled = true, blurScale = 1f, glassLens = true, glassHighlight = true, glassPremium = true,
            decorativeMotion = true, minDamping = 0f, stiffnessScale = 1f,
        )

        fun of(level: VisualQuality): QualityProfile = when (level) {
            VisualQuality.LOW -> Low
            VisualQuality.MID -> Mid
            VisualQuality.HIGH -> High
            VisualQuality.ULTRA -> Ultra
        }
    }
}

/** The quality level in effect. Changing it in Settings updates the whole app at once. */
val LocalQuality = staticCompositionLocalOf { QualityProfile.of(VisualQuality.HIGH) }
