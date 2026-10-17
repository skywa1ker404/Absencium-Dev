package skywa1ker404.absencium.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * The date chooser, built on the app's popup card so it is frosted glass whenever "Apply frosted glass on other elements"
 * is on and a solid card otherwise (a platform dialog could do neither). Place it through [AnimatedPopup].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerPopup(
    initial: LocalDate,
    onPicked: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Pick a date",
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    PopupShell(onDismissRequest = onDismiss, dismissOnOutsideTap = true) {
        PopupBody(title = title, onClose = onDismiss) {
            DatePicker(
                state = state,
                title = null,
                showModeToggle = false,
                colors = DatePickerDefaults.colors(containerColor = Color.Transparent),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PillButton(text = "Cancel", onClick = onDismiss, tonal = true, modifier = Modifier.weight(1f))
                PillButton(
                    text = "OK",
                    onClick = {
                        state.selectedDateMillis?.let { onPicked(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                        onDismiss()
                    },
                    enabled = state.selectedDateMillis != null,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
