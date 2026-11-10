package skywa1ker404.absencium.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import me.rerere.rikkahub.RouteActivity
import skywa1ker404.absencium.data.ai.AiTargetType

/** One request to show the chat. [id] changes on every request so a second request restarts it. */
data class AiRequest(val id: Int, val destination: String?)

/** A page of the app to show after the chat closes: where an AI action's result lives. */
data class AiOpen(val type: AiTargetType, val id: Long)

/**
 * The one door into Absencium AI, the embedded chat. Every AI button in the app goes through here. While the app's window is showing,
 * the chat opens as a layer of that same window (see [AiLayer]); only when there is no such window (a notification, the share sheet)
 * does it fall back to its own activity.
 */
object AbsenciumAi {
    private var counter = 0
    private val _request = MutableStateFlow<AiRequest?>(null)

    /** The chat to show, or null when it is closed. */
    val request: StateFlow<AiRequest?> = _request

    /** True while the app's window is there to host the chat. */
    @Volatile var hostActive: Boolean = false

    /** Asked for by the chat's settings gear: close the chat and show the app's own AI settings page. */
    val appSettingsRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** A page the chat asked the app to show; the app navigates there once, then clears it. */
    val pendingOpen = MutableStateFlow<AiOpen?>(null)

    /** The main screen's tab to show next (1 attendance, 2 vault); the main screen takes it once. */
    val pendingTab = MutableStateFlow<Int?>(null)

    /** From the AI actions folder: close the chat and show the entry or page an action made or changed. */
    fun openTarget(context: Context, type: AiTargetType, id: Long) {
        pendingOpen.value = AiOpen(type, id)
        if (hostActive) {
            _request.value = null
            return
        }
        val intent = Intent(context, skywa1ker404.absencium.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** Opens the chat, or with [settings] straight on its settings (providers, models, assistants). */
    fun open(context: Context, settings: Boolean = false, destination: String? = null) {
        val page = destination ?: if (settings) "settings" else null
        if (hostActive) {
            _request.value = AiRequest(++counter, page)
            return
        }
        val intent = Intent(context, RouteActivity::class.java)
        if (settings) intent.putExtra(RouteActivity.EXTRA_OPEN_SETTINGS, true)
        if (destination != null) intent.putExtra(RouteActivity.EXTRA_SETTINGS_DESTINATION, destination)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun close() {
        _request.value = null
    }

    /** From the chat: leave it and open the app's AI settings page. Returns false when the chat is not in the app's window. */
    fun showAppSettings(): Boolean {
        if (!hostActive || _request.value == null) return false
        _request.value = null
        appSettingsRequests.tryEmit(Unit)
        return true
    }
}
