package com.trajectory.vault.ui.settings

import androidx.compose.runtime.Composable
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AboutScreenDestination
import com.ramcosta.composedestinations.generated.destinations.AiSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ChangelogScreenDestination
import com.ramcosta.composedestinations.generated.destinations.IntroTourScreenDestination
import com.ramcosta.composedestinations.generated.destinations.AppearanceSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.GeneralSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.StorageSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.TestLabScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.trajectory.vault.ui.components.NavCard
import com.trajectory.vault.ui.icons.AppIcons
import com.trajectory.vault.ui.components.ScreenScaffold
import com.trajectory.vault.ui.components.SectionTitle

@Destination<RootGraph>
@Composable
fun SettingsHubScreen(navigator: DestinationsNavigator) {
    ScreenScaffold(title = "Settings", onBack = { navigator.navigateUp() }) {
        NavCard(
            icon = AppIcons.School,
            title = "General",
            subtitle = "Attendance threshold, Focus Guard, Exam Mode, subjects",
            onClick = { navigator.navigate(GeneralSettingsScreenDestination) },
        )
        NavCard(
            icon = AppIcons.Palette,
            title = "Appearance",
            subtitle = "Visual quality, theme mode, color engine, accent color",
            onClick = { navigator.navigate(AppearanceSettingsScreenDestination) },
        )
        NavCard(
            icon = AppIcons.Storage,
            title = "Storage & Data",
            subtitle = "Notes folder, backup and restore, archive tools",
            onClick = { navigator.navigate(StorageSettingsScreenDestination) },
        )
        NavCard(
            icon = AppIcons.AutoAwesome,
            title = "AI & Integrations",
            subtitle = "Ask AI, API key vault, Momentum tracker",
            onClick = { navigator.navigate(AiSettingsScreenDestination) },
        )

        SectionTitle("App")
        NavCard(
            icon = AppIcons.PlayCircle,
            title = "Replay intro",
            subtitle = "A short tour of what Absencium is for",
            onClick = { navigator.navigate(IntroTourScreenDestination) },
        )
        NavCard(
            icon = AppIcons.History,
            title = "Changelog",
            subtitle = "What changed in each version",
            onClick = { navigator.navigate(ChangelogScreenDestination) },
        )
        NavCard(
            icon = AppIcons.Info,
            title = "About",
            subtitle = "Version, developer and source code",
            onClick = { navigator.navigate(AboutScreenDestination) },
        )

        SectionTitle("Developer")
        NavCard(
            icon = AppIcons.Science,
            title = "Test Lab",
            subtitle = "Check the database, storage and debug log on this device",
            onClick = { navigator.navigate(TestLabScreenDestination) },
        )
    }
}
