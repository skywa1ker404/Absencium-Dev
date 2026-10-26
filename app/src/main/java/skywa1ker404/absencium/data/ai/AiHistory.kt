package skywa1ker404.absencium.data.ai

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import skywa1ker404.absencium.logging.DebugLogger
import java.io.File

/** One AI request: what was sent, what came back (or why it failed). */
data class AiHistoryEntry(
    val time: Long,
    val model: String,
    val prompt: String,
    val result: String?,
    val error: String?,
    val attachments: Int,
)

/**
 * The AI Hub's history: every request and answer, kept on this phone only (a small JSON file in the app's private storage,
 * never uploaded). The newest [MAX_ENTRIES] are kept.
 */
class AiHistory(context: Context) {
    private val file = File(context.filesDir, "ai_history.json")
    private val lock = Any()
    private val _entries = MutableStateFlow(load())
    val entries: StateFlow<List<AiHistoryEntry>> = _entries.asStateFlow()

    fun add(model: String, prompt: String, result: String?, error: String?, attachments: Int) {
        val entry = AiHistoryEntry(System.currentTimeMillis(), model, prompt.take(MAX_PROMPT), result?.take(MAX_RESULT), error?.take(500), attachments)
        synchronized(lock) {
            _entries.value = (listOf(entry) + _entries.value).take(MAX_ENTRIES)
            save(_entries.value)
        }
    }

    fun clear() {
        synchronized(lock) {
            _entries.value = emptyList()
            save(emptyList())
        }
    }

    fun remove(time: Long) {
        synchronized(lock) {
            _entries.value = _entries.value.filterNot { it.time == time }
            save(_entries.value)
        }
    }

    private fun load(): List<AiHistoryEntry> = try {
        if (!file.isFile) emptyList() else {
            val array = JSONArray(file.readText())
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                AiHistoryEntry(
                    time = o.getLong("time"),
                    model = o.optString("model"),
                    prompt = o.optString("prompt"),
                    result = if (o.has("result")) o.optString("result") else null,
                    error = if (o.has("error")) o.optString("error") else null,
                    attachments = o.optInt("attachments"),
                )
            }
        }
    } catch (e: Exception) {
        DebugLogger.e("AiHistory", "The AI history could not be read", e)
        emptyList()
    }

    private fun save(list: List<AiHistoryEntry>) {
        try {
            val array = JSONArray()
            list.forEach {
                array.put(
                    JSONObject().put("time", it.time).put("model", it.model).put("prompt", it.prompt)
                        .apply { it.result?.let { r -> put("result", r) }; it.error?.let { e -> put("error", e) } }
                        .put("attachments", it.attachments),
                )
            }
            file.writeText(array.toString())
        } catch (e: Exception) {
            DebugLogger.e("AiHistory", "The AI history could not be saved", e)
        }
    }

    companion object {
        const val MAX_ENTRIES = 200
        const val MAX_PROMPT = 6_000
        const val MAX_RESULT = 30_000
    }
}
