package skywa1ker404.absencium.ui.search

import skywa1ker404.absencium.ui.components.ExpressiveEmptyState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import skywa1ker404.absencium.ui.components.AppTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import skywa1ker404.absencium.ui.components.OverlayStage
import skywa1ker404.absencium.ui.components.PopupShell
import skywa1ker404.absencium.ui.components.sheetContainerColor
import skywa1ker404.absencium.ui.components.sheetScrimColor
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import skywa1ker404.absencium.data.calendar.CalendarData
import skywa1ker404.absencium.data.db.ExamEntity
import skywa1ker404.absencium.data.db.GuideEntity
import skywa1ker404.absencium.data.db.NoteEntity
import skywa1ker404.absencium.domain.FuzzySearch
import skywa1ker404.absencium.domain.pretty
import skywa1ker404.absencium.ui.calendar.subjectColor
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.SectionTitle
import skywa1ker404.absencium.ui.containerViewModel
import skywa1ker404.absencium.ui.icons.AppIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_PER_GROUP = 8

private data class SearchResults(
    val notes: List<NoteEntity> = emptyList(),
    val guides: List<GuideEntity> = emptyList(),
    val exams: List<ExamEntity> = emptyList(),
) {
    val isEmpty: Boolean get() = notes.isEmpty() && guides.isEmpty() && exams.isEmpty()
}

/** Only the start of a very long note is scored, so one huge note cannot stall the search. */
private const val MAX_SCORED_BODY = 20_000

private fun <T> List<T>.ranked(score: (T) -> Int): List<T> =
    map { it to score(it) }.filter { it.second > 0 }.sortedByDescending { it.second }.map { it.first }.take(MAX_PER_GROUP)

/**
 * Global search over typed note text and titles, guide and PDF titles, and exams. Matching is
 * forgiving of typos. Opening a note edits it, a guide opens in a PDF viewer, and an exam shows its details.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSheet(
    calendar: CalendarData?,
    onDismiss: () -> Unit,
    onOpenNote: (NoteEntity) -> Unit,
    onOpenGuide: (GuideEntity) -> Unit,
    onOpenExam: (ExamEntity) -> Unit,
) {
    val viewModel = containerViewModel { SearchViewModel(it) }
    val index by viewModel.index.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val colors = MaterialTheme.colorScheme
    val subjectNames = calendar?.subjects?.associate { it.id to it.name }.orEmpty()
    val exams = calendar?.exams.orEmpty()

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val results by produceState(SearchResults(), query, index, exams) {
        val text = query.trim()
        value = if (text.isEmpty()) {
            SearchResults()
        } else {
            // Wait for a pause in typing: each key restarts this block, so a fast typist never scores big notes per letter.
            kotlinx.coroutines.delay(180)
            withContext(Dispatchers.Default) {
                SearchResults(
                    notes = index.notes.ranked { note ->
                        // A title match counts double over a match somewhere in the body.
                        maxOf(
                            FuzzySearch.score(text, note.title) * 2,
                            FuzzySearch.score(text, note.bodyText.take(MAX_SCORED_BODY)),
                        )
                    },
                    guides = index.guides.ranked { FuzzySearch.score(text, it.fileName) },
                    exams = exams.ranked { exam ->
                        maxOf(
                            FuzzySearch.score(text, exam.title),
                            FuzzySearch.score(text, subjectNames[exam.subjectId].orEmpty()),
                        )
                    },
                )
            }
        }
    }

    PopupShell(onDismissRequest = onDismiss, scrollable = false, dismissOnOutsideTap = true) {
        OverlayStage {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.9f)
                .padding(start = 20.dp, end = 20.dp, top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search notes, PDFs, exams", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                leadingIcon = { Icon(AppIcons.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(AppIcons.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            )

            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when {
                    query.isBlank() -> HintText("Type to search. Typos are forgiven.")
                    results.isEmpty -> ExpressiveEmptyState(AppIcons.Search, "No matches", "Nothing found for \"${query.trim()}\". Try another word.")
                }

                if (results.notes.isNotEmpty()) {
                    SectionTitle("Notes")
                    results.notes.forEach { note ->
                        ResultCard(
                            title = note.title,
                            subtitle = (subjectNames[note.subjectId] ?: "Unknown subject") +
                                if (note.status == skywa1ker404.absencium.data.db.NoteStatus.DRAFT) " · Draft" else "",
                            dotColor = subjectColor(note.subjectId),
                            onClick = {
                                onDismiss()
                                onOpenNote(note)
                            },
                        )
                    }
                }
                if (results.guides.isNotEmpty()) {
                    SectionTitle("Guides and PDFs")
                    results.guides.forEach { guide ->
                        ResultCard(
                            title = guide.fileName,
                            subtitle = subjectNames[guide.subjectId] ?: "Unknown subject",
                            dotColor = subjectColor(guide.subjectId),
                            onClick = {
                                onDismiss()
                                onOpenGuide(guide)
                            },
                        )
                    }
                }
                if (results.exams.isNotEmpty()) {
                    SectionTitle("Exams")
                    results.exams.forEach { exam ->
                        ResultCard(
                            title = exam.title,
                            subtitle = "${subjectNames[exam.subjectId] ?: "Unknown subject"} · ${exam.date.pretty()}",
                            dotColor = subjectColor(exam.subjectId),
                            onClick = {
                                onDismiss()
                                onOpenExam(exam)
                            },
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun ResultCard(title: String, subtitle: String, dotColor: Color, onClick: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}
