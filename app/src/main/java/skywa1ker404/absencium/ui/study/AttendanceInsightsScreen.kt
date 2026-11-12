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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import skywa1ker404.absencium.appContainer
import skywa1ker404.absencium.domain.attendanceMargin
import skywa1ker404.absencium.domain.subjectAttendance
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.components.SectionTitle
import skywa1ker404.absencium.ui.settings.rememberSettingsViewModel
import java.util.Locale

/** Per subject: where attendance stands against its goal and what missing a few more lectures would do. */
@Destination<RootGraph>
@Composable
fun AttendanceInsightsScreen(navigator: DestinationsNavigator) {
    val container = LocalContext.current.applicationContext.appContainer
    val settingsViewModel = rememberSettingsViewModel()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val data by container.calendarRepository.data.collectAsStateWithLifecycle(initialValue = null)
    val rows = remember(data) { data?.subjectAttendance().orEmpty() }
    val colors = MaterialTheme.colorScheme
    ScreenScaffold(title = "Attendance insights", onBack = { navigator.navigateUp() }, largeTitle = true) {
        val held = rows.sumOf { it.held }
        val overall = if (held > 0) rows.sumOf { it.present } * 100.0 / held else null
        StudyHero(
            label = "Overall attendance",
            figure = overall?.let { String.format(Locale.US, "%.1f%%", it) } ?: "–",
            caption = if (overall == null) "Log some lectures to see insights" else "Goal ${settings.attendanceThresholdPercent}% · $held lectures held",
            icon = skywa1ker404.absencium.ui.icons.AppIcons.CalendarMonth,
            hue = skywa1ker404.absencium.ui.components.hueFor(3), shape = BadgeShape.SUNNY,
        )
        if (rows.isEmpty()) HintText("Log some lectures to see insights.")
        rows.forEach { item ->
            val goal = item.subject.attendanceGoal ?: settings.attendanceThresholdPercent
            val margin = attendanceMargin(item.present, item.held, goal)
            var skip by remember(item.subject.id) { mutableIntStateOf(1) }
            SectionTitle(item.subject.name)
            AppCard(modifier = Modifier.fillMaxWidth()) {
                val p = item.percent
                Text(
                    text = p?.let { String.format(Locale.US, "%.1f%%", it) } ?: "No lectures yet",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (p != null && p < goal) colors.error else colors.primary,
                )
                Text(
                    text = "Goal $goal% · ${item.present} present, ${item.absent} absent, ${item.cancelled} cancelled",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
                if (p != null) CapsuleProgress((p / 100.0).toFloat(), if (p < goal) colors.error else colors.primary, Modifier.padding(top = 10.dp))
                if (item.held > 0) {
                    Text(
                        text = if (margin.mustAttend > 0) "Attend the next ${margin.mustAttend} lectures to reach the goal"
                        else "You can skip ${margin.canSkip} lectures and stay at the goal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (margin.mustAttend > 0) colors.error else colors.onSurface,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillButton(text = "−", onClick = { if (skip > 1) skip-- }, tonal = true, compact = true)
                        Text("Miss $skip more", style = MaterialTheme.typography.titleSmall)
                        PillButton(text = "+", onClick = { if (skip < 30) skip++ }, tonal = true, compact = true)
                    }
                    Text(
                        text = "Attendance would be " + String.format(Locale.US, "%.1f%%", item.present * 100.0 / (item.held + skip)),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
        HintText("Excused and cancelled lectures are not counted as held.", modifier = Modifier.padding(top = 8.dp))
    }
}
