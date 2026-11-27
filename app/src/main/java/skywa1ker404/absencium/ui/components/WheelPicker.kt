package skywa1ker404.absencium.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlinx.coroutines.flow.filter
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.clip
import skywa1ker404.absencium.ui.theme.pillShape
import androidx.compose.foundation.background
import java.time.LocalTime

private val ItemHeight = 40.dp

/**
 * One wheel of values 0 until [count]: it scrolls with the finger, settles with a spring on the centred value and reports it through
 * [onValueChange]. With [infinite] the values loop round (59 to 0), as the hour and minute wheels do.
 */
@Composable
fun WheelPicker(
    count: Int,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    infinite: Boolean = true,
    width: Dp = 64.dp,
    label: (Int) -> String = { it.toString().padStart(2, '0') },
) {
    val total = if (infinite) count * 2000 else count
    val base = if (infinite) (total / 2) - ((total / 2) % count) else 0
    val state = rememberLazyListState(initialFirstVisibleItemIndex = base + value.coerceIn(0, count - 1))
    val latest by rememberUpdatedState(onValueChange)
    val haptic = LocalHapticFeedback.current
    var lastReported = value

    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }.filter { !it }.collect {
            val info = state.layoutInfo
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
            val nearest = info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - center) } ?: return@collect
            val delta = (nearest.offset + nearest.size / 2 - center).toFloat()
            if (abs(delta) > 1f) {
                state.animateScrollBy(delta, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
            } else {
                val v = nearest.index % count
                if (v != lastReported) {
                    lastReported = v
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    latest(v)
                }
            }
        }
    }

    Box(modifier = modifier.width(width).height(ItemHeight * 3), contentAlignment = Alignment.Center) {
        // The band behind the centred value.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ItemHeight)
                .clip(pillShape())
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)),
        )
        LazyColumn(
            state = state,
            contentPadding = PaddingValues(vertical = ItemHeight),
            modifier = Modifier.fillMaxHeight(),
        ) {
            items(total) { index ->
                val center = state.layoutInfo.let { (it.viewportStartOffset + it.viewportEndOffset) / 2 }
                val item = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                val away = if (item == null) 2f else abs(item.offset + item.size / 2 - center) / item.size.toFloat()
                Box(modifier = Modifier.fillMaxWidth().height(ItemHeight), contentAlignment = Alignment.Center) {
                    Text(
                        text = label(index % count),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.alpha((1f - away * 0.55f).coerceIn(0.25f, 1f)),
                    )
                }
            }
        }
    }
}

/** Hour, minute and (in 12-hour mode) AM/PM wheels. [value] is the time shown; [onChange] gets the new time. */
@Composable
fun TimeWheels(value: LocalTime, is24Hour: Boolean, onChange: (LocalTime) -> Unit, modifier: Modifier = Modifier) {
    val pm = value.hour >= 12
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        if (is24Hour) {
            WheelPicker(count = 24, value = value.hour, onValueChange = { onChange(value.withHour(it)) })
        } else {
            // 1 to 12 on the wheel, held as 0 to 11.
            WheelPicker(
                count = 12,
                value = (value.hour % 12 + 11) % 12,
                onValueChange = { shown -> onChange(value.withHour((shown + 1) % 12 + if (pm) 12 else 0)) },
                label = { (it + 1).toString() },
            )
        }
        Text(":", style = MaterialTheme.typography.titleLarge)
        WheelPicker(count = 60, value = value.minute, onValueChange = { onChange(value.withMinute(it)) })
        if (!is24Hour) {
            WheelPicker(
                count = 2,
                value = if (pm) 1 else 0,
                infinite = false,
                onValueChange = { half -> onChange(value.withHour(value.hour % 12 + if (half == 1) 12 else 0)) },
                label = { if (it == 0) "AM" else "PM" },
            )
        }
    }
}

/** Hours (0 to 12) and minutes (0 to 59) of a lecture's length; [minutes] is the total. */
@Composable
fun DurationWheels(minutes: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val h = (minutes / 60).coerceIn(0, 12)
    val m = minutes % 60
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        WheelPicker(count = 13, value = h, infinite = false, onValueChange = { onChange(it * 60 + m) }, label = { "$it h" })
        WheelPicker(count = 60, value = m, onValueChange = { onChange(h * 60 + it) }, label = { "$it min" }, width = 88.dp)
    }
}
