package skywa1ker404.absencium.data.ai

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import skywa1ker404.absencium.logging.DebugLogger
import java.io.File

enum class ChatRole { USER, ASSISTANT }

/** A file sent with a message, kept in the app's private storage so it can be shown again and sent again with later questions. */
data class ChatAttachment(val name: String, val mime: String, val path: String) {
    val isImage: Boolean get() = mime.startsWith("image/")
    val isAudio: Boolean get() = mime.startsWith("audio/")
    val isVideo: Boolean get() = mime.startsWith("video/")
}

/**
 * One message of a chat. [sources] are the web pages an answer was grounded on, [attachments] the files sent with a question. [actions] is the raw JSON of the changes the assistant proposed (shown as cards the student confirms),
 * [doneActions] the positions of those already applied or dismissed, so a card does not come back after the app restarts.
 */
data class ChatMessage(
    val id: Long,
    val role: ChatRole,
    val text: String,
    val time: Long,
    val model: String = "",
    val failed: Boolean = false,
    val actions: String? = null,
    val doneActions: Set<Int> = emptySet(),
    val sources: List<ChatSource> = emptyList(),
    val attachments: List<ChatAttachment> = emptyList(),
)

/** A conversation. [provider] and [model] are set when the student picked another model for this chat than the default one. */
data class AiChat(
    val id: Long,
    val title: String,
    val pinned: Boolean,
    val created: Long,
    val updated: Long,
    val provider: String? = null,
    val model: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    /** Web search is on for this chat (needs a model that can search). */
    val webSearch: Boolean = false,
    /** Study mode: the AI teaches step by step instead of only giving answers. */
    val study: Boolean = false,
    /** Extra reasoning before answering (Gemini, Claude and OpenAI reasoning models). */
    val thinking: Boolean = false,
    /** Hidden from the main list, kept and searchable under Archived. */
    val archived: Boolean = false,
    /** Study mode difficulty: "", "beginner", "school", "college" or "expert". */
    val level: String = "",
    /** The id of the assistant profile this chat uses, or empty for the plain assistant. */
    val assistant: String = "",
    /** An incognito chat: kept only in memory while the screen is open, never written to the file, with no memory read or saved. */
    val incognito: Boolean = false,
) {
    /** What the sidebar shows: the title, or the start of the first message while the chat has no name yet. */
    val label: String
        get() = title.ifBlank { messages.firstOrNull { it.role == ChatRole.USER }?.text?.lineSequence()?.firstOrNull().orEmpty().take(60).ifBlank { "New chat" } }
}

/** Reads and writes chats as JSON. Separate from the file handling so it can be tested. */
object AiChatsJson {
    fun encode(chats: List<AiChat>): String {
        val array = JSONArray()
        chats.forEach { chat ->
            val messages = JSONArray()
            chat.messages.forEach { m ->
                messages.put(
                    JSONObject().put("id", m.id).put("role", m.role.name).put("text", m.text).put("time", m.time)
                        .put("model", m.model).put("failed", m.failed)
                        .apply {
                            m.actions?.let { put("actions", it) }
                            if (m.doneActions.isNotEmpty()) put("done", m.doneActions.joinToString(","))
                            if (m.sources.isNotEmpty()) {
                                put("sources", JSONArray().apply { m.sources.forEach { src -> put(JSONObject().put("title", src.title).put("url", src.url)) } })
                            }
                            if (m.attachments.isNotEmpty()) {
                                put(
                                    "attachments",
                                    JSONArray().apply { m.attachments.forEach { a -> put(JSONObject().put("name", a.name).put("mime", a.mime).put("path", a.path)) } },
                                )
                            }
                        },
                )
            }
            array.put(
                JSONObject().put("id", chat.id).put("title", chat.title).put("pinned", chat.pinned)
                    .put("created", chat.created).put("updated", chat.updated)
                    .put("web", chat.webSearch).put("study", chat.study).put("thinking", chat.thinking).put("archived", chat.archived)
                    .put("level", chat.level).put("assistant", chat.assistant)
                    .apply {
                        chat.provider?.let { put("provider", it) }
                        chat.model?.let { put("model", it) }
                    }
                    .put("messages", messages),
            )
        }
        return array.toString()
    }

