package skywa1ker404.absencium.ui.vault.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import skywa1ker404.absencium.AppContainer
import skywa1ker404.absencium.data.db.AttachmentEntity
import skywa1ker404.absencium.data.db.AttachmentKind
import skywa1ker404.absencium.data.db.AttendanceEntity
import skywa1ker404.absencium.data.db.AttendanceStatus
import skywa1ker404.absencium.data.db.NoteKind
import skywa1ker404.absencium.data.db.NoteStatus
import skywa1ker404.absencium.data.db.SubjectEntity
import skywa1ker404.absencium.logging.DebugLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import android.graphics.Bitmap
import android.net.Uri
import skywa1ker404.absencium.data.ai.AiConnection
import skywa1ker404.absencium.data.ai.AiException
import skywa1ker404.absencium.data.ai.AiImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import skywa1ker404.absencium.data.ai.ApiStyle
import skywa1ker404.absencium.data.ai.YouTubeClient
import skywa1ker404.absencium.data.ai.YouTubeException
import skywa1ker404.absencium.data.ai.YouTubeLinks
import skywa1ker404.absencium.data.ai.YouTubeVideo
import skywa1ker404.absencium.ui.settings.aiUsable
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

/** The steps of the Add Entry popup. */
enum class EntryStage { SUBJECT, ACTIONS, PICTURES, YOUTUBE, AUDIO, SOLVE, GENERATE }

/** Whether the AI is writing notes from the video. */
enum class YouTubeStep { IDLE, NOTES }

/** A photo that was picked or taken but is not saved into an entry yet. */
data class PendingPicture(val id: Long, val path: String, val name: String, val mimeType: String, val sizeBytes: Long)

data class AddEntryState(
    val stage: EntryStage = EntryStage.SUBJECT,
    val subjectId: Long = 0L,
    val date: LocalDate? = null,
    val title: String = "",
    /** True when the pictures step was opened with the camera button. */
    val cameraFirst: Boolean = false,
    val pictures: List<PendingPicture> = emptyList(),
    val busy: Boolean = false,
    /** What a long save is doing right now (shown with a Cancel button), or null. */
    val busyStep: String? = null,
    /** 0..1 while a file uploads to the AI, null when there is nothing to measure. */
    val busyProgress: Float? = null,
    val message: String? = null,
    val youtubeUrl: String = "",
    val video: YouTubeVideo? = null,
    val videoThumb: Bitmap? = null,
    val videoLoading: Boolean = false,
    val videoError: String? = null,
    val youtubeStep: YouTubeStep = YouTubeStep.IDLE,
    /** A transcript the user pasted; used for notes with any provider and saved under the notes. */
    val pastedTranscript: String = "",
    /** True when the user tried to use AI before a provider, model and key were saved. */
    val aiSetupMissing: Boolean = false,
    val youtubeError: String? = null,
    /** Text extraction from the pending photos (Upload a Pic / Camera). */
    val extractStep: ExtractStep = ExtractStep.IDLE,
    val extractedText: String = "",
    val extractError: String? = null,
    val extractSetupMissing: Boolean = false,
    /** Text the user accepted; saved as the entry's text together with the photos. */
    val acceptedText: String? = null,
    /** The question or topic typed for Solve Homework and Generate AI Notes, and whether the AI is working on it. */
    val aiPrompt: String = "",
    val aiRunning: Boolean = false,
    val aiError: String? = null,
    val aiSetupMissing2: Boolean = false,
    /** What the AI is doing now ("Writing", "Finding diagrams") and the choices made for the answer. */
    val aiStep: String = "",
    /** Audio Note: also turn the recording into text with the user's AI when it is saved. */
    val transcribe: Boolean = false,
    val noteLength: skywa1ker404.absencium.data.ai.NoteLength = skywa1ker404.absencium.data.ai.NoteLength.MODERATE,
    val noteTemplate: skywa1ker404.absencium.data.ai.NoteTemplate = skywa1ker404.absencium.data.ai.NoteTemplate.FULL,
    val syllabusNodeId: Long? = null,
    val courseInfo: String = "",
    val genSearchDiagrams: Boolean = false,
    val genDrawDiagrams: Boolean = false,
    val genTables: Boolean = true,
    val genPyq: Boolean = false,
    val genAssignment: Boolean = false,
    /** Audio Note: a recording in progress or waiting to be saved. */
    val recording: Boolean = false,
    val recordedPath: String? = null,
    val recordedMs: Long = 0L,
)

/** Where the photo text extraction is: nothing yet, running, or waiting for the user to keep or cancel the result. */
enum class ExtractStep { IDLE, RUNNING, REVIEW }

class AddEntryViewModel(private val container: AppContainer) : ViewModel() {

    private val store = container.attachmentStore

