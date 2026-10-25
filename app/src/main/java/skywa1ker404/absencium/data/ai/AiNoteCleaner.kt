package skywa1ker404.absencium.data.ai

/**
 * Tidies what a model wrote so it reads well in the notes editor and never shows markup symbols. It removes code fences
 * and backticks, turns LaTeX maths into plain Unicode (x², ½, √, π, ≤ ...), and maps list and heading marks onto the
 * ones the editor draws. Pure text in, text out.
 */
object AiNoteCleaner {

    private val symbols = mapOf(
        "times" to "×", "cdot" to "·", "div" to "÷", "pm" to "±", "mp" to "∓", "leq" to "≤", "le" to "≤", "geq" to "≥", "ge" to "≥",
        "neq" to "≠", "ne" to "≠", "approx" to "≈", "equiv" to "≡", "propto" to "∝", "infty" to "∞", "to" to "→", "rightarrow" to "→",
        "leftarrow" to "←", "Rightarrow" to "⇒", "Leftarrow" to "⇐", "leftrightarrow" to "↔", "Leftrightarrow" to "⇔", "rightleftharpoons" to "⇌",
        "sum" to "∑", "prod" to "∏", "int" to "∫", "partial" to "∂", "nabla" to "∇", "in" to "∈", "subset" to "⊂", "cup" to "∪", "cap" to "∩",
        "forall" to "∀", "exists" to "∃", "therefore" to "∴", "angle" to "∠", "perp" to "⊥", "parallel" to "∥", "circ" to "°", "degree" to "°",
        "alpha" to "α", "beta" to "β", "gamma" to "γ", "delta" to "δ", "epsilon" to "ε", "varepsilon" to "ε", "zeta" to "ζ", "eta" to "η",
        "theta" to "θ", "lambda" to "λ", "mu" to "μ", "nu" to "ν", "xi" to "ξ", "pi" to "π", "rho" to "ρ", "sigma" to "σ", "tau" to "τ",
        "phi" to "φ", "varphi" to "φ", "chi" to "χ", "psi" to "ψ", "omega" to "ω", "Gamma" to "Γ", "Delta" to "Δ", "Theta" to "Θ",
        "Lambda" to "Λ", "Pi" to "Π", "Sigma" to "Σ", "Phi" to "Φ", "Psi" to "Ψ", "Omega" to "Ω", "ldots" to "…", "dots" to "…", "cdots" to "…",
        "hbar" to "ħ", "ell" to "ℓ", "prime" to "′", "quad" to "  ", "qquad" to "  ", "lt" to "<", "gt" to ">",
    )

