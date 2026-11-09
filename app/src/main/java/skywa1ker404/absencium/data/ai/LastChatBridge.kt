package skywa1ker404.absencium.data.ai

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import me.rerere.ai.core.MessageRole
import me.rerere.ai.provider.Modality
import me.rerere.ai.provider.Model
import me.rerere.ai.provider.ProviderManager
import me.rerere.ai.provider.ProviderSetting
import me.rerere.ai.provider.TextGenerationParams
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.common.platform.android.AndroidPlatformJwtSigner
import me.rerere.common.platform.android.AndroidPlatformMediaEncoder
import me.rerere.common.platform.android.OkHttpPlatformHttpClient
import okhttp3.OkHttpClient
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi

/**
 * Sends Absencium chat requests through LastChat's provider layer (ported under `lastchat/`, AGPL-3.0, see THIRD_PARTY_NOTICES.md).
 * Only the wire work is theirs: request building, reasoning, token counts and reply parsing. Keys, failover, response modes,
 * quiz, links, actions and memory stay Absencium's own, and the key arrives here already chosen by [ApiKeyVault].
 */
@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
class LastChatBridge {

    private val manager = ProviderManager(
        platformHttpClient = OkHttpPlatformHttpClient(
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .writeTimeout(180, TimeUnit.SECONDS)
                .build(),
        ),
        platformMediaEncoder = AndroidPlatformMediaEncoder(),
        platformJwtSigner = AndroidPlatformJwtSigner(),
    )

    /** One answer from [connection]'s model using [key] (null for providers that need none). Failures come back as [AiException] with the HTTP code. */
    suspend fun chat(connection: AiConnection, key: String?, system: String, turns: List<ChatTurn>, options: ChatOptions): ChatReply =
        withContext(Dispatchers.IO) {
            val model = connection.model.trim().removePrefix("models/")
            if (model.isEmpty()) throw AiException("Choose a model in Settings, then Absencium AI first.")
            val messages = messagesOf(system, turns)
            val params = paramsOf(model, turns, options)
            val setting = settingOf(connection, key.orEmpty())
            var attempt = 0
            while (true) {
                try {
                    val chunk = manager.getProviderByType(setting).generateText(setting, messages, params)
                    val message = chunk.choices.firstNotNullOfOrNull { it.message ?: it.delta }
                    val text = message?.toText().orEmpty()
                    if (text.isBlank()) throw AiException("The model sent no text. Try again or pick another model.")
                    val usage = chunk.usage ?: message?.usage
                    return@withContext ChatReply(text, tokensIn = usage?.promptTokens?.toLong() ?: 0L, tokensOut = usage?.completionTokens?.toLong() ?: 0L)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val failure = failureOf(e)
                    if (!failure.temporary || attempt >= MAX_RETRIES) throw failure
                    attempt++
                    delay(2_000L * attempt)
                }
            }
            @Suppress("UNREACHABLE_CODE")
            error("unreachable")
        }

    internal fun messagesOf(system: String, turns: List<ChatTurn>): List<UIMessage> = buildList {
        add(UIMessage(role = MessageRole.SYSTEM, parts = listOf(UIMessagePart.Text(system))))
        turns.forEach { turn ->
            val parts = ArrayList<UIMessagePart>()
            turn.images.forEach { file ->
                val url = "data:${file.mimeType};base64,${file.base64}"
                parts += when {
                    file.isPdf -> UIMessagePart.Document(url, "document.pdf", file.mimeType)
                    file.isAudio -> UIMessagePart.Audio(url)
                    else -> UIMessagePart.Image(url)
                }
            }
            parts += UIMessagePart.Text(turn.text)
            add(UIMessage(role = if (turn.role == ChatRole.USER) MessageRole.USER else MessageRole.ASSISTANT, parts = parts))
        }
    }

    private fun paramsOf(model: String, turns: List<ChatTurn>, options: ChatOptions): TextGenerationParams {
        val sees = turns.any { t -> t.images.any { !it.isPdf && !it.isAudio } }
        val hears = turns.any { t -> t.images.any { it.isAudio } }
        val input = buildList {
            add(Modality.TEXT)
            if (sees) add(Modality.IMAGE)
            if (hears) add(Modality.AUDIO)
        }
        return TextGenerationParams(
            model = Model(modelId = model, displayName = model, inputModalities = input),
            temperature = options.temperature?.toFloat(),
            maxTokens = options.maxTokens,
            thinkingBudget = if (options.thinking) THINKING_BUDGET else null,
        )
    }

    private fun settingOf(connection: AiConnection, key: String): ProviderSetting {
        val base = connection.baseUrl.trim().trimEnd('/')
        return when (connection.style) {
            ApiStyle.OPENAI_COMPATIBLE -> ProviderSetting.OpenAI(name = connection.provider.label, apiKey = key, baseUrl = base)
            ApiStyle.ANTHROPIC -> ProviderSetting.Claude(name = connection.provider.label, apiKey = key, baseUrl = base)
            ApiStyle.GEMINI -> ProviderSetting.Google(name = connection.provider.label, apiKey = key, baseUrl = base)
        }
    }

    internal fun failureOf(e: Exception): AiException {
        if (e is AiException) return e
        val text = e.message.orEmpty()
        val match = HTTP.find(text)
        if (match != null) {
            val code = match.groupValues[1].toInt()
            return AiException(describe(code, match.groupValues[2]), code)
        }
        if (e is IOException) return AiException("Could not reach the provider. Check the connection and the API URL.")
        return AiException(text.ifBlank { "The AI request failed." })
    }

    private fun describe(code: Int, body: String): String {
        val detail = Regex("\"message\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").find(body)?.groupValues?.get(1)
            ?.replace("\\n", " ")?.replace("\\\"", "\"")
            ?: body.take(200).ifBlank { "no details" }
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

    internal companion object {
        const val MAX_RETRIES = 2
        const val THINKING_BUDGET = 8_192
        val HTTP = Regex("Failed to get response: (\\d{3}) ?(.*)", RegexOption.DOT_MATCHES_ALL)
    }
}