    val subjects: StateFlow<List<SubjectEntity>> = container.database.subjectDao().observeAll()
        .map { list -> list.filter { it.isActive } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val absences: StateFlow<List<AttendanceEntity>> = container.database.attendanceDao().observeAll()
        .map { list -> list.filter { it.status == AttendanceStatus.ABSENT } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _state = MutableStateFlow(AddEntryState())
    val state: StateFlow<AddEntryState> = _state.asStateFlow()

    private var nextPictureId = 1L

    private val youtube = YouTubeClient()
    private var detailsJob: Job? = null
    private var pipelineJob: Job? = null
    private var extractJob: Job? = null

    /** The file the camera is currently writing into. */
    private var cameraFile: File? = null

    /** Starts a fresh popup. With a subject and a date the first two steps are skipped. */
    fun begin(subjectId: Long, date: LocalDate?) {
        discardPictures(_state.value.pictures)
        _state.value = AddEntryState(
            stage = if (subjectId != 0L && date != null) EntryStage.ACTIONS else EntryStage.SUBJECT,
            subjectId = subjectId,
            date = date,
        )
    }

    /** Ends the popup without saving: pending photos are deleted from storage. */
    fun cancel() {
        detailsJob?.cancel()
        pipelineJob?.cancel()
        extractJob?.cancel()
        aiJob?.cancel()
        discardRecording()
        discardPictures(_state.value.pictures)
        cameraFile?.let { store.delete(it.absolutePath) }
        cameraFile = null
        _state.value = AddEntryState()
    }

    fun chooseSubject(id: Long) = _state.update { it.copy(subjectId = id, date = if (it.subjectId == id) it.date else null) }

    fun chooseDate(date: LocalDate?) = _state.update { it.copy(date = date) }

    fun next() = _state.update { if (it.subjectId != 0L) it.copy(stage = EntryStage.ACTIONS) else it }

    fun toStage(stage: EntryStage) = _state.update { it.copy(stage = stage, message = null) }

    fun openPictures(camera: Boolean) = _state.update { it.copy(stage = EntryStage.PICTURES, cameraFirst = camera, message = null) }

    fun setTitle(title: String) = _state.update { it.copy(title = title) }

    fun clearMessage() = _state.update { it.copy(message = null) }

    fun showMessage(text: String) = _state.update { it.copy(message = text) }

    /** Copies the picked photos into the app's storage so they can be edited and survive the picker closing. */
    fun addPictures(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            val added = mutableListOf<PendingPicture>()
            var failed = 0
            for (uri in uris) {
                val stored = store.import(uri)
                if (stored == null || !stored.mimeType.startsWith("image/")) {
                    stored?.let { store.delete(it.path) }
                    failed++
                } else {
                    added += PendingPicture(nextPictureId++, stored.path, stored.displayName, stored.mimeType, stored.sizeBytes)
                }
            }
            _state.update {
                it.copy(
                    busy = false,
                    pictures = it.pictures + added,
                    message = if (failed > 0) "$failed ${if (failed == 1) "file was" else "files were"} not a readable photo and was skipped." else null,
                )
            }
        }
    }

    /** Makes the empty file the camera will write the next photo into. */
    fun prepareCameraFile(): Uri {
        val file = store.newFile("jpg")
        cameraFile = file
        return store.uriFor(file)
    }

    fun cameraFinished(taken: Boolean) {
        val file = cameraFile ?: return
        cameraFile = null
        if (taken && file.length() > 0) {
            val picture = PendingPicture(nextPictureId++, file.absolutePath, "Photo.jpg", "image/jpeg", file.length())
            _state.update { it.copy(pictures = it.pictures + picture) }
        } else {
            store.delete(file.absolutePath)
        }
    }

    /** Swaps a photo for its cropped version. The original file is deleted once the new one is in place. */
    fun replacePicture(id: Long, file: File) {
        val old = _state.value.pictures.firstOrNull { it.id == id }
        if (old == null) {
            store.delete(file.absolutePath)
            return
        }
        val updated = old.copy(path = file.absolutePath, mimeType = "image/jpeg", sizeBytes = file.length())
        _state.update { it.copy(pictures = it.pictures.map { p -> if (p.id == id) updated else p }, acceptedText = null, extractedText = "", extractStep = ExtractStep.IDLE) }
        store.delete(old.path)
        ImageTools.forget(old.path)
    }

    /** A fresh file for the crop editor to write into. */
    fun newPictureFile(): File = store.newFile("jpg")

    fun dismissExtractSetup() = _state.update { it.copy(extractSetupMissing = false) }

    fun cancelExtraction() {
        extractJob?.cancel()
        _state.update { it.copy(extractStep = ExtractStep.IDLE, extractedText = "", extractError = null, extractSetupMissing = false) }
    }

    fun setExtractedText(text: String) = _state.update { it.copy(extractedText = text) }

    /** Keeps the reviewed text; it is saved with the photos. The photos themselves are kept either way. */
    fun acceptExtraction() = _state.update {
        it.copy(acceptedText = it.extractedText.trim().ifEmpty { null }, extractStep = ExtractStep.IDLE, extractedText = "")
    }

    fun discardAcceptedText() = _state.update { it.copy(acceptedText = null) }

    /**
     * Sends the pending photos to the AI model and shows the text it reads. Nothing is saved until the user accepts.
     * Without a saved provider, model and key the setup prompt is shown instead.
     */
    fun extractText() {
        val current = _state.value
        if (current.pictures.isEmpty() || current.extractStep == ExtractStep.RUNNING) return
        val settings = container.settingsState.value ?: return
        if (!aiUsable(settings, container.activeKeySaved())) {
            _state.update { it.copy(extractSetupMissing = true, extractError = null) }
            return
        }
        _state.update { it.copy(extractStep = ExtractStep.RUNNING, extractError = null, extractSetupMissing = false) }
        extractJob = viewModelScope.launch {
            try {
                val connection = AiConnection(settings.aiProvider, settings.effectiveAiStyle, settings.aiBaseUrl, settings.aiModel)
                val images = withContext(Dispatchers.IO) {
                    current.pictures.take(MAX_EXTRACT_PHOTOS).mapNotNull { picture ->
                        val bitmap = ImageTools.decode(picture.path, EXTRACT_EDGE) ?: return@mapNotNull null
                        val bytes = java.io.ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }.toByteArray()
                        AiImage("image/jpeg", android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP))
                    }
                }
                if (images.isEmpty()) throw AiException("The photos could not be read from storage.")
                val reply = container.aiClient.generate(connection, extractPrompt(images.size), images)
                if (reply.isBlank()) throw AiException("The model did not find any text in the photo${if (images.size > 1) "s" else ""}.")
                _state.update { it.copy(extractStep = ExtractStep.REVIEW, extractedText = reply.trim()) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: AiException) {
                _state.update { it.copy(extractStep = ExtractStep.IDLE, extractError = failureText(skywa1ker404.absencium.data.ai.AI_FAILED_INLINE)) }
            } catch (e: Exception) {
                DebugLogger.e("Extract", "Text extraction failed", e)
                _state.update { it.copy(extractStep = ExtractStep.IDLE, extractError = failureText(e.message ?: e.javaClass.simpleName)) }
            }
        }
    }

