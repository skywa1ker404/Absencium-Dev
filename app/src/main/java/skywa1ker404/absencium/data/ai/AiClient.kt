package skywa1ker404.absencium.data.ai

import skywa1ker404.absencium.data.security.ApiKeyVault
import skywa1ker404.absencium.logging.DebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Whether a model costs money to use, as far as the provider's own model list says. */
enum class ModelCost { FREE, PAID, UNKNOWN }

/**
 * One model from a provider's list. [cost] is only known when the provider publishes prices in its
 * list (OpenRouter does); every other provider returns [ModelCost.UNKNOWN] because its list does not
 * say. [acceptsImages] is null when the list does not say either.
 */
data class AiModel(
    val id: String,
    val label: String,
    val cost: ModelCost,
    val acceptsImages: Boolean?,
)

/** Where and how to talk to the provider. Built from the saved settings (or the unsaved form). */
data class AiConnection(
    val provider: AiProvider,
    val style: ApiStyle,
    val baseUrl: String,
    val model: String,
)

/**
 * A base64 file sent along with a prompt: a picture (JPEG or PNG), a PDF, or a recording. Not every provider takes every kind;
 * the client says so in plain words when one cannot.
 */
class AiImage(val mimeType: String, val base64: String) {
    val isPdf: Boolean get() = mimeType == "application/pdf"
    val isAudio: Boolean get() = mimeType.startsWith("audio/")
}

/** Something went wrong talking to the provider; [message] is written for the user. */
class AiException(message: String) : Exception(message)

/**
 * A small HTTP client for the three request shapes Ask AI supports: OpenAI-compatible chat
 * completions, Anthropic messages and Google Gemini generateContent. Model lists use each provider's
 * documented list endpoint. Keys come from the encrypted [ApiKeyVault] and are never logged.
 */
class AiClient(private val keyVault: ApiKeyVault) {

    /** Every request and its answer is kept here for the AI Hub; set once by the app container. */
    var history: AiHistory? = null

    /** The models the provider offers, sorted by name. */
    suspend fun listModels(connection: AiConnection): List<AiModel> = withContext(Dispatchers.IO) {
        val key = keyFor(connection)
        val base = connection.baseUrl.trim().trimEnd('/')
        val models = when (connection.style) {
            ApiStyle.OPENAI_COMPATIBLE -> {
                val body = call("GET", "$base/models", openAiHeaders(key), null)
                parseOpenAiModels(body, openRouter = connection.provider == AiProvider.OPENROUTER)
            }

            ApiStyle.ANTHROPIC -> {
                val body = call("GET", "$base/models?limit=1000", anthropicHeaders(key), null)
                parseAnthropicModels(body)
            }

            ApiStyle.GEMINI -> listGeminiModels(base, key)
        }
        models.sortedBy { it.label.lowercase() }
    }

    /**
     * Sends [prompt] (and any [images]) to the connection's model and returns the reply text. [videoUrl] is only
     * used by Gemini, which can read a public YouTube link directly; the other styles ignore it.
     */
    suspend fun generate(
        connection: AiConnection,
        prompt: String,
        images: List<AiImage> = emptyList(),
        videoUrl: String? = null,
        maxTokens: Int? = null,
    ): String {
        try {
            val reply = generateRaw(connection, prompt, images, videoUrl, maxTokens)
            history?.add(connection.model, prompt, reply, null, images.size)
            return reply
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            history?.add(connection.model, prompt, null, e.message ?: e.javaClass.simpleName, images.size)
            throw e
        }
    }

