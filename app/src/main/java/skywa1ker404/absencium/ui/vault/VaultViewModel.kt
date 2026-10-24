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
import skywa1ker404.absencium.data.share.PreparedShare
import skywa1ker404.absencium.data.share.ShareFormat
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

    private val _pendingShare = MutableStateFlow<PreparedShare?>(null)

    /** A finished export waiting to be handed to the Sharesheet; the screen opens it and calls [openShare]. */
    val pendingShare: StateFlow<PreparedShare?> = _pendingShare.asStateFlow()

    /** Builds a file from [notes] in [format] (several notes, or a ZIP, make a ZIP). The result appears in [pendingShare]. */
    fun share(notes: List<NoteEntity>, format: ShareFormat) {
        viewModelScope.launch {
            val snapshot = data.value
            val result = container.shareService.prepare(notes, { id -> snapshot?.attachmentsOf(id).orEmpty() }, format)
            result.onSuccess { _pendingShare.value = it }
                .onFailure { _message.value = it.message ?: "The export could not be prepared." }
        }
    }

    /** Opens the Sharesheet for [share] and clears it. A problem, or photos that were left out, is shown as the message. */
    fun openShare(share: PreparedShare) {
        _pendingShare.value = null
        val problem = container.shareService.share(share)
        if (problem != null) _message.value = problem else if (share.note != null) _message.value = share.note
    }

    /** Several entries at once (the Vault's multi-select). One undo brings all of them back. */
    fun deleteNotes(notes: List<NoteEntity>) = perform("delete the entries") {
        val trashed = mutableListOf<Long>()
        for (note in notes) {
            val files = repository.attachmentsFor(note.id).map { it.filePath }
            if (repository.deleteNote(note)) {
                withContext(Dispatchers.IO) {
                    files.forEach {
                        container.attachmentStore.delete(it)
                        ImageTools.forget(it)
                    }
                }
            } else {
                trashed += note.id
            }
        }
        if (trashed.isNotEmpty()) {
            val text = if (trashed.size == 1) "Entry moved to the Trash" else "${trashed.size} entries moved to the Trash"
            container.undoBus.offer(text) { trashed.forEach { repository.restoreNote(it) } }
        }
    }

    fun setNotesStatus(notes: List<NoteEntity>, status: NoteStatus) = perform("update the entries") {
        notes.forEach { repository.setNoteStatus(it, status) }
    }

    /** Moves entries to another subject. Their photos follow, because they belong to the entry. */
    fun moveNotes(notes: List<NoteEntity>, subjectId: Long) = perform("move the entries") {
        notes.forEach { repository.saveNote(it.id, subjectId, it.title, it.bodyText, it.status) }
    }

    /** Adds photos picked in the editor to an existing entry. */
    /** Adds photos to a note. [onAdded] gets the new attachment ids, in order, so the editor can put them on the page. */
    fun addPhotos(noteId: Long, uris: List<Uri>, onAdded: (List<Long>) -> Unit = {}) = perform("add the photos") {
        var skipped = 0
        val added = ArrayList<Long>()
        for (uri in uris) {
            val stored = container.attachmentStore.import(uri)
            if (stored == null || !stored.mimeType.startsWith("image/")) {
                stored?.let { container.attachmentStore.delete(it.path) }
                skipped++
                continue
            }
            added += repository.addAttachment(
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
        if (added.isNotEmpty()) onAdded(added)
        if (skipped > 0) _message.value = "$skipped ${if (skipped == 1) "file was" else "files were"} not a readable photo."
    }

    /** Removes a photo (or other attachment) from an entry and deletes its file. */
    /** What came back from an AI request made in the editor: the text, or an error, or that no AI is set up yet. */
    data class AiOutcome(val text: String? = null, val error: String? = null, val setupMissing: Boolean = false)

    /** Sends [request] to the AI the user set up and gives the answer to [onResult] on the main thread. Returns the job so it can be cancelled. */
    fun askAi(request: String, onResult: (AiOutcome) -> Unit): kotlinx.coroutines.Job {
        val settings = container.settingsState.value
        if (settings == null || !skywa1ker404.absencium.ui.settings.aiReady(settings, container.apiKeyVault.hasKey())) {
            onResult(AiOutcome(setupMissing = true))
            return viewModelScope.launch { }
        }
        return viewModelScope.launch {
            try {
                val connection = skywa1ker404.absencium.data.ai.AiConnection(settings.aiProvider, settings.effectiveAiStyle, settings.aiBaseUrl, settings.aiModel)
                val reply = container.aiClient.generate(connection, request)
                onResult(if (reply.isBlank()) AiOutcome(error = "The model sent back an empty answer.") else AiOutcome(text = reply.trim()))
            } catch (e: CancellationException) {
                throw e
            } catch (e: skywa1ker404.absencium.data.ai.AiException) {
                onResult(AiOutcome(error = e.message ?: "The AI request failed."))
            } catch (e: Exception) {
                DebugLogger.e("Vault", "The AI request failed", e)
                onResult(AiOutcome(error = "Something went wrong: ${e.message ?: e.javaClass.simpleName}"))
            }
        }
    }

    // ---- Camera, recording and audio files inside the editor ----

    private var cameraFile: java.io.File? = null
    private val recorder by lazy { skywa1ker404.absencium.data.audio.AudioRecorder(container.appContext) }
    private var recordingFile: java.io.File? = null

    /** What the editor's recorder is doing: idle, recording, or a finished take waiting to be added. */
    data class RecorderState(val recording: Boolean = false, val takePath: String? = null, val takeMs: Long = 0L)

    private val _recorder = MutableStateFlow(RecorderState())
    val recorderState: StateFlow<RecorderState> = _recorder.asStateFlow()

    /** The address the camera app writes the next photo to. */
    fun prepareCameraFile(): Uri {
        val file = container.attachmentStore.newFile("jpg")
        cameraFile = file
        return container.attachmentStore.uriFor(file)
    }

    /** The camera returned: a real photo becomes a picture of the note ([onAdded] gets its attachment), anything else is thrown away. */
    fun cameraFinished(noteId: Long, taken: Boolean, onAdded: (AttachmentEntity) -> Unit) {
        val file = cameraFile ?: return
        cameraFile = null
        if (!taken || file.length() == 0L) {
            container.attachmentStore.delete(file.absolutePath)
            return
        }
        perform("add the photo") {
            val id = repository.addAttachment(
                AttachmentEntity(noteId = noteId, kind = AttachmentKind.IMAGE, filePath = file.absolutePath, displayName = "Photo.jpg", mimeType = "image/jpeg", sizeBytes = file.length()),
            )
            val saved = repository.attachmentsFor(noteId).firstOrNull { it.id == id }
            if (saved != null) onAdded(saved)
        }
    }

    /** Adds audio files picked on the phone to a note; [onAdded] gets the new attachment ids. */
    fun addAudioFiles(noteId: Long, uris: List<Uri>, onAdded: (List<Long>) -> Unit) = perform("add the audio") {
        val added = ArrayList<Long>()
        for (uri in uris) {
            val stored = container.attachmentStore.import(uri)
            if (stored == null || !stored.mimeType.startsWith("audio/")) {
                stored?.let { container.attachmentStore.delete(it.path) }
                _message.value = "That file is not audio."
                continue
            }
            added += repository.addAttachment(
                AttachmentEntity(noteId = noteId, kind = AttachmentKind.AUDIO, filePath = stored.path, displayName = stored.displayName, mimeType = stored.mimeType, sizeBytes = stored.sizeBytes),
            )
        }
        if (added.isNotEmpty()) onAdded(added)
    }

    fun startRecording() {
        if (_recorder.value.recording) return
        _recorder.value.takePath?.let { container.attachmentStore.delete(it) }
        val file = container.attachmentStore.newFile("m4a")
        val problem = recorder.start(file)
        if (problem != null) {
            _message.value = problem
            _recorder.value = RecorderState()
            return
        }
        recordingFile = file
        _recorder.value = RecorderState(recording = true)
    }

    fun stopRecording() {
        val file = recordingFile ?: return
        val length = recorder.stop(file)
        recordingFile = null
        if (length == null) {
            _message.value = "That recording was too short to keep."
            _recorder.value = RecorderState()
        } else {
            _recorder.value = RecorderState(takePath = file.absolutePath, takeMs = length)
        }
    }

    fun recordingElapsed(): Long = recorder.elapsedMs()

    fun micDenied() {
        _message.value = "Allow the microphone in Android settings to record."
    }

    /** Throws the running or finished take away. */
    fun discardRecording() {
        recordingFile?.let { recorder.abort(it) }
        recordingFile = null
        _recorder.value.takePath?.let { container.attachmentStore.delete(it) }
        _recorder.value = RecorderState()
    }

    /** Puts the finished take into the note as an audio attachment; [onAdded] gets its id. */
    fun addRecording(noteId: Long, onAdded: (Long) -> Unit) {
        val take = _recorder.value
        val path = take.takePath ?: return
        _recorder.value = RecorderState()
        perform("add the recording") {
            val file = java.io.File(path)
            val id = repository.addAttachment(
                AttachmentEntity(noteId = noteId, kind = AttachmentKind.AUDIO, filePath = path, displayName = "Recording.m4a", mimeType = "audio/mp4", sizeBytes = file.length(), durationMs = take.takeMs),
            )
            onAdded(id)
        }
    }

    override fun onCleared() {
        recordingFile?.let { recorder.abort(it) }
        super.onCleared()
    }

    /** A fresh file for the crop editor to write into. */
    fun newPictureFile(): java.io.File = container.attachmentStore.newFile("jpg")

    /** Swaps a picture for its edited (cropped, turned) version; the old file is deleted once the new one is recorded. */
    fun replaceAttachmentFile(attachment: AttachmentEntity, file: java.io.File) = perform("save the edited picture") {
        repository.updateAttachment(attachment.copy(filePath = file.absolutePath, mimeType = "image/jpeg", sizeBytes = file.length()))
        withContext(Dispatchers.IO) { container.attachmentStore.delete(attachment.filePath) }
        ImageTools.forget(attachment.filePath)
    }

    /**
     * Saves a doodle as a PNG attachment of the note. A new doodle gets a new attachment; an edited one rewrites its own file.
     * [onSaved] gets the attachment id.
     */
    fun saveDoodle(noteId: Long, existing: AttachmentEntity?, bitmap: android.graphics.Bitmap, onSaved: (Long) -> Unit) = perform("save the doodle") {
        val id = withContext(Dispatchers.IO) {
            val file = container.attachmentStore.newFile("png")
            file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            if (existing == null) {
                repository.addAttachment(
                    AttachmentEntity(
                        noteId = noteId,
                        kind = AttachmentKind.SKETCH,
                        filePath = file.absolutePath,
                        displayName = "Doodle.png",
                        mimeType = "image/png",
                        sizeBytes = file.length(),
                    ),
                )
            } else {
                repository.updateAttachment(existing.copy(filePath = file.absolutePath, mimeType = "image/png", sizeBytes = file.length()))
                container.attachmentStore.delete(existing.filePath)
                ImageTools.forget(existing.filePath)
                existing.id
            }
        }
        onSaved(id)
    }

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
