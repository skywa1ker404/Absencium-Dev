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
        val text = ChatAppTools.APP_PROMPT + "\n\n" + TASK_RULES + AiHost.TASK_MARK + instruction.trim()
        return send(title, text, images)
    }

    /**
     * Runs a request whose answer the app reads itself (an outline, a list of holidays, a rewritten paragraph): still a hidden chat in
     * the AI actions folder, but the assistant uses no tools and answers with exactly what the request asks for. The answer is the reply.
     */
    suspend fun ask(title: String, request: String, images: List<String> = emptyList()): AiHost.TaskResult =
        send(title, TEXT_RULES + AiHost.TASK_MARK + request.trim(), images)

    /**
     * [ask] with in-memory pictures or PDFs (they are written to the cache first, since the hub reads files), returning the answer
     * itself. Throws an [AiException] when the assistant did not answer.
     */
    suspend fun answer(title: String, request: String, files: List<AiImage> = emptyList()): String {
        val dir = java.io.File(application.cacheDir, "ai-task").apply { mkdirs() }
        val paths = files.filter { !it.isAudio }.mapIndexed { i, f ->
            val ext = if (f.isPdf) "pdf" else "jpg"
            java.io.File(dir, "${System.nanoTime()}-$i.$ext").also { it.writeBytes(android.util.Base64.decode(f.base64, android.util.Base64.DEFAULT)) }.absolutePath
        }
        val result = ask(title, request, paths)
        result.error?.let { throw AiException(it) }
        return result.reply
    }

    private suspend fun send(title: String, text: String, images: List<String>): AiHost.TaskResult {
        val parts = buildList<UIMessagePart> {
            add(UIMessagePart.Text(text))
            images.forEach { path ->
                if (path.endsWith(".pdf")) add(UIMessagePart.Document("file://$path", java.io.File(path).name, "application/pdf"))
                else add(UIMessagePart.Image("file://$path"))
            }
        }
        return AiApp.runHostTask(application, title, parts)
    }

    private companion object {
        const val TEXT_RULES =
            "This request comes from a button in the app, not from a person typing. Do not use any tools and do not ask questions. " +
                "Answer with exactly what the request asks for, in the format it asks for, and nothing else."

        const val TASK_RULES =
            "This request comes from a button in the app, not from a person typing. Nobody can answer a question or approve a change, so do not ask: " +
                "decide, use the tools, and finish. Deleting is not available here. Keep the final answer to one or two plain sentences saying what was made or changed."
    }
}
