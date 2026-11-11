package skywa1ker404.absencium.data.ai

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import skywa1ker404.absencium.logging.DebugLogger
import java.io.File

/** Where an AI action's result lives in the app, so its Open button can take the student there. */
enum class AiTargetType { NOTE, SUBJECT, SYLLABUS, ATTENDANCE, TRASH }

/**
 * One thing Absencium AI did: notes or a homework answer made on the Add Entry page, or a change made from the chat.
 * It keeps only what the student chose or typed ([fields]) and a short preview of the result, never the instructions
 * the app sent to the model.
 */
data class AiActionEntry(
    val id: Long,
    val time: Long,
    /** What was done, in plain words: "Generated notes", "Added a note", "Moved an exam to the Trash". */
    val action: String,
    /** What it was done to: the note's title, the subject, the exam. */
    val title: String,
    val fields: List<Pair<String, String>>,
    val ok: Boolean,
    val preview: String?,
    val error: String?,
    val targetType: AiTargetType?,
    val targetId: Long,
    /** The hidden hub chat this happened in, when it happened in one. */
    val chatId: String? = null,
)

/**
 * The AI actions folder: the list behind Statistics, then AI actions in the chat. A small JSON file in the app's private storage,
 * never uploaded; the newest [MAX_ENTRIES] are kept.
 */
class AiActionLog(context: Context) {
    private val file = File(context.filesDir, "ai_actions.json")
    private val lock = Any()
    private val _entries = MutableStateFlow(load())
    val entries: StateFlow<List<AiActionEntry>> = _entries.asStateFlow()

    fun add(
        action: String,
        title: String,
        fields: List<Pair<String, String>>,
        ok: Boolean,
        preview: String? = null,
        error: String? = null,
        targetType: AiTargetType? = null,
        targetId: Long = 0,
        chatId: String? = null,
    ) {
        val now = System.currentTimeMillis()
        synchronized(lock) {
            // Ids are the time in milliseconds, nudged forward so two actions in the same millisecond stay distinct.
            val id = maxOf(now, (_entries.value.firstOrNull()?.id ?: 0L) + 1)
            val entry = AiActionEntry(
                id, now, action, title.take(120),
                fields.filter { it.second.isNotBlank() }.map { it.first to it.second.trim().take(MAX_FIELD) },
                ok, preview?.take(MAX_PREVIEW), error?.take(300), targetType, targetId, chatId,
            )
            _entries.value = (listOf(entry) + _entries.value).take(MAX_ENTRIES)
            save(_entries.value)
        }
    }

    fun remove(id: Long) {
        synchronized(lock) {
            _entries.value = _entries.value.filterNot { it.id == id }
            save(_entries.value)
        }
    }

    fun clear() {
        synchronized(lock) {
            _entries.value = emptyList()
            save(emptyList())
        }
    }

    fun find(id: Long): AiActionEntry? = _entries.value.firstOrNull { it.id == id }

    /** The first thing a hidden chat made or changed that the app can show, or null when it made nothing. */
    fun targetOf(chatId: String): AiActionEntry? = _entries.value.lastOrNull { it.chatId == chatId && it.ok && it.targetType != null }

    /** The hidden chats that made or changed something the app can show. */
    val chatsWithTargets: Flow<Set<String>> = _entries.map { list -> list.filter { it.ok && it.targetType != null }.mapNotNull { it.chatId }.toSet() }

    fun removeChat(chatId: String) {
        synchronized(lock) {
            _entries.value = _entries.value.filterNot { it.chatId == chatId }
            save(_entries.value)
        }
    }

    private fun load(): List<AiActionEntry> = try {
        if (!file.isFile) emptyList() else {
            val array = JSONArray(file.readText())
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                val fields = o.optJSONArray("fields")
                AiActionEntry(
                    id = o.getLong("id"),
                    time = o.getLong("time"),
                    action = o.optString("action"),
                    title = o.optString("title"),
                    fields = (0 until (fields?.length() ?: 0)).map { j -> fields!!.getJSONArray(j).let { it.getString(0) to it.getString(1) } },
                    ok = o.optBoolean("ok", true),
                    preview = if (o.has("preview")) o.optString("preview") else null,
                    error = if (o.has("error")) o.optString("error") else null,
                    targetType = o.optString("targetType").takeIf { it.isNotEmpty() }?.let { runCatching { AiTargetType.valueOf(it) }.getOrNull() },
                    targetId = o.optLong("targetId"),
                    chatId = o.optString("chatId").takeIf { it.isNotEmpty() },
                )
            }
        }
    } catch (e: Exception) {
        DebugLogger.e("AiActionLog", "The AI actions could not be read", e)
        emptyList()
    }

    private fun save(list: List<AiActionEntry>) {
        try {
            val array = JSONArray()
            list.forEach { e ->
                array.put(
                    JSONObject().put("id", e.id).put("time", e.time).put("action", e.action).put("title", e.title)
                        .put("fields", JSONArray().apply { e.fields.forEach { put(JSONArray().put(it.first).put(it.second)) } })
                        .put("ok", e.ok)
                        .apply {
                            e.preview?.let { put("preview", it) }
                            e.error?.let { put("error", it) }
                            e.targetType?.let { put("targetType", it.name); put("targetId", e.targetId) }
                            e.chatId?.let { put("chatId", it) }
                        },
                )
            }
            file.writeText(array.toString())
        } catch (e: Exception) {
            DebugLogger.e("AiActionLog", "The AI actions could not be saved", e)
        }
    }

    companion object {
        const val MAX_ENTRIES = 300
        const val MAX_FIELD = 400
        const val MAX_PREVIEW = 1_500
    }
}
