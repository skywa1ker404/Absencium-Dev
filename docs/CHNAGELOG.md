working on it

## 2026-10-28 (development, not device-tested)
- Fixed: popups jumping up and leaving a gap when the keyboard opens on a low text field (the window now resizes for the keyboard).
- Fixed: applying a style again, or after other styles, left raw markers in the note; styles now toggle off cleanly, replace each other where they should, and a line keeps one list, quote or heading prefix.
- Changed: subject folder tabs are a floating pill at the bottom with a floating action button; Subjects and Syllabus add actions are floating buttons; the AI Hub button is a squircle; the topic filter opens from the subject name.
- Changed: Home, Today, Trash and Subjects redesigned with tinted cards.

## 2026-10-29 (development, not device-tested)
- Changed: Home has a closed-by-default drawer for the subjects to watch under the bunk buffer card, a pinned Subjects button instead of the AI button beside the pill, and an Attendance popup (Present / Absent today and notes collected).
- Changed: the subject folder uses the main navigation pill, which shrinks while scrolling, with a button that slides away; the list runs under the navigation bar.
- Fixed: the Import an outline dialog buttons; the chosen day in the calendar now has a ring so it differs from a holiday; note previews no longer show leftover style markers.

## 2026-10-29, later (development, not device-tested)
- Removed: the pinned Subjects button beside the pill on Home.
- Fixed: "Add today's notes" was cut off to "Add today's"; the buttons now wrap.
- Changed: the extended buttons (subject folder tabs, Syllabus) fold to just their icon while you scroll down and unfold on scrolling up; the pill no longer shrinks on a page that cannot scroll.
- Fixed: a colour or size on a whole heading or list line showed the hidden `## ` or `- `; styles go after the line's prefix, one pair per line over several lines. The character count counts what is shown.
- Changed: the note's topic ("Uncategorized") is a dropdown; one solid topic menu with indented rows and a tick is used in the editor and the folder.
- Added: Edit (rename and change the date) when one note is selected; the same selection on Study material (file under a topic, delete); list / card mode on Notes and Study material.
- Changed: importing an outline reads only the chosen subject's syllabus (the AI is told the subject), shows a list to choose from, and never adds what is already there.
- Changed: the Vault shows subject folders as equal gradient tiles with an icon chip.
- Changed: empty pages spin slowly and morph every 5 seconds; study material opens inside the app; reading a document with the AI shows progress with Cancel and saves nothing if the AI cannot be reached; the Home chevron is larger.
- Fixed: PDFs are read as small page pictures in batches (they timed out as one big upload); empty Home, Vault and folder show the morphing badge, now moving like the loading indicator; the attendance arrow no longer covers a pill.
- Changed: PDFs are uploaded whole with a wavy progress line; Present / Absent / Total and the notes buttons sit side by side; the calendar entry type buttons have no emojis.
- Appearance: theme mode, theme and presets are picture tiles with a one-line helper; Liquid Glass has its own preview.
- AI: failures show a bar on every screen with Copy; HTTP 500/502/503/504 are retried; the provider's reason is logged; Groq PDF and picture-count limits are respected; upload progress redraws less.
- Fixed: AI answers are formatted instead of raw Markdown; the ask bar is never behind the keyboard; blocks added from inside a code block go below it.
- Attendance: hatched days off, Present / Absent / Total tiles, last-6-months capsule chart.
- Syllabus: calmer layout; topic side panel in folders; import from one document and/or up to five pictures.
- Attendance: scrollable month chart, swipe between months, no lines through glass day beads. Timetable: week strip and a highlighted current lecture.
- Themes: Sage & Oxblood and Orchid & Ember presets. AI Hub: a chat.

## 2026-10-31 (development, not device-tested)
- Fixed: the syllabus import builds a proper nested tree and leaves out course details; rows are smaller; tapping a unit shows only that unit.
- Fixed: "The provider's reply was empty." (Gemini thinking tokens used up the reply; empty replies are retried and explained).
- Fixed: an AI error shows once, at the bottom, for 5 seconds, and swipes away left or right.
- Fixed: the date picker in Edit entry, the absent-date choice, the "Kind of notes" layouts, image / table / style markers breaking when typing next to them, a random shape on empty pages, the folder pill on empty pages, the drafts pull-down inside a folder (that subject only), the shadow under the main button, rotation reset in the photo editor.
- Changed: the AI Hub is now **Absencium AI**, a chat with a sidebar of chats, a model chooser, pin / rename / delete / share, edit and copy on your messages, and (only where you allow it) suggestions that change your timetable, exams, holidays, attendance, subjects and notes after you tap Apply.
- Changed: every AI provider keeps its own API key; AI & Integrations lists your providers, tests a connection and switches in one tap.
- Changed: sharing a note offers PDF (with a Themed PDF toggle) and a greyed-out Sharewave.
