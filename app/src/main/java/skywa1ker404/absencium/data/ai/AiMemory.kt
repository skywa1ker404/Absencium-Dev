package skywa1ker404.absencium.data.ai

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import skywa1ker404.absencium.logging.DebugLogger
import java.io.File

/**
 * One thing Absencium AI remembers. [chatId] is null for a global memory (used in every chat) and the chat's id for a memory that only
 * that conversation uses.
 */
data class MemoryItem(val id: Long, val text: String, val chatId: Long? = null, val created: Long = id, val updated: Long = created)

/** Reads and writes memories as JSON, separate from the file so it can be tested. */
object AiMemoryJson {
    fun encode(items: List<MemoryItem>): String = JSONArray().also { array ->
        items.forEach { m ->
            array.put(JSONObject().put("id", m.id).put("text", m.text).put("chat", m.chatId ?: JSONObject.NULL).put("created", m.created).put("updated", m.updated))
        }
    }.toString()

    fun decode(text: String): List<MemoryItem> {
        val array = JSONArray(text)
        return (0 until array.length()).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            val body = o.optString("text").trim()
            if (body.isEmpty()) null else MemoryItem(o.optLong("id"), body, if (o.isNull("chat")) null else o.optLong("chat"), o.optLong("created"), o.optLong("updated"))
        }
    }
}

/**
 * What Absencium AI remembers across chats, kept on this phone only (a small JSON file, never uploaded). Whether it is used at all is the
 * Memory switch in Settings, AI & Integrations. [relevant] picks the memories that matter to a question on this device, without any
 * request to a server: words are compared after trimming common endings, and the most overlapping memories come first.
 */
class AiMemory(context: Context) {
    private val file = File(context.filesDir, "ai_memory.json")
    private val lock = Any()
    private val _items = MutableStateFlow(load())
    val items: StateFlow<List<MemoryItem>> = _items.asStateFlow()

    /** Saves [text] unless the same thing is already remembered. Returns the saved item, or null for a duplicate or empty text. */
    fun add(text: String, chatId: Long? = null): MemoryItem? {
        synchronized(lock) {
            val clean = text.trim().replace(Regex("\\s+"), " ").take(MAX_TEXT)
            if (clean.isEmpty() || _items.value.any { it.chatId == chatId && it.text.equals(clean, ignoreCase = true) }) return null
            val now = System.currentTimeMillis()
            val id = maxOf(now, (_items.value.maxOfOrNull { it.id } ?: 0L) + 1)
            val item = MemoryItem(id, clean, chatId, now, now)
            change(listOf(item) + _items.value)
            return item
        }
    }

    fun update(id: Long, text: String) = synchronized(lock) {
        val clean = text.trim().take(MAX_TEXT)
        if (clean.isNotEmpty()) change(_items.value.map { if (it.id == id) it.copy(text = clean, updated = System.currentTimeMillis()) else it })
    }

    fun delete(id: Long) = synchronized(lock) { change(_items.value.filterNot { it.id == id }) }

    /** Forgets everything, or only what one chat remembered. */
    fun clear(chatId: Long? = null) = synchronized(lock) { change(if (chatId == null) emptyList() else _items.value.filterNot { it.chatId == chatId }) }

    fun forgetChat(chatId: Long) = clear(chatId)

    /** The memories to give the model for [query] in chat [chatId]: every global one when few, otherwise the best matches, plus this chat's own. */
    fun relevant(query: String, chatId: Long?, limit: Int = LIMIT): List<MemoryItem> = pick(_items.value, query, chatId, limit)

    private fun change(list: List<MemoryItem>) {
        _items.value = list.take(MAX_ITEMS)
        try {
            file.writeText(AiMemoryJson.encode(_items.value))
        } catch (e: Exception) {
            DebugLogger.e("AiMemory", "The memories could not be saved", e)
        }
    }

    private fun load(): List<MemoryItem> = try {
        if (!file.isFile) emptyList() else AiMemoryJson.decode(file.readText())
    } catch (e: Exception) {
        DebugLogger.e("AiMemory", "The memories could not be read", e)
        emptyList()
    }

    companion object {
        const val MAX_TEXT = 400
        const val MAX_ITEMS = 300
        const val LIMIT = 24

        private val STOP = setOf("the", "a", "an", "and", "or", "of", "to", "in", "is", "are", "my", "me", "i", "it", "for", "on", "with", "that", "this", "what", "how", "do", "does", "you")

        fun words(text: String): Set<String> =
            text.lowercase().split(Regex("[^a-z0-9]+")).filter { it.length > 1 && it !in STOP }.map { stem(it) }.toSet()

        private fun stem(w: String): String = when {
            w.length > 5 && w.endsWith("ing") -> w.dropLast(3)
            w.length > 4 && w.endsWith("ed") -> w.dropLast(2)
            w.length > 4 && w.endsWith("es") -> w.dropLast(2)
            w.length > 3 && w.endsWith("s") -> w.dropLast(1)
            else -> w
        }

        /** This chat's memories always come; global ones come best-match first, newest first on a tie, so a short list is sent whole. */
        fun pick(all: List<MemoryItem>, query: String, chatId: Long?, limit: Int): List<MemoryItem> {
            val own = all.filter { chatId != null && it.chatId == chatId }
            val global = all.filter { it.chatId == null }
            if (global.size + own.size <= limit) return own + global
            val q = words(query)
            val ranked = global.sortedWith(compareByDescending<MemoryItem> { m -> words(m.text).count { it in q } }.thenByDescending { it.updated })
            return own + ranked.take((limit - own.size).coerceAtLeast(0))
        }
    }
}
