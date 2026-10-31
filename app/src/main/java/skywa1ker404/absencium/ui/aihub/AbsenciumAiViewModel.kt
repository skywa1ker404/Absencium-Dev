package skywa1ker404.absencium.ui.aihub

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import skywa1ker404.absencium.AppContainer
import skywa1ker404.absencium.data.ai.ActionResult
import skywa1ker404.absencium.data.ai.AiAction
import skywa1ker404.absencium.data.ai.AiActionParser
import skywa1ker404.absencium.data.ai.AiChat
import skywa1ker404.absencium.data.ai.AiChatPrompt
import skywa1ker404.absencium.data.ai.AiConnection
import skywa1ker404.absencium.data.ai.AiHistoryEntry
import skywa1ker404.absencium.data.ai.AiProvider
import skywa1ker404.absencium.data.ai.AiScope
import skywa1ker404.absencium.data.ai.ChatMessage
import skywa1ker404.absencium.data.ai.ChatRole
import skywa1ker404.absencium.data.settings.AppSettings
import skywa1ker404.absencium.logging.DebugLogger

/** The state of Absencium AI: the open chat, the list of chats, and the one answer being waited for. */
class AbsenciumAiViewModel(private val container: AppContainer) : ViewModel() {
    private val store = container.aiChats

    val settings: StateFlow<AppSettings?> = container.settingsState
    val chats: StateFlow<List<AiChat>> = store.chats
    val allowed: StateFlow<Set<AiScope>> = container.aiPermissions.allowed

    /** Prompts the app itself made (notes, syllabus, homework). Shown read-only in the sidebar. */
    val appPrompts: StateFlow<List<AiHistoryEntry>> = container.aiHistory.entries

    private val _draft = MutableStateFlow(store.blank())
    private val _currentId = MutableStateFlow(_draft.value.id)

    /** The open chat: a saved one, or the empty draft that is saved when its first message is sent. */
    val current: StateFlow<AiChat> = combine(store.chats, _currentId, _draft) { chats, id, draft ->
        chats.firstOrNull { it.id == id } ?: draft.copy(id = id)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, _draft.value)

    private val _pending = MutableStateFlow(false)
    val pending: StateFlow<Boolean> = _pending.asStateFlow()
    private var job: Job? = null

    /** What happened to each change card this session, so a card can say "Applied" or why it was refused. */
    val results = mutableStateMapOf<String, ActionResult>()

    fun connection(chat: AiChat = current.value): AiConnection? = container.aiConnectionFor(chat.provider?.let { runCatching { AiProvider.valueOf(it) }.getOrNull() }, chat.model)

    /** The connection of [provider] with the model saved for it, for fetching its model list. */
    fun connectionForProvider(provider: AiProvider): AiConnection? = container.aiConnectionFor(provider, null)

    fun ready(): Boolean = connection() != null

    /** The providers that can answer right now, each with the model saved for it. */
    fun usableProviders(): List<Pair<AiProvider, String>> = AiProvider.entries.mapNotNull { p ->
        container.aiConnectionFor(p, null)?.let { p to it.model }
    }

    fun newChat() {
        cancel()
        _draft.value = store.blank()
        _currentId.value = _draft.value.id
    }

    fun open(id: Long) {
        if (id == _currentId.value) return
        cancel()
        _currentId.value = id
    }

    fun rename(title: String) {
        val chat = current.value
        if (chat.messages.isNotEmpty()) store.rename(chat.id, title)
    }

    fun togglePin() {
        val chat = current.value
        if (chat.messages.isNotEmpty()) store.pin(chat.id, !chat.pinned)
    }

    fun delete(id: Long = current.value.id) {
        store.delete(id)
        if (id == _currentId.value) newChat()
    }

    /** Uses [model] of [provider] for this chat only. */
    fun setModel(provider: AiProvider, model: String) {
        val chat = current.value
        if (chat.messages.isEmpty()) {
            _draft.update { it.copy(provider = provider.name, model = model) }
        } else {
            store.put(chat.copy(provider = provider.name, model = model))
        }
    }

