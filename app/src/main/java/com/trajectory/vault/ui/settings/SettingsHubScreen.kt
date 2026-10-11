package com.trajectory.vault.ui.settings

import androidx.compose.runtime.Composable
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AiSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.AppearanceSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.GeneralSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.StorageSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.TestLabScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.trajectory.vault.ui.components.NavCard
import com.trajectory.vault.ui.components.ScreenScaffold
import com.trajectory.vault.ui.components.SectionTitle

@Destination<RootGraph>
@Composable
fun SettingsHubScreen(navigator: DestinationsNavigator) {
    ScreenScaffold(title = "Settings", onBack = { navigator.navigateUp() }) {
        NavCard(
            emoji = "🎓",
            title = "General",
            subtitle = "Attendance threshold, Focus Guard, Exam Mode, subjects",
            onClick = { navigator.navigate(GeneralSettingsScreenDestination) },
        )
        NavCard(
            emoji = "🎨",
            title = "Appearance",
            subtitle = "Theme mode, color engine, accent color",
            onClick = { navigator.navigate(AppearanceSettingsScreenDestination) },
        )
        NavCard(
            emoji = "🗄️",
            title = "Storage & Data",
            subtitle = "Notes folder, backup and restore, archive tools",
            onClick = { navigator.navigate(StorageSettingsScreenDestination) },
        )
        NavCard(
            emoji = "✨",
            title = "AI & Integrations",
            subtitle = "Ask AI, API key vault, Momentum tracker",
            onClick = { navigator.navigate(AiSettingsScreenDestination) },
        )

        SectionTitle("Developer")
        NavCard(
            emoji = "🧪",
            title = "Test Lab",
            subtitle = "Check the database, storage and debug log on this device",
            onClick = { navigator.navigate(TestLabScreenDestination) },
        )
    }
}
