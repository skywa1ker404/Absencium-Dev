package skywa1ker404.absencium.ui.profile

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
import skywa1ker404.absencium.ui.components.AppTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import skywa1ker404.absencium.ui.components.OverlayStage
import skywa1ker404.absencium.ui.components.PopupShell
import skywa1ker404.absencium.ui.components.sheetContainerColor
import skywa1ker404.absencium.ui.components.sheetScrimColor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import skywa1ker404.absencium.domain.overall
import androidx.compose.ui.unit.dp
import skywa1ker404.absencium.data.calendar.CalendarData
import skywa1ker404.absencium.data.settings.AppSettings
import skywa1ker404.absencium.domain.attendanceMargin
import skywa1ker404.absencium.domain.subjectAttendance
import skywa1ker404.absencium.ui.calendar.subjectColor
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.NavCard
import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.components.SectionTitle
import skywa1ker404.absencium.ui.main.initialsOf
import kotlin.math.roundToInt

/** The profile hub: avatar and name, the Settings entry, and the attendance log of each subject. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSheet(
    data: CalendarData?,
    settings: AppSettings,
    onDismiss: () -> Unit,
    onSaveName: (String) -> Unit,
    onOpenAttendance: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAiHub: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var name by remember(settings.displayName) { mutableStateOf(settings.displayName) }
    val threshold = settings.attendanceThresholdPercent

    PopupShell(onDismissRequest = onDismiss, scrollable = false, dismissOnOutsideTap = true) {
        OverlayStage {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // A ring around the avatar shows the overall attendance, with the number on a small badge.
                val overallPercent = remember(data) { data?.subjectAttendance()?.overall()?.percent }
                val ringTrack = colors.surfaceContainerHighest
                val ringColor = if (overallPercent != null && overallPercent < threshold) colors.error else colors.primary
                Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.size(92.dp)) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .drawBehind {
                            val stroke = 5.dp.toPx()
                            val inset = stroke / 2f
                            val arc = Size(size.width - stroke, size.height - stroke)
                            drawArc(ringTrack, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
                            if (overallPercent != null) {
                                drawArc(
                                    ringColor,
                                    -90f,
                                    360f * (overallPercent / 100.0).toFloat().coerceIn(0f, 1f),
                                    false,
                                    Offset(inset, inset),
                                    arc,
                                    style = Stroke(stroke, cap = StrokeCap.Round),
                                )
                            }
                        }
                        .padding(9.dp)
                        .clip(CircleShape)
                        .background(colors.primaryContainer),
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
                if (overallPercent != null) {
                    Text(
                        text = "${overallPercent.roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onPrimary,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(ringColor)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
                }
                AppTextField(
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

            NavCard(
                icon = AppIcons.AutoAwesome,
                title = "AI Hub",
                subtitle = if (settings.askAiEnabled) "Every AI request and answer, kept on this phone" else "AI Features are off. Turn them on in Settings, or ignore this.",
                onClick = onOpenAiHub,
            )

            SectionTitle("Attendance logs")
            val rows = data?.subjectAttendance().orEmpty()
            if (rows.isEmpty()) {
                HintText("Add subjects and log lectures to see attendance here.")
            }
            rows.forEach { item ->
                AppCard(modifier = Modifier.fillMaxWidth(), onClick = onOpenAttendance) {
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
}
