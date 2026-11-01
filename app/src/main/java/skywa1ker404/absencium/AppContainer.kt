package skywa1ker404.absencium

import android.content.Context
import skywa1ker404.absencium.data.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import skywa1ker404.absencium.data.ai.AiClient
import skywa1ker404.absencium.data.backup.BackupManager
import skywa1ker404.absencium.data.calendar.CalendarRepository
import skywa1ker404.absencium.data.db.AbsenciumDatabase
import skywa1ker404.absencium.data.security.ApiKeyVault
import skywa1ker404.absencium.data.settings.SettingsRepository
import skywa1ker404.absencium.data.storage.AttachmentStore
import skywa1ker404.absencium.data.storage.NotesStorage
import skywa1ker404.absencium.data.storage.NotesStorageSettings
import skywa1ker404.absencium.data.subjects.SubjectRepository
import skywa1ker404.absencium.data.trash.TrashRepository
import skywa1ker404.absencium.data.trash.UndoBus
import skywa1ker404.absencium.data.vault.VaultRepository

/** What the reading miniplayer shows: the note, where it is filed and which page the reader reached. */
data class MiniReading(val noteId: Long, val title: String, val place: String, val page: Int, val pages: Int)

/**
 * Hand-built dependency container, following the repository pattern from Now in Android
 * (the database and DataStore are the source of truth and repositories expose Flows)
 * without a DI framework.
 */