    /**
     * Sends [text]. With [replaceFrom] the message with that id and everything after it is dropped first, which is how editing a
     * question works: the conversation continues from the new wording.
     */
    fun send(text: String, replaceFrom: Long? = null) {
        val message = text.trim()
        if (message.isEmpty() || _pending.value) return
        val chat = current.value
        val connection = connection(chat) ?: return
        val kept = if (replaceFrom == null) chat.messages else chat.messages.takeWhile { it.id != replaceFrom }
        val now = System.currentTimeMillis()
        val user = ChatMessage(id = now, role = ChatRole.USER, text = message, time = now)
        val withUser = chat.copy(messages = kept + user, updated = now)
        store.put(withUser)
        _currentId.value = withUser.id
        answer(withUser, connection, kept, message)
    }

    /** Asks again for the last answer: drops it (and a failed attempt) and sends the question that led to it. */
    fun regenerate() {
        if (_pending.value) return
        val chat = current.value
        val lastUser = chat.messages.lastOrNull { it.role == ChatRole.USER } ?: return
        send(lastUser.text, replaceFrom = lastUser.id)
    }

    private fun answer(chat: AiChat, connection: AiConnection, earlier: List<ChatMessage>, message: String) {
        _pending.value = true
        job = viewModelScope.launch {
            try {
                val allowedNow = container.aiPermissions.allowed.value
                val calendar = if (allowedNow.isEmpty()) skywa1ker404.absencium.data.calendar.CalendarData() else container.calendarRepository.data.first()
                val notes = if (AiScope.NOTES in allowedNow) container.vaultRepository.data.first().notes else emptyList()
                val prompt = AiChatPrompt.build(
                    allowed = allowedNow,
                    calendar = calendar,
                    notes = notes,
                    history = earlier.filter { !it.failed && it.text.isNotBlank() }.map { it.role to it.text },
                    message = message,
                )
                val reply = container.aiClient.generate(connection, prompt, record = false)
                val parsed = AiActionParser.split(reply)
                val shown = parsed.text.ifBlank { if (parsed.actions.isNotEmpty()) "Here is what I suggest." else reply.trim() }
                append(chat.id, ChatMessage(System.currentTimeMillis(), ChatRole.ASSISTANT, shown, System.currentTimeMillis(), connection.model, actions = parsed.json?.takeIf { parsed.actions.isNotEmpty() }))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The reason is announced by the bar at the bottom of the screen; the chat only marks that this one failed.
                DebugLogger.e("AbsenciumAi", "A chat answer failed", e)
                append(chat.id, ChatMessage(System.currentTimeMillis(), ChatRole.ASSISTANT, "", System.currentTimeMillis(), connection.model, failed = true))
            } finally {
                _pending.value = false
            }
        }
    }

    private fun append(chatId: Long, message: ChatMessage) {
        val chat = store.get(chatId) ?: return
        // Ids are the time of day in milliseconds; an instant failure could land in the same one as the question, and list keys must differ.
        val id = maxOf(message.id, (chat.messages.maxOfOrNull { it.id } ?: 0L) + 1)
        store.put(chat.copy(messages = chat.messages + message.copy(id = id), updated = System.currentTimeMillis()))
    }

    fun cancel() {
        job?.cancel()
        _pending.value = false
    }

    /** Applies one proposed change after the student tapped Apply. */
    fun apply(messageId: Long, index: Int, action: AiAction) {
        val chatId = current.value.id
        viewModelScope.launch {
            val result = container.aiActionRunner.apply(action)
            results["$messageId:$index"] = result
            if (result.ok) store.markActionDone(chatId, messageId, index)
        }
    }

    /** Applies several proposed changes in order (a new subject must exist before a note is added to it). */
    fun applyAll(messageId: Long, items: List<Pair<Int, AiAction>>) {
        val chatId = current.value.id
        viewModelScope.launch {
            for ((index, action) in items) {
                val result = container.aiActionRunner.apply(action)
                results["$messageId:$index"] = result
                if (result.ok) store.markActionDone(chatId, messageId, index)
            }
        }
    }

    /** Marks a card as handled without changing anything. */
    fun dismiss(messageId: Long, index: Int) {
        store.markActionDone(current.value.id, messageId, index)
        results.remove("$messageId:$index")
    }

    fun deleteAppPrompt(time: Long) = container.aiHistory.remove(time)
    fun clearAppPrompts() = container.aiHistory.clear()

    /** The open chat as plain text, for sharing. */
    fun transcript(): String = current.value.messages.filter { !it.failed }.joinToString("\n\n") {
        (if (it.role == ChatRole.USER) "You: " else "Absencium AI: ") + it.text
    }

    override fun onCleared() {
        job?.cancel()
    }
}
