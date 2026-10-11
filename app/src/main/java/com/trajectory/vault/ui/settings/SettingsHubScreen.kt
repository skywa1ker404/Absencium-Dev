package com.trajectory.vault.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AppearanceSettingsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.trajectory.vault.ui.components.GlassCard
import com.trajectory.vault.ui.components.ScreenScaffold

@Destination<RootGraph>
@Composable
fun SettingsHubScreen(navigator: DestinationsNavigator) {
    ScreenScaffold(title = "Settings", onBack = { navigator.navigateUp() }) {
        SettingsEntry(
            emoji = "🎨",
            title = "Appearance",
            subtitle = "Theme mode, color engine, accent color",
            onClick = { navigator.navigate(AppearanceSettingsScreenDestination) },
        )
    }
}

@Composable
private fun SettingsEntry(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Text("$emoji  $title", style = MaterialTheme.typography.titleMedium)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
