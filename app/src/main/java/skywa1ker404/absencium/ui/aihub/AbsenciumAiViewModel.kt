package skywa1ker404.absencium.ui.aihub

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import skywa1ker404.absencium.AppContainer
import skywa1ker404.absencium.data.ai.ActionResult
import skywa1ker404.absencium.data.ai.AiAction
import skywa1ker404.absencium.data.ai.AiAssistants
import skywa1ker404.absencium.data.ai.AiChat
import skywa1ker404.absencium.data.ai.AiChatService
import skywa1ker404.absencium.data.ai.AiConnection
import skywa1ker404.absencium.data.ai.AiHistoryEntry
import skywa1ker404.absencium.data.ai.AiNoteCleaner
import skywa1ker404.absencium.data.ai.AiProvider
import skywa1ker404.absencium.data.ai.AiScope
import skywa1ker404.absencium.data.ai.AssistantProfile
import skywa1ker404.absencium.data.ai.ChatAttachment
import skywa1ker404.absencium.data.ai.ChatMessage
import skywa1ker404.absencium.data.ai.ChatRole
import skywa1ker404.absencium.data.ai.MemoryItem
import skywa1ker404.absencium.data.ai.ProviderUsage
import skywa1ker404.absencium.data.db.NoteStatus
import skywa1ker404.absencium.data.db.SubjectEntity
import skywa1ker404.absencium.data.settings.AppSettings
import skywa1ker404.absencium.logging.DebugLogger
import java.io.File

/**
 * The state of Absencium AI as one screen sees it. Answers are written by [AiChatService] on the app's own scope, so nothing here is
 * cancelled when the student opens another chat, leaves the screen or closes the app; this class only starts them and shows them.
 */
class AbsenciumAiViewModel(private val container: AppContainer) : ViewModel() {
    private val store = container.aiChats
    private val service = container.aiChatService

    val settings: StateFlow<AppSettings?> = container.settingsState
    val chats: StateFlow<List<AiChat>> = store.chats
    val allowed: StateFlow<Set<AiScope>> = container.aiPermissions.allowed

    /** Prompts the app itself made (notes, syllabus, homework). Shown read-only in the sidebar. */
    val appPrompts: StateFlow<List<AiHistoryEntry>> = container.aiHistory.entries

    val memories: StateFlow<List<MemoryItem>> = container.aiMemory.items
    val assistants: StateFlow<List<AssistantProfile>> = container.aiAssistants.all
    val usage: StateFlow<List<ProviderUsage>> = container.aiUsage.rows

    /** Chats being answered right now, and chats with an answer not yet looked at. */
    val busyChats: StateFlow<Set<Long>> = service.pending
    val unread: StateFlow<Set<Long>> = service.unread
    val results: StateFlow<Map<String, ActionResult>> = service.results
    val working: StateFlow<Set<String>> = service.working

