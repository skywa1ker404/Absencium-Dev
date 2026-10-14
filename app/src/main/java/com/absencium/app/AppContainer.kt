package com.absencium.app

import android.content.Context
import com.absencium.app.data.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import com.absencium.app.data.ai.AiClient
import com.absencium.app.data.backup.DatabaseBackup
import com.absencium.app.data.calendar.CalendarRepository
import com.absencium.app.data.db.AbsenciumDatabase
import com.absencium.app.data.security.ApiKeyVault
import com.absencium.app.data.settings.SettingsRepository
import com.absencium.app.data.storage.AttachmentStore
import com.absencium.app.data.storage.NotesStorage
import com.absencium.app.data.storage.NotesStorageSettings
import com.absencium.app.data.subjects.SubjectRepository
import com.absencium.app.data.vault.VaultRepository

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
    val databaseBackup = DatabaseBackup(appContext)
    val aiClient = AiClient(apiKeyVault)
    val calendarRepository: CalendarRepository by lazy { CalendarRepository(database, settingsRepository.settings) }
    val vaultRepository: VaultRepository by lazy { VaultRepository(database) }
    val subjectRepository: SubjectRepository by lazy { SubjectRepository(database.subjectDao(), database.subjectCascadeDao()) }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as AbsenciumApp).container
