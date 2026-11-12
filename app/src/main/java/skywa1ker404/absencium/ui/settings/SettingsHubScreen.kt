package skywa1ker404.absencium.ui.settings

import androidx.compose.runtime.Composable
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AboutScreenDestination
import com.ramcosta.composedestinations.generated.destinations.AiSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ChangelogScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RemindersScreenDestination
import com.ramcosta.composedestinations.generated.destinations.IntroTourScreenDestination
import com.ramcosta.composedestinations.generated.destinations.AppearanceSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.GeneralSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.StorageSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.TestLabScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import skywa1ker404.absencium.ui.components.NavCard
import skywa1ker404.absencium.ui.components.CardStack
import skywa1ker404.absencium.ui.components.GroupPosition
import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.components.SectionTitle

@Destination<RootGraph>
@Composable
fun SettingsHubScreen(navigator: DestinationsNavigator) {
    ScreenScaffold(title = "Settings", onBack = { navigator.navigateUp() }, largeTitle = true) {
        CardStack {
            NavCard(
                icon = AppIcons.School,
                title = "General",
                subtitle = null,
                onClick = { navigator.navigate(GeneralSettingsScreenDestination) },
                position = GroupPosition.First,
            )
            NavCard(
                icon = AppIcons.Palette,
                title = "Appearance",
                subtitle = null,
                onClick = { navigator.navigate(AppearanceSettingsScreenDestination) },
                position = GroupPosition.Middle,
            )
            NavCard(
                icon = AppIcons.Schedule,
                title = "Reminders",
                subtitle = "All off until you choose",
                onClick = { navigator.navigate(RemindersScreenDestination) },
                position = GroupPosition.Middle,
            )
            NavCard(
                icon = AppIcons.Storage,
                title = "Storage & Data",
                subtitle = null,
                onClick = { navigator.navigate(StorageSettingsScreenDestination) },
                position = GroupPosition.Middle,
            )
            NavCard(
                icon = AppIcons.AutoAwesome,
                title = "Absencium AI",
                subtitle = null,
                onClick = { navigator.navigate(AiSettingsScreenDestination) },
                position = GroupPosition.Last,
            )
        }

        SectionTitle("App")
        CardStack {
            NavCard(
                icon = AppIcons.PlayCircle,
                title = "Replay intro",
                subtitle = null,
                onClick = { navigator.navigate(IntroTourScreenDestination) },
                position = GroupPosition.First,
            )
            NavCard(
                icon = AppIcons.History,
                title = "Changelog",
                subtitle = null,
                onClick = { navigator.navigate(ChangelogScreenDestination) },
                position = GroupPosition.Middle,
            )
            NavCard(
                icon = AppIcons.Info,
                title = "About",
                subtitle = null,
                onClick = { navigator.navigate(AboutScreenDestination) },
                position = GroupPosition.Last,
            )
        }

        SectionTitle("Developer")
        CardStack {
            NavCard(
                icon = AppIcons.Science,
                title = "Test Lab",
                subtitle = null,
                onClick = { navigator.navigate(TestLabScreenDestination) },
                position = GroupPosition.Only,
            )
        }
    }
}
