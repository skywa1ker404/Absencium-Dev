package skywa1ker404.absencium.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.components.PopupBody
import skywa1ker404.absencium.ui.components.PopupShell
import skywa1ker404.absencium.ui.icons.AppIcons
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** What the calendar asks the screen to show: the month on display and what to do with the month the user chooses. */
class MonthPickRequest(val current: YearMonth, val onPick: (YearMonth) -> Unit)

/** A centred popup to jump to any month and year: arrows change the year, a tap on a month chooses it. */
@Composable
fun MonthYearPopup(request: MonthPickRequest, onDismiss: () -> Unit) {
    var year by rememberSaveable { mutableIntStateOf(request.current.year) }
    PopupShell(onDismissRequest = onDismiss, dismissOnOutsideTap = true, centered = true) {
        PopupBody(title = "Go to month", onClose = onDismiss) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = { if (year > MIN_YEAR) year-- }) { Icon(AppIcons.ChevronLeft, contentDescription = "Previous year") }
                Text(text = year.toString(), style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { if (year < MAX_YEAR) year++ }) { Icon(AppIcons.ChevronRight, contentDescription = "Next year") }
            }
            for (row in 0 until 4) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (column in 0 until 3) {
                        val month = Month.of(row * 3 + column + 1)
                        val chosen = YearMonth.of(year, month)
                        PillButton(
                            text = month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                            onClick = {
                                request.onPick(chosen)
                                onDismiss()
                            },
                            tonal = chosen != request.current,
                            compact = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

private const val MIN_YEAR = 1900
private const val MAX_YEAR = 2200
