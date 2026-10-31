package skywa1ker404.absencium.ui.syllabus

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import skywa1ker404.absencium.AppContainer
import skywa1ker404.absencium.data.ai.AiException
import skywa1ker404.absencium.data.db.SyllabusNodeEntity
import skywa1ker404.absencium.data.db.SyllabusStatus
import skywa1ker404.absencium.data.ocr.Extracted
import skywa1ker404.absencium.data.syllabus.SyllabusTree
import skywa1ker404.absencium.logging.DebugLogger

/** Where reading a syllabus outline from text or a file stands. */
sealed interface OutlineImport {
    data object Idle : OutlineImport
    /** [progress] is 0..1 while a file is uploading, null when the step has no measurable progress. */
    data class Working(val step: String, val progress: Float? = null) : OutlineImport
    /** The outline that was read, waiting for the student to accept it. */
    data class Ready(val items: List<SyllabusTree.OutlineItem>) : OutlineImport
    data class Problem(val message: String, val setupMissing: Boolean = false) : OutlineImport
}

/** One subject's syllabus: the tree, how many notes and materials hang off each node, and changing it. */
class SyllabusViewModel(private val container: AppContainer, private val subjectId: Long) : ViewModel() {

    private val dao = container.database.syllabusDao()

    val nodes: StateFlow<List<SyllabusNodeEntity>> =
        dao.observeBySubject(subjectId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** For each node, how many notes and study materials are filed under it directly. */
    val linkCounts: StateFlow<Map<Long, Int>> = combine(
        container.database.noteDao().observeAll(),
        container.database.guideDao().observeBySubject(subjectId),
    ) { notes, guides ->
        val counts = HashMap<Long, Int>()
        notes.forEach { n -> if (n.subjectId == subjectId) n.syllabusNodeId?.let { counts[it] = (counts[it] ?: 0) + 1 } }
        guides.forEach { g -> g.syllabusNodeId?.let { counts[it] = (counts[it] ?: 0) + 1 } }
        counts as Map<Long, Int>
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _subjectName = MutableStateFlow("")
    val subjectName: StateFlow<String> = _subjectName.asStateFlow()

    private val _import = MutableStateFlow<OutlineImport>(OutlineImport.Idle)
    val importState: StateFlow<OutlineImport> = _import.asStateFlow()
    private var importJob: Job? = null

    init {
        viewModelScope.launch { _subjectName.value = container.database.subjectDao().getById(subjectId)?.name.orEmpty() }
    }

    fun aiReady(): Boolean = container.aiConnection() != null

    // ---- Editing the tree ----

    fun add(parentId: Long?, title: String, details: String? = null) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val siblings = nodes.value.filter { it.parentId == parentId }
            val next = (siblings.maxOfOrNull { it.position } ?: -1) + 1
            dao.insert(
                SyllabusNodeEntity(
                    subjectId = subjectId,
                    parentId = parentId,
                    title = clean.take(120),
                    details = details?.trim()?.ifEmpty { null },
                    position = next,
                ),
            )
        }
    }

    fun rename(id: Long, title: String) {
        val clean = title.trim().take(120)
        if (clean.isEmpty()) return
        update(id) { it.copy(title = clean) }
    }

    fun setDetails(id: Long, details: String) = update(id) { it.copy(details = details.trim().ifEmpty { null }) }

    /** Moves a node on to its next status. On a unit or chapter every node below it takes the same status, so one tap finishes a whole unit. */
    fun cycleStatus(id: Long) {
        viewModelScope.launch {
            val all = nodes.value
            val node = all.firstOrNull { it.id == id } ?: return@launch
            val next = SyllabusTree.nextStatus(node.status)
            val now = System.currentTimeMillis()
            SyllabusTree.withDescendants(all, id).forEach { target ->
                all.firstOrNull { it.id == target }?.let { dao.update(it.copy(status = next, updatedAt = now)) }
            }
        }
    }

    fun setStatus(id: Long, status: SyllabusStatus) = update(id) { it.copy(status = status) }

    /** Swaps a node with the one above ([up]) or below it among its siblings. */
    fun move(id: Long, up: Boolean) {
        viewModelScope.launch {
            val all = nodes.value
            val node = all.firstOrNull { it.id == id } ?: return@launch
            val siblings = all.filter { it.parentId == node.parentId }.sortedWith(compareBy({ it.position }, { it.id })).toMutableList()
            val index = siblings.indexOfFirst { it.id == id }
            val target = if (up) index - 1 else index + 1
            if (index < 0 || target !in siblings.indices) return@launch
            siblings.add(target, siblings.removeAt(index))
            val now = System.currentTimeMillis()
            // Positions are rewritten 0..n so imported rows that all started at 0 stay in the order shown.
            siblings.forEachIndexed { position, s -> if (s.position != position) dao.update(s.copy(position = position, updatedAt = now)) }
        }
    }

