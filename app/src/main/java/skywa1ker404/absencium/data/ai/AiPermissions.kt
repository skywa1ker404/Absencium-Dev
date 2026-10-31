package skywa1ker404.absencium.data.ai

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A part of the app's data Absencium AI can be allowed to look at and propose changes to. */
enum class AiScope(val label: String, val blurb: String) {
    SUBJECTS("Subjects", "Your subject names, so it can refer to them and add new ones."),
    TIMETABLE("Timetable", "Your weekly lectures. It can propose adding or moving a lecture."),
    EXAMS("Exams", "Exam dates and rooms. It can propose adding or changing an exam."),
    HOLIDAYS("Holidays", "Your holidays. It can propose adding one."),
    ATTENDANCE("Attendance", "Present and absent counts and days. It can propose marking a day."),
    NOTES("Notes", "The titles of your notes (never their text). It can propose a new note."),
}

/**
 * Which parts of the app's data Absencium AI may use. Every one is off until the student turns it on, and a change is never applied
 * without the student tapping Apply on a card. Kept on this phone only.
 */
class AiPermissions(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("absencium_ai_permissions", Context.MODE_PRIVATE)
    private val _allowed = MutableStateFlow(read())
    val allowed: StateFlow<Set<AiScope>> = _allowed.asStateFlow()

    fun set(scope: AiScope, on: Boolean) {
        prefs.edit { putBoolean(scope.name, on) }
        _allowed.value = read()
    }

    fun isAllowed(scope: AiScope) = scope in _allowed.value

    private fun read(): Set<AiScope> = AiScope.entries.filter { prefs.getBoolean(it.name, false) }.toSet()
}
