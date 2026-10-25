package skywa1ker404.absencium.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import skywa1ker404.absencium.logging.DebugLogger
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** A picture for a note: its bytes, file type, and the credit line to show under it. */
class DiagramPicture(val bytes: ByteArray, val mimeType: String, val extension: String, val caption: String)

/**
 * Finds or draws the diagrams an AI answer asked for. Fetching searches Wikimedia Commons (freely licensed pictures, no key,
 * only the search words are sent) and keeps the licence in the caption. Drawing asks the user's own AI provider.
 * Both are optional and only run when the user switched them on.
 */
class DiagramService(private val ai: AiClient) {

    /** The best freely licensed picture for [query], or null when nothing fits or the network is unavailable. */
    suspend fun fetch(query: String): DiagramPicture? = withContext(Dispatchers.IO) {
        try {
            val url = "https://commons.wikimedia.org/w/api.php?action=query&format=json&generator=search&gsrnamespace=6&gsrlimit=12" +
                "&gsrsearch=${URLEncoder.encode(query, "UTF-8")}" +
                "&prop=imageinfo&iiprop=url|mime|extmetadata&iiurlwidth=1000"
            val pick = pickBest(get(url).toString(Charsets.UTF_8)) ?: return@withContext null
            val bytes = get(pick.imageUrl)
            if (bytes.isEmpty() || bytes.size > MAX_BYTES) return@withContext null
            val png = pick.mime != "image/jpeg"
            DiagramPicture(bytes, if (png) "image/png" else "image/jpeg", if (png) "png" else "jpg", pick.caption)
        } catch (e: IOException) {
            DebugLogger.e("Diagrams", "Could not fetch a diagram", e)
            null
        } catch (e: org.json.JSONException) {
            DebugLogger.e("Diagrams", "Unexpected reply while fetching a diagram", e)
            null
        }
    }

    /** Draws [description] as a clean labelled diagram on a white background. Throws [AiException] with a readable reason. */
    suspend fun draw(connection: AiConnection, description: String): DiagramPicture {
        val bytes = ai.generateImage(
            connection,
            "Draw a clean, flat, textbook-style educational diagram on a plain white background: $description. " +
                "Use clear labels in a legible font, thin dark outlines and a few soft colours. No watermark, no decoration.",
        )
        val png = bytes.size > 8 && bytes[1] == 'P'.code.toByte() && bytes[2] == 'N'.code.toByte()
        return DiagramPicture(bytes, if (png) "image/png" else "image/jpeg", if (png) "png" else "jpg", "Drawn by AI")
    }

    class Pick(val imageUrl: String, val mime: String, val caption: String)

    private fun get(url: String): ByteArray {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("User-Agent", "Absencium/1.0 (offline-first student app; GPL-3.0)")
            if (connection.responseCode !in 200..299) throw IOException("HTTP ${connection.responseCode}")
            return connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val MAX_BYTES = 6_000_000

        /** Chooses the first JPEG or PNG in a Commons search reply and builds its credit line. Pure, so it is tested. */
        fun pickBest(json: String): Pick? {
            val pages = JSONObject(json).optJSONObject("query")?.optJSONObject("pages") ?: return null
            val ordered = pages.keys().asSequence().map { pages.getJSONObject(it) }.sortedBy { it.optInt("index", Int.MAX_VALUE) }
            for (page in ordered) {
                val info = page.optJSONArray("imageinfo")?.optJSONObject(0) ?: continue
                val mime = info.optString("mime")
                // An SVG is delivered as a PNG thumbnail (its "thumburl"), so it is usable too.
                if (mime != "image/jpeg" && mime != "image/png" && mime != "image/svg+xml") continue
                val url = info.optString("thumburl").ifBlank { info.optString("url") }
                if (url.isBlank()) continue
                val meta = info.optJSONObject("extmetadata")
                val licence = meta?.optJSONObject("LicenseShortName")?.optString("value").orEmpty()
                val artist = meta?.optJSONObject("Artist")?.optString("value").orEmpty().replace(Regex("<[^>]*>"), "").trim().take(60)
                val title = page.optString("title").removePrefix("File:").substringBeforeLast('.')
                val credit = listOf(title, artist, licence).filter { it.isNotBlank() }.joinToString(", ")
                return Pick(url, mime, "Picture: $credit (Wikimedia Commons)")
            }
            return null
        }
    }
}
