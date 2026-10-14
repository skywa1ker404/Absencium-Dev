package com.absencium.app.ui.testlab

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.absencium.app.logging.DebugLogger
import com.absencium.app.ui.TestLabViewModel
import com.absencium.app.ui.components.AppCard
import com.absencium.app.ui.components.HintText
import com.absencium.app.ui.components.MessageCard
import com.absencium.app.ui.components.PillButton
import com.absencium.app.ui.components.ScreenScaffold
import com.absencium.app.ui.components.SectionTitle
import com.absencium.app.ui.containerViewModel
import com.absencium.app.ui.label
import com.absencium.app.ui.settings.rememberSettingsViewModel
import com.absencium.app.ui.theme.effectiveColorEngine
import com.absencium.app.ui.theme.isMonetAndBlurSupported

/** Checks the parts built so far (database, storage, debug log) on the device the app is running on. */
@Destination<RootGraph>
@Composable
fun TestLabScreen(navigator: DestinationsNavigator) {
    val viewModel = containerViewModel { TestLabViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settingsViewModel = rememberSettingsViewModel()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    ScreenScaffold(title = "Test Lab", onBack = { navigator.navigateUp() }) {
        HintText("Each check below runs for real on this device and writes to debug.log.")

        SectionTitle("This device")
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Text("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})", style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (isMonetAndBlurSupported) {
                    "Monet and Liquid Glass: available"
                } else {
                    "Monet and Liquid Glass: not available (needs Android 12)"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Color engine in use: ${effectiveColorEngine(settings.colorEngine).label()}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        SectionTitle("Database: Absencium.db")
        AppCard(modifier = Modifier.fillMaxWidth()) {
            if (state.tableCounts.isEmpty()) {
                Text("Reading tables...", style = MaterialTheme.typography.bodyMedium)
            }
            state.tableCounts.forEach { (name, count) ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(name, style = MaterialTheme.typography.bodyMedium)
                    Text(count.toString(), style = MaterialTheme.typography.titleSmall)
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                PillButton(text = "Add samples", onClick = { viewModel.addSampleSubjects() })
                PillButton(text = "Remove samples", onClick = { viewModel.removeSampleData() }, tonal = true)
            }
        }
        val databaseMessage = state.databaseMessage
        if (databaseMessage != null) MessageCard(databaseMessage)

        SectionTitle("Notes storage")
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Writes a small file into your notes folder, reads it back and lists the folder.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PillButton(
                text = "Run storage test",
                onClick = { viewModel.runStorageTest() },
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        val storageMessage = state.storageMessage
        if (storageMessage != null) MessageCard(storageMessage)

        SectionTitle("Debug log")
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = DebugLogger.logFilePath ?: "Logger not started",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val scroll = rememberScrollState()
            LaunchedEffect(state.logTail) { scroll.scrollTo(scroll.maxValue) }
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .verticalScroll(scroll)
                    .padding(12.dp),
            ) {
                Text(
                    text = state.logTail.ifBlank { "(log is empty)" },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                PillButton(text = "Refresh", onClick = { viewModel.refresh() }, tonal = true)
                PillButton(text = "Log a line", onClick = { viewModel.logTestLine() }, tonal = true)
                PillButton(
                    text = "Copy",
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("debug.log", DebugLogger.readTail(2000)))
                        copied = true
                    },
                    tonal = true,
                )
            }
            if (copied) {
                HintText("Copied the last 2000 lines to the clipboard.", modifier = Modifier.padding(top = 8.dp))
            }
        }

        SectionTitle("Crash logging")
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Closes the app on purpose with a test crash. After reopening, the Test Lab log should show the stack trace.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PillButton(
                text = "Trigger test crash",
                onClick = { throw RuntimeException("Test crash triggered from the Test Lab") },
                tonal = true,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
