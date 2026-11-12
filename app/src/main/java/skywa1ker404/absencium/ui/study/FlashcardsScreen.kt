package skywa1ker404.absencium.ui.study

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import skywa1ker404.absencium.appContainer
import skywa1ker404.absencium.data.db.FlashcardEntity
import skywa1ker404.absencium.domain.CardGrade
import skywa1ker404.absencium.domain.reviewed
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.AppTextField
import skywa1ker404.absencium.ui.components.ExpressiveChip
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.components.SectionTitle
import java.time.LocalDate

/** Flashcards: review what is due (flip, then say how well you knew it), add cards by hand, or have Absencium AI make them. */
@Destination<RootGraph>
@Composable
fun FlashcardsScreen(navigator: DestinationsNavigator) {
    val container = LocalContext.current.applicationContext.appContainer
    val cards by container.database.flashcardDao().observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val data by container.calendarRepository.data.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val today = LocalDate.now()
    val due = cards.filter { !it.dueDate.isAfter(today) }

    val queue = remember { mutableStateListOf<FlashcardEntity>() }
    var reviewing by remember { mutableStateOf(false) }
    var revealed by remember { mutableStateOf(false) }
    var reviewed by remember { mutableStateOf(0) }

    var front by remember { mutableStateOf("") }
    var back by remember { mutableStateOf("") }
    var subjectId by remember { mutableStateOf<Long?>(null) }
    var aiBusy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val subjects = data?.subjects?.filter { it.isActive }.orEmpty()

    ScreenScaffold(title = "Flashcards", onBack = { if (reviewing) reviewing = false else navigator.navigateUp() }, largeTitle = true) {
        if (reviewing) {
            val current = queue.firstOrNull()
            if (current == null) {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Text("All done", style = MaterialTheme.typography.headlineSmall)
                    Text("You reviewed $reviewed cards.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    PillButton(text = "Back", onClick = { reviewing = false }, modifier = Modifier.padding(top = 12.dp))
                }
            } else {
                val flip by animateFloatAsState(
                    targetValue = if (revealed) 180f else 0f,
                    animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
                    label = "cardFlip",
                )
                Text("${queue.size} left", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
                AppCard(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp).graphicsLayer { rotationY = flip; cameraDistance = 16f * density },
                    onClick = { revealed = !revealed },
                    container = if (flip > 90f) colors.secondaryContainer else colors.primaryContainer,
                ) {
                    Box(modifier = Modifier.fillMaxWidth().heightIn(min = 190.dp).graphicsLayer { rotationY = if (flip > 90f) 180f else 0f }, contentAlignment = Alignment.Center) {
                        Text(
                            text = if (flip > 90f) current.back else current.front,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            color = if (flip > 90f) colors.onSecondaryContainer else colors.onPrimaryContainer,
                        )
                    }
                }
                if (!revealed) {
                    HintText("Tap the card to see the answer.")
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        CardGrade.entries.forEach { grade ->
                            PillButton(
                                text = grade.name.lowercase().replaceFirstChar { it.uppercase() },
                                tonal = grade != CardGrade.GOOD, compact = true, modifier = Modifier.weight(1f),
                                onClick = {
                                    val next = current.reviewed(grade, today)
                                    scope.launch { container.database.flashcardDao().update(next) }
                                    queue.removeAt(0)
                                    reviewed++
                                    // A card that was missed comes back at the end of this session.
                                    if (grade == CardGrade.AGAIN) queue.add(next)
                                    revealed = false
                                },
                            )
                        }
                    }
                }
            }
        } else {
            StudyHero(
                label = "Due today", figure = "${due.size} ${if (due.size == 1) "card" else "cards"}",
                caption = "${cards.size} cards in total",
                icon = skywa1ker404.absencium.ui.icons.AppIcons.Book,
                hue = skywa1ker404.absencium.ui.components.hueFor(1), shape = BadgeShape.CLOVER,
            )
            PillButton(
                text = "Review", enabled = due.isNotEmpty(),
                onClick = { queue.clear(); queue.addAll(due.shuffled()); reviewed = 0; revealed = false; reviewing = true },
            )

            SectionTitle("Add a card")
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (subjects.isNotEmpty()) {
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ExpressiveChip(selected = subjectId == null, onClick = { subjectId = null }, label = { Text("No subject") })
                            subjects.forEach { s -> ExpressiveChip(selected = subjectId == s.id, onClick = { subjectId = s.id }, label = { Text(s.name) }) }
                        }
                    }
                    AppTextField(value = front, onValueChange = { front = it }, label = { Text("Question") }, modifier = Modifier.fillMaxWidth())
                    AppTextField(value = back, onValueChange = { back = it }, label = { Text("Answer") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillButton(
                            text = "Add card", enabled = front.isNotBlank() && back.isNotBlank(),
                            onClick = {
                                val card = FlashcardEntity(subjectId = subjectId, front = front.trim(), back = back.trim())
                                scope.launch { container.database.flashcardDao().insert(card) }
                                front = ""; back = ""
                            },
                        )
                        PillButton(
                            text = "Make with AI", tonal = true, loading = aiBusy, enabled = !aiBusy,
                            onClick = {
                                aiBusy = true
                                message = null
                                val subjectName = subjects.firstOrNull { it.id == subjectId }?.name
                                scope.launch {
                                    val topic = if (subjectName != null) "the subject \"$subjectName\" (use its syllabus and notes)" else "the student's recent notes"
                                    val result = container.aiTasks.run(
                                        "Flashcards",
                                        "Make about 10 short flashcards for $topic. Call list_flashcards first so none repeats, read what you need with get_syllabus, search_notes and read_note, then add them with one add_flashcards call (one card per line: question ||| answer, one fact per card).",
                                    )
                                    message = result.error ?: "Done. Open AI actions to see what the assistant did."
                                    aiBusy = false
                                }
                            },
                        )
                    }
                    message?.let { HintText(it) }
                }
            }

            if (cards.isNotEmpty()) {
                SectionTitle("All cards")
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    cards.take(100).forEachIndexed { index, card ->
                        if (index > 0) skywa1ker404.absencium.ui.components.SettingDivider()
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(card.front, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                                Text(
                                    text = if (card.dueDate.isAfter(today)) "Next ${card.dueDate}" else "Due now",
                                    style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                                )
                            }
                            PillButton(text = "Delete", tonal = true, compact = true, onClick = { scope.launch { container.database.flashcardDao().delete(card.id) } })
                        }
                    }
                }
            }
        }
    }
}