    fun aiAvailable(): Boolean = container.textExtractor.aiReady()

    private fun failureText(reason: String?): String =
        "Could not extract the text: ${reason ?: "unknown error"}. Check that the chosen model can read images, then try again, or save the photos without the text."

    private fun extractPrompt(count: Int): String =
        "Read all the text in the attached ${if (count == 1) "photo" else "$count photos, in order"}. " +
            "Return only the text, keeping its structure (headings, lists, line breaks, formulas written as plain text). " +
            (if (count > 1) "Start each photo with a line \"Photo N\". " else "") +
            "Do not add commentary. If a part is unreadable write [unclear]. If there is no text at all, reply with nothing."

    fun removePicture(id: Long) {
        val removed = _state.value.pictures.firstOrNull { it.id == id } ?: return
        store.delete(removed.path)
        ImageTools.forget(removed.path)
        _state.update { it.copy(pictures = it.pictures.filterNot { p -> p.id == id }) }
    }

    /** Saves the pending photos as one entry. [onSaved] gets the new entry's id. */
    fun savePictures(subjectName: String, onSaved: (Long) -> Unit) {
        val current = _state.value
        if (current.busy || current.pictures.isEmpty() || current.subjectId == 0L) return
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            try {
                val title = current.title.trim().ifEmpty { "$subjectName photos" }
                val now = System.currentTimeMillis()
                val attachments = current.pictures.mapIndexed { index, picture ->
                    AttachmentEntity(
                        noteId = 0L,
                        kind = AttachmentKind.IMAGE,
                        filePath = picture.path,
                        displayName = picture.name,
                        mimeType = picture.mimeType,
                        sizeBytes = picture.sizeBytes,
                        createdAt = now + index,
                    )
                }
                val id = container.vaultRepository.createEntry(
                    subjectId = current.subjectId,
                    title = title,
                    kind = NoteKind.PICTURES,
                    body = current.acceptedText.orEmpty(),
                    absentDate = current.date,
                    attachments = attachments,
                )
                putPicturesOnPage(id, current.subjectId, title, current.acceptedText.orEmpty())
                current.date?.let { container.calendarRepository.setNotesCollected(listOf(current.subjectId), it, true) }
                DebugLogger.i("Vault", "Saved entry $id with ${attachments.size} photo(s)")
                // The files now belong to the entry, so they must not be deleted when the popup closes.
                _state.value = AddEntryState()
                onSaved(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DebugLogger.e("Vault", "Could not save the photo entry", e)
                _state.update { it.copy(busy = false, message = "Could not save the entry: ${e.message ?: e.javaClass.simpleName}") }
            }
        }
    }

    /** Pictures of a new entry go straight onto its page, above any text, instead of waiting in the strip under the note. */
    private suspend fun putPicturesOnPage(id: Long, subjectId: Long, title: String, body: String) {
        val images = container.vaultRepository.attachmentsFor(id).filter { it.kind == AttachmentKind.IMAGE }.sortedBy { it.createdAt }
        if (images.isEmpty()) return
        val blocks = images.joinToString("\n") {
            skywa1ker404.absencium.ui.vault.markdown.NoteBlocks.token(skywa1ker404.absencium.ui.vault.markdown.EmbedKind.IMAGE, it.id, 100)
        }
        container.vaultRepository.saveNote(id, subjectId, title, blocks + "\n" + (if (body.isBlank()) "" else "\n" + body), NoteStatus.ACTIVE)
    }

    fun openYouTube() = _state.update { it.copy(stage = EntryStage.YOUTUBE, message = null) }

