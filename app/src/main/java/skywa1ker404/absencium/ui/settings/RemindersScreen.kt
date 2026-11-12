package skywa1ker404.absencium.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import skywa1ker404.absencium.notify.ReminderScheduler
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.ExpressiveChip
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.components.SectionTitle

/** Reminders for lectures, exams and low attendance. Everything is off until the student picks an option. */
@Destination<RootGraph>
@Composable
fun RemindersScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val viewModel = rememberSettingsViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var denied by remember { mutableStateOf(false) }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> denied = !granted }

    // Turning any reminder on asks for the notification permission first on Android 13 and newer.
    fun choose(apply: () -> Unit, turnsOn: Boolean) {
        apply()
        if (turnsOn && Build.VERSION.SDK_INT >= 33 && !ReminderScheduler.canNotify(context)) askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    ScreenScaffold(title = "Reminders", onBack = { navigator.navigateUp() }, largeTitle = true) {
        SectionTitle("Before a lecture")
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0 to "Off", 5 to "5 min", 10 to "10 min", 15 to "15 min", 30 to "30 min", 60 to "1 hour").forEach { (minutes, label) ->
                ExpressiveChip(
                    selected = settings.remindLectureMinutes == minutes,
                    onClick = { choose({ viewModel.setRemindLectureMinutes(minutes) }, minutes > 0) },
                    label = { Text(label) },
                )
            }
        }
        SectionTitle("Before an exam (at 8:00)")
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0 to "Off", 1 to "1 day", 2 to "2 days", 3 to "3 days", 7 to "1 week").forEach { (days, label) ->
                ExpressiveChip(
                    selected = settings.remindExamDays == days,
                    onClick = { choose({ viewModel.setRemindExamDays(days) }, days > 0) },
                    label = { Text(label) },
                )
            }
        }
        SectionTitle("Attendance (at 18:00)")
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExpressiveChip(selected = !settings.remindLowAttendance, onClick = { viewModel.setRemindLowAttendance(false) }, label = { Text("Off") })
            ExpressiveChip(selected = settings.remindLowAttendance, onClick = { choose({ viewModel.setRemindLowAttendance(true) }, true) }, label = { Text("Alert when under my goal") })
        }
        if (denied) {
            AppCard(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), container = MaterialTheme.colorScheme.errorContainer) {
                Text("Notifications are blocked for Absencium. Allow them in the phone's app settings to get reminders.", color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
        HintText(
            "Reminders use one low-battery alarm and may arrive a few minutes late on some phones. They are set again after a restart, a time change or an update. Nothing is sent anywhere.",
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
