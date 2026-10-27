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
    data class Working(val step: String) : OutlineImport
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
                val read = container.textExtractor.extract(stored.path, stored.mimeType, stored.displayName) { step ->
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

    private suspend fun outlineFromText(text: String): OutlineImport {
        val connection = container.aiConnection()
            ?: return OutlineImport.Problem("AI Features are off or not set up. You can still add the outline as typed text.", setupMissing = true)
        return try {
            val reply = container.aiClient.generate(connection, OUTLINE_PROMPT + text.take(MAX_SOURCE), maxTokens = 8000)
            val items = SyllabusTree.parseOutline(reply)
            if (items.isEmpty()) OutlineImport.Problem("The AI did not find any units or topics in that text.") else OutlineImport.Ready(items)
        } catch (e: CancellationException) {
            throw e
        } catch (e: AiException) {
            OutlineImport.Problem(e.message ?: "The AI request failed.")
        } catch (e: Exception) {
            DebugLogger.e("Syllabus", "Reading the outline failed", e)
            OutlineImport.Problem("Something went wrong: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    /** Adds the outline that was read, below [parentId] (null for the top level), and closes the import. */
    fun insertOutline(items: List<SyllabusTree.OutlineItem>, parentId: Long? = null) {
        viewModelScope.launch {
            val existing = nodes.value
            // The ids of the most recent node at each depth, so a deeper line is filed under the one above it.
            val trail = arrayOfNulls<Long>(SyllabusTree.MAX_DEPTH + 2)
            val nextPosition = HashMap<Long?, Int>()
            fun position(parent: Long?): Int {
                val start = nextPosition[parent] ?: ((existing.filter { it.parentId == parent }.maxOfOrNull { it.position } ?: -1) + 1)
                nextPosition[parent] = start + 1
                return start
            }
            items.forEach { item ->
                val parent = if (item.depth == 0) parentId else trail[item.depth - 1] ?: parentId
                val id = dao.insert(SyllabusNodeEntity(subjectId = subjectId, parentId = parent, title = item.title, position = position(parent)))
                trail[item.depth] = id
                for (deeper in item.depth + 1 until trail.size) trail[deeper] = null
            }
            _import.value = OutlineImport.Idle
        }
    }

    companion object {
        private const val MAX_SOURCE = 24_000

        /** Neutral about the course: it only reshapes the text it is given. */
        const val OUTLINE_PROMPT =
            "Turn the syllabus text below into a plain outline. Write one item per line. Put units or modules at the left edge, " +
                "and indent each deeper level by two spaces (chapters, then topics, then subtopics; at most four levels). " +
                "Keep the original wording and order, add nothing that is not in the text, and leave out marks, hours, credits and " +
                "book lists. Do not number the lines, do not use bullets or Markdown, and write no explanation before or after.\n\nSyllabus text:\n"
    }
}