    private suspend fun generateRaw(
        connection: AiConnection,
        prompt: String,
        images: List<AiImage>,
        videoUrl: String?,
        maxTokens: Int?,
    ): String =
        withContext(Dispatchers.IO) {
            val key = keyFor(connection)
            val base = connection.baseUrl.trim().trimEnd('/')
            val model = connection.model.trim()
            if (model.isEmpty()) throw AiException("Choose a model in Settings, AI & Integrations first.")
            when (connection.style) {
                ApiStyle.OPENAI_COMPATIBLE -> {
                    val content = JSONArray().put(JSONObject().put("type", "text").put("text", prompt))
                    images.forEach {
                        when {
                            it.isPdf -> content.put(
                                JSONObject().put("type", "file").put(
                                    "file",
                                    JSONObject().put("filename", "document.pdf").put("file_data", "data:application/pdf;base64,${it.base64}"),
                                ),
                            )
                            it.isAudio -> {
                                val format = when (it.mimeType) {
                                    "audio/mpeg", "audio/mp3" -> "mp3"
                                    "audio/wav", "audio/x-wav", "audio/wave" -> "wav"
                                    else -> throw AiException(
                                        "This kind of API only takes WAV or MP3 audio, and Absencium records M4A. " +
                                            "Pick a Google Gemini model in Settings, AI & Integrations to turn recordings into text.",
                                    )
                                }
                                content.put(JSONObject().put("type", "input_audio").put("input_audio", JSONObject().put("data", it.base64).put("format", format)))
                            }
                            else -> content.put(
                                JSONObject().put("type", "image_url")
                                    .put("image_url", JSONObject().put("url", "data:${it.mimeType};base64,${it.base64}")),
                            )
                        }
                    }
                    val body = JSONObject().put("model", model).put(
                        "messages",
                        JSONArray().put(JSONObject().put("role", "user").put("content", content)),
                    )
                    val reply = call("POST", "$base/chat/completions", openAiHeaders(key), body.toString())
                    parseOpenAiReply(reply)
                }

                ApiStyle.ANTHROPIC -> {
                    val content = JSONArray()
                    images.forEach {
                        if (it.isAudio) {
                            throw AiException("Claude models cannot listen to audio. Pick a Google Gemini model in Settings, AI & Integrations to turn recordings into text.")
                        }
                        content.put(
                            JSONObject().put("type", if (it.isPdf) "document" else "image").put(
                                "source",
                                JSONObject().put("type", "base64").put("media_type", it.mimeType).put("data", it.base64),
                            ),
                        )
                    }
                    content.put(JSONObject().put("type", "text").put("text", prompt))
                    val body = JSONObject().put("model", model).put("max_tokens", maxTokens ?: 4096).put(
                        "messages",
                        JSONArray().put(JSONObject().put("role", "user").put("content", content)),
                    )
                    val reply = call("POST", "$base/messages", anthropicHeaders(key), body.toString())
                    parseAnthropicReply(reply)
                }

                ApiStyle.GEMINI -> {
                    val parts = JSONArray().put(JSONObject().put("text", prompt))
                    // Gemini can watch a public YouTube video by its link; no other provider can.
                    if (videoUrl != null) {
                        parts.put(JSONObject().put("file_data", JSONObject().put("file_uri", videoUrl)))
                    }
                    images.forEach {
                        // Gemini lists AAC, MP3, WAV, OGG, FLAC and AIFF; an M4A recording is AAC audio in an MP4 wrapper.
                        val mime = if (it.mimeType == "audio/mp4" || it.mimeType == "audio/x-m4a" || it.mimeType == "audio/m4a") "audio/aac" else it.mimeType
                        parts.put(
                            JSONObject().put(
                                "inline_data",
                                JSONObject().put("mime_type", mime).put("data", it.base64),
                            ),
                        )
                    }
                    val body = JSONObject().put("contents", JSONArray().put(JSONObject().put("parts", parts)))
                    if (maxTokens != null) body.put("generationConfig", JSONObject().put("maxOutputTokens", maxTokens))
                    val id = model.removePrefix("models/")
                    val url = "$base/models/$id:generateContent?key=${URLEncoder.encode(key.orEmpty(), "UTF-8")}"
                    val reply = call("POST", url, emptyMap(), body.toString())
                    parseGeminiReply(reply)
                }
            }
        }

    private fun keyFor(connection: AiConnection): String? {
        val key = keyVault.read()
        if (connection.provider.keyRequired && key.isNullOrBlank()) {
            throw AiException("Save your API key first (Settings, AI & Integrations, API key).")
        }
        return key
    }

    private fun openAiHeaders(key: String?): Map<String, String> =
        if (key.isNullOrBlank()) emptyMap() else mapOf("Authorization" to "Bearer $key")

    private fun anthropicHeaders(key: String?): Map<String, String> =
        mapOf("x-api-key" to key.orEmpty(), "anthropic-version" to ANTHROPIC_VERSION)

