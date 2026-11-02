package skywa1ker404.absencium.data.ai

import skywa1ker404.absencium.data.calendar.CalendarData
import skywa1ker404.absencium.data.db.AttendanceStatus
import skywa1ker404.absencium.data.db.NoteEntity
import skywa1ker404.absencium.data.db.SyllabusNodeEntity
import skywa1ker404.absencium.data.db.SyllabusStatus
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Everything the chat needs to know about the student, gathered by the chat service and turned into text here (pure, so it can be tested). */
data class ChatContext(
    val allowed: Set<AiScope> = emptySet(),
    val calendar: CalendarData = CalendarData(),
    val notes: List<NoteEntity> = emptyList(),
    /** Study material files (names only; their content is only sent after the student approves a read). */
    val materials: List<skywa1ker404.absencium.data.db.GuideEntity> = emptyList(),
    val syllabus: List<SyllabusNodeEntity> = emptyList(),
    val thresholdPercent: Int = 75,
    val studentName: String? = null,
    val study: Boolean = false,
    val webSearch: Boolean = false,
    /** Study mode difficulty: "", "beginner", "school", "college" or "expert". */
    val level: String = "",
    /** What the student remembered for this question (global memories and this chat's own), when Memory is allowed. */
    val memories: List<MemoryItem> = emptyList(),
    /** Custom instructions of the assistant profile this chat uses. */
    val instructions: String = "",
    val temperature: Double? = null,
    val today: LocalDate = LocalDate.now(),
)

/**
 * The system instruction Absencium AI is given. It is a capable general assistant first; the app's data is extra context it may use, and
 * only the parts the student allowed are ever included. Notes' text is never included, only their titles.
 */
object AiChatPrompt {
    private const val MAX_LINES = 40
    private const val MAX_SYLLABUS_LINES = 80

