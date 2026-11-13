# Batch 43 plan (owner 2026-11-12)

Source: `TODOList.md` rows 43.1 to 43.12. UI work follows `docs/ui overhaul/` (read `README.md` and `M3E_RESEARCH.md` first) and the design audit docs; every new element uses the Material 3 Expressive components and springs.

## Build order (each step is its own CI build)
1. **Bugs (43.1, 43.2):** note-menu AI with the note linked, hub back buttons, notes generation stopping midway, LastChat onboarding and the 81 remaining LastChat mentions (42.1), Python / workspace text search, action chat first message, Excused chip.
2. **Navigation and popups (43.5, 43.6):** one hub-style page transition, the Home page bleed, one popup open animation and animated resize.
3. **Pickers (43.3, 43.4):** attach popup as Vault navigation, collapsed topic trees, main syllabus page as folders.
4. **Profile (43.7):** Edit profile page, one course-and-year entry, picture with the existing crop tool, profile remade from pictures 4 and 5. Needs pictures (43.12).
5. **Duplicates (43.8):** ask the owner one by one, then unify viewers, editors, crop, audio trimmer.
6. **Styles (43.9, 43.10, 43.11):** glass header, Monochrome, Appearance redesign with Liquid Glass / Pixelated / Nothing dot matrix, one FAB style. Needs pictures 2 and 3.

## Questions still to ask the owner (row 43.8)
Known doubles to list first: the two crop tools (app and AI), the PDF viewer and the reading screen, the image viewer and the image editor, audio player and recorder, the AI request paths still outside the hub (video notes, text extraction), the Settings AI page and the hub settings, the two attendance entry places, the exam importer and the manual exam sheet.

## Pictures
Pictures 1 to 5 of the 2026-11-12 request were not attached; they are to be saved as `docs/ui overhaul/28` onward.

## Code audit of the owner's feature lists (2026-11-13, source-checked, not CI-checked)

**Already in the app (no work):** excused absence (Excused chip, `AttendanceStatus.EXCUSED`); lecture types (`LectureType` in the timetable popup); built-in note templates (`NoteTemplates`, a fixed set; your own saved templates are not built); `[[note]]` links parsed and drawn (no link picker); note version history + restore (AI tools `list_note_versions`, `restore_note_version`); pass mark (`ExamEntity`/subject `pass_mark`); marks and GPA (`get_grades`, `set_exam_marks`); per-subject attendance goal (`attendance_goal`, tool `set_subject_goals`); bulk mark (`bulk_set_attendance`); bulk delete / move notes; duplicate note; Trash viewer and restore (`list_trash`, `restore_from_trash`); syllabus delete / edit / move tools; calendar view tool; flashcards, study goals, events; reminders (lecture, exam, event, low attendance); exports (.ics, CSV, PDF); Pomodoro / stopwatch / breaks; tags stored on notes (`NoteTags`).

**Not found in the code (still to build):** AI tools to add / edit tags, add / delete attachments, reorder syllabus topics, import a syllabus from a file / pasted text, create a study plan, set a reminder on an exam, export / share an AI chat, filter notes; timer tied to a note; quizzes / MCQs; multiple exam dates per subject (theory + practical); timetable overlap warning and bulk add; room filter; `.ics` import; syllabus progress charts and export; auto-complete topics from notes; offline-mode toggle; note body search (verify in global search); the "do without asking" wording (not found in code, so only the category grouping needs checking); per-category tool folders in the assistant tools list (the categories exist in `CATEGORIES`, the UI grouping is to be checked on a device).