    private val superscripts = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴', '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
        '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾', 'n' to 'ⁿ', 'i' to 'ⁱ', 'x' to 'ˣ',
    )
    private val subscripts = mapOf(
        '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄', '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
        '+' to '₊', '-' to '₋', '=' to '₌', '(' to '₍', ')' to '₎', 'a' to 'ₐ', 'e' to 'ₑ', 'o' to 'ₒ', 'x' to 'ₓ', 'n' to 'ₙ', 'i' to 'ᵢ',
    )

    private val display = Regex("""\$\$(.+?)\$\$|\\\[(.+?)\\\]""", RegexOption.DOT_MATCHES_ALL)
    private val inline = Regex("""\\\((.+?)\\\)|(?<![\\\w$])\$([^$\n]+?)\$(?![\w$])""")

    fun clean(raw: String): String {
        var text = raw.replace("\r\n", "\n").trim()
        text = text.replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
        text = stripFences(text)
        text = display.replace(text) { m -> "\n{align:center}" + math((m.groups[1] ?: m.groups[2])!!.value) + "\n" }
        text = inline.replace(text) { m -> math((m.groups[1] ?: m.groups[2])!!.value) }
        // Maths written without dollar signs but still in LaTeX.
        if (text.contains('\\')) text = latexLeftovers(text)
        text = text.lines().joinToString("\n") { tidyLine(it) }
        text = text.replace("`", "")
        return text.replace(Regex("\n{3,}"), "\n\n").trim()
    }

    private fun stripFences(text: String): String =
        text.lines().filterNot { it.trim().startsWith("```") }.joinToString("\n")

    private fun tidyLine(line: String): String {
        val trimmedStart = line.trimStart()
        val indent = line.length - trimmedStart.length
        val pad = line.substring(0, indent)
        return when {
            // Box drawings made of text characters render as junk; the AI is told not to, and what slips through is cleaned up.
            Regex("^\\+[-=+]{4,}\\+?$").matches(trimmedStart.trim()) -> ""
            Regex("^[vV↓|]$").matches(trimmedStart.trim()) -> "{align:center}↓"
            Regex("^\\[\\s*([^\\[\\]]{2,80}?)\\s*\\]$").matches(trimmedStart.trim()) ->
                "{align:center}**" + trimmedStart.trim().removePrefix("[").removeSuffix("]").trim() + "**"
            Regex("^#{4,}\\s").containsMatchIn(trimmedStart) -> "### " + trimmedStart.trimStart('#').trim()
            Regex("^[*+•]\\s+").containsMatchIn(trimmedStart) && !trimmedStart.startsWith("**") -> pad + "- " + trimmedStart.drop(1).trim()
            Regex("^-{3,}$|^\\*{3,}$|^_{3,}$").matches(trimmedStart.trim()) -> "---"
            else -> line.trimEnd()
        }
    }

    private fun latexLeftovers(text: String): String {
        // Only touch lines that still hold a backslash command, so ordinary text with a stray backslash is left alone.
        return text.lines().joinToString("\n") { line ->
            if (Regex("""\\[A-Za-z]+""").containsMatchIn(line)) math(line) else line
        }
    }

    /** One piece of LaTeX maths as plain Unicode text. */
    fun math(latex: String): String {
        var s = latex.trim()
        s = s.replace(Regex("""\\(left|right|big|Big|bigg|Bigg|displaystyle|operatorname)(?![A-Za-z])\s?|\\[,;!:]"""), "")
        s = s.replace(Regex("""\\(text|textbf|textit|mathrm|mathbf)\{([^{}]*)\}"""), "$2")
        repeat(4) {
            s = s.replace(Regex("""\\[dt]?frac\{([^{}]*)\}\{([^{}]*)\}""")) { m -> fraction(math(m.groupValues[1]), math(m.groupValues[2])) }
            s = s.replace(Regex("""\\sqrt\[(\w+)\]\{([^{}]*)\}""")) { m -> root(m.groupValues[1]) + wrap(math(m.groupValues[2])) }
            s = s.replace(Regex("""\\sqrt\{([^{}]*)\}""")) { m -> "√" + wrap(math(m.groupValues[1])) }
            s = s.replace(Regex("""\\sqrt\s*(\w)""")) { m -> "√" + m.groupValues[1] }
        }
        s = s.replace(Regex("""\\([A-Za-z]+)""")) { m -> symbols[m.groupValues[1]] ?: m.groupValues[1] }
        s = s.replace(Regex("""\^\{([^{}]*)\}""")) { m -> script(m.groupValues[1], superscripts, "^") }
        s = s.replace(Regex("""\^([0-9nix+-])""")) { m -> script(m.groupValues[1], superscripts, "^") }
        s = s.replace(Regex("""_\{([^{}]*)\}""")) { m -> script(m.groupValues[1], subscripts, "_") }
        s = s.replace(Regex("""_([0-9aeoxni])""")) { m -> script(m.groupValues[1], subscripts, "_") }
        s = s.replace("\\\\", "\n").replace("\\%", "%").replace("\\&", "&").replace("\\_", "_").replace("{", "").replace("}", "")
        return s.replace(Regex("[ \t]{2,}"), " ").trim()
    }

    private fun script(body: String, map: Map<Char, Char>, mark: String): String =
        if (body.isNotEmpty() && body.all { it in map }) body.map { map.getValue(it) }.joinToString("") else "$mark($body)"

    private fun wrap(s: String): String = if (s.length <= 1 || s.all { it.isLetterOrDigit() }) s else "($s)"

    private fun fraction(top: String, bottom: String): String {
        val simple = mapOf("1/2" to "½", "1/3" to "⅓", "2/3" to "⅔", "1/4" to "¼", "3/4" to "¾", "1/5" to "⅕", "1/8" to "⅛")
        simple["$top/$bottom"]?.let { return it }
        return "${wrap(top)}/${wrap(bottom)}"
    }

    private fun root(index: String): String = when (index) {
        "3" -> "∛"
        "4" -> "∜"
        else -> script(index, superscripts, "^") + "√"
    }
}
