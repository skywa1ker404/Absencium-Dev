package skywa1ker404.absencium.data.ai

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/** What one provider has been used for: requests and tokens in total, and tokens today. The counts come from the provider's own replies. */
data class ProviderUsage(val provider: AiProvider, val requests: Long, val tokensIn: Long, val tokensOut: Long, val tokensToday: Long)

/** Per-provider token statistics for chat answers, kept on this phone only (never uploaded). */
class AiUsage(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("absencium_ai_usage", Context.MODE_PRIVATE)
    private val _rows = MutableStateFlow(read())
    val rows: StateFlow<List<ProviderUsage>> = _rows.asStateFlow()

    @Synchronized
    fun record(provider: AiProvider, tokensIn: Long, tokensOut: Long, today: LocalDate = LocalDate.now()) {
        val name = provider.name
        val sameDay = prefs.getString("${name}_day", "") == today.toString()
        prefs.edit {
            putLong("${name}_req", prefs.getLong("${name}_req", 0) + 1)
            putLong("${name}_in", prefs.getLong("${name}_in", 0) + tokensIn)
            putLong("${name}_out", prefs.getLong("${name}_out", 0) + tokensOut)
            putString("${name}_day", today.toString())
            putLong("${name}_today", (if (sameDay) prefs.getLong("${name}_today", 0) else 0) + tokensIn + tokensOut)
        }
        _rows.value = read()
    }

    @Synchronized
    fun reset(provider: AiProvider? = null) {
        prefs.edit {
            AiProvider.entries.filter { provider == null || it == provider }.forEach { p ->
                listOf("req", "in", "out", "day", "today").forEach { remove("${p.name}_$it") }
            }
        }
        _rows.value = read()
    }

    private fun read(today: LocalDate = LocalDate.now()): List<ProviderUsage> = AiProvider.entries.mapNotNull { p ->
        val requests = prefs.getLong("${p.name}_req", 0)
        if (requests == 0L) null else ProviderUsage(
            p, requests, prefs.getLong("${p.name}_in", 0), prefs.getLong("${p.name}_out", 0),
            if (prefs.getString("${p.name}_day", "") == today.toString()) prefs.getLong("${p.name}_today", 0) else 0,
        )
    }
}
