package com.absencium.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import com.absencium.app.ui.components.AppTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absencium.app.AppContainer
import com.absencium.app.data.ai.AiConnection
import com.absencium.app.data.ai.AiException
import com.absencium.app.data.ai.AiModel
import com.absencium.app.data.ai.AiProvider
import com.absencium.app.data.ai.ModelCost
import com.absencium.app.logging.DebugLogger
import com.absencium.app.ui.components.ExpressiveChip
import com.absencium.app.ui.components.HintText
import com.absencium.app.ui.components.PillButton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AiModelsState(
    val loading: Boolean = false,
    val models: List<AiModel> = emptyList(),
    val error: String? = null,
    /** True once a fetch has finished successfully, so an empty list can be told apart from "not fetched yet". */
    val fetched: Boolean = false,
)

/** Fetches the model list of the provider currently chosen in the settings form. */
class AiModelsViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(AiModelsState())
    val state: StateFlow<AiModelsState> = _state.asStateFlow()

    fun load(connection: AiConnection) {
        _state.value = AiModelsState(loading = true)
        viewModelScope.launch {
            _state.value = try {
                AiModelsState(models = container.aiClient.listModels(connection), fetched = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: AiException) {
                AiModelsState(error = e.message)
            } catch (e: Exception) {
                DebugLogger.e("AiModels", "Could not fetch the model list", e)
                AiModelsState(error = "Could not fetch the model list. See debug.log for details.")
            }
        }
    }

    /** Forget the list when the provider changes, because it belongs to the old one. */
    fun reset() {
        _state.value = AiModelsState()
    }
}

private enum class CostFilter { ALL, FREE, PAID }

private const val MAX_SHOWN = 40

/**
 * The list of models a provider offers, with a search box. OpenRouter publishes prices, so its models
 * are labelled Free or Paid and can be filtered; the other providers' lists do not say, and the card
 * says so instead of guessing. Tapping a model fills the model name field.
 */
@Composable
fun ModelPickerCard(
    provider: AiProvider,
    state: AiModelsState,
    selectedId: String,
    onFetch: () -> Unit,
    onPick: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(CostFilter.ALL) }

    PillButton(
        text = if (state.fetched) "Refresh available models" else "Fetch available models",
        tonal = true,
        loading = state.loading,
        onClick = onFetch,
        modifier = Modifier.padding(top = 12.dp),
    )
    state.error?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = colors.error,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
    if (!state.fetched) return

    val priced = state.models.any { it.cost != ModelCost.UNKNOWN }
    if (!priced) {
        HintText(
            text = "${provider.label} does not say in its model list which models are free, so no Free or Paid " +
                "label is shown. OpenRouter is the one listed provider that publishes prices.",
            modifier = Modifier.padding(top = 8.dp),
        )
    }
    AppTextField(
        value = query,
        onValueChange = { query = it },
        label = { Text("Search ${state.models.size} models") },
        singleLine = true,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
    if (priced) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            CostFilter.entries.forEach { option ->
                ExpressiveChip(
                    selected = filter == option,
                    onClick = { filter = option },
                    label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) },
                )
            }
        }
    }
    val shown = state.models
        .filter { model ->
            (query.isBlank() || model.label.contains(query, ignoreCase = true) || model.id.contains(query, ignoreCase = true)) &&
                when (filter) {
                    CostFilter.ALL -> true
                    CostFilter.FREE -> model.cost == ModelCost.FREE
                    CostFilter.PAID -> model.cost == ModelCost.PAID
                }
        }
    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        shown.take(MAX_SHOWN).forEach { model ->
            val selected = model.id == selectedId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selected) colors.primaryContainer else colors.surfaceContainerHigh)
                    .clickable { onPick(model.id) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (selected) colors.onPrimaryContainer else colors.onSurface,
                        maxLines = 1,
                    )
                    if (model.label != model.id) {
                        Text(
                            text = model.id,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
                if (model.acceptsImages == true) Badge("Images", colors.secondaryContainer, colors.onSecondaryContainer)
                when (model.cost) {
                    ModelCost.FREE -> Badge("Free", colors.tertiaryContainer, colors.onTertiaryContainer)
                    ModelCost.PAID -> Badge("Paid", colors.surfaceContainerHighest, colors.onSurface)
                    ModelCost.UNKNOWN -> Unit
                }
            }
        }
    }
    if (shown.size > MAX_SHOWN) {
        HintText("Showing the first $MAX_SHOWN of ${shown.size}. Type in the search box to narrow it down.", Modifier.padding(top = 6.dp))
    }
    if (shown.isEmpty()) HintText("No models match.", Modifier.padding(top = 6.dp))
}

@Composable
private fun Badge(text: String, container: androidx.compose.ui.graphics.Color, content: androidx.compose.ui.graphics.Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = content,
        modifier = Modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
