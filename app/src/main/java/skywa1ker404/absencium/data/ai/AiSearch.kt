package skywa1ker404.absencium.data.ai

import me.rerere.common.platform.android.OkHttpPlatformHttpClient
import me.rerere.search.PlatformBingSearchClient
import me.rerere.search.SearchService
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Gives Absencium AI's search module (ported under `aihub/search`, AGPL-3.0) its HTTP client once; nothing else is shared with it. */
object AiSearch {
    @Volatile
    private var ready = false

    @Synchronized
    fun install() {
        if (ready) return
        val http = OkHttpPlatformHttpClient(
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build(),
        )
        SearchService.installPlatformHttpClient(http)
        SearchService.installBingSearchClient(PlatformBingSearchClient(http))
        SearchService.installAcceptLanguageProvider { java.util.Locale.getDefault().toLanguageTag() + ",en;q=0.8" }
        ready = true
    }
}