    fun system(c: ChatContext): String = buildString {
        append("You are Absencium AI, the student's study companion inside the Absencium app, and a sharp, versatile assistant: studying, explaining and solving problems in any subject, writing, coding, planning, advice, general knowledge. ")
        append("Never limit yourself to the app's data and never deflect because something is not in the app.\n\n")
        append("HOW YOU TALK. Sound like a smart, friendly person who knows the subject, not like a template.\n")
        append("- Answer the actual question in the first sentence. No openers (\"Great question\", \"Sure\", \"Certainly\"), no restating the question, no closing offers (\"Let me know if...\", \"I hope this helps\").\n")
        append("- Let the question set the length and shape. A casual or simple message gets a sentence or two of plain prose, no headings, no bullets. Use lists, tables, headings or code only when the content really is a list, a comparison, steps or code. Do not repeat the same layout in every answer.\n")
        append("- Write for a phone: short paragraphs, the key point first, nothing padded. Hard problems get the key steps and a check of the result, not a lecture.\n")
        append("- Have a view. When asked which is better or what to do, pick one and say why. When you are unsure, say so in a few words instead of guessing. If the student's own work has a mistake, point to it.\n")
        append("- Use what you know of the conversation (earlier messages, their subjects and exams when allowed) naturally. Ask one question back only when you truly cannot answer well without it.\n")
        append("- No emojis unless the student uses them. Do not introduce yourself or mention being offline or an app unless asked. Reply in the language the student writes in.\n")
        append("- Use Markdown. Write maths in plain text or Unicode (x², √, ∫, ≤), not LaTeX.\n")
        append("Today is ${c.today} (${c.today.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}).")
        c.studentName?.takeIf { it.isNotBlank() }?.let { append(" The student's name is $it.") }
        append('\n')
        if (c.instructions.isNotBlank()) append("\nCUSTOM INSTRUCTIONS FROM THE STUDENT (follow them in every answer):\n").append(c.instructions.trim()).append('\n')
        append("\nNOTES AND LONG TEXT. When the student wants notes, a summary, a revision sheet or a plan to keep, write it completely at the length they ask for ")
        append("(moderate is about 500 to 800 words, lengthy 1,500 to 2,500, in depth 3,500 or more; lengthy when they give none). Cover every point with explanations, examples and exam-style questions; never abbreviate or ask whether to continue. ")
        append(if (AiScope.NOTES in c.allowed) "To save it as a note, propose an add_note action (below): the app's note writer then writes the full note with the editor's formatting. In the chat itself use plain Markdown.\n" else "Use plain Markdown; the student can save the answer as a note.\n")
        append("\nQUIZZES. When the student asks to be quizzed or tested, never write the questions or the answers in your text. Say one short line, then send ONE fenced block labelled absencium-quiz holding a JSON object; the app asks the questions one at a time, checks the answers and shows the score. ")
        append("""Format: {"title":"","questions":[{"type":"mcq","q":"","options":["","","",""],"answer":"B","explain":"one line"},{"type":"true_false","q":"a statement","answer":"true","explain":""},{"type":"short","q":"","answer":"model answer","accept":["other correct wordings"],"explain":""}]}. """)
        append("Mix the types, default to 5 questions (more when asked), make wrong options plausible, keep typed answers to a word or short phrase, and base the questions on the topic, note or study material they name (read it first with read_content when it is listed). If no topic can be told, ask which one in a single line instead of guessing.\n")
        if (c.study) {
            append("\nSTUDY MODE is on. Teach: explain the idea simply first, then build up, using an example and an analogy. ")
            val level = when (c.level) {
                "beginner" -> "a complete beginner: plain words, no assumed background"
                "school" -> "a school student: simple language with everyday examples"
                "college" -> "a college student: proper terms, derivations and worked examples"
                "expert" -> "an expert: rigorous, concise, with edge cases and references to the theory"
                else -> null
            }
            append(if (level != null) "Pitch it for $level. Adjust at once when they say it is too easy or too hard. " else "Match the difficulty to the student's level (ask once if you cannot tell, or adjust when they say it is too easy or hard). ")
            append("When it fits, finish with one short question or exercise that checks understanding.\n")
        }
        if (c.webSearch) append("\nWeb search is on: use it for anything recent or factual that may have changed, and base the answer on what you find.\n")
        if (c.allowed.isEmpty()) {
            append("\nYou cannot see or change anything in the student's Absencium data. Mention that they can allow it in Settings, AI & Integrations, only when it would genuinely help.\n")
        } else {
            append("\nThe student allowed you to use these parts of their Absencium data: ${c.allowed.joinToString { it.label }}. Nothing else is shared.\n\n")
            append(context(c))
            append("\nLINKS. Whenever you mention a subject, note or study material file from the lists above, write its exact name in double square brackets, like [[Cell division]]: the app turns it into a link the student taps to open it. Do this on your own, once per item, with no extra words; never use double brackets for anything else and never write web addresses for app content.\n")
            append(actions(c.allowed))
            if (AiScope.MEMORY in c.allowed) append(memory(c.memories))
        }
        append("\nApp facts, only when asked: Absencium keeps notes, attendance, timetable, exams, syllabus and study materials on the student's device.\n")
        append("\nBEFORE YOU ANSWER, REMEMBER: respond to exactly what was asked, at the length it deserves (a single word or topic gets a compact overview, not a lecture). Do not end with \"Anything else?\", an offer or a recap. Do not add a \"quick check\" or quiz unless asked; when asked for a quiz use the absencium-quiz block only and never print the answers.\n")
    }

    private fun memory(items: List<MemoryItem>): String = buildString {
        if (items.isNotEmpty()) {
            append("\nWHAT YOU REMEMBER ABOUT THE STUDENT (use it naturally where it helps; never recite it):\n")
            items.forEach { append("- ").append(it.text).append('\n') }
        }
        append("\nTo remember something durable and useful about the student for later chats (their course, year, goals, strengths and weak spots, how they like answers), ")
        append("add {\"type\":\"remember\",\"text\":\"one short sentence\"} to the absencium-actions block; add \"chat_only\":true when it only matters for this conversation. ")
        append("It is saved by itself and needs no card: never mention it unless asked, and never save one-off requests, secrets, passwords or other people's private details.\n")
    }

