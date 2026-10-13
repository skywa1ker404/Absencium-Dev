package com.trajectory.vault.domain

import com.trajectory.vault.data.calendar.CalendarData
import com.trajectory.vault.data.db.AttendanceStatus
import com.trajectory.vault.data.db.SubjectEntity

/** Attendance counts for one subject. Cancelled lectures are not counted as held. */
data class SubjectAttendance(
    val subject: SubjectEntity,
    val present: Int,
    val absent: Int,
    val cancelled: Int,
) {
    val held: Int get() = present + absent

    /** Null until at least one lecture has been held. */
    val percent: Double? get() = if (held == 0) null else present * 100.0 / held
}

/**
 * How far above or below the threshold attendance is.
 * [canSkip] is how many more lectures can be missed while staying at or above the threshold.
 * [mustAttend] is how many lectures in a row must be attended to get back to the threshold.
 * At most one of them is above zero.
 */
data class Margin(val canSkip: Int, val mustAttend: Int)

fun attendanceMargin(present: Int, held: Int, thresholdPercent: Int): Margin {
    if (held == 0) return Margin(canSkip = 0, mustAttend = 0)
    val surplus = 100 * present - thresholdPercent * held
    return if (surplus >= 0) {
        Margin(canSkip = surplus / thresholdPercent, mustAttend = 0)
    } else {
        // Smallest y with (present + y) * 100 >= threshold * (held + y); the threshold is at most 85.
        val denominator = 100 - thresholdPercent
        Margin(canSkip = 0, mustAttend = (-surplus + denominator - 1) / denominator)
    }
}

/** Attendance per active subject, counted from the logged records. */
fun CalendarData.subjectAttendance(): List<SubjectAttendance> {
    // One pass over the records instead of three passes per subject.
    val bySubject = attendance.groupBy { it.subjectId }
    return subjects.filter { it.isActive }.map { subject ->
        val entries = bySubject[subject.id].orEmpty()
        SubjectAttendance(
            subject = subject,
            present = entries.count { it.status == AttendanceStatus.PRESENT },
            absent = entries.count { it.status == AttendanceStatus.ABSENT },
            cancelled = entries.count { it.status == AttendanceStatus.CANCELLED },
        )
    }
}

/** Present and held lectures added up over all the given subjects. */
data class OverallAttendance(val present: Int, val held: Int) {
    val percent: Double? get() = if (held == 0) null else present * 100.0 / held
}

fun List<SubjectAttendance>.overall(): OverallAttendance =
    OverallAttendance(present = sumOf { it.present }, held = sumOf { it.held })
