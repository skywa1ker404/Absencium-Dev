package skywa1ker404.absencium.data.ai

/** How long the generated notes should be. */
enum class NoteLength(val label: String, val guide: String, val maxTokens: Int) {
    MODERATE("Moderate", "about 1 page, 500 to 800 words: the essentials, tight and easy to revise", 6_000),
    LENGTHY("Lengthy", "about 3 pages, 1,500 to 2,500 words: every important point with explanations and examples", 10_000),
    IN_DEPTH("In Depth", "6 pages or more, at least 3,500 words: complete coverage with reasoning, derivations, several worked examples, edge cases and exam questions", 16_000),
}

/** One choice of the single size setting each kind of notes has. Its words change with the kind (length, density, time you have, ...). */
data class NoteDepth(val label: String, val guide: String, val maxTokens: Int)

/**
 * The kind of notes wanted. [depthTitle] and [depths] are the one setting that suits this kind; [layout] lists what this kind
 * may contain. Whether each part is really needed is decided per topic by the planning step, never by a fixed template.
 */
enum class NoteTemplate(
    val label: String,
    val guide: String,
    val layout: String,
    val depthTitle: String,
    val depths: List<NoteDepth>,
) {
    FULL(
        "Full note",
        "The complete note on the topic and nothing else: no questions and answers, no past-paper questions.",
        "",
        "Notes length",
        NoteLength.entries.map { NoteDepth(it.label, it.guide, it.maxTokens) },
    ),
    KEY_DETAILS(
        "Key details, line by line",
        "One short, self-contained fact per line, grouped under small headings. No paragraphs.",
        "Only short headings (\"## \") and, under each, one fact per line as a \"- \" bullet of at most 15 words. No paragraphs, no introduction, no conclusion. " +
            "Put the single most important fact of each heading in ==highlight==. Group the facts in the order a student would learn them. " +
            "A table only if a set of facts is truly a table of values (for example constants), never to compare.",
        "Detail",
        listOf(
            NoteDepth("Essentials", "only the facts that decide marks, about 25 lines in total", 4_000),
            NoteDepth("Standard", "every important fact, about 50 lines in total", 7_000),
            NoteDepth("Exhaustive", "every fact the topic holds, 80 lines or more, still one fact per line", 10_000),
        ),
    ),
    SUMMARY(
        "Summary",
        "A compact summary a student can read in a few minutes, with the few things that must be remembered highlighted.",
        "A compact summary: a title, a short overview, the main points as bullets or short lines, and the few facts that must be remembered in ==highlight==. " +
            "No long sections, no derivations, no worked examples. Add a formula line or a tiny table only if the topic cannot be summarised without it.",
        "Size",
        listOf(
            NoteDepth("Brief", "about 150 words, a one-minute read", 2_500),
            NoteDepth("Standard", "about 300 words, a two-minute read", 4_000),
            NoteDepth("Detailed", "about 500 words, one page", 6_000),
        ),
    ),
    PYQ(
        "Previous year questions",
        "Questions of the kind this exam usually asks, grouped by marks, each with a model answer and the points where marks are earned.",
        "A question bank, not notes. Group by marks with headings that match the questions this topic really gets (for example very short, short and long). " +
            "Each question is a numbered line in **bold**, followed by a model answer sized to its marks, then the 2 to 4 points an examiner looks for. " +
            "Use diagrams, derivations or numericals only where this topic's questions use them. Do not invent exam years or paper names. " +
            "End with the most repeated topics only if the questions show a pattern.",
        "Question bank",
        listOf(
            NoteDepth("Quick set", "about 10 questions", 6_000),
            NoteDepth("Standard", "about 20 questions", 10_000),
            NoteDepth("Full bank", "35 or more questions", 16_000),
        ),
    ),
    LAST_MINUTE(
        "Last-minute exam preparation",
        "Rapid revision of what is most likely to be asked, in the order that gives the most marks first.",
        "A rapid-revision sheet built from the topic's most-tested content, most valuable first. Use only the parts the topic has: definitions, formulas and facts, mnemonics, " +
            "diagrams to practise drawing (name them, do not draw), common traps (in red), and a \"- [ ] \" checklist to tick off before the exam. " +
            "One line each, no explanation longer than a sentence, and nothing the student should be learning for the first time.",
        "Time you have",
        listOf(
            NoteDepth("15 minutes", "one short page: only the few things most often asked", 3_500),
            NoteDepth("1 hour", "one to two pages: the most-tested content with the key working", 6_500),
            NoteDepth("Night before", "two pages at most: everything that can come, still one line per item", 9_500),
        ),
    ),
    PRACTICAL(
        "Practical observation notes",
        "A practical record for this experiment, shaped to its subject.",
        "A practical record in the standard order, using only the parts this experiment needs: aim, apparatus or materials, theory (short), procedure (numbered steps), " +
            "observations (an EMPTY table with the right columns and blank rows for the student to fill in), calculation (the formulas with each symbol defined), result, precautions and sources of error. " +
            "Shape it to the subject: physics needs instrument readings, least count and a circuit or ray diagram note; chemistry needs balanced equations, colour or precipitate observations and safety; " +
            "biology needs a labelled-diagram note, stain and magnification. Skip a part, such as calculation, when the experiment has none.",
        "Record",
        listOf(
            NoteDepth("Brief", "aim, procedure, observation table and result only", 5_000),
            NoteDepth("Full record", "every part the experiment needs, ready to copy into the practical file", 8_000),
            NoteDepth("With viva", "the full record plus six to eight viva questions with one-line answers", 12_000),
        ),
    ),
}

