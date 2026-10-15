package skywa1ker404.absencium.ui.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.components.SectionTitle

/** One release in the changelog: [groups] maps a heading such as "Added" to its lines. */
private data class Release(val version: String, val date: String, val groups: List<Pair<String, List<String>>>)

/** Newest first. Only things that exist in the app belong here. */
private val Releases = listOf(
    Release(
        version = "0.1.0",
        date = "October 2026",
        groups = listOf(
            "Added" to listOf(
                "Visual Quality: Low, Mid, High and Ultra, chosen for your phone at first launch and changeable any time.",
                "Liquid Glass built on the Backdrop library: bars that blur and bend what scrolls behind them.",
                "Edit saved exams, holidays, lectures and subject names.",
                "Holidays across several days in one go, and a same-time-every-day switch for the timetable.",
                "Saturday and Sunday holiday switches in Settings and setup, on by default.",
                "Notes collected tracking for days you were absent, from the attendance card.",
                "AI model picker that lists the models of the provider you chose and shows which are free where the provider says so.",
                "About page, this changelog and a replayable intro.",
            ),
            "Improved" to listOf(
                "Navigation pill: dragging across it switches the page live, before you lift your finger.",
                "Smoother scrolling on the Home and Attendance tabs.",
                "Subjects with entries can now be deleted, after two confirmations.",
            ),
        ),
    ),
)

@Destination<RootGraph>
@Composable
fun ChangelogScreen(navigator: DestinationsNavigator) {
    ScreenScaffold(title = "Changelog", onBack = { navigator.navigateUp() }) {
        Releases.forEach { release ->
            SectionTitle("${release.version} · ${release.date}")
            AppCard(modifier = Modifier.fillMaxWidth()) {
                release.groups.forEachIndexed { index, (heading, lines) ->
                    Text(
                        text = heading,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = if (index == 0) 0.dp else 12.dp, bottom = 4.dp),
                    )
                    Column {
                        lines.forEach { line ->
                            Text(
                                text = "•  $line",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
