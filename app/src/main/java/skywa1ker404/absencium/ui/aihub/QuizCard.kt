package skywa1ker404.absencium.ui.aihub

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import skywa1ker404.absencium.data.ai.AiQuiz
import skywa1ker404.absencium.data.ai.QuizQuestion
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.PillButton

/**
 * A quiz the AI set, asked one question at a time: tap an option (multiple choice, true or false) or type an answer, see at once whether
 * it was right with the explanation, and finish with a score and a review of the missed questions, which can be asked again.
 * Typed answers that do not match the model answer word for word are judged by the student against it.
 */
@Composable
fun QuizCard(json: String) {
    val colors = MaterialTheme.colorScheme
    val all = remember(json) { AiQuiz.parse(json) }
    val title = remember(json) { AiQuiz.title(json) }
    var round by remember(json) { mutableStateOf(all) }
    var index by remember(json) { mutableIntStateOf(0) }
    var picked by remember(json) { mutableStateOf<Int?>(null) }
    var typed by remember(json) { mutableStateOf("") }
    var revealed by remember(json) { mutableStateOf(false) }
    var verdict by remember(json) { mutableStateOf<Boolean?>(null) }
    var judging by remember(json) { mutableStateOf(false) }
    var score by remember(json) { mutableIntStateOf(0) }
    var done by remember(json) { mutableStateOf(false) }
    val missed = remember(json) { mutableStateListOf<QuizQuestion>() }

    fun restart(list: List<QuizQuestion>) {
        round = list; index = 0; picked = null; typed = ""; revealed = false; verdict = null; judging = false; score = 0; done = false; missed.clear()
    }
    if (all.isEmpty()) return

    AppCard(modifier = Modifier.fillMaxWidth(), container = colors.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().animateContentSize(spring(stiffness = 400f)), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (done) {
                Text(if (title.isNotBlank()) "$title: finished" else "Quiz finished", style = MaterialTheme.typography.titleMedium)
                Text("$score of ${round.size}", style = MaterialTheme.typography.displaySmall, color = colors.primary)
                if (missed.isNotEmpty()) {
                    Text("To review", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                    missed.forEach { q ->
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surfaceContainerHigh).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(q.question, style = MaterialTheme.typography.bodyMedium)
                            Text("Answer: ${q.answer}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = colors.primary)
                            if (q.explain.isNotBlank()) Text(q.explain, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                } else {
                    Text("Every answer was right.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (missed.isNotEmpty()) PillButton(text = "Retry the missed", onClick = { restart(missed.toList()) }, compact = true)
                    PillButton(text = "Start again", onClick = { restart(all) }, compact = true, tonal = true)
                }
                return@Column
            }
            val q = round[index]
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (title.isNotBlank()) title else "Quiz", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f, fill = false))
                Text("${index + 1} of ${round.size}", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
            }
            LinearProgressIndicator(
                progress = { (index + if (revealed) 1 else 0) / round.size.toFloat() },
                modifier = Modifier.fillMaxWidth().clip(CircleShape),
            )
            Text(q.question, style = MaterialTheme.typography.titleMedium)

            fun finishQuestion(right: Boolean?) {
                revealed = true
                verdict = right
                if (right == true) score++ else if (right == false) missed += q
            }

            if (q.kind == QuizQuestion.Kind.TYPED) {
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    enabled = !revealed,
                    singleLine = true,
                    placeholder = { Text("Type your answer") },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!revealed) {
                    PillButton(
                        text = "Check",
                        enabled = typed.isNotBlank(),
                        compact = true,
                        onClick = {
                            val result = AiQuiz.checkTyped(q, typed)
                            if (result == null) {
                                revealed = true
                                judging = true
                            } else {
                                finishQuestion(result)
                            }
                        },
                    )
                }
            } else {
                q.options.forEachIndexed { i, option ->
                    val state = when {
                        !revealed -> OptionState.IDLE
                        i == q.correct -> OptionState.CORRECT
                        i == picked -> OptionState.WRONG
                        else -> OptionState.DIM
                    }
                    QuizOption(letter = ('A' + i).toString(), text = option, state = state) {
                        if (!revealed) {
                            picked = i
                            finishQuestion(i == q.correct)
                        }
                    }
                }
            }

            if (revealed) {
                if (judging) {
                    Text("Your answer: $typed", style = MaterialTheme.typography.bodyMedium)
                    Text("Model answer: ${q.answer}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = colors.primary)
                    Text("Does yours say the same thing?", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillButton(text = "Yes, I had it", onClick = { judging = false; finishQuestion(true) }, compact = true)
                        PillButton(text = "No, I missed it", onClick = { judging = false; finishQuestion(false) }, compact = true, tonal = true)
                    }
                } else {
                    val right = verdict == true
                    Text(
                        if (right) "Correct" else "Not quite. Answer: ${q.answer}",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (right) colors.primary else colors.error,
                    )
                    if (q.explain.isNotBlank()) Text(q.explain, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    PillButton(
                        text = if (index + 1 < round.size) "Next" else "See score",
                        compact = true,
                        onClick = {
                            if (index + 1 < round.size) {
                                index++; picked = null; typed = ""; revealed = false; verdict = null
                            } else {
                                done = true
                            }
                        },
                    )
                }
            }
        }
    }
}

private enum class OptionState { IDLE, CORRECT, WRONG, DIM }

@Composable
private fun QuizOption(letter: String, text: String, state: OptionState, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val background by animateColorAsState(
        when (state) {
            OptionState.IDLE -> colors.surfaceContainerHigh
            OptionState.CORRECT -> colors.tertiaryContainer
            OptionState.WRONG -> colors.errorContainer
            OptionState.DIM -> colors.surfaceContainerHigh.copy(alpha = 0.5f)
        },
        spring(stiffness = 500f),
        label = "quizOption",
    )
    val content = when (state) {
        OptionState.CORRECT -> colors.onTertiaryContainer
        OptionState.WRONG -> colors.onErrorContainer
        OptionState.DIM -> colors.onSurfaceVariant
        OptionState.IDLE -> colors.onSurface
    }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(background).clickable(enabled = state == OptionState.IDLE, onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(colors.surface.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
            Text(letter, style = MaterialTheme.typography.labelLarge, color = content)
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, color = content, modifier = Modifier.weight(1f))
    }
}
