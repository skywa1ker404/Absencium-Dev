package skywa1ker404.absencium.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import skywa1ker404.absencium.data.calendar.CalendarData
import skywa1ker404.absencium.data.db.AttendanceEntity
import skywa1ker404.absencium.data.db.AttendanceStatus
import skywa1ker404.absencium.data.db.SubjectEntity
import java.time.LocalDate

class AttendanceMathTest {
    private val maths = SubjectEntity(id = 1, name = "Maths")
    private val physics = SubjectEntity(id = 2, name = "Physics")
    private val retired = SubjectEntity(id = 3, name = "Old", isActive = false)

    private fun mark(subject: Long, day: Int, status: AttendanceStatus) =
        AttendanceEntity(subjectId = subject, date = LocalDate.of(2026, 11, 1).plusDays(day.toLong()), status = status)

    @Test fun noLecturesHeldHasNoPercentAndNoMargin() {
        val row = CalendarData(subjects = listOf(maths)).subjectAttendance().single()
        assertEquals(0, row.held)
        assertNull(row.percent)
        assertEquals(Margin(0, 0), attendanceMargin(0, 0, 75))
    }

    @Test fun cancelledAndExcusedAreNotHeld() {
        val data = CalendarData(
            subjects = listOf(maths),
            attendance = listOf(
                mark(1, 0, AttendanceStatus.PRESENT),
                mark(1, 1, AttendanceStatus.ABSENT),
                mark(1, 2, AttendanceStatus.CANCELLED),
                mark(1, 3, AttendanceStatus.EXCUSED),
            ),
        )
        val row = data.subjectAttendance().single()
        assertEquals(1, row.present)
        assertEquals(1, row.absent)
        assertEquals(1, row.cancelled)
        assertEquals(2, row.held)
        assertEquals(50.0, row.percent!!, 0.0001)
    }

    @Test fun inactiveSubjectsAreLeftOut() {
        val rows = CalendarData(subjects = listOf(maths, retired)).subjectAttendance()
        assertEquals(listOf("Maths"), rows.map { it.subject.name })
    }

    @Test fun overallAddsUpHeldLecturesNotPercentages() {
        val data = CalendarData(
            subjects = listOf(maths, physics),
            attendance = listOf(
                mark(1, 0, AttendanceStatus.PRESENT),
                mark(2, 0, AttendanceStatus.PRESENT),
                mark(2, 1, AttendanceStatus.PRESENT),
                mark(2, 2, AttendanceStatus.ABSENT),
            ),
        )
        val overall = data.subjectAttendance().overall()
        assertEquals(3, overall.present)
        assertEquals(4, overall.held)
        assertEquals(75.0, overall.percent!!, 0.0001)
        assertNull(emptyList<SubjectAttendance>().overall().percent)
    }

    @Test fun canSkipCountsWholeLecturesAboveTheGoal() {
        // 9 of 10 at 75 %: surplus 900 - 750 = 150, 150 / 75 = 2 more misses (9 of 12 = 75 %).
        assertEquals(Margin(canSkip = 2, mustAttend = 0), attendanceMargin(9, 10, 75))
        // Exactly on the goal: nothing to spare.
        assertEquals(Margin(canSkip = 0, mustAttend = 0), attendanceMargin(3, 4, 75))
    }

    @Test fun mustAttendIsTheSmallestRunThatReachesTheGoal() {
        // 6 of 10 at 75 %: (6 + y) * 100 >= 75 * (10 + y) -> y >= 6.
        assertEquals(Margin(canSkip = 0, mustAttend = 6), attendanceMargin(6, 10, 75))
        val y = 6
        assertTrue((6 + y) * 100 >= 75 * (10 + y))
        assertTrue((6 + y - 1) * 100 < 75 * (10 + y - 1))
    }

    @Test fun aGoalOfOneHundredDoesNotDivideByZero() {
        // Treated as 99 %: 10 of 10 has no spare lecture; 9 of 10 needs 90 in a row to get back to 99 %.
        assertEquals(Margin(0, 0), attendanceMargin(10, 10, 100))
        assertEquals(Margin(0, 90), attendanceMargin(9, 10, 100))
    }
}
