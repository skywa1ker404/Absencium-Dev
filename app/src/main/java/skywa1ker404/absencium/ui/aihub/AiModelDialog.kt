package skywa1ker404.absencium.ui.aihub

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import skywa1ker404.absencium.data.ai.AiConnection
import skywa1ker404.absencium.data.ai.AiProvider
import skywa1ker404.absencium.ui.components.AlertDialog
import skywa1ker404.absencium.ui.components.AppTextField
import skywa1ker404.absencium.ui.components.ExpressiveChip
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.containerViewModel
import skywa1ker404.absencium.ui.settings.AiModelsViewModel
import skywa1ker404.absencium.ui.settings.ModelPickerCard

/**
 * Chooses the provider and model for this chat. Only providers with a saved key (or none needed) and a saved URL are offered; the
 * model list is fetched from the provider on request, and any model name can be typed.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiModelDialog(
    providers: List<Pair<AiProvider, String>>,
    current: AiConnection?,
    connectionFor: (AiProvider) -> AiConnection?,
    onPick: (AiProvider, String) -> Unit,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var provider by remember { mutableStateOf(current?.provider ?: providers.firstOrNull()?.first) }
    var typed by remember { mutableStateOf("") }
    val modelsViewModel = containerViewModel { AiModelsViewModel(it) }
    val modelsState by modelsViewModel.state.collectAsStateWithLifecycle()
    val chosen = provider

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Choose a model") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (providers.isEmpty() || chosen == null) {
                    Text("No provider is ready. Add one with its API key in the AI settings.", style = MaterialTheme.typography.bodyMedium)
                    PillButton(text = "Open AI settings", onClick = { onDismiss(); onOpenSettings() })
                } else {
                    Text("Provider", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        providers.forEach { (option, _) ->
                            ExpressiveChip(
                                selected = chosen == option,
                                onClick = { provider = option; modelsViewModel.reset() },
                                label = { Text(option.label, maxLines = 1, softWrap = false) },
                                shape = CircleShape,
                            )
                        }
                    }
                    val saved = providers.firstOrNull { it.first == chosen }?.second.orEmpty()
                    PillButton(text = "Use saved model: $saved", tonal = true, onClick = { onPick(chosen, saved) }, maxLines = 2)
                    ModelPickerCard(
                        provider = chosen,
                        state = modelsState,
                        selectedId = if (current?.provider == chosen) current?.model.orEmpty() else saved,
                        onFetch = { connectionFor(chosen)?.let { modelsViewModel.load(it) } },
                        onPick = { onPick(chosen, it) },
                    )
                    AppTextField(
                        value = typed,
                        onValueChange = { typed = it },
                        label = { Text("Or type a model name") },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                    if (typed.isNotBlank()) PillButton(text = "Use $typed", onClick = { onPick(chosen, typed.trim()) }, maxLines = 2)
                    HintText("This changes the model for this chat only. Set the default in the AI settings.")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
