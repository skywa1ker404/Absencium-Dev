package skywa1ker404.absencium.data.ai

import skywa1ker404.absencium.data.security.ApiKeyVault
import skywa1ker404.absencium.logging.DebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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

/** One turn of a conversation sent to the model. A turn may carry pictures or PDFs. */
data class ChatTurn(val role: ChatRole, val text: String, val images: List<AiImage> = emptyList())

/** A web page the answer was grounded on (only when web search was on). */
data class ChatSource(val title: String, val url: String)

/** The answer of [AiClient.chat]: the text and the sources it used, if any. */
/** Per-request switches of a chat: the internet, extra reasoning, and the model's own limits. */
data class ChatOptions(val webSearch: Boolean = false, val thinking: Boolean = false, val temperature: Double? = null, val maxTokens: Int? = null)

data class ChatReply(
    val text: String,
    val sources: List<ChatSource> = emptyList(),
    /** The model that really answered, which is another one when a limit sent the question to a saved key or provider that still had room. */
    val model: String = "",
    val tokensIn: Long = 0,
    val tokensOut: Long = 0,
)

/**
 * What a screen shows inline when a request failed. The reason itself is spelled out once, in the card at the bottom of the screen
 * (see AiErrorHost), so it is not repeated next to the button.
 */
const val AI_FAILED_INLINE = "The AI request failed. The reason is shown at the bottom of the screen."

/** Something went wrong talking to the provider; [message] is written for the user. */
class AiException(message: String, val httpCode: Int = 0, val maxImages: Int? = null) : Exception(message) {
    /** True for answers that usually pass by themselves: the provider is busy or briefly down. */
    val temporary: Boolean get() = httpCode == 500 || httpCode == 502 || httpCode == 503 || httpCode == 504

    /** True when the key ran out of quota or is rate limited, so another saved key or provider may still answer. */
    val limitReached: Boolean get() = httpCode == 429 || (httpCode == 403 && message.orEmpty().contains("quota", ignoreCase = true))
}

/**
 * A small HTTP client for the three request shapes Ask AI supports: OpenAI-compatible chat
 * completions, Anthropic messages and Google Gemini generateContent. Model lists use each provider's
 * documented list endpoint. Keys come from the encrypted [ApiKeyVault] and are never logged.
 */
class AiClient(private val keyVault: ApiKeyVault) {

    /** Every request and its answer is kept here for the AI Hub; set once by the app container. */
    var history: AiHistory? = null

    /** Per-provider token statistics; set once by the app container. */
    var usage: AiUsage? = null

    /** Other usable providers (each with its saved model), tried by [chat] when every saved key of the chosen one hit its limit. */
    var fallbacks: () -> List<AiConnection> = { emptyList() }

    private val _failures = MutableSharedFlow<String>(extraBufferCapacity = 4, onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST)

    /**
     * One short line for every request that failed, so the app can show it wherever the student is, even after the
     * screen that asked has been closed.
     */
    val failures: SharedFlow<String> = _failures.asSharedFlow()

    private val imageLimits = HashMap<String, Int>()

