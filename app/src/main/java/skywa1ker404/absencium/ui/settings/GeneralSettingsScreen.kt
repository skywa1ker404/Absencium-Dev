package skywa1ker404.absencium.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.SubjectsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import skywa1ker404.absencium.data.settings.AppSettings
import skywa1ker404.absencium.ui.components.NavCard
import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.components.SectionTitle
import skywa1ker404.absencium.ui.components.SliderCard
import skywa1ker404.absencium.ui.components.SwitchCard

@Destination<RootGraph>
@Composable
fun GeneralSettingsScreen(navigator: DestinationsNavigator) {
    val viewModel = rememberSettingsViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    ScreenScaffold(title = "General", onBack = { navigator.navigateUp() }, largeTitle = true) {
        SectionTitle("Attendance")
        SliderCard(
            title = "Attendance threshold",
            subtitle = "Warn me below this",
            value = settings.attendanceThresholdPercent,
            range = AppSettings.MIN_ATTENDANCE_THRESHOLD..AppSettings.MAX_ATTENDANCE_THRESHOLD,
            valueLabel = { "$it%" },
            onValueChangeFinished = { viewModel.setAttendanceThreshold(it) },
        )

        SectionTitle("Weekends")
        SwitchCard(
            title = "Sunday is a holiday",
            subtitle = null,
            checked = settings.sundayHoliday,
            onCheckedChange = { viewModel.setSundayHoliday(it) },
        )
        SwitchCard(
            title = "Saturday is a holiday",
            subtitle = null,
            checked = settings.saturdayHoliday,
            onCheckedChange = { viewModel.setSaturdayHoliday(it) },
        )

        SectionTitle("Focus")
        SliderCard(
            title = "Focus Guard grace timer",
            subtitle = "Time away before Focus Guard steps in",
            value = settings.focusGraceMinutes,
            range = 1..30,
            valueLabel = { "$it min" },
            onValueChangeFinished = { viewModel.setFocusGraceMinutes(it) },
        )
        SwitchCard(
            title = "Exam Mode auto-silence",
            subtitle = "Silence notifications during exams",
            checked = settings.examModeAutoSilence,
            onCheckedChange = { viewModel.setExamModeAutoSilence(it) },
        )

        SectionTitle("Subjects")
        NavCard(
            icon = AppIcons.Book,
            title = "Subjects",
            subtitle = null,
            onClick = { navigator.navigate(SubjectsScreenDestination) },
        )
    }
}
