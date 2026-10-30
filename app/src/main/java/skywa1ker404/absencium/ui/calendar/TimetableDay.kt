package skywa1ker404.absencium.ui.calendar

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import skywa1ker404.absencium.data.db.TimetableEntity
import skywa1ker404.absencium.domain.hm
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.drawHatch
import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.theme.LocalQuality
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

/**
 * The weekdays as a strip of rounded squares (image 11): the short weekday above the date of that day this week. The chosen day is
 * filled with the theme's main colour, today has a small dot.
 */
@Composable
fun WeekStrip(days: List<DayOfWeek>, selected: DayOfWeek, today: LocalDate, onSelect: (DayOfWeek) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val q = LocalQuality.current
    val monday = today.with(DayOfWeek.MONDAY)
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        days.forEach { day ->
            val on = day == selected
            val date = monday.plusDays((day.value - 1).toLong())
            val lift by animateFloatAsState(
                targetValue = if (on) 1f else 0f,
                animationSpec = q.spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "weekTile",
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height((64 + 6 * lift).dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (on) colors.primary else colors.surfaceContainerHigh)
                    .clickable { onSelect(day) }
                    .semantics { this.selected = on; contentDescription = day.getDisplayName(TextStyle.FULL, Locale.getDefault()) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (on) colors.onPrimary.copy(alpha = 0.85f) else colors.onSurfaceVariant,
                    maxLines = 1,
                )
                Text(
                    date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (on) colors.onPrimary else colors.onSurface,
                )
                Box(
                    Modifier.padding(top = 3.dp).size(5.dp).clip(CircleShape)
                        .background(if (date == today) (if (on) colors.onPrimary else colors.primary) else androidx.compose.ui.graphics.Color.Transparent),
                )
            }
        }
    }
}

private enum class SlotPhase { PAST, NOW, LATER }

private fun minutesText(minutes: Long): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0L -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}

/**
 * One day's lectures in order. When [now] is given (the day is today) the lecture in progress is a highlighted card with how long is
 * left, a "Now" line sits above it (or above the next lecture between two), lectures that are over fade, and gaps of half an hour or
 * more show as hatched breaks.
 */
@Composable
fun TimetableDay(
    slots: List<TimetableEntity>,
    subjectName: (Long) -> String,
    now: LocalTime?,
    onEdit: (TimetableEntity) -> Unit,
    onDelete: (TimetableEntity) -> Unit,
) {
    val phases = slots.map { slot ->
        when {
            now == null -> SlotPhase.LATER
            !now.isBefore(slot.endTime) -> SlotPhase.PAST
            !now.isBefore(slot.startTime) -> SlotPhase.NOW
            else -> SlotPhase.LATER
        }
    }
    // Where the "Now" line goes when no lecture is running: before the first one still to come, if any has already finished.
    val lineAt = when {
        now == null -> -1
        phases.indexOf(SlotPhase.NOW) >= 0 -> phases.indexOf(SlotPhase.NOW)
        phases.contains(SlotPhase.PAST) && phases.contains(SlotPhase.LATER) -> phases.indexOf(SlotPhase.LATER)
        else -> -1
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        slots.forEachIndexed { index, slot ->
            if (index > 0) {
                val gap = Duration.between(slots[index - 1].endTime, slot.startTime).toMinutes()
                if (gap >= 30) BreakBlock(minutesText(gap))
            }
            if (index == lineAt) NowLine(if (now != null) now.hm() else "")
            SlotCard(
                slot = slot,
                name = subjectName(slot.subjectId),
                phase = phases[index],
                now = now,
                onEdit = { onEdit(slot) },
                onDelete = { onDelete(slot) },
            )
        }
    }
}

@Composable
private fun NowLine(time: String) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            "Now $time",
            style = MaterialTheme.typography.labelMedium,
            color = colors.onPrimary,
            modifier = Modifier.clip(CircleShape).background(colors.primary).padding(horizontal = 10.dp, vertical = 3.dp),
        )
        Box(Modifier.weight(1f).height(2.dp).clip(CircleShape).background(colors.primary.copy(alpha = 0.5f)))
    }
}

/** A free stretch between two lectures: a hatched strip with its length. */
@Composable
private fun BreakBlock(length: String) {
    val colors = MaterialTheme.colorScheme
    val hatch = colors.onSurfaceVariant.copy(alpha = 0.25f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceContainerHigh.copy(alpha = 0.5f))
            .drawBehind { drawHatch(hatch, spacing = 6.dp) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Break  $length",
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.clip(CircleShape).background(colors.surfaceContainerHighest).padding(horizontal = 12.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun SlotCard(
    slot: TimetableEntity,
    name: String,
    phase: SlotPhase,
    now: LocalTime?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val isNow = phase == SlotPhase.NOW
    val ink = if (isNow) colors.onPrimaryContainer else colors.onSurface
    val soft = if (isNow) colors.onPrimaryContainer.copy(alpha = 0.75f) else colors.onSurfaceVariant
    val total = Duration.between(slot.startTime, slot.endTime).toMinutes().coerceAtLeast(1)
    val done = if (isNow && now != null) Duration.between(slot.startTime, now).toMinutes().coerceIn(0, total) else 0L
    val fraction by animateFloatAsState(done.toFloat() / total, LocalQuality.current.spring(), label = "lectureProgress")
    AppCard(
        modifier = Modifier.fillMaxWidth().alpha(if (phase == SlotPhase.PAST) 0.55f else 1f),
        container = if (isNow) colors.primaryContainer else null,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.width(5.dp).height(56.dp).clip(CircleShape).background(subjectColor(slot.subjectId)))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(AppIcons.Schedule, contentDescription = null, tint = soft, modifier = Modifier.size(16.dp))
                    Text("${slot.startTime.hm()} to ${slot.endTime.hm()}", style = MaterialTheme.typography.labelLarge, color = soft)
                    if (isNow) {
                        Text(
                            "${minutesText(total - done)} left",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onPrimary,
                            modifier = Modifier.clip(CircleShape).background(colors.primary).padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(name, style = MaterialTheme.typography.titleMedium, color = ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val extra = listOfNotNull(
                    slot.roomNumber?.takeIf { it.isNotBlank() }?.let { "Room $it" },
                    slot.teacher?.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                if (extra.isNotEmpty()) Text(extra, style = MaterialTheme.typography.bodySmall, color = soft, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (isNow) {
                    Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(5.dp).clip(CircleShape).background(colors.primary.copy(alpha = 0.2f))) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).clip(CircleShape).background(colors.primary))
                    }
                }
            }
            Column {
                IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) { Icon(AppIcons.Edit, contentDescription = "Edit lecture", tint = soft, modifier = Modifier.size(20.dp)) }
                IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) { Icon(AppIcons.Delete, contentDescription = "Remove lecture", tint = soft, modifier = Modifier.size(20.dp)) }
            }
        }
    }
}
