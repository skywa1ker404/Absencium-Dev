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

    /** What the notes editor can draw. The chat quotes it so its answers can be saved as notes without losing their look. */
    internal val EDITOR = """
        Write only with the formatting below. It is what the student's notes editor shows; nothing else renders.
        - Title and headings: a line starting with "# ", "## " or "### ".
        - Emphasis: **bold** for key terms, *italic* for names and foreign words, ==highlight== for the one fact to remember, ++underline++ for a label, ~~strike~~ only for a wrong idea being corrected.
        - Colour a short phrase with {color:C62828|text|} (red: warnings, common mistakes), {color:2E7D32|text|} (green: results, tips), {color:1565C0|text|} (blue: definitions). Use colour sparingly. Every colour or size tag MUST end with a bar and a closing brace, `|}` (never a lone `}`): {color:1565C0|cell division|}.
        - Lists: "- " bullets, "1. " numbered steps, "a) " lettered options, "- [ ] " a checklist of things to revise or do.
        - Table: a header row, a separator row, then rows, e.g. "| Feature | A | B |" then "|---|---|---|". Use a table whenever you compare things, list properties, or show data.
        - Quote or key idea: a line starting with "> ".
        - Divider between big parts: a line with only "---".
        - Centre a line (every formula, key result): start it with {align:center}
        - Link to another note: [[Note title]] only if the student named it.
    """.trimIndent()

    private val RULES = """
        Rules for the answer:
        - Never draw with text characters: no boxes, no +---+ lines, no lone "v" or "|" arrows, no [ boxed ] words. Show a flow as numbered steps, a table, or centred lines joined with arrows such as {align:center}A → B → C.
        - Never show code or markup symbols. No code fences, no backticks, no LaTeX, no dollar signs, no backslash commands, no HTML.
        - Write every formula as clean readable Unicode on its own centred line, for example {align:center}**y = mx + c** or {align:center}A → B → C, using Unicode symbols such as ² ½ √ ± ≤ → Δ. Define each symbol on the next line.
        - Choose the sections this particular topic needs, in the order that teaches it best. Do not reuse one fixed layout, and leave out any section that has nothing real to say.
        - Sound like a good student or a kind teacher wrote it by hand: plain, direct sentences of uneven length, concrete examples, no filler openers ("Certainly", "In conclusion", "It is important to note"), no lists of three adjectives, no summary that repeats the page.
        - Be accurate. If you are unsure of something, say so in one short line instead of guessing.
        - Reply with the notes only: no greeting, no "here are your notes", no closing remark.
    """.trimIndent()

    private val SECTION_MENU = """
        Parts a note can have (a menu, not a checklist: take only what this topic truly needs, rename them to suit it, add your own if needed, and drop the rest):
        a short overview; why it matters; definitions of key terms; the main idea step by step; a derivation or proof; a formula sheet with each symbol defined;
        a worked example with numbers; a comparison table; a timeline; causes and effects; a process as numbered steps; types as a table; a mnemonic;
        real-life uses; common mistakes; a quick-revision checklist; a short summary.
        Let the subject decide the shape: quantitative subjects need formulas, units and worked examples; narrative subjects need timelines, causes and consequences;
        language subjects need rules, examples and exception tables; descriptive subjects need classifications, comparisons and labelled steps; practical subjects need procedures and observations.
    """.trimIndent()

    private fun syllabusPart(s: SyllabusContext): String =
        if (s.isEmpty) "" else "\nThe student's syllabus (stay inside it and follow its depth, terms and order; skip what it does not cover):\n${s.describe()}\n"

    private fun diagramPart(b: NoteBrief): String = when {
        !b.searchDiagrams && !b.drawDiagrams -> "Do not mention diagrams or pictures."
        else ->
            "The student switched diagrams on, so include them: put between two and five lines of the exact form {{diagram: a short description of the picture}} " +
                "on their own, each where the picture belongs (for example right after the paragraph it illustrates). " +
                "Describe each picture in plain English words that would find or draw it, using the plain, common English name of the subject, then the word diagram. Use different pictures, not repeats."
    }

    private fun priority(request: String): String =
        if (request.isBlank()) "The student typed nothing, so work from the attached photo(s) and the defaults below."
        else "What the student typed is the first priority and overrides every default below, including length, kind and layout, even if it asks for a format different from the one chosen:\n\"\"\"\n${request.trim()}\n\"\"\""

    /** What the kind of notes is allowed to contain; for the full note this is what keeps questions and answers out of it. */
    private fun scope(b: NoteBrief): String = when (b.template) {
        NoteTemplate.FULL ->
            "This is the complete note on the topic and nothing else. Do not add questions and answers, previous-year questions, a quiz or practice problems unless the student's own text asks for them."
        NoteTemplate.PYQ -> "This is a question bank, so questions are the content."
        else -> "Stay inside this kind of notes; do not add the content of other kinds (no questions and answers unless this kind is about questions)."
    }

    /**
     * The planning step. A short, cheap call made before writing: the model looks at this topic and decides which parts it needs
     * and in what form. The writing call then follows that plan, so a table, comparison, formula sheet, revision list or
     * list of mistakes appears only where the topic calls for it, instead of every note getting every part.
     */
    fun plan(b: NoteBrief): String = """
        You are planning study notes before they are written. Subject: ${b.subject}.

        ${priority(b.request)}

        Kind of notes: ${b.template.label}. ${b.template.guide}
        Size: ${b.depth.label} (${b.depth.guide}).
        ${if (b.template.layout.isEmpty()) SECTION_MENU else "This kind may contain: ${b.template.layout}"}
        ${syllabusPart(b.syllabus)}
        ${scope(b)}

        Decide what THIS topic needs, as a teacher would before writing, and nothing more. A table only if things are really compared or listed with properties, a formula sheet only if there are formulas,
        a worked example only if the topic is solved by working, a timeline only if order in time matters, common mistakes or a revision list only if there are real ones worth the space, a mnemonic only if it helps recall.
        Fit the parts to the size: a small size has few parts.

        Reply with the plan only, in this exact form and nothing else:
        Type: one word from quantitative, narrative, language, descriptive, practical, mixed
        Then one line per part, in writing order: Title | what it holds | form
        where form is one of paragraphs, bullets, steps, table, formula, example, timeline, checklist.
        At most 12 lines.
    """.trimIndent()

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

    /** Study notes. [plan] is the output of [plan] (through [parsePlan]); without one the model is told to decide the parts itself. */
    fun notes(b: NoteBrief, plan: String? = null): String = """
        You are an expert teacher writing study notes for a student. Subject: ${b.subject}.

        ${priority(b.request)}

        Defaults, used only where the student's text says nothing different:
        Kind of notes: ${b.template.label}. ${b.template.guide}
        Size: ${b.depth.label} (${b.depth.guide}). This size is a requirement, not a hint: do not stop early, and do not pad to reach it either.
        ${scope(b)}
        ${if (b.template.layout.isEmpty()) "" else "This kind of notes may contain: ${b.template.layout} Only the student's own typed request can change that."}
        ${syllabusPart(b.syllabus)}
        $EDITOR

        ${if (plan != null) planPart(plan) else decidePart(b)}

        $RULES
        ${diagramPart(b)}
    """.trimIndent()

    private fun planPart(plan: String) = """
        The plan for this topic (made by looking at what the topic needs). Write exactly these parts in this order. Do not add a table, comparison, worked example, common-mistakes list, revision list, questions or any other part that is not in the plan, and do not leave a planned part out:
        $plan
    """.trimIndent()

    private fun decidePart(b: NoteBrief) = """
        Before writing, decide silently what this topic needs and write only that. Do not add a table, comparison, formula sheet, worked example, common-mistakes list, revision list or any other part unless this topic really calls for it.
        ${if (b.template.layout.isEmpty()) SECTION_MENU else ""}
        $STYLE_GUIDE
    """.trimIndent()

    /** A worked solution to homework given as text and/or photos. The answer's shape and length follow the question. */
    fun homework(b: NoteBrief): String = """
        You are a patient tutor. Subject: ${b.subject}.
        Solve the homework in ${if (b.hasPhotos) "the attached photo(s)" else "the question below"}.

        ${priority(b.request)}
        ${syllabusPart(b.syllabus)}
        Work out what the question asks for, then answer in the shape and length it needs: ${SOLUTION}
        $ASSIGNMENT

        $EDITOR

        $STYLE_GUIDE

        $RULES
        ${diagramPart(b)}
    """.trimIndent()

    /**
     * How to choose among the editor's styles. One rule for the chat, Generate Notes, homework and the editor's AI panel: the content
     * decides which styles appear, so no two notes need look alike and nothing is added only to show a feature.
     */
    internal val STYLE_GUIDE = """
        How to use the styles: let the content decide. Use a style only where it helps the reader and leave out the rest. Never add a table, colour, highlight, checklist or centred line just to show the editor can do it, and never repeat one layout from note to note.
        - A table when things are compared, classified or listed with properties. Example: | Term | Meaning |, then |---|---|, then rows.
        - Numbered steps for a process or working, "a) " letters for options or parts, bullets for properties.
        - A formula or key result on its own centred line, like {align:center}a + b = c, only when there is one.
        - ==highlight== for the one fact to remember in a section, **bold** for each key term the first time it appears, ++underline++ for sub-labels, colour only for a real definition ({color:1565C0|..|}), warning ({color:C62828|..|}) or result ({color:2E7D32|..|}).
        - A "- [ ] " checklist only when there is something to revise or do; "> " only for a rule or idea that must not be forgotten; a line of --- only between big parts.
        - Quantitative topics want formulas, units and worked examples; narrative topics timelines, causes and consequences; language topics rules, examples and exception tables; descriptive topics classifications and labelled steps; practical topics procedures and observations.
        - Short content stays simple: a few lines need no headings, tables or colours.
    """.trimIndent()

    private val SOLUTION = """
        Shape the answer to the question. For a calculation: the question restated, the given data in a small table, the formula used, the working one numbered step at a time with each result on a centred line, then the final answer highlighted and in bold with its units.
        For an essay, short answer or theory question: write it as a good exam answer, using headings, a table where things are compared, and the key points highlighted.
        For several questions, answer each under its own heading. Finish with one line on the idea the question was really testing.
    """.trimIndent()

    private val ASSIGNMENT = """
        If the question asks for an assignment, project, report or essay, write the answer as a proper handed-in assignment instead: a title block (title, subject, "Submitted by" and "Date" lines left blank to fill in), an introduction, numbered headed sections that answer the question in order,
        tables and worked examples where they belong, a conclusion in the student's own voice, and a short list of references to the kinds of sources a student would cite (textbook chapters, never made-up URLs or page numbers).
    """.trimIndent()

    /** A request typed in the editor's AI panel, with the text it applies to. */
    fun edit(instruction: String, selection: String): String = """
        You are helping a student edit their note.
        What the student asked (follow it exactly): $instruction

        Text to work on:
        $selection

        $EDITOR

        $STYLE_GUIDE

        $RULES
    """.trimIndent()

    /** A diagram description found in the answer. */
    val diagramLine = Regex("""(?m)^[ \t]*\{\{diagram:\s*(.+?)\s*\}\}[ \t]*$""")

    /** The descriptions the model asked pictures for, in order, without duplicates and at most [limit]. */
    fun diagramRequests(text: String, limit: Int = 3): List<String> =
        diagramLine.findAll(text).map { it.groupValues[1] }.distinct().take(limit).toList()

    /** Removes every diagram line that was not turned into a picture. */
    fun withoutDiagramLines(text: String): String = diagramLine.replace(text, "").replace(Regex("\n{3,}"), "\n\n").trim()
}
