package me.rerere.ai.context

import me.rerere.ai.provider.Model
import me.rerere.ai.ui.TurnGroup
import me.rerere.ai.ui.toTurnGroups
import me.rerere.ai.ui.UIMessage

/**
 * System dynamics estimator for conversation velocity, runway, and summarization cooldowns.
 *
 * Implements an asymmetric Exponentially Weighted Moving Average (EWMA) to rapidly respond
 * to token volume spikes (e.g. pasting large code blocks or documents) while slowly decaying
 * on short conversational turns.
 */
object ContextTurnEstimator {
    /** Fast attack coefficient when incoming turn exceeds current velocity. */
    const val ATTACK_ALPHA = 0.50

    /** Slow decay coefficient when incoming turn is smaller than current velocity. */
    const val DECAY_ALPHA = 0.12

    /** Minimum safe token floor per turn. */
    const val FLOOR_TOKENS = 150

    /**
     * Computes the peak-aware asymmetric EWMA turn velocity from a list of turn token counts.
     */
    fun estimateTurnVelocity(turnTokenCounts: List<Int>): Double {
        if (turnTokenCounts.isEmpty()) return FLOOR_TOKENS.toDouble()
        var current = turnTokenCounts.first().toDouble().coerceAtLeast(FLOOR_TOKENS.toDouble())
        for (i in 1 until turnTokenCounts.size) {
            val count = turnTokenCounts[i].toDouble()
            val alpha = if (count > current) ATTACK_ALPHA else DECAY_ALPHA
            current = alpha * count + (1.0 - alpha) * current
        }
        return maxOf(FLOOR_TOKENS.toDouble(), current)
    }



    /**
     * Returns the minimum unsummarized turns ($N_{\text{min}}$) required before auto-summarizing.
     */
    fun minimumCooldownTurns(archetype: ContextScaleArchetype): Int = when (archetype) {
        ContextScaleArchetype.MICRO -> 4
        ContextScaleArchetype.COMPACT -> 6
        ContextScaleArchetype.VAST -> 10
    }

    /**
     * Returns the minimum unsummarized tokens ($K_{\text{min}}$) required before auto-summarizing.
     */
    fun minimumCooldownTokens(archetype: ContextScaleArchetype): Int = when (archetype) {
        ContextScaleArchetype.MICRO -> 512
        ContextScaleArchetype.COMPACT -> 1_500
        ContextScaleArchetype.VAST -> 3_000
    }
}
