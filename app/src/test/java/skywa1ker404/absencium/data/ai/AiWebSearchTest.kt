package skywa1ker404.absencium.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiWebSearchTest {
    private val html = """
        <div class="result"><a rel="nofollow" class="result__a" href="//duckduckgo.com/l/?uddg=https%3A%2F%2Fen.wikipedia.org%2Fwiki%2FCinchona&amp;rut=abc">Cinchona - <b>Wikipedia</b></a>
        <a class="result__snippet" href="x">Cinchona is a genus of &amp; flowering plants.</a></div>
        <div class="result"><a rel="nofollow" class="result__a" href="//duckduckgo.com/y.js?ad_provider=bing">An advert</a>
        <a class="result__snippet" href="x">Buy now</a></div>
        <div class="result"><a class="result__a" href="https://example.com/a">Second</a><a class="result__snippet" href="x">Other</a></div>
    """.trimIndent()

    @Test fun resultsAreReadAndAdvertsDropped() {
        val hits = AiWebSearch.parse(html)
        assertEquals(2, hits.size)
        assertEquals("https://en.wikipedia.org/wiki/Cinchona", hits[0].url)
        assertEquals("Cinchona - Wikipedia", hits[0].title)
        assertEquals("Cinchona is a genus of & flowering plants.", hits[0].snippet)
    }

    @Test fun anEmptySearchSaysSoInsteadOfInventingSources() {
        assertTrue(AiWebSearch.block(emptyList()).contains("found nothing"))
        assertTrue(AiWebSearch.block(AiWebSearch.parse(html)).contains("[1] Cinchona"))
    }

    @Test fun keysAreMatchedToTheirProvider() {
        assertEquals(AiProvider.OPENROUTER, providerForKey("sk-or-v1-abc"))
        assertEquals(AiProvider.ANTHROPIC, providerForKey("sk-ant-api03-x"))
        assertEquals(AiProvider.GEMINI, providerForKey("AIzaSyA"))
        assertEquals(AiProvider.GROQ, providerForKey("gsk_abc"))
        assertEquals(AiProvider.OPENAI, providerForKey("sk-proj-abc"))
        assertNull(providerForKey("hello"))
    }
}
