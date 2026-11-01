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
 * A reusable assistant: custom instructions, and optionally the model and temperature it prefers. [builtIn] ones ship with the app and
 * cannot be edited or deleted (copy one to change it).
 */
data class AssistantProfile(
    val id: String,
    val name: String,
    val instructions: String,
    val provider: String? = null,
    val model: String? = null,
    val temperature: Double? = null,
    val builtIn: Boolean = false,
)

/** Reads and writes assistants as JSON; also the format of exported and imported assistant files. */
object AiAssistantsJson {
    const val FORMAT = "absencium-assistants"

    fun encode(items: List<AssistantProfile>): String = JSONObject().put("format", FORMAT).put(
        "assistants",
        JSONArray().also { array ->
            items.forEach { a ->
                array.put(
                    JSONObject().put("id", a.id).put("name", a.name).put("instructions", a.instructions)
                        .put("provider", a.provider ?: JSONObject.NULL).put("model", a.model ?: JSONObject.NULL)
                        .put("temperature", a.temperature ?: JSONObject.NULL),
                )
            }
        },
    ).toString()

    /** Every readable assistant of an exported file (or a plain list). Entries without a name or instructions are skipped. */
    fun decode(text: String): List<AssistantProfile> {
        val trimmed = text.trim()
        val array = if (trimmed.startsWith("[")) JSONArray(trimmed) else JSONObject(trimmed).optJSONArray("assistants") ?: JSONArray()
        return (0 until array.length()).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            val name = o.optString("name").trim()
            val instructions = o.optString("instructions").trim()
            if (name.isEmpty() || instructions.isEmpty()) return@mapNotNull null
            AssistantProfile(
                id = o.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                name = name.take(60),
                instructions = instructions.take(MAX_INSTRUCTIONS),
                provider = o.optString("provider").takeIf { it.isNotBlank() && it != "null" },
                model = o.optString("model").takeIf { it.isNotBlank() && it != "null" },
                temperature = if (o.isNull("temperature")) null else o.optDouble("temperature").takeIf { !it.isNaN() }?.coerceIn(0.0, 2.0),
            )
        }
    }

    const val MAX_INSTRUCTIONS = 4_000
}

/** The student's assistants (a small JSON file on this phone) and the presets that ship with the app. */
class AiAssistants(context: Context) {
    private val file = File(context.filesDir, "ai_assistants.json")
    private val lock = Any()
    private val mine = MutableStateFlow(load())
    private val _all = MutableStateFlow(PRESETS + mine.value)

    /** The presets first, then the student's own. */
    val all: StateFlow<List<AssistantProfile>> = _all.asStateFlow()

    fun find(id: String): AssistantProfile? = if (id.isBlank()) null else _all.value.firstOrNull { it.id == id }

    fun save(profile: AssistantProfile) {
        if (profile.builtIn) return
        synchronized(lock) { change(mine.value.filterNot { it.id == profile.id } + profile.copy(builtIn = false)) }
    }

    fun delete(id: String) = synchronized(lock) { change(mine.value.filterNot { it.id == id }) }

    /** The student's own assistants as an exportable file's text. */
    fun export(): String = AiAssistantsJson.encode(mine.value)

    /** Adds the assistants of an exported file as new ones (never replacing existing). Returns how many were added, or -1 when unreadable. */
    fun import(text: String): Int = synchronized(lock) {
        try {
            val incoming = AiAssistantsJson.decode(text).map { it.copy(id = java.util.UUID.randomUUID().toString(), builtIn = false) }
            if (incoming.isNotEmpty()) change(mine.value + incoming)
            incoming.size
        } catch (e: Exception) {
            DebugLogger.e("AiAssistants", "An assistant file could not be read", e)
            -1
        }
    }

    private fun change(list: List<AssistantProfile>) {
        mine.value = list.take(MAX_ASSISTANTS)
        _all.value = PRESETS + mine.value
        try {
            file.writeText(AiAssistantsJson.encode(mine.value))
        } catch (e: Exception) {
            DebugLogger.e("AiAssistants", "The assistants could not be saved", e)
        }
    }

    private fun load(): List<AssistantProfile> = try {
        if (!file.isFile) emptyList() else AiAssistantsJson.decode(file.readText())
    } catch (e: Exception) {
        DebugLogger.e("AiAssistants", "The assistants could not be read", e)
        emptyList()
    }

    companion object {
        const val MAX_ASSISTANTS = 30

        val PRESETS = listOf(
            AssistantProfile(
                "preset-study", "Study tutor",
                "Act as a patient tutor. Explain from first principles, build up step by step with examples and analogies, check understanding with a short question at the end, and point out common mistakes.",
                builtIn = true,
            ),
            AssistantProfile(
                "preset-research", "Research assistant",
                "Act as a careful research assistant. Be precise, separate facts from opinion, state how sure you are, compare sources and views, and name what is still unknown. Cite sources when you have them.",
                temperature = 0.3, builtIn = true,
            ),
            AssistantProfile(
                "preset-writing", "Writing coach",
                "Act as a writing coach. Improve clarity, structure and tone while keeping the student's own voice. Show the improved text, then briefly say what changed and why.",
                temperature = 0.8, builtIn = true,
            ),
            AssistantProfile(
                "preset-exam", "Exam coach",
                "Act as an exam coach. Focus on what is likely to be asked, teach answer structure and marking schemes, give timed practice questions, and give blunt, specific feedback on answers.",
                builtIn = true,
            ),
        )
    }
}
