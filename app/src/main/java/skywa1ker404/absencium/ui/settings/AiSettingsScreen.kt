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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import skywa1ker404.absencium.ui.AiPermissionsViewModel
import skywa1ker404.absencium.ui.AiProvidersViewModel
import skywa1ker404.absencium.ui.ProviderRow
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.ExpressiveEmptyState
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.components.SettingCaption
import skywa1ker404.absencium.ui.components.SettingDivider
import skywa1ker404.absencium.ui.components.SettingGroup
import skywa1ker404.absencium.ui.components.SettingRow
import skywa1ker404.absencium.ui.components.SettingSwitchRow
import skywa1ker404.absencium.ui.containerViewModel
import skywa1ker404.absencium.ui.icons.AppIcons

/**
 * Everything about AI in one place, in short grouped lists: the master switch, the providers the student set up (tap one to use it),
 * which parts of the app Absencium AI may use, the image options and integrations.
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
    val usage by providersViewModel.usage.collectAsStateWithLifecycle()
    val active = rows.firstOrNull { it.active }
    val ready = active != null && active.complete
    var picking by remember { mutableStateOf(false) }

    ScreenScaffold(title = "AI & Integrations", onBack = { navigator.navigateUp() }) {
        StatusCard(settings.askAiEnabled, ready, active, onOpen = { navigator.navigate(AiHubScreenDestination()) })
        AskAiToggle(settings, settingsViewModel, ready = ready)

        SettingCaption("Providers")
        if (rows.isEmpty()) {
            ExpressiveEmptyState(
                icon = AppIcons.AutoAwesome,
                title = "No providers yet",
                text = "Add the AI provider you have an API key for. Each provider keeps its own keys, so you can switch any time.",
                action = { PillButton(text = "Add a provider", onClick = { picking = true }) },
            )
        } else {
            SettingGroup {
                rows.forEachIndexed { i, row ->
                    if (i > 0) SettingDivider()
                    ProviderRowItem(
                        row = row,
                        onUse = { if (row.complete) providersViewModel.use(row.provider) else navigator.navigate(AiProviderScreenDestination(provider = row.provider.name)) },
                        onEdit = { navigator.navigate(AiProviderScreenDestination(provider = row.provider.name)) },
                    )
                }
                SettingDivider()
                SettingRow(
                    title = "Add a provider",
                    onClick = { picking = true },
                    leading = {
                        Icon(AppIcons.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp))
                    },
                )
            }
        }

        SettingCaption("Token usage")
        if (usage.isEmpty()) {
            HintText("Nothing counted yet. Each answer's tokens are added here, per provider, as the provider reports them. Counts stay on this phone.")
        } else {
            SettingGroup {
                usage.forEachIndexed { i, row ->
                    if (i > 0) SettingDivider()
                    SettingRow(
                        title = row.provider.label,
                        subtitle = "${row.requests} answers · ${"%,d".format(row.tokensIn)} in · ${"%,d".format(row.tokensOut)} out · ${"%,d".format(row.tokensToday)} today",
                    )
                }
                PillButton(text = "Reset counts", onClick = providersViewModel::resetUsage, tonal = true, compact = true, modifier = Modifier.padding(top = 6.dp))
            }
        }

        SettingCaption("What Absencium AI can use")
        SettingGroup {
            AiScope.entries.forEachIndexed { i, scope ->
                if (i > 0) SettingDivider()
                SettingSwitchRow(
                    title = scope.label,
                    subtitle = scope.blurb,
                    checked = scope in allowed,
                    onCheckedChange = { permissionsViewModel.set(scope, it) },
                    enabled = settings.askAiEnabled,
                )
            }
        }
        HintText("Everything is off until you allow it. It sees only what you allow, and every change it suggests waits for your tap on Apply. Note text is never sent, only titles.")

        SettingCaption("Images in generated notes")
        SettingGroup {
            SettingSwitchRow(
                title = "Search images (experimental)",
                subtitle = "Looks for free pictures on Wikimedia Commons and Openverse. Only search words leave the phone; check each result.",
                checked = settings.searchImagesEnabled,
                onCheckedChange = settingsViewModel::setSearchImagesEnabled,
            )
            SettingDivider()
            SettingSwitchRow(
                title = "Generate images",
                subtitle = "Asks your AI to draw pictures. Needs a paid Google Gemini or OpenAI plan.",
                checked = settings.generateImagesEnabled,
                onCheckedChange = settingsViewModel::setGenerateImagesEnabled,
            )
        }

        SettingCaption("Integrations")
        SettingGroup {
            SettingSwitchRow(
                title = "Momentum tracker",
                subtitle = "Daily streak and study momentum. $OPTIONAL_NOTE",
                checked = settings.momentumTrackerEnabled,
                onCheckedChange = settingsViewModel::setMomentumTrackerEnabled,
            )
        }
        HintText("Absencium works fully offline. AI is only used when you turn it on and ask.")
    }

    if (picking) {
        AiProviderPickerSheet(
            added = rows.map { it.provider }.toSet(),
            onPick = { picking = false; navigator.navigate(AiProviderScreenDestination(provider = it.name)) },
            onDismiss = { picking = false },
        )
    }
}

@Composable
private fun StatusCard(enabled: Boolean, ready: Boolean, active: ProviderRow?, onOpen: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val good = enabled && ready
    AppCard(modifier = Modifier.fillMaxWidth(), container = if (good) colors.primaryContainer else colors.surfaceContainerHigh) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(AppIcons.AutoAwesome, contentDescription = null, tint = if (good) colors.onPrimaryContainer else colors.onSurfaceVariant)
            Column(Modifier.weight(1f)) {
                Text("Absencium AI", style = MaterialTheme.typography.titleMedium, color = if (good) colors.onPrimaryContainer else colors.onSurface)
                Text(
                    text = when {
                        !enabled -> "Off. Turn on AI Features below to use it."
                        ready && active != null -> "Ready with ${active.provider.label}, ${active.model}."
                        else -> "Add a provider below to get started."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (good) colors.onPrimaryContainer else colors.onSurfaceVariant,
                )
            }
            if (good) PillButton(text = "Open chat", onClick = onOpen, compact = true)
        }
    }
}

@Composable
private fun ProviderRowItem(row: ProviderRow, onUse: () -> Unit, onEdit: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    SettingRow(
        title = row.provider.label,
        subtitle = listOfNotNull(
            row.model.ifBlank { "No model chosen" },
            when {
                row.keyCount > 1 -> "${row.keyCount} keys"
                row.hasKey -> "key •••• ${row.lastFour.orEmpty()}"
                row.provider.keyRequired -> "no key yet"
                else -> "no key needed"
            },
            if (!row.complete) "tap to finish" else null,
        ).joinToString("  ·  "),
        onClick = onUse,
        leading = { ProviderBadge(row.provider, active = row.active) },
        trailing = {
            if (row.active) {
                Text(
                    "In use",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onPrimary,
                    modifier = Modifier.clip(CircleShape).background(colors.primary).padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
            IconButton(onClick = onEdit) { Icon(AppIcons.Edit, contentDescription = "Edit ${row.provider.label}", tint = colors.onSurfaceVariant) }
        },
    )
}
