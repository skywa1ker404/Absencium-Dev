package com.trajectory.vault

import android.content.Context
import com.trajectory.vault.data.db.TrajectoryDatabase
import com.trajectory.vault.data.settings.SettingsRepository
import com.trajectory.vault.data.storage.NotesStorage
import com.trajectory.vault.data.storage.NotesStorageSettings

/**
 * Hand-built dependency container, following the repository pattern from Now in Android
 * (the database and DataStore are the source of truth and repositories expose Flows)
 * without a DI framework.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val database: TrajectoryDatabase by lazy { TrajectoryDatabase.get(appContext) }
    val settingsRepository = SettingsRepository(appContext)
    val notesStorageSettings = NotesStorageSettings(appContext)
    val notesStorage = NotesStorage(appContext, notesStorageSettings)
}

val Context.appContainer: AppContainer
    get() = (applicationContext as TrajectoryApp).container
