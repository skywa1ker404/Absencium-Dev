package skywa1ker404.absencium.domain

import skywa1ker404.absencium.data.db.FlashcardEntity
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.roundToInt

enum class CardGrade { AGAIN, HARD, GOOD, EASY }

/**
 * The card after one review: the better it is known, the longer until it returns. "Again" keeps the card due today so it
 * comes back in the same session; the others set the next day from the interval and the card's ease.
 */
fun FlashcardEntity.reviewed(grade: CardGrade, today: LocalDate = LocalDate.now()): FlashcardEntity = when (grade) {
    CardGrade.AGAIN -> copy(reps = 0, lapses = lapses + 1, ease = max(1.3, ease - 0.2), intervalDays = 0, dueDate = today)
    CardGrade.HARD -> {
        val days = max(1, (max(intervalDays, 1) * 1.2).roundToInt())
        copy(reps = reps + 1, ease = max(1.3, ease - 0.15), intervalDays = days, dueDate = today.plusDays(days.toLong()))
    }
    CardGrade.GOOD -> {
        val days = when (reps) { 0 -> 1; 1 -> 3; else -> max(1, (intervalDays * ease).roundToInt()) }
        copy(reps = reps + 1, intervalDays = days, dueDate = today.plusDays(days.toLong()))
    }
    CardGrade.EASY -> {
        val days = if (reps == 0) 3 else max(2, (max(intervalDays, 1) * ease * 1.3).roundToInt())
        copy(reps = reps + 1, ease = ease + 0.15, intervalDays = days, dueDate = today.plusDays(days.toLong()))
    }
}
