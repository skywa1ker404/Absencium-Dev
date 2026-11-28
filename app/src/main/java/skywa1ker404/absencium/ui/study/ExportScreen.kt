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
    // Events found in a chosen calendar file that are not already in the student's own events, waiting to be added.
    var importing by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<List<skywa1ker404.absencium.data.db.EventEntity>>(emptyList()) }
    var foundCount by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        importing = true
        message = null
        scope.launch {
            val text = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        // Reads at most the limit plus one byte, so a huge file is refused without being loaded whole.
                        val limit = skywa1ker404.absencium.data.export.IcsImport.MAX_BYTES
                        val out = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(16 * 1024)
                        while (out.size() <= limit) {
                            val n = input.read(buffer, 0, minOf(buffer.size, limit + 1 - out.size()))
                            if (n < 0) break
                            out.write(buffer, 0, n)
                        }
                        if (out.size() > limit) null else out.toString(Charsets.UTF_8.name())
                    }
                }.getOrNull()
            }
            if (text == null) {
                message = "That file could not be read, or it is larger than 2 MB."
            } else {
                val found = skywa1ker404.absencium.data.export.IcsImport.parse(text)
                foundCount = found.size
                pending = skywa1ker404.absencium.data.export.IcsImport.newEvents(found, events)
                message = if (found.isEmpty()) "No events were found in that file." else null
            }
            importing = false
        }
    }

    ScreenScaffold(title = "Export & share", onBack = { navigator.navigateUp() }, largeTitle = true) {
        StudyHero(
            label = "Take it with you", figure = "4 formats", caption = "Calendar, attendance, marks and a PDF report",
            icon = skywa1ker404.absencium.ui.icons.AppIcons.OpenInNew,
            hue = skywa1ker404.absencium.ui.components.hueFor(4), shape = BadgeShape.BURST,
        )
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
        AppCard(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Import a calendar file", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Reads an .ics file on this phone and adds its events to your own events. Events you already have are skipped, and nothing is added until you confirm.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (foundCount > 0) {
                    Text(
                        "${pending.size} new of $foundCount found" + if (pending.size < foundCount) " (${foundCount - pending.size} already there)" else "",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    pending.take(8).forEach { e ->
                        Text(
                            "${e.date}" + (e.startTime?.let { " $it" } ?: "") + " · " + e.title,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                    }
                    if (pending.size > 8) HintText("and ${pending.size - 8} more")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillButton(
                            text = if (pending.isEmpty()) "Nothing new" else "Add ${pending.size}",
                            enabled = pending.isNotEmpty(),
                            compact = true,
                            onClick = {
                                val toAdd = pending
                                scope.launch {
                                    toAdd.forEach { container.database.eventDao().insert(it) }
                                    message = "Added ${toAdd.size} event${if (toAdd.size == 1) "" else "s"} to your own events."
                                    pending = emptyList()
                                    foundCount = 0
                                }
                            },
                        )
                        PillButton(text = "Cancel", tonal = true, compact = true, onClick = { pending = emptyList(); foundCount = 0 })
                    }
                } else {
                    PillButton(
                        text = "Choose a file", tonal = true, compact = true, loading = importing, enabled = !importing,
                        onClick = { picker.launch(arrayOf("text/calendar", "text/plain", "application/octet-stream", "*/*")) },
                    )
                }
            }
        }
        message?.let { HintText(it, modifier = Modifier.padding(top = 8.dp)) }
        HintText("Files are made in this phone's temporary storage and removed after an hour.", modifier = Modifier.padding(top = 8.dp))
    }
}
