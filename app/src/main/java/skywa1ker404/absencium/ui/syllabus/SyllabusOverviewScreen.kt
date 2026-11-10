package skywa1ker404.absencium.ui.syllabus

import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.components.ExpressiveEmptyState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.SyllabusScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import skywa1ker404.absencium.AppContainer
import skywa1ker404.absencium.data.syllabus.SyllabusTree
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.appContainer
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.setValue
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.components.bounceIn
import skywa1ker404.absencium.ui.containerViewModel

/** One subject on the overview: its name, how many syllabus items it has and how far along it is. */
data class SyllabusSummary(val subjectId: Long, val name: String, val items: Int, val progress: Float)

class SyllabusOverviewViewModel(container: AppContainer) : ViewModel() {
    val summaries: StateFlow<List<SyllabusSummary>> = combine(
        container.subjectRepository.subjects,
        container.database.syllabusDao().observeAll(),
    ) { subjects, nodes ->
        val bySubject = nodes.groupBy { it.subjectId }
        subjects.filter { it.isActive }.map { s ->
            val own = bySubject[s.id].orEmpty()
            SyllabusSummary(s.id, s.name, own.size, SyllabusTree.progress(own, null))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/**
 * The main syllabus page: every active subject, each opening into its own full syllabus right here (units, chapters, topics, status,
 * adding, renaming, moving, deleting, details and importing an outline), the same as the subject's own page.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Destination<RootGraph>
@Composable
fun SyllabusOverviewScreen(navigator: DestinationsNavigator) {
    val viewModel = containerViewModel { SyllabusOverviewViewModel(it) }
    val summaries by viewModel.summaries.collectAsStateWithLifecycle()

    ScreenScaffold(title = "Syllabus", onBack = { navigator.navigateUp() }) {
        if (summaries.isEmpty()) ExpressiveEmptyState(AppIcons.School, "No subjects yet", "Add a subject first (Vault, Edit subjects), then build its syllabus here.")
        summaries.forEachIndexed { index, s ->
            androidx.compose.runtime.key(s.subjectId) { SubjectSection(s, index, navigator) }
        }
    }
}

/** One subject: its name and progress as a header, and its whole syllabus below when opened. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SubjectSection(s: SyllabusSummary, index: Int, navigator: DestinationsNavigator) {
    val container = androidx.compose.ui.platform.LocalContext.current.appContainer
    val syllabus: SyllabusViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        key = "syllabus-all-${s.subjectId}",
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = SyllabusViewModel(container, s.subjectId) as T
        },
    )
    val ui = androidx.compose.runtime.remember { SyllabusUiState() }
    var open by androidx.compose.runtime.saveable.rememberSaveable(s.subjectId) { androidx.compose.runtime.mutableStateOf(false) }
    val turn by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (open) 180f else 0f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.7f, stiffness = 380f),
        label = "turn",
    )
    AppCard(modifier = Modifier.fillMaxWidth().bounceIn((index * 45).coerceAtMost(450)), onClick = { open = !open }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(s.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (s.items == 0) "No syllabus yet" else "${s.items} ${if (s.items == 1) "item" else "items"} · ${(s.progress * 100).toInt()}% studied",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            androidx.compose.material3.Icon(AppIcons.ExpandMore, contentDescription = if (open) "Close" else "Open", modifier = Modifier.graphicsLayer { rotationZ = turn })
        }
        if (s.items > 0) LinearWavyProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
    }
    androidx.compose.animation.AnimatedVisibility(
        visible = open,
        enter = androidx.compose.animation.expandVertically(androidx.compose.animation.core.spring(0.85f, 380f)) + androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.shrinkVertically(androidx.compose.animation.core.spring(1f, 500f)) + androidx.compose.animation.fadeOut(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton("Add unit", onClick = { ui.editing = Editing.Add(null, SyllabusTree.levelName(0)) }, compact = true)
                PillButton("Import outline", onClick = { syllabus.dismissImport(); ui.showImport = true }, tonal = true, compact = true)
                PillButton("Open page", onClick = { navigator.navigate(SyllabusScreenDestination(subjectId = s.subjectId)) }, tonal = true, compact = true)
            }
            SyllabusContent(syllabus, ui, rootId = null, onUnitGone = {}, showHeader = false)
        }
    }
    SyllabusDialogs(syllabus, ui)
}
