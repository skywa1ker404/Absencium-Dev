package com.trajectory.vault.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.SubjectsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.trajectory.vault.data.settings.AppSettings
import com.trajectory.vault.ui.components.NavCard
import com.trajectory.vault.ui.components.ScreenScaffold
import com.trajectory.vault.ui.components.SectionTitle
import com.trajectory.vault.ui.components.SliderCard
import com.trajectory.vault.ui.components.SwitchCard

@Destination<RootGraph>
@Composable
fun GeneralSettingsScreen(navigator: DestinationsNavigator) {
    val viewModel = rememberSettingsViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    ScreenScaffold(title = "General", onBack = { navigator.navigateUp() }) {
        SectionTitle("Attendance")
        SliderCard(
            title = "Attendance threshold",
            subtitle = "Trajectory warns you when a subject falls below this percentage.",
            value = settings.attendanceThresholdPercent,
            range = AppSettings.MIN_ATTENDANCE_THRESHOLD..AppSettings.MAX_ATTENDANCE_THRESHOLD,
            valueLabel = { "$it%" },
            onValueChangeFinished = { viewModel.setAttendanceThreshold(it) },
        )

        SectionTitle("Focus")
        SliderCard(
            title = "Focus Guard grace timer",
            subtitle = "How long you can step away before Focus Guard steps in.",
            value = settings.focusGraceMinutes,
            range = 1..30,
            valueLabel = { "$it min" },
            onValueChangeFinished = { viewModel.setFocusGraceMinutes(it) },
        )
        SwitchCard(
            title = "Exam Mode auto-silence",
            subtitle = "Silence notifications automatically while an exam is on. " +
                "Your choice is saved now and used once exams are added to the calendar.",
            checked = settings.examModeAutoSilence,
            onCheckedChange = { viewModel.setExamModeAutoSilence(it) },
        )

        SectionTitle("Subjects")
        NavCard(
            emoji = "📚",
            title = "Subjects",
            subtitle = "Add subjects, mute them, or mark them active or inactive",
            onClick = { navigator.navigate(SubjectsScreenDestination) },
        )
    }
}
