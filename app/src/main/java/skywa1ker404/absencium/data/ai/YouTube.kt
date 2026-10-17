package skywa1ker404.absencium.data.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import skywa1ker404.absencium.logging.DebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
 * Reads a video's public details through YouTube's oEmbed endpoint, which is documented and needs no key. It does not
 * read captions or download anything from YouTube: the app has no unofficial YouTube access. Notes from a video come from
 * Gemini, which can read a public link itself, or from a transcript the user pastes. Every failure comes back as a
 * [YouTubeException] with a reason the user can act on.
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
    }
}