/** Where in the student's course this belongs, taken from their syllabus tree. */
data class SyllabusContext(
    val course: String = "",
    val yearOrSemester: String = "",
    val unit: String = "",
    val chapter: String = "",
    val topic: String = "",
    val practical: Boolean = false,
) {
    val isEmpty get() = listOf(course, yearOrSemester, unit, chapter, topic).all { it.isBlank() }

    fun describe(): String = buildList {
        if (course.isNotBlank()) add("Course: $course")
        if (yearOrSemester.isNotBlank()) add("Year or semester: $yearOrSemester")
        if (unit.isNotBlank()) add("Unit: $unit")
        if (chapter.isNotBlank()) add("Chapter: $chapter")
        if (topic.isNotBlank()) add("Topic: $topic")
        if (practical) add("This belongs to the practical syllabus.")
    }.joinToString("\n")
}

/** Everything chosen in the Generate AI Notes or Solve Homework screen. [depthIndex] picks one of the kind's [NoteTemplate.depths]. */
data class NoteBrief(
    val request: String,
    val subject: String,
    val length: NoteLength = NoteLength.MODERATE,
    val template: NoteTemplate = NoteTemplate.FULL,
    val syllabus: SyllabusContext = SyllabusContext(),
    val searchDiagrams: Boolean = false,
    val drawDiagrams: Boolean = false,
    val hasPhotos: Boolean = false,
    val depthIndex: Int? = null,
) {
    /** The size setting in force: the chosen one for this kind of notes, else the plain length. */
    val depth: NoteDepth
        get() = depthIndex?.let { template.depths.getOrNull(it) } ?: NoteDepth(length.label, length.guide, length.maxTokens)
}

/**
 * The instructions sent with every notes or homework request. They teach the model exactly what the notes editor can
 * draw, so an answer uses headings, tables, lists, highlights and centred formulas instead of raw markup, and they ask it
 * to pick the sections the topic needs rather than filling one fixed template. What the student typed always wins.
 */
object NotePrompts {

    private val ALL_NEEDS = setOf("table", "formula", "colour", "extras")

    /**
     * What the notes editor can draw, written only as far as the notes need: headings, bold, highlight and lists are always there; a table,
     * a centred formula, colour and the rarer styles are described only when this kind of note or the plan can use them, so a short
     * summary does not carry the rules for tables and colours.
     */
    internal fun editor(needs: Set<String> = ALL_NEEDS): String = buildString {
        append("The notes editor draws only this: headings \"# \", \"## \", \"### \"; **bold** for key terms; ==highlight== for the one fact to remember; \"- \" bullets; \"1. \" numbered steps.")
        if ("table" in needs) append("\nTable: a header row, a |---|---| row, then rows; only when things are compared or listed with properties.")
        if ("formula" in needs) append("\nA formula or key result goes on its own line starting {align:center}, in plain Unicode (² √ ± ≤ → Δ), never LaTeX, e.g. {align:center}**y = mx + c**; define each symbol on the next line.")
        if ("colour" in needs) append("\nColour, sparingly: {color:C62828|text|} red for a warning or mistake, {color:2E7D32|text|} green for a result, {color:1565C0|text|} blue for a definition. Always close with |}.")
        if ("extras" in needs) append("\nAlso: \"- [ ] \" checklist, \"> \" key idea, \"---\" between big parts, *italic*, ++underline++, ~~strike~~ for a wrong idea corrected, [[Note title]] only if the student named it.")
    }