    /** How many pictures one request to [connection]'s model may carry: [default], or less once the provider has said so. */
    fun imageLimit(connection: AiConnection, default: Int): Int = minOf(default, imageLimits["${connection.style}|${connection.model}"] ?: default)

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
        /** Called with 0..1 while the request body is being uploaded, so a big file can show its progress. */
        onUpload: ((Float) -> Unit)? = null,
        /** True when the caller can send fewer pictures at a time if the model says it takes fewer; the failure is then not announced. */
        adaptiveImages: Boolean = false,
        /** False for a chat message: the chat keeps its own transcript, so it is not repeated in the list of prompts the app made. */
        record: Boolean = true,
    ): String {
        try {
            val reply = generateRaw(connection, prompt, images, videoUrl, maxTokens, onUpload)
            if (record) history?.add(connection.model, prompt, reply, null, images.size)
            return reply
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            if (record) history?.add(connection.model, prompt, null, e.message ?: e.javaClass.simpleName, images.size)
            val limit = (e as? AiException)?.maxImages
            if (adaptiveImages && limit != null && images.size > limit) {
                // The model takes fewer pictures than were sent. The limit is remembered and the caller sends smaller batches,
                // so this is not a failure worth a card.
                imageLimits["${connection.style}|${connection.model}"] = limit
            } else {
                _failures.tryEmit(e.message ?: "The AI request failed.")
            }
            throw e
        }
    }

    private suspend fun generateRaw(
        connection: AiConnection,
        prompt: String,
        images: List<AiImage>,
        videoUrl: String?,
        maxTokens: Int?,
        onUpload: ((Float) -> Unit)? = null,
    ): String =
        withContext(Dispatchers.IO) {
            val key = keyFor(connection)
            val base = connection.baseUrl.trim().trimEnd('/')
            val model = connection.model.trim()
            if (model.isEmpty()) throw AiException("Choose a model in Settings, AI & Integrations first.")
            when (connection.style) {
                ApiStyle.OPENAI_COMPATIBLE -> {
                    val content = JSONArray().put(JSONObject().put("type", "text").put("text", prompt))
                    if (base.contains("groq", ignoreCase = true)) {
                        if (images.any { it.isPdf }) throw AiException("Groq cannot read PDF files. Pick a Google Gemini or Claude model in Settings, AI & Integrations for PDFs.")
                        if (images.size > GROQ_MAX_IMAGES) throw AiException("Groq takes at most $GROQ_MAX_IMAGES pictures in one request.")
                    }
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
                    val reply = callRetrying("POST", "$base/chat/completions", openAiHeaders(key), body.toString(), onUpload)
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
                    val reply = callRetrying("POST", "$base/messages", anthropicHeaders(key), body.toString(), onUpload)
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
                    // No output cap for Gemini: its hidden thinking is counted inside maxOutputTokens, so a cap meant for the visible
                    // answer could be used up by the thinking and leave an empty reply.
                    val body = JSONObject().put("contents", JSONArray().put(JSONObject().put("parts", parts)))
                    val id = model.removePrefix("models/")
                    val url = "$base/models/$id:generateContent?key=${URLEncoder.encode(key.orEmpty(), "UTF-8")}"
                    var result = parseGemini(callRetrying("POST", url, emptyMap(), body.toString(), onUpload))
                    // An empty answer with a normal stop is a known hiccup of the flash models: once more usually works.
                    if (result.text.isBlank() && result.retryable) {
                        DebugLogger.w(TAG, "Gemini returned no text (${result.summary()}), asking once more")
                        delay(1_500)
                        result = parseGemini(callRetrying("POST", url, emptyMap(), body.toString(), onUpload))
                    }
                    if (result.text.isBlank()) throw AiException(result.problem())
                    result.text
                }
            }
        }

    /**
     * A real conversation: a system prompt kept apart from the turns, every earlier turn in its own role (so the model sees the chat the
     * way a chat app would send it), and optionally web search with sources (Gemini only; other styles ignore it). Failures are announced
     * by the bar at the bottom of the screen, like every other request.
     */
    suspend fun chat(connection: AiConnection, system: String, turns: List<ChatTurn>, options: ChatOptions = ChatOptions()): ChatReply {
        try {
            return withFailover(connection, system, turns, options)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            _failures.tryEmit(e.message ?: "The AI request failed.")
            throw e
        }
    }

    /**
     * Tries every saved key of the chosen provider in turn, and when all of them have hit their limit the other providers that have a key
     * (each with its saved model). Only a rate or quota limit moves on; any other failure is the real answer.
     */
    private suspend fun withFailover(connection: AiConnection, system: String, turns: List<ChatTurn>, options: ChatOptions): ChatReply {
        if (connection.provider.keyRequired && keyVault.keyCount(connection.provider) == 0) keyFor(connection)
        var last: AiException? = null
        val candidates = listOf(connection) + fallbacks().filter { it.provider != connection.provider }
        for (candidate in candidates) {
            val keys: List<String?> = if (candidate.provider.keyRequired) keyVault.usableKeys(candidate.provider) else listOf(keyVault.read(candidate.provider))
            if (keys.isEmpty()) {
                last = last ?: AiException("Every saved ${candidate.provider.label} key has reached its limit. Try again later or add another key.", 429)
                continue
            }
            for (key in keys) {
                try {
                    val reply = chatRaw(candidate, system, turns, options, key)
                    usage?.record(candidate.provider, reply.tokensIn, reply.tokensOut)
                    return reply.copy(model = candidate.model)
                } catch (e: AiException) {
                    if (!e.limitReached) throw e
                    last = e
                    if (key != null) keyVault.markLimited(key)
                    DebugLogger.w(TAG, "${candidate.provider.label} key hit its limit, trying the next one")
                }
            }
        }
        throw last ?: AiException("No AI key is available.")
    }

    private suspend fun chatRaw(connection: AiConnection, system: String, turns: List<ChatTurn>, options: ChatOptions, key: String?): ChatReply =
        withContext(Dispatchers.IO) {
            val base = connection.baseUrl.trim().trimEnd('/')
            val model = connection.model.trim()
            if (model.isEmpty()) throw AiException("Choose a model in Settings, AI & Integrations first.")
            if (connection.style != ApiStyle.GEMINI && turns.any { t -> t.images.any { it.isAudio || it.mimeType.startsWith("video/") } }) {
                throw AiException("Recordings and videos need a Google Gemini model. Pick one for this chat, or remove the file.")
            }
            when (connection.style) {
                ApiStyle.OPENAI_COMPATIBLE -> {
                    val messages = JSONArray().put(JSONObject().put("role", "system").put("content", system))
                    turns.forEach { turn ->
                        val role = if (turn.role == ChatRole.USER) "user" else "assistant"
                        if (turn.images.isEmpty()) {
                            messages.put(JSONObject().put("role", role).put("content", turn.text))
                        } else {
                            if (base.contains("groq", ignoreCase = true)) {
                                if (turn.images.any { it.isPdf }) throw AiException("Groq cannot read PDF files. Pick a Google Gemini or Claude model for PDFs.")
                                if (turn.images.size > GROQ_MAX_IMAGES) throw AiException("Groq takes at most $GROQ_MAX_IMAGES pictures in one request.")
                            }
                            val content = JSONArray().put(JSONObject().put("type", "text").put("text", turn.text))
                            turn.images.forEach {
                                content.put(
                                    if (it.isPdf) {
                                        JSONObject().put("type", "file").put(
                                            "file",
                                            JSONObject().put("filename", "document.pdf").put("file_data", "data:application/pdf;base64,${it.base64}"),
                                        )
                                    } else {
                                        JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", "data:${it.mimeType};base64,${it.base64}"))
                                    },
                                )
                            }
                            messages.put(JSONObject().put("role", role).put("content", content))
                        }
                    }
                    val body = JSONObject().put("model", model).put("messages", messages)
                    options.temperature?.let { body.put("temperature", it) }
                    options.maxTokens?.let { body.put("max_tokens", it) }
                    val openAi = base.contains("api.openai.com", ignoreCase = true)
                    // Extra reasoning only where the service takes it (OpenAI's reasoning models); others would refuse the field.
                    if (options.thinking && openAi && Regex("^(o\\d|gpt-5)").containsMatchIn(model)) body.put("reasoning_effort", "medium")
                    // OpenRouter searches the web through its web plugin and returns the pages as citations.
                    if (options.webSearch && base.contains("openrouter", ignoreCase = true)) body.put("plugins", JSONArray().put(JSONObject().put("id", "web")))
                    val raw = callRetrying("POST", "$base/chat/completions", openAiHeaders(key), body.toString(), null)
                    ChatReply(parseOpenAiReply(raw), openAiSources(raw), tokensIn = usageOf(raw, "usage", "prompt_tokens"), tokensOut = usageOf(raw, "usage", "completion_tokens"))
                }

                ApiStyle.ANTHROPIC -> {
                    val messages = JSONArray()
                    turns.forEach { turn ->
                        val content = JSONArray()
                        turn.images.forEach {
                            content.put(
                                JSONObject().put("type", if (it.isPdf) "document" else "image").put(
                                    "source",
                                    JSONObject().put("type", "base64").put("media_type", it.mimeType).put("data", it.base64),
                                ),
                            )
                        }
                        content.put(JSONObject().put("type", "text").put("text", turn.text))
                        messages.put(JSONObject().put("role", if (turn.role == ChatRole.USER) "user" else "assistant").put("content", content))
                    }
                    val limit = options.maxTokens ?: if (options.thinking) 24_000 else 16_000
                    val body = JSONObject().put("model", model).put("max_tokens", limit).put("system", system).put("messages", messages)
                    if (options.thinking) {
                        body.put("thinking", JSONObject().put("type", "enabled").put("budget_tokens", 6_000))
                    } else {
                        options.temperature?.let { body.put("temperature", it) }
                    }
                    if (options.webSearch) {
                        body.put("tools", JSONArray().put(JSONObject().put("type", "web_search_20250305").put("name", "web_search").put("max_uses", 5)))
                    }
                    val raw = callRetrying("POST", "$base/messages", anthropicHeaders(key), body.toString(), null)
                    ChatReply(parseAnthropicReply(raw), anthropicSources(raw), tokensIn = usageOf(raw, "usage", "input_tokens"), tokensOut = usageOf(raw, "usage", "output_tokens"))
                }

                ApiStyle.GEMINI -> {
                    val contents = JSONArray()
                    turns.forEach { turn ->
                        val parts = JSONArray().put(JSONObject().put("text", turn.text))
                        turn.images.forEach {
                            parts.put(JSONObject().put("inline_data", JSONObject().put("mime_type", it.mimeType).put("data", it.base64)))
                        }
                        contents.put(JSONObject().put("role", if (turn.role == ChatRole.USER) "user" else "model").put("parts", parts))
                    }
                    val body = JSONObject()
                        .put("system_instruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
                        .put("contents", contents)
                    if (options.webSearch) body.put("tools", JSONArray().put(JSONObject().put("google_search", JSONObject())))
                    val config = JSONObject().put("maxOutputTokens", options.maxTokens ?: 32_768)
                    options.temperature?.let { config.put("temperature", it) }
                    if (options.thinking) {
                        // Gemini 3 takes a level, Gemini 2.5 a token budget; other models are left on their own setting.
                        when {
                            model.contains("gemini-3", ignoreCase = true) -> config.put("thinkingConfig", JSONObject().put("thinkingLevel", "medium"))
                            model.contains("gemini-2.5", ignoreCase = true) -> config.put("thinkingConfig", JSONObject().put("thinkingBudget", 8_192))
                        }
                    }
                    body.put("generationConfig", config)
                    val id = model.removePrefix("models/")
                    val url = "$base/models/$id:generateContent?key=${URLEncoder.encode(key.orEmpty(), "UTF-8")}"
                    var raw = callRetrying("POST", url, emptyMap(), body.toString(), null)
                    var result = parseGemini(raw)
                    if (result.text.isBlank() && result.retryable) {
                        DebugLogger.w(TAG, "Gemini returned no text (${result.summary()}), asking once more")
                        delay(1_500)
                        raw = callRetrying("POST", url, emptyMap(), body.toString(), null)
                        result = parseGemini(raw)
                    }
                    if (result.text.isBlank()) throw AiException(result.problem())
                    ChatReply(
                        result.text, geminiSources(raw),
                        tokensIn = usageOf(raw, "usageMetadata", "promptTokenCount"),
                        tokensOut = usageOf(raw, "usageMetadata", "candidatesTokenCount") + usageOf(raw, "usageMetadata", "thoughtsTokenCount"),
                    )
                }
            }
        }

    private fun usageOf(body: String, group: String, field: String): Long = try {
        JSONObject(body).optJSONObject(group)?.optLong(field, 0L) ?: 0L
    } catch (e: Exception) {
        0L
    }

    private fun openAiSources(body: String): List<ChatSource> = try {
        val notes = JSONObject(body).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optJSONArray("annotations")
        if (notes == null) emptyList() else {
            (0 until notes.length()).mapNotNull { i ->
                val cite = notes.optJSONObject(i)?.optJSONObject("url_citation") ?: return@mapNotNull null
                val link = cite.optString("url")
                if (link.isBlank()) null else ChatSource(cite.optString("title").ifBlank { link }, link)
            }.distinctBy { it.url }
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun anthropicSources(body: String): List<ChatSource> = try {
        val content = JSONObject(body).optJSONArray("content")
        val found = ArrayList<ChatSource>()
        if (content != null) {
            for (i in 0 until content.length()) {
                val block = content.optJSONObject(i) ?: continue
                if (block.optString("type") != "web_search_tool_result") continue
                val results = block.optJSONArray("content") ?: continue
                for (k in 0 until results.length()) {
                    val r = results.optJSONObject(k) ?: continue
                    val link = r.optString("url")
                    if (link.isNotBlank()) found += ChatSource(r.optString("title").ifBlank { link }, link)
                }
            }
        }
        found.distinctBy { it.url }
    } catch (e: Exception) {
        emptyList()
    }

    /** The pages Gemini's search used, from the grounding data of an answer. Titles are the site names; links are Google's redirects. */
    private fun geminiSources(body: String): List<ChatSource> = try {
        val chunks = JSONObject(body).optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("groundingMetadata")?.optJSONArray("groundingChunks")
        if (chunks == null) emptyList() else {
            (0 until chunks.length()).mapNotNull { i ->
                val web = chunks.optJSONObject(i)?.optJSONObject("web") ?: return@mapNotNull null
                val link = web.optString("uri")
                if (link.isBlank()) null else ChatSource(web.optString("title").ifBlank { link }, link)
            }.distinctBy { it.url }
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun keyFor(connection: AiConnection): String? {
        val key = keyVault.read(connection.provider)
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

    /** What Gemini sent back: the visible text and why it stopped. */
    private class GeminiResult(
        val text: String,
        val finish: String,
        val blocked: String,
        val thoughtTokens: Int,
    ) {
        val retryable: Boolean get() = blocked.isBlank() && (finish.isBlank() || finish == "STOP" || finish == "OTHER")
        fun summary(): String = "finish=${finish.ifBlank { "none" }}, blocked=${blocked.ifBlank { "no" }}, thinking tokens=$thoughtTokens"
        fun problem(): String = when {
            blocked.isNotBlank() -> "Gemini blocked the request ($blocked). Try another model or rephrase."
            finish == "MAX_TOKENS" -> "Gemini ran out of room before it wrote an answer (it used $thoughtTokens tokens thinking). Try a smaller input or another model."
            finish in setOf("SAFETY", "PROHIBITED_CONTENT", "BLOCKLIST", "SPII", "RECITATION", "IMAGE_SAFETY") ->
                "Gemini's safety filter stopped the answer ($finish). Try other wording or another model."
            else -> "Gemini sent back no text (${summary()}). Try again or pick another model."
        }
    }

    private fun parseGemini(body: String): GeminiResult {
        val json = JSONObject(body)
        val candidate = json.optJSONArray("candidates")?.optJSONObject(0)
        val parts = candidate?.optJSONObject("content")?.optJSONArray("parts")
        // Thought summaries are marked "thought": true; only the real answer is kept.
        val text = parts?.let { p ->
            (0 until p.length()).joinToString("") { i ->
                val part = p.getJSONObject(i)
                if (part.optBoolean("thought", false)) "" else part.optString("text")
            }
        }.orEmpty()
        val finish = candidate?.optString("finishReason", "").orEmpty()
        val blocked = json.optJSONObject("promptFeedback")?.optString("blockReason", "").orEmpty()
        val thoughts = json.optJSONObject("usageMetadata")?.optInt("thoughtsTokenCount", 0) ?: 0
        return GeminiResult(text, finish, blocked, thoughts)
    }

    /** A POST that tries again, after a pause, when the provider says it is busy (HTTP 500, 502, 503 or 504). */
    private suspend fun callRetrying(method: String, url: String, headers: Map<String, String>, body: String?, onUpload: ((Float) -> Unit)?): String {
        // A big upload is not repeated more than once: sending it again costs minutes on a slow connection.
        val retries = if (body != null && body.length > 1_000_000) 1 else 2
        var attempt = 0
        while (true) {
            try {
                return call(method, url, headers, body, onUpload)
            } catch (e: AiException) {
                if (!e.temporary || attempt >= retries) throw e
                attempt++
                DebugLogger.w(TAG, "$method ${url.substringBefore('?')} busy (HTTP ${e.httpCode}), trying again ($attempt of $retries)")
                delay(2_000L * attempt)
            }
        }
    }

    private fun call(method: String, url: String, headers: Map<String, String>, body: String?, onUpload: ((Float) -> Unit)? = null): String {
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
            val bytes = body?.toByteArray(Charsets.UTF_8)
            // After a big upload the model may think for a while (a long PDF), so the wait for the answer is longer then.
            connection.readTimeout = if (bytes != null && bytes.size > 1_000_000) 600_000 else 300_000
            headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
            if (bytes != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                if (onUpload != null) {
                    // Streamed in pieces so the progress is real; without this the whole body is held back until it is all written.
                    connection.setFixedLengthStreamingMode(bytes.size)
                    connection.outputStream.use { out ->
                        var sent = 0
                        var reported = 0f
                        while (sent < bytes.size) {
                            val n = minOf(UPLOAD_CHUNK, bytes.size - sent)
                            out.write(bytes, sent, n)
                            sent += n
                            // Reported in whole percents: a screen redrawn for every 32 KB piece would stutter during a big upload.
                            val fraction = sent.toFloat() / bytes.size
                            if (fraction - reported >= 0.01f || sent == bytes.size) {
                                reported = fraction
                                onUpload(fraction)
                            }
                        }
                    }
                } else {
                    connection.outputStream.use { it.write(bytes) }
                }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty()
            DebugLogger.i(TAG, "$method $shown -> HTTP $code")
            if (code !in 200..299) {
                val reason = describeError(code, text)
                // The provider's own explanation (never the key) goes to the debug log, so a 400 can be told apart from another 400.
                DebugLogger.w(TAG, "$method $shown rejected: $reason")
                val maxImages = Regex("up to (\\d+) images?", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.toIntOrNull()
                throw AiException(reason, code, maxImages)
            }
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
            400 -> " The provider refused this request. If a file or photo was attached, the chosen model may not accept it or it may be too big."
            404 -> " Check the API URL and the model name."
            408 -> " The provider timed out. Try again."
            413 -> " The upload is too big for this provider."
            429 -> " Too many requests or the quota is used up."
            500, 502, 503, 504 -> " The provider is busy or down right now, and it was tried again automatically. Wait a minute and retry, or pick another model."
            else -> ""
        }
        return "The provider answered HTTP $code: $detail.$hint"
    }

    private companion object {
        const val TAG = "AiClient"
        const val ANTHROPIC_VERSION = "2023-06-01"
        const val MAX_PAGES = 10
        const val UPLOAD_CHUNK = 32 * 1024
        const val GROQ_MAX_IMAGES = 5
        const val GEMINI_IMAGE_MODEL = "gemini-2.5-flash-image"
        const val OPENAI_IMAGE_MODEL = "gpt-image-1"
    }
}