    private fun listGeminiModels(base: String, key: String?): List<AiModel> {
        val all = mutableListOf<AiModel>()
        var pageToken: String? = null
        var pages = 0
        do {
            val url = buildString {
                append("$base/models?pageSize=1000&key=").append(URLEncoder.encode(key.orEmpty(), "UTF-8"))
                if (pageToken != null) append("&pageToken=").append(URLEncoder.encode(pageToken, "UTF-8"))
            }
            val json = JSONObject(call("GET", url, emptyMap(), null))
            val array = json.optJSONArray("models") ?: JSONArray()
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val methods = item.optJSONArray("supportedGenerationMethods")
                // Only models that can generate text are useful for Ask AI.
                if (methods != null && (0 until methods.length()).none { methods.getString(it) == "generateContent" }) continue
                val id = item.getString("name").removePrefix("models/")
                all += AiModel(id, item.optString("displayName", id).ifBlank { id }, ModelCost.UNKNOWN, null)
            }
            pageToken = json.optString("nextPageToken", "").ifBlank { null }
            pages++
        } while (pageToken != null && pages < MAX_PAGES)
        return all
    }

    private fun parseOpenAiModels(body: String, openRouter: Boolean): List<AiModel> {
        val data = JSONObject(body).optJSONArray("data") ?: return emptyList()
        return (0 until data.length()).map { i ->
            val item = data.getJSONObject(i)
            val id = item.getString("id")
            if (!openRouter) {
                AiModel(id, id, ModelCost.UNKNOWN, null)
            } else {
                // OpenRouter publishes prices as decimal strings in USD per token; zero for both means free.
                val pricing = item.optJSONObject("pricing")
                val prompt = pricing?.optString("prompt", "")?.toDoubleOrNull()
                val completion = pricing?.optString("completion", "")?.toDoubleOrNull()
                val cost = when {
                    prompt != null && completion != null -> if (prompt == 0.0 && completion == 0.0) ModelCost.FREE else ModelCost.PAID
                    id.endsWith(":free") -> ModelCost.FREE
                    else -> ModelCost.UNKNOWN
                }
                val modalities = item.optJSONObject("architecture")?.optJSONArray("input_modalities")
                val images = modalities?.let { m -> (0 until m.length()).any { m.getString(it) == "image" } }
                AiModel(id, item.optString("name", id).ifBlank { id }, cost, images)
            }
        }
    }

    private fun parseAnthropicModels(body: String): List<AiModel> {
        val data = JSONObject(body).optJSONArray("data") ?: return emptyList()
        return (0 until data.length()).map { i ->
            val item = data.getJSONObject(i)
            val id = item.getString("id")
            val images = item.optJSONObject("capabilities")?.optJSONObject("image_input")?.optBoolean("supported")
            AiModel(id, item.optString("display_name", id).ifBlank { id }, ModelCost.UNKNOWN, images)
        }
    }

    private fun parseOpenAiReply(body: String): String {
        val message = JSONObject(body).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")
            ?: throw AiException("The provider's reply had no message.")
        val content = message.opt("content")
        val text = when (content) {
            is String -> content
            is JSONArray -> (0 until content.length()).joinToString("") { content.optJSONObject(it)?.optString("text").orEmpty() }
            else -> ""
        }
        return text.ifBlank { throw AiException("The provider's reply was empty.") }
    }

    private fun parseAnthropicReply(body: String): String {
        val content = JSONObject(body).optJSONArray("content") ?: throw AiException("The provider's reply had no content.")
        val text = (0 until content.length()).joinToString("") {
            val block = content.getJSONObject(it)
            if (block.optString("type") == "text") block.optString("text") else ""
        }
        return text.ifBlank { throw AiException("The provider's reply was empty.") }
    }

    /**
     * Draws a picture from [prompt] and returns its bytes (PNG or JPEG). Gemini uses its image model through the same key;
     * OpenAI-compatible services use /images/generations. Claude models cannot draw, which is said plainly.
     */
    suspend fun generateImage(connection: AiConnection, prompt: String): ByteArray =
        withContext(Dispatchers.IO) {
            val key = keyFor(connection)
            val base = connection.baseUrl.trim().trimEnd('/')
            when (connection.style) {
                ApiStyle.ANTHROPIC -> throw AiException("Claude models cannot draw pictures. Use a Google Gemini or OpenAI key to generate diagrams, or turn on fetching them from the internet.")
                ApiStyle.GEMINI -> {
                    val body = JSONObject()
                        .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
                        .put("generationConfig", JSONObject().put("responseModalities", JSONArray().put("TEXT").put("IMAGE")))
                    val url = "$base/models/$GEMINI_IMAGE_MODEL:generateContent?key=${URLEncoder.encode(key.orEmpty(), "UTF-8")}"
                    val json = JSONObject(call("POST", url, emptyMap(), body.toString()))
                    val parts = json.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
                    var data: String? = null
                    if (parts != null) for (i in 0 until parts.length()) {
                        val inline = parts.getJSONObject(i).optJSONObject("inlineData") ?: parts.getJSONObject(i).optJSONObject("inline_data")
                        if (inline != null) data = inline.optString("data")
                    }
                    if (data.isNullOrBlank()) throw AiException("Gemini did not send back a picture.")
                    android.util.Base64.decode(data, android.util.Base64.DEFAULT)
                }
                ApiStyle.OPENAI_COMPATIBLE -> {
                    val body = JSONObject().put("model", OPENAI_IMAGE_MODEL).put("prompt", prompt).put("size", "1024x1024")
                    val json = JSONObject(call("POST", "$base/images/generations", openAiHeaders(key), body.toString()))
                    val item = json.optJSONArray("data")?.optJSONObject(0)
                    val b64 = item?.optString("b64_json").orEmpty()
                    if (b64.isBlank()) throw AiException("The service did not send back a picture. It may not offer image generation.")
                    android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                }
            }
        }

    private fun parseGeminiReply(body: String): String {
        val json = JSONObject(body)
        val parts = json.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
        val text = parts?.let { p -> (0 until p.length()).joinToString("") { p.getJSONObject(it).optString("text") } }.orEmpty()
        if (text.isBlank()) {
            val blocked = json.optJSONObject("promptFeedback")?.optString("blockReason", "").orEmpty()
            throw AiException(if (blocked.isNotBlank()) "Gemini blocked the request ($blocked)." else "The provider's reply was empty.")
        }
        return text
    }

    private fun call(method: String, url: String, headers: Map<String, String>, body: String?): String {
        // The URL can carry an API key (Gemini), so only the host and path are logged.
        val shown = url.substringBefore('?')
        val connection = try {
            URL(url).openConnection() as HttpURLConnection
        } catch (e: IOException) {
            throw AiException("The API URL is not valid.")
        }
        try {
            connection.requestMethod = method
            connection.connectTimeout = 20_000
            connection.readTimeout = 120_000
            headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty()
            DebugLogger.i(TAG, "$method $shown -> HTTP $code")
            if (code !in 200..299) throw AiException(describeError(code, text))
            return text
        } catch (e: AiException) {
            throw e
        } catch (e: IOException) {
            DebugLogger.e(TAG, "$method $shown failed", e)
            throw AiException("Could not reach ${URL(url).host}. Check your connection and the API URL.")
        } catch (e: org.json.JSONException) {
            DebugLogger.e(TAG, "$method $shown returned unexpected JSON", e)
            throw AiException("The provider's reply was not in the expected format.")
        } finally {
            connection.disconnect()
        }
    }

    private fun describeError(code: Int, body: String): String {
        val message = try {
            JSONObject(body).optJSONObject("error")?.optString("message")
        } catch (e: org.json.JSONException) {
            null
        }
        val detail = message?.takeIf { it.isNotBlank() } ?: body.take(200).ifBlank { "no details" }
        val hint = when (code) {
            401, 403 -> " Check that your API key is correct and allowed to use this."
            404 -> " Check the API URL and the model name."
            429 -> " Too many requests or the quota is used up."
            else -> ""
        }
        return "The provider answered HTTP $code: $detail.$hint"
    }

    private companion object {
        const val TAG = "AiClient"
        const val ANTHROPIC_VERSION = "2023-06-01"
        const val MAX_PAGES = 10
        const val GEMINI_IMAGE_MODEL = "gemini-2.5-flash-image"
        const val OPENAI_IMAGE_MODEL = "gpt-image-1"
    }
}