    fun decode(text: String): List<AiChat> {
        val array = JSONArray(text)
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            val messages = o.optJSONArray("messages") ?: JSONArray()
            AiChat(
                id = o.getLong("id"),
                title = o.optString("title"),
                pinned = o.optBoolean("pinned"),
                created = o.optLong("created"),
                updated = o.optLong("updated"),
                provider = if (o.has("provider")) o.optString("provider") else null,
                model = if (o.has("model")) o.optString("model") else null,
                webSearch = o.optBoolean("web"),
                study = o.optBoolean("study"),
                thinking = o.optBoolean("thinking"),
                archived = o.optBoolean("archived"),
                level = o.optString("level"),
                assistant = o.optString("assistant"),
                messages = (0 until messages.length()).map { j ->
                    val m = messages.getJSONObject(j)
                    ChatMessage(
                        id = m.getLong("id"),
                        role = runCatching { ChatRole.valueOf(m.optString("role")) }.getOrDefault(ChatRole.USER),
                        text = m.optString("text"),
                        time = m.optLong("time"),
                        model = m.optString("model"),
                        failed = m.optBoolean("failed"),
                        actions = if (m.has("actions")) m.optString("actions") else null,
                        doneActions = m.optString("done").split(',').mapNotNull { it.trim().toIntOrNull() }.toSet(),
                        sources = m.optJSONArray("sources")?.let { a ->
                            (0 until a.length()).mapNotNull { k -> a.optJSONObject(k)?.let { o -> ChatSource(o.optString("title"), o.optString("url")) } }
                        }.orEmpty(),
                        attachments = m.optJSONArray("attachments")?.let { a ->
                            (0 until a.length()).mapNotNull { k -> a.optJSONObject(k)?.let { o -> ChatAttachment(o.optString("name"), o.optString("mime"), o.optString("path")) } }
                        }.orEmpty(),
                    )
                },
            )
        }
    }
}

/**
 * The chats of Absencium AI, kept on this phone only (a small JSON file in the app's private storage, never uploaded).
 * Pinned chats come first, then the most recently used.
 */
class AiChats(context: Context) {
    private val file = File(context.filesDir, "ai_chats.json")
    private val lock = Any()
    private val _chats = MutableStateFlow(sorted(load()))
    val chats: StateFlow<List<AiChat>> = _chats.asStateFlow()

    fun get(id: Long): AiChat? = _chats.value.firstOrNull { it.id == id }

    /** A new empty chat. It is only kept once it holds a message. */
    fun blank(): AiChat {
        val now = System.currentTimeMillis()
        return AiChat(id = now, title = "", pinned = false, created = now, updated = now)
    }

    /** Saves [chat] (new or changed). A chat without messages is dropped instead of being kept as an empty entry. */
    fun put(chat: AiChat) = change { list ->
        val rest = list.filterNot { it.id == chat.id }
        if (chat.messages.isEmpty()) rest else rest + chat.copy(messages = chat.messages.takeLast(MAX_MESSAGES))
    }

    fun rename(id: Long, title: String) = update(id) { it.copy(title = title.trim().take(80)) }
    fun pin(id: Long, pinned: Boolean) = update(id) { it.copy(pinned = pinned) }
    fun archive(id: Long, archived: Boolean) = update(id) { it.copy(archived = archived) }
    fun delete(id: Long) = change { list -> list.filterNot { it.id == id } }

    /** Forgets every incognito chat (they were never on the file). */
    fun dropIncognito() {
        if (_chats.value.any { it.incognito }) change { list -> list.filterNot { it.incognito } }
    }

    /** Marks the action card at [index] of message [messageId] as handled. */
    fun markActionDone(chatId: Long, messageId: Long, index: Int) = update(chatId) { chat ->
        chat.copy(messages = chat.messages.map { if (it.id == messageId) it.copy(doneActions = it.doneActions + index) else it })
    }

    private fun update(id: Long, block: (AiChat) -> AiChat) = change { list -> list.map { if (it.id == id) block(it) else it } }

    private fun change(block: (List<AiChat>) -> List<AiChat>) {
        synchronized(lock) {
            _chats.value = sorted(block(_chats.value)).take(MAX_CHATS)
            save(_chats.value)
        }
    }

    private fun sorted(list: List<AiChat>) = list.sortedWith(compareByDescending<AiChat> { it.pinned }.thenByDescending { it.updated })

    private fun load(): List<AiChat> = try {
        if (!file.isFile) emptyList() else AiChatsJson.decode(file.readText())
    } catch (e: Exception) {
        DebugLogger.e("AiChats", "The chats could not be read", e)
        emptyList()
    }

    private fun save(list: List<AiChat>) {
        try {
            file.writeText(AiChatsJson.encode(list.filterNot { it.incognito }))
        } catch (e: Exception) {
            DebugLogger.e("AiChats", "The chats could not be saved", e)
        }
    }

    companion object {
        const val MAX_CHATS = 100
        const val MAX_MESSAGES = 200
    }
}
