package skywa1ker404.absencium.domain

import skywa1ker404.absencium.data.calendar.CalendarData
import skywa1ker404.absencium.data.db.SubjectEntity
import skywa1ker404.absencium.data.settings.MarksScale
import java.util.Locale

/** The marks of one subject, worked out from its exams that have a result. */
data class SubjectGrade(
    val subject: SubjectEntity,
    /** Exams with a result, counted in the figure below. */
    val graded: Int,
    /** Percent over the graded exams, using each exam's weightage when every one of them has one. */
    val percent: Double?,
) {
    val passing: Boolean? get() = percent?.let { it >= subject.passMark }
}

fun CalendarData.subjectGrades(): List<SubjectGrade> {
    val bySubject = exams.groupBy { it.subjectId }
    return subjects.filter { it.isActive }.map { subject ->
        val done = bySubject[subject.id].orEmpty().filter {
            val max = it.maxMarks
            it.marksObtained != null && max != null && max > 0
        }
        val weighted = done.isNotEmpty() && done.all { (it.weightage ?: 0.0) > 0.0 }
        val percent = when {
            done.isEmpty() -> null
            weighted -> {
                val weight = done.sumOf { it.weightage!! }
                done.sumOf { it.marksObtained!! / it.maxMarks!! * 100.0 * it.weightage!! } / weight
            }
            else -> done.sumOf { it.marksObtained!! } / done.sumOf { it.maxMarks!! } * 100.0
        }
        SubjectGrade(subject, done.size, percent)
    }
}

/** Grade points on a 4.0 scale from a percentage (a common US letter-grade table). */
fun gpa4(percent: Double): Double = when {
    percent >= 93 -> 4.0
    percent >= 90 -> 3.7
    percent >= 87 -> 3.3
    percent >= 83 -> 3.0
    percent >= 80 -> 2.7
    percent >= 77 -> 2.3
    percent >= 73 -> 2.0
    percent >= 70 -> 1.7
    percent >= 67 -> 1.3
    percent >= 63 -> 1.0
    percent >= 60 -> 0.7
    else -> 0.0
}

/** A percentage written in the chosen scale: "76.5%", "7.7 / 10" or "3.0 / 4.0". */
fun formatMarks(percent: Double, scale: MarksScale): String = when (scale) {
    MarksScale.PERCENTAGE -> String.format(Locale.US, "%.1f%%", percent)
    MarksScale.CGPA_10 -> String.format(Locale.US, "%.1f / 10", (percent / 10.0).coerceAtMost(10.0))
    MarksScale.GPA_4 -> String.format(Locale.US, "%.1f / 4.0", gpa4(percent))
}

/** The average of the subjects that have marks, in the same way as each subject. Null when none has any. */
fun List<SubjectGrade>.overallPercent(): Double? =
    mapNotNull { it.percent }.takeIf { it.isNotEmpty() }?.average()
