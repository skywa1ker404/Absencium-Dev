package skywa1ker404.absencium.data.ai

import android.app.Application
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.AiApp
import me.rerere.rikkahub.AiHost

/**
 * Every AI action of the app goes here: it is handed to the AI hub as a hidden, read-only chat (listed in the AI actions folder),
 * and the hub's assistant decides what to do with the app's tools. Nothing in the app talks to a provider on its own.
 */
class AiTasks(private val application: Application) {

    /**
     * Runs [instruction] as one action. [images] are picture file paths sent along (a photographed page, a timetable). The assistant
     * is told the structure of the app and that nobody can answer questions or approve, so it decides and uses the tools itself.
     */
    suspend fun run(title: String, instruction: String, images: List<String> = emptyList()): AiHost.TaskResult {
        val text = ChatAppTools.APP_PROMPT + "\n\n" + TASK_RULES + "\n\n" + instruction.trim()
        val parts = buildList<UIMessagePart> {
            add(UIMessagePart.Text(text))
            images.forEach { add(UIMessagePart.Image("file://$it")) }
        }
        return AiApp.runHostTask(application, title, parts)
    }

    private companion object {
        const val TASK_RULES =
            "This request comes from a button in the app, not from a person typing. Nobody can answer a question or approve a change, so do not ask: " +
                "decide, use the tools, and finish. Deleting is not available here. Keep the final answer to one or two plain sentences saying what was made or changed."
    }
}
