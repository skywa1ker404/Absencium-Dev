# UI overhaul: reference images and design analysis

Owner request 2026-10-27: a complete overhaul of the whole app's look and layout (not touch-ups), using these 15 images as the standing reference for every future UI decision. **Only the 3-button navigation pill keeps its design** (shape, lens, selected bead). Everything else (elements, positions, shapes, typography, motion) may change, including swapping one kind of element for another.

**Rights note.** Images 3 to 15 are other designers' mock-ups, kept here as private inspiration. Nothing from them (artwork, icons, illustrations, photos, brand marks) is copied into the app. We take structure, proportion, spacing, colour logic and behaviour only. They are not part of the APK. Image 1 and 2 are screenshots of Absencium itself.

**Rules for using this folder**
1. Open the matching image before designing or changing any screen (the table at the end says which).
2. Follow the app's chosen theme (Default, Monet, Presets, Liquid Glass): the images show *layout and shape*, the colours always come from the theme.
3. Everything must also work with Liquid Glass off and with Performance mode.
4. GPL-3.0, offline-first: no avatars from the network, no remote fonts, no telemetry.

---

## Our own screenshots

### 01 `01-current-app-glass-9-screens.jpg` (Absencium today, 9 screens)
Trash, Appearance, Today, Attendance calendar, Vault grid, Subjects, Home, Search popup, Add entry popup. What is wrong, grid cell by grid cell:
- **All panes look like grey-green plastic.** Every card is the same flat tint with a pale haze across the top-left and a bright rim, so nothing has depth and the wallpaper does not read through. Cause: sheen gradients and a uniform rim on every card (removed on 2026-10-27, see Step 19).
- **Trash, Subjects, Today:** long stacks of identical wide cards with small text buttons ("Restore", "Delete for good", "Mute", "Rename") inside. Hard to scan, no hierarchy, buttons look like plain text. Needs grouped lists, icon-led rows and a clear primary action.
- **Appearance:** description text is tiny and dense inside the switch cards. Needs one-line titles, a short helper line, and the preview of each theme.
- **Attendance:** the stats gauge sits under the navigation pill and its text shows through the pill (the pill's veil was thinned by mistake; restored). The stat chips ("Present 2 / Absent 4 / Total 6") collide with the pill. Needs bottom padding equal to the pill height plus the floating buttons.
- **Vault:** uneven folder tiles (one tall, one short), small text. Needs a consistent grid with big icon chips, a count and a hue per subject.
- **Search / Add entry popups:** content under the popup shows through unblurred so the labels clash; tiles are the same flat tint as the popup. Needs one glass layer (the popup) with calmer inner tiles.
- **Good and to keep:** the pill and its lens, the AI button beside the pill, the title style "Absencium", the coloured status dots.

### 02 `02-ai-options-popup-bug.jpg` (AI notes options, keyboard open)
Shows: "Include, Tap to switch each on or off" with two selected chips, a large empty gap, then "Your course" with a text field whose bottom edge is cut by the end of the popup, keyboard open. The popup is solid (no glass), the field overflows the sheet, and the gap comes from fixed-height sections. Fix in the overhaul: popup content scrolls with `imePadding`, no fixed heights, chips wrap in a flow row, the popup uses the glass layer. (This is the screenshot that arrived with the message about the nested-style note bug; the note-style fix was made from the text of that message, see TODOList 10.31.)

---

## Reference designs

Common ground of almost all of them: big rounded containers (28 to 36 dp), capsule chips and buttons, pastel tonal cards with a white icon chip, a round "back / search / more" button on every header, a floating bottom bar that is separate from the content, generous spacing and one strong accent per screen.

### 03 `03-notes-pastel-cards-ai-menu.jpg` (My Notes, note editor)
- Pale periwinkle backdrop, near-white screen. Header: very large light-weight title ("My Notes") with a round grey icon button at the right.
- Filter chips in a scrolling row: selected = tinted fill + 1 dp border + count badge ("All Notes 32"), others outlined with their count.
- **Note cards are tinted by hue** (green to-do, blue sprint plan, purple diet plan): fill = very light tone, 1 dp border = slightly deeper tone, title and body text in the same hue family (dark to mid). A white round star button with outline star sits top right. Radius about 24 dp.
- Checklists are shown inside the card with outlined rounded checkboxes.
- Editor: big blue title, row of round-ish toolbar icons (bold, italic, heading, quote, link, image, bullet, number), then sectioned content with small bold headings. An outlined pill "Ask AI to..." with a sparkle sits under the content and opens a rounded list (Continue writing, Improve writing, Summarize, Fix spelling and grammar), each with a line icon.
- **Take:** hue-per-subject note cards; filter chips with counts; AI "Ask AI to..." pill in the editor; monochrome editor theme that follows the subject hue.

### 04 `04-tasks-calendar-pastel-purple.png` (My Tasks, My Calendar)
- Header row: avatar, round search button, round more button. Title bold.
- Chip tabs with icon; the selected one has a gradient outline and a filled count badge.
- "Active Task" cards in pastel lavender, pink, cream: title, "Progress 80%" and a **diagonal-striped gradient progress bar** (purple to orange).
- New tasks: full-width pastel cards (pink, blue) with a rounded colour checkbox and a two-line description.
- Calendar: week strip of **tall capsule day chips**; the selected day has a gradient ring and a filled gradient circle for the number; other days are pale lilac circles.
- Timeline: hour labels at the left, event cards with a **coloured bar at the left edge**, a time pill inside the card, tinted by hue. A **hatched block** with a label chip ("Break 1hr") marks a free gap, with a thin line with an arrow for "now".
- Gradient circular FAB with calendar-plus icon. Bottom: dark floating bar of round icon buttons; the selected one expands into a white capsule with icon and label.
- **Take:** timeline view for the timetable and today; hatched free periods; capsule day chips; striped progress; "now" line.

### 05 `05-ai-study-app-purple.png` (AI Study App, 4 screens)
- Home: three large circular quick actions (Scan and solve, Type question, Create exam) in pastel circles with outline icons and a caption below; "Focus study" card with a play button; "Today's progress" as tinted rows with round icons; "Quick actions".
- Bottom bar with a **raised centre button** (purple rounded square with sparkle, label "AI Tutor").
- Planner: month pill with arrows, weekday header, selected day a purple capsule; two pastel stat tiles (Pending, Total); "Today's schedule" as a timeline of white cards with a dot per item.
- My Study: chips (All, ...), list of white cards with an illustration, title, subtitle and chevron.
- AI Assistant: chat bubbles (white with soft shadow, user bubbles right with a small avatar), suggestion chips with icons above the input, input pill "Ask Anything" with a send arrow.
- **Take:** AI Hub as a chat with suggestion chips; quick-action circles on Home; today's schedule as timeline; stat tiles.

### 06 `06-glass-workforce-blue.png` (the glass reference, small image)
The Liquid Glass reference. Light blue-grey gradient wallpaper; **white translucent panes** with a soft blur, a thin white edge, a faint diffuse shadow underneath and **no glow or haze**. Cards: headline "Empower Your Workflow", announcements with a grid of icon tiles (Time Tracking, Org Chart, Expenses) as small frosted squares; check in/out card over a map; the "Absence" calendar in a frosted panel with round translucent date cells and a blue selected date; task rows with a percent chip; a **frosted bottom pill** with a blue filled round selected icon; round frosted back and bell buttons.
- **Take:** this is what Liquid Glass should look like: clear blur, flat veil of the theme colour at about 40 to 55 %, one thin edge, soft shadow, content readable. In dark themes the veil is the dark surface colour, the edge is a dim white.

### 07 `07-green-gradient-folders-ai-notes.png` (AI notes app: Home, Recording sheet, Paywall)
- Sage-green to blush gradient wallpaper. Top row: a round **gradient orb with a sparkle** (AI) at the left, a white capsule button, an avatar at the right.
- Search is a translucent pill. A frosted strip "Next up: ..." sits above a **big white card**: icon chip, count badge, small caption, bold two-line sentence, **black pill button with a round icon** ("Start Now").
- "My Folders": tiles with a soft blurred two-tone gradient (rose, lavender), a frosted icon chip at the top left, name and "15 Notes" in white at the bottom. This is the subject folder grid we need.
- Bottom bar: white capsule with line icons; the selected tab is a **large dark capsule** with a white icon.
- Recording sheet: a large soft **aura (blurred green orb)** with a waveform-sparkle icon, a two-weight title (grey first line, dark second), small body, four round white buttons (audio, video, image, link), a long dark "Start Recording" pill and a round settings button.
- Paywall: gradient background, feature list with line icons, a white translucent plan card with a pink discount badge, a toggle row, a black "Subscribe" pill.
- **Take:** subject folders as gradient tiles with an icon chip; AI orb; sheet with aura for recording; two-weight headings; dark primary pills.

### 08 `08-lavender-lesson-schedule-dark-pill.png` (Start Your Journey, Lesson schedule, Courses)
- Lilac backdrop, white screens with 36 dp corners. Big bold title and a round outlined search button.
- Chips: selected = solid purple, others white with a thin outline.
- "Set your weekly goal!" input as a long white pill with a lavender icon circle and an arrow.
- Large pastel card (lavender, mint peeking at the right) with a white rounded-square icon, avatar stack, label, title, time and a **purple circular arrow button**. Below, two smaller cards with lock icons.
- Schedule card: weekday header and **two-row date grid of circles**; past days have a hatch pattern; the selected day is solid purple; a **range** is shown by a lavender band joining the circles; dots mark events. "Weekly progress 60%" as a lavender tile with an arc gauge.
- "Study day 6" bar chart of **capsule bars**: hatch pattern for empty part, solid purple for the selected one, dark tooltip bubble with the day.
- Courses: **ring gauge** in purple, mint and pink with percent labels on leader lines; 2x2 pastel tiles (mint, lavender, periwinkle, pink) with a white round icon, title, minutes and a frosted "Learn more" pill.
- Bottom bar: dark charcoal capsule with round icon buttons; the selected one is a filled purple circle.
- **Take:** the attendance calendar (circle grid, hatch for holidays or cancelled, bands for ranges), analytics gauge and capsule bars, pastel tiles.

### 09 `09-homework-pastel-list-pill.jpg` (Homework)
- Warm neutral backdrop. A tab-like title row ("Subjects  Homework  Library") where the active word is bold black and the others grey.
- Grey search pill. Two inline dropdown filters ("Subject: All", "Sort by: Do first").
- Heading "Tuesday 6" with "4 tasks" and a calendar icon.
- **Full-width pastel rows** (blue, mint, cream, lilac), radius about 30 dp, each with a white rounded-square icon, title, duration and a **white pill action** ("Done" / "To do").
- Bottom: a floating white capsule with three icons, the selected one a black rounded square, and a separate round white button for chat/AI.
- **Take:** Today and Homework lists, task rows with a one-tap state pill, inline dropdown filters, an AI button next to the bar (we already have it).

### 10 `10-study-focus-tools.jpg` (Study tools)
- Header: hamburger, centred title, avatar and a sun/moon switch.
- Eyebrow "Your focus space", large title, one-line description.
- "Quick start": **2x2 pastel tiles** (lavender, light blue, mint, peach), each with a small coloured dot, bold title and a one-line hint.
- "Your study tools": white list cards with an icon square, title, description and chevron.
- **Take:** Settings hub and Study tools as a quick-start grid plus list cards; the theme switch in the header.

### 11 `11-tasks-schedule-profile-dark-pill.png` (My tasks, My schedule, My profile)
- Tasks: white cards with a status tag (orange "In progress", purple "To do"), title, subject, date with icon, comment count, and a **segmented striped progress bar**. Filter chips (selected = black fill). Round white search and bell buttons.
- Schedule: week strip of rounded squares (selected = purple), list cards with clock icon and the time range; the **current class is a green card** and a "Now" divider sits above it.
- Profile: purple header, avatar card with name and class, two segmented tabs, an About paragraph, "Upcoming activities" rows with icon tiles and a more button.
- Bottom: dark capsule with round icons, selected = white circle.
- **Take:** timetable day view with a highlighted current lecture, profile card, segmented tabs.

### 12 `12-learning-progress-pastel-cards.png` (Start learning, Your progress, Time tracker)
- Onboarding: illustration on a purple field, a white bottom sheet with a large top radius, bold two-line title, black "Get Started" pill and a purple round arrow button.
- Progress: greeting with a thin progress bar and a book icon; big title and round search; pastel cards (sage, lilac) with a white round icon, rating chip, category, title and a round arrow button with a partial ring; faint illustration watermark.
- Tracker: round back and more buttons, two stat tiles (sage, yellow) with a big number and a round arrow; "Progress" section with **capsule bars** per month (hatched track, filled value, dark value bubble).
- **Take:** Home progress cards, per-subject stat tiles, onboarding sheet.

### 13 `13-learniq-ai-summary-quiz.png` (LearnIQ: Home, AI Summary, AI Quiz)
- Soft white with indigo accent. Header: logo, bell, avatar. Two-line greeting. Search pill with mic and image buttons (this is the voice prompt and image prompt row we want in the AI Hub).
- Study streak card: weekday dots with checks, **half-donut gauge** with hatch.
- "Continue learning": cards with hatched coloured progress blocks and a percent. "AI Tools": three tiles with pastel icon squares (Summary, Study, Quiz).
- AI Summary: tabs as chips (Summary filled indigo, Notes, Mind Map, Flashcards), "AI generated summary" card, key points as bullets, a formula card, horizontal "Important formulas" cards, a wide indigo "Generate Quiz" pill.
- Quiz: segmented progress pills, question, four option cards with letter circles (selected = indigo outline and tint), "AI explanation" card, **Previous (ghost) / Next (indigo)** buttons.
- **Take:** AI Hub and generated notes layout (tabs for Summary, Notes, Mind map, Flashcards), quiz screen, streak card for attendance.

### 14 `14-notes-mono-editor-toolbars.webp` (monochrome notes: editor, list, reading, annotation)
- Black-and-white with one purple highlight colour. Thick dark device frame (ignore).
- Selection popover: a **dark rounded toolbar** (copy, paste, cut, more) with a small caret. Formatting sheet: highlighter row with colour dot and comment icons, then a grid of toggle buttons: B I S U lists, H1 H2 H3, indent, alignment (selected = filled black), quote.
- My Notes: avatar, centred title, round more button, search field with a layout-toggle icon, **masonry** of cards: image card with a pin, a locked card with a blurred preview and shield, a card with avatars and a checklist, a tag chip ("Work") and a recording icon, a floating black **+** button.
- Reading: big bold title, metadata line, paragraphs with a purple highlight, floating dark toolbar (highlighter, colour, comment, bold), quote block with a purple left bar.
- Annotation: image with a sticker that has selection handles, hand-drawn circle and arrow, a label chip; bottom white toolbar (colour, mirror, align, layers up/down, delete in red).
- Audio note: dark rounded player with play button, waveform and duration.
- **Take:** our reading mode (selection pill, highlight toolbar), the editor toolbar sheet as a toggle-button grid (M3E connected buttons), masonry vault list, locked notes, audio player.

### 15 `15-tasks-efficiency-soft-3d.webp` (Tasks, My Efficiency)
- Lavender backdrop, soft shadows. Timeline with hour labels and a tick ruler; **pill-shaped pastel cards** (blue, teal, purple, pink, orange) with a small-caps label, a more button and text; one card lifted with a stronger shadow while dragged.
- Efficiency: a **thick donut gauge** in green with a soft inner shadow and a big "89%", **vertical capsule bars** per month in different hues, an outlined dropdown pill ("for January").
- Bottom: white bar with line icons and a small dot under the selected one.
- **Take:** analytics (donut, capsule bars), drag lift feedback in lists (spring scale and shadow).

---

## Design system distilled for Absencium

| Area | Rule |
| --- | --- |
| Corners | Cards and sheets 28 to 36 dp, rows 24 to 30 dp, chips and buttons capsule, icon chips rounded square (14 to 18 dp) or M3E shapes (Cookie, Sunny, Pill, Clover...) |
| Surfaces | Each subject has a hue. Cards are tonal tints of that hue with a 1 dp tonal border. Liquid Glass: the same tint as a translucent pane over the wallpaper |
| Type | Very large light title for each top-level page; two-weight headings (grey then dark); small eyebrow label above titles; numbers big and bold. Use the M3 Expressive emphasised styles |
| Buttons | Primary = dark (inverse surface) capsule, optionally with a round icon at its end. Secondary = round icon buttons (back, search, more) in a tinted circle. Selected chip = filled, others outlined with count badges |
| Progress | Capsule or wavy bars with diagonal hatch for the empty part; ring and half-donut gauges; capsule bar charts with a value bubble |
| Calendar | Circle date grid, hatch for non-class days, bands for ranges, dots for events, tall capsule for the selected weekday |
| Timeline | Hour labels left, cards with a coloured left bar, hatched free gaps, a "now" line, a different colour for the current item |
| Lists | Full-width pastel rows with a white icon chip and a one-tap state pill; masonry for notes |
| Sheets | Large top radius, handle, aura or illustration at the top, two-weight title, round option buttons, one dark primary pill |
| AI | Gradient orb for the AI button, "Ask AI to..." pill with a list, chat bubbles with suggestion chips, tabs for generated output |
| Motion | Springs everywhere (M3 Expressive motion scheme): press squash, bounce in, shape morph on select, lifted card while dragging, loaders that morph between shapes with haptics |
| Navigation | **Keep the 3-button pill as is** (lens, bead, label). Other bars and buttons follow the references: round buttons, dark primary pills |

## Which image for which screen

| Screen | Look at |
| --- | --- |
| Home | 07, 13, 05, 12 |
| Attendance (calendar, stats) | 08, 04, 15 |
| Timetable and Today | 04, 11, 09 |
| Vault (subject folders) | 07, 14, 03 |
| Subject folder, notes list | 03, 14, 09 |
| Note editor and toolbars | 03, 14 |
| Reading mode | 14 |
| Syllabus | 12, 08, 11 |
| AI Hub, AI panels | 13, 05, 07, 03 |
| Popups and sheets | 07, 06, 12 |
| Settings, Profile, About | 10, 11, 09 |
| Analytics | 08, 15, 13 |
| Liquid Glass look of any element | 06 |
| What not to repeat | 01, 02 |

---

## Our own screenshots, second set (2026-10-28, owner: "ugly looking generic")

| File | Screen | What is wrong and what replaces it |
| --- | --- | --- |
| `16-ai-options-popup-keyboard-gap.jpg` | AI notes options, keyboard open | Card cut off at the top and a gap above the keyboard (fixed by the height clamp in `25f76a2`; re-test). Still plain tonal cards: becomes glass sections with chips (images 07, 13) |
| `17-ai-options-syllabus-dropdown.jpg` | Same popup, syllabus topic list open | A long list of full paths in one grey box. Becomes a tree picker with the path as breadcrumb chips and indent (image 12) |
| `18-show-notes-under-dialog.jpg` | "Show notes under" topic filter | Plain radio list in a stock dialog. Becomes a glass sheet with the tree, hue dots and counts per topic |
| `19-subject-folder-study-tab.jpg` | Subject folder, Study tab (old ChoiceBar tabs, before the TabPill) | Empty text and a plain button. Becomes grouped pastel rows with a white icon square and a pill action (image 09), empty state with the shape badge |
| `20-subject-folder-syllabus-tab.jpg` | Subject folder, Syllabus tab | Two flat full-width bars. Becomes a progress card with a ring and the units as pastel tiles (images 08, 12) |
| `21-syllabus-tree-screen.jpg` | Syllabus screen | Every node is its own big card, nested ones just shifted right, status as a grey text pill. Becomes one pastel card per unit holding its chapters and topics as rows with a thin guide line, a round status mark (hollow, half, check) instead of the text pill, striped / wavy progress (images 04, 11, 12) |
| `22-subject-folder-notes-tab.jpg` | Subject folder, Notes tab | Identical dark cards, raw markup shown in the preview (`` `==~~++{size:30|`...``), no topic, no status. Becomes hue-tinted cards with a clean text preview (markers stripped), topic chip, date and status dot, optionally a masonry grid (images 03, 14) |

Owner rule (2026-10-28): redesign these three (notes list, syllabus tree, study material) from images 3 to 15 when the screen overhaul (Step 19.4) reaches them, and **never drop a pending task after new requests arrive** (see the ledger in `TODOList.md`).

## Status of the layout work (2026-10-28, CI green `79fbc9f`)

- Pictures 16 and the gap above the keyboard: the cause was the activity's window mode (pan instead of resize), fixed in the manifest; the height clamp from `25f76a2` stays. Needs a device check.
- Owner reference "pic 2" (AI button, pill, plus and search buttons at the bottom): the folder screen now follows it. The tabs are a floating glass pill, the main action is an extended floating button above it on the right, the AI Hub button became a squircle. The same floating pattern is used for Add subject (Subjects) and Add unit / Import outline (Syllabus).
- Redesigned and CI green: notes list, study material, syllabus tree, Home, Today, Trash, Subjects.
- Still to redesign: Appearance, Vault grid, Attendance, Timetable day view, AI Hub, Settings hub, profile, onboarding, editor toolbar, reading selection pill, AI options popup and the tree-style topic picker (screens 17 and 18).

### Update 2026-10-29 (CI green `91daba9`)
- Screens 23 to 27 (owner screenshots: Attendance card on the calendar tab, three states; Import an outline dialog with squeezed buttons; folder notes tab in the light theme with a copy of the pill and a band at the bottom) were fixed as TODOList 19.11 to 19.13.
- Home follows images 07 and 13 more closely: one bunk buffer card with a drawer under it for the subjects to watch (hidden by default), quick actions Attendance / Timetable / AI Hub, a pinned Subjects button beside the pill.
- The owner's rule: read this file before every new UI task; every new element uses the Material 3 Expressive components and springs.

### Update 2026-10-29, later (CI green `497da1f`)
- Owner screenshots 28 to 33 (folder notes tab with the button half off screen, the editor header, the topic menu, the selection toolbar, the attendance card).
- Extended buttons now follow the Material extended FAB pattern: icon plus label, the label folds away on scroll down and returns on scroll up (`ShellFab(expanded)`); the pill shrinks only when the list really scrolls. Used on the folder tabs and the Syllabus tree.
- The Home pinned Subjects squircle from 2026-10-29 was removed again at the owner's request.
- Topic menu redesigned: one component (`TopicMenu`), 48 dp rows with one leading slot (icon or level dot), indent per level, tinted chosen row with a tick, solid container, scrolls when long.
- The editor header is the date, visible character count and save state on one line, with the topic as a dropdown chip under it.
- Selection toolbar: Edit only for one note; study material has its own selection (Topic, Delete); a list / card switch sits at the right of the breadcrumb on both tabs.
- Next on the list: Vault grid (image 07), then Appearance, Attendance, Timetable day view, AI Hub, Settings hub, profile (with the syllabus card, TODOList 19.21), onboarding, editor toolbar, reading selection pill.


### Update 2026-10-29, Vault (CI green `311f7de`)
- Vault grid follows image 07: equal gradient tiles, frosted icon chip, count at the bottom. Next: Appearance.
- Appearance (done 2026-10-30, 19.26): picture tiles for theme mode, theme and presets (mini screen drawn in each scheme's own colours), Liquid Glass row with a glass preview, one-line titles and one helper line. Next: Attendance, Timetable day view, AI Hub, Settings hub, profile.
- Attendance (done 2026-10-30, 19.29): hatched off days, stat tiles, capsule month chart (images 08 and 04). Next: Timetable day view (11), AI Hub as chat (05, 13), Settings hub, profile.
