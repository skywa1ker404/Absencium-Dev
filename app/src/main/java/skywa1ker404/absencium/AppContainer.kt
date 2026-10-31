package skywa1ker404.absencium

import android.content.Context
import skywa1ker404.absencium.data.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
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
    val aiClient = AiClient(apiKeyVault).also { it.history = aiHistory }
    /** The chats of Absencium AI and which parts of the app's data it may use. Both stay on this phone. */
    val aiChats = skywa1ker404.absencium.data.ai.AiChats(appContext)
    val aiPermissions = skywa1ker404.absencium.data.ai.AiPermissions(appContext)
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
        skywa1ker404.absencium.data.ai.AiActionRunner(database, calendarRepository, vaultRepository, subjectRepository)
    }
    val subjectRepository: SubjectRepository by lazy { SubjectRepository(database.subjectDao(), database.subjectCascadeDao()) }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as AbsenciumApp).container