    /** Removes a node and everything below it. Notes and materials filed there go back to "Uncategorized". */
    fun delete(id: Long) {
        viewModelScope.launch {
            val ids = SyllabusTree.withDescendants(nodes.value, id)
            container.database.noteDao().clearSyllabusNodes(ids)
            container.database.guideDao().clearSyllabusNodes(ids)
            dao.delete(id)
        }
    }

    private fun update(id: Long, change: (SyllabusNodeEntity) -> SyllabusNodeEntity) {
        viewModelScope.launch {
            val node = nodes.value.firstOrNull { it.id == id } ?: dao.getById(id) ?: return@launch
            dao.update(change(node).copy(updatedAt = System.currentTimeMillis()))
        }
    }

    // ---- Reading an outline from text or a file ----

    fun dismissImport() {
        importJob?.cancel()
        _import.value = OutlineImport.Idle
    }

    /** Reads [text] as an outline written with indentation. Nothing is sent anywhere. */
    fun importTyped(text: String) {
        val items = SyllabusTree.parseOutline(text)
        _import.value = if (items.isEmpty()) OutlineImport.Problem("No lines to read. Write one title per line, indented to show what sits inside what.") else OutlineImport.Ready(items)
    }

    /** Asks the student's own AI to turn messy [text] (a pasted syllabus page) into an outline. */
    fun importWithAi(text: String) {
        if (text.isBlank()) {
            _import.value = OutlineImport.Problem("Paste the syllabus text first.")
            return
        }
        importJob?.cancel()
        _import.value = OutlineImport.Working("Asking the AI to find the units and topics")
        importJob = viewModelScope.launch { _import.value = outlineFromText(text) }
    }

