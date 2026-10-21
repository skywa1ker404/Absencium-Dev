package skywa1ker404.absencium.ui.vault.markdown

/** Starting points for a note. Plain Markdown, so they work in the editor, the preview and every export. */
data class NoteTemplate(val name: String, val description: String, val body: String)

object NoteTemplates {
    val all: List<NoteTemplate> = listOf(
        NoteTemplate(
            "Lecture notes", "Topic, key points, examples, questions",
            "## Topic\n\n## Key points\n- \n\n## Examples\n\n## Questions to ask\n- [ ] \n",
        ),
        NoteTemplate(
            "Cornell notes", "Cues, notes and a summary",
            "## Cues\n- \n\n## Notes\n\n## Summary\n",
        ),
        NoteTemplate(
            "Revision sheet", "Definitions, formulas, mistakes to avoid",
            "## Definitions\n- \n\n## Formulas\n\n```\n\n```\n\n## Common mistakes\n- \n\n## Check before the exam\n- [ ] \n",
        ),
        NoteTemplate(
            "Problem practice", "Question, working, answer, what I learned",
            "## Question\n\n## Working\n\n## Answer\n\n## What I learned\n",
        ),
        NoteTemplate(
            "To-do list", "A simple checklist",
            "## To do\n- [ ] \n- [ ] \n- [ ] \n",
        ),
    )
}
