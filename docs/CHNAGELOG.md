working on it

## 2026-10-28 (development)
- Fixed: popups jumping up and leaving a gap when the keyboard opens on a low text field (the window now resizes for the keyboard).
- Fixed: applying a style again, or after other styles, left raw markers in the note; styles now toggle off cleanly, replace each other where they should, and a line keeps one list, quote or heading prefix.
- Changed: subject folder tabs are a floating pill at the bottom with a floating action button; Subjects and Syllabus add actions are floating buttons; the AI Hub button is a squircle; the topic filter opens from the subject name.
- Changed: Home, Today, Trash and Subjects redesigned with tinted cards.

## 2026-10-29 (development)
- Changed: Home has a closed-by-default drawer for the subjects to watch under the bunk buffer card, a pinned Subjects button instead of the AI button beside the pill, and an Attendance popup (Present / Absent today and notes collected).
- Changed: the subject folder uses the main navigation pill, which shrinks while scrolling, with a button that slides away; the list runs under the navigation bar.
- Fixed: the Import an outline dialog buttons; the chosen day in the calendar now has a ring so it differs from a holiday; note previews no longer show leftover style markers.

## 2026-10-29, later (development)
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

## 2026-10-31 (development)
- Fixed: the syllabus import builds a proper nested tree and leaves out course details; rows are smaller; tapping a unit shows only that unit.
- Fixed: "The provider's reply was empty." (Gemini thinking tokens used up the reply; empty replies are retried and explained).
- Fixed: an AI error shows once, at the bottom, for 5 seconds, and swipes away left or right.
- Fixed: the date picker in Edit entry, the absent-date choice, the "Kind of notes" layouts, image / table / style markers breaking when typing next to them, a random shape on empty pages, the folder pill on empty pages, the drafts pull-down inside a folder (that subject only), the shadow under the main button, rotation reset in the photo editor.
- Changed: the AI Hub is now **Absencium AI**, a chat with a sidebar of chats, a model chooser, pin / rename / delete / share, edit and copy on your messages, and (only where you allow it) suggestions that change your timetable, exams, holidays, attendance, subjects and notes after you tap Apply.
- Changed: every AI provider keeps its own API key; AI & Integrations lists your providers, tests a connection and switches in one tap.
- Changed: sharing a note offers PDF (with a Themed PDF toggle) and a greyed-out Sharewave.

## 2026-11-01 (development)
- Changed: Absencium AI answers keep being written when you open another chat, leave the screen or close the app (a small "AI answers in progress" notification shows only while one is being written). A finished answer you have not seen shows a dot in the chat list.
- Changed: Absencium AI is a general assistant first: it no longer limits itself to your app data, answers in depth, and writes notes at the length you ask in the app's own note format (long notes come from the note writer, not a short chat reply).
- Added: optional Web search with sources (Gemini, Claude, OpenRouter), optional Thinking, optional Memory across chats (view, edit, delete), assistants with presets and import / export, study mode difficulty, files in a question (pictures, PDFs, text, recordings, videos, notes), Retry with another model, Continue, Archive, Save as note, token usage per provider, extra API keys per provider with automatic switching when one hits its limit.
- Changed: new chat screen, full-screen chat list and "+" sheet after the reference screenshots.
- Planned: a notification when an answer is ready while you are elsewhere; tapping it opens that chat (see TODOList 5.6).

## 2026-11-01 (development)
- Fixed: Absencium AI greeting no longer overlaps the suggestions; chat text no longer runs under a solid block behind the bars; the sidebar slides in from the side and cannot be clicked through; the model chooser is a compact sheet and the provider pills no longer glitch; "needs attention" is no longer said for subjects at or above the required percentage.
- Fixed: `{color:...|text}` no longer shows as raw text in notes and AI chat (missing closers are repaired as the AI writes, when a note loads and when it is shown).
- Changed: the empty chat page offers an incognito chat (not saved, no memory) instead of the new-chat icon; the thinking glow became the loading shape; "Send a note" is a compact picker like the Vault, also for study material.
- Added: the AI can ask to read the content of a note or study material; nothing is sent until you approve the card.
- Added: empty-page indicators on the Drafts popup and the Search page; reading mode Search finds words in the note with a separate AI icon; selecting text no longer scrolls the page to the top.
- Docs: reference pictures and feature list for Absencium AI in `docs/ui overhaul/Ai/Generical-reference/`; `CURRENT.md` merged into `TODOList.md`.