class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext

    val database: AbsenciumDatabase by lazy { AbsenciumDatabase.get(appContext) }
    val settingsRepository = SettingsRepository(appContext)

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * The saved settings, read as soon as the app process starts so they are ready before the first screen.
     * Null only until that first read finishes; the splash screen stays up until then.
     */
    val settingsState: StateFlow<AppSettings?> = settingsRepository.settings
        .stateIn(appScope, SharingStarted.Eagerly, null)
    val notesStorageSettings = NotesStorageSettings(appContext)
    val notesStorage = NotesStorage(appContext, notesStorageSettings)
    val apiKeyVault = ApiKeyVault(appContext).also { vault -> vault.legacyOwner = { settingsState.value?.aiProvider } }
    /** URL, model and request style of every provider the student has set up (keys stay in [apiKeyVault]). */
    val aiProfiles = skywa1ker404.absencium.data.ai.AiProfiles(appContext)
    val attachmentStore = AttachmentStore(appContext)
    val shareService: skywa1ker404.absencium.data.share.ShareService by lazy { skywa1ker404.absencium.data.share.ShareService(appContext) }
    val backupManager = BackupManager(appContext)
    val aiHistory = skywa1ker404.absencium.data.ai.AiHistory(appContext)
    val aiUsage = skywa1ker404.absencium.data.ai.AiUsage(appContext)
    val aiClient = AiClient(apiKeyVault).also {
        it.history = aiHistory
        it.usage = aiUsage
        // When every saved key of the chosen provider hit its limit, other providers with a key take over.
        it.fallbacks = { skywa1ker404.absencium.data.ai.AiProvider.entries.mapNotNull { p -> aiConnectionFor(p, null) } }
    }
    /** The chats of Absencium AI and which parts of the app's data it may use. Both stay on this phone. */
    val aiChats = skywa1ker404.absencium.data.ai.AiChats(appContext)
    val aiPermissions = skywa1ker404.absencium.data.ai.AiPermissions(appContext)
    val aiMemory = skywa1ker404.absencium.data.ai.AiMemory(appContext)
    val aiAssistants = skywa1ker404.absencium.data.ai.AiAssistants(appContext)
        /** Finds or draws the images AI notes ask for (both optional, off by default). */
    val diagramService: skywa1ker404.absencium.data.ai.DiagramService by lazy { skywa1ker404.absencium.data.ai.DiagramService(aiClient) }
    /** True when a key is saved for the provider in use. */
    fun activeKeySaved(): Boolean = settingsState.value?.aiProvider?.let { apiKeyVault.hasKey(it) } ?: false

    /** The AI the user set up, or null when AI Features are off or not yet set up. */
    fun aiConnection(): skywa1ker404.absencium.data.ai.AiConnection? {
        val s = settingsState.value ?: return null
        if (!skywa1ker404.absencium.ui.settings.aiUsable(s, apiKeyVault.hasKey(s.aiProvider))) return null
        return skywa1ker404.absencium.data.ai.AiConnection(s.aiProvider, s.effectiveAiStyle, s.aiBaseUrl, s.aiModel)
    }

    /**
     * The connection for one chat: [provider] and [model] when the chat picked its own, otherwise the provider in use. Null while AI is
     * off or that provider is not complete (no URL or model saved, or no key saved when it needs one).
     */
    fun aiConnectionFor(provider: skywa1ker404.absencium.data.ai.AiProvider?, model: String?): skywa1ker404.absencium.data.ai.AiConnection? {
        val s = settingsState.value ?: return null
        if (!s.askAiEnabled) return null
        val chosen = provider ?: s.aiProvider
        val profile = if (chosen == s.aiProvider && s.aiConnectionSaved) {
            skywa1ker404.absencium.data.ai.AiProfile(chosen, s.aiBaseUrl.ifBlank { chosen.defaultBaseUrl }, s.aiModel, s.effectiveAiStyle)
        } else {
            aiProfiles.get(chosen) ?: return null
        }
        if (profile.baseUrl.isBlank() || (chosen.keyRequired && !apiKeyVault.hasKey(chosen))) return null
        val useModel = model?.takeIf { it.isNotBlank() } ?: profile.model
        if (useModel.isBlank()) return null
        return skywa1ker404.absencium.data.ai.AiConnection(chosen, profile.style, profile.baseUrl, useModel)
    }

    /** Reads text out of pictures, PDFs, documents and recordings with the user's AI. */
    val textExtractor: skywa1ker404.absencium.data.ocr.TextExtractor by lazy {
        skywa1ker404.absencium.data.ocr.TextExtractor(aiClient) { aiConnection() }
    }
    val calendarRepository: CalendarRepository by lazy { CalendarRepository(database, settingsRepository.settings) }
    val vaultRepository: VaultRepository by lazy { VaultRepository(database) }
    val trashRepository: TrashRepository by lazy { TrashRepository(database, attachmentStore) }
    val undoBus = UndoBus()

    /** The note being read in the minimised reading player, or null. Lives here so every screen can show it. */
    val readingMini = kotlinx.coroutines.flow.MutableStateFlow<MiniReading?>(null)
    private val readingPrefs = appContext.getSharedPreferences("reading", Context.MODE_PRIVATE)

    /** Where reading stopped in a note (scroll position), so reading can continue from there. */
    fun readingPosition(noteId: Long): Int = readingPrefs.getInt("pos_$noteId", 0)
    fun saveReadingPosition(noteId: Long, value: Int) { readingPrefs.edit().putInt("pos_$noteId", value).apply() }

    /** A bookmarked place in a note, or -1. */
    fun bookmark(noteId: Long): Int = readingPrefs.getInt("mark_$noteId", -1)
    fun saveBookmark(noteId: Long, value: Int?) {
        readingPrefs.edit().apply { if (value == null) remove("mark_$noteId") else putInt("mark_$noteId", value) }.apply()
    }
    /** Applies the changes Absencium AI proposed once the student confirms them. */
    val aiActionRunner: skywa1ker404.absencium.data.ai.AiActionRunner by lazy {
        skywa1ker404.absencium.data.ai.AiActionRunner(database, calendarRepository, vaultRepository, subjectRepository) { brief, subject, length ->
            // A note the chat proposed is written by the same note writer as the Add Entry screen, at the length the chat chose.
            val connection = aiConnection() ?: return@AiActionRunner null
            runCatching {
                val raw = aiClient.generate(
                    connection,
                    skywa1ker404.absencium.data.ai.NotePrompts.notes(skywa1ker404.absencium.data.ai.NoteBrief(request = brief, subject = subject, length = length)),
                    maxTokens = length.maxTokens,
                    record = false,
                )
                skywa1ker404.absencium.data.ai.AiNoteCleaner.clean(raw).takeIf { it.isNotBlank() }
            }.getOrNull()
        }
    }

    /** Files attached to chat messages (copies in the app's private storage). */
    val aiAttachments = skywa1ker404.absencium.data.ai.AiAttachments(appContext)

    /**
     * Answers chats on the app's own scope, so an answer keeps being written when the student leaves the chat, the screen or the app.
     * A foreground service keeps the process alive while any answer is in flight.
     */
    val aiChatService: skywa1ker404.absencium.data.ai.AiChatService by lazy {
        skywa1ker404.absencium.data.ai.AiChatService(
            scope = appScope,
            chats = aiChats,
            client = aiClient,
            attachments = aiAttachments,
            actionRunner = { aiActionRunner },
            connectionFor = { chat -> aiConnectionFor(chat.provider?.let { runCatching { skywa1ker404.absencium.data.ai.AiProvider.valueOf(it) }.getOrNull() }, chat.model) },
            contextFor = { chat -> loadChatContext(chat) },
            memory = aiMemory,
            memoryAllowed = { skywa1ker404.absencium.data.ai.AiScope.MEMORY in aiPermissions.allowed.value },
            contentReader = { read -> readContent(read) },
            onBusy = { busy -> skywa1ker404.absencium.data.ai.AiWorkService.setBusy(appContext, busy) },
        )
    }

    /** The content of a note or study material the student approved Absencium AI to read, as a file to send; null when it cannot be found. */
    private suspend fun readContent(read: skywa1ker404.absencium.data.ai.AiAction.ReadContent): skywa1ker404.absencium.data.ai.ChatAttachment? {
        val vault = vaultRepository.data.first()
        val subject = vault.subjects.firstOrNull { it.name.trim().equals(read.subject.trim(), ignoreCase = true) } ?: return null
        val dir = java.io.File(appContext.filesDir, "ai_attachments").also { it.mkdirs() }
        return try {
            if (read.material) {
                val guide = vault.guides.firstOrNull { it.subjectId == subject.id && it.fileName.trim().equals(read.title.trim(), ignoreCase = true) } ?: return null
                val uri = if (guide.localUri.startsWith("content://")) android.net.Uri.parse(guide.localUri) else android.net.Uri.fromFile(java.io.File(guide.localUri))
                aiAttachments.import(uri, guide.fileName).attachment
            } else {
                val note = vault.notes.firstOrNull { it.deletedAt == null && it.subjectId == subject.id && it.title.trim().equals(read.title.trim(), ignoreCase = true) } ?: return null
                val file = java.io.File(dir, "note-${note.id}-${System.nanoTime()}.txt")
                file.writeText("${note.title}\n\n${note.bodyText}")
                skywa1ker404.absencium.data.ai.ChatAttachment("${note.title}.txt", "text/plain", file.absolutePath)
            }
        } catch (e: Exception) {
            skywa1ker404.absencium.logging.DebugLogger.e("AiContent", "An approved read failed", e)
            null
        }
    }

    /** What Absencium AI may know about the student right now: only the parts of the app they allowed. */
    private suspend fun loadChatContext(chat: skywa1ker404.absencium.data.ai.AiChat): skywa1ker404.absencium.data.ai.ChatContext {
        val allowed = aiPermissions.allowed.value
        val calendar = if (allowed.isEmpty()) skywa1ker404.absencium.data.calendar.CalendarData() else calendarRepository.data.first()
        val notes = if (skywa1ker404.absencium.data.ai.AiScope.NOTES in allowed) vaultRepository.data.first().notes else emptyList()
        val materials = if (skywa1ker404.absencium.data.ai.AiScope.MATERIAL in allowed) vaultRepository.data.first().guides else emptyList()
        val syllabus = if (skywa1ker404.absencium.data.ai.AiScope.SYLLABUS in allowed) database.syllabusDao().observeAll().first() else emptyList()
        val settings = settingsState.value
        return skywa1ker404.absencium.data.ai.ChatContext(
            allowed = allowed,
            calendar = calendar,
            notes = notes,
            materials = materials,
            syllabus = syllabus,
            thresholdPercent = settings?.attendanceThresholdPercent ?: 75,
            studentName = settings?.displayName?.takeIf { it.isNotBlank() },
            memories = if (skywa1ker404.absencium.data.ai.AiScope.MEMORY in allowed && !chat.incognito) {
                aiMemory.relevant(chat.messages.lastOrNull { it.role == skywa1ker404.absencium.data.ai.ChatRole.USER && !it.hidden }?.text.orEmpty(), chat.id)
            } else {
                emptyList()
            },
            instructions = aiAssistants.find(chat.assistant)?.instructions.orEmpty(),
            temperature = aiAssistants.find(chat.assistant)?.temperature,
        )
    }
    val subjectRepository: SubjectRepository by lazy { SubjectRepository(database.subjectDao(), database.subjectCascadeDao()) }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as AbsenciumApp).container