    /**
     * Reads everything the student gave: one document, up to [MAX_PICTURES] pictures and/or typed text, each source on its own, and
     * joins what they found into one outline (the same unit named in two sources appears once). If one source fails and another works,
     * the outline from the working one is shown.
     */
    fun importSources(doc: Uri?, pictures: List<Uri>, text: String) {
        if (doc == null && pictures.isEmpty()) {
            importWithAi(text)
            return
        }
        if (container.aiConnection() == null) {
            _import.value = OutlineImport.Problem("AI Features are off or not set up. You can still paste the outline as text.", setupMissing = true)
            return
        }
        importJob?.cancel()
        _import.value = OutlineImport.Working("Reading what you picked")
        importJob = viewModelScope.launch {
            val kept = ArrayList<String>()
            try {
                val found = ArrayList<List<SyllabusTree.OutlineItem>>()
                var problem: OutlineImport.Problem? = null
                fun take(result: OutlineImport) {
                    when (result) {
                        is OutlineImport.Ready -> found += result.items
                        is OutlineImport.Problem -> if (problem == null) problem = result
                        else -> Unit
                    }
                }
                if (doc != null) {
                    _import.value = OutlineImport.Working("Reading the document")
                    val stored = container.attachmentStore.import(doc)
                    if (stored == null) {
                        take(OutlineImport.Problem("That document could not be read."))
                    } else {
                        kept += stored.path
                        take(outlineFromStored(stored.path, stored.mimeType, stored.displayName))
                    }
                }
                if (pictures.isNotEmpty()) {
                    _import.value = OutlineImport.Working("Reading the ${if (pictures.size == 1) "picture" else "${pictures.size} pictures"}")
                    val paths = pictures.take(MAX_PICTURES).mapNotNull { uri -> container.attachmentStore.import(uri)?.path?.also { kept += it } }
                    take(if (paths.isEmpty()) OutlineImport.Problem("The pictures could not be read.") else outlineFromPictures(paths))
                }
                if (text.isNotBlank()) {
                    _import.value = OutlineImport.Working("Reading the typed text")
                    take(outlineFromText(text))
                }
                val merged = SyllabusTree.mergeOutlines(found)
                _import.value = when {
                    merged.isNotEmpty() -> OutlineImport.Ready(merged)
                    else -> problem ?: OutlineImport.Problem("No syllabus was found in what you picked.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DebugLogger.e("Syllabus", "Reading the sources failed", e)
                _import.value = OutlineImport.Problem("Something went wrong: ${e.message ?: e.javaClass.simpleName}")
            } finally {
                kept.forEach { container.attachmentStore.delete(it) }
            }
        }
    }

    /** One stored document as an outline: a PDF whole, a Word or text file as text. */
    private suspend fun outlineFromStored(path: String, mime: String, name: String): OutlineImport {
        val kind = skywa1ker404.absencium.data.ocr.FileKind.of(mime, name)
        if (kind == skywa1ker404.absencium.data.ocr.FileKind.PDF) return outlineFromPdf(path)
        val read = container.textExtractor.extract(path, mime, name, tidy = false) { step -> _import.value = OutlineImport.Working(step) }
        return when (read) {
            is Extracted.Failed -> OutlineImport.Problem(read.message, read.setupMissing)
            is Extracted.Text -> outlineFromText(read.text)
        }
    }

    /** Up to five pictures of the syllabus go up together in one request. */
    private suspend fun outlineFromPictures(paths: List<String>): OutlineImport {
        val connection = container.aiConnection()
            ?: return OutlineImport.Problem("AI Features are off or not set up. You can still add the outline as typed text.", setupMissing = true)
        val subject = _subjectName.value.ifEmpty { container.database.subjectDao().getById(subjectId)?.name.orEmpty() }
        return try {
            val images = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                paths.mapNotNull { path ->
                    val bitmap = skywa1ker404.absencium.ui.vault.entry.ImageTools.decode(path, PICTURE_EDGE) ?: return@mapNotNull null
                    val bytes = java.io.ByteArrayOutputStream().also { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, it) }.toByteArray()
                    skywa1ker404.absencium.data.ai.AiImage("image/jpeg", android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP))
                }
            }
            if (images.isEmpty()) return OutlineImport.Problem("The pictures could not be opened.")
            val reply = container.aiClient.generate(
                connection,
                outlinePrompt(subject).replace("Text:\n", "The syllabus is in the attached ${if (images.size == 1) "picture" else "${images.size} pictures, in order"}."),
                images,
                maxTokens = 8000,
                onUpload = { f -> _import.value = if (f >= 1f) OutlineImport.Working("Uploaded. The AI is reading the pictures") else OutlineImport.Working("Uploading the pictures", f) },
            )
            if (reply.trim().equals("NONE", ignoreCase = true)) {
                return OutlineImport.Problem("The pictures do not seem to show a syllabus for $subject.")
            }
            val items = SyllabusTree.parseReply(reply)
            if (items.isEmpty()) OutlineImport.Problem("The AI did not find any units or topics in the pictures.") else OutlineImport.Ready(items)
        } catch (e: CancellationException) {
            throw e
        } catch (e: AiException) {
            OutlineImport.Problem(skywa1ker404.absencium.data.ai.AI_FAILED_INLINE)
        } catch (e: Exception) {
            DebugLogger.e("Syllabus", "Reading the pictures failed", e)
            OutlineImport.Problem("Something went wrong: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    /** Reads a picked document or photo with the AI, then turns what it says into an outline. */
    fun importFile(uri: Uri) {
        if (container.aiConnection() == null) {
            _import.value = OutlineImport.Problem("AI Features are off or not set up. You can still paste the outline as text.", setupMissing = true)
            return
        }
        importJob?.cancel()
        _import.value = OutlineImport.Working("Reading the file")
        importJob = viewModelScope.launch {
            val stored = container.attachmentStore.import(uri)
            if (stored == null) {
                _import.value = OutlineImport.Problem("That file could not be read.")
                return@launch
            }
            try {
                val kind = skywa1ker404.absencium.data.ocr.FileKind.of(stored.mimeType, stored.displayName)
                if (kind == skywa1ker404.absencium.data.ocr.FileKind.PDF) {
                    _import.value = outlineFromPdf(stored.path)
                    return@launch
                }
                // Word and text files are already text: they go straight to the outline step with no tidy-up request first.
                val read = container.textExtractor.extract(stored.path, stored.mimeType, stored.displayName, tidy = false) { step ->
                    _import.value = OutlineImport.Working(step)
                }
                _import.value = when (read) {
                    is Extracted.Failed -> OutlineImport.Problem(read.message, read.setupMissing)
                    is Extracted.Text -> {
                        _import.value = OutlineImport.Working("Asking the AI to find the units and topics")
                        outlineFromText(read.text)
                    }
                }
            } finally {
                container.attachmentStore.delete(stored.path)
            }
        }
    }

    /**
     * Reads a PDF as small page pictures, a few pages per request, asking for this subject's outline from each batch, so a big
     * session PDF does not have to travel as one huge upload. Batches that do not mention the subject answer NONE and add nothing.
     */
    private suspend fun outlineFromPdf(path: String): OutlineImport {
        val connection = container.aiConnection()
            ?: return OutlineImport.Problem("AI Features are off or not set up. You can still add the outline as typed text.", setupMissing = true)
        val subject = _subjectName.value.ifEmpty { container.database.subjectDao().getById(subjectId)?.name.orEmpty() }
        val extractor = container.textExtractor
        return try {
            val file = java.io.File(path)
            val replies = ArrayList<String>()
            var note = ""
            if (file.length() <= skywa1ker404.absencium.data.ocr.TextExtractor.MAX_AI_FILE_BYTES) {
                // The whole PDF goes up in one request, with the upload shown as a wavy progress line.
                _import.value = OutlineImport.Working("Uploading the PDF", 0f)
                val bytes = file.readBytes()
                val pdf = skywa1ker404.absencium.data.ai.AiImage("application/pdf", android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP))
                val reply = container.aiClient.generate(
                    connection,
                    outlinePrompt(subject).replace("Text:\n", "The syllabus document is attached as a PDF."),
                    listOf(pdf),
                    maxTokens = 8000,
                    onUpload = { f ->
                        _import.value = if (f >= 1f) OutlineImport.Working("Uploaded. The AI is reading all the pages") else OutlineImport.Working("Uploading the PDF", f)
                    },
                )
                if (!reply.trim().equals("NONE", ignoreCase = true)) replies += reply.trim()
            } else {
                // Too big to send whole: the first pages as small pictures, a few at a time.
                val total = extractor.pdfPageCount(path)
                val used = minOf(total, skywa1ker404.absencium.data.ocr.TextExtractor.MAX_PDF_PAGES)
                var batch = container.aiClient.imageLimit(connection, skywa1ker404.absencium.data.ocr.TextExtractor.PDF_BATCH)
                var from = 0
                while (from < used) {
                    val to = minOf(used, from + batch)
                    _import.value = OutlineImport.Working("This PDF is large, reading pages ${from + 1} to $to of $total", from.toFloat() / used)
                    try {
                        val reply = container.aiClient.generate(
                            connection,
                            outlinePrompt(subject).replace("Text:\n", "The pages are attached as pictures (pages ${from + 1} to $to of $total)."),
                            extractor.pdfImages(path, from, to),
                            maxTokens = 8000,
                            adaptiveImages = true,
                        )
                        if (!reply.trim().equals("NONE", ignoreCase = true)) replies += reply.trim()
                        from = to
                    } catch (e: AiException) {
                        // The model takes fewer pictures than this: the batch shrinks to its limit and these pages go again.
                        val limit = e.maxImages
                        if (limit == null || limit >= to - from) throw e
                        batch = limit
                    }
                }
                if (total > used) note = " in its first $used pages"
            }
            val items = SyllabusTree.mergeOutlines(replies.map { SyllabusTree.parseReply(it) })
            if (items.isEmpty()) {
                OutlineImport.Problem("The document does not seem to contain a syllabus for $subject$note. Check the subject name, or use a PDF of just its part.")
            } else {
                OutlineImport.Ready(items)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: AiException) {
            OutlineImport.Problem(skywa1ker404.absencium.data.ai.AI_FAILED_INLINE)
        } catch (e: java.io.IOException) {
            OutlineImport.Problem(e.message ?: "The file could not be read.")
        } catch (e: Exception) {
            DebugLogger.e("Syllabus", "Reading the PDF outline failed", e)
            OutlineImport.Problem("Something went wrong: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    private suspend fun outlineFromText(text: String): OutlineImport {
        val connection = container.aiConnection()
            ?: return OutlineImport.Problem("AI Features are off or not set up. You can still add the outline as typed text.", setupMissing = true)
        val subject = _subjectName.value.ifEmpty { container.database.subjectDao().getById(subjectId)?.name.orEmpty() }
        return try {
            val reply = container.aiClient.generate(connection, outlinePrompt(subject) + sourceFor(text, subject), maxTokens = 8000)
            if (reply.trim().equals("NONE", ignoreCase = true)) {
                return OutlineImport.Problem("The text does not seem to contain a syllabus for $subject. Check the subject name, or paste just its part.")
            }
            val items = SyllabusTree.parseReply(reply)
            if (items.isEmpty()) OutlineImport.Problem("The AI did not find any units or topics in that text.") else OutlineImport.Ready(items)
        } catch (e: CancellationException) {
            throw e
        } catch (e: AiException) {
            OutlineImport.Problem(skywa1ker404.absencium.data.ai.AI_FAILED_INLINE)
        } catch (e: Exception) {
            DebugLogger.e("Syllabus", "Reading the outline failed", e)
            OutlineImport.Problem("Something went wrong: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    /**
     * Adds the items of the outline that were chosen ([chosen] are indexes into [items]), below [parentId] (null for the top level),
     * and closes the import. Nothing that is already there is added twice: an item whose title already sits under the same parent
     * is skipped, and what is inside it goes into the existing node.
     */
    fun insertOutline(items: List<SyllabusTree.OutlineItem>, chosen: Set<Int> = items.indices.toSet(), parentId: Long? = null) {
        viewModelScope.launch {
            val existing = nodes.value
            fun norm(t: String) = t.trim().replace(Regex("\\s+"), " ").lowercase()
            val known = HashMap<Pair<Long?, String>, Long>()
            existing.forEach { known[it.parentId to norm(it.title)] = it.id }
            // The ids of the most recent node at each depth, so a deeper line is filed under the one above it.
            val trail = arrayOfNulls<Long>(SyllabusTree.MAX_DEPTH + 2)
            val nextPosition = HashMap<Long?, Int>()
            fun position(parent: Long?): Int {
                val start = nextPosition[parent] ?: ((existing.filter { it.parentId == parent }.maxOfOrNull { it.position } ?: -1) + 1)
                nextPosition[parent] = start + 1
                return start
            }
            items.forEachIndexed { index, item ->
                val parent = if (item.depth == 0) parentId else trail[item.depth - 1] ?: parentId
                val match = known[parent to norm(item.title)]
                val id = when {
                    match != null -> match
                    index in chosen -> dao.insert(SyllabusNodeEntity(subjectId = subjectId, parentId = parent, title = item.title, position = position(parent)))
                        .also { known[parent to norm(item.title)] = it }
                    else -> null
                }
                trail[item.depth] = id
                for (deeper in item.depth + 1 until trail.size) trail[deeper] = null
            }
            _import.value = OutlineImport.Idle
        }
    }

    companion object {
        private const val MAX_SOURCE = 60_000

        /** The most pictures one import reads. */
        const val MAX_PICTURES = 5
        private const val PICTURE_EDGE = 1800

        /**
         * A whole session's papers hold many subjects, so the AI is told which one to read and to leave the rest out. It answers NONE
         * when that subject is not in the text. Neutral about the course: it only reshapes the text it is given.
         */
        fun outlinePrompt(subject: String): String =
            "The text below may hold the syllabus of several subjects, or a whole session's papers. Read ONLY the syllabus of the " +
                "subject named \"$subject\" and ignore every other subject, department, rule, schedule and general note. " +
                "Answer with JSON only, no Markdown and no explanation: {\"items\":[{\"title\":\"...\",\"children\":[{\"title\":\"...\",\"children\":[...]}]}]}. " +
                "The top level holds the units or modules in order. Inside a unit go its chapters or headings, inside those its topics, and inside " +
                "those any subtopics (at most four levels in all). Leave \"children\" out for a line with nothing under it. " +
                "Keep the original wording and order and add nothing that is not in the text. Include ONLY what is taught: leave out the course " +
                "code, hours, credits, scope, course objectives, outcomes, prerequisites, marks, assessment, reference and book lists. " +
                "Keep every title short, at most about ten words: when a line is a long sentence that lists many things, use its main idea " +
                "as the title and make each listed thing a child. Keep labels such as \"Unit I\" at the start of a title. " +
                "If the subject \"$subject\" is not in the text, answer with the single word NONE.\n\nText:\n"

        /** At most [MAX_SOURCE] characters of [text]: from just before where [subject] is first named when the text is longer. */
        fun sourceFor(text: String, subject: String): String {
            if (text.length <= MAX_SOURCE) return text
            val at = if (subject.isBlank()) -1 else text.indexOf(subject, ignoreCase = true)
            val from = if (at < 0) 0 else (at - 500).coerceAtLeast(0)
            return text.substring(from, minOf(text.length, from + MAX_SOURCE))
        }
    }
}
