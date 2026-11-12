package skywa1ker404.absencium.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import skywa1ker404.absencium.appContainer
import skywa1ker404.absencium.data.db.ExamEntity
import skywa1ker404.absencium.data.settings.MarksScale
import skywa1ker404.absencium.domain.SubjectGrade
import skywa1ker404.absencium.domain.formatMarks
import skywa1ker404.absencium.domain.overallPercent
import skywa1ker404.absencium.domain.pretty
import skywa1ker404.absencium.domain.subjectGrades
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.AppTextField
import skywa1ker404.absencium.ui.components.ExpressiveChip
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.components.SectionTitle
import skywa1ker404.absencium.ui.components.SettingDivider
import skywa1ker404.absencium.ui.settings.rememberSettingsViewModel
import java.util.Locale

private fun scaleLabel(scale: MarksScale) = when (scale) {
    MarksScale.PERCENTAGE -> "Percentage"
    MarksScale.CGPA_10 -> "CGPA (10)"
    MarksScale.GPA_4 -> "GPA (4.0)"
}

private fun number(value: Double?): String = value?.let { if (it % 1.0 == 0.0) it.toLong().toString() else String.format(Locale.US, "%.2f", it) }.orEmpty()

/** Marks per subject from the exams that have a result, shown in the scale chosen here, with the pass mark of each subject. */
@Destination<RootGraph>
@Composable
fun GradesScreen(navigator: DestinationsNavigator) {
    val container = LocalContext.current.applicationContext.appContainer
    val settingsViewModel = rememberSettingsViewModel()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val data by container.calendarRepository.data.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val scale = settings.marksScale
    val grades = remember(data) { data?.subjectGrades().orEmpty() }
    val exams = data?.exams.orEmpty()
    var editing by remember { mutableStateOf<Long?>(null) }

    ScreenScaffold(title = "Grades & GPA", onBack = { navigator.navigateUp() }, largeTitle = true) {
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MarksScale.entries.forEach { option ->
                ExpressiveChip(selected = scale == option, onClick = { settingsViewModel.setMarksScale(option) }, label = { Text(scaleLabel(option)) })
            }
        }
        val overall = grades.overallPercent()
        StudyHero(
            label = "Overall",
            figure = overall?.let { formatMarks(it, scale) } ?: "–",
            caption = if (overall == null) "Add marks to an exam below to see your average" else "Average over ${grades.count { it.percent != null }} subjects",
            icon = skywa1ker404.absencium.ui.icons.AppIcons.School,
            hue = skywa1ker404.absencium.ui.components.hueFor(2), shape = BadgeShape.FLOWER,
        )
        if (grades.isEmpty()) HintText("Add subjects and exams first.")
        grades.forEach { grade ->
            val subjectExams = exams.filter { it.subjectId == grade.subject.id }.sortedBy { it.date }
            SectionTitle(grade.subject.name)
            AppCard(modifier = Modifier.fillMaxWidth()) {
                SubjectSummary(grade, scale)
                grade.percent?.let { CapsuleProgress((it / 100.0).toFloat(), if (grade.passing == false) colors.error else colors.primary, Modifier.padding(top = 10.dp)) }
                subjectExams.forEach { exam ->
                    SettingDivider()
                    ExamMarksRow(
                        exam = exam,
                        open = editing == exam.id,
                        onToggle = { editing = if (editing == exam.id) null else exam.id },
                        onSave = { obtained, max ->
                            scope.launch { container.calendarRepository.updateExam(exam.copy(marksObtained = obtained, maxMarks = max)) }
                            editing = null
                        },
                    )
                }
                if (subjectExams.isEmpty()) HintText("No exams for this subject yet.", modifier = Modifier.padding(top = 8.dp))
            }
        }
        HintText(
            "Weightage counts when every graded exam in a subject has one; otherwise the marks are added up. 10-point CGPA is the percentage divided by 10.",
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun SubjectSummary(grade: SubjectGrade, scale: MarksScale) {
    val container = LocalContext.current.applicationContext.appContainer
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    var pass by remember(grade.subject.passMark) { mutableStateOf(number(grade.subject.passMark)) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = grade.percent?.let { formatMarks(it, scale) } ?: "No marks yet",
                style = MaterialTheme.typography.titleLarge,
            )
            val passing = grade.passing
            Text(
                text = when (passing) {
                    null -> "${grade.graded} graded exams"
                    true -> "Passing · ${grade.graded} graded"
                    false -> "Below the pass mark · ${grade.graded} graded"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (passing == false) colors.error else colors.onSurfaceVariant,
            )
        }
        AppTextField(
            value = pass,
            onValueChange = { text ->
                pass = text
                val value = text.toDoubleOrNull()
                if (value != null && value in 0.0..100.0) {
                    scope.launch { container.database.subjectDao().update(grade.subject.copy(passMark = value)) }
                }
            },
            label = { Text("Pass %") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.padding(start = 12.dp).fillMaxWidth(0.3f),
        )
    }
}

@Composable
private fun ExamMarksRow(exam: ExamEntity, open: Boolean, onToggle: () -> Unit, onSave: (Double?, Double?) -> Unit) {
    val colors = MaterialTheme.colorScheme
    var obtained by remember(exam.marksObtained) { mutableStateOf(number(exam.marksObtained)) }
    var max by remember(exam.maxMarks) { mutableStateOf(number(exam.maxMarks)) }
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(exam.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = exam.date.pretty() + (exam.weightage?.let { " · weight ${number(it)}" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            val scored = exam.marksObtained
            val outOf = exam.maxMarks
            PillButton(
                text = if (scored != null && outOf != null) "${number(scored)} / ${number(outOf)}" else "Add marks",
                onClick = onToggle, tonal = true, compact = true,
            )
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = open,
            enter = androidx.compose.animation.expandVertically(androidx.compose.animation.core.spring(0.8f, 380f)) + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.shrinkVertically(androidx.compose.animation.core.spring(1f, 500f)) + androidx.compose.animation.fadeOut(),
        ) {
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AppTextField(
                    value = obtained, onValueChange = { obtained = it }, label = { Text("Marks") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f),
                )
                AppTextField(
                    value = max, onValueChange = { max = it }, label = { Text("Out of") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f),
                )
                PillButton(
                    text = "Save",
                    enabled = (obtained.isBlank() && max.isBlank()) || (obtained.toDoubleOrNull() != null && (max.toDoubleOrNull() ?: 0.0) > 0.0),
                    onClick = { onSave(obtained.toDoubleOrNull(), max.toDoubleOrNull()) },
                    compact = true,
                )
            }
        }
    }
}