## 2026-11-02 (development)
- Fixed: a crash when an AI answer or note contained a colour tag (Android's regex engine rejects a bare `}`).
- Fixed: the answer now says when another provider answered because yours was at its limit; a limit rests a key for one minute, not ten.
- Fixed: OpenRouter web search no longer fails with HTTP 402 on an account without credits; the question is answered without it and web search turns itself off for that chat.
- Fixed: the header overlapping the chat text; the "+" sheet's Notes tile wrapping.
- Changed: the AI answers like a person (answer first, no openers or closing offers, length follows the question); chat history sent to the AI is capped and shortened so a long chat costs less; thinking runs at Medium effort.
- Changed: when the AI reads a note or material you approved, the result goes to it directly instead of appearing as a message.
- Added: the AI links notes, study material and subjects by name; tapping a link opens it.
- Added: quizzes are interactive (multiple choice, true or false, typed answers) with a score and a review of the missed questions.
- Removed: every shadow in the app (menus, buttons, bars, sidebar, glass).
- Fixed: Remove provider did nothing; it now removes the key, URL and model, and another provider takes over when it was in use.
- Fixed: a plain "hi" cost about 3.5k tokens; app data, the actions format, quiz format and note rules are only sent when the conversation needs them.
- Changed: the AI & Integrations page is grouped lists with short rows, an empty state, and a searchable provider picker; the model title and chooser are smaller.
- Added: several API keys per provider; choose the one in use, the others take over at a limit.
- Changed: the AI only receives the parts of your app data, and only the action, quiz and note formats, that your messages call for.
- Fixed: a quiz appearing as raw JSON in a code block; the page showing through behind the title bar.
- Changed: the AI's fixed instruction is about half as long, and the full weekly timetable is only sent when you ask about the week.
- Added: Response mode (Efficient, Balanced, Maximum quality) in AI & Integrations; the app now works out locally what a request needs before asking the model.
- Added: each answer shows the tokens the provider reported for it.

## 2026-11-03 (development)
- Added: web search for every AI provider (the app searches DuckDuckGo with your question's words and shows the sources); Gemini still searches by itself.
- Added: switch a provider off without deleting it; a pasted API key suggests its provider; keys show when they are resting after a limit.
- Fixed: a deleted provider stayed in AI & Integrations until you left and came back; asking the AI to save its last answer as a note now saves exactly that answer.
- Fixed: web search reporting "limit reached" on a Gemini key that had tokens left; every model now uses the app's own web search.
- Changed (branch Absencium-ai): chat requests now go through the LastChat provider layer (OpenAI, Claude, Google); your keys, failover, response modes, quiz, links and notes work as before. The ported code no longer writes your questions to the system log.
- Changed (branch Absencium-ai): AI web search now uses LastChat's keyless search, which picks sources by question type (news, weather, encyclopedia) and shows a quick answer when there is one; the old DuckDuckGo search is the fallback.
- Changed (branch Absencium-ai): Absencium now needs Android 9 (API 28) or newer, because the embedded LastChat chat needs it.
- Changed (branch Absencium-ai): AI & Integrations shows only the AI switch and providers; response mode, what AI can use, token counts, images and integrations are under a closed Advanced section. In a provider, the API URL and API style are under Advanced too (open for a custom provider).
- Changed (branch Absencium-ai): LastChat's chat app (chat screen, input bar, attachment menu, pickers, settings, export) is now built into Absencium as its own module, without its cloud, update, web-server, widget, workspace and on-device-model parts; AI & Integrations has a "LastChat chat" row that opens it. It keeps its own chat database and settings.
- Fixed (branch Absencium-ai): builds ran out of memory and took 27 minutes; build memory is raised and the unused debug build of the chat module is skipped.
- Changed (branch Absencium-ai): there is now one AI, Absencium AI. Every AI button opens the same chat, and the old AI hub, provider screens and key vault screens are gone. Pick your provider, key and model inside Absencium AI; the note writer, syllabus import, text from files and homework use that same choice.
- Added (branch Absencium-ai): Absencium AI can look up and change your app: subjects, notes, timetable, exams, holidays, attendance and syllabus. Looking things up needs no tap; every change, and reading a whole note, waits for your approval, and deleted items go to the Trash.
- Changed (branch Absencium-ai): the chat uses your theme (Default, Monet, Liquid Glass, presets) and the 90 Hz cap, so it looks like the rest of the app.
- Changed (branch Absencium-ai): development builds contain arm64 only, so the APK is far smaller; stable builds are one APK per processor type.
- Fixed (branch Absencium-ai): the assistant is now called Absencium AI instead of "Generical" and knows it lives inside Absencium, not LastChat; existing chats keep working.
- Added (branch Absencium-ai): Absencium AI can see your Vault: notes, drafts, attachments (with the text read from them) and study guides, and can move a note between Active, Finished and Draft after you approve.
- Fixed (branch Absencium-ai): Absencium AI no longer adds duplicate lectures, exams or notes, no longer undoes an attendance mark by marking it twice, checks dates against the weekday, and puts a syllabus into the Syllabus (new tool) instead of a note.
- Changed (branch Absencium-ai): Absencium AI follows your theme more closely: Liquid Glass wallpaper behind the chat, the morphing loader instead of plain spinners, no shadows, and the chat's own theme settings are replaced by the app's Appearance.
- Added (branch Absencium-ai): Absencium AI shows a bulk change as one plan you approve once, and AI settings, Advanced, has "Do without asking" switches for subjects, notes, timetable, exams, holidays, attendance and syllabus (deleting always asks).
- Fixed (branch Absencium-ai): asking Absencium AI to add to a note now adds to the existing note (or the one you just had open) instead of creating a new one, and the chat reports exactly what was saved.
- Changed (branch Absencium-ai): chat notes, Generate Notes and homework use one formatting rule: tables, colours, highlights, formulas, checklists and headings appear only where the content needs them, not in a fixed layout.
- Removed (branch Absencium-ai): the old built-in AI chat code, its memory, assistants and background service, now that Absencium AI does all of it.
- Fixed (branch Absencium-ai): the approval card for Absencium AI changes now shows what will be added or changed in plain words (every step of a plan), not cut-off technical text.
- Added (branch Absencium-ai): a tick box for every step of an Absencium AI plan (untick to skip it), and "Ask Absencium AI" in the note editor and reading menus, which lets "this note" mean the note you are in.
- Added (branch Absencium-ai): an empty Absencium AI chat offers suggestion chips built on your phone from your own data: the next exam, the subject furthest under your attendance target, today's lectures and the note you just had open.
