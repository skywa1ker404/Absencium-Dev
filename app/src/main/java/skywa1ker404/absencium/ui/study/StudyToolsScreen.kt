package skywa1ker404.absencium.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AttendanceInsightsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ExportScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FocusTimerScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FlashcardsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.GradesScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PlannerScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import skywa1ker404.absencium.appContainer
import skywa1ker404.absencium.domain.overallPercent
import skywa1ker404.absencium.domain.subjectGrades
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.GroupPosition
import skywa1ker404.absencium.ui.components.NavCard
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.components.SectionTitle
import skywa1ker404.absencium.ui.icons.AppIcons
import kotlin.math.roundToInt

/** The home of the study helpers: quick timers on top, then grades, attendance insights and exports. */
@Destination<RootGraph>
@Composable
fun StudyToolsScreen(navigator: DestinationsNavigator) {
    val container = LocalContext.current.applicationContext.appContainer
    val data by container.calendarRepository.data.collectAsStateWithLifecycle(initialValue = null)
    val average = data?.subjectGrades()?.overallPercent()
    val colors = MaterialTheme.colorScheme
    ScreenScaffold(title = "Study Tools", onBack = { navigator.navigateUp() }, largeTitle = true) {
        Text("Choose a tool and make a little room to learn.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
        SectionTitle("Quick start")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile("Pomodoro", "25-minute focus rounds", colors.primaryContainer, colors.onPrimaryContainer, Modifier.weight(1f)) {
                    navigator.navigate(FocusTimerScreenDestination(mode = FocusMode.POMODORO))
                }
                QuickTile("Stopwatch", "Track open-ended study time", colors.secondaryContainer, colors.onSecondaryContainer, Modifier.weight(1f)) {
                    navigator.navigate(FocusTimerScreenDestination(mode = FocusMode.STOPWATCH))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile("10-minute break", "A reverse countdown", colors.tertiaryContainer, colors.onTertiaryContainer, Modifier.weight(1f)) {
                    navigator.navigate(FocusTimerScreenDestination(mode = FocusMode.BREAK_10))
                }
                QuickTile("15-minute break", "A longer reset timer", colors.surfaceContainerHighest, colors.onSurface, Modifier.weight(1f)) {
                    navigator.navigate(FocusTimerScreenDestination(mode = FocusMode.BREAK_15))
                }
            }
        }
        SectionTitle("Your study tools")
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            NavCard(
                icon = AppIcons.Check, title = "To-do & upcoming",
                subtitle = "Your list, exams, holidays and lectures ahead",
                onClick = { navigator.navigate(PlannerScreenDestination) }, position = GroupPosition.First,
            )
            NavCard(
                icon = AppIcons.Book, title = "Flashcards",
                subtitle = "Review what is due, or make cards with AI",
                onClick = { navigator.navigate(FlashcardsScreenDestination) }, position = GroupPosition.Middle,
            )
            NavCard(
                icon = AppIcons.School, title = "Grades & GPA",
                subtitle = average?.let { "Average ${it.roundToInt()}%" } ?: "Marks, weightage and pass marks",
                onClick = { navigator.navigate(GradesScreenDestination) }, position = GroupPosition.Middle,
            )
            NavCard(
                icon = AppIcons.CalendarMonth, title = "Attendance insights",
                subtitle = "Goals and how many lectures you can skip",
                onClick = { navigator.navigate(AttendanceInsightsScreenDestination) }, position = GroupPosition.Middle,
            )
            NavCard(
                icon = AppIcons.OpenInNew, title = "Export & share",
                subtitle = "Calendar (.ics), CSV and a PDF report",
                onClick = { navigator.navigate(ExportScreenDestination) }, position = GroupPosition.Last,
            )
        }
    }
}

@Composable
private fun QuickTile(title: String, subtitle: String, container: Color, content: Color, modifier: Modifier, onClick: () -> Unit) {
    AppCard(modifier = modifier, onClick = onClick, container = container) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = content)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = content.copy(alpha = 0.8f), modifier = Modifier.padding(top = 4.dp))
    }
}
