package skywa1ker404.absencium.data.ai

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** How much Absencium AI spends on an answer. Balanced is the default. */
enum class AiMode(val label: String, val blurb: String) {
    EFFICIENT("Efficient", "Fewest tokens: short history, few memories, a cap on short answers."),
    BALANCED("Balanced", "Sensible limits that keep answers complete."),
    MAX("Maximum quality", "Long history, more memories, no caps."),
}

enum class Complexity { SIMPLE, NORMAL, COMPLEX }

/** What one request may spend: how much history, how many memories, and a cap on the answer (null = the provider's own limit). */
data class Budget(val fullTurns: Int, val maxTurns: Int, val oldTurnChars: Int, val memories: Int, val maxOutput: Int?)

/** The result of looking at a request locally, before any model is called. */
data class TaskPlan(val topics: PromptTopics, val complexity: Complexity, val budget: Budget)

/**
 * Decides, on the phone and without a model call, what a request needs: which parts of the app, which formats, how hard it is, and
 * what it may spend. The chat service then builds only that instruction and history.
 */
object AiOrchestrator {
    private val HARD = listOf(
        "prove", "derive", "solve", "step by step", "step-by-step", "analy", "compare", "debug", "algorithm", "code", "calculate",
        "why does", "in depth", "in-depth", "essay", "evaluate", "optimi",
    )

    fun plan(recent: String, last: String, mode: AiMode, subjects: Collection<String> = emptyList()): TaskPlan {
        val topics = PromptTopics.of(recent, subjects)
        val complexity = complexity(last, topics)
        return TaskPlan(topics, complexity, budget(mode, complexity, topics))
    }

    fun complexity(last: String, topics: PromptTopics): Complexity {
        val t = last.lowercase()
        return when {
            last.length > 600 || HARD.any { it in t } || topics.notes || topics.quiz -> Complexity.COMPLEX
            last.length <= 60 -> Complexity.SIMPLE
            else -> Complexity.NORMAL
        }
    }

    fun budget(mode: AiMode, complexity: Complexity, topics: PromptTopics): Budget {
        // An answer that must be complete (notes, a quiz, hard work) is never capped.
        val open = complexity == Complexity.COMPLEX || topics.notes || topics.quiz
        return when (mode) {
            AiMode.EFFICIENT -> Budget(4, 12, 400, 3, if (open) null else if (complexity == Complexity.SIMPLE) 800 else 2000)
            AiMode.BALANCED -> Budget(8, 30, 700, 8, null)
            AiMode.MAX -> Budget(12, 40, 1200, 15, null)
        }
    }
}

/** The chosen [AiMode], kept on this phone. */
class AiModeStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("absencium_ai_mode", Context.MODE_PRIVATE)
    private val _mode = MutableStateFlow(read())
    val mode: StateFlow<AiMode> = _mode.asStateFlow()

    fun set(mode: AiMode) {
        prefs.edit { putString("mode", mode.name) }
        _mode.value = mode
    }

    private fun read(): AiMode = runCatching { AiMode.valueOf(prefs.getString("mode", null) ?: "") }.getOrDefault(AiMode.BALANCED)
}
