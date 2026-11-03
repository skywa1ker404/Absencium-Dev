package skywa1ker404.absencium.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import me.rerere.search.SearchCommonOptions
import me.rerere.search.SearchService
import me.rerere.search.SearchServiceOptions

/**
 * Web search that works with any model: the question's words go to DuckDuckGo's plain HTML page (no key, no account), the top results
 * come back as titles, snippets and links, and they are put in front of the model as text. Only the search words leave the phone.
 */
object AiWebSearch {
    data class Hit(val title: String, val url: String, val snippet: String)

    private val link = Regex("<a[^>]*class=\"result__a\"[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>", RegexOption.DOT_MATCHES_ALL)
    private val snippet = Regex("<a[^>]*class=\"result__snippet\"[^>]*>(.*?)</a>", RegexOption.DOT_MATCHES_ALL)
    private val tags = Regex("<[^>]+>")

    fun parse(html: String, max: Int = 5): List<Hit> {
        val snippets = snippet.findAll(html).map { clean(it.groupValues[1]) }.toList()
        val hits = ArrayList<Hit>()
        link.findAll(html).forEachIndexed { i, m ->
            val url = target(m.groupValues[1]) ?: return@forEachIndexed
            val title = clean(m.groupValues[2])
            if (title.isNotEmpty()) hits += Hit(title, url, snippets.getOrElse(i) { "" })
        }
        return hits.distinctBy { it.url }.take(max)
    }

    /** The real address behind DuckDuckGo's redirect link, or null for an advert. */
    private fun target(href: String): String? {
        if (href.contains("y.js")) return null
        val encoded = Regex("uddg=([^&\"]+)").find(href)?.groupValues?.get(1)
        val url = if (encoded != null) URLDecoder.decode(encoded, "UTF-8") else href.let { if (it.startsWith("//")) "https:$it" else it }
        return url.takeIf { it.startsWith("http") }
    }

    private fun clean(html: String): String = tags.replace(html, "")
        .replace("&amp;", "&").replace("&quot;", "\"").replace("&#x27;", "'").replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">")
        .replace(Regex("\\s+"), " ").trim()

    /**
     * LastChat's keyless search first (it picks the right sources for news, weather, encyclopedia and general questions, and merges them),
     * then the plain DuckDuckGo page when that finds nothing. Only the search words leave the phone.
     */
    suspend fun search(query: String, max: Int = 5): List<Hit> {
        val rich = runCatching { keyless(query, max) }.getOrDefault(emptyList())
        return rich.ifEmpty { duckDuckGo(query, max) }
    }

    private suspend fun keyless(query: String, max: Int): List<Hit> {
        LastChatSearch.install()
        val result = SearchService.getService(SearchServiceOptions.KeylessOptions())
            .search(buildJsonObject { put("query", query.take(200)) }, SearchCommonOptions(resultSize = max), SearchServiceOptions.KeylessOptions())
            .getOrThrow()
        val hits = ArrayList<Hit>()
        result.answer?.takeIf { it.isNotBlank() }?.let { hits += Hit("Quick answer", "", it.take(600)) }
        result.items.forEach { hits += Hit(it.title, it.url, it.text.take(600)) }
        return hits.take(max)
    }

    private suspend fun duckDuckGo(query: String, max: Int): List<Hit> = withContext(Dispatchers.IO) {
        val connection = (URL("https://html.duckduckgo.com/html/").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 12_000
            doOutput = true
            setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        }
        try {
            connection.outputStream.use { it.write(("q=" + URLEncoder.encode(query.take(200), "UTF-8")).toByteArray()) }
            if (connection.responseCode != 200) emptyList() else parse(connection.inputStream.bufferedReader().use { it.readText() }, max)
        } finally {
            connection.disconnect()
        }
    }

    /** The results as text for the model's instruction; says plainly when nothing was found, so it does not invent sources. */
    fun block(hits: List<Hit>): String =
        if (hits.isEmpty()) "WEB SEARCH found nothing usable for this question. Say so and answer from what you know, marked as such.\n"
        else buildString {
            append("WEB RESULTS, fetched just now for this question. Base the answer on them where they help and cite as [1], [2]; do not invent other sources.\n")
            hits.forEachIndexed { i, h -> append("[${i + 1}] ${h.title}${if (h.url.isBlank()) "" else " (${h.url})"}: ${h.snippet}\n") }
        }
}
