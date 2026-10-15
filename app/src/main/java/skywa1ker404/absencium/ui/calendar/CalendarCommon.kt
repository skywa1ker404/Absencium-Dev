package skywa1ker404.absencium.ui.calendar

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import skywa1ker404.absencium.data.db.Priority
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * A stable colour per subject, used for exam dots and badges. It is picked from the active theme's own colours
 * (and blends of them), so the dots follow Default, Monet and every preset instead of using fixed hues.
 */
@Composable
fun subjectColor(subjectId: Long): Color {
    val scheme = MaterialTheme.colorScheme
    val palette = remember(scheme) {
        listOf(
            scheme.primary,
            scheme.tertiary,
            scheme.secondary,
            lerp(scheme.primary, scheme.tertiary, 0.5f),
            lerp(scheme.secondary, scheme.tertiary, 0.5f),
            lerp(scheme.primary, scheme.secondary, 0.5f),
        )
    }
    return palette[Math.floorMod(subjectId, palette.size.toLong()).toInt()]
}

fun Priority.label(): String = when (this) {
    Priority.HIGH -> "High"
    Priority.NORMAL -> "Normal"
    Priority.LOW -> "Low"
}

/** "Today", "Tomorrow", "In 5 days" or "3 days ago". */
fun daysLabel(today: LocalDate, date: LocalDate): String {
    val days = ChronoUnit.DAYS.between(today, date)
    return when {
        days == 0L -> "Today"
        days == 1L -> "Tomorrow"
        days > 1L -> "In $days days"
        days == -1L -> "Yesterday"
        else -> "${-days} days ago"
    }
}
