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
