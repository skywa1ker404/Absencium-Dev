package com.trajectory.vault.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trajectory.vault.AppContainer
import com.trajectory.vault.appContainer
import com.trajectory.vault.data.db.SubjectEntity
import com.trajectory.vault.data.storage.NotesStorageMode
import com.trajectory.vault.logging.DebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Creates a ViewModel that needs the [AppContainer], without a DI framework. */
@Composable
inline fun <reified VM : ViewModel> containerViewModel(crossinline builder: (AppContainer) -> VM): VM {
    val container = LocalContext.current.appContainer
    return viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = builder(container) as T
        },
    )
}

// ---------------------------------------------------------------------------------------------
// Storage, backup and restore
// ---------------------------------------------------------------------------------------------

data class StorageUiState(
    val mode: NotesStorageMode? = null,
    val path: String? = null,
    val message: String? = null,
    val busy: Boolean = false,
    val restoreStaged: Boolean = false,
)

class StorageViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(StorageUiState(restoreStaged = container.databaseBackup.hasStagedRestore()))
    val state: StateFlow<StorageUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val (mode, path) = withContext(Dispatchers.IO) {
                container.notesStorageSettings.selectedMode() to container.notesStorage.rootFolder()?.displayPath
            }
            _state.update { it.copy(mode = mode, path = path) }
        }
    }

    fun useAppFiles() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { container.notesStorageSettings.useAppFiles() }
            _state.update { it.copy(message = "Notes will be stored in the app folder.") }
            refresh()
        }
    }

    fun useCustomFolder(uri: Uri) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { container.notesStorageSettings.useCustomFolder(uri) }
            _state.update {
                it.copy(
                    message = if (ok) "Notes folder updated." else "Could not use that folder. See debug.log for details.",
                )
            }
            refresh()
        }
    }

    fun backupTo(uri: Uri) {
        runBusy("Backup") {
            val size = container.appContext.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                container.databaseBackup.export(out)
            }
            if (size == null) "Could not open the file to write the backup." else "Backup saved ($size bytes of data)."
        }
    }

    fun restoreFrom(uri: Uri) {
        runBusy("Restore") {
            val input = container.appContext.contentResolver.openInputStream(uri)
            // stageRestore returns null on success, so the stream check has to be separate.
            val error = if (input == null) {
                "Could not open the backup file."
            } else {
                input.use { container.databaseBackup.stageRestore(it) }
            }
            if (error == null) {
                _state.update { it.copy(restoreStaged = true) }
                "Backup checked and ready. Close and reopen Absencium to finish restoring."
            } else {
                error
            }
        }
    }

    private fun runBusy(label: String, work: suspend () -> String) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            val message = try {
                withContext(Dispatchers.IO) { work() }
            } catch (e: Exception) {
                DebugLogger.e("Backup", "$label failed", e)
                "$label failed: ${e.message ?: e.javaClass.simpleName}"
            }
            _state.update { it.copy(busy = false, message = message) }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// AI key vault
// ---------------------------------------------------------------------------------------------

data class AiKeyUiState(val hasKey: Boolean = false, val lastFour: String? = null, val message: String? = null)

class AiKeyViewModel(private val container: AppContainer) : ViewModel() {

    private val vault = container.apiKeyVault

    private val _state = MutableStateFlow(AiKeyUiState(vault.hasKey(), vault.lastFour()))
    val state: StateFlow<AiKeyUiState> = _state.asStateFlow()

    fun save(key: String) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { vault.save(key) }
            _state.value = if (ok) {
                AiKeyUiState(hasKey = true, lastFour = vault.lastFour(), message = "Key saved, encrypted on this device.")
            } else {
                _state.value.copy(message = "Could not save the key. See debug.log for details.")
            }
        }
    }

    fun clear() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { vault.clear() }
            _state.value = AiKeyUiState(message = "Key removed.")
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Subjects
// ---------------------------------------------------------------------------------------------

class SubjectsViewModel(container: AppContainer) : ViewModel() {

    private val repository = container.subjectRepository

    val subjects: StateFlow<List<SubjectEntity>> = repository.subjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun add(name: String) = perform { repository.add(name) }

    fun setActive(subject: SubjectEntity, active: Boolean) = perform { repository.setActive(subject, active) }

    fun setMuted(subject: SubjectEntity, muted: Boolean) = perform { repository.setMuted(subject, muted) }

    fun delete(subject: SubjectEntity) = perform { repository.delete(subject) }

    suspend fun entryCount(subject: SubjectEntity): Int = repository.entryCount(subject)

