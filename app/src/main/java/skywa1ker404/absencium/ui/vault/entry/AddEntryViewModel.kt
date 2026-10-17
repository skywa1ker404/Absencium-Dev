package skywa1ker404.absencium.ui.vault.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import skywa1ker404.absencium.AppContainer
import skywa1ker404.absencium.data.db.AttachmentEntity
import skywa1ker404.absencium.data.db.AttachmentKind
import skywa1ker404.absencium.data.db.AttendanceEntity
import skywa1ker404.absencium.data.db.AttendanceStatus
import skywa1ker404.absencium.data.db.NoteKind
import skywa1ker404.absencium.data.db.SubjectEntity
import skywa1ker404.absencium.logging.DebugLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
import skywa1ker404.absencium.ui.settings.aiReady
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

/** The steps of the Add Entry popup. */
enum class EntryStage { SUBJECT, ACTIONS, PICTURES, YOUTUBE }

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
        if (!aiReady(settings, container.apiKeyVault.hasKey())) {
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
                _state.update { it.copy(extractStep = ExtractStep.IDLE, extractError = failureText(e.message)) }
            } catch (e: Exception) {
                DebugLogger.e("Extract", "Text extraction failed", e)
                _state.update { it.copy(extractStep = ExtractStep.IDLE, extractError = failureText(e.message ?: e.javaClass.simpleName)) }
            }
        }
    }

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
        val ready = aiReady(settings, container.apiKeyVault.hasKey())
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
                _state.update { it.copy(youtubeStep = YouTubeStep.IDLE, youtubeError = e.message) }
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
        discardPictures(_state.value.pictures)
    }
}
