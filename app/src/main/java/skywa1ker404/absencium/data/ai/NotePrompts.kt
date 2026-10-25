package skywa1ker404.absencium.data.ai

/** How long the generated notes should be. */
enum class NoteLength(val label: String, val guide: String, val maxTokens: Int) {
    MODERATE("Moderate", "about 1 page, 500 to 800 words: the essentials, tight and easy to revise", 6_000),
    LENGTHY("Lengthy", "about 3 pages, 1,500 to 2,500 words: every important point with explanations and examples", 10_000),
    IN_DEPTH("In Depth", "6 pages or more, at least 3,500 words: complete coverage with reasoning, derivations, several worked examples, edge cases and exam questions", 16_000),
}

/** The kind of notes wanted. */
enum class NoteTemplate(val label: String, val guide: String) {
    FULL("Full note", "A complete, well-organised note on the topic, with the sections the topic needs."),
    KEY_DETAILS("Key details, line by line", "One short, self-contained fact per line, grouped under small headings. No paragraphs."),
    SUMMARY("Summary", "A compact summary a student can read in two minutes, with the few things that must be remembered highlighted."),
    PYQ("Previous year questions", "Questions of the kind this exam usually asks, grouped by marks, each with a model answer and the point where marks are earned."),
    LAST_MINUTE("Last-minute exam preparation", "Rapid revision: formulas, definitions, diagrams to practise, mnemonics, common traps, and a checklist to tick off."),
    PRACTICAL("Practical observation notes", "Aim, apparatus, theory, procedure, an observation table to fill in, calculation, result, precautions and viva questions."),
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

/** Everything chosen in the Generate AI Notes or Solve Homework screen. */
data class NoteBrief(
    val request: String,
    val subject: String,
    val length: NoteLength = NoteLength.MODERATE,
    val template: NoteTemplate = NoteTemplate.FULL,
    val syllabus: SyllabusContext = SyllabusContext(),
    val searchDiagrams: Boolean = false,
    val drawDiagrams: Boolean = false,
    val tablesAndGraphs: Boolean = true,
    val pyq: Boolean = false,
    val assignment: Boolean = false,
    val hasPhotos: Boolean = false,
)

/**
 * The instructions sent with every notes or homework request. They teach the model exactly what the notes editor can
 * draw, so an answer uses headings, tables, lists, highlights and centred formulas instead of raw markup, and they ask it
 * to pick the sections the topic needs rather than filling one fixed template. What the student typed always wins.
 */
object NotePrompts {

    private val EDITOR = """
        Write only with the formatting below. It is what the student's notes editor shows; nothing else renders.
        - Title and headings: a line starting with "# ", "## " or "### ".
        - Emphasis: **bold** for key terms, *italic* for names and foreign words, ==highlight== for the one fact to remember, ++underline++ for a label, ~~strike~~ only for a wrong idea being corrected.
        - Colour a short phrase with {color:C62828|text|} (red: warnings, common mistakes), {color:2E7D32|text|} (green: results, tips), {color:1565C0|text|} (blue: definitions). Use colour sparingly.
        - Lists: "- " bullets, "1. " numbered steps, "a) " lettered options, "- [ ] " a checklist of things to revise or do.
        - Table: a header row, a separator row, then rows, e.g. "| Feature | A | B |" then "|---|---|---|". Use a table whenever you compare things, list properties, or show data.
        - Quote or key idea: a line starting with "> ".
        - Divider between big parts: a line with only "---".
        - Centre a line (every formula, key result): start it with {align:center}
        - Link to another note: [[Note title]] only if the student named it.
    """.trimIndent()

    private val RULES = """
        Rules for the answer:
        - Never show code or markup symbols. No code fences, no backticks, no LaTeX, no dollar signs, no backslash commands, no HTML.
        - Write every formula as clean readable Unicode on its own centred line, for example {align:center}**E = mc²**, {align:center}x = (-b ± √(b² - 4ac)) / 2a. Define each symbol on the next line.
        - Choose the sections this particular topic needs, in the order that teaches it best. Do not reuse one fixed layout, and leave out any section that has nothing real to say.
        - Sound like a good student or a kind teacher wrote it by hand: plain, direct sentences of uneven length, concrete examples, no filler openers ("Certainly", "In conclusion", "It is important to note"), no lists of three adjectives, no summary that repeats the page.
        - Be accurate. If you are unsure of something, say so in one short line instead of guessing.
        - Reply with the notes only: no greeting, no "here are your notes", no closing remark.
    """.trimIndent()

    private val SECTION_MENU = """
        Sections to choose from (use the ones that fit, rename them to suit the topic, add your own if the topic needs one):
        a short overview; why it matters; definitions of key terms; the main idea step by step; a derivation or proof; a formula sheet with each symbol defined;
        a worked example with numbers; a comparison table; a timeline; causes and effects; a process as numbered steps; types as a table; a mnemonic;
        real-life uses; common mistakes; likely exam questions; a quick-revision checklist; a short summary.
        Science and maths lean on formulas, units and worked examples. History leans on timelines, causes and consequences. Languages lean on rules and exception tables.
        Biology leans on labelled steps and comparison tables.
    """.trimIndent()

