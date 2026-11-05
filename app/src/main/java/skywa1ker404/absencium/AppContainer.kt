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
    }
    /** Bumped whenever a provider or key is added, changed, switched off or removed, so every screen showing them redraws at once. */
    val providersRevision = kotlinx.coroutines.flow.MutableStateFlow(0)
        /** Finds or draws the images AI notes ask for (both optional, off by default). */
    val diagramService: skywa1ker404.absencium.data.ai.DiagramService by lazy { skywa1ker404.absencium.data.ai.DiagramService(aiClient) }
    /** True when Absencium AI has a chat model with a key (or a local server) ready for the app's own AI features. */
    fun activeKeySaved(): Boolean = chatChoice() != null

    private fun chatChoice(): me.rerere.rikkahub.LastChatApi.Choice? =
        runCatching { me.rerere.rikkahub.LastChatApi.choice(appContext as android.app.Application) }.getOrNull()
            ?.takeIf { it.apiKey.isNotBlank() || it.baseUrl.contains("localhost") || it.baseUrl.contains("127.0.0.1") || it.baseUrl.contains("192.168.") }

    /**
     * The AI the student set up in Absencium AI, or null when AI Features are off or no chat model is chosen yet. The app's own AI
     * features (note writer, syllabus import, text from files, homework) all use it.
     */
    fun aiConnection(): skywa1ker404.absencium.data.ai.AiConnection? {
        if (settingsState.value?.askAiEnabled != true) return null
        val c = chatChoice() ?: return null
        val provider = when {
            c.baseUrl.contains("openrouter") -> skywa1ker404.absencium.data.ai.AiProvider.OPENROUTER
            c.baseUrl.contains("groq") -> skywa1ker404.absencium.data.ai.AiProvider.GROQ
            c.baseUrl.contains("11434") -> skywa1ker404.absencium.data.ai.AiProvider.OLLAMA
            c.kind == me.rerere.rikkahub.LastChatApi.Kind.GOOGLE -> skywa1ker404.absencium.data.ai.AiProvider.GEMINI
            c.kind == me.rerere.rikkahub.LastChatApi.Kind.CLAUDE -> skywa1ker404.absencium.data.ai.AiProvider.ANTHROPIC
            c.baseUrl.contains("api.openai.com") -> skywa1ker404.absencium.data.ai.AiProvider.OPENAI
            else -> skywa1ker404.absencium.data.ai.AiProvider.CUSTOM
        }
        val style = when (c.kind) {
            me.rerere.rikkahub.LastChatApi.Kind.GOOGLE -> skywa1ker404.absencium.data.ai.ApiStyle.GEMINI
            me.rerere.rikkahub.LastChatApi.Kind.CLAUDE -> skywa1ker404.absencium.data.ai.ApiStyle.ANTHROPIC
            else -> skywa1ker404.absencium.data.ai.ApiStyle.OPENAI_COMPATIBLE
        }
        return skywa1ker404.absencium.data.ai.AiConnection(provider, style, c.baseUrl, c.modelId, apiKey = c.apiKey)
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

    val subjectRepository: SubjectRepository by lazy { SubjectRepository(database.subjectDao(), database.subjectCascadeDao()) }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as AbsenciumApp).container
