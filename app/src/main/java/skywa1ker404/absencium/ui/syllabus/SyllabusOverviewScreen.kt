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

/** Every active subject with its syllabus progress; tap one to open its tree. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Destination<RootGraph>
@Composable
fun SyllabusOverviewScreen(navigator: DestinationsNavigator) {
    val viewModel = containerViewModel { SyllabusOverviewViewModel(it) }
    val summaries by viewModel.summaries.collectAsStateWithLifecycle()

    ScreenScaffold(title = "Syllabus", onBack = { navigator.navigateUp() }) {
        if (summaries.isEmpty()) ExpressiveEmptyState(AppIcons.School, "No subjects yet", "Add a subject first (Vault, Edit subjects), then build its syllabus here.")
        else HintText("Units, chapters and topics for each subject. Notes and study material can be filed under any of them.")
        summaries.forEachIndexed { index, s ->
            AppCard(
                modifier = Modifier.fillMaxWidth().bounceIn((index * 45).coerceAtMost(450)),
                onClick = { navigator.navigate(SyllabusScreenDestination(subjectId = s.subjectId)) },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(s.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (s.items == 0) "Not started" else "${(s.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (s.items == 0) "No syllabus yet" else "${s.items} ${if (s.items == 1) "item" else "items"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (s.items > 0) {
                        LinearWavyProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                    }
                }
            }
        }
    }
}