    private fun syllabusPart(s: SyllabusContext): String =
        if (s.isEmpty) "" else "\nThe student's syllabus (stay inside it and follow its depth, terms and order; skip what it does not cover):\n${s.describe()}\n"

    private fun diagramPart(b: NoteBrief): String = when {
        !b.searchDiagrams && !b.drawDiagrams -> "Do not mention diagrams or pictures."
        else ->
            "The student switched diagrams on, so include them: put between two and five lines of the exact form {{diagram: a short description of the picture}} " +
                "on their own, each where the picture belongs (for example right after the paragraph it illustrates), such as {{diagram: labelled cross-section of a human heart}}. " +
                "Describe each picture in plain words that would find or draw it: the subject, and the word diagram, figure or graph. Use different pictures, not repeats."
    }

    private fun extras(b: NoteBrief): String = buildList {
        if (b.tablesAndGraphs) add("Add comparison tables and, where numbers or trends matter, a small data table that could be plotted, wherever they teach better than prose.")
        else add("Do not use tables.")
        if (b.pyq) add("End with a heading \"Previous year questions\": the questions of this kind that exams usually ask on this topic, with the marks, each followed by a short model answer. Do not invent exam years or paper names.")
    }.joinToString("\n")

    private fun priority(request: String): String =
        if (request.isBlank()) "The student typed nothing, so work from the attached photo(s) and the defaults below."
        else "What the student typed is the first priority and overrides every default below, including length, template and layout, even if it asks for a format different from the one chosen:\n\"\"\"\n${request.trim()}\n\"\"\""

    /** Study notes. */
    fun notes(b: NoteBrief): String = """
        You are an expert teacher writing study notes for a student. Subject: ${b.subject}.

        ${priority(b.request)}

        Defaults, used only where the student's text says nothing different:
        Length: ${b.length.label} (${b.length.guide}). This length is a requirement, not a hint: do not stop early, do not summarise where the student asked for depth, and keep going until the topic is fully covered at that size.
        Kind of notes: ${b.template.label}. ${b.template.guide}
        ${syllabusPart(b.syllabus)}
        $EDITOR

        $SECTION_MENU

        $FEATURES

        ${extras(b)}

        $RULES
        ${diagramPart(b)}
    """.trimIndent()

    /** A worked solution to homework given as text and/or photos. */
    fun homework(b: NoteBrief): String = """
        You are a patient tutor. Subject: ${b.subject}.
        Solve the homework in ${if (b.hasPhotos) "the attached photo(s)" else "the question below"}.

        ${priority(b.request)}
        ${syllabusPart(b.syllabus)}
        ${if (b.assignment) ASSIGNMENT else SOLUTION}

        Length: ${b.length.label} (${b.length.guide}).
        $EDITOR

        $FEATURES

        ${extras(b)}

        $RULES
        ${diagramPart(b)}
    """.trimIndent()

    /** The editor's features the answer must really use, with a tiny example of each. */
    private val FEATURES = """
        Use the editor's features visibly, wherever they fit the content (this is required, not optional):
        - At least one table when anything is compared, classified or listed with properties. Example: | Term | Meaning |, then |---|---|, then rows.
        - Key facts to remember in ==highlight==; headings of definitions in {color:1565C0|blue|}; warnings and common mistakes in {color:C62828|red|}; results and tips in {color:2E7D32|green|}.
        - Every formula and key result on its own centred line: {align:center}F = ma
        - Numbered steps for any process or working, "a) " letters for options or parts, bullets for properties.
        - A "- [ ] " revision checklist near the end.
        - "> " for the one idea or rule that must not be forgotten, and a line of --- between the big parts.
        - **bold** for each key term the first time it appears, ++underline++ for sub-labels.
    """.trimIndent()

    private val SOLUTION = """
        Shape the answer to the question. For a calculation: the question restated, the given data in a small table, the formula used, the working one numbered step at a time with each result on a centred line, then the final answer highlighted and in bold with its units.
        For an essay, short answer or theory question: write it as a good exam answer, using headings, a table where things are compared, and the key points highlighted.
        For several questions, answer each under its own heading. Finish with one line on the idea the question was really testing.
    """.trimIndent()

    private val ASSIGNMENT = """
        Write the answer as a proper handed-in assignment: a title block (title, subject, "Submitted by" and "Date" lines left blank to fill in), an introduction, numbered headed sections that answer the question in order,
        tables and worked examples where they belong, a conclusion in the student's own voice, and a short list of references to the kinds of sources a student would cite (textbook chapters, never made-up URLs or page numbers).
    """.trimIndent()

    /** A request typed in the editor's AI panel, with the text it applies to. */
    fun edit(instruction: String, selection: String): String = """
        You are helping a student edit their note.
        What the student asked (follow it exactly): $instruction

        Text to work on:
        $selection

        $EDITOR

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
