package skywa1ker404.absencium.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import skywa1ker404.absencium.data.ai.AiProvider
import skywa1ker404.absencium.ui.AiProvidersViewModel
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.containerViewModel

/** Adds a provider (no argument) or edits one ([provider] is its name). */
@Destination<RootGraph>
@Composable
fun AiProviderScreen(navigator: DestinationsNavigator, provider: String? = null) {
    val viewModel = containerViewModel { AiProvidersViewModel(it) }
    val editing = provider?.let { name -> AiProvider.entries.firstOrNull { it.name == name } }
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    ScreenScaffold(
        title = when {
            editing == null -> "Add a provider"
            rows.any { it.provider == editing } -> "Edit ${editing.label}"
            else -> "Set up ${editing.label}"
        },
        onBack = { navigator.navigateUp() },
    ) {
        AiProviderForm(initial = editing, viewModel = viewModel, onDone = { navigator.navigateUp() })
    }
}
