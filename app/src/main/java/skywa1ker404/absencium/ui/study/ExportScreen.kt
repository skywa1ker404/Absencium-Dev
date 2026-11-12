package skywa1ker404.absencium.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import skywa1ker404.absencium.appContainer
import skywa1ker404.absencium.data.export.StudyExportKind
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.components.SettingDivider
import skywa1ker404.absencium.ui.settings.rememberSettingsViewModel

private fun describe(kind: StudyExportKind) = when (kind) {
    StudyExportKind.CALENDAR_ICS -> "Weekly timetable, exams, holidays and your own events for any calendar app"
    StudyExportKind.ATTENDANCE_CSV -> "Every logged lecture, one row each"
    StudyExportKind.MARKS_CSV -> "Exams with marks, percent and weightage"
    StudyExportKind.REPORT_PDF -> "Attendance, marks and upcoming exams on printable pages"
}

/** Builds a file on this phone and opens Android's Sharesheet. Nothing is sent anywhere by Absencium itself. */
@Destination<RootGraph>
@Composable
fun ExportScreen(navigator: DestinationsNavigator) {
    val container = LocalContext.current.applicationContext.appContainer
    val settings by rememberSettingsViewModel().settings.collectAsStateWithLifecycle()
    val data by container.calendarRepository.data.collectAsStateWithLifecycle(initialValue = null)
    val events by container.database.eventDao().observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf<StudyExportKind?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    ScreenScaffold(title = "Export & share", onBack = { navigator.navigateUp() }, largeTitle = true) {
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                StudyExportKind.entries.forEachIndexed { index, kind ->
                    if (index > 0) SettingDivider()
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(kind.label, style = MaterialTheme.typography.titleSmall)
                        Text(describe(kind), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        PillButton(
                            text = "Share", tonal = true, compact = true,
                            loading = busy == kind, enabled = data != null && busy == null,
                            onClick = {
                                val snapshot = data ?: return@PillButton
                                busy = kind
                                message = null
                                scope.launch {
                                    container.studyExport.prepare(kind, snapshot, settings.attendanceThresholdPercent, events)
                                        .onSuccess { message = container.shareService.share(it) }
                                        .onFailure { message = it.message ?: "The export failed." }
                                    busy = null
                                }
                            },
                        )
                    }
                }
            }
        }
        message?.let { HintText(it, modifier = Modifier.padding(top = 8.dp)) }
        HintText("Files are made in this phone's temporary storage and removed after an hour.", modifier = Modifier.padding(top = 8.dp))
    }
}
