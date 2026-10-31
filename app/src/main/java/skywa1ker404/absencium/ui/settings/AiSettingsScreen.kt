package skywa1ker404.absencium.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AiHubScreenDestination
import com.ramcosta.composedestinations.generated.destinations.AiProviderScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import skywa1ker404.absencium.data.ai.AiScope
import skywa1ker404.absencium.ui.AiProvidersViewModel
import skywa1ker404.absencium.ui.AiPermissionsViewModel
import skywa1ker404.absencium.ui.ProviderRow
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.components.SectionTitle
import skywa1ker404.absencium.ui.components.SwitchCard
import skywa1ker404.absencium.ui.containerViewModel
import skywa1ker404.absencium.ui.icons.AppIcons

/**
 * Everything about AI in one place: the master switch, the providers the student set up (each with its own key; tap one to use it),
 * which parts of the app Absencium AI may use, and the image options.
 */
@Destination<RootGraph>
@Composable
fun AiSettingsScreen(navigator: DestinationsNavigator) {
    val settingsViewModel = rememberSettingsViewModel()
    val providersViewModel = containerViewModel { AiProvidersViewModel(it) }
    val permissionsViewModel = containerViewModel { AiPermissionsViewModel(it) }
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val rows by providersViewModel.rows.collectAsStateWithLifecycle()
    val allowed by permissionsViewModel.allowed.collectAsStateWithLifecycle()
    val active = rows.firstOrNull { it.active }
    val ready = active != null && active.complete

    ScreenScaffold(title = "AI & Integrations", onBack = { navigator.navigateUp() }) {
        StatusCard(settings.askAiEnabled, ready, active, onOpen = { navigator.navigate(AiHubScreenDestination) })
        AskAiToggle(settings, settingsViewModel, ready = ready)

        SectionTitle("Your providers")
        if (rows.isEmpty()) {
            HintText("No provider yet. Add one with your own API key; each provider keeps its own key, so you can switch any time.")
        }
        rows.forEach { row ->
            ProviderCard(
                row = row,
                onUse = { if (row.complete) providersViewModel.use(row.provider) else navigator.navigate(AiProviderScreenDestination(provider = row.provider.name)) },
                onEdit = { navigator.navigate(AiProviderScreenDestination(provider = row.provider.name)) },
            )
        }
        PillButton(text = "Add a provider", onClick = { navigator.navigate(AiProviderScreenDestination()) }, tonal = rows.isNotEmpty())

        SectionTitle("What Absencium AI can use")
        HintText("Everything is off until you allow it. It sees only what you allow, and every change it suggests waits for your tap on Apply. Note text is never sent, only titles.")
        AiScope.entries.forEach { scope ->
            SwitchCard(
                title = scope.label,
                subtitle = scope.blurb,
                checked = scope in allowed,
                onCheckedChange = { permissionsViewModel.set(scope, it) },
                enabled = settings.askAiEnabled,
            )
        }

        SectionTitle("Images in generated notes")
        SwitchCard(
            title = "Search Images (experimental)",
            subtitle = "Lets Generate Notes look for free pictures online (Wikimedia Commons, Openverse). Only search words leave the phone. Results can be unrelated, so check each one.",
            checked = settings.searchImagesEnabled,
            onCheckedChange = settingsViewModel::setSearchImagesEnabled,
        )
        SwitchCard(
            title = "Generate Images",
            subtitle = "Lets Generate Notes ask your AI to draw pictures. Needs a paid Google Gemini or OpenAI plan; free plans have no image quota.",
            checked = settings.generateImagesEnabled,
            onCheckedChange = settingsViewModel::setGenerateImagesEnabled,
        )

        SectionTitle("Integrations")
        MomentumToggle(settings, settingsViewModel)
        HintText("Absencium works fully offline. AI is only used when you turn it on and ask.")
    }
}

@Composable
private fun StatusCard(enabled: Boolean, ready: Boolean, active: ProviderRow?, onOpen: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    AppCard(modifier = Modifier.fillMaxWidth(), container = if (enabled && ready) colors.primaryContainer else colors.surfaceContainerHigh) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(AppIcons.AutoAwesome, contentDescription = null, tint = if (enabled && ready) colors.onPrimaryContainer else colors.onSurfaceVariant)
            Column(Modifier.weight(1f)) {
                Text("Absencium AI", style = MaterialTheme.typography.titleMedium, color = if (enabled && ready) colors.onPrimaryContainer else colors.onSurface)
                Text(
                    text = when {
                        !enabled -> "Off. Turn on AI Features below to use it."
                        ready && active != null -> "Ready with ${active.provider.label}, ${active.model}."
                        else -> "Add a provider below to get started."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled && ready) colors.onPrimaryContainer else colors.onSurfaceVariant,
                )
            }
        }
        if (enabled && ready) PillButton(text = "Open chat", onClick = onOpen, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun ProviderCard(row: ProviderRow, onUse: () -> Unit, onEdit: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onUse, highlighted = row.active) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(row.provider.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                    if (row.active) {
                        Text(
                            "In use",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onPrimary,
                            modifier = Modifier.clip(CircleShape).background(colors.primary).padding(horizontal = 10.dp, vertical = 3.dp),
                        )
                    }
                }
                Text(
                    text = listOfNotNull(
                        row.model.ifBlank { "No model chosen" },
                        if (row.hasKey) "key •••• ${row.lastFour.orEmpty()}" else if (row.provider.keyRequired) "no key yet" else "no key needed",
                    ).joinToString("  ·  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                )
                if (!row.active) {
                    Text(
                        text = if (row.complete) "Tap to use" else "Tap to finish setting up",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            IconButton(onClick = onEdit) { Icon(AppIcons.Edit, contentDescription = "Edit ${row.provider.label}", tint = colors.onSurfaceVariant) }
        }
    }
}
