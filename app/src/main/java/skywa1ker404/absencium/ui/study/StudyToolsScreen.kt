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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AttendanceInsightsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FocusTimerScreenDestination
import com.ramcosta.composedestinations.generated.destinations.GradesScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import skywa1ker404.absencium.appContainer
import skywa1ker404.absencium.domain.overallPercent
import skywa1ker404.absencium.domain.subjectGrades
import skywa1ker404.absencium.ui.components.GroupPosition
import skywa1ker404.absencium.ui.components.NavCard
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.icons.AppIcons
import kotlin.math.roundToInt

/** The home of the study helpers: grades, attendance insights and the focus timer. */
@Destination<RootGraph>
@Composable
fun StudyToolsScreen(navigator: DestinationsNavigator) {
    val container = LocalContext.current.applicationContext.appContainer
    val data by container.calendarRepository.data.collectAsStateWithLifecycle(initialValue = null)
    val average = data?.subjectGrades()?.overallPercent()
    ScreenScaffold(title = "Study Tools", onBack = { navigator.navigateUp() }, largeTitle = true) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            NavCard(
                icon = AppIcons.School, title = "Grades & GPA",
                subtitle = average?.let { "Average ${it.roundToInt()}%" } ?: "Marks, weightage and pass marks",
                onClick = { navigator.navigate(GradesScreenDestination) }, position = GroupPosition.First,
            )
            NavCard(
                icon = AppIcons.CalendarMonth, title = "Attendance insights",
                subtitle = "Goals, how many lectures you can skip",
                onClick = { navigator.navigate(AttendanceInsightsScreenDestination) }, position = GroupPosition.Middle,
            )
            NavCard(
                icon = AppIcons.Schedule, title = "Focus timer",
                subtitle = "Pomodoro with study and break rounds",
                onClick = { navigator.navigate(FocusTimerScreenDestination) }, position = GroupPosition.Last,
            )
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text(
                "Study planner and flashcards are next.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}