    fun deleteWithEntries(subject: SubjectEntity) = perform { repository.deleteWithEntries(subject) }

    private fun perform(block: suspend () -> String?) {
        viewModelScope.launch { _message.value = block() }
    }
}

// ---------------------------------------------------------------------------------------------
// Test Lab
// ---------------------------------------------------------------------------------------------

data class TestLabUiState(
    val tableCounts: List<Pair<String, Int>> = emptyList(),
    val databaseMessage: String? = null,
    val storageMessage: String? = null,
    val logTail: String = "",
    val busy: Boolean = false,
)

class TestLabViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(TestLabUiState())
    val state: StateFlow<TestLabUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val counts = withContext(Dispatchers.IO) { loadTableCounts() }
            val tail = withContext(Dispatchers.IO) { DebugLogger.readTail() }
            _state.update { it.copy(tableCounts = counts, logTail = tail) }
        }
    }

    fun addSampleSubjects() {
        viewModelScope.launch {
            val message = withContext(Dispatchers.IO) {
                try {
                    val dao = container.database.subjectDao()
                    var added = 0
                    listOf("Sample: Mathematics", "Sample: Physics", "Sample: Literature").forEach { name ->
                        try {
                            dao.insert(SubjectEntity(name = name))
                            added++
                        } catch (e: Exception) {
                            // The subject name is unique, so a repeat insert fails. That is expected here.
                            DebugLogger.d("TestLab", "Sample subject \"$name\" was not added: ${e.message}")
                        }
                    }
                    DebugLogger.i("TestLab", "Added $added sample subjects")
                    "Added $added sample subjects."
                } catch (e: Exception) {
                    DebugLogger.e("TestLab", "Adding sample subjects failed", e)
                    "Failed: ${e.message ?: e.javaClass.simpleName}"
                }
            }
            _state.update { it.copy(databaseMessage = message) }
            refresh()
        }
    }

    fun removeSampleData() {
        viewModelScope.launch {
            val message = withContext(Dispatchers.IO) {
                try {
                    val removed = container.database.subjectDao().deleteSamples()
                    DebugLogger.i("TestLab", "Removed $removed sample subjects")
                    "Removed $removed sample subjects."
                } catch (e: Exception) {
                    DebugLogger.e("TestLab", "Removing sample subjects failed", e)
                    "Failed: ${e.message ?: e.javaClass.simpleName}"
                }
            }
            _state.update { it.copy(databaseMessage = message) }
            refresh()
        }
    }

    fun runStorageTest() {
        viewModelScope.launch {
            val message = withContext(Dispatchers.IO) { storageTest() }
            _state.update { it.copy(storageMessage = message) }
            refresh()
        }
    }

    fun logTestLine() {
        DebugLogger.i("TestLab", "Manual test line written from the Test Lab")
        refresh()
    }

    private fun loadTableCounts(): List<Pair<String, Int>> =
        try {
            val database = container.database.openHelper.readableDatabase
            val names = mutableListOf<String>()
            database.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' " +
                    "AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%' " +
                    "AND name != 'room_master_table' ORDER BY name",
            ).use { cursor ->
                while (cursor.moveToNext()) names.add(cursor.getString(0))
            }
            names.map { name ->
                // The names come from sqlite_master, not from user input.
                val count = database.query("SELECT COUNT(*) FROM \"$name\"").use { cursor ->
                    if (cursor.moveToFirst()) cursor.getInt(0) else 0
                }
                name to count
            }
        } catch (e: Exception) {
            DebugLogger.e("TestLab", "Could not count table rows", e)
            emptyList()
        }

    private fun storageTest(): String {
        val root = container.notesStorage.rootFolder()
            ?: return "No notes folder is available. Choose one in Settings, then Storage and Data."
        val folder = root.childFolder("_testlab") ?: return "Could not create the test folder in ${root.displayPath}."
        val stamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        val written = folder.openOutput("hello.txt", "text/plain")?.use { out ->
            out.write("Absencium storage test at $stamp".toByteArray(Charsets.UTF_8))
            true
        } ?: false
        if (!written) return "Could not write a test file in ${folder.displayPath}."
        val readBack = folder.openInput("hello.txt")?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: return "Wrote the test file but could not read it back."
        val files = folder.listFileNames()
        DebugLogger.i("TestLab", "Storage test passed in ${folder.displayPath}: files=$files")
        return "Passed. Wrote and read back \"$readBack\". Files in the test folder: ${files.joinToString()}. " +
            "Location: ${folder.displayPath}"
    }
}
