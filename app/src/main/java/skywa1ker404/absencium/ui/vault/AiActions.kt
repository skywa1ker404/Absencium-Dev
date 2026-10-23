package skywa1ker404.absencium.ui.vault

import androidx.compose.ui.graphics.vector.ImageVector
import skywa1ker404.absencium.ui.icons.AppIcons

/**
 * What the editor's AI button can do to the text. [minChars] is how much text there has to be: a summary of two words or a
 * "shorten" of a short line gives nothing, so those actions wait until the selection (or the note, when nothing is selected)
 * is long enough. [replaces] says whether the answer normally takes the place of the text (an edit) or goes below it (new material).
 */
enum class AiAction(
    val label: String,
    val icon: () -> ImageVector,
    val minChars: Int,
    val replaces: Boolean,
    private val instruction: String,
) {
    SUMMARISE("Summarise", { AppIcons.Summarize }, 60, false,
        "Summarise the text below in a short paragraph, then give the three to five most important points as a bullet list."),
    SHORTEN("Shorten", { AppIcons.Shorten }, 40, true,
        "Rewrite the text below so it is clearly shorter, about half its length, keeping every important fact and the same language."),
    POLISH("Polish", { AppIcons.Sparkle }, 20, true,
        "Improve the wording of the text below so it reads smoothly and clearly. Keep the meaning, the language and about the same length."),
    GRAMMAR("Fix grammar", { AppIcons.Spellcheck }, 20, true,
        "Correct the spelling, grammar and punctuation of the text below. Change nothing else and keep the same length and language."),
    EXTEND("Extend", { AppIcons.Expand }, 10, true,
        "Expand the text below with more detail and explanation, about twice as long, staying on the same topic and in the same language."),
    CONTINUE("Continue", { AppIcons.PenLine }, 0, false,
        "Continue writing from where the text below stops, in the same style and language, for one or two paragraphs. Do not repeat the text."),
    EXPLAIN("Explain simply", { AppIcons.Lightbulb }, 30, false,
        "Explain the text below in simple words for a student who is meeting the idea for the first time. Use one short example."),
    KEY_POINTS("Key points", { AppIcons.ListBulleted }, 60, false,
        "Pull the key points out of the text below as a short bullet list, plus a list of the important terms with one-line meanings."),
    TODOS("To-do list", { AppIcons.Tasks }, 30, false,
        "Turn the text below into a checklist of actions, one per line, each written as \"- [ ] action\". Add nothing that is not in the text."),
    QUESTIONS("Quiz me", { AppIcons.Quiz }, 60, false,
        "Write five review questions about the text below, each followed by a short answer."),
    TRANSLATE("Translate", { AppIcons.Translate }, 2, true,
        "Translate the text below into English. If it is already English, translate it into Hindi.");

    /** True when [length] characters are not enough for this action. */
    fun tooShort(length: Int): Boolean = length < minChars

    /** The full request sent to the model. Markdown formatting is kept so the answer drops straight into the note. */
    fun request(text: String): String =
        "$instruction\nAnswer with the result only, no introduction. Use plain Markdown (headings with #, lists with -, **bold**) only where it helps.\n\nText:\n$text"

    companion object {
        /** A request typed by the user. */
        fun custom(instruction: String, text: String): String =
            "Follow this instruction for the note text below: ${instruction.trim()}\n" +
                "Answer with the result only, no introduction. Use plain Markdown (headings with #, lists with -, **bold**) only where it helps." +
                (if (text.isBlank()) "" else "\n\nText:\n$text")

        /** The part of a long note sent to the model: the whole text, or for continuing, only the end of it. */
        fun excerpt(text: String, forContinue: Boolean): String {
            val limit = if (forContinue) 1_500 else 24_000
            return if (text.length <= limit) text else text.takeLast(limit)
        }
    }
}

/** One finished AI request, kept while the note is open so a result can be brought back. */
data class AiHistoryItem(val title: String, val result: String, val replaces: Boolean)
