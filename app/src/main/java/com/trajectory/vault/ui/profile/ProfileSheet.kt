package com.trajectory.vault.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.trajectory.vault.data.calendar.CalendarData
import com.trajectory.vault.data.settings.AppSettings
import com.trajectory.vault.domain.attendanceMargin
import com.trajectory.vault.domain.subjectAttendance
import com.trajectory.vault.ui.calendar.subjectColor
import com.trajectory.vault.ui.components.AppCard
import com.trajectory.vault.ui.components.HintText
import com.trajectory.vault.ui.components.NavCard
import com.trajectory.vault.ui.icons.AppIcons
import com.trajectory.vault.ui.components.PillButton
import com.trajectory.vault.ui.components.SectionTitle
import com.trajectory.vault.ui.main.initialsOf
import kotlin.math.roundToInt

/** The profile hub: avatar and name, the Settings entry, and the attendance log of each subject. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSheet(
    data: CalendarData?,
    settings: AppSettings,
    onDismiss: () -> Unit,
    onSaveName: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var name by remember(settings.displayName) { mutableStateOf(settings.displayName) }
    val threshold = settings.attendanceThresholdPercent

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier.size(72.dp).clip(CircleShape).background(colors.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    val initials = initialsOf(name)
                    if (initials == null) {
                        Icon(
                            AppIcons.Person,
                            contentDescription = null,
                            tint = colors.onPrimaryContainer,
                            modifier = Modifier.size(36.dp),
                        )
                    } else {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.headlineMedium,
                            color = colors.onPrimaryContainer,
                        )
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Your name") },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f),
                )
            }
            if (name.trim() != settings.displayName.trim()) {
                PillButton(text = "Save name", onClick = { onSaveName(name.trim()) })
            }

            NavCard(
                icon = AppIcons.Tune,
                title = "Settings",
                subtitle = "General, appearance, storage and AI",
                onClick = onOpenSettings,
            )

            SectionTitle("Attendance logs")
            val rows = data?.subjectAttendance().orEmpty()
            if (rows.isEmpty()) {
                HintText("Add subjects and log lectures to see attendance here.")
            }
            rows.forEach { item ->
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(subjectColor(item.subject.id)))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.subject.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${item.present} present · ${item.absent} absent · ${item.cancelled} cancelled",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                        val percent = item.percent
                        if (percent != null) {
                            val margin = attendanceMargin(item.present, item.held, threshold)
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${percent.roundToInt()}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (percent < threshold) colors.error else colors.primary,
                                )
                                Text(
                                    text = if (margin.mustAttend > 0) "attend ${margin.mustAttend}" else "can skip ${margin.canSkip}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