    private fun actions(allowed: Set<AiScope>): String = buildString {
        append("\nCHANGING THE APP. Only when the student asks you to add, change or mark something (or you want to remember something), end your answer with ONE fenced block labelled absencium-actions ")
        append("holding a JSON array, and only for the allowed parts. The student sees each change as a card and decides, so never say a change has been made; say what you propose. ")
        append("Use subject and topic names exactly as listed. Dates YYYY-MM-DD, times HH:mm, days MONDAY..SUNDAY. Types:\n")
        val types = buildList {
            if (AiScope.SUBJECTS in allowed) add("""{"type":"add_subject","name":""}""")
            if (AiScope.NOTES in allowed || AiScope.MATERIAL in allowed) {
                add("""{"type":"read_content","kind":"note|material","subject":"","title":""} (only when you need the actual text of a listed note or study material file to answer; use the exact subject and title as listed, at most 4 per answer. The student approves each read with one tap; the app then gives you the content straight away and you carry on answering with it, so just say in a short line what you are about to read. Never ask the student to paste or send the text)""")
            }
            if (AiScope.NOTES in allowed) {
                add("""{"type":"add_note","subject":"","title":"","brief":"what the note must cover","length":"moderate|lengthy|in_depth"}""")
                add("""(for add_note leave "body" out: the app's note writer then writes the full note from your brief. Only put "body" for a note of a few lines)""")
            }
            if (AiScope.TIMETABLE in allowed) {
                add("""{"type":"add_timetable","day":"MONDAY","subject":"","start":"09:00","end":"10:00","room":""}""")
                add("""{"type":"update_timetable","subject":"","day":"MONDAY","match_start":"09:00","new_day":"","start":"","end":"","room":""} (leave out what stays the same)""")
            }
            if (AiScope.EXAMS in allowed) {
                add("""{"type":"add_exam","subject":"","date":"","title":"","room":"","weightage":0}""")
                add("""{"type":"update_exam","match":"title or subject","title":"","date":"","room":""} (leave out what stays the same)""")
            }
            if (AiScope.HOLIDAYS in allowed) add("""{"type":"add_holiday","date":"","title":""}""")
            if (AiScope.ATTENDANCE in allowed) add("""{"type":"set_attendance","subject":"","date":"","status":"PRESENT|ABSENT|CANCELLED"}""")
            if (AiScope.SYLLABUS in allowed) add("""{"type":"set_syllabus_status","subject":"","topic":"","status":"NOT_STARTED|IN_PROGRESS|COMPLETED"}""")
        }
        types.forEach { append("- ").append(it).append('\n') }
        append("If a subject does not exist yet, add it first in the same array.\n")
    }

    fun context(c: ChatContext): String = buildString {
        val data = c.calendar
        val today = c.today
        val names = data.subjects.associate { it.id to it.name }
        append("APP DATA (read-only snapshot)\n")
        if (names.isNotEmpty()) append("Subjects: ").append(names.values.joinToString(", ")).append('\n')
        if (AiScope.TIMETABLE in c.allowed) {
            val holidays = data.holidays.associateBy { it.date }
            fun day(date: LocalDate, label: String) {
                val lectures = data.timetable.filter { it.dayOfWeek == date.dayOfWeek }.sortedBy { it.startTime }
                val off = holidays[date]
                val line = when {
                    off != null -> "holiday (${off.title})"
                    lectures.isEmpty() -> "no lectures"
                    else -> lectures.joinToString("; ") { "${it.startTime}-${it.endTime} ${names[it.subjectId].orEmpty()}" + (it.roomNumber?.let { r -> " room $r" } ?: "") }
                }
                append("$label ($date): $line\n")
            }
            day(today, "Today")
            day(today.plusDays(1), "Tomorrow")
            val lines = data.timetable.sortedWith(compareBy({ it.dayOfWeek }, { it.startTime })).take(MAX_LINES)
                .map { "${it.dayOfWeek} ${it.startTime}-${it.endTime} ${names[it.subjectId].orEmpty()}" + (it.roomNumber?.let { r -> " room $r" } ?: "") }
            if (lines.isNotEmpty()) append("Weekly timetable:\n").append(lines.joinToString("\n") { "- $it" }).append('\n')
        }
        if (AiScope.EXAMS in c.allowed) {
            val lines = data.exams.filter { !it.date.isBefore(today) }.sortedBy { it.date }.take(MAX_LINES).map {
                val days = ChronoUnit.DAYS.between(today, it.date)
                "${it.date} (${if (days == 0L) "today" else "in $days days"}) ${it.title} (${names[it.subjectId].orEmpty()})" +
                    (it.room?.let { r -> " room $r" } ?: "")
            }
            if (lines.isNotEmpty()) append("Upcoming exams:\n").append(lines.joinToString("\n") { "- $it" }).append('\n')
        }
        if (AiScope.HOLIDAYS in c.allowed) {
            val lines = data.holidays.filter { !it.date.isBefore(today) }.sortedBy { it.date }.take(MAX_LINES).map { "${it.date} ${it.title}" }
            if (lines.isNotEmpty()) append("Upcoming holidays:\n").append(lines.joinToString("\n") { "- $it" }).append('\n')
        }
        if (AiScope.ATTENDANCE in c.allowed) {
            val lines = data.subjects.mapNotNull { s ->
                val rows = data.attendance.filter { it.subjectId == s.id }
                if (rows.isEmpty()) null else attendanceLine(s.name, rows.count { it.status == AttendanceStatus.PRESENT }, rows.count { it.status == AttendanceStatus.ABSENT }, c.thresholdPercent)
            }
            append("Required attendance: ${c.thresholdPercent}%. A subject marked SAFE meets it and is fine: never say it needs attention or care. Only a subject marked BELOW REQUIREMENT is a concern; a safe one close to the limit may get a gentle heads-up at most.\n")
            if (lines.isNotEmpty()) append("Attendance:\n").append(lines.joinToString("\n") { "- $it" }).append('\n')
        }
        if (AiScope.SYLLABUS in c.allowed) {
            val text = syllabus(c.syllabus, names)
            if (text.isNotBlank()) append("Syllabus (status per topic):\n").append(text)
        }
        if (AiScope.NOTES in c.allowed) {
            val lines = c.notes.filter { it.deletedAt == null }.sortedByDescending { it.updatedAt }.take(MAX_LINES)
                .map { "${names[it.subjectId].orEmpty()}: ${it.title}" }
            if (lines.isNotEmpty()) append("Recent note titles (their text is not shared unless the student approves a read_content request):\n").append(lines.joinToString("\n") { "- $it" }).append('\n')
        }
        if (AiScope.MATERIAL in c.allowed) {
            val lines = c.materials.sortedByDescending { it.updatedAt }.take(MAX_LINES).map { "${names[it.subjectId].orEmpty()}: ${it.fileName}" }
            if (lines.isNotEmpty()) append("Study material files (their content is not shared unless the student approves a read_content request):\n").append(lines.joinToString("\n") { "- $it" }).append('\n')
        }
    }

