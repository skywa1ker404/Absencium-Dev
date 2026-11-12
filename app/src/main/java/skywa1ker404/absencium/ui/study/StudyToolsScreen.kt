package skywa1ker404.absencium.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AttendanceInsightsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ExportScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FlashcardsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FocusTimerScreenDestination
import com.ramcosta.composedestinations.generated.destinations.GradesScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PlannerScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import skywa1ker404.absencium.appContainer
import skywa1ker404.absencium.domain.overallPercent
import skywa1ker404.absencium.domain.subjectGrades
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.components.hueFor
import skywa1ker404.absencium.ui.icons.AppIcons
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/** The home of the study helpers: a summary of today, quick timers, and the study tools. */
@Destination<RootGraph>
@Composable
fun StudyToolsScreen(navigator: DestinationsNavigator) {
    val container = LocalContext.current.applicationContext.appContainer
    val data by container.calendarRepository.data.collectAsStateWithLifecycle(initialValue = null)
    val cards by container.database.flashcardDao().observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val goals by container.database.studyGoalDao().observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val colors = MaterialTheme.colorScheme
    val today = LocalDate.now()
    val average = data?.subjectGrades()?.overallPercent()
    val due = cards.count { !it.dueDate.isAfter(today) }
    val open = goals.count { !it.done }
    val nextExam = data?.exams?.filter { it.deletedAt == null && !it.date.isBefore(today) }?.minByOrNull { it.date }
    val examDays = nextExam?.let { ChronoUnit.DAYS.between(today, it.date).toInt() }
    val examName = nextExam?.let { e -> data?.subjects?.firstOrNull { it.id == e.subjectId }?.name }

    ScreenScaffold(title = "Study", onBack = { navigator.navigateUp() }, largeTitle = true) {
        Text("Your focus space", style = MaterialTheme.typography.labelLarge, color = colors.primary, modifier = Modifier.padding(horizontal = 4.dp))
        Text("Choose a tool and make a little room to learn.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))

        StudyHero(
            label = if (examDays != null) "Next exam" else "Today",
            figure = when {
                examDays == null -> if (due + open == 0) "All clear" else "${due + open} to do"
                examDays == 0 -> "Today"
                examDays == 1 -> "Tomorrow"
                else -> "In $examDays days"
            },
            caption = listOfNotNull(
                examName?.takeIf { examDays != null },
                "$due cards due".takeIf { due > 0 },
                "$open to-dos".takeIf { open > 0 },
            ).joinToString(" · ").ifBlank { "Nothing waiting for you" },
            icon = AppIcons.School, hue = hueFor(0), shape = BadgeShape.SUNNY,
            modifier = Modifier.padding(top = 8.dp),
        )

        Text("Quick start", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, top = 12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PastelTile("Pomodoro", "Open a 25-minute focus timer", hueFor(0), BadgeShape.COOKIE, AppIcons.Schedule, Modifier.weight(1f), 0) {
                    navigator.navigate(FocusTimerScreenDestination(mode = FocusMode.POMODORO))
                }
                PastelTile("Stopwatch", "Track open-ended study time", hueFor(1), BadgeShape.CLOVER, AppIcons.PlayCircle, Modifier.weight(1f), 60) {
                    navigator.navigate(FocusTimerScreenDestination(mode = FocusMode.STOPWATCH))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PastelTile("10-minute break", "Open a reverse countdown", hueFor(2), BadgeShape.FLOWER, AppIcons.Refresh, Modifier.weight(1f), 120) {
                    navigator.navigate(FocusTimerScreenDestination(mode = FocusMode.BREAK_10))
                }
                PastelTile("15-minute break", "Open a longer reset timer", hueFor(3), BadgeShape.BURST, AppIcons.Opacity, Modifier.weight(1f), 180) {
                    navigator.navigate(FocusTimerScreenDestination(mode = FocusMode.BREAK_15))
                }
            }
        }

        Text("Your study tools", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, top = 12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ToolCard("Calendar & to-do", "Your day, your list and your own events.", AppIcons.CalendarMonth, hueFor(0), BadgeShape.COOKIE9, delayMs = 0,
                meta = if (open > 0) "$open open" else null) { navigator.navigate(PlannerScreenDestination) }
            ToolCard("Flashcards", "Review decks and keep difficult cards close.", AppIcons.Book, hueFor(1), BadgeShape.CLOVER, delayMs = 60,
                meta = if (due > 0) "$due due today" else null) { navigator.navigate(FlashcardsScreenDestination) }
            ToolCard("Grades & GPA", "Marks, pass marks and your average.", AppIcons.School, hueFor(2), BadgeShape.FLOWER, delayMs = 120,
                meta = average?.let { "Average ${it.roundToInt()}%" }) { navigator.navigate(GradesScreenDestination) }
            ToolCard("Attendance insights", "Goals and how many lectures you can skip.", AppIcons.Check, hueFor(3), BadgeShape.SUNNY, delayMs = 180) {
                navigator.navigate(AttendanceInsightsScreenDestination)
            }
            ToolCard("Export & share", "Calendar, CSV files and a PDF report.", AppIcons.OpenInNew, hueFor(4), BadgeShape.BURST, delayMs = 240) {
                navigator.navigate(ExportScreenDestination)
            }
        }
    }
}