    /** All of it, for the chat: it quotes this so an answer can be saved as a note without losing its look. */
    internal val EDITOR: String = editor()

    /** One rule for the chat, the notes screens and the editor's AI panel: the content decides which styles appear. */
    internal val STYLE_GUIDE = "Use a style only where the content needs it: a table for comparisons, numbered steps for a process, a centred line for a formula, a checklist only for things to do. Short content stays plain; never repeat one layout from note to note."

    private val RULES = "Rules: never draw with text characters (boxes, +---+ lines, arrows made of v or |); use numbered steps, a table or centred lines with →. No code fences, backticks, LaTeX or HTML. " +
        "Write like a student or kind teacher: plain, direct sentences, concrete examples, no filler openers, no summary that repeats the page. Be accurate; if unsure, say so in one line. Reply with the notes only."

    private val SECTION_MENU = "Take only the parts this topic needs, in the order that teaches it best, and leave the rest out: overview, definitions, the idea step by step, derivation, formula sheet, worked example, comparison table, timeline, " +
        "process steps, mnemonic, common mistakes, revision checklist, summary. Quantitative subjects need formulas and worked examples; narrative ones timelines and causes; language ones rules and exceptions; " +
        "descriptive ones classifications and labelled steps; practical ones procedures and observations."

    /** The editor styles this note can use: from the plan's forms when there is one, else from the kind of notes. */
    private fun needs(b: NoteBrief, plan: String?): Set<String> {
        if (plan != null) {
            val p = plan.lowercase()
            return buildSet {
                if ("table" in p) add("table")
                if ("formula" in p || "example" in p) add("formula")
                if ("checklist" in p || "mistake" in p) add("colour")
                if ("checklist" in p || "timeline" in p) add("extras")
            }
        }
        return when (b.template) {
            NoteTemplate.FULL -> ALL_NEEDS
            NoteTemplate.KEY_DETAILS -> emptySet()
            NoteTemplate.SUMMARY -> setOf("formula")
            NoteTemplate.PYQ, NoteTemplate.PRACTICAL -> setOf("table", "formula")
            NoteTemplate.LAST_MINUTE -> setOf("formula", "colour", "extras")
        }
    }

    private fun syllabusPart(s: SyllabusContext): String =
        if (s.isEmpty) "" else "\nThe student's syllabus (stay inside it, in its order and depth):\n${s.describe()}"

    private fun diagramPart(b: NoteBrief): String =
        if (!b.searchDiagrams && !b.drawDiagrams) "Do not mention diagrams or pictures."
        else "Diagrams are on: put two to five lines of the exact form {{diagram: plain English description, ending in the word diagram}} on their own where each picture belongs, each a different picture."

    private fun priority(request: String, hasPhotos: Boolean): String =
        if (request.isBlank()) (if (hasPhotos) "The student typed nothing: work from the attached photo(s) and the defaults below." else "")
        else "The student's own request comes first and overrides every default below, even length, kind and layout: ${request.trim()}"

    private fun scope(b: NoteBrief): String = when (b.template) {
        NoteTemplate.FULL -> "Notes only: no questions and answers, quiz or practice problems unless the student's request asks for them."
        NoteTemplate.PYQ -> "This is a question bank, so questions are the content."
        else -> "Stay inside this kind of notes."
    }

    /**
     * The planning step: a short, cheap call before writing in which the model decides which parts this topic needs and in what form,
     * so a table, comparison, formula sheet or revision list appears only where the topic calls for it.
     */
    fun plan(b: NoteBrief): String = listOf(
        "Plan study notes before they are written. Subject: ${b.subject}.",
        priority(b.request, false),
        "Kind: ${b.template.label}. ${b.template.guide}",
        "Size: ${b.depth.label} (${b.depth.guide}).",
        if (b.template.layout.isEmpty()) SECTION_MENU else "This kind may contain: ${b.template.layout}",
        syllabusPart(b.syllabus).trim(),
        scope(b),
        "Decide what THIS topic needs and nothing more; a small size has few parts.",
        "Reply with the plan only: first \"Type: \" and one word from quantitative, narrative, language, descriptive, practical, mixed; then one line per part in writing order, " +
            "\"Title | what it holds | form\", form being paragraphs, bullets, steps, table, formula, example, timeline or checklist. At most 12 lines.",
    ).filter { it.isNotBlank() }.joinToString("\n")

