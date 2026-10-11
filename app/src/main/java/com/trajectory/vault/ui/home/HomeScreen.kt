package com.trajectory.vault.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.SettingsHubScreenDestination
import com.ramcosta.composedestinations.generated.destinations.TestLabScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.trajectory.vault.ui.components.GlassCard
import com.trajectory.vault.ui.components.NavCard
import com.trajectory.vault.ui.components.ScreenScaffold
import com.trajectory.vault.ui.description
import com.trajectory.vault.ui.label
import com.trajectory.vault.ui.settings.rememberSettingsViewModel
import com.trajectory.vault.ui.theme.effectiveColorEngine

@Destination<RootGraph>(start = true)
@Composable
fun HomeScreen(navigator: DestinationsNavigator) {
    val viewModel = rememberSettingsViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val engine = effectiveColorEngine(settings.colorEngine)

    ScreenScaffold(title = "Trajectory", onBack = null) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("Welcome", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "This is an early test build. Open Settings to change the theme live, or the Test Lab to check the database, storage and log.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("Your look", style = MaterialTheme.typography.titleMedium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(
                    text = "Accent",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(settings.accentColor)),
                )
            }
            Text("Theme: ${settings.themeMode.label()}", style = MaterialTheme.typography.bodyMedium)
            Text("Colors: ${engine.label()}", style = MaterialTheme.typography.bodyMedium)
            if (engine != settings.colorEngine) {
                Text(
                    text = "${settings.colorEngine.label()} is selected but not available on this Android version, " +
                        "so Solid Accent is used.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = engine.description(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        NavCard(
            emoji = "⚙️",
            title = "Settings",
            subtitle = "General, appearance, storage and data, AI",
            onClick = { navigator.navigate(SettingsHubScreenDestination) },
        )
        NavCard(
            emoji = "🧪",
            title = "Test Lab",
            subtitle = "Check the database, storage and debug log",
            onClick = { navigator.navigate(TestLabScreenDestination) },
        )
    }
}
