package skywa1ker404.absencium.ui.vault

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import skywa1ker404.absencium.AppContainer
import skywa1ker404.absencium.data.db.AttachmentEntity
import skywa1ker404.absencium.data.db.AttachmentKind
import skywa1ker404.absencium.data.db.GuideEntity
import skywa1ker404.absencium.data.db.GuideType
import skywa1ker404.absencium.data.db.NoteEntity
import skywa1ker404.absencium.data.db.NoteStatus
import skywa1ker404.absencium.data.db.SubjectEntity
import skywa1ker404.absencium.data.storage.NotesStorage
import skywa1ker404.absencium.data.storage.sanitizeFileName
import skywa1ker404.absencium.data.vault.VaultData
import skywa1ker404.absencium.logging.DebugLogger
import skywa1ker404.absencium.ui.vault.entry.ImageTools
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Largest text file that is imported into a note. */
private const val MAX_TEXT_IMPORT_BYTES = 2 * 1024 * 1024

class VaultViewModel(private val container: AppContainer) : ViewModel() {

    private val repository = container.vaultRepository

    /** Null until the first read from the database finishes. */
    val data: StateFlow<VaultData?> = repository.data
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** Marks the notes of an absent day (ISO date text) as collected for a subject. Does nothing for a blank date. */
    suspend fun markNotesCollected(subjectId: Long, isoDate: String) {
        if (isoDate.isBlank()) return
        try {
            container.calendarRepository.setNotesCollected(listOf(subjectId), java.time.LocalDate.parse(isoDate), true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            DebugLogger.e("Vault", "Could not mark the notes as collected", e)
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    suspend fun getNote(id: Long): NoteEntity? =
        try {
            repository.getNote(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            DebugLogger.e("Vault", "Could not read note $id", e)
            _message.value = "Could not open the note. See debug.log for details."
            null
        }

    /** Saves a note and returns its id, or null if saving failed (the reason is logged and shown). */
    suspend fun saveNote(id: Long, subjectId: Long, title: String, body: String, status: NoteStatus, tags: String? = null): Long? =
        try {
            repository.saveNote(id, subjectId, title, body, status, tags)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            DebugLogger.e("Vault", "Could not save note $id", e)
            _message.value = "Could not save the note. See debug.log for details."
            null
        }

    /** Saves from a screen that is being closed, so the write must outlive the screen. */
    fun saveInBackground(id: Long, subjectId: Long, title: String, body: String, status: NoteStatus, tags: String? = null) =
        perform("save the note") { repository.saveNote(id, subjectId, title, body, status, tags) }

    fun setNoteStatus(note: NoteEntity, status: NoteStatus) = perform("update the note") {
        repository.setNoteStatus(note, status)
    }

    fun deleteNote(note: NoteEntity) = perform("delete the note") {
        // A draft is thrown away at once, files included. Anything else goes to the Trash with its photos kept, so the
        // undo (or a later restore) finds everything; the files are removed when the Trash is emptied.
        val files = repository.attachmentsFor(note.id).map { it.filePath }
        val wasDraft = repository.deleteNote(note)
        if (wasDraft) {
            withContext(Dispatchers.IO) {
                files.forEach {
                    container.attachmentStore.delete(it)
                    ImageTools.forget(it)
                }
            }
        } else {
            container.undoBus.offer("Entry moved to the Trash") { repository.restoreNote(note.id) }
        }
    }

    /** Adds photos picked in the editor to an existing entry. */
    fun addPhotos(noteId: Long, uris: List<Uri>) = perform("add the photos") {
        var skipped = 0
        for (uri in uris) {
            val stored = container.attachmentStore.import(uri)
            if (stored == null || !stored.mimeType.startsWith("image/")) {
                stored?.let { container.attachmentStore.delete(it.path) }
                skipped++
                continue
            }
            repository.addAttachment(
                AttachmentEntity(
                    noteId = noteId,
                    kind = AttachmentKind.IMAGE,
                    filePath = stored.path,
                    displayName = stored.displayName,
                    mimeType = stored.mimeType,
                    sizeBytes = stored.sizeBytes,
                ),
            )
        }
        if (skipped > 0) _message.value = "$skipped ${if (skipped == 1) "file was" else "files were"} not a readable photo."
    }

    /** Removes a photo (or other attachment) from an entry and deletes its file. */
    fun deleteAttachment(attachment: AttachmentEntity) = perform("remove the attachment") {
        repository.deleteAttachment(attachment)
        withContext(Dispatchers.IO) { container.attachmentStore.delete(attachment.filePath) }
        ImageTools.forget(attachment.filePath)
    }

    fun discardDraft(id: Long) = perform("remove the draft") { repository.deleteNoteById(id) }

    fun deleteGuide(guide: GuideEntity) = perform("delete the guide") {
        withContext(Dispatchers.IO) {
            // Remove the stored copy too, when it is in the notes folder.
            container.notesStorage.rootFolder()
                ?.childFolder(NotesStorage.GUIDES_FOLDER)
                ?.delete(guide.fileName)
        }
        repository.deleteGuide(guide)
    }

    /**
     * Copies a picked PDF into the notes folder (Guides and Question Banks) and records it.
     * Returns null on success, or a message explaining what went wrong.
     */
    suspend fun importGuide(source: Uri, title: String, subject: SubjectEntity, type: GuideType): String? =
        withContext(Dispatchers.IO) {
            try {
                val root = container.notesStorage.rootFolder()
                    ?: return@withContext "Choose a notes folder first (Settings, Storage and Data)."
                val folder = root.childFolder(NotesStorage.GUIDES_FOLDER)
                    ?: return@withContext "Could not open the Guides folder. See debug.log for details."
                val existing = folder.listFileNames().toSet()
                val base = sanitizeFileName("${subject.name} - ${title.trim()}")
                var name = "$base.pdf"
                var copy = 2
                while (name in existing) name = "$base ($copy).pdf".also { copy++ }

                val input = container.appContext.contentResolver.openInputStream(source)
                    ?: return@withContext "Could not read the selected file."
                val output = folder.openOutput(name, "application/pdf")
                    ?: return@withContext "Could not write to the notes folder. See debug.log for details."
                input.use { i -> output.use { o -> i.copyTo(o) } }

                val location = folder.locationOf(name)
                    ?: return@withContext "The file was copied but could not be found again. See debug.log for details."
                repository.addGuide(subject.id, name, location, type)
                DebugLogger.i("Vault", "Imported guide \"$name\" for ${subject.name}")
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DebugLogger.e("Vault", "Could not import the guide", e)
                "Could not import the PDF: ${e.message ?: e.javaClass.simpleName}"
            }
        }

    /** Reads a small text or Markdown file. Returns the text, or null after setting a message. */
    suspend fun readTextFile(source: Uri): String? =
        withContext(Dispatchers.IO) {
            try {
                val stream = container.appContext.contentResolver.openInputStream(source)
                if (stream == null) {
                    _message.value = "Could not read the selected file."
                    return@withContext null
                }
                stream.use { input ->
                    val buffer = java.io.ByteArrayOutputStream()
                    val chunk = ByteArray(8 * 1024)
                    var total = 0
                    while (true) {
                        val read = input.read(chunk)
                        if (read < 0) break
                        total += read
                        if (total > MAX_TEXT_IMPORT_BYTES) {
                            _message.value = "That file is larger than 2 MB, so it was not imported."
                            return@withContext null
                        }
                        buffer.write(chunk, 0, read)
                    }
                    buffer.toString(Charsets.UTF_8.name())
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DebugLogger.e("Vault", "Could not read the text file", e)
                _message.value = "Could not read the selected file. See debug.log for details."
                null
            }
        }

    private fun perform(action: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DebugLogger.e("Vault", "Could not $action", e)
                _message.value = "Could not $action. See debug.log for details."
            }
        }
    }
}