    val subjects: StateFlow<List<SubjectEntity>> =
        container.database.subjectDao().observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Everything the chat can link to by name: subjects, notes and study material. */
    val vault: StateFlow<skywa1ker404.absencium.data.vault.VaultData> =
        container.vaultRepository.data.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), skywa1ker404.absencium.data.vault.VaultData())

    /** The notes that can be attached to a question, with the subject each belongs to. */
    val noteChoices: StateFlow<List<PickItem>> = container.vaultRepository.data.map { vault ->
        vault.notes.filter { it.deletedAt == null }.sortedByDescending { it.updatedAt }.map { PickItem(it.id, it.title, it.subjectId) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The study material (files) that can be attached to a question. */
    val materialChoices: StateFlow<List<PickItem>> = container.vaultRepository.data.map { vault ->
        vault.guides.sortedByDescending { it.updatedAt }.map { PickItem(it.id, it.fileName, it.subjectId) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _draft = MutableStateFlow(store.blank())
    private val _currentId = MutableStateFlow(_draft.value.id)

    /** The open chat: a saved one, or the empty draft that is saved when its first message is sent. */
    val current: StateFlow<AiChat> = combine(store.chats, _currentId, _draft) { chats, id, draft ->
        chats.firstOrNull { it.id == id } ?: draft.copy(id = id)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, _draft.value)

    /** True while the open chat is being answered. */
    val pending: StateFlow<Boolean> = combine(service.pending, _currentId) { busy, id -> id in busy }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Files chosen for the next message. */
    private val _files = MutableStateFlow<List<ChatAttachment>>(emptyList())
    val files: StateFlow<List<ChatAttachment>> = _files

    /** A short plain message about the last thing that did not work (a file that could not be added) or worked (a note saved). */
    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice

    private var visible = false

    fun clearNotice() {
        _notice.value = null
    }

    /** Tells the service whether the student is looking at the open chat, so a finished answer elsewhere can be announced. */
    fun setVisible(on: Boolean) {
        visible = on
        service.setViewing(if (on) _currentId.value else null)
    }

    fun connection(chat: AiChat = current.value): AiConnection? =
        container.aiConnectionFor(chat.provider?.let { runCatching { AiProvider.valueOf(it) }.getOrNull() }, chat.model)

    /** The connection of [provider] with the model saved for it, for fetching its model list. */
    fun connectionForProvider(provider: AiProvider): AiConnection? = container.aiConnectionFor(provider, null)

    /** The providers that can answer right now, each with the model saved for it. */
    fun usableProviders(): List<Pair<AiProvider, String>> = AiProvider.entries.mapNotNull { p ->
        container.aiConnectionFor(p, null)?.let { p to it.model }
    }

    /** Web search works with every model: Gemini searches itself, any other gets the app's own search results. */
    fun canSearch(): Boolean = connection() != null

    /** Starts or leaves an incognito chat: nothing is written to the phone and memory is not used or saved. Only on an empty chat. */
    fun setIncognito(on: Boolean) = change { it.copy(incognito = on) }

    fun newChat() {
        store.dropIncognito()
        _draft.value = store.blank()
        _files.value = emptyList()
        switchTo(_draft.value.id)
    }

    fun open(id: Long) {
        if (id == _currentId.value) return
        store.dropIncognito()
        _files.value = emptyList()
        switchTo(id)
    }

    private fun switchTo(id: Long) {
        _currentId.value = id
        if (visible) service.setViewing(id)
    }

    fun rename(title: String, id: Long = current.value.id) {
        if (store.get(id) != null) store.rename(id, title)
    }

    fun togglePin(id: Long = current.value.id) = store.get(id)?.let { store.pin(id, !it.pinned) }
    fun toggleArchive(id: Long = current.value.id) = store.get(id)?.let { store.archive(id, !it.archived) }

    fun delete(id: Long = current.value.id) {
        service.stop(id)
        container.aiMemory.forgetChat(id)
        store.delete(id)
        if (id == _currentId.value) newChat()
    }

    /** Changes a setting of the open chat; before its first message it changes the draft. */
    private fun change(block: (AiChat) -> AiChat) {
        val chat = current.value
        if (chat.messages.isEmpty()) _draft.update { block(it.copy(id = chat.id)) } else store.put(block(chat))
    }

    /** Uses [model] of [provider] for this chat only. */
    fun setModel(provider: AiProvider, model: String) = change { it.copy(provider = provider.name, model = model) }

    fun setWebSearch(on: Boolean) = change { it.copy(webSearch = on) }
    fun setStudy(on: Boolean) = change { it.copy(study = on) }
    fun setThinking(on: Boolean) = change { it.copy(thinking = on) }
    fun setLevel(level: String) = change { it.copy(level = level) }

    /** Gives the open chat an assistant profile (or none), and its preferred model when it has one that is ready. */
    fun setAssistant(profile: AssistantProfile?) {
        change { it.copy(assistant = profile?.id.orEmpty()) }
        val provider = profile?.provider?.let { runCatching { AiProvider.valueOf(it) }.getOrNull() }
        if (provider != null && profile.model != null && container.aiConnectionFor(provider, profile.model) != null) setModel(provider, profile.model)
    }

    fun saveAssistant(profile: AssistantProfile) = container.aiAssistants.save(profile)
    fun deleteAssistant(id: String) = container.aiAssistants.delete(id)
    fun exportAssistants(): String = container.aiAssistants.export()
    fun importAssistants(text: String): Int = container.aiAssistants.import(text)

    fun setScope(scope: AiScope, on: Boolean) = container.aiPermissions.set(scope, on)

    fun addMemory(text: String, thisChatOnly: Boolean) {
        container.aiMemory.add(text, if (thisChatOnly) current.value.id else null)
    }

    fun updateMemory(id: Long, text: String) = container.aiMemory.update(id, text)
    fun deleteMemory(id: Long) = container.aiMemory.delete(id)
    fun clearMemory() = container.aiMemory.clear()

    // ---- files ----

    fun addFiles(uris: List<Uri>) {
        viewModelScope.launch {
            for (uri in uris.take(MAX_FILES - _files.value.size)) {
                val result = container.aiAttachments.import(uri)
                result.attachment?.let { file -> _files.update { it + file } }
                result.error?.let { _notice.value = it }
            }
        }
    }

    /** Attaches the text of the chosen notes, so the question is answered from them (only notes the student picks are ever sent). */
    fun attachNotes(ids: Set<Long>) {
        viewModelScope.launch {
            val notes = container.vaultRepository.data.first().notes.filter { it.id in ids }
            for (note in notes.take(MAX_FILES - _files.value.size)) {
                try {
                    val dir = File(container.appContext.filesDir, "ai_attachments").also { it.mkdirs() }
                    val file = File(dir, "note-${note.id}-${System.nanoTime()}.txt")
                    file.writeText("${note.title}\n\n${note.bodyText}")
                    _files.update { it + ChatAttachment("${note.title}.txt", "text/plain", file.absolutePath) }
                } catch (e: Exception) {
                    DebugLogger.e("AbsenciumAi", "A note could not be attached", e)
                    _notice.value = "That note could not be attached."
                }
            }
        }
    }

    /** Attaches the chosen study material files, so the question is answered from them (only what the student ticks is ever sent). */
    fun attachMaterials(ids: Set<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val guides = container.vaultRepository.data.first().guides.filter { it.id in ids }
            for (guide in guides.take(MAX_FILES - _files.value.size)) {
                val uri = if (guide.localUri.startsWith("content://")) Uri.parse(guide.localUri) else Uri.fromFile(File(guide.localUri))
                val result = container.aiAttachments.import(uri, guide.fileName)
                result.attachment?.let { file -> _files.update { it + file } }
                result.error?.let { _notice.value = it }
            }
        }
    }

    fun removeFile(file: ChatAttachment) {
        _files.update { it - file }
        container.aiAttachments.discard(file)
    }

    // ---- sending ----

    /**
     * Sends [text] (with the chosen files). With [replaceFrom] the message with that id and everything after it is dropped first, which
     * is how editing a question works: the conversation continues from the new wording, with the files it had.
     */
    fun send(text: String, replaceFrom: Long? = null) {
        val message = text.trim()
        val chat = current.value
        if (chat.id in busyChats.value) return
        val replaced = replaceFrom?.let { id -> chat.messages.firstOrNull { it.id == id } }
        val attachments = _files.value + replaced?.attachments.orEmpty()
        if (message.isEmpty() && attachments.isEmpty()) return
        if (connection(chat) == null) return
        val kept = if (replaceFrom == null) chat.messages else chat.messages.takeWhile { it.id != replaceFrom }
        _files.value = emptyList()
        submit(chat, kept, message.ifEmpty { "Look at the attached file." }, attachments)
    }

    private fun submit(chat: AiChat, kept: List<ChatMessage>, text: String, attachments: List<ChatAttachment>) {
        val now = System.currentTimeMillis()
        val id = maxOf(now, (kept.maxOfOrNull { it.id } ?: 0L) + 1)
        val user = ChatMessage(id = id, role = ChatRole.USER, text = text, time = now, attachments = attachments)
        val saved = chat.copy(messages = kept + user, updated = now)
        store.put(saved)
        _currentId.value = saved.id
        if (visible) service.setViewing(saved.id)
        service.answer(saved.id)
    }

    /** Asks again for the last answer: drops it (and a failed attempt) and sends the question that led to it, with its files. */
    fun regenerate() {
        val chat = current.value
        if (chat.id in busyChats.value || connection(chat) == null) return
        val lastUser = chat.messages.lastOrNull { it.role == ChatRole.USER && !it.hidden } ?: return
        submit(chat, chat.messages.takeWhile { it.id != lastUser.id }, lastUser.text, lastUser.attachments)
    }

    /** Asks the last question again with [model] of [provider] instead (this chat keeps it from then on). */
    fun retryWith(provider: AiProvider, model: String) {
        setModel(provider, model)
        regenerate()
    }

    /** Asks the model to carry on from where its last answer stopped. */
    fun continueAnswer() {
        val chat = current.value
        if (chat.id in busyChats.value || connection(chat) == null) return
        submit(chat, chat.messages, "Continue exactly where you stopped, without repeating anything.", emptyList())
    }

    fun stop() = service.stop(current.value.id)

    // ---- cards ----

    fun apply(messageId: Long, items: List<Pair<Int, AiAction>>) = service.apply(current.value.id, messageId, items)
    fun dismiss(messageId: Long, index: Int) = service.dismiss(current.value.id, messageId, index)

    // ---- notes ----

    /** Saves an answer as a note of [subjectId], cleaned into the editor's markup. */
    fun saveAsNote(message: ChatMessage, subjectId: Long) {
        viewModelScope.launch {
            try {
                val body = AiNoteCleaner.clean(message.text)
                val question = current.value.messages.lastOrNull { it.role == ChatRole.USER && !it.hidden && it.id < message.id }?.text.orEmpty()
                val heading = body.lineSequence().map { it.trim().trimStart('#', ' ').trim() }.firstOrNull { it.isNotEmpty() }
                val title = (heading ?: question).replace(Regex("[*_=+~`]"), "").take(80).ifBlank { "AI note" }
                container.vaultRepository.saveNote(0, subjectId, title, body, NoteStatus.ACTIVE)
                _notice.value = "Saved as a note."
            } catch (e: Exception) {
                DebugLogger.e("AbsenciumAi", "An answer could not be saved as a note", e)
                _notice.value = "That could not be saved as a note."
            }
        }
    }

    fun deleteAppPrompt(time: Long) = container.aiHistory.remove(time)
    fun clearAppPrompts() = container.aiHistory.clear()

    /** The open chat as plain text, for sharing. */
    fun transcript(): String = current.value.messages.filter { !it.failed && !it.hidden }.joinToString("\n\n") {
        (if (it.role == ChatRole.USER) "You: " else "Absencium AI: ") + it.text
    }

    private companion object {
        const val MAX_FILES = 8
    }

    override fun onCleared() {
        store.dropIncognito()
        super.onCleared()
    }
}
