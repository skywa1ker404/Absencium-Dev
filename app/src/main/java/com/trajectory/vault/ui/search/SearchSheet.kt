package com.trajectory.vault.ui.search

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import com.trajectory.vault.ui.components.GlassStage
import com.trajectory.vault.ui.components.sheetContainerColor
import com.trajectory.vault.ui.components.sheetScrimColor
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
import com.trajectory.vault.data.calendar.CalendarData
import com.trajectory.vault.data.db.ExamEntity
import com.trajectory.vault.data.db.GuideEntity
import com.trajectory.vault.data.db.NoteEntity
import com.trajectory.vault.domain.FuzzySearch
import com.trajectory.vault.domain.pretty
import com.trajectory.vault.ui.calendar.subjectColor
import com.trajectory.vault.ui.components.AppCard
import com.trajectory.vault.ui.components.HintText
import com.trajectory.vault.ui.components.SectionTitle
import com.trajectory.vault.ui.containerViewModel
import com.trajectory.vault.ui.icons.AppIcons
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
            withContext(Dispatchers.Default) {
                SearchResults(
                    notes = index.notes.ranked { note ->
                        // A title match counts double over a match somewhere in the body.
                        maxOf(
                            FuzzySearch.score(text, note.title) * 2,
                            FuzzySearch.score(text, note.bodyText),
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = sheetContainerColor(),
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = sheetScrimColor(),
    ) {
        GlassStage {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.9f)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search notes, PDFs and exams") },
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
                    results.isEmpty -> HintText("No matches for \"${query.trim()}\".")
                }

                if (results.notes.isNotEmpty()) {
                    SectionTitle("Notes")
                    results.notes.forEach { note ->
                        ResultCard(
                            title = note.title,
                            subtitle = (subjectNames[note.subjectId] ?: "Unknown subject") +
                                if (note.status == com.trajectory.vault.data.db.NoteStatus.DRAFT) " · Draft" else "",
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
