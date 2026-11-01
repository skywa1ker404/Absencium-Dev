package skywa1ker404.absencium.ui.aihub

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import skywa1ker404.absencium.data.ai.AiConnection
import skywa1ker404.absencium.data.ai.AiProvider
import skywa1ker404.absencium.data.ai.ModelCost
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.components.sheetContainerColor
import skywa1ker404.absencium.ui.components.sheetScrimColor
import skywa1ker404.absencium.ui.containerViewModel
import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.settings.AiModelsViewModel

/**
 * Chooses the provider and model for this chat, as a compact bottom sheet. Only providers with a saved key (or none needed) and a saved
 * URL are offered; the model list is fetched from the provider on request, and any model name can be typed.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AiModelDialog(
    providers: List<Pair<AiProvider, String>>,
    current: AiConnection?,
    connectionFor: (AiProvider) -> AiConnection?,
    onPick: (AiProvider, String) -> Unit,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var provider by remember { mutableStateOf(current?.provider ?: providers.firstOrNull()?.first) }
    var typed by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    val modelsViewModel = containerViewModel { AiModelsViewModel(it) }
    val state by modelsViewModel.state.collectAsStateWithLifecycle()
    val chosen = provider

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = sheetContainerColor(),
        scrimColor = sheetScrimColor(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp).navigationBarsPadding().imePadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Model", style = MaterialTheme.typography.titleMedium)
            if (providers.isEmpty() || chosen == null) {
                Text("No provider is ready. Add one with its API key in the AI settings.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                PillButton(text = "Open AI settings", onClick = { onDismiss(); onOpenSettings() })
                return@Column
            }
            val saved = providers.firstOrNull { it.first == chosen }?.second.orEmpty()
            val active = if (current?.provider == chosen) current.model else saved

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(providers.map { it.first }) { option ->
                    Pill(option.label, selected = chosen == option) { if (chosen != option) { provider = option; query = ""; modelsViewModel.reset() } }
                }
            }

            if (saved.isNotBlank()) {
                ModelRow(title = saved, subtitle = "Saved for this provider", selected = active == saved, tag = null) { onPick(chosen, saved) }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SearchPill(value = query, onChange = { query = it }, placeholder = if (state.fetched) "Search ${state.models.size} models" else "Search models", modifier = Modifier.weight(1f))
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    if (state.loading) {
                        LoadingIndicator(modifier = Modifier.size(28.dp))
                    } else {
                        IconButton(onClick = { connectionFor(chosen)?.let { modelsViewModel.load(it) } }) {
                            Icon(AppIcons.Refresh, contentDescription = if (state.fetched) "Refresh the model list" else "Fetch the model list")
                        }
                    }
                }
            }
            state.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.error) }
            if (!state.fetched && state.error == null && !state.loading) {
                Text("Tap the refresh button to list every model this provider offers.", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
            if (state.fetched) {
                val shown = remember(state.models, query) {
                    state.models.filter { query.isBlank() || it.label.contains(query, true) || it.id.contains(query, true) }
                }
                LazyColumn(Modifier.heightIn(max = 240.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(shown.take(100), key = { it.id }) { model ->
                        ModelRow(
                            title = model.label,
                            subtitle = model.id.takeIf { it != model.label },
                            selected = model.id == active,
                            tag = when (model.cost) { ModelCost.FREE -> "Free"; ModelCost.PAID -> "Paid"; ModelCost.UNKNOWN -> null },
                        ) { onPick(chosen, model.id) }
                    }
                }
                if (shown.isEmpty()) Text("No models match.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SearchPill(value = typed, onChange = { typed = it }, placeholder = "Or type a model name", leading = false, modifier = Modifier.weight(1f))
                TextButton(onClick = { onPick(chosen, typed.trim()) }, enabled = typed.isNotBlank()) { Text("Use") }
            }
            Text("This changes the model for this chat only. The default is set in the AI settings.", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
    }
}

/** A provider choice: one fixed height and no icon that appears on selection, so the row never shifts when one is tapped. */
@Composable
private fun Pill(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val background by animateColorAsState(if (selected) colors.primaryContainer else colors.surfaceContainerHigh, spring(stiffness = 500f), label = "pillBg")
    Box(
        modifier = Modifier.height(34.dp).clip(CircleShape).background(background).clickable(onClick = onClick).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colors.onPrimaryContainer else colors.onSurface,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun SearchPill(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, leading: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier.height(40.dp).clip(CircleShape).background(colors.surfaceContainerHigh).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (leading) Icon(AppIcons.Search, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface),
                cursorBrush = SolidColor(colors.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ModelRow(title: String, subtitle: String?, selected: Boolean, tag: String?, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) colors.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (selected) colors.onSecondaryContainer else colors.onSurface)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.onSurfaceVariant)
        }
        if (tag != null) Text(tag, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        if (selected) Icon(AppIcons.Check, contentDescription = "In use", tint = colors.onSecondaryContainer, modifier = Modifier.size(18.dp))
    }
}
