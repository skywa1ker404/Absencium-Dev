package skywa1ker404.absencium.ui.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import skywa1ker404.absencium.domain.SubjectAttendance
import skywa1ker404.absencium.domain.attendanceMargin
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.ExpressiveChip
import skywa1ker404.absencium.ui.components.drawHatch
import skywa1ker404.absencium.ui.components.hueFor
import skywa1ker404.absencium.ui.theme.CircleShape
import skywa1ker404.absencium.ui.theme.LocalQuality
import skywa1ker404.absencium.ui.theme.pillShape
import kotlin.math.roundToInt

/**
 * One tinted row per subject: a letter chip, the name, the attendance percentage, a striped bar (the attended share is solid, the
 * rest is hatched, a tick marks the goal) and one line saying how many lectures can still be missed or must be attended.
 * A subject's own goal wins over the general [threshold].
 */
@Composable
fun SubjectAttendanceRows(
    rows: List<SubjectAttendance>,
    threshold: Int,
    onSetGoal: (subjectId: Long, goal: Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            key(row.subject.id) {
                SubjectAttendanceRow(row, row.subject.attendanceGoal ?: threshold, threshold, onSetGoal)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectAttendanceRow(row: SubjectAttendance, goal: Int, threshold: Int, onSetGoal: (Long, Int?) -> Unit) {
    var editing by rememberSaveable(row.subject.id) { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val q = LocalQuality.current
    val hue = hueFor(row.subject.id)
    val percent = row.percent
    val below = percent != null && percent < goal
    val fill by animateFloatAsState(
        targetValue = ((percent ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f),
        animationSpec = q.spring(),
        label = "subjectBarFill",
    )
    val barColor by animateColorAsState(
        targetValue = if (below) colors.error else hue.accent,
        animationSpec = q.spring(),
        label = "subjectBarColor",
    )
    val margin = attendanceMargin(row.present, row.held, goal)
    val line = when {
        row.held == 0 -> "No lectures held yet"
        margin.mustAttend > 0 -> "Attend the next ${margin.mustAttend} to reach $goal%"
        margin.canSkip > 0 -> "Can miss ${margin.canSkip} more and stay at $goal%"
        else -> "Right at $goal%; do not miss the next one"
    }
    val label = "${row.subject.name}, ${percent?.roundToInt()?.let { "$it percent" } ?: "no lectures yet"}, goal $goal percent"

    AppCard(container = hue.container, onClick = { editing = !editing }, modifier = Modifier.semantics { contentDescription = label }) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(hue.accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = row.subject.name.trim().firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.surface,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = row.subject.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = hue.onContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (row.held == 0) "Goal $goal%" else "${row.present} of ${row.held} attended · goal $goal%",
                        style = MaterialTheme.typography.bodySmall,
                        color = hue.onContainer.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = percent?.let { "${it.roundToInt()}%" } ?: "–",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (below) colors.error else hue.onContainer,
                )
            }
            StripedBar(fill = fill, goal = goal / 100f, color = barColor, stripe = hue.onContainer.copy(alpha = 0.28f), tick = hue.onContainer)
            Text(
                text = line,
                style = MaterialTheme.typography.labelMedium,
                color = if (below) colors.error else hue.onContainer.copy(alpha = 0.85f),
            )
            AnimatedVisibility(visible = editing) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Goal for this subject", style = MaterialTheme.typography.labelMedium, color = hue.onContainer)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExpressiveChip(
                            selected = row.subject.attendanceGoal == null,
                            onClick = { onSetGoal(row.subject.id, null) },
                            label = { Text("General $threshold%") },
                        )
                        GOAL_CHOICES.forEach { g ->
                            ExpressiveChip(
                                selected = row.subject.attendanceGoal == g,
                                onClick = { onSetGoal(row.subject.id, g) },
                                label = { Text("$g%") },
                            )
                        }
                    }
                }
            }
        }
    }
}

private val GOAL_CHOICES = listOf(60, 65, 70, 75, 80, 85, 90)

/** The attended share is solid; what is left of the bar is hatched; a thin tick marks the goal. */
@Composable
private fun StripedBar(fill: Float, goal: Float, color: androidx.compose.ui.graphics.Color, stripe: androidx.compose.ui.graphics.Color, tick: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(pillShape())
            .drawBehind { drawHatch(stripe) },
    ) {
        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(fill).clip(pillShape()).background(color))
        Box(modifier = Modifier.fillMaxWidth(goal).fillMaxHeight(), contentAlignment = Alignment.CenterEnd) {
            Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(tick))
        }
    }
}
