package com.trajectory.vault

import android.content.Context
import com.trajectory.vault.data.ai.AiClient
import com.trajectory.vault.data.backup.DatabaseBackup
import com.trajectory.vault.data.calendar.CalendarRepository
import com.trajectory.vault.data.db.TrajectoryDatabase
import com.trajectory.vault.data.security.ApiKeyVault
import com.trajectory.vault.data.settings.SettingsRepository
import com.trajectory.vault.data.storage.NotesStorage
import com.trajectory.vault.data.storage.NotesStorageSettings
import com.trajectory.vault.data.subjects.SubjectRepository
import com.trajectory.vault.data.vault.VaultRepository

/**
 * Hand-built dependency container, following the repository pattern from Now in Android
 * (the database and DataStore are the source of truth and repositories expose Flows)
 * without a DI framework.
 */
class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext

    val database: TrajectoryDatabase by lazy { TrajectoryDatabase.get(appContext) }
    val settingsRepository = SettingsRepository(appContext)
    val notesStorageSettings = NotesStorageSettings(appContext)
    val notesStorage = NotesStorage(appContext, notesStorageSettings)
    val apiKeyVault = ApiKeyVault(appContext)
    val databaseBackup = DatabaseBackup(appContext)
    val aiClient = AiClient(apiKeyVault)
    val calendarRepository: CalendarRepository by lazy { CalendarRepository(database) }
    val vaultRepository: VaultRepository by lazy { VaultRepository(database) }
    val subjectRepository: SubjectRepository by lazy { SubjectRepository(database.subjectDao(), database.subjectCascadeDao()) }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as TrajectoryApp).container