    /** "Physics: 18 present, 4 absent, 81.8% (needs 75%): can skip 2 more lectures." */
    fun attendanceLine(name: String, present: Int, absent: Int, threshold: Int): String {
        val total = present + absent
        val pct = if (total == 0) 0.0 else present * 100.0 / total
        val head = "$name: $present present, $absent absent, ${"%.1f".format(pct)}% (needs $threshold%)"
        if (total == 0) return head
        return if (pct >= threshold) {
            val skip = if (threshold <= 0) Int.MAX_VALUE else (present * 100.0 / threshold - total).toInt().coerceAtLeast(0)
            "$head [SAFE]" + if (skip == Int.MAX_VALUE) "" else ": can miss $skip more lectures"
        } else {
            val need = if (threshold >= 100) null else Math.ceil((threshold * total - 100.0 * present) / (100 - threshold)).toInt()
            "$head [BELOW REQUIREMENT]" + if (need == null) ": below the requirement" else ": short, must attend the next $need lectures in a row"
        }
    }

    private fun syllabus(nodes: List<SyllabusNodeEntity>, names: Map<Long, String>): String = buildString {
        var lines = 0
        for ((subjectId, name) in names) {
            val all = nodes.filter { it.subjectId == subjectId }
            if (all.isEmpty() || lines >= MAX_SYLLABUS_LINES) continue
            val parents = all.map { it.parentId }.toSet()
            val leaves = all.filter { it.id !in parents }
            append("- $name: ${leaves.count { it.status == SyllabusStatus.COMPLETED }}/${leaves.size} topics completed\n")
            lines++
            fun walk(parent: Long?, depth: Int) {
                for (node in all.filter { it.parentId == parent }.sortedBy { it.position }) {
                    if (lines >= MAX_SYLLABUS_LINES) return
                    append("  ".repeat(depth + 1)).append("- ").append(node.title).append(" [").append(node.status.name.lowercase().replace('_', ' ')).append("]\n")
                    lines++
                    walk(node.id, depth + 1)
                }
            }
            walk(null, 0)
        }
    }
}

/** Shapes a stored conversation into turns every provider accepts: no empty turns, starts with the student, and roles alternate. */
object ChatTurns {
    fun normalize(turns: List<ChatTurn>): List<ChatTurn> {
        val out = ArrayList<ChatTurn>()
        for (turn in turns) {
            if (turn.text.isBlank() && turn.images.isEmpty()) continue
            if (out.isEmpty() && turn.role != ChatRole.USER) continue
            val last = out.lastOrNull()
            if (last != null && last.role == turn.role) {
                out[out.lastIndex] = last.copy(text = (last.text + "\n\n" + turn.text).trim(), images = last.images + turn.images)
            } else {
                out += turn
            }
        }
        return out
    }
}