    /** Reads a plan reply: the lines of the form "Title | what it holds | form" and the topic type. Null when it holds no usable part. */
    fun parsePlan(reply: String): String? {
        val cleaned = reply.replace("```", "").lineSequence().map { it.trim().trimStart('-', '*', ' ').trim() }.filter { it.isNotBlank() }.toList()
        val type = cleaned.firstOrNull { it.startsWith("Type:", ignoreCase = true) }
        val parts = cleaned.filter { it.count { c -> c == '|' } >= 1 }.take(12)
        if (parts.isEmpty()) return null
        return buildList {
            if (type != null) add(type)
            addAll(parts)
        }.joinToString("\n")
    }

    /**
     * Study notes. Only what this request needs is written into the prompt: the editor styles the kind of note can use, the layout of
     * that kind, the syllabus, the diagram line, and either the plan or a short menu. [plan] is the output of [plan] through [parsePlan].
     */
    fun notes(b: NoteBrief, plan: String? = null): String = listOf(
        "Write study notes for a student. Subject: ${b.subject}.",
        priority(b.request, b.hasPhotos),
        "Kind: ${b.template.label}. ${b.template.guide}",
        "Size: ${b.depth.label} (${b.depth.guide}). It is a requirement: do not stop early and do not pad.",
        scope(b),
        if (b.template.layout.isEmpty()) "" else "This kind may contain: ${b.template.layout} Only the student's own request can change that.",
        syllabusPart(b.syllabus).trim(),
        editor(needs(b, plan)),
        if (plan != null) "Write exactly these parts in this order and add nothing else:\n$plan"
        else "Decide silently what this topic needs and write only that. " + (if (b.template.layout.isEmpty()) SECTION_MENU else ""),
        RULES,
        diagramPart(b),
    ).filter { it.isNotBlank() }.joinToString("\n")

    /** A worked solution to homework given as text and/or photos. The answer's shape and length follow the question. */
    fun homework(b: NoteBrief): String = listOf(
        "You are a patient tutor. Subject: ${b.subject}. Solve the homework in ${if (b.hasPhotos) "the attached photo(s)" else "the question below"}.",
        priority(b.request, b.hasPhotos),
        syllabusPart(b.syllabus).trim(),
        "Shape the answer to the question. Calculation: the given data, the formula, the working one numbered step at a time with each result on a centred line, then the final answer in bold with its units. " +
            "Theory or essay: a good exam answer with headings and the key points highlighted. Several questions: one heading each. End with one line on what the question tested. " +
            "If it asks for an assignment, project or report, write a proper one: title block (Submitted by and Date left blank), introduction, numbered sections, conclusion, references to kinds of sources only (never made-up URLs or pages).",
        editor(setOf("table", "formula")),
        RULES,
        diagramPart(b),
    ).filter { it.isNotBlank() }.joinToString("\n")

    /** A request typed in the editor's AI panel, with the text it applies to. */
    fun edit(instruction: String, selection: String): String =
        "Edit part of a student's note. Do exactly what they ask: $instruction\n\nText to work on:\n$selection\n\n$EDITOR\n$STYLE_GUIDE\n$RULES"

    /** A diagram description found in the answer. */
    val diagramLine = Regex("""(?m)^[ \t]*\{\{diagram:\s*(.+?)\s*\}\}[ \t]*$""")

    /** The descriptions the model asked pictures for, in order, without duplicates and at most [limit]. */
    fun diagramRequests(text: String, limit: Int = 3): List<String> =
        diagramLine.findAll(text).map { it.groupValues[1] }.distinct().take(limit).toList()

    /** Removes every diagram line that was not turned into a picture. */
    fun withoutDiagramLines(text: String): String = diagramLine.replace(text, "").replace(Regex("\n{3,}"), "\n\n").trim()
}