    /** Reads the pasted link. A valid link fetches the video's title, channel and thumbnail after a short pause. */
    fun setYouTubeUrl(text: String) {
        detailsJob?.cancel()
        val id = YouTubeLinks.videoId(text)
        _state.update {
            it.copy(
                youtubeUrl = text,
                video = if (it.video?.id == id) it.video else null,
                videoThumb = if (it.video?.id == id) it.videoThumb else null,
                videoError = if (text.isBlank() || id != null) null else "That does not look like a YouTube video link.",
                videoLoading = id != null && it.video?.id != id,
                youtubeError = null,
                aiSetupMissing = false,
            )
        }
        if (id == null || _state.value.video?.id == id) return
        detailsJob = viewModelScope.launch {
            delay(400)
            try {
                val video = youtube.details(text)
                val thumb = youtube.thumbnail(video.thumbnailUrl)
                _state.update { it.copy(video = video, videoThumb = thumb, videoLoading = false, videoError = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: YouTubeException) {
                _state.update { it.copy(videoLoading = false, videoError = e.message) }
            } catch (e: Exception) {
                DebugLogger.e("YouTube", "Could not read the video details", e)
                _state.update { it.copy(videoLoading = false, videoError = "Could not read the video details.") }
            }
        }
    }

    fun setPastedTranscript(text: String) = _state.update { it.copy(pastedTranscript = text, youtubeError = null) }

    /** True when the chosen AI provider is Gemini, which can read a public YouTube link without a transcript. */
    fun canWatchLink(): Boolean = container.settingsState.value?.effectiveAiStyle == ApiStyle.GEMINI

    fun dismissAiSetup() = _state.update { it.copy(aiSetupMissing = false) }

    /**
     * Has the AI write study notes and saves them as a new entry. The notes come from the pasted transcript when there is
     * one (any provider), otherwise from the video link itself (Gemini only). With [skipAi] the pasted transcript alone is
     * saved. Nothing is thrown away: the saved entry keeps the transcript under the notes. [onSaved] gets the new entry's id.
     */
    fun transcribe(skipAi: Boolean, onSaved: (Long) -> Unit) {
        val current = _state.value
        val video = current.video ?: return
        if (current.youtubeStep != YouTubeStep.IDLE || current.subjectId == 0L) return
        val settings = container.settingsState.value ?: return
        val transcript = current.pastedTranscript.trim().ifEmpty { null }
        val ready = aiUsable(settings, container.activeKeySaved())
        if (!ready && !skipAi) {
            _state.update { it.copy(aiSetupMissing = true, youtubeError = null) }
            return
        }
        _state.update { it.copy(youtubeStep = YouTubeStep.NOTES, aiSetupMissing = false, youtubeError = null) }
        pipelineJob = viewModelScope.launch {
            try {
                val connection = AiConnection(settings.aiProvider, settings.effectiveAiStyle, settings.aiBaseUrl, settings.aiModel)
                var notes: String? = null
                if (!skipAi) {
                    notes = when {
                        transcript != null -> container.aiClient.generate(connection, notesPrompt(video, transcript))
                        connection.style == ApiStyle.GEMINI ->
                            container.aiClient.generate(connection, notesPromptFromVideo(video), videoUrl = video.url)
                        else -> throw YouTubeException(NEEDS_TRANSCRIPT)
                    }
                } else if (transcript == null) {
                    throw YouTubeException(NEEDS_TRANSCRIPT)
                }
                val body = buildString {
                    append("# ").append(video.title).append("\n")
                    append("Source: ").append(video.url)
                    if (video.channel.isNotBlank()) append(" (").append(video.channel).append(")")
                    append("\n\n")
                    if (notes != null) append(notes.trim()).append("\n\n")
                    if (transcript != null) append("---\n\n## Transcript\n\n").append(transcript).append("\n")
                }
                val id = container.vaultRepository.createEntry(
                    subjectId = current.subjectId,
                    title = video.title.take(120),
                    kind = if (notes != null) NoteKind.AI_NOTES else NoteKind.TEXT,
                    body = body,
                    absentDate = current.date,
                    attachments = emptyList(),
                )
                current.date?.let { container.calendarRepository.setNotesCollected(listOf(current.subjectId), it, true) }
                DebugLogger.i("YouTube", "Saved entry $id from video ${video.id}")
                _state.value = AddEntryState()
                onSaved(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: AiException) {
                _state.update { it.copy(youtubeStep = YouTubeStep.IDLE, youtubeError = skywa1ker404.absencium.data.ai.AI_FAILED_INLINE) }
            } catch (e: YouTubeException) {
                _state.update { it.copy(youtubeStep = YouTubeStep.IDLE, youtubeError = e.message) }
            } catch (e: Exception) {
                DebugLogger.e("YouTube", "The notes pipeline failed", e)
                _state.update { it.copy(youtubeStep = YouTubeStep.IDLE, youtubeError = "Something went wrong: ${e.message ?: e.javaClass.simpleName}") }
            }
        }
    }

    private fun notesPrompt(video: YouTubeVideo, transcript: String): String {
        val text = if (transcript.length > MAX_TRANSCRIPT_CHARS) {
            transcript.take(MAX_TRANSCRIPT_CHARS) + "\n[The transcript was cut here because it is very long.]"
        } else {
            transcript
        }
        return "You are a study assistant. Below is the transcript of the YouTube video \"${video.title}\"" +
            (if (video.channel.isNotBlank()) " by ${video.channel}" else "") + ".\n" +
            "Write clear, structured study notes in Markdown: a two or three line summary, then a heading for each topic with " +
            "short bullet points, a list of key terms with one-line definitions, and five review questions at the end. " +
            "Use only what the transcript says; do not add facts that are not in it.\n\nTranscript:\n" + text
    }

    private fun notesPromptFromVideo(video: YouTubeVideo): String =
        "You are a study assistant. Watch the attached YouTube video \"${video.title}\" and write clear, structured study notes " +
            "in Markdown: a two or three line summary, a heading for each topic with short bullet points, key terms with one-line " +
            "definitions, and five review questions at the end. Use only what is in the video."

    // ---- Text note, documents, audio, homework, generated notes ----

    private var aiJob: Job? = null
    private val recorder by lazy { skywa1ker404.absencium.data.audio.AudioRecorder(container.appContext) }
    private var recordingFile: File? = null

    /** An empty text note in the chosen subject, opened straight into the editor. */
    fun createTextNote(subjectName: String, onSaved: (Long) -> Unit) {
        val current = _state.value
        if (current.busy || current.subjectId == 0L) return
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            try {
                val id = container.vaultRepository.createEntry(current.subjectId, "$subjectName notes", NoteKind.TEXT, "", current.date, emptyList())
                current.date?.let { container.calendarRepository.setNotesCollected(listOf(current.subjectId), it, true) }
                _state.value = AddEntryState()
                onSaved(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DebugLogger.e("Vault", "Could not create the text note", e)
                _state.update { it.copy(busy = false, message = "Could not create the note: ${e.message ?: e.javaClass.simpleName}") }
            }
        }
    }

    /** Saves the chosen files (PDF, Word, anything) as one entry. */
    fun addDocuments(uris: List<Uri>, subjectName: String, extractText: Boolean = false, onSaved: (Long) -> Unit) {
        val current = _state.value
        if (uris.isEmpty() || current.busy || current.subjectId == 0L) return
        _state.update { it.copy(busy = true, busyStep = "Copying the files", message = null) }
        val copied = ArrayList<String>()
        docsJob = viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val attachments = ArrayList<AttachmentEntity>()
                for ((index, uri) in uris.withIndex()) {
                    val stored = store.import(uri) ?: continue
                    copied += stored.path
                    attachments += AttachmentEntity(
                        noteId = 0L,
                        kind = if (stored.mimeType.startsWith("image/")) AttachmentKind.IMAGE else AttachmentKind.DOCUMENT,
                        filePath = stored.path,
                        displayName = stored.displayName,
                        mimeType = stored.mimeType,
                        sizeBytes = stored.sizeBytes,
                        createdAt = now + index,
                    )
                }
                if (attachments.isEmpty()) {
                    _state.update { it.copy(busy = false, busyStep = null, busyProgress = null, message = "None of the files could be read.") }
                    return@launch
                }
                val title = if (attachments.size == 1) attachments[0].displayName.substringBeforeLast('.').ifBlank { "$subjectName file" } else "$subjectName files"
                // Optionally the words of the files go into the note itself, so the entry is searchable and editable.
                val body = StringBuilder()
                if (extractText) {
                    for (a in attachments) {
                        val kind = skywa1ker404.absencium.data.ocr.FileKind.of(a.mimeType, a.displayName)
                        if (kind == skywa1ker404.absencium.data.ocr.FileKind.OTHER) continue
                        _state.update { it.copy(busyStep = "Reading ${a.displayName} with the AI") }
                        when (val r = container.textExtractor.extract(
                            a.filePath, a.mimeType, a.displayName,
                            onUpload = { f -> _state.update { st -> st.copy(busyProgress = if (f >= 1f) null else f) } },
                        ) { step ->
                            _state.update { st -> st.copy(busyStep = "${a.displayName}: $step") }
                        }) {
                            is skywa1ker404.absencium.data.ocr.Extracted.Text ->
                                body.append(if (attachments.size > 1) "## ${a.displayName}\n" else "").append(skywa1ker404.absencium.data.ai.AiNoteCleaner.clean(r.text)).append("\n\n")
                            is skywa1ker404.absencium.data.ocr.Extracted.Failed -> {
                                // Nothing is saved: the files are removed again and the student can retry or choose "Just attach".
                                copied.forEach { store.delete(it) }
                                _state.update {
                                    it.copy(busy = false, busyStep = null, busyProgress = null, message = "${a.displayName}: ${r.message} Nothing was saved. Try again, or choose \"Just attach\" to add the file without its text.")
                                }
                                return@launch
                            }
                        }
                    }
                }
                val id = container.vaultRepository.createEntry(current.subjectId, title, NoteKind.DOCUMENT, body.toString().trim(), current.date, attachments)
                putPicturesOnPage(id, current.subjectId, title, body.toString().trim())
                current.date?.let { container.calendarRepository.setNotesCollected(listOf(current.subjectId), it, true) }
                _state.value = AddEntryState()
                onSaved(id)
            } catch (e: CancellationException) {
                copied.forEach { store.delete(it) }
                throw e
            } catch (e: Exception) {
                DebugLogger.e("Vault", "Could not save the files", e)
                copied.forEach { store.delete(it) }
                _state.update { it.copy(busy = false, busyStep = null, busyProgress = null, message = "Could not save the files: ${e.message ?: e.javaClass.simpleName}") }
            }
        }
    }

    private var docsJob: Job? = null

    /** Stops a file save that is still copying or reading with the AI; the copies are removed. */
    fun cancelDocuments() {
        docsJob?.cancel()
        _state.update { it.copy(busy = false, busyStep = null, busyProgress = null, message = "Cancelled. Nothing was saved.") }
    }

    // Audio

    fun openAudio() = _state.update { it.copy(stage = EntryStage.AUDIO, message = null) }

    fun startRecording() {
        if (_state.value.recording) return
        _state.value.recordedPath?.let { store.delete(it) }
        val file = store.newFile("m4a")
        val problem = recorder.start(file)
        if (problem != null) {
            _state.update { it.copy(message = problem, recordedPath = null, recordedMs = 0L) }
            return
        }
        recordingFile = file
        _state.update { it.copy(recording = true, recordedPath = null, recordedMs = 0L, message = null) }
    }

    fun stopRecording() {
        val file = recordingFile ?: return
        val length = recorder.stop(file)
        recordingFile = null
        _state.update {
            if (length == null) it.copy(recording = false, recordedPath = null, recordedMs = 0L, message = "That recording was too short to keep.")
            else it.copy(recording = false, recordedPath = file.absolutePath, recordedMs = length)
        }
    }

    fun recordingElapsed(): Long = recorder.elapsedMs()

    fun discardRecording() {
        recordingFile?.let { recorder.abort(it) }
        recordingFile = null
        _state.value.recordedPath?.let { store.delete(it) }
        _state.update { it.copy(recording = false, recordedPath = null, recordedMs = 0L) }
    }

    fun micDenied() = _state.update { it.copy(message = "Allow the microphone in Android settings to record.") }

    /** Saves the recording as an entry whose page shows the recording. */
    fun saveRecording(subjectName: String, onSaved: (Long) -> Unit) {
        val current = _state.value
        val path = current.recordedPath ?: return
        if (current.busy || current.subjectId == 0L) return
        if (current.transcribe) {
            val settings = container.settingsState.value
            if (settings == null || !aiUsable(settings, container.activeKeySaved())) {
                _state.update { it.copy(message = "Turning a recording into text needs an AI. Set one up in Settings, AI & Integrations, or switch the option off.") }
                return
            }
        }
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            try {
                val file = File(path)
                val title = current.title.trim().ifEmpty { "$subjectName recording" }
                val id = container.vaultRepository.createEntry(
                    current.subjectId, title, NoteKind.AUDIO, "", current.date,
                    listOf(
                        AttachmentEntity(
                            noteId = 0L, kind = AttachmentKind.AUDIO, filePath = path, displayName = "Recording.m4a",
                            mimeType = "audio/mp4", sizeBytes = file.length(), durationMs = current.recordedMs,
                        ),
                    ),
                )
                val audio = container.vaultRepository.attachmentsFor(id).firstOrNull { it.kind == AttachmentKind.AUDIO }
                if (audio != null) {
                    var body = "{{audio:${audio.id}|100|center}}\n"
                    if (current.transcribe) {
                        when (val r = container.textExtractor.extract(path, "audio/mp4", "Recording.m4a")) {
                            is skywa1ker404.absencium.data.ocr.Extracted.Text -> body += "\n" + skywa1ker404.absencium.data.ai.AiNoteCleaner.clean(r.text) + "\n"
                            is skywa1ker404.absencium.data.ocr.Extracted.Failed -> body += "\n*The recording could not be turned into text: ${r.message}*\n"
                        }
                    }
                    container.vaultRepository.saveNote(id, current.subjectId, title, body, NoteStatus.ACTIVE)
                }
                current.date?.let { container.calendarRepository.setNotesCollected(listOf(current.subjectId), it, true) }
                _state.value = AddEntryState()
                onSaved(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DebugLogger.e("Vault", "Could not save the recording", e)
                _state.update { it.copy(busy = false, message = "Could not save the recording: ${e.message ?: e.javaClass.simpleName}") }
            }
        }
    }

    // Solve Homework and Generate AI Notes

    fun openSolve() = _state.update { it.copy(stage = EntryStage.SOLVE, message = null, aiError = null, aiSetupMissing2 = false) }

    fun openGenerate() = _state.update { it.copy(stage = EntryStage.GENERATE, message = null, aiError = null, aiSetupMissing2 = false) }

    fun setAiPrompt(text: String) = _state.update { it.copy(aiPrompt = text) }

    /** Which image sources the user switched on in Settings, AI & Integrations. Both are off until they do. */
    data class ImageSources(val search: Boolean = false, val generate: Boolean = false)

    val imageSources: StateFlow<ImageSources> = container.settingsState
        .map { ImageSources(it?.searchImagesEnabled == true, it?.generateImagesEnabled == true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ImageSources())

    fun setTranscribe(v: Boolean) = _state.update { it.copy(transcribe = v) }

    fun setNoteLength(v: skywa1ker404.absencium.data.ai.NoteLength) = _state.update { it.copy(noteLength = v) }
    fun setNoteTemplate(v: skywa1ker404.absencium.data.ai.NoteTemplate) = _state.update { it.copy(noteTemplate = v) }
    fun setSyllabusNode(id: Long?) = _state.update { it.copy(syllabusNodeId = id) }
    fun setCourseInfo(v: String) = _state.update { it.copy(courseInfo = v) }
    fun setGenSearchDiagrams(v: Boolean) = _state.update { it.copy(genSearchDiagrams = v) }
    fun setGenDrawDiagrams(v: Boolean) = _state.update { it.copy(genDrawDiagrams = v) }
    fun setGenTables(v: Boolean) = _state.update { it.copy(genTables = v) }
    fun setGenPyq(v: Boolean) = _state.update { it.copy(genPyq = v) }
    fun setGenAssignment(v: Boolean) = _state.update { it.copy(genAssignment = v) }

    /** The chosen subject's syllabus tree as dropdown choices; empty when it has none. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val syllabusChoices: StateFlow<List<skywa1ker404.absencium.data.ai.SyllabusPath.Choice>> = _state
        .map { it.subjectId }
        .distinctUntilChanged()
        .flatMapLatest { id ->
            if (id == 0L) kotlinx.coroutines.flow.flowOf(emptyList())
            else container.database.syllabusDao().observeBySubject(id).map { skywa1ker404.absencium.data.ai.SyllabusPath.choices(it) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun dismissAiSetup2() = _state.update { it.copy(aiSetupMissing2 = false) }

    /**
     * Sends the typed question (and, for homework, the chosen photos) to the AI and saves the answer as a new entry; the
     * photos stay with the entry. Without a saved AI connection the setup prompt shows instead.
     */
    fun runAiTask(solve: Boolean, subjectName: String, onSaved: (Long) -> Unit) {
        val current = _state.value
        if (current.aiRunning || current.subjectId == 0L) return
        val prompt = current.aiPrompt.trim()
        if (prompt.isEmpty() && !(solve && current.pictures.isNotEmpty())) {
            _state.update { it.copy(aiError = if (solve) "Type the question or add a photo of it." else "Type what the notes should be about.") }
            return
        }
        val settings = container.settingsState.value ?: return
        if (!aiUsable(settings, container.activeKeySaved())) {
            _state.update { it.copy(aiSetupMissing2 = true, aiError = null) }
            return
        }
        _state.update { it.copy(aiRunning = true, aiError = null, aiSetupMissing2 = false) }
        aiJob = viewModelScope.launch {
            try {
                val connection = AiConnection(settings.aiProvider, settings.effectiveAiStyle, settings.aiBaseUrl, settings.aiModel)
                val images = if (solve) withContext(Dispatchers.IO) {
                    current.pictures.take(MAX_EXTRACT_PHOTOS).mapNotNull { picture ->
                        val bitmap = ImageTools.decode(picture.path, EXTRACT_EDGE) ?: return@mapNotNull null
                        val bytes = java.io.ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }.toByteArray()
                        AiImage("image/jpeg", android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP))
                    }
                } else emptyList()
                val sources = imageSources.value
                val useSearch = current.genSearchDiagrams && sources.search
                val useDraw = current.genDrawDiagrams && sources.generate
                val nodes = if (current.syllabusNodeId != null) container.database.syllabusDao().observeBySubject(current.subjectId).first() else emptyList()
                val brief = skywa1ker404.absencium.data.ai.NoteBrief(
                    request = prompt,
                    subject = subjectName,
                    length = current.noteLength,
                    template = current.noteTemplate,
                    syllabus = skywa1ker404.absencium.data.ai.SyllabusPath.context(nodes, current.syllabusNodeId, current.courseInfo)
                        .let { if (it.isEmpty && current.courseInfo.isNotBlank()) it.copy(course = current.courseInfo.trim()) else it },
                    searchDiagrams = useSearch,
                    drawDiagrams = useDraw,
                    tablesAndGraphs = current.genTables,
                    pyq = current.genPyq,
                    assignment = solve && current.genAssignment,
                    hasPhotos = images.isNotEmpty(),
                )
                _state.update { it.copy(aiStep = if (solve) "Working on the answer" else "Writing your notes") }
                val ask = if (solve) skywa1ker404.absencium.data.ai.NotePrompts.homework(brief) else skywa1ker404.absencium.data.ai.NotePrompts.notes(brief)
                val raw = container.aiClient.generate(connection, ask, images, maxTokens = current.noteLength.maxTokens)
                if (raw.isBlank()) throw AiException("The model sent back an empty answer.")
                val reply = try {
                    skywa1ker404.absencium.data.ai.AiNoteCleaner.clean(raw)
                } catch (e: Throwable) {
                    DebugLogger.e("Vault", "The note cleaner failed; the raw answer is used", e)
                    raw.trim()
                }

                // Diagrams: only when the user switched fetching or drawing on.
                val wantsDiagrams = useSearch || useDraw
                var wanted = if (wantsDiagrams) skywa1ker404.absencium.data.ai.NotePrompts.diagramRequests(reply, 5) else emptyList()
                // The model did not place any diagram lines: look for one picture of the topic itself.
                if (wantsDiagrams && wanted.isEmpty()) wanted = listOf("${prompt.lineSequence().firstOrNull { it.isNotBlank() }?.take(80) ?: subjectName} diagram")
                val failedDiagrams = ArrayList<String>()
                val found = LinkedHashMap<String, Pair<AttachmentEntity, String>>()
                var diagramNote: String? = null
                for ((index, query) in wanted.withIndex()) {
                    _state.update { it.copy(aiStep = "Getting diagram ${index + 1} of ${wanted.size}") }
                    var picture = if (useSearch) container.diagramService.fetch(query) else null
                    if (picture == null && useDraw) {
                        picture = try {
                            container.diagramService.draw(connection, query)
                        } catch (e: AiException) {
                            val msg = e.message.orEmpty()
                            diagramNote = if (msg.contains("429") || msg.contains("quota", true)) {
                                "your AI plan has no image generation allowance right now (quota used up or not included in the free plan)"
                            } else msg.lineSequence().first().take(140)
                            null
                        }
                    }
                    if (picture == null) {
                        failedDiagrams += query
                        continue
                    }
                    val file = store.newFile(picture.extension)
                    withContext(Dispatchers.IO) { file.writeBytes(picture.bytes) }
                    found[query] = AttachmentEntity(
                        noteId = 0L, kind = AttachmentKind.IMAGE, filePath = file.absolutePath, displayName = "Diagram ${index + 1}.${picture.extension}",
                        mimeType = picture.mimeType, sizeBytes = file.length(),
                    ) to picture.caption
                }

                diagramNote?.let { DebugLogger.i("Vault", "Diagrams: $it") }
                val title = (if (solve) "Homework: " else "") + (prompt.lineSequence().firstOrNull { it.isNotBlank() }?.take(60) ?: "$subjectName homework")
                val attachments = (if (solve) current.pictures.map {
                    AttachmentEntity(noteId = 0L, kind = AttachmentKind.IMAGE, filePath = it.path, displayName = it.name, mimeType = it.mimeType, sizeBytes = it.sizeBytes)
                } else emptyList()) + found.values.map { it.first }
                val id = container.vaultRepository.createEntry(
                    current.subjectId, title, if (solve) NoteKind.HOMEWORK else NoteKind.AI_NOTES,
                    skywa1ker404.absencium.data.ai.NotePrompts.withoutDiagramLines(reply), current.date, attachments,
                )
                if (found.isNotEmpty() || failedDiagrams.isNotEmpty()) {
                    val saved = container.vaultRepository.attachmentsFor(id).associateBy { it.filePath }
                    var body = reply
                    for ((query, entry) in found) {
                        val stored = saved[entry.first.filePath] ?: continue
                        val line = Regex("""(?m)^[ \t]*\{\{diagram:\s*${Regex.escape(query)}\s*\}\}[ \t]*$""")
                        val block = skywa1ker404.absencium.ui.vault.markdown.NoteBlocks.token(skywa1ker404.absencium.ui.vault.markdown.EmbedKind.IMAGE, stored.id, 80) +
                            "\n{align:center}*" + entry.second + "*"
                        body = if (line.containsMatchIn(body)) body.replace(line, Regex.escapeReplacement(block)) else body.trimEnd() + "\n\n" + block
                    }
                    body = skywa1ker404.absencium.data.ai.NotePrompts.withoutDiagramLines(body)
                    if (failedDiagrams.isNotEmpty()) {
                        val why = diagramNote?.let { ": $it" } ?: if (useSearch) ": nothing suitable was found online, or there is no internet" else ""
                        body = body.trimEnd() + "\n\n*No picture could be added for ${failedDiagrams.joinToString(", ") { "\"$it\"" }}$why*"
                    }
                    container.vaultRepository.saveNote(id, current.subjectId, title, body, NoteStatus.ACTIVE)
                }
                current.date?.let { container.calendarRepository.setNotesCollected(listOf(current.subjectId), it, true) }
                _state.value = AddEntryState()
                onSaved(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: AiException) {
                _state.update { it.copy(aiRunning = false, aiError = skywa1ker404.absencium.data.ai.AI_FAILED_INLINE) }
            } catch (e: Throwable) {
                // Errors (not only exceptions) are shown instead of ending the app; the stack trace goes to debug.log.
                DebugLogger.e("Vault", "The AI task failed", e)
                _state.update { it.copy(aiRunning = false, aiError = "Something went wrong: ${e.message ?: e.javaClass.simpleName}") }
            }
        }
    }

    fun cancelAiTask() {
        aiJob?.cancel()
        _state.update { it.copy(aiRunning = false) }
    }

    private fun discardPictures(pictures: List<PendingPicture>) {
        pictures.forEach {
            store.delete(it.path)
            ImageTools.forget(it.path)
        }
    }

    private companion object {
        const val MAX_TRANSCRIPT_CHARS = 120_000
        const val NEEDS_TRANSCRIPT = "Paste the video's transcript below. Only Gemini can write notes from the link alone."
        const val MAX_EXTRACT_PHOTOS = 8
        const val EXTRACT_EDGE = 1600
    }

    override fun onCleared() {
        // The popup was torn down without a save or a cancel (the process is ending); nothing else holds these files.
        recordingFile?.let { recorder.abort(it) }
        _state.value.recordedPath?.let { store.delete(it) }
        discardPictures(_state.value.pictures)
    }
}
