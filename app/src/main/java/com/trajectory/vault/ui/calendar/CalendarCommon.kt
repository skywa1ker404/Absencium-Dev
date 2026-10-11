package com.trajectory.vault.ui.calendar

import androidx.compose.ui.graphics.Color
import com.trajectory.vault.data.db.Priority
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** A stable colour per subject, used for exam dots and badges. */
fun subjectColor(subjectId: Long): Color = Color.hsv(((subjectId * 67L) % 360L).toFloat(), 0.55f, 0.95f)

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
