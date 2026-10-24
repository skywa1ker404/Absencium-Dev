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
    val apiKeyVault = ApiKeyVault(appContext)
    val attachmentStore = AttachmentStore(appContext)
    val shareService: skywa1ker404.absencium.data.share.ShareService by lazy { skywa1ker404.absencium.data.share.ShareService(appContext) }
    val backupManager = BackupManager(appContext)
    val aiClient = AiClient(apiKeyVault)
    /** Reads text out of pictures, PDFs, documents and recordings, with the user's AI or on the phone. */
    val textExtractor: skywa1ker404.absencium.data.ocr.TextExtractor by lazy {
        skywa1ker404.absencium.data.ocr.TextExtractor(aiClient, skywa1ker404.absencium.data.ocr.LocalOcr(appContext)) {
            val s = settingsState.value
            if (s != null && skywa1ker404.absencium.ui.settings.aiReady(s, apiKeyVault.hasKey())) {
                skywa1ker404.absencium.data.ai.AiConnection(s.aiProvider, s.effectiveAiStyle, s.aiBaseUrl, s.aiModel)
            } else {
                null
            }
        }
    }
    val calendarRepository: CalendarRepository by lazy { CalendarRepository(database, settingsRepository.settings) }
    val vaultRepository: VaultRepository by lazy { VaultRepository(database) }
    val trashRepository: TrashRepository by lazy { TrashRepository(database, attachmentStore) }
    val undoBus = UndoBus()
    val subjectRepository: SubjectRepository by lazy { SubjectRepository(database.subjectDao(), database.subjectCascadeDao()) }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as AbsenciumApp).container
