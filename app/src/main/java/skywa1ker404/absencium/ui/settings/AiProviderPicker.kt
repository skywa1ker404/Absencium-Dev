package skywa1ker404.absencium.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import skywa1ker404.absencium.data.ai.AiProvider
import skywa1ker404.absencium.ui.aihub.SearchPill
import skywa1ker404.absencium.ui.components.SettingRow
import skywa1ker404.absencium.ui.components.sheetContainerColor
import skywa1ker404.absencium.ui.components.sheetScrimColor
import skywa1ker404.absencium.ui.icons.AppIcons

/** One line about each provider, for the picker. */
fun AiProvider.blurb(): String = when (this) {
    AiProvider.OPENAI -> "GPT models. Uses your own API key."
    AiProvider.ANTHROPIC -> "Claude models. Uses your own API key."
    AiProvider.GEMINI -> "Gemini models from Google. Uses your own API key."
    AiProvider.OPENROUTER -> "Many models behind one key, some of them free."
    AiProvider.GROQ -> "Fast open models. Uses your own API key."
    AiProvider.OLLAMA -> "Models on your own computer or server. No key needed."
    AiProvider.CUSTOM -> "Any server that speaks the OpenAI, Claude or Gemini API."
}

/** A round badge with the provider's first letter: no logos are bundled, so nothing is fetched or shipped. */
@Composable
fun ProviderBadge(provider: AiProvider, modifier: Modifier = Modifier, active: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier.size(40.dp).clip(CircleShape).background(if (active) colors.primary else colors.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            provider.label.first().uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = if (active) colors.onPrimary else colors.onSecondaryContainer,
        )
    }
}

/**
 * Pick which provider to set up: a search pill, the custom endpoint as an accent row, then the ready-made providers with one line
 * each. A provider already added is marked and opens for editing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiProviderPickerSheet(added: Set<AiProvider>, onPick: (AiProvider) -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    val shown = AiProvider.entries.filter { it != AiProvider.CUSTOM && (query.isBlank() || it.label.contains(query, true) || it.blurb().contains(query, true)) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = sheetContainerColor(),
        scrimColor = sheetScrimColor(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 12.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Add a provider", style = MaterialTheme.typography.titleMedium)
            SearchPill(value = query, onChange = { query = it }, placeholder = "Search providers", modifier = Modifier.fillMaxWidth())
            SettingRow(
                title = "Add a custom provider",
                subtitle = AiProvider.CUSTOM.blurb(),
                onClick = { onPick(AiProvider.CUSTOM) },
                leading = {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(colors.primaryContainer), contentAlignment = Alignment.Center) {
                        Icon(AppIcons.Add, contentDescription = null, tint = colors.onPrimaryContainer)
                    }
                },
            )
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                items(shown, key = { it.name }) { provider ->
                    SettingRow(
                        title = provider.label,
                        subtitle = provider.blurb(),
                        onClick = { onPick(provider) },
                        leading = { ProviderBadge(provider) },
                        trailing = {
                            if (provider in added) Text("Added", style = MaterialTheme.typography.labelSmall, color = colors.primary)
                            else Icon(AppIcons.ChevronRight, contentDescription = null, tint = colors.onSurfaceVariant)
                        },
                    )
                }
            }
            if (shown.isEmpty()) Text("No provider matches. Use a custom provider for any other server.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}
