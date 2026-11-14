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
- Fixed (branch Absencium-ai): with Liquid Glass on, the solid white or black band behind page titles is gone in the app and in Absencium AI; titles sit on a faint glass veil over the wallpaper.
- Changed (branch Absencium-ai): the new-chat screen opened from "Ask Absencium AI" shows the same data-based chips; the Absencium AI avatar follows the theme colours.
- Changed (branch Absencium-ai): the chat drawer shows your Absencium profile (name and avatar style); the name also replaces the chat's own user name in prompts.
- Changed (branch Absencium-ai): with Liquid Glass, page headers are a blur of what scrolls under them in the page colour, dissolving at the lower edge (Home, Vault, Calendar, settings pages, note editor); other themes keep the solid fade.
- Changed (branch Absencium-ai): frosted popups are lighter in light mode and blur harder; in Absencium AI the input bar, chips and approval card blur the messages behind them under Liquid Glass, and the drawer is a translucent pane over a blurred page.
- Changed (branch Absencium-ai): the gear in Absencium AI opens Absencium's own AI settings page: the main list holds Open Absencium AI, Default models, Providers and keys, Assistants and Web search; one closed Advanced section holds Plugins, Skills, Voice, Android integration, Chat storage and backup, Do without asking, pictures and Momentum.
- Changed (branch Absencium-ai): Imagine is hidden in the chat drawer until "Generate images (Imagine)" is switched on in AI settings, Advanced (off by default); Statistics stays.
- Changed (branch Absencium-ai): the profile is a full page (not a popup) in the Settings card style: avatar with the attendance ring, name, course, year and college (kept on the phone), overall attendance with the skip or attend count, each subject's attendance log, syllabus progress, next exams, Vault counts, and shortcuts to Absencium AI and Settings.
- Changed (branch Absencium-ai): opening Absencium AI grows in with a springy fade instead of a cut.
- Changed (branch Absencium-ai): Settings, General, Appearance, Storage & Data, Absencium AI and Profile open with a large centred title that scrolls away into the bar title; the Settings hub groups its entries as joined cards (large outer corners, small where they meet) like the chat's settings.
- Fixed (branch Absencium-ai): chat settings pages (models, providers, assistants, search, plugins and the rest) use see-through glass cards under Liquid Glass; the Topics panel in a subject is frosted glass; the chat's collapsed header is a veil that dissolves instead of a solid black band.
- Fixed (branch Absencium-ai): opening a chat settings page from AI settings slides in, and Back slides it out and returns straight to AI settings (no chat flash in between, no double animation).
- Changed (branch Absencium-ai): Profile redesigned: one centred card (avatar with ring, name, course · year · college, Edit opens the fields in place), three number tiles, one attendance card and one syllabus card with a bar per subject, next exams, and joined shortcuts.
- Fixed (branch Absencium-ai): Liquid Glass now applies at the theme level: every surface on a screen (cards, tiles, lists, plain Surfaces) is see-through over the wallpaper, not only the screens fixed one by one; dialogs, menus and popups keep solid colours so text stays readable; the chat's dropdown menus are solid too.
- Changed (branch Absencium-ai): Absencium AI opens as a layer inside Absencium's own window (no second activity): it grows in over the page with a spring and Back returns to the same page, with the same theme, wallpaper and system bars; the chat's settings gear and settings rows return to the app's AI settings without leaving the window. Notifications, the share sheet and the assistant overlay still open the chat in its own screen.
- Changed (branch Absencium-ai): less text on pages: explanatory hints removed (Appearance, AI settings, Syllabus, Calendar, Licenses, Test Lab, Onboarding, Storage) and row subtitles shortened or dropped on Settings, General, AI settings, Appearance, Onboarding, About, Profile and Trash; errors, loading and empty states keep their text.
- Changed (branch Absencium-ai): one forward and back animation in Absencium and Absencium AI, one section-title style, AI settings and Profile redesigned, wording made shorter and more professional across screens.
- Changed (branch Absencium-ai): AI notes are planned first: a short step decides which parts the topic needs and only those are written, so tables, comparisons, revision lists and common mistakes appear only where they belong; a full note contains only the note, never questions and answers.
- Changed (branch Absencium-ai): Generate AI Notes: the tables and previous-year-question switches are gone; each kind of notes has its own size setting (length, detail, size, question bank size, time you have, record); course, year or semester is taken from the Profile with a menu, a clear button and free typing; Solve Homework keeps only the question, photos, course and picture options.
- Added (branch Absencium-ai): AI actions folder on the chat's Statistics page listing notes, homework and chat changes with what was typed or chosen, a preview of the result and an Open button to the page it belongs to.
- Added (branch Absencium-ai): Vault option in the chat's attach menu to link a note, syllabus or material by reference without uploading it.
- Changed (branch Absencium-ai): every remaining LastChat name in the code and app is now Absencium AI (module folder, classes, files, resources, scheme, export formats, visible text); credits stay in the Licenses and THIRD_PARTY_NOTICES.md only; an existing chat database is moved across so chats are kept.
- Added (branch Absencium-ai): Profile has a Syllabus button opening one page with every subject's syllabus and all the features of a subject's syllabus page.
- Added (branch Absencium-ai): Absencium AI tools to edit, move and delete a syllabus topic and to read the text of a Vault material (the student approves each read); the tools are listed in category folders in the assistant's Tools tab.
- Changed (branch Absencium-ai): the AI actions folder now lists hidden Absencium AI chats, one per action, like the normal chat list; they can be opened and read but not written to (the prompt field says Read only).
- Changed (branch Absencium-ai): Add Entry notes and homework are made by Absencium AI in a hidden chat that saves the note itself, instead of a separate AI path.
- Added (branch Absencium-ai): an Excused attendance status (a medical or approved absence) in the day sheet; like Cancelled it is left out of the percentage.
- Added (branch Absencium-ai): Absencium AI tools to list and restore Trash items, move, duplicate and bulk move or delete notes, view a month of the calendar, mark attendance over a range of days, and filter the timetable by room; exams show days left.
- Changed (branch Absencium-ai): syllabus outline import, holiday and exam import, text from photos and the editor's AI requests are now hidden Absencium AI chats listed in AI actions, instead of separate AI calls.
- Added (branch Absencium-ai, CI green `43140074665`): Study Tools page (icon beside Profile) with quick timers, Grades & GPA, Attendance insights, Flashcards with spaced review, To-do and your own events, and Export & share (`.ics`, CSV, PDF report).
- Added (branch Absencium-ai, CI green `43140074665`): Reminders settings (all off until chosen) for lectures, exams, your own events and low attendance, using one low-battery alarm that is set again after a restart.
- Added (branch Absencium-ai, CI green `43140074665`): lecture types in the timetable, exam marks and pass marks, history of earlier note texts (last 20) with Restore in the note editor, and Absencium AI tools for flashcards, to-dos and events. The database is now version 5.
- Changed (branch Absencium-ai, CI green `43003913457`): Absent is a split button with Excused inside; the read-only AI action chat hides Edit, Fork, Delete and Regenerate; models that cannot read images say so plainly; the app is 43.8 MB instead of 74.1 MB.
- Changed (branch Absencium-ai, CI green `43202471499`): Study Tools and the calendar page redesigned (hero summary card, pastel tiles, shape badges, week strip, timeline, to-do progress).
- Fixed (branch Absencium-ai, CI green `43203890335`): "Ask Absencium AI" in a note's menu opens the AI hub with the note linked in the message box.
- Fixed (branch Absencium-ai, CI pending): the top Back button of the AI hub and its settings leaves the chat for the app when nothing is left to go back to.
- Fixed (branch Absencium-ai, CI green `43284989972`): the top Back button of the AI hub and its settings now leaves the chat for the app.
- Changed (branch Absencium-ai, CI green `43351210596`): Absent is one button again; Excused is a chip beside Notes collected once Absent is chosen. The chat's first-run provider setup no longer shows inside the app. An AI action's long answer that was cut off is continued automatically.
- Changed (branch Absencium-ai, CI green `43351276080`): AI action chats show only the request, not the fixed instructions.
- Changed (branch Absencium-ai, CI green `43351644567`): a Vault link attached in Absencium AI is shown as an attachment tile (note, syllabus or material) in the message box and in the sent message.
- Changed (branch Absencium-ai, CI green `43351770960`): Home, Attendance and Vault slide forward and back like the AI hub's pages.
- Changed (branch Absencium-ai, CI green `43352041917`): every dialog opens with one spring grow and fade and resizes smoothly; the syllabus topic pickers (File under a topic, Add Entry) are one collapsed tree with arrows at the left. An earlier run, `43351839821`, failed on a missing `getValue` import in `SyllabusPicker.kt` and was fixed in the next commit.
- Changed (branch Absencium-ai, CI pending, Batch 44): the Profile tabs are now Overview, Academic and Insights, built from data the app already has (attendance, syllabus, goals, exams, timetable, notes, study time); sections that need new features are listed as TODO row 44.14.
- Changed (branch Absencium-ai, CI pending, Batch 44): one topic sheet everywhere; AI Vault link tabs; profile picture in the top bar and AI drawer with the AI's avatar card; Vault access approval Auto or Manual; round floating buttons in the hub settings; Python/Workspace traces removed; original Home pill restored; popup opening no longer squeezes content.
- Added (branch Absencium-ai, CI green `43551343493`): one Style setting (Default, Liquid Glass, Pixelated, Dot matrix) with a first Dot matrix theme using the bundled Doto font.
- Changed (branch Absencium-ai, CI green `43485589374`): a Vault note or syllabus tile in an AI message opens that note or syllabus.
- Changed (branch Absencium-ai, CI green `43431495994`): the Profile page is remade (banner, cookie-shaped picture, Overview / Statistics / Subjects tabs, heat-map, subject tiles, small AI and Settings icons).
- Changed (branch Absencium-ai, CI green `43431858151`): the main Syllabus page lists each subject as a folder that opens its syllabus page; full-screen popups open with the same spring motion.
- Added (branch Absencium-ai, CI green `43352206487`): Edit profile page with a cropped profile picture; course and year are saved together as one line.
- Changed (branch Absencium-ai, CI green `43386351738`; round buttons on the other screens green `43386616561`): on Home, Attendance and Vault the round buttons sit beside the navigation pill at the same height with search above, and the pair stays centred. The run `43352325474` failed on a missing `Row` import in `MainScreen.kt` (`MainScreen.kt:375 Unresolved reference 'Row'`), fixed in `f886c0c`.
