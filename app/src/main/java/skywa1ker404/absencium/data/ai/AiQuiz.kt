package skywa1ker404.absencium.data.ai

import org.json.JSONArray
import org.json.JSONObject

/** One quiz question the AI wrote. [options] is empty for a typed answer; [correct] is the index of the right option for choice questions. */
data class QuizQuestion(
    val kind: Kind,
    val question: String,
    val options: List<String>,
    val correct: Int,
    /** The model answer for a typed question (and the right option's text for the others). */
    val answer: String,
    val accept: List<String>,
    val explain: String,
) {
    enum class Kind { CHOICE, TRUE_FALSE, TYPED }
}

/**
 * Quizzes. The AI never writes questions and answers as plain text: it sends one fenced `absencium-quiz` block with a JSON object
 * `{"title":"","questions":[{"type":"mcq|true_false|short","q":"","options":[],"answer":"","accept":[],"explain":""}]}` and the app
 * asks them one at a time, checks the answers and shows a score. Pure, so it can be tested.
 */
object AiQuiz {
    const val LABEL = "absencium-quiz"
    private val fence = Regex("```[ \\t]*" + LABEL + "[ \\t]*\\r?\\n?(.*?)(```|\\z)", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))

    private val anyFence = Regex("```[ \\t]*[A-Za-z0-9_-]*[ \\t]*\\r?\\n?(.*?)(```|\\z)", RegexOption.DOT_MATCHES_ALL)

    /**
     * The reply without its quiz block, and the quiz JSON when the block holds at least one usable question. A model that labels the block
     * differently (a plain fence, `json`) or sends the JSON bare is still understood, so raw JSON never reaches the chat.
     */
    fun extract(reply: String): Pair<String, String?> {
        val labelled = fence.find(reply)
        if (labelled != null) {
            val json = labelled.groupValues[1].trim()
            return reply.removeRange(labelled.range).trim() to (if (parse(json).isEmpty()) null else json)
        }
        for (match in anyFence.findAll(reply)) {
            val json = match.groupValues[1].trim()
            if (json.contains("\"questions\"") && parse(json).isNotEmpty()) return reply.removeRange(match.range).trim() to json
        }
        val at = reply.indexOf("\"questions\"")
        if (at >= 0) {
            val start = reply.lastIndexOf('{', at)
            val end = reply.lastIndexOf('}')
            if (start >= 0 && end > start) {
                val json = reply.substring(start, end + 1)
                if (parse(json).isNotEmpty()) return (reply.substring(0, start) + reply.substring(end + 1)).trim() to json
            }
        }
        return reply to null
    }

    fun parse(json: String): List<QuizQuestion> {
        val array: JSONArray = try {
            val trimmed = json.trim()
            if (trimmed.startsWith("[")) JSONArray(trimmed) else JSONObject(trimmed).optJSONArray("questions") ?: return emptyList()
        } catch (e: Exception) {
            return emptyList()
        }
        return (0 until array.length()).mapNotNull { array.optJSONObject(it)?.let(::question) }
    }

    fun title(json: String): String = try {
        if (json.trim().startsWith("{")) JSONObject(json.trim()).optString("title").trim() else ""
    } catch (e: Exception) {
        ""
    }

    private fun question(o: JSONObject): QuizQuestion? {
        val text = (o.optString("q").ifBlank { o.optString("question") }).trim()
        if (text.isEmpty()) return null
        val answer = o.opt("answer")?.toString()?.trim().orEmpty()
        val explain = o.optString("explain").trim()
        val accept = o.optJSONArray("accept")?.let { a -> (0 until a.length()).map { a.optString(it).trim() }.filter { it.isNotEmpty() } }.orEmpty()
        val type = o.optString("type").lowercase().replace("-", "_").replace(" ", "_")
        val options = o.optJSONArray("options")?.let { a -> (0 until a.length()).map { stripLetter(a.optString(it)) }.filter { it.isNotEmpty() } }.orEmpty()
        return when {
            type.startsWith("true") || type == "tf" || type == "boolean" -> {
                val isTrue = answer.lowercase().let { it == "true" || it == "t" || it == "yes" }
                QuizQuestion(QuizQuestion.Kind.TRUE_FALSE, text, listOf("True", "False"), if (isTrue) 0 else 1, if (isTrue) "True" else "False", emptyList(), explain)
            }
            options.size >= 2 && type != "short" && type != "typed" && type != "fill" -> {
                val index = optionIndex(answer, options)
                if (index < 0) null else QuizQuestion(QuizQuestion.Kind.CHOICE, text, options, index, options[index], emptyList(), explain)
            }
            answer.isNotEmpty() -> QuizQuestion(QuizQuestion.Kind.TYPED, text, emptyList(), -1, answer, accept, explain)
            else -> null
        }
    }

    private fun stripLetter(option: String) = option.trim().replace(Regex("^[A-Da-d][).:]\\s+"), "").trim()

    private fun optionIndex(answer: String, options: List<String>): Int {
        val a = answer.trim()
        if (a.length == 1 && a[0].uppercaseChar() in 'A'..'F') return (a[0].uppercaseChar() - 'A').takeIf { it < options.size } ?: -1
        val letter = Regex("^([A-Fa-f])[).:]").find(a)?.groupValues?.get(1)
        if (letter != null) return (letter[0].uppercaseChar() - 'A').takeIf { it < options.size } ?: -1
        val byText = options.indexOfFirst { norm(it) == norm(stripLetter(a)) }
        return byText
    }

    /** Lower case with punctuation and spaces dropped, so "The  Cell." equals "the cell". */
    fun norm(text: String) = text.lowercase().filter { it.isLetterOrDigit() }

    /**
     * Whether a typed answer matches: true when it equals the model answer or an accepted one (ignoring case and punctuation, or the same
     * number), null when it does not match and the student should judge it against the model answer themselves, since a worded answer can
     * be right in other words.
     */
    fun checkTyped(q: QuizQuestion, given: String): Boolean? {
        val g = norm(given)
        if (g.isEmpty()) return false
        val all = listOf(q.answer) + q.accept
        if (all.any { norm(it) == g }) return true
        val number = given.trim().replace(",", "").toDoubleOrNull()
        if (number != null && all.any { it.trim().replace(",", "").toDoubleOrNull() == number }) return true
        return null
    }
}
