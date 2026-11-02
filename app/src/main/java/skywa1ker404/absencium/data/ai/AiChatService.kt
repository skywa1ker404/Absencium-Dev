package skywa1ker404.absencium.data.ai

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import skywa1ker404.absencium.logging.DebugLogger

/** An answer that finished. [seen] is true when the student was looking at that chat; otherwise a notification can announce it. */
data class ChatCompletion(val chatId: Long, val title: String, val preview: String, val ok: Boolean, val seen: Boolean)

/**
 * Answers chats on the app's own scope, not a screen's, so an answer keeps being written while the student opens another chat, leaves
 * Absencium AI or closes the app. Each chat has at most one answer in flight. [onBusy] tells the system the app is working (the
 * foreground service that keeps the process alive); [completions] tells the notification layer an answer is ready.
 */
class AiChatService(
    private val scope: CoroutineScope,
    private val chats: AiChats,
    private val client: AiClient,
    private val attachments: AiAttachments,
    private val actionRunner: () -> AiActionRunner,
    private val connectionFor: (AiChat) -> AiConnection?,
    private val contextFor: suspend (AiChat) -> ChatContext,
    private val memory: AiMemory,
    private val memoryAllowed: () -> Boolean,
    /** Turns an approved read request into the attachment to send (the note's text or the material file), or null when it cannot be found. */
    private val contentReader: suspend (AiAction.ReadContent) -> ChatAttachment? = { null },
    private val onBusy: (Boolean) -> Unit = {},
) {
    /** One answer in flight; the object identifies it, so a replaced answer never clears its successor's state. */
    private class Run {
        var job: Job? = null
    }

    private val jobs = HashMap<Long, Run>()
    private val lock = Any()

    private val _pending = MutableStateFlow<Set<Long>>(emptySet())

    /** Chats that are being answered right now. */
    val pending: StateFlow<Set<Long>> = _pending.asStateFlow()

    private val _unread = MutableStateFlow<Set<Long>>(emptySet())

    /** Chats with an answer the student has not looked at yet. */
    val unread: StateFlow<Set<Long>> = _unread.asStateFlow()

    private val viewing = MutableStateFlow<Long?>(null)

    private val _completions = MutableSharedFlow<ChatCompletion>(extraBufferCapacity = 16)
    val completions: SharedFlow<ChatCompletion> = _completions.asSharedFlow()

    private val _results = MutableStateFlow<Map<String, ActionResult>>(emptyMap())

    /** What happened to each change card ("chat:message:index"), so a card can say "Applied" or why it was refused. */
    val results: StateFlow<Map<String, ActionResult>> = _results.asStateFlow()

    private val _working = MutableStateFlow<Set<String>>(emptySet())

    /** The change cards being applied right now (a note is written by the AI first, which takes a while). */
    val working: StateFlow<Set<String>> = _working.asStateFlow()

    /** Tells the service which chat is on screen, or null when none is (screen closed or app in the background). */
    fun setViewing(chatId: Long?) {
        viewing.value = chatId
        if (chatId != null) _unread.update { it - chatId }
    }

    fun key(chatId: Long, messageId: Long, index: Int) = "$chatId:$messageId:$index"

    /** What the last few student messages touch, so the instruction only carries the parts that matter (a plain "hi" stays small). */
    private fun topicsOf(chat: AiChat): PromptTopics =
        PromptTopics.of(chat.messages.filter { it.role == ChatRole.USER }.takeLast(3).joinToString("\n") { it.text })

    /** Starts answering the last question of [chatId] (already saved in the store). Replaces an answer still being written for that chat. */
    fun answer(chatId: Long) {
        val chat = chats.get(chatId) ?: return
        val connection = connectionFor(chat) ?: return
        val run = Run()
        synchronized(lock) {
            jobs.put(chatId, run)?.job?.cancel()
            _pending.update { it + chatId }
        }
        onBusy(true)
        val launched = scope.launch(start = CoroutineStart.LAZY) {
            try {
                val context = contextFor(chat)
                val searching = chat.webSearch && canSearch(connection)
                val turns = ChatTurns.normalize(turnsOf(chat))
                if (turns.isEmpty()) return@launch
                val reply = client.chat(connection, AiChatPrompt.system(context.copy(study = chat.study, level = chat.level, webSearch = searching, topics = topicsOf(chat))), turns, ChatOptions(searching, chat.thinking, context.temperature))
                val parsed = AiActionParser.split(reply.text)
                val (withoutQuiz, quiz) = AiQuiz.extract(parsed.text)
                val shown = withoutQuiz.ifBlank { if (quiz != null) "Here is your quiz." else if (parsed.actions.isNotEmpty()) "Here is what I suggest." else reply.text.trim() }
                if (memoryAllowed() && !chat.incognito) parsed.remembered.forEach { memory.add(it.text, if (it.chatOnly) chatId else null) }
                if (shown.isBlank() && parsed.actions.isEmpty()) throw AiException("The model sent back an empty answer.")
                if (reply.webOff) chats.get(chatId)?.let { chats.put(it.copy(webSearch = false)) }
                append(chatId, ChatMessage(0, ChatRole.ASSISTANT, shown, 0, reply.model.ifBlank { connection.model }, via = reply.replacedProvider, actions = parsed.json?.takeIf { parsed.actions.isNotEmpty() }, sources = reply.sources, quiz = quiz))
                finished(chatId, shown, ok = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The reason is announced by the bar at the bottom of the screen; the chat only marks that this one failed.
                DebugLogger.e("AiChatService", "A chat answer failed", e)
                append(chatId, ChatMessage(0, ChatRole.ASSISTANT, "", 0, connection.model, failed = true))
                finished(chatId, "The answer failed.", ok = false)
            } finally {
                val idle = synchronized(lock) {
                    if (jobs[chatId] === run) {
                        jobs.remove(chatId)
                        _pending.update { it - chatId }
                    }
                    jobs.isEmpty()
                }
                if (idle) onBusy(false)
            }
        }
        run.job = launched
        launched.start()
    }

    /** Stops the answer being written for [chatId]; the question stays, with "No answer yet · Retry". */
    fun stop(chatId: Long) {
        val idle = synchronized(lock) {
            jobs.remove(chatId)?.job?.cancel()
            _pending.update { it - chatId }
            jobs.isEmpty()
        }
        if (idle) onBusy(false)
    }

    /**
     * The conversation as turns, kept small so a long chat does not cost more with every question: the newest [FULL_TURNS] messages go in full,
     * older ones are shortened, anything beyond [MAX_TURNS] is left out, and files are sent again only for the two newest messages that have them.
     */
    private suspend fun turnsOf(chat: AiChat): List<ChatTurn> {
        var budget = MAX_SENT_BYTES
        var filesLeft = FILE_MESSAGES
        val turns = ArrayList<ChatTurn>()
        var seen = 0
        // Newest first, so the files of the latest questions are the ones kept when the total is too big to send again.
        for (message in chat.messages.asReversed()) {
            if (message.failed) continue
            if (seen >= MAX_TURNS) break
            val old = seen >= FULL_TURNS
            seen++
            var text = message.text
            if (old && text.length > OLD_TURN_CHARS) text = text.take(OLD_TURN_CHARS).trimEnd() + " …"
            val images = ArrayList<AiImage>()
            val sendFiles = message.attachments.isNotEmpty() && filesLeft > 0
            if (sendFiles) filesLeft--
            for (file in message.attachments) {
                if (!sendFiles) {
                    text += "\n[Attached \"${file.name}\" earlier in this chat.]"
                    continue
                }
                val part = attachments.part(file)
                if (part == null) {
                    text += "\n[Attached \"${file.name}\", no longer stored.]"
                } else if (part.text != null) {
                    text = part.text + "\n\n" + text
                } else if (part.image != null) {
                    if (part.image.base64.length <= budget) {
                        budget -= part.image.base64.length
                        images += part.image
                    } else {
                        text += "\n[Attached \"${file.name}\", too much to send again.]"
                    }
                }
            }
            turns += ChatTurn(message.role, text, images)
        }
        return turns.asReversed()
    }

    private fun append(chatId: Long, message: ChatMessage) {
        val chat = chats.get(chatId) ?: return
        val now = System.currentTimeMillis()
        // Ids are times in milliseconds; two messages can land in the same one, and list keys must differ.
        val id = maxOf(now, (chat.messages.maxOfOrNull { it.id } ?: 0L) + 1)
        chats.put(chat.copy(messages = chat.messages + message.copy(id = id, time = now), updated = now))
    }

    private fun finished(chatId: Long, preview: String, ok: Boolean) {
        val seen = viewing.value == chatId
        if (!seen) _unread.update { it + chatId }
        val title = chats.get(chatId)?.label ?: "Absencium AI"
        _completions.tryEmit(ChatCompletion(chatId, title, preview.take(160), ok, seen))
    }

    /** Applies the proposed changes [items] (index with action) in order; a new subject must exist before a note is added to it. */
    fun apply(chatId: Long, messageId: Long, items: List<Pair<Int, AiAction>>) {
        val keys = items.map { key(chatId, messageId, it.first) }
        _working.update { it + keys }
        scope.launch {
            // What the student approved the AI to read; it goes into the chat as their next message and the AI answers again.
            val approved = ArrayList<ChatAttachment>()
            try {
                for ((index, action) in items) {
                    val key = key(chatId, messageId, index)
                    val result = if (action is AiAction.ReadContent) {
                        val file = contentReader(action)
                        if (file != null) {
                            approved += file
                            ActionResult(true, "Read. The AI is using it.")
                        } else {
                            ActionResult(false, "That ${if (action.material) "study material" else "note"} could not be found or read.")
                        }
                    } else {
                        actionRunner().apply(action)
                    }
                    _results.update { it + (key to result) }
                    if (result.ok) chats.markActionDone(chatId, messageId, index)
                    _working.update { it - key }
                }
            } finally {
                _working.update { it - keys.toSet() }
            }
            if (approved.isNotEmpty()) {
                append(chatId, ChatMessage(0, ChatRole.USER, "[Result of your read request, approved by the student: " + approved.joinToString(", ") { it.name } + ". Use it now to answer; do not mention this message.]", 0, attachments = approved, hidden = true))
                answer(chatId)
            }
        }
    }

    /** Marks a card as handled without changing anything. */
    fun dismiss(chatId: Long, messageId: Long, index: Int) {
        chats.markActionDone(chatId, messageId, index)
        _results.update { it - key(chatId, messageId, index) }
    }

    companion object {
        /** Whether this connection can search the web: Gemini and Claude have it built in, OpenRouter through its web plugin. */
        fun canSearch(connection: AiConnection) =
            connection.style == ApiStyle.GEMINI || connection.style == ApiStyle.ANTHROPIC || connection.baseUrl.contains("openrouter", ignoreCase = true)

        /** The most base64 text of files sent again with one request. */
        private const val MAX_SENT_BYTES = 24_000_000

        /** Messages sent in full, the most that are sent at all, and how many characters an older message keeps. */
        private const val FULL_TURNS = 8
        private const val MAX_TURNS = 30
        private const val OLD_TURN_CHARS = 700

        /** Messages whose attached files are sent again with a new question. */
        private const val FILE_MESSAGES = 2
    }
}
