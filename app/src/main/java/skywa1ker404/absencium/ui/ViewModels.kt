package skywa1ker404.absencium.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import skywa1ker404.absencium.AppContainer
import skywa1ker404.absencium.appContainer
import skywa1ker404.absencium.data.backup.BackupSummary
import skywa1ker404.absencium.data.backup.StageResult
import skywa1ker404.absencium.data.db.SubjectEntity
import skywa1ker404.absencium.data.storage.NotesStorageMode
import skywa1ker404.absencium.logging.DebugLogger
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
    /** The chosen backup is password-protected and the password has not been given (or was wrong). */
    val needsPassword: Boolean = false,
    val passwordWasWrong: Boolean = false,
    /** What the staged backup contains, shown next to the Restart button. */
    val preview: BackupSummary? = null,
)

class StorageViewModel(private val container: AppContainer) : ViewModel() {

    private val backups = container.backupManager

    private val _state = MutableStateFlow(StorageUiState(restoreStaged = backups.hasStagedRestore()))
    val state: StateFlow<StorageUiState> = _state.asStateFlow()

    /** The encrypted file the user chose, kept while the password is asked for. */
    private var pendingRestore: Uri? = null

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

    /** Writes a backup package. A blank [password] leaves it unencrypted. */
    fun backupTo(uri: Uri, password: String, includeAttachments: Boolean) {
        runBusy("Backup") {
            val settingsJson = container.settingsRepository.exportJson()
            val chars = password.toCharArray().takeIf { it.isNotEmpty() }
            val result = container.appContext.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                backups.export(out, chars, includeAttachments, settingsJson)
            }
            if (result == null) {
                "Could not open the file to write the backup."
            } else {
                val note = buildString {
                    append(if (result.includesAttachments) "with ${result.attachmentFiles} files" else "database and settings only")
                    append(if (result.encrypted) ", password-protected" else ", not encrypted")
                }
                container.settingsRepository.setLastBackup(System.currentTimeMillis(), note)
                "Backup saved ($note)."
            }
        }
    }

    /** Checks the chosen backup and stages it. An encrypted one first asks for its password ([needsPassword]). */
    fun restoreFrom(uri: Uri, password: String? = null) {
        runBusy("Restore") {
            pendingRestore = null
            val resolver = container.appContext.contentResolver
            val size = resolver.query(uri, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)?.use {
                if (it.moveToFirst() && !it.isNull(0)) it.getLong(0) else null
            }
            val input = resolver.openInputStream(uri) ?: return@runBusy "Could not open the backup file."
            val result = input.use { backups.stage(it, password?.toCharArray()?.takeIf { c -> c.isNotEmpty() }, size) }
            finishStage(result) { pendingRestore = uri }
        }
    }

    fun cancelPassword() {
        pendingRestore = null
        _state.update { it.copy(needsPassword = false, passwordWasWrong = false) }
    }

    fun submitPassword(password: String) {
        val uri = pendingRestore ?: return
        restoreFrom(uri, password)
    }

    fun discardStagedRestore() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { backups.cancelStagedRestore() }
            _state.update { it.copy(restoreStaged = false, preview = null, message = "The staged restore was discarded.") }
        }
    }

    private fun finishStage(result: StageResult, onNeedsPassword: () -> Unit): String {
        return when (result) {
            is StageResult.Ready -> {
                _state.update { it.copy(restoreStaged = true, preview = result.summary, needsPassword = false, passwordWasWrong = false) }
                if (result.summary.sameAsCurrent) {
                    "This backup is identical to the data on this phone. It is ready anyway; restart to apply it, or discard it."
                } else {
                    "Backup checked and ready. Restart Absencium to finish restoring."
                }
            }
            StageResult.NeedsPassword -> {
                onNeedsPassword()
                _state.update { it.copy(needsPassword = true, passwordWasWrong = false) }
                "This backup is password-protected."
            }
            is StageResult.Failed -> {
                if (result.passwordProblem) {
                    _state.update { it.copy(needsPassword = true, passwordWasWrong = true) }
                }
                result.message
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

/** One provider the student has set up: where it is, which model, and whether a key is saved for it. */
data class ProviderRow(
    val provider: skywa1ker404.absencium.data.ai.AiProvider,
    val hasKey: Boolean,
    val lastFour: String?,
    val baseUrl: String,
    val model: String,
    val style: skywa1ker404.absencium.data.ai.ApiStyle,
    val active: Boolean,
) {
    /** True when this provider could answer right now: URL and model saved, and a key if it needs one. */
    val complete: Boolean get() = baseUrl.isNotBlank() && model.isNotBlank() && (!provider.keyRequired || hasKey)
}

/**
 * Every provider the student set up, with its own key (kept in the encrypted vault) and its own URL and model, and which one is in
 * use. Switching provider never asks for a key again.
 */
class AiProvidersViewModel(private val container: AppContainer) : ViewModel() {

    private val vault = container.apiKeyVault
    private val profiles = container.aiProfiles
    private val refresh = MutableStateFlow(0)

    val rows: StateFlow<List<ProviderRow>> = kotlinx.coroutines.flow.combine(container.settingsState, refresh) { settings, _ -> build(settings) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), build(container.settingsState.value))

    private fun build(settings: skywa1ker404.absencium.data.settings.AppSettings?): List<ProviderRow> =
        skywa1ker404.absencium.data.ai.AiProvider.entries.mapNotNull { provider ->
            val active = settings?.aiProvider == provider && settings.aiConnectionSaved
            val saved = profiles.get(provider)
            if (!active && !vault.hasKey(provider) && saved == null) return@mapNotNull null
            val profile = profileFor(provider, settings)
            ProviderRow(provider, vault.hasKey(provider), vault.lastFour(provider), profile.baseUrl, profile.model, profile.style, active)
        }

    /** What is saved for [provider], or its defaults. The provider in use is read from the settings, which hold the truth for it. */
    fun profileFor(
        provider: skywa1ker404.absencium.data.ai.AiProvider,
        settings: skywa1ker404.absencium.data.settings.AppSettings? = container.settingsState.value,
    ): skywa1ker404.absencium.data.ai.AiProfile =
        if (settings != null && settings.aiProvider == provider && settings.aiConnectionSaved) {
            skywa1ker404.absencium.data.ai.AiProfile(provider, settings.aiBaseUrl.ifBlank { provider.defaultBaseUrl }, settings.aiModel, settings.effectiveAiStyle)
        } else {
            profiles.get(provider) ?: skywa1ker404.absencium.data.ai.AiProfile(provider, provider.defaultBaseUrl, "", provider.style)
        }

    fun keyHint(provider: skywa1ker404.absencium.data.ai.AiProvider): String? = if (vault.hasKey(provider)) vault.lastFour(provider) else null

    /**
     * Saves the provider (and its key when one was typed). With [use] it also becomes the provider in use; the first provider set up
     * always does. [onDone] gets null on success or a message.
     */
    fun save(
        provider: skywa1ker404.absencium.data.ai.AiProvider,
        baseUrl: String,
        model: String,
        style: skywa1ker404.absencium.data.ai.ApiStyle,
        key: String,
        use: Boolean,
        onDone: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            val failure = withContext(Dispatchers.IO) {
                if (key.isNotBlank() && !vault.save(provider, key)) {
                    "Could not save the key. See debug.log for details."
                } else {
                    profiles.save(skywa1ker404.absencium.data.ai.AiProfile(provider, baseUrl, model, style))
                    null
                }
            }
            if (failure == null) {
                val current = container.settingsState.value
                if (use || current == null || !current.aiConnectionSaved || current.aiProvider == provider) {
                    container.settingsRepository.setAiConnection(provider, baseUrl, model, style)
                }
                DebugLogger.i("AI", "Provider saved: ${provider.name} url=$baseUrl model=$model")
                refresh.update { it + 1 }
            }
            onDone(failure)
        }
    }

    /** Saves a typed key first, then runs [block] (fetching models and testing both need it in the vault). */
    fun withKey(provider: skywa1ker404.absencium.data.ai.AiProvider, key: String, block: () -> Unit) {
        viewModelScope.launch {
            if (key.isNotBlank()) withContext(Dispatchers.IO) { vault.save(provider, key) }
            refresh.update { it + 1 }
            block()
        }
    }

    /** Makes [provider] the one in use, with the URL and model saved for it. */
    fun use(provider: skywa1ker404.absencium.data.ai.AiProvider) {
        val profile = profileFor(provider)
        if (profile.baseUrl.isBlank() || profile.model.isBlank()) return
        viewModelScope.launch { container.settingsRepository.setAiConnection(provider, profile.baseUrl, profile.model, profile.style) }
    }

    /** Forgets the provider: its key and its saved URL and model. If it was in use, another complete provider takes over. */
    fun remove(provider: skywa1ker404.absencium.data.ai.AiProvider) {
        viewModelScope.launch {
            val wasActive = container.settingsState.value?.aiProvider == provider
            withContext(Dispatchers.IO) {
                vault.clear(provider)
                profiles.remove(provider)
            }
            refresh.update { it + 1 }
            if (wasActive) rows.value.firstOrNull { it.provider != provider && it.complete }?.let { use(it.provider) }
        }
    }

    /** Sends one tiny request to check the URL, key and model together. */
    fun test(connection: skywa1ker404.absencium.data.ai.AiConnection, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                container.aiClient.generate(connection, "Reply with the single word OK.", record = false)
                onResult(true, "Connected. ${connection.model} answered.")
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                onResult(false, skywa1ker404.absencium.data.ai.AI_FAILED_INLINE)
            }
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

    fun rename(subject: SubjectEntity, name: String) = perform { repository.rename(subject, name) }

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

// ---------------------------------------------------------------------------------------------
// Absencium AI permissions
// ---------------------------------------------------------------------------------------------

/** Which parts of the app's data Absencium AI may use. */
class AiPermissionsViewModel(private val container: AppContainer) : ViewModel() {
    val allowed: StateFlow<Set<skywa1ker404.absencium.data.ai.AiScope>> = container.aiPermissions.allowed
    fun set(scope: skywa1ker404.absencium.data.ai.AiScope, on: Boolean) = container.aiPermissions.set(scope, on)
}
