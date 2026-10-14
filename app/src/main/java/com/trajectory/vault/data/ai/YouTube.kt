package com.trajectory.vault.data.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.trajectory.vault.logging.DebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** What the app shows about a video before it is transcribed. */
data class YouTubeVideo(
    val id: String,
    val url: String,
    val title: String,
    val channel: String,
    val thumbnailUrl: String,
)

/** Something went wrong reaching YouTube; [message] is written for the user. */
class YouTubeException(message: String) : Exception(message)

object YouTubeLinks {
    private val patterns = listOf(
        Regex("""(?:youtube\.com|youtube-nocookie\.com)/watch\?(?:[^#]*&)?v=([A-Za-z0-9_-]{11})"""),
        Regex("""youtu\.be/([A-Za-z0-9_-]{11})"""),
        Regex("""youtube(?:-nocookie)?\.com/(?:shorts|embed|live|v)/([A-Za-z0-9_-]{11})"""),
    )

    /** The 11 character video id inside a YouTube link, or null when the text is not a YouTube video link. */
    fun videoId(input: String): String? {
        val text = input.trim()
        if (text.isEmpty()) return null
        return patterns.firstNotNullOfOrNull { it.find(text)?.groupValues?.get(1) }
    }

    fun watchUrl(id: String) = "https://www.youtube.com/watch?v=$id"
}

/**
 * Reads a video's public details (YouTube's oEmbed endpoint, which is documented and needs no key) and its captions.
 * Captions are read through YouTube's own app endpoint, which is not an official API: it can change or refuse
 * requests, and videos without captions have nothing to read. Every failure comes back as a [YouTubeException]
 * with a reason the user can act on.
 */
class YouTubeClient {

    suspend fun details(url: String): YouTubeVideo = withContext(Dispatchers.IO) {
        val id = YouTubeLinks.videoId(url) ?: throw YouTubeException("That does not look like a YouTube video link.")
        val watch = YouTubeLinks.watchUrl(id)
        val endpoint = "https://www.youtube.com/oembed?format=json&url=" + URLEncoder.encode(watch, "UTF-8")
        val body = try {
            request("GET", endpoint, emptyMap(), null)
        } catch (e: YouTubeException) {
            if (e.message?.contains("HTTP 40") == true) {
                throw YouTubeException("YouTube could not find that video. It may be private, removed or not embeddable.")
            }
            throw e
        }
        try {
            val json = JSONObject(body)
            YouTubeVideo(
                id = id,
                url = watch,
                title = json.optString("title").ifBlank { "YouTube video" },
                channel = json.optString("author_name"),
                thumbnailUrl = json.optString("thumbnail_url"),
            )
        } catch (e: org.json.JSONException) {
            throw YouTubeException("YouTube sent back details in a format the app did not expect.")
        }
    }

    suspend fun thumbnail(url: String): Bitmap? = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext null
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            try {
                connection.inputStream.use { BitmapFactory.decodeStream(it) }
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Could not load the thumbnail", e)
            null
        }
    }

    /** The video's captions as plain text, without timestamps. Prefers a hand-written English track. */
    suspend fun transcript(videoId: String): String = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put(
                "context",
                JSONObject().put(
                    "client",
                    JSONObject().put("clientName", "ANDROID").put("clientVersion", ANDROID_CLIENT_VERSION).put("hl", "en"),
                ),
            )
            .put("videoId", videoId)
        val reply = request(
            "POST",
            "https://www.youtube.com/youtubei/v1/player?prettyPrint=false",
            mapOf(
                "Content-Type" to "application/json",
                "User-Agent" to "com.google.android.youtube/$ANDROID_CLIENT_VERSION (Linux; U; Android 14) gzip",
            ),
            payload.toString(),
        )
        val json = try {
            JSONObject(reply)
        } catch (e: org.json.JSONException) {
            throw YouTubeException("YouTube sent back an answer the app could not read.")
        }
        val playability = json.optJSONObject("playabilityStatus")
        val status = playability?.optString("status").orEmpty()
        if (status.isNotEmpty() && status != "OK") {
            val reason = playability?.optString("reason").orEmpty().ifBlank { status }
            throw YouTubeException("YouTube will not give out this video's captions: $reason")
        }
        val tracks = json.optJSONObject("captions")
            ?.optJSONObject("playerCaptionsTracklistRenderer")
            ?.optJSONArray("captionTracks")
        if (tracks == null || tracks.length() == 0) {
            throw YouTubeException("This video has no captions, so there is no transcript to read.")
        }
        val track = pickTrack(tracks)
        val baseUrl = track.optString("baseUrl")
        if (baseUrl.isBlank()) throw YouTubeException("YouTube did not give a caption address for this video.")
        val xml = request("GET", baseUrl, mapOf("User-Agent" to "Mozilla/5.0"), null)
        val text = parseCaptions(xml)
        if (text.isBlank()) throw YouTubeException("The captions came back empty. YouTube may be limiting caption downloads right now.")
        text
    }

    private fun pickTrack(tracks: JSONArray): JSONObject {
        val all = (0 until tracks.length()).map { tracks.getJSONObject(it) }
        fun english(t: JSONObject) = t.optString("languageCode").startsWith("en")
        fun manual(t: JSONObject) = t.optString("kind") != "asr"
        return all.firstOrNull { english(it) && manual(it) }
            ?: all.firstOrNull { manual(it) }
            ?: all.firstOrNull { english(it) }
            ?: all.first()
    }

    private fun request(method: String, url: String, headers: Map<String, String>, body: String?): String {
        val shown = url.substringBefore('?')
        val connection = try {
            URL(url).openConnection() as HttpURLConnection
        } catch (e: IOException) {
            throw YouTubeException("The address is not valid.")
        }
        try {
            connection.requestMethod = method
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty()
            DebugLogger.i(TAG, "$method $shown -> HTTP $code")
            if (code !in 200..299) throw YouTubeException("YouTube answered HTTP $code.")
            return text
        } catch (e: YouTubeException) {
            throw e
        } catch (e: IOException) {
            DebugLogger.e(TAG, "$method $shown failed", e)
            throw YouTubeException("Could not reach YouTube. Check your internet connection.")
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TAG = "YouTube"
        const val ANDROID_CLIENT_VERSION = "20.10.38"
    }
}

private val TextTag = Regex("""<text[^>]*>(.*?)</text>""", RegexOption.DOT_MATCHES_ALL)
private val ParagraphTag = Regex("""<p[^>]*>(.*?)</p>""", RegexOption.DOT_MATCHES_ALL)
private val AnyTag = Regex("""<[^>]+>""")
private val NumericEntity = Regex("""&#(x?[0-9a-fA-F]+);""")

/** Turns a YouTube caption file (either of its XML shapes) into one block of plain text. */
internal fun parseCaptions(xml: String): String {
    val lines = TextTag.findAll(xml).map { it.groupValues[1] }.toList()
        .ifEmpty { ParagraphTag.findAll(xml).map { it.groupValues[1] }.toList() }
    return lines
        .map { decodeEntities(decodeEntities(AnyTag.replace(it, ""))).replace('\n', ' ').trim() }
        .filter { it.isNotEmpty() }
        .joinToString(" ")
}

private fun decodeEntities(text: String): String {
    val numeric = NumericEntity.replace(text) { m ->
        val raw = m.groupValues[1]
        val code = if (raw.startsWith("x", ignoreCase = true)) raw.substring(1).toIntOrNull(16) else raw.toIntOrNull()
        if (code != null && code in 1..0x10FFFF) String(Character.toChars(code)) else m.value
    }
    return numeric
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
}
