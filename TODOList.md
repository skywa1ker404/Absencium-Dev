Absencium: plan and status

**What we are building.** Absencium is an offline-first Android study companion (Kotlin, Jetpack Compose, Material 3 Expressive, Room, minSdk 26). It tracks attendance, timetable, exams, holidays, tasks and reminders, and keeps notes and study material in a Vault of subject folders. A block-based note editor and a per-subject syllabus tree link notes, past papers and guides to topics. Optional AI (your own API keys) helps with extraction, notes and chat. Everything works without a network. There is no sync; sharing comes after 1.0. Built only by GitHub Actions in `skywa1ker404/Absencium-Dev`; the stable `Absencium` repo stays empty until you approve 1.0.0. GPL-3.0.

# Absencium TODO list

Single source of truth for what is done, in progress, pending and planned (it also holds what used to be `CURRENT.md`, merged on 2026-11-01). **Every specification given so far is merged here** (the master specification, the earlier roadmap, the improvements and features list of 2026-10-18, the Sharing and Sharewave specification, the repository audit request and the owner's later requests). New requirements go into the step they belong to in the Specification section; a requirement that changes is struck through and its replacement follows it.

**How this file is kept.** Updated after every CI build. A batch moves from *Next tasks* to *Current tasks* when it starts; when it finishes it moves to *Completed tasks* (with date `DDMMYY` and CI hash), *Partially Complete* (what remains and why it was postponed) or *Bugs fixed*. Current tasks are never dropped when new requests arrive. A replaced plan goes to *Cancelled plans*.

**Status words.** DONE means it compiled on CI (and is named with the commit). WAITING FOR CI means it is written and pushed but CI has not confirmed it. Nothing is marked DONE that has not compiled.

Last updated: 011126 (Batch 28 written, waiting for CI; last green build `dd2321b`, 011126). CI errors are read from the commit comment the workflow posts.

---

## Current tasks

### Batch 28: Absencium AI fixes, reading-mode search, content access with approval (owner 2026-11-01), WAITING FOR CI

Details of the row: 19.41 and 11.28, 11.36. References: `docs/ui overhaul/Ai/Generical-reference/` (pictures 05 to 19 with a per-picture inspection and the owner's feature list).

| # | Task | Status |
| - | --- | --- |
| 28.1 | Empty-page indicator (`ExpressiveEmptyState`) on the Drafts popup and the Search page | WAITING FOR CI |
| 28.2 | Selecting text in a note no longer scrolls the page to the top (editor and reading mode keep their scroll while the selection changes: `KeepScrollOnSelection`) | WAITING FOR CI |
| 28.3 | Reading mode: the Search icon finds the written words in the note (find bar, match count, previous / next, highlights, scrolls to the match, also inside tables); a separate Sparkle icon beside it asks the AI | WAITING FOR CI |
| 28.4 | Absencium AI empty page: greeting no longer overlaps the suggestions; on the empty page the new-chat icon is replaced by **Incognito chat** (not saved, no memory) | WAITING FOR CI |
| 28.5 | Chat text no longer runs under a solid block behind the top bar and the input area (content under the bars, soft fades) | WAITING FOR CI |
| 28.6 | "Send a note" picker rebuilt compact like the Vault (subject sections, search, ticks); the same picker for study material | WAITING FOR CI |
| 28.7 | Model chooser: compact bottom sheet, smaller text, fixed-height provider pills (no glitch when tapped), model search and fetch | WAITING FOR CI |
| 28.8 | Thinking glow replaced by the Material loading shape with a shimmering "Thinking" line | WAITING FOR CI |
| 28.9 | Sidebar slides in from the side and swallows touches, so the chat menu behind it cannot be clicked | WAITING FOR CI |
| 28.10 | The AI no longer says a subject needs attention at 75 % or more (each subject is tagged `[SAFE]` / `[BELOW REQUIREMENT]` against the student's own threshold; the suggestion is reworded) | WAITING FOR CI |
| 28.11 | Raw `{color:1565C0|cell division}` no longer shows in notes or AI chat: tags missing their `|}` closer are repaired when the AI writes, when a note loads and when it is shown; the prompt now insists on the closer | WAITING FOR CI |
| 28.12 | The AI can read the **content of notes and study material, with approval**: scope "Study material", action `read_content`, a card says what it wants to read, the content is sent only after Apply | WAITING FOR CI |
| 28.13 | Reference pictures 05 to 19 and the AI feature list saved in `docs/ui overhaul/Ai/Generical-reference/` with an inspection of every picture | DONE (docs) |
| 28.14 | Docs merged: `CURRENT.md` into this file in the new format | DONE (docs) |

## Next tasks

- **Batch 29: Reference-driven AI features** (rows 11.28 to 11.35; references `docs/ui overhaul/Ai/Generical-reference/README.md`, pictures 05 to 19): permission areas widened to Tasks, Reminders, Vault records, Trash, Drafts, saved chats and a "what was sent" view; edit / replace / delete of notes and records through confirm cards; embedded JavaScript runtime (QuickJS) for calculations, regex and parsing, off by default; memory page (modes, similarity slider, statistics, debugger, auto-summary, semantic embeddings); keyless / custom search providers (SearXNG, DuckDuckGo RSS, result size); thinking levels Off / Low / Medium (default) / High with Temperature, Top P, Max tokens; LaTeX, code highlighting, Mermaid; schema versioning and import validation; assistants editor as sub-pages with prompt variables, intros, clone and reorder; read aloud.
- **Batch 30: AI follow-ups from the Readme list** (rows 11.6 to 11.27 "pending" parts): message branching, chat PDF export, search inside one chat, embeddings retrieval from notes and guides, per-model parameters, favourites, failover outside chat, camera capture and previews, per-assistant allowed areas, interactive scored quiz, in-place note edit, page-pointing PDF answers, revision plan into the calendar, clash detection, `add_syllabus_node`.
- **Batch 31: Notification when an AI answer is ready** (5.6, 11.25; `AiChatService.completions` and `AiHubScreen(chatId)` are ready) with the notification groundwork of 5.6.
- **Batch 32: Settings hub, profile with the syllabus card (19.21), onboarding** (images 03, 10; onboarding still has its older option cards).
- **Batch 33: Editor toolbar as a connected toggle grid with an "Ask AI to..." pill (images 03, 14), reading selection pill, AI options popup restyle and tree-style topic picker (screens 17, 18).**
- **Batch 34: Attendance per-subject tinted rows and striped progress bar (images 08, 04, 13), Vault grid even tiles (image 07), remaining inline add / import buttons as floating buttons (Home and Vault empty states, Test Lab, Storage).**
- **Batch 35: Liquid Glass rest (19.1: press / drag squash and highlight on floating buttons, toggles, sliders, calmer tiles in popups, places ignoring "Apply frosted glass on other elements") and Material 3 Expressive (19.3: unread guide pages need pasted text, then the full component set).**
- **Batch 36: Step 10 leftovers**: markup tools saved in reading mode (10.26), audio play / pause in the mini-player (10.29), hide the mic while scrolling (10.28), Paste in the multi-select toolbar (10.32), "AI is disabled" messages and consent popup (10.34), subject folder sidebar / sort / card view (10.15), practical syllabus option and "generate notes for this topic" (10.16, 10.18), pages, doodle shapes and layers, text reflow beside media, syllabus-aware AI (10.36 onward), study material rename (needs a migration), list / card mode remembered as a setting.
- **Later, in the order of the Specification:** Steps 5 to 9 (platform integration, timetable overhaul, attendance and academic correctness, reminders / tasks / Quiet Mode, calendar and search), the pre-1.0 audit (Step 3), Steps 12 to 17 and the stable 1.0.0 release.

## Completed tasks

- **Batch 1: Step 0, Build, signing and release separation** (171026, Ci: 6834a09). Row by row in the Specification.
- **Batch 2: Step 1, Reported bugs and small UX fixes** (171026, Ci: 6834a09). Row by row in the Specification.
- **Batch 3: Step 1b, Licence review fixes before GPL-3.0 approval** (171026, Ci: 8099a04). Row by row in the Specification.
- **Batch 4: Step 1c, Resolved audit decisions** (171026, Ci: b6c0288, af7303a). Row by row in the Specification.
- **Batch 5: Step 1d, Reported on-device issues and release docs** (181026, Ci: 525d03b). Row by row in the Specification.
- **Batch 6: Step 1e, Pill drag, title and scrollbar corrections** (181026, Ci: 14c41fd). Row by row in the Specification.
- **Batch 7: Step 1f, Optional glossy glass** (181026, Ci: 1d33a15). Row by row in the Specification.
- **Batch 8: Step 1g, Popups, date picker, chips and glass lag** (181026 to 201026, Ci: 2fb6710, a38c782, 795afcc). Row by row in the Specification.
- **Batch 9: Step 2, Open bugs and checks on what already compiled** (201026, Ci: 795afcc). Row by row in the Specification.
- **Batch 10: Step 4, Data foundation: one Room migration and one export format** (201026 to 241026, Ci: 795afcc, a54febc). Row by row in the Specification.
- **Batch 11: Step 10, Vault, editor and file handling** (211026 to 271026, Ci: 24af814, a54febc, 045103c, 656a670, 8960989, 602e2b2, 20ab275, f116f26). Row by row in the Specification.
- **Batch 12: Liquid Glass first pass and every dialog on the app's own dialog (19.1)** (271026 to 281026, Ci: 25f76a2, a1a755e).
- **Batch 13: Layout overhaul start: notes, study material, syllabus tree, Trash, Subjects (19.4)** (281026, Ci: d56d315).
- **Batch 14: Keyboard gap, stacked styles rewrite, floating folder controls, topic menu on the subject name (19.7 to 19.10)** (281026, Ci: 79fbc9f).
- **Batch 15: Folder bottom pill, Home watch drawer, Attendance popup, screenshot bugs (19.11 to 19.14)** (291026, Ci: 91daba9).
- **Batch 16: Extended buttons that fold on scroll, editor header audit, one topic menu, selection toolbar, subject-only outline import (19.15 to 19.20)** (291026, Ci: 497da1f).
- **Batch 17: Vault grid (19.22)** (291026, Ci: 311f7de).
- **Batch 18: Empty-page badge, extract dialogs, study material viewer (19.23)** (291026, Ci: 804cda1).
- **Batch 19: Empty-state badge, documents read as pages (19.24)** (291026, Ci: 171fe1b).
- **Batch 20: Whole-PDF upload with wavy progress, buttons side by side (19.25)** (291026, Ci: 1b34fdf).
- **Batch 21: Appearance redesign (19.26)** (291026, Ci: 966c8b3).
- **Batch 22: AI failure bar and automatic retry (19.27)** (301026, Ci: e886ecb).
- **Batch 23: Reading / editor AI fixes and Attendance redesign (19.28, 19.29)** (301026, Ci: d73afc7).
- **Batch 24: Syllabus screen, topic sidebar, multi-source outline import, attendance and settings polish, Timetable day view (19.30 to 19.33)** (301026, Ci: fd87162, 93975a4).
- **Batch 25: AI Hub as a chat (19.34)** (301026, Ci: ed71aeb).
- **Batch 26: Bugs batch, Absencium AI, per-provider keys, share as PDF, AI roadmap (19.35 to 19.39)** (311026, Ci: 86482cd).
- **Batch 27: Absencium AI rebuild: background answers, smarter prompt, memory, search, assistants, attachments, failover, new chat screen (19.40, rows 11.6 to 11.27)** (011126, Ci: dd2321b).

**Bugs fixed**

- **Keyboard gap** under every text field low on the screen: `adjustResize` (19.7) (281026, Ci: 79fbc9f).
- **Stacked styles** piling up markers (bold, colour, size, line prefixes): new `InlineStyle` engine (19.8) (281026, Ci: 79fbc9f).
- **Screenshot bugs**: import dialog buttons squeezed, chosen day vs holiday colour, leftover markers in note previews (19.13) (291026, Ci: 91daba9).
- **Editor header**: colour / size written in front of the heading mark, hidden markers in the character count (19.17) (291026, Ci: 497da1f).
- **PDF import time-outs**: PDFs sent whole with progress, 10 minute wait (19.24, 19.25) (291026, Ci: 171fe1b, 1b34fdf).
- **AI errors not shown**, HTTP 503 / 400 on Gemini and Groq: failure bar, automatic retry, Groq guards (19.27) (301026, Ci: e886ecb).
- **AI answers as raw Markdown**, question bar behind the keyboard, checkbox inside a code block (19.28) (301026, Ci: d73afc7).
- **Syllabus import tree, empty provider replies, double AI errors, date picker behind the dialog, absent date, markup tokens cut in half, random empty shape, folder pill, drafts pull, FAB shadow, rotation reset** (19.35) (311026, Ci: 86482cd).

## Partially Complete

- **Liquid Glass (19.1):** press / drag squash and highlight on floating buttons, toggles and sliders; calmer inner tiles in popups; dialogs, menus and pickers that ignore "Apply frosted glass on other elements". Postponed behind the AI and screen work.
- **Material 3 Expressive (19.3):** the developer.android.com guide pages and the m3.material.io blog could not be read from this workspace; waits for pasted text. Components already in use are listed in `docs/ui overhaul/M3E_RESEARCH.md`.
- **Screen redesign (19.4):** done: Vault grid, notes, study material, syllabus, Home, Today, Trash, Subjects, Appearance, Attendance (calendar, tiles, 6-month chart), Timetable day view, AI chat. Left: see Next tasks, Batches 32 to 34.
- **Floating buttons (19.9):** done on the subject folder, Subjects, Syllabus. Left: Home and Vault empty states, Test Lab, Storage.
- **Study material (19.19):** rename needs a display-name column on guides (the file name is also the stored file's name), so it waits for a migration; list / card mode is remembered only while the screen lives.
- **Document reading (19.24, 19.25):** PDFs over 15 MB fall back to page pictures (first 36 pages); a page-range picker or the provider's file upload is needed.
- **Attendance (19.29):** per-subject rows and the striped bar are left.
- **Absencium AI (rows 11.6 to 11.27):** each row lists what is pending; the notification for a finished answer waits for the notification groundwork (5.6).
- **Step 10 (Vault and editor):** the items in Next tasks, Batch 36.
- **Frame drops (19.27):** the cluster at 19:51 is not pinned to a screen (needs the raw log); `onTrimMemory level=20` means "UI hidden", not memory pressure.

## Your actions needed

- Paste the text of the Material 3 guide pages (19.3) so the Expressive component set can be applied from verified sources.
- Decide the open items in `docs/MASTER_SPEC.md`: in-app updater, Operations card, power and thermal handling.
- Re-import the 89-page syllabus PDF to check the formatting (19.30); if AI requests still fail, send the debug log (the phone must be able to reach the provider's host).

## Project Rules

- Native Android, Kotlin, Jetpack Compose, Room (`Absencium.db`), Material 3 Expressive, Coroutines / Flow, DataStore; minSdk 26. 100% offline-first, no internet needed for core features, zero telemetry, zero ads, strict privacy; everything GPL-3.0 compatible.
- Design: Material 3 Expressive and springy animation on every new or changed element, including loading and empty pages (loading icons morph between Material shapes with haptics); themes Default, Monet, Liquid Glass (translucent glass with a soft sheen, no glow) and Color Presets always respected; refresh rate capped at 90 Hz. Open `docs/ui overhaul/README.md` before every UI task. Only the 3-button navigation pill keeps its design.
- Never assume an API or dependency exists: inspect the repository. Do not rebuild a feature that already exists; reuse the existing component.
- Halt on error: if CI, compilation or a test fails, stop, print the exact error, fix it or wait. Nothing is called done until it compiled on CI.
- Bug fixes come before new features; commit and run CI only after the known bugs of the batch are fixed. Development, CI and reports happen in `skywa1ker404/Absencium-Dev` (package `skywa1ker404.absencium.dev`); builds only through GitHub Actions. `skywa1ker404/Absencium` stays empty until a stable 1.0.0 is recommended, tested and explicitly approved by Anakin.
- Commits are authored `skywa1ker404 <aalaskar43@gmail.com>` with no co-author or session trailers.
- Replies are short and token-efficient; this file is updated after every CI build in the format above, keeps every specification detail, and never carries "not tested" remarks.
- A specification that was replaced is ~~struck through~~ and the new one follows it immediately. Steps in the Specification run in implementation order; each heading shows how many tasks it holds.

## Your Approval Required

- Approval of the repository audit report once it is written (Step 3).
- Approval of the cleanup table below, file by file.
- Keyless web search (11.31): which sites the app may contact (the backends are listed in the reference picture 18); nothing is added without your approval because of the zero-telemetry rule.
- New dependencies for Batch 29 (JavaScript engine, embeddings, syntax highlighting, Mermaid): licence and size reported first.
- The stable 1.0.0 release and publishing to `skywa1ker404/Absencium`.

## Cancelled plans

- **Sync, routine backup and cloud transport (Step 18, 4.6, 4.7):** removed 2026-10-22 at the owner's request; manual backup and restore stay.
- **Optional GPS attendance (12.6):** excluded; manual attendance is enough and no location permission is needed.
- **Reading themes and Eye comfort:** removed 2026-10-28 at the owner's request.
- **Home Subjects squircle beside the pill and the AI Hub button beside the pill (19.12, 19.15):** removed; the AI Hub is the third Home quick action and stays in the profile sheet.
- **The single "AI Hub" chat (19.34):** replaced by Absencium AI (19.36, 19.40), renamed 2026-10-31.
- **One API key for all providers:** replaced by per-provider keys (19.37).
- **Share formats other than PDF:** removed (19.38); Sharewave is greyed out until Step 17.
- **Topic dropdown in the folder (19.10):** replaced by the sidebar (19.31); the editor keeps the small dropdown.
- **Syllabus import by text only and 36-page batch reading of PDFs:** replaced by nested JSON (19.35) and whole-PDF upload (19.25).
- **Thinking glow animation in Absencium AI:** replaced by the Material loading shape (Batch 28).
- **`CURRENT.md`:** merged into this file on 2026-11-01.
- Older replaced items are struck through in the Specification (0.3, 1.2, 1.3, 1.7, 1b.2 to 1b.5, 1c.5, 1c.6, 1f.1, 2.3, 2.4, 6.2 to 6.6, 10.1, 10.8, 16.7).

---

## Specification (all steps, in implementation order)

Everything below is the detailed specification and per-task status. The sections above are the working view.


## Step 0. Build, signing and release separation (6 tasks)

| # | Task | Status |
| - | --- | --- |
| 0.1 | Dev application ID `skywa1ker404.absencium.dev`, stable ID `skywa1ker404.absencium`, chosen by `absenciumChannel` | DONE (CI green `6834a09`) |
| 0.2 | Dev signs with the public throwaway key `app/signing/absencium-dev.jks`; stable signs only from the four `ABSENCIUM_*` secrets | DONE (CI green `6834a09`) |
| 0.3 | ~~CI publishes a `build-N` prerelease on every push~~ CI builds, tests and uploads APK artifacts only; releases come from the manual `dev-release.yml` | DONE (CI green `6834a09`) |
| 0.4 | APK verification script (`verify_apk.sh`: ID, certificate, ABIs, SHA-256) | DONE (CI green `6834a09`) |
| 0.5 | Stable repo templates in `release/stable/` and `scripts/export_stable.sh` with a link check | written, export dry run pending |
| 0.6 | **Bug:** the "Identity and secret guard" matched its own pattern list in `android.yml` and failed `assemble-debug`. Fixed by excluding `.github` and `release` from the scan; error comment no longer empty | DONE (CI green `6834a09`) |

## Step 1. Reported bugs and small UX fixes (8 tasks)

| # | Task | Status |
| - | --- | --- |
| 1.1 | Default Theme description no longer says "Everforest" | DONE (CI green `6834a09`) |
| 1.2 | ~~Default colour engine: Monet~~ the app starts on the Default Theme; it can still be changed in onboarding | DONE (CI green `6834a09`) |
| 1.3 | ~~Restore card: "Close app"~~ "Restart app" relaunches Absencium so the staged restore is applied | DONE (CI green `6834a09`) |
| 1.4 | Liquid Glass options greyed out and not tappable in Performance mode (onboarding tile and panel, Appearance settings switch, blur slider, "Apply frosted glass on other elements") | DONE (CI green `6834a09`) |
| 1.5 | Calendar month forward/back: short fade-through (90 ms out, 210 ms in) while the card animates its height | DONE (CI green `6834a09`) |
| 1.6 | Tap "Month Year" in the calendar header: centred popup with year arrows and a month grid | DONE (CI green `6834a09`) |
| 1.7 | ~~Plain platform date dialogs~~ date chooser is an app popup that follows "Apply frosted glass on other elements" (used by the holiday/exam editor and Add Entry custom date) | DONE (CI green `6834a09`) |
| 1.8 | About: website link https://skywa1ker404.github.io/apps/absencium/ | DONE (CI green `6834a09`) |

## Step 1b. Licence review fixes before GPL-3.0 approval (8 tasks)

| # | Task | Status |
| - | --- | --- |
| 1b.1 | 16 untraceable icons replaced with Google's Material path data (Apache-2.0); Draft uses `edit_note` | DONE (CI green `8099a04`) |
| 1b.2 | ~~AI-generated `hero.png` and `showcase.png`~~ original illustrations (`docs/art/`, rendered by `render.py`), labelled as illustrations | written, images checked by eye; no code involved |
| 1b.3 | ~~Obtainium's badge image~~ plain text badge drawn for the project | written |
| 1b.4 | ~~Monokai Pro preset~~ removed (21 presets); stored selection falls back to the default preset | DONE (CI green `8099a04`) |
| 1b.5 | ~~YouTube captions: unofficial endpoint opt-in switch~~ superseded by 1c.1 | DONE (CI green `8099a04`) |
| 1b.6 | Third-party notices, README licence paragraph, `LICENSE_AUDIT.md` updated; notices no longer link dev-only files | written |
| 1b.7 | Stable export dry run: file set builds, link check passes except `LICENSE` (expected) | DONE for the file set; stable `README.md` drafted (see Step 16) |
| 1b.8 | Decisions needed from Anakin: authorship wording, logo ownership, keep or drop the captions switch, old keys | OPEN |

## Step 1c. Resolved audit decisions (8 tasks)

| # | Task | Status |
| - | --- | --- |
| 1c.1 | YouTube option (b): unofficial caption fetch and its setting removed; Gemini link reading kept; paste-the-transcript box for other providers; tile and screen renamed "YouTube Video Notes" | DONE (CI green `b6c0288`) |
| 1c.2 | Authorship sentence added to the README FAQ and licence section (not a substitute for the GPL text) | written |
| 1c.3 | `absencium-upgrade.jks` removed from the tree, `.gitignore` blocks keystores, passwords out of the docs, migration plan for friends' APKs in `docs/SIGNING.md`; history not rewritten | DONE (CI green `b6c0288`) |
| 1c.4 | Stable CI guard: commits must be authored by skywa1ker404 (staged in `release/stable`, not published) | written, runs only after publishing |
| 1c.5 | Logo provenance recorded (GPT, by the owner's account); OpenAI terms read. ~~Original replacement proposed~~ Owner decided to keep the current logo | DONE (documents) |
| 1c.6 | ~~Contributor sentence only explained~~ Contributor-licence sentence applied to `CONTRIBUTION.md` | DONE (text; CI green `af7303a`) |
| 1c.8 | In-app "Open-source licences" screen (About, Legal) with notices copied into assets; CI checks the copies match `docs/` | DONE (CI green `af7303a`; not opened on a device) |
| 1c.7 | `LICENSE_AUDIT.md` and notices updated; source scanner could not run (no PyPI), so verification is incomplete | DONE (documents) |

## Step 1d. Reported on-device issues and release docs (7 tasks)

| # | Task | Status |
| - | --- | --- |
| 1d.1 | Navigation pill: page text no longer shows garbled through the pill and the selection bead (denser veil, light 4 dp softening); the bead's lens refraction is kept; label fades in only when fully open | DONE (CI green `525d03b`) |
| 1d.2 | AI & Integrations: API style chips wrap instead of squeezing "Google Gemini" into single letters | DONE (CI green `525d03b`) |
| 1d.3 | Page headers: title area nearly opaque so scrolled text cannot bleed through it | DONE (CI green `525d03b`) |
| 1d.4 | Long lists: licences screen and subject folders are lazy lists with a scrollbar on the licences screen; search waits for a typing pause and scores only the first 20,000 characters of a note; model filter remembered. Other screens with long text not yet profiled | DONE for these screens (CI green `525d03b`); lag on other screens (for example very long notes) **still open, not measured** |
| 1d.5 | "No colour" preset (neutral greys), listed first and offered only while Liquid Glass is on; with glass off it falls back to the default theme | DONE (CI green `525d03b`) |
| 1d.6 | Stable export now includes `LICENSE`, the stable README and the images it links; link check skips the bundled notice copies; export fails on a broken link or a keystore | DONE in a temporary directory (not CI) |
| 1d.7 | Dev README, stable README (`release/stable/README.md`) and `release/stable/release-notes/v1.0.0.md` drafted from source-checked facts; the release notes still need the certificate fingerprint pasted in | written, awaiting Anakin's review |

## Step 1e. Pill drag, title and scrollbar corrections (5 tasks)

| # | Task | Status |
| - | --- | --- |
| 1e.1 | Pill look restored exactly as before step 1d (veil, lens bead, label fade, header fades); the 1d.1 and 1d.3 changes are withdrawn at the owner's request | DONE (CI green `14c41fd`) |
| 1e.2 | **Bug:** dragging the selection pill to the last tab made it shake. Cause (worked out from the geometry, not measured on a device): the pill is centred and narrows as the last tab opens, so its left edge moves under the finger, and the drag position was measured from that edge; near the end the highlight's on-screen position even runs backwards for a few pixels. Fix: measure from the pill's centre and place the finger between each tab's resting spot, which only rises | DONE (CI green `14c41fd`); **needs your device check** |
| 1e.3 | Licences scrollbar can be dragged (28 dp strip at the right edge) | DONE (CI green `14c41fd`) |
| 1e.4 | Liquid Glass look: soft sheen on glass cards and a fourth colour plus a light streak in the glass wallpaper, after the reference pictures; the pill and floating bars are untouched | DONE (CI green `14c41fd`) |
| 1e.5 | Docs-only push test: a documentation-only commit (this file) pushed without `[skip ci]` to see whether CI stays idle | DONE: CI stayed idle for docs-only commit `8068ba0`; the next code push `14c41fd` started a run |

## Step 1f. Optional glossy glass (1 task)

| # | Task | Status |
| - | --- | --- |
| 1f.1 | ~~Settings, Appearance: "Glossy glass" switch, shown only while Liquid Glass is on, **off by default**; adds a shine band and a glint to glass cards and buttons (not to the pill or floating bars)~~ **superseded: the option is removed (10.20)** | DONE (CI green `1d33a15`), removal pending |

## Step 1g. Popups, date picker, chips and glass lag (7 tasks)

| # | Task | Status |
| - | --- | --- |
| 1g.1 | Search and + button square artefact: the slide-away no longer fades (a fade made the glass blur show as a pale square) | DONE (CI green `2fb6710`) |
| 1g.2 | Year list of the date picker had no background (Add Entry for the calendar and for notes): container is now opaque | DONE (CI green `a38c782`) |
| 1g.3 | Lag in popups in Liquid Glass (Add Entry, AI import confirmation list): the glass pane is now its own layer under the content so scrolling, toggling and typing do not redo the blur. Cause is reasoned from the code, not measured | DONE (CI green `a38c782`) |
| 1g.4 | "College Holiday" chip wrapped onto two lines: chips wrap as a row and labels stay on one line | DONE (CI green `a38c782`) |
| 1g.5 | Liquid Glass lag everywhere: cards (`glassSurface`) no longer blur. They only ever sampled the smooth wallpaper, so the blur was invisible but cost a blur pass per card per frame; the glass look, sheen and vibrancy are kept. The floating pill, bars and popups keep their real glass. Cause is reasoned from the code, **not measured** | DONE (CI green `795afcc`) |
| 1g.6 | Present/Absent switch: the translucent selection that showed behind the animation and vanished afterwards came from the highlight being a backdrop-sampling glass control inside a fading, scaling layer. It is now a flat tinted fill with the same sheen. Reasoned from the code, since the recording cannot be seen | DONE (CI green `795afcc`) |
| 1g.7 | Lag in the AI model selector list in Liquid Glass: its rows are plain fills; the card around it was the blurred part, so 1g.5 should cover it. Not separately changed or measured. If it still lags, the next step is to make the list lazy (it composes up to 40 rows at once) | DONE with 1g.5, **needs a device check** |

## Step 2. Open bugs and checks on what already compiled (7 tasks)

Bugs come before features. Everything marked DONE above compiled on CI but has not been tried on a phone.

| # | Task | Status |
| - | --- | --- |
| 2.1 | Device check of the CI-green changes: pill drag to the last tab (1e.2), licences screen and scrollbar drag, API style chips, No colour preset, glossy glass | pending |
| 2.2 | **Bug:** Present / Absent Today buttons: the ghost selection behind the animation is fixed in 1g.6 (owner described it on 2026-10-19). Double taps are also ignored for 450 ms (2.7) | DONE (CI green `795afcc`) |
| 2.3 | ~~Navigation pill label clipping~~ the owner prefers the current pill look (1e.1); re-open only if the clipping is seen again after the drag fix | pending |
| 2.4 | ~~Garbled text under the glass navigation bar~~ withdrawn the same way; the title-area and veil changes of 1d.1 / 1d.3 were reverted | superseded |
| 2.5 | Lag with a lot of text or many entries on screens other than the licences screen, subject folders and search (for example typing in a very long note): measure on the device first, then fix the measured cause | pending |
| 2.6 | Prevent stale saves overwriting newer edits, make the save-status indicator accurate, predictable draft creation and deletion (data-loss class, from the reliability list) | **partly done** (CI green `795afcc`): saves of notes now run one at a time in request order, and saving a note that was deleted while its editor was open updates the Trash row instead of inserting a duplicate; drafts are deleted at once with their files, other entries go to the Trash. **Not done:** the save-status indicator and a check for a note changed elsewhere |
| 2.7 | Rapid-tap guard: prevent duplicate actions from double taps on save, delete, mark present/absent | **partly done** (CI green `795afcc`): every `PillButton` and both halves of the Present/Absent switch ignore a second tap within 450 ms. **Not done:** other clickable rows and icon buttons that save or delete |

**Tests:** Manual device script; a regression test for each fixed bug where logic is involved. · **Effort:** S to M

## Step 3. Audit of the candidate repositories (audit only, no code) (4 tasks)

Nothing is copied or added until this report is approved. It decides which proven designs to follow and which licences rule code reuse out.

| # | Task | Status |
| - | --- | --- |
| 3.1 | Inspect Absencium-Dev first (source, this file, `docs/MASTER_SPEC.md`, `docs/ROADMAP.md`, `docs/SPECIFICATION.md`) and list existing, partial, planned and missing features | pending |
| 3.2 | **Draft written** (licences read from each repo's own file; features traced to source paths; dependencies, bundled assets and file headers not audited, ScanCode not run). Audit 16 repositories: ImageToolbox, June, Kori, Reef, Tomato, FaceDown, Self-Attendance, baca., LastChat, Damier, bubble-notice-android, WikiReader, yearly-progress, Keshu, Etar Calendar, Agora. For each: README, source, actual LICENSE files (check version differences), dependencies, assets, the inspected commit, unresolved uncertainties. Special licence checks: Yearly Progress AGPL/GPL discrepancy, Damier Commons Clause, Reef additional terms, Kori licence, FaceDown and Self-Attendance reuse permission. No code with unclear permission is copied; AGPL code is not assumed to become GPL-only | pending |
| 3.3 | Comparison matrix (repository, verified licence, feature, existing equivalent, benefit, difficulty, risks, recommendation) and a recommended / deferred list per area (Vault, AI Hub, Attendance, Reminders, Tasks, Calendar, focus tools, widgets) | draft in `docs/FEATURE_AUDIT.md` (feature table and recommendations); awaiting your review |
| 3.4 | Save the report as `docs/FEATURE_AUDIT.md`, update the steps below only where it changes them, and wait for approval | draft saved; **waiting for your approval** |

**Tests:** Every claim cites a file or commit; anything only read from a README is labelled as such. · **Effort:** L (research only) · **Gate:** Anakin approves the report

## Step 4. Data foundation: one Room migration and one export format (7 tasks after removing 4.6 and 4.7)

Almost every later step (undo, backup, sharing, widgets) needs stable ids, change times, tombstones and a versioned export. Doing it once now means the later steps add code instead of rewriting it.

| # | Task | Status |
| - | --- | --- |
| 4.1 | Room version 2 as a real `Migration` with an exported schema (never destructive): stable `uuid` per syncable row, `updatedAt` revision, `deletedAt` tombstone, plus the `teacher` column the timetable needs. Later versions add columns the same way | DONE (CI green `795afcc`). The migration SQL was run in SQLite on a copy of the v1 schema and its result is identical to the v2 schema Room exported (columns, types, defaults, indices, foreign keys). `updated_at` is also kept current by database triggers. **Not run on a phone**, and there is no instrumented migration test yet (see 4.8) |
| 4.2 | Trash and undo: deleted rows are tombstoned, shown in a Trash screen, restorable, purged after a set time; the undo toast of the timetable work uses it | DONE for notes, exams, holidays and lectures (CI green `795afcc`): Trash screen (opened from the trash icon in the Vault; removed from Settings on 2026-10-22), 30-day purge at start-up, undo bar. Subjects and attendance entries are **not** trashed (deleted at once) |
| 4.3 | Versioned export format: manifest (format version, app version, schema version, created, counts, SHA-256 per file), the database, attachments, settings without secrets; import validates manifest and hashes first | DONE (CI green `795afcc`). Manifest rules are covered by JVM unit tests that CI runs; the manifest JSON reader/writer uses `org.json` and is **not** unit-tested |
| 4.4 | Backup and restore on top of it: attachments included, restore validation, duplicate detection, interrupted-import recovery, low-storage check, backup status shown in Settings | DONE (CI green `795afcc`): staged restore with rollback, "same as current" detection, low-storage check, last-backup line, restore preview. **Not run end to end on a phone**; there is no automated export-wipe-import test (it needs Android) |
| 4.5 | Optional password encryption of backups (AES-256-GCM, key derived with a reviewed library, no hand-written crypto) with a threat model written in `docs/PRIVACY.md` | DONE with a deviation (CI green `795afcc`): PBKDF2-SHA256 and AES-256-GCM come from the Android platform, not a separate library, and the 64 KiB chunk framing around them is our own code. That framing is unit-tested but **not independently reviewed**; the limits are written in `docs/PRIVACY.md` |
| 4.6 | ~~Rolling local snapshots (3)~~ **Removed 2026-10-22 on the owner's request** (interval backup): the daily snapshot, its Settings switch and its list are gone. The manual backup and restore of 4.4 stays | REMOVED (CI green `a54febc`) |
| 4.7 | ~~Seams for later steps (Transport, change-set builder, read-only scope)~~ **Removed 2026-10-22** with all sync code; Step 17 builds what it needs when it starts | REMOVED (CI green `a54febc`) |
| 4.8 | Test infrastructure: Room migration tests, export/import round-trip tests, preference-recovery test; CI runs them | **partly done** (CI green `795afcc`): JUnit 4 added; JVM tests for the encryption (round trip, wrong password, bit flips, truncation, reordering), manifest rules, Trash retention and the local transport run in `testDebugUnitTest` on CI. CI's log is not readable from here, so the test count is not confirmed beyond the step passing. **Not done:** Room migration test (needs an instrumented runner), full export/import round trip, preference-recovery test |
| 4.9 | Safe preference recovery (corrupt DataStore falls back to defaults) and preserve data when subjects are renamed or moved | **partly done** (CI green `795afcc`): a corrupt DataStore file is replaced with defaults and logged. Rename/move safety is **not verified**: rows refer to subjects by id, so a rename should keep them, but no test or check was done |

**Tests:** Migration test from the exported v1 schema; round-trip test (export, wipe, import, compare); a tampered manifest is rejected. · **Effort:** L

## Step 5. Android integration base (9 tasks)

Notifications, locale, time zones and process-death recovery are needed by reminders, widgets and translations, so they come before them.

| # | Task | Status |
| - | --- | --- |
| 5.1 | Per-app language: system language by default, in-app selector (`LocaleConfig`), English fallback, locale-aware dates, times, numbers and decimals, 12/24-hour follows the system, configurable week start, correct weekday and month names | pending |
| 5.2 | Move every hard-coded string into resources (new screens use resources from this step on); RTL layout check; Unicode and emoji safe text handling | pending |
| 5.3 | One clock / time-zone provider used everywhere (reminders, events, midnight and month/year boundaries, leap day) | pending |
| 5.4 | Edge-to-edge insets, camera cut-out and gesture navigation, Predictive Back, reliable system Back, unsaved-change confirmation, keyboard-aware layouts | pending |
| 5.5 | Screen-state restoration and process-death recovery for forms, editors and the timer | pending |
| 5.6 | Notification groundwork: channels per purpose, POST_NOTIFICATIONS handling with a denied-permission explanation, exact-alarm decision (inexact by default, exact only when the user opts in), reboot rescheduling receiver, per-API feature table in `docs/ROADMAP.md`. **Notifications section, owner 2026-10-31: AI answer ready.** When an Absencium AI answer finishes while the student is not looking at that chat (`AiChatService.completions` already emits `ChatCompletion(chatId, title, preview, ok, seen=false)`), post a notification on an "AI answers" channel (title = chat title, text = start of the answer, nothing when `seen`); tapping it opens `AiHubScreen(chatId)` on that chat (the screen already takes an optional `chatId`) and clears the unread dot. Needs POST_NOTIFICATIONS handling first; `AiWorkService` already shows the required "working" notification while answering | pending |
| 5.7 | Permission-denial handling pattern (one reusable explainer) and graceful revoked-storage handling | pending |
| 5.8 | Custom HEX palette creation that respects Performance mode and reduced motion; all existing themes preserved | pending |
| 5.9 | Reduced-motion setting wired into every spring and transition; optional subtle haptics setting; respect system refresh rate, battery saver and thermal state (no forced refresh rate) | pending |

**Tests:** Switch language, theme, font size, display size and 12/24 h on a device; rotate; kill the process mid-edit; deny each permission. · **Effort:** L

## Step 6. Timetable overhaul (12 tasks)

Unchanged from the earlier plan; it needs the migration of Step 4 (teacher column).

| # | Task | Status |
| - | --- | --- |
| 6.1 | Teacher (optional) when adding or editing a lecture | pending |
| 6.2 | ~~Start time as a text field~~ Start time is a button that opens a frosted wheel picker (HH : MM, AM/PM for 12 h, 1–24 for 24 h, infinite scroll, minutes 0–59). Required | pending |
| 6.3 | ~~End time~~ Lecture duration is an optional button, shown only after a start time is chosen; wheel of 0–12 h and 0–59 min, default 0:00, no AM/PM; both buttons re-open to edit | pending |
| 6.4 | Subject chosen from a dropdown, ~~horizontal chips~~ | pending |
| 6.5 | "Add a lecture on Monday" becomes a button that opens a popup with a day button (phone's first day by default) and a dropdown (Mon–Fri or Mon–Sun by the app setting) | pending |
| 6.6 | Edit (pen) and delete icons on entries, ~~"Edit" / "Remove" text~~ | pending |
| 6.7 | Delete asks for confirmation, then a toast "Entry deleted" with Undo for about 4–5 s (uses the Trash of Step 4) | pending |
| 6.8 | Swipe an entry left to delete | pending |
| 6.9 | "Import Lecture Timetable" with the same popup style as the other two imports | pending |
| 6.10 | After AI scanning: tick options for room number, teacher name, and time gaps between classes as duration (yes/no) | pending |
| 6.11 | A/B (alternating) schedules, replacement classes and reliable schedule changes | pending |
| 6.12 | Docs and changelog for the above | pending |

**Tests:** Add, edit, delete, undo and swipe on a device; A/B week switch across a month boundary. · **Effort:** L

## Step 7. Attendance and academic correctness (10 tasks)

These are the numbers students rely on, so they are fixed and tested before new features read them.

| # | Task | Status |
| - | --- | --- |
| 7.1 | Zero classes never divides by zero; correct rounding; recalculation after every edit; configurable institutional target (default 60–85 % slider already exists) and a per-subject override | pending |
| 7.2 | Statuses: present, absent, cancelled, extra class and bunked, counted correctly; holidays and cancelled classes excluded; weekends, custom holidays, unexpected closures and duplicate holidays handled | pending |
| 7.3 | Weighted lectures and practicals; optional attendance units (class duration divided by a chosen time increment) | pending |
| 7.4 | Safe-absence projections and bunk buffer per subject, with per-subject history; cancelled and bunked counts in analytics | pending |
| 7.5 | Semester dates (optional, track by semester), archive manager, historical comparison, semester-end projection | pending |
| 7.6 | Past attendance: type how many days you were present since a date and it fills backwards from yesterday using the weekly timetable (with a preview before saving) | pending |
| 7.7 | Undo and historical corrections that keep old records; correct month/year boundaries and leap years | pending |
| 7.8 | Cross-feature what-if planning (absences, extra classes, exams) built on the same calculator, not a second one | pending |
| 7.9 | Profile and attendance logs: circular progress cards, Home cards and the clover button open the log popup; drag up about 85 % for full screen | pending |
| 7.10 | Minutes-before reminder for the next class (setting) once Step 8 lands | pending |

**Tests:** Unit tests for every formula (zero, rounding, holidays, leap day, midnight); the existing records must give the same percentages before and after. · **Effort:** L

## Step 8. Reminders, tasks and Quiet Mode (6 tasks)

Needs the notification groundwork of Step 5 and the attendance logic of Step 7.

| # | Task | Status |
| - | --- | --- |
| 8.1 | Tasks and reminders with smart filters, recurring items, snooze, quick actions, past-deadline handling, duplicate-notification prevention, quiet hours, notification preferences; the "AI answer ready" notification of 5.6 is listed in notification preferences | pending |
| 8.2 | Reboot-, time-zone- and update-safe scheduling (re-registers after boot, time change and app update); battery-conscious, no polling | pending |
| 8.3 | Quiet Mode: one-tap on, essential notifications you choose, a visible indicator, optional automatic exit; shares its settings, timer and statistics with Focus Guard and the study timer instead of copying them | pending |
| 8.4 | Notification actions per Android version: classic actions everywhere; newer features (Android 16 progress / Live Updates and anything newer) only where the version supports them, with a fallback; confirm Android 17 specifics against developer.android.com before building | pending |
| 8.5 | Eisenhower task view and Deadline Radar | deferred to 1.x (owner decision 2026-10-18) |
| 8.6 | "Next up" opens the lecture details; the next lectures are an expandable dropdown there | pending |

**Tests:** Reboot, force-stop, time-zone change, denied permission, airplane mode, DST-free and midnight cases; no duplicate notifications. · **Effort:** L

## Step 9. Calendar, search and navigation (6 tasks)

| # | Task | Status |
| - | --- | --- |
| 9.1 | Calendar views (list/grid, schedule, day, 3-day, week, month), today's entry first, optional auto-delete of past entries, tap for details, long-press menu, week-start preference | pending |
| 9.2 | Top-left tick icon in a non-pointy Material 3 Expressive polygon opens the To-Do list; Today icon; search icon; three-dot menu with a sidebar for view modes and categories | pending |
| 9.3 | ICS import and export with preview, duplicate detection, conflict handling, time-zone-safe scheduling | pending |
| 9.4 | Main search popup with a horizontally scrolling category row (All, Notes, Calendar, later AI Chat); language-aware search; filters; responsive on large databases | pending |
| 9.5 | Replace the remaining custom dropdowns with `InlineDropdown`; consistent empty states, loading states and error messages | pending |
| 9.6 | AI can add day-by-day calendar entries from uploaded media | pending |

**Tests:** Import a real .ics with repeats and zones; search a database of thousands of rows without a visible stall. · **Effort:** L

## Step 10. Vault, editor and file handling (22 tasks)

The editor and file tools come before study tools, widgets and sharing because those all read notes and attachments.

| # | Task | Status |
| - | --- | --- |
| 10.1 | Text Notes editor: Markdown engine, templates, related-note links, reliable autosave with an accurate indicator. ~~Tags with autocomplete~~ the tag editor is **removed from the editor** (2026-10-22; the stored field stays so old notes keep their tags) | superseded by the live editor 10.8 to 10.13 and 10.21 |
| 10.2 | Vault as a file explorer (breadcrumbs, badges, long-press multi-select, batch actions), drafts folded in; duplicate filenames, deleted subjects, empty folders, large images, inaccessible files and low storage handled | first version DONE (CI green `24af814`): breadcrumbs, badges, long-press multi-select with Finish/Move/Delete/Share, drafts in the folder, empty-folder text. Not done: large-image and inaccessible-file handling. **The folder page is redesigned by 10.15**, which replaces this layout |
| 10.3 | Local OCR (Tesseract with downloadable language packs, separate from AI), PDF text extraction, EXIF scrubbing on export; blurry and rotated image handling, unsupported-language message, editable OCR preview | **unblocked 2026-10-21**: you named tesseract-ocr/tesseract (Apache-2.0, verified on its GitHub page). That is the C++ engine, so the app uses the Android wrapper Tesseract4Android (adaptech-cz, Apache-2.0, minSdk 21, bundles Tesseract 5.5.1 and Leptonica; coordinates `cz.adaptech.tesseract4android:tesseract4android:4.9.0` from JitPack, read from its README). To do at build time: confirm CI can resolve it from JitPack, check the bundled Leptonica, libjpeg and libpng licences, and add all of them to `docs/DEPENDENCY_LICENSES.md`. Language data files are downloaded by the user, not bundled. EXIF scrubbing on export does not need it and is planned with 10.11 |
| 10.4 | "Extract Text" popup (With AI or Locally); finish the Add Entry actions; link missed classes to notes and attachments | **Add Entry actions CI green `a54febc`**: Text Notes (empty note, opens the editor), Document / File (any file, listed under the note and opened in another app), Audio Note (recorder with the microphone permission, shows in the note with play), Solve Homework (typed question and/or photos to your AI, answer saved as the entry), Generate AI Notes (topic to your AI). **Not done:** the "Extract Text" popup with the local option (needs 10.3), linking missed classes |
| 10.5 | Timetable OCR into the editable import preview of Step 6 | pending |
| 10.6 | Note export and share: PDF, Markdown, ZIP, text, through Android's Sharesheet with a `FileProvider`, in a small reusable `ShareService` | PDF, Markdown, text and ZIP DONE earlier (CI green `24af814`). **PDF now follows the page (CI green `a54febc`):** headings, lists, alignment, colours, sizes, highlight, quotes, code, tables with borders, and pictures and doodles at their size and alignment; recordings are left out. **Not done:** ZIP export still writes the note text with the `{{image:..}}` lines and does not strip photo metadata (EXIF, with 10.3) |
| 10.7 | Audio memos | **CI green `a54febc`:** recorder (AAC .m4a), the Audio Note stage in Add Entry, and an inline player in the note. **Not done:** timestamps inside notes, voice-to-text |
| 10.8 | ~~Editor modes: Edit and Preview, check mark goes to the preview~~ **Live editor (owner spec 2026-10-22):** one always-editable page that already looks exactly like the preview (headings, lists, check boxes, colours, sizes, alignment, pictures, doodles and tables), no separate preview mode, formatting characters hidden; tapping a check box flips it, tapping a [[link]] opens the note | CI green `a54febc` |
| 10.9 | Smaller header: a smaller title and one small line with date, character count, category and save state. The category says "Uncategorized" until the note is linked to a syllabus topic (needs 10.16) | header CI green `a54febc`; category picker pending |
| 10.10 | Bottom toolbar of icon-only buttons (Text style, Alignment and lists, Checklist, Add pictures, Add more); **every tap puts the keyboard away** so the page and panels can be seen. Text style panel tabs: Style, Symbols, Dividers, Templates; lists panel: left, centre, right, bullets, numbers, letters, quote; Add panel: Table, Doodle, Link a note, Code block. Formatting: bold, italic, underline, strike, highlight, code, colour, size, headings. **Not done:** the AI button, scan document and recording from inside the note, the paper and page adjustment | CI green `a54febc` (icons are drawn from path data written by hand, so they may need a visual pass on a device) |
| 10.11 | Notes are made of blocks: text, pictures, tables, doodles, recordings. Pictures are inserted at the cursor as their own block; tap one to move it up or down, set small, medium, large or full width, align it, crop and rotate (the crop editor), view it full screen or remove it; a round handle drags the width. Tables open a cell editor with add and remove row and column. A thin strip between two blocks adds a line to type on. **Not done:** text flowing beside a picture, Copy / Share / Save of a single picture | CI green `a54febc` |
| 10.12 | Doodle canvas: pen, eraser, six colours, size slider, undo and redo, plain, ruled or squared paper; a saved doodle is a PNG that can be drawn on again | CI green `a54febc` (opacity, colour spectrum and a stylus pressure option are not done) |
| 10.13 | More menu: Notes info, Share or export, Mark as finished, Delete (to the Trash). Undo and redo are in the top bar. **Not done:** page adjustment (paper style, texture, margin); Reminder, Lock and Generate widget (added when Steps 8, 14, 13 exist) | CI green `a54febc` |
| 10.14 | AI in the editor: the AI button opens a prompt bar where you type your own instruction and send; AI creation actions (Summary, Polish, Shorten, Extend, Continue writing, Revise grammar, Extract to-dos, Translate and the like); the three-dot menu of the AI panel opens the AI history. Needs the AI settings to be set up; works with the providers already in the app | pending |
| 10.15 | **Subject folder redesign (owner spec 2026-10-21).** Header shows the subject name with an arrowhead that opens a sidebar: All notes (every kind: absent, normal and others), Absent notes (ordered by which to write next, with finished ones marked), and a filter by syllabus topic, chapter or unit. A sort menu with list and card view. The + button on All opens the editor at once; on Absent it opens the date selector and then the editor. Search and + sit where the main page has them. Bottom: the same pill as the main menu (same animation and theme) with three tabs: Notes, Syllabus (this subject), Study material. The AI button sits to the right of the pill when AI is set up | PARTLY DONE (CI green `045103c`): inside a subject there are now Notes, Study material and Syllabus tabs and a filter by topic (unit, chapter or topic, including everything inside it). **Not built:** the sidebar, Absent notes ordering, sort menu and card view, the pill-style tabs; these are replaced by the layout redesign of Step 19 |
| 10.16 | **Syllabus (moved into 1.0 by the owner; was 12.3).** Per subject a tree: unit, chapter, topic, subtopic, with status (not started, in progress, completed) and progress. Import with AI from a photo, PDF or text, and ask AI to generate notes for a topic. A main Syllabus button on the Vault page's + menu shows every subject (reference layout from the owner, in the app's theme). Needs database version 3 | DONE except AI notes per topic (CI green `045103c`): per-subject tree (unit, chapter, topic, subtopic), status that cycles on tap (a unit or chapter sets everything inside it), progress, add, rename, details, move up and down, delete (filed notes go back to Uncategorized), import from typed text or a pasted page ("Tidy with AI") or a file read with the AI, a Syllabus overview of every subject from the Vault + menu. Database version 3 already holds the table. **Pending:** "generate notes for this topic" (with 10.36) |
| 10.17 | Study material tab per subject: PYQ, answer book, guide (extends the existing Guides and Question Banks) | DONE (CI green `045103c`): Study material tab groups previous year papers, answer books, guides and syllabus documents, each filed under a topic; adding uses the existing Guide / Answer PDF entry |
| 10.18 | Everything connects: notes, PYQs and study material can be linked to a syllabus topic, chapter or unit (the Uncategorized picker); the folder sidebar filters by it; a syllabus summary card in the profile (with the profile overhaul in Step 14) | PARTLY DONE (CI green `045103c`): a note is filed from the editor menu ("File under a topic"; the header and Notes info show the real category) and a guide from its card; the folder filters by topic. **Pending:** syllabus summary card in the profile (Step 14) |
| 10.19 | Vault page: a trash icon opens the Trash | DONE (CI green `656a670`/`02e5d73`) |
| 10.20 | Remove the "Glossy glass" option from Liquid Glass settings | DONE (CI green `8960989`) |
| 10.21 | **Owner spec 2026-10-22:** remove the tag option; no difference between editing and preview; every functional button an unmistakable icon instead of text; a slightly smaller editor header and info; any toolbar tap minimises the keyboard; keep this long TODOList; remove everything related to sync and interval backup; PDF export follows the note exactly (pictures, doodles, tables, everything except audio); finish the other Add Entry actions; remove Trash from Settings; update docs | CI green `a54febc` (all parts written; none confirmed on a phone) |
| 10.22 | Trash removed from the Settings hub (the Vault's trash icon is now the only way in) | CI green `a54febc` |
| 10.23 | **AI generation (owner spec 2026-10-24/25):** answers use the editor's real formatting (headings, tables, comparison tables, lists, checklists, highlights, colour, centred Unicode formulas), choose sections per topic, never show code or LaTeX, sound human. Add Entry options: Notes length dropdown (Moderate / Lengthy / In Depth), Kind of notes (Full note, Key details line by line, Summary, PYQ, Last-minute preparation, Practical observation notes), link to the subject's syllabus tree (unit / chapter / topic), course / year / semester, Tables and graphs, Add previous year questions, Fetch diagrams from the internet (Wikimedia Commons, optional), Generate diagrams with AI (optional), Assignment format for homework. **What the user typed always wins** | pushed `99839b5`, waiting for CI. **Not done:** practical syllabus tree, voice input on prompt fields, same options inside the editor AI panel |
| 10.24 | Extract text / transcribe everywhere: picture, PDF, Word, text and audio files, "With AI" or "Locally", PDF viewer, icon-grid attachment menus (2 or 3 columns), remove attachment | pushed `99839b5`, waiting for CI. **Not done:** the same choice inside Add Entry and Import popups, 4th Import option for documents, recorder text overlap check |
| 10.25 | **Pages:** a note scrolls as page 1, page 2 ...; media can be dragged between pages | NOT STARTED |
| 10.26 | **Reading mode** (3-dot menu): no editing, zoom and scroll, Edit FAB, 3-dot menu with Share, Focus mode, Light / Dark / Eye comfort, Mark as finished (removes it from the preview); markup tools saved with the note; bookmark a page and continue from there | DONE except markup tools (CI green `602e2b2`): reading mode from the editor menu with zoom, themes (Auto, Light, Dark, Eye comfort), bookmark and continue, Edit button, Mark as finished. **Pending:** markup tools saved with the note |
| 10.27 | **Selection pill in reading mode:** Explain with AI, Translate (language chooser, also in the main AI popup), Word meaning, and a search bar to ask about the selection or the whole note; the answer opens as a bottom popup that minimises and maximises | DONE (CI green `602e2b2`): selection pill (Explain, Translate with language chooser, Meaning, Ask), ask bar, answer popup that minimises and closes |
| 10.28 | **Voice search icon** in every AI prompt field, hidden while scrolling | PARTLY DONE (CI green `602e2b2`): mic button (offline speech recogniser preferred) in the AI panel, the AI task prompt and the reading ask bar. **Pending:** hiding it while scrolling |
| 10.29 | **Reading miniplayer:** minimise the reading mode to a miniplayer (thumbnail, note name and page number, chapter / unit / subject from the syllabus link, play / pause when an audio note is playing); swipe left or right pins it as a half-hidden bubble (tap to reopen, drag to move, drag onto X to dismiss); swipe down dismisses; tap or swipe up maximises; shown on every page of the app | PARTLY DONE (CI green `602e2b2`): miniplayer with the bubble, drag to X, swipe gestures and reopen. **Pending:** play / pause for audio notes |
| 10.30 | **Doodle shapes and layers:** many geometric shapes, layers, everything editable again | NOT STARTED |
| 10.31 | **Stacked formatting:** several styles combined on one piece of text must render and edit correctly (example from the owner: alignment + size + strike + underline + highlight + italic + code) | DONE (CI green `20ab275`, `045103c`; JVM tests run by CI): bold-italic, nested colour and size tags pair up, the innermost one wins in the preview as in the editor, code spans no longer break tag pairing, regression test with the owner's example string. **Reopened and rewritten 2026-10-28** (owner: raw codes appeared when a style was applied twice after 2 or 3 others, e.g. `- [ ] a) > a) > - a) > a) *++`{size:12|# **~~`...`): see 19.8 |
| 10.32 | **Multi-select in a subject folder:** holding a note shows a floating navigation pill at the top with the count, Cancel and actions that change with the action (Move, Cancel, Paste ...) | DONE except Paste (CI green `f116f26`): floating Material 3 Expressive toolbar with the count, Finish / Reopen, Share, Move, Delete and Cancel |
| 10.33 | Vault FAB gets "Edit subjects" (opens the Subjects tab) | DONE (CI green `f116f26`) |
| 10.34 | **Rename "Ask AI" to "AI Features".** The switch disables everything AI: the AI Hub button next to the pill disappears (still in Profile with a "disabled" note); every AI button or popup says "AI is disabled. Enable it or ignore"; only local extraction works. Turning it on shows a transparent consent popup (Agree / Decline), the same one the onboarding gets when it is overhauled | PARTLY DONE (CI green `f116f26`): named "AI Features"; the AI Hub button hides when it is off and stays in Profile with a note. **Pending:** "AI is disabled. Enable it or ignore" on every AI button and the consent popup |
| 10.35 | **AI Hub history:** every AI action, input prompt and result is kept in the AI Hub | DONE (CI green `f116f26`): AI Hub keeps every request and answer on the phone (200 newest), delete one or all |
| 10.36 | Every AI feature (summaries, quizzes, extraction, import) becomes syllabus-aware and humanised like 10.23 | NOT STARTED |
| 10.37 | The editor's navigation pill selected icon uses the same selection effect (distinction and light refraction) as the main pill | NOT STARTED |
| 10.38 | All UI elements, layouts and animations consistent across the app | continuous |
| 10.39 | **Before 1.0:** audit all project files; remove junk, loopholes, broken or unfinished code and performance problems (only with your consent, without removing any feature or quality) | Step 16 |

**Tests:** Large-image and low-storage runs, a note with 50 k characters, share to two apps and to a missing app. · **Effort:** XL

## Step 11. Absencium AI (the AI Hub, renamed 2026-10-31) and AI settings (36 tasks)

**Deferred to 1.x by the owner (2026-10-18), then pulled forward and now active: the chat, per-provider keys and the settings redesign (19.35 to 19.37), the rebuild (19.40) and the reference-driven features (11.28 to 11.36, owner 2026-11-01).** Rows 11.1 to 11.5 keep their original 1.x status.

Builds on the existing providers, model picker and key vault; adds to them rather than replacing them. Everything here is optional, off by default, uses the student's own key, and respects the theme options and Material 3 Expressive.

| # | Task | Status |
| - | --- | --- |
| 11.1 | Several API keys per provider with a setup guide dropdown | keys per provider DONE in 19.37 (CI green `86482cd`); several keys for one provider and the setup guide dropdown deferred to 1.x |
| 11.2 | Show Gemini as recommended (can read YouTube links) and OpenRouter as recommended for its free/paid listing; look up and verify free models of the other providers before showing a recommended all-rounder for each; for Gemini recommend the Flash-Lite 3.5 model if the fetched model list contains it (otherwise the nearest Flash-Lite entry that list does contain); the name always comes from the fetched list, never typed from memory; other providers under an "Other models" dropdown; guide for getting the chosen provider's key | deferred to 1.x |
| 11.3 | Clear explanation of what data is sent to an AI provider; AI failures never block local features; AI stays optional and off by default | deferred to 1.x (consent popup and the per-area permission switches exist) |
| 11.4 | Ask AI inside notes with query history and jump-to-source (from the master specification); voice prompt on long-press of the AI icon; optional AI Memory (off by default) | deferred to 1.x |
| 11.5 | Design reference from the repository audit (Agora, LastChat) for conversation management, context handling and tool calling, decided after the audit. LastChat is AGPL-3.0: ideas only, never copy its code | started (19.36); rest deferred to 1.x |
| 11.6 | **Advanced chat:** edit and resend (DONE 19.36), regenerate (DONE), pin (DONE), rename / delete (DONE), search chats by title and text (DONE); **stop, continue and retry with another model** (19.40: Stop in the box, Continue and "Retry with another model" under the last answer, a stopped or cut-off question shows "No answer yet · Retry"), **archive** (19.40: long-press a chat, filter Archived); **message branching** (keep the old answer when editing), export a chat as PDF, search inside one chat | partly done; branching, PDF export, in-chat search pending |
| 11.7 | **Persistent AI memory and local retrieval (RAG):** optional **Memory** switch (off by default, in AI settings and in the chat menu); **global memory** used in every chat and **conversation memory** for one chat (`chat_only`); the AI saves lasting facts itself (`remember`, no card, never secrets or one-off requests) and the student can **view, edit, add, delete and forget all** (`AiMemory`, `MemoryDialog`); retrieval on the phone before each question (word overlap with simple stemming, this chat's memories always included; no request to a server). **Pending:** semantic (embedding) retrieval through a provider's embeddings endpoint, automatic retrieval from notes and guides | DONE, CI green `dd2321b`; embeddings and notes retrieval pending |
| 11.8 | **Provider and model management:** custom OpenAI-compatible endpoint (DONE), connection testing (DONE 19.37), per-chat model (DONE 19.36); **multiple API keys per provider** (19.40: "Add another key" in the provider form, up to 8 extra, all in the Keystore vault); **automatic failover:** when a key hits its rate or quota limit (HTTP 429 / quota) the next saved key is tried, and when all keys of the provider are resting (10 min) the other providers that have a key take over with their own saved model (chat answers; the answer shows the model that really replied); per-assistant temperature (19.40). **Pending:** per-model max tokens, favourite models, recommended models, failover for note writing / OCR / syllabus import | DONE, CI green `dd2321b`; per-model parameters, favourites, failover outside chat pending |
| 11.9 | **Multimodal chat:** attach **pictures** (scaled to 1600 px), **PDFs** (up to 15 MB), **text files**, **recordings and short videos** (up to 15 MB, Gemini models; any other model says so plainly) and **notes** (their text) from the "+" sheet; file chips before sending and file lines in the question; the files are sent again with later questions so follow-ups work (**document Q&A**, audio is heard and transcribed by the model, video is understood by Gemini); Groq PDFs and non-Gemini audio / video are refused with a reason. **Pending:** camera capture, picture thumbnails and a page preview for PDFs | DONE, CI green `dd2321b`; camera, previews pending |
| 11.10 | **Web search with citations:** per-chat **Web search** switch ("+" sheet, optional, off by default); uses the provider's own search: Gemini Google Search grounding, Claude web search tool, OpenRouter web plugin; the sources are listed under the answer (tap one to open it) and stay saved with it for source-grounded follow-ups. Other providers say the switch needs one of these | DONE, CI green `dd2321b`; other providers pending |
| 11.11 | **Custom assistants:** reusable profiles with custom system instructions, optional model and temperature; presets for **study, research, writing and exam coaching**; create / edit / copy-to-edit / delete; **import and export** as a JSON file (`AiAssistants`, `AssistantsDialog`); chosen per chat. **Pending:** allowed areas per assistant | DONE, CI green `dd2321b`; per-assistant allowed areas pending |
| 11.12 | **Study mode:** per-chat switch that teaches step by step, checks understanding with a question, and has **adjustable difficulty** (Auto, Beginner, School, College, Expert). **Pending:** hints that grow, spaced prompts | DONE, CI green `dd2321b`; hints and spaced prompts pending |
| 11.13 | **Quiz generator:** "Quiz me" in the "+" sheet makes a quiz with answers from attached notes, PDFs or study material, or from a topic. **Pending:** an interactive scored quiz with a review of wrong answers (the recall quiz tables already exist) | partly done (text quiz in chat); interactive quiz pending |
| 11.14 | **Note assistant:** pick notes with the "+" sheet (their text is sent only then), then summarise, rewrite, improve or organise them; any answer can be saved with "Save as note" (cleaned into the editor's markup); a new note the chat proposes is **written by the app's note writer at the length asked** (moderate / lengthy / in depth, full formatting), not by a short chat reply. **Pending:** replace a note in place through a confirm card, flashcards | partly done; in-place edit pending |
| 11.15 | **PDF assistant:** attach a PDF and ask, summarise or extract (the model reads the file); **pending:** answers that point to the page, a library of guides to pick from | partly done |
| 11.16 | **Revision assistant:** the AI is given exam countdowns, syllabus progress, attendance and today's / tomorrow's lectures (when allowed) and the "Revision plan" prompt asks for a plan from them. **Pending:** adding the plan to the calendar through confirm cards | partly done |
| 11.17 | **Timetable assistant:** add and move lectures (DONE as confirm cards, 19.36); today's and tomorrow's lectures and holidays are in its context (19.40). **Pending:** clash detection, free-slot suggestions | partly done |
| 11.18 | **Attendance assistant:** mark a day (DONE as a confirm card, 19.36); the context now carries each subject's present / absent counts, the percentage against the required one, how many lectures can still be missed and how many must be attended in a row to recover (`AiChatPrompt.attendanceLine`) | DONE, CI green `dd2321b` |
| 11.19 | **Vault integration:** the "+" sheet's Notes tile sends the text of the notes the student ticks, for that message only; new notes are created from a chat (written in full by the note writer). **Pending:** pick whole folders / documents, mention a note by name in the box | partly done |
| 11.20 | **Context-aware AI with explicit permission:** per-area switches (19.36) now also **Syllabus** and **Memory** (19.40); more areas (reminders, tasks, per-note text), a "what was sent" view for each message | partly done |
| 11.21 | **Per-provider token usage statistics:** local counters per provider (answers, tokens in / out as the provider reports them, tokens today) in AI & Integrations, "Reset counts", never uploaded (`AiUsage`, chat answers). **Pending:** per model, and for note writing and other AI requests | DONE, CI green `dd2321b`; per-model and other requests pending |
| 11.22 | **To-do / task actions** ("add this to my to-do list"): needs the tasks feature of Step 8 first, then an `add_task` action | pending (waits for Step 8) |
| 11.23 | **Syllabus actions:** set a topic's status (`set_syllabus_status`, 19.40, confirm card); **pending:** add units from the chat (`add_syllabus_node`) | partly done |
| 11.24 | **Share a conversation** as a PDF and through Sharewave (plain-text share exists, 19.36) | partly done; the rest waits for Step 17 |
| 11.25 | **Answers keep being written in the background (owner 2026-10-31):** answers are produced by `AiChatService` on the app's own scope, not the screen's, so switching chats, leaving Absencium AI or pressing Home does not stop them; a foreground service (`AiWorkService`, "AI answers in progress", only while an answer is in flight, `dataSync` type) keeps the process alive after the app is closed from recents where Android allows it. Chats with a running answer show "Writing" in the list, finished-but-unseen ones a dot (also on the menu button). **Notification when an answer is ready (owner 2026-10-31):** pending, see 5.6. | DONE, CI green `dd2321b`; process survival depends on the phone's battery rules |
| 11.26 | **Smarter answers (owner 2026-10-31, "too dumb / pre-programmed"):** a general-purpose system prompt (no longer limited to the app, no self-introduction), real system + multi-turn messages instead of one flat prompt, much larger output limits, the full note-markup guide in the prompt, richer app context, tolerant action parsing (the action block is always hidden), **notes written by the note writer at the requested length**, **Thinking** switch per chat (Gemini 3 thinking level, Gemini 2.5 budget, Claude extended thinking, OpenAI reasoning effort) | DONE, CI green `dd2321b` |
| 11.27 | **New chat screen (owner 2026-10-31, references in `docs/ui overhaul/Ai`):** single-line model title, new-chat and "..." pill, full-screen spring chat list (search, filter All / Pinned / Unread / Archived, long-press actions, App prompts folder, avatar and settings at the bottom), empty state with the morph badge, big greeting and icon suggestions, input pill with "+", mic and round send / stop buttons, quiet user bubble, answer action row (copy, again, share, more) with the "can make mistakes" line, a glow while writing, "+" sheet (Photos, Files, Notes, Web search, Thinking, Study mode with difficulty, Assistant, Memory, ready prompts) | DONE, CI green `dd2321b` |
| 11.28 | **Full app awareness with explicit permission (owner 2026-11-01 list):** the AI may read, propose edits to and manage Absencium Core (attendance, timetable, exams, tasks, reminders, calendar, syllabus, study material) and personal storage (notes, saved chats, the encrypted Vault, Trash, Drafts). **Strict approval rule:** every write, edit, replacement or removal (a word, a line or a whole document) is only a proposal until the student approves it in an application-level confirmation gate ("AI is requesting to modify entry #42. Approve / Deny?"). Batch 28: Study material scope and **reading the content of a note or material with approval** (`read_content`, 19.41). Pending: Tasks, Reminders, Vault records, Trash, Drafts and saved chats as permission areas; propose edit / replace / delete of a note or record (word, line, document) through the gate; a "what was sent" view | WAITING FOR CI (reading); rest pending |
| 11.29 | **Embedded JavaScript runtime** (QuickJS, picture 06) for calculations, parsing, regex and scripting; a switch under Tools (off by default); no network or file access from scripts, time and memory limits, result shown in the chat. Python only if a lightweight fully on-device option is found. Library licence and size reported before adding it | pending |
| 11.30 | **Memory and RAG upgrade (pictures 07, 08):** memory mode card (Off / Basic / Basic + RAG), Reference recent chats, a memory search tool the AI can call, advanced (episodic) memory, similarity-threshold slider 0.0 to 1.0, local semantic embeddings with an on-device vector store (honest note when no embedding model is selected: lexical retrieval keeps working), context management (sliding window plus automatic summary), statistics (total / embedded), manage memory (search, sort, add, refresh, edit, delete, summarise), a memory debugger ("test RAG retrieval"), the page collapses when memory is off | pending (basic memory exists, 11.7) |
| 11.31 | **Web search providers (pictures 06, 14, 15, 18):** provider picker sheet, "Prefer built-in search", result size, **keyless** router (no API key, classifies weather / news / sports / images / web and falls back between sources), SearXNG, DuckDuckGo RSS, Google Search grounding; "What is Keyless?" sheet listing every backend contacted. Needs your approval of the sites contacted (see Your Approval Required) | pending (Gemini, Claude, OpenRouter search exists, 11.10) |
| 11.32 | **Thinking and inference control (picture 09):** thinking level Off / Low / Medium (default) / High; Temperature with named zones (switch plus slider), Top P, Max tokens ("Auto"), Stream output; per assistant, optional (switch off = provider default) | pending (thinking is a switch today, 11.26) |
| 11.33 | **Rendering:** LaTeX formulas (`$inline$`, `$$display$$`), code syntax highlighting, Mermaid diagrams, tables (exists) in answers | pending |
| 11.34 | **Schema versioning and validation:** a format version in exports of assistants, notes and memory / vector indices, Room migrations for every new table, validation and a clear error on import | pending (assistants import / export exists) |
| 11.35 | **Assistants editor and read aloud (pictures 10, 11, 12, 13, 19):** the assistant page as grouped sub-pages (Profile, Prompt, Context, Models, Memory, Tools & Search, Appearance, Advanced), prompt editor with a token estimate, expand and variable chips (`{{user}}`, `{{cur_date}}`, `{{model_name}}` ...; device info, battery and location only when allowed), several intros, clone and drag-to-reorder; read aloud with System TTS (offline) first, then a custom OpenAI-compatible endpoint and cloud voices | pending |
| 11.36 | **Absencium AI UI fixes (owner 2026-11-01):** incognito chat, compact model chooser and note / material picker, no solid block behind the bars, sliding sidebar, loading shape instead of the glow, "needs attention" fix, raw colour tags fix (details in 19.41) | WAITING FOR CI |

**Tests:** Provider list fetched offline and online; a failing key leaves notes untouched; a note is sent to the AI only after Apply. · **Effort:** L

## Step 12. Study and focus tools (6 tasks)

**Deferred to 1.x by the owner (2026-10-18), except any dependency the approved core scope needs.**

| # | Task | Status |
| - | --- | --- |
| 12.1 | Study timer that survives backgrounding, screen lock and process death, one notification, no duplicates | deferred to 1.x |
| 12.2 | Daily Recall and spaced repetition (7-day loop, sticky lock, repeat badge) as specified; flashcards; Emergency Cram Mode before exams | deferred to 1.x |
| 12.3 | Semester-linked exam preparation and syllabus tracking; optional AI-generated study material | syllabus tracking **moved into 1.0 as 10.16 to 10.18**; semester-linked exam preparation stays deferred to 1.x |
| 12.4 | Retrospective study category log and Momentum tracker (optional, off by default) | deferred to 1.x |
| 12.5 | Gradebook and GPA | deferred to 1.x (owner decision 2026-10-18) |
| 12.6 | ~~Optional GPS attendance assistance (marks present after 5 minutes in a classroom area)~~ **excluded**: manual attendance is enough and it avoids location permission | excluded |

**Tests:** Timer survives a force-stop; recall questions respect the lock; spaced dates correct across time-zone change. · **Effort:** L

## Step 13. Widgets, shortcuts and quick actions (3 tasks)

**Deferred to 1.x by the owner (2026-10-18), except any dependency the approved core scope needs.**

Needs attendance, tasks and the timer to exist.

| # | Task | Status |
| - | --- | --- |
| 13.1 | Home-screen widgets: 1x1 quick capture, then 1x2 up to 4x4 (attendance, next up, deadlines); theme-aware, resizable, refresh and stale-data handling. The widget framework (Jetpack Glance or RemoteViews) is chosen after weighing APK size | deferred to 1.x |
| 13.2 | Launcher shortcuts, Quick Settings tile, Quick Add, notification actions | deferred to 1.x |
| 13.3 | Customizable, smart dashboard summaries and start-screen choice | deferred to 1.x |

**Tests:** Widget on a real launcher at several sizes; after reboot; with the app force-stopped. · **Effort:** L

## Step 14. Settings, privacy, accessibility and polish (6 tasks)

The consistency sweep that needs most features in place.

| # | Task | Status |
| - | --- | --- |
| 14.1 | Privacy controls and a privacy dashboard, optional app lock (BiometricPrompt), no sensitive data in logs, size-limited rotating debug log, redacted diagnostics export | pending |
| 14.2 | About: Report a bug and Request a feature buttons that open a prefilled GitHub issue (explained to the owner step by step), "Buy Me a Chai" card (https://www.buymeachai.in/skywa1ker404, the only support link), review prompt | pending |
| 14.3 | Tapping the version number 7 times opens a safe, lightweight debug page in release builds that shows nothing private | pending |
| 14.4 | Accessibility: font scaling without clipped text, TalkBack labels, contrast in every theme, status never by colour alone, accessible charts, focus order, display-size changes, comfortable touch targets | pending |
| 14.5 | Consistency sweep: icons, spacing, typography, shapes, long text and filenames, loading and empty states, undo for deletions, confirmation for destructive actions; permissions onboarding that is short and never overwhelming | pending |
| 14.6 | Outdated docs refresh: `IDENTITY.md`, changelog file and in-app changelog, README, intro text | pending |

**Tests:** TalkBack pass, largest font, largest display size, every theme. · **Effort:** L

## Step 15. Performance and stability pass (3 tasks)

| # | Task | Status |
| - | --- | --- |
| 15.1 | Startup and navigation with no lag: measure cold start and screen transitions on a device, fix measured causes only | pending |
| 15.2 | Avoid database queries during recomposition; efficient image loading; large imports and exports off the main thread; leak and low-RAM checks; no continuous polling | pending |
| 15.3 | Decide with the owner whether a Baseline Profile is worth its build cost | pending |

**Tests:** Cold start time before/after; 10 000-row database; memory profile. · **Effort:** M

## Step 16. Final testing and the stable 1.0.0 release (9 tasks)

**Approved scope (2026-10-18): 1.0.0 is Steps 2 to 10 and 14 to 16, in order, bug fixes first.** The AI Hub, study tools and widgets (Steps 11 to 13) follow as 1.x. Nothing is published until device testing and release checks pass and you approve.

| # | Task | Status |
| - | --- | --- |
| 16.1 | Regression tests and the full device test list: fresh install, upgrade with data, permission denial, force-stop, reboot, backup and restore, airplane mode, language and theme switching, font and display size, 12/24 h, create/edit/save/discard/delete/restore, midnight, month/year and leap day, rapid taps, nearly full storage, interrupted backup or import, migrations | pending |
| 16.2 | Dev CI green on both jobs and the APK report checked | pending |
| 16.3 | Signing: stable key and the four secrets are set (reported by Anakin); the stable certificate SHA-256 is in `release/stable/release-notes/v1.0.0.md`. Not verifiable from here | owner reports done |
| 16.4 | Dev README, stable README and `release-notes/v1.0.0.md` reviewed by Anakin (drafted) | drafted |
| 16.5 | Run `scripts/export_stable.sh` into a temporary folder and check every link and asset (passed once, repeat before release) | pending |
| 16.6 | Licence audit: ScanCode run by Anakin, licence assets inside the APK checked by Anakin, remaining items in `docs/LICENSE_AUDIT.md` cleared | pending |
| 16.7 | ~~Add the GPL-3.0 LICENSE (needs approval)~~ added by Anakin (commit 3adf0bb); its SHA-256 matches gnu.org's gpl-3.0.txt | done by owner |
| 16.8 | On approval only: publish to `Absencium`, run the stable workflow, publish the verified release, update `data/projects.json` in `skywa1ker404.github.io` (text only) | pending |
| 16.9 | After approval: clear files that are no longer used (see "Cleanup proposal" below) | needs approval |

**Tests:** The release checklist above, on a real phone, from the signed APK. · **Effort:** M · **Gate:** Anakin's explicit approval

## Step 17. Sharing and Sharewave (after the stable release) (6 tasks)

Approved placement: after the stable release; there is no sync. Needs the export format of Step 4 and the `ShareService` of Step 10, so it adds code rather than rewriting.

| # | Task | Status |
| - | --- | --- |
| 17.1 | Audit the repository, current sharing code and permissions, then report the plan, risks, compatibility limits and required permissions before significant changes | pending |
| 17.2 | Android Sharesheet: `ACTION_SEND` / `ACTION_SEND_MULTIPLE` for notes, text, images, PDFs, documents, selected files, exported timetables and folder archives; correct MIME types; `FileProvider` content URIs; URI permissions, temporary files, missing apps, unsupported formats and errors handled; the native Sharesheet stays the universal fallback; no assumption that other apps expose chats or contacts | pending |
| 17.3 | Sharewave research and prototype on real devices: discovery (BLE or local network), pairing (QR and manual fallback), transfer over Wi-Fi Direct, local Wi-Fi or hotspot; Google Nearby Connections only as an optional transport; document Android-version permissions (location only if a mechanism truly needs it) and manufacturer limits before choosing | pending |
| 17.4 | Quick Send: entries, notes, images, files and folders with structure and metadata; recipient must accept; progress, cancel, retry, recovery; authenticated pairing and encrypted transfer; filename, size, type and path validation; never overwrite silently, never execute received files | pending |
| 17.5 | Shared Vault Access: owner exposes only selected folders or entries for a limited time; recipient browses, previews, caches, downloads or imports with conflict handling; active-session list with devices, paths, permissions, status and expiry; manual revoke and automatic expiry (copies already taken stay); live access needs the owner's device | pending |
| 17.6 | Security, compatibility, accessibility and failure-path tests (interrupted transfer, low storage, denied permission, corrupted data, expired or revoked session); update specs, roadmap, README and user docs | pending |

**Tests:** Two or more real phones from different makers; transfer with Wi-Fi off and airplane mode on; tampered and oversized files rejected. · **Effort:** XL · **Gate:** Anakin approves the plan and the transport choice

## Step 18. ~~Optional cloud: sync and routine backup~~ REMOVED (2026-10-22)

The owner asked to remove everything related to sync and interval backup. There is no sync, no scheduled or daily backup and no cloud transport in the app, the settings or the plan. Manual backup and restore (Step 4.4) stay. If cloud features are wanted later they start as a new step with a new decision.

---

## Cleanup proposal (nothing is deleted without your approval)

| File | Proposal |
| --- | --- |
| `docs/SPECIFICATION.md` | master specification copy; keep until its content is folded into the steps above, then remove |
| `docs/archive/MASTER_SPEC_2026-10-16_original.md` | the original 711-line text, moved here on 2026-10-27 and marked as superseded; `docs/MASTER_SPEC.md` is now the current specification without the cancelled plans |
| `docs/CHNAGELOG.md` | contains only "working on it" and its name is misspelt; replace by `CHANGELOG.md` when the changelog is refreshed |
| `docs/LICENSE_AUDIT.md` | keep until the audit is cleared, then fold into `THIRD_PARTY_LICENSES.md` |
| `docs/IDENTITY.md`, `docs/SIGNING.md`, `docs/CONTRIBUTION.md`, `docs/THIRD_PARTY_LICENSES.md`, `docs/DEPENDENCY_LICENSES.md`, `docs/licenses/`, `docs/art/` | keep |

## Why this order

- Bug fixes first: users lose trust to data loss and glitches before they miss a feature.
- The data foundation (Step 4) comes before features that need stable ids, a Trash and a versioned export, so later steps add code instead of rewriting it.
- The Vault and editor (Step 10) come before study tools, widgets and sharing, because those all read notes and attachments.
- Platform integration (Step 5) and the timetable and attendance steps (6, 7) come early because they fix the daily-use core.
- AI, study tools and widgets (11 to 13) move to 1.x so 1.0.0 stays small enough to test properly.
- Performance and stability (15) and final testing (16) come last, right before the stable release, and nothing is published until Anakin approves.

## Open decisions for Anakin
1. Approval of the audit report once it is written (Step 3).
2. Approval of the cleanup table above, file by file.

## Step 19. Full UI overhaul: Liquid Glass, Material 3 Expressive, new layouts (owner request 2026-10-27, active; rows 19.22 onward were moved here from the old ledger on 2026-11-01)

Replaces the old "restyle after the reference kit" task. Reference material lives in `docs/ui overhaul/` (15 images + README with a screen-by-screen table); every UI change opens the matching image first. **Only the 3-button navigation pill keeps its design** (shape, light-bending lens, bead). Everything else may change, including swapping one kind of element for another. Colours always come from the theme; Liquid Glass off and Performance mode must keep working. GPL-3.0: patterns from Apache-2.0 references (Kyant0 Backdrop, Meticha, Material code) are fine; nothing copied from the mock-up images.

| # | Task | Status |
| - | --- | --- |
| 19.1 | **Batch 1, Liquid Glass.** Go through image 01 (9 screens) grid by grid, then rebuild the glass on the Kyant0 Backdrop patterns (`https://github.com/Kyant0/AndroidLiquidGlass`: backdrop blur, vibrancy, lens refraction, flat theme veil, thin edge, soft shadow, press and drag interaction), **no glow, no haze**. Real backdrop glass on floating controls, FABs, top bars and popups; clean flat tinted panes for scrolling cards (many blurred layers lag). Fix what image 01 shows: content under the pill (bottom padding), stacked identical cards in Trash / Subjects / Today, dense Appearance text, uneven Vault tiles, popups showing unblurred content | IN PROGRESS. Done, CI green `25f76a2`: popups use a stronger blur (18 dp minimum), a denser veil and Kyant's plain edge; card panes have one quiet hairline edge instead of the bright gradient rim; the wallpaper gets one broad soft patch of the theme colour; no glow anywhere. **Also done, CI green `a1a755e`:** every stock dialog (17 files) is now the app's own dialog: large rounded corners, thin edge, see-through in the theme colour with Liquid Glass + frosted overlays and the system blur behind it on Android 12+ where the phone allows it; one themed menu (`AppMenu`) for the 3-dot menus. **Still to do:** Kyant press / drag squash and the pressed-highlight shader on floating buttons, glass toggles and sliders, calmer inner tiles in popups, then the layout fixes for Trash, Subjects, Today, Appearance and Vault (finished in batch 3) |
| 19.2 | **AI options popup (image 02).** Content scrolls with `imePadding`, no fixed heights, chips wrap, field never clipped, glass layer | FIX MADE, CI green `25f76a2`: the popup's size animation could outgrow the space left by the keyboard and cut off the bottom; the height limit now comes first. **Second report 2026-10-28 (any text field low on the screen, popup jumps up and leaves a gap until you type):** see 19.7 |
| 19.3 | **Batch 2, real Material 3 Expressive.** Research first and write the verified results to `docs/ui overhaul/M3E_RESEARCH.md`: every page of `developer.android.com/design/ui/mobile/guides`, `m3.material.io/blog/m3-expressive-motion-theming`, the compose-material3 release notes (1.4.0-alpha11 up to the version in use, 1.5.0-alpha29), Android 16 and 17 design changes, and the repositories Minus, PixelPlayer and June (cloned separately, usage patterns only, licences checked). **Research status 2026-10-28 (see `docs/ui overhaul/M3E_RESEARCH.md`):** Minus (Apache-2.0) and June (GPL-3.0) cloned and searched; PixelPlayer's licence is proprietary, so nothing is copied from it; the developer.android.com guide pages and the m3.material.io blog could not be read (blocked / need JavaScript), so those parts are still open and need pasted text from the owner. Then use the whole component set, not one button type: button groups (standard and connected), toggle and split buttons, extended and menu FABs, floating toolbars, flexible app bars, wavy progress, loading indicator with shape morphing and haptics, carousels, expressive navigation rail and bar, chips, sliders, switches with icons, `MaterialShapes` everywhere, the expressive motion scheme and springs on every screen including loading and empty states | NOT STARTED |
| 19.4 | **Batch 3, layout, shape and typography overhaul of every screen** (**started 2026-10-28, CI green `d56d315`:** notes list as hue-tinted cards with a clean preview and chips, study material as pastel rows with an Open pill and type filter chips, folder Study and Syllabus tabs, the Syllabus tree as one pastel card per unit with round status marks and guide lines, shape-morphing empty states; **still to do:** every other screen) from images 03 to 15 (screen mapping in `docs/ui overhaul/README.md`): Home, Today, Attendance, Timetable, Calendar, Vault and subject folder, note editor, reading mode, Syllabus, AI Hub, Settings, Appearance, Trash, Search and Add entry popups, onboarding. Hue-tinted cards, round icon chips, large light titles, capsule chips, floating bottom controls, generous spacing, one accent per screen | IN PROGRESS. Done (CI green): Trash (hue cards, icon square, "days left" chip that turns red at 3 or fewer, Restore and Delete for good), Subjects (hue card, coloured letter square, Active / Mute chips, 3-dot menu with Rename and Delete), Home, Today lecture cards, folder tabs, notes, study material, syllabus. **Left:** see the ledger |
| 19.5 | Keep the 3-button pill exactly as it is: after every batch compare it with image 01 and the previous build | ongoing |
| 19.6 | Docs: update `docs/ui overhaul/README.md`, MASTER_SPEC section 2, this file and the changelog after each batch | ongoing |
| 19.7 | **Keyboard gap bug (owner 2026-10-28, every text field low on the screen).** Cause found by reading the code: `MainActivity` had no `windowSoftInputMode`, so Android chose "pan" for a window it could not tell scrolls, and moved the whole window up on top of the popup's own keyboard padding. Fix: `android:windowSoftInputMode="adjustResize"` on the activity (the app is edge-to-edge, so the insets reach Compose and the popup's `imePadding` is the only thing that moves it) | DONE in code, CI green `79fbc9f` |
| 19.8 | **Stacked styles rewritten (owner 2026-10-28).** Causes: the live editor hides the markers, so a selection of the word "bar" in `**bar**` reaches the code starting in front of the hidden `**`; the old code then wrapped again (`****bar****`), and toggling bold off under another style added a second pair inside the first. Line prefixes (list, check, number, letter, quote, heading) each stripped only some of the others and went in front of the alignment tag, so they piled up. New engine `InlineStyle` (+ `MarkdownEdit`): the selection is fitted to the markers (steps in over markers at its edges, reaches out to hold the other half of a pair); a style is removed from the pair that really encloses the selection, splitting the pair when only part is selected; the same style inside the selection is merged instead of nested; colour and size are replaced through other styles; code drops the markers inside it and cannot be styled; `***` keeps the other half. One block prefix per line after the alignment tag; a line that already holds a stack is cleaned up when a new prefix is chosen. The parser reads `*a **b***` and `**a *b***` (nested bold and italic) the same way as the editor. 17 new JVM tests | DONE in code, CI green `79fbc9f` (tests run by CI) |
| 19.9 | **Floating bottom controls (owner 2026-10-28, "like pic 2": AI button, pill and FAB).** The AI Hub button is a squircle (rounded square) beside the pill instead of a circle. The subject folder has its tabs as a floating glass pill at the bottom (`TabPill(floating = true)`) with an extended floating button above it on the right ("New entry" / "Add material" / "Edit syllabus" by tab, hidden while notes are selected); the in-list "New entry" bar, tab bar and "Add study material" button are gone. Subjects: "Add subject" floating button opening a small dialog (the inline field card is gone). Syllabus: "Add unit" and "Import outline" floating buttons. Floating controls live in the screen's `overlay` (outside the glass layer they sample) | DONE, CI green `79fbc9f`. **Left:** the empty-state "Add subjects" buttons on Home and Vault, Test Lab, Storage |
| 19.10 | **Topic filter as a menu on the subject name (owner 2026-10-28).** The "Topic: All" chip is gone; tapping the subject name in the folder's title bar opens a menu with "All topics", the whole syllabus tree indented by level (the chosen topic in the accent colour with a tick), and "Open the syllabus". An active filter shows under the name and as a removable chip above the list. The old "Show notes under" dialog is no longer used from the folder (still used to file a note or guide under a topic) | DONE, CI green `79fbc9f` |
| 19.11 | **Folder bottom controls like the main pill (owner 2026-10-29, pic 5).** The folder now uses the main `BottomPill` itself (lens, bead, label) instead of a copy; it shrinks to icons and the button slides away while the list scrolls down and returns on scrolling up (`PillScroll` / `rememberPillScroll`, the same distances as the main screen). The list now runs to the very bottom under the navigation bar (`ScreenScaffold(edgeToEdgeBottom = true)`), which removes the solid band under the cards | DONE, CI green `91daba9` |
| 19.12 | **Home (owner 2026-10-29, pics 1 to 3).** "Watch these subjects" is no longer an open section: a small arrow with no text in the bottom right corner of the bunk buffer card opens a joined card under it in the same theme colour (hidden by default, the two cards' meeting corners squared and animated). The AI Hub button beside the pill is gone; on Home a pinned squircle **Subjects** button sits there instead (the AI Hub is the third Home quick action when AI Features are on, and stays in the profile sheet). The Home **Attendance** quick action opens a popup with the Attendance card (percentage, Present / Absent today, "Did you collect today's notes?" with the per-subject list, "Open the calendar"); the bunk buffer card still opens the Attendance tab | DONE, CI green `91daba9` |
| 19.13 | **Bugs from screenshots 2026-10-29.** Import an outline dialog (pic 4): the three actions were squeezed into one row and "Use as typed" wrapped letter by letter; "File" and "Tidy with AI" are now buttons under the text field and the dialog keeps one short action. Calendar: the chosen day and a holiday had the same colour; a holiday keeps a stronger warm tone even when chosen and the chosen day gets a ring in the theme's main colour. Note previews (pic 5) no longer show leftover markers or stacked prefixes of notes broken before 19.8 (`{size:12|`, `**`, `a) > a)`) | DONE, CI green `91daba9` |
| 19.14 | Shared pieces added: `AppCard(shape = ...)` for joined cards, `ExpandChevron(tint = ...)`, `AttendancePopup`, `PillScroll`, `ScreenScaffold(titleContent, edgeToEdgeBottom)` | DONE, CI green `91daba9` |
| 19.15 | **Home Subjects squircle removed (owner 2026-10-29).** The pinned Subjects button beside the pill is gone completely; subjects are reached from the Vault and Settings. **"Add today's notes" label:** the two buttons of the "Did you collect today's notes?" card now wrap (`FlowRow`) instead of sharing a row, so the whole label shows | DONE, CI green `497da1f` |
| 19.16 | **Extended buttons behave like Material extended FABs.** `ShellFab(expanded)`: icon and label, the label folds away with a spring (icon only) while the list scrolls down and grows back on scrolling up; the button stays on screen instead of sliding half away. Used on the three folder tabs and the Syllabus tree (which now has scroll tracking). `PillScroll` now counts only scroll the list really used, so a page too short to scroll no longer shrinks the pill | DONE, CI green `497da1f` |
| 19.17 | **Editor header audit (owner 2026-10-29, pic 4).** Found and fixed: (1) a colour or size put on a whole heading or list line was written in front of the `## ` / `- `, so the live page showed the hidden mark in the accent colour; styles now go after the line's block prefix and, over several lines, one pair per line (the page reads one line at a time, so a pair across a line break stayed as raw characters); (2) the character count counted the hidden markers (it now counts what is shown, and says "1 character"); (3) "Uncategorized" is now a dropdown chip under the date line that opens the topic menu (19.18) and files the note; (4) the date, count and save state keep to one line. Notes already broken before this keep their stray mark until the style is applied again | DONE, CI green `497da1f` |
| 19.18 | **One topic menu (`TopicMenu`, owner 2026-10-29, pic 5).** Same rows everywhere (the folder's topic filter and the editor's topic dropdown): a first row ("All topics" / "Uncategorized"), then every unit, chapter and topic indented by level with a dot in its level colour, a tick and a tinted row for the chosen one, then "Open the syllabus". Every row is 48 dp tall with the same 20 dp leading slot, the container is fully solid (no text behind it showing through), it scrolls when long and sits a little below the title | DONE, CI green `497da1f` |
| 19.19 | **Selection toolbar (owner 2026-10-29, pic 6).** **Edit** appears only when exactly one note is selected: rename it and change its date (the time of day is kept). The same long-press selection now works on **Study material**: Topic (file the selected PDFs under a topic) and Delete. **List / card mode** (button at the right of the breadcrumb, Notes and Study material): cards are the tinted tiles, list is one compact line each (`NoteRow`, `GuideRow`) | DONE, CI green `497da1f`. Left: rename for study material (needs a migration, see ledger), remember the mode as a setting |
| 19.20 | **Outline import for one subject only (owner 2026-10-29).** The AI prompt names the subject and tells the AI to read only that subject out of a whole session's papers and to answer NONE when it is not there (then the dialog says so). Long documents are cut from where the subject is first named, up to 60,000 characters. After the outline is read the dialog lists every item with a check box: the student picks what to import; taking an item off takes what is inside it off, putting one on puts on what it sits inside. Duplicates are never offered twice or added: an item with the same title (ignoring case and spacing) under the same parent is shown as "Already in the syllabus", and what is inside it goes into the existing node (`SyllabusTree.duplicates`, tested) | DONE, CI green `497da1f` |
| 19.21 | **Profile syllabus card (owner 2026-10-29), PENDING until the profile is overhauled.** A card in the profile showing the % of the whole syllabus done (all subjects). Tapping it opens a menu with the syllabus of every subject and the import action. The import reads one document (for example a whole session's syllabus PDF) and the AI imports **only the syllabus of subjects that already exist**, each under its own subject's folder: it never creates subjects, never puts one subject's units in another, shows a review list per subject with check boxes, and skips duplicates exactly as 19.20 does | PENDING (after the current overhaul) |
| 19.22 | **Vault grid (image 07, owner list 2026-10-29).** Subject folders are equal square gradient tiles (two-tone wash of the theme's tonal colours), a frosted icon chip with the subject's colour dot at the top left, name, note count and "finished / guides" line at the bottom, springy press. Pull-down Drafts and the Trash button are unchanged | DONE, CI green `311f7de` |
| 19.23 | **Owner 2026-10-29 (afternoon).** (1) Empty-page badge: turns slowly anti-clockwise and truly morphs into the next Material shape every 5 s with a springy squash (`MorphShape` from the morph's cubics; still in performance mode). (2) Add entry, Document / File, "Extract with AI": the save now shows a "Working on it" dialog with the step and a Cancel button; if the AI cannot be reached nothing is saved (copied files are removed) and the message names the file and offers "Just attach". (3) Import outline from a file: a hint under the spinner and Cancel; a phone that cannot reach the AI service still waits for the network's own timeout (up to about two minutes) before the error appears. (4) Study material opens in the app's own PDF viewer (`GuideViewer`; "open in another app" stays in its top bar), from the folder and from search. (5) The bunk-buffer chevron is a 40 dp button with a 28 dp arrow | DONE, CI green `804cda1`. The cause of the AI errors in your log is the phone failing to reach generativelanguage.googleapis.com (and one image-model quota 429); check the connection / DNS / VPN, the app cannot fix that |
| 19.24 | **Owner 2026-10-29 (screens 1, 2, 5, 3, 4).** Empty Home, empty Vault and an empty folder now use the morphing badge (`ExpressiveEmptyState`), whose change now matches the Material loading indicator: its own shape list, a bouncy spring (damping 0.6, stiffness 200) with overshoot, and a turn on each change, every 5 s on top of the slow anti-clockwise spin. The bunk-buffer arrow has its own corner: the pills wrap (`FlowRow`) beside it and can never be covered. **Documents:** the cause of the time-outs was that a PDF was sent whole (up to 15 MB of base64) while pictures are shrunk, on a connection that uploads at a few KB/s. PDFs are now read as small page pictures, 6 pages per request (up to 36 pages, said when more), with progress "Reading pages 1 to 6 of 40" and Cancel; Word and text files go straight to the outline step with no tidy-up request; the outline import asks for the subject's outline from each batch and ignores batches that do not mention it | DONE, CI green `171fe1b`. Left: formatting quality of imported syllabi needs your re-test with the same PDF; very long PDFs beyond 36 pages need a page range picker |
| 19.25 | **Owner 2026-10-29 (evening).** (1) PDFs go up **whole** again (the 36-page batch reading read only 32 of 89 pages): one request with the upload shown as a **wavy Material 3 Expressive progress line** with a percentage (syllabus import and the Document / File extract dialog); the wait for the answer after a big upload is now up to 10 minutes. A PDF over 15 MB (too big for one request) falls back to page pictures, the first 36 pages, and says so. (2) Present / Absent / Total are always side by side (Home card: own row under the ring with the arrow at its end; calendar pills never wrap). (3) "Yes, I have them" / "Add today's notes" sit side by side, each wraps to two lines instead of being cut. (4) The exam / holiday type buttons in Add Calendar Entry sit in one row and have no emojis (the only two emojis in the app) | DONE, CI green `1b34fdf`. Left: a page-range picker or the provider's file upload for PDFs over 15 MB |
| 19.26 | **Appearance redesign (README image 01 note).** Theme mode is four picture tiles (Light, Dark, OLED, Auto split) with one helper line; Theme is three preview tiles (Default, Monet from the wallpaper, Presets) with one helper line; Liquid Glass is a switch row with its own glass preview; Visual quality keeps its three rows with one-line helpers. The Presets page is a two-column grid of mini-screen previews. Shared `ThemePreview.kt` (`MiniScreen`, `PreviewTile`, `GlassPreview`) | DONE, CI green `966c8b3`. The onboarding wizard still has its own older option cards |
| 19.27 | **AI failures (owner log 2026-10-29: HTTP 400 on Gemini and Groq, HTTP 503 on Gemini, nothing shown).** Cause: errors were only red one-line text inside the screen that asked, so a closed popup or a screen left meanwhile showed nothing; 503 gave no advice; the debug log held the code but not the provider's reason; Groq cannot take PDF files and takes at most 5 pictures per request while the app sent PDFs whole and 6 pages per batch. Done: a failure bar on every screen (message, Copy, Close, 10 s); automatic retry of HTTP 500/502/503/504 (2 times, 1 after a big upload); the provider's own reason is written to the debug log; 400/408/413/5xx hints; Groq PDF and picture-count guard; PDFs go whole only to Gemini, Claude, OpenAI and OpenRouter, other services get page pictures, 5 per batch; upload progress reported per whole percent (less redrawing during a big upload) | DONE, CI green `e886ecb`. Frame-drop cluster 19:51 not yet pinned to a screen (needs the raw log); `onTrimMemory level=20` is "UI hidden", not memory pressure |
| 19.28 | **Owner screenshots 2026-10-30 (reading, editor).** (1) AI answers showed raw Markdown (`**bold**`, `- ` lists) in the reading answer popup and the editor AI panel: now drawn as formatted text (`AiMarkdown`). (2) With the keyboard open the "Ask about this note" bar sat behind it: the bottom stack now has `imePadding`, and the bar is a solid pill so note text no longer shows through. (3) The editor AI panel was about 70% of the screen with an answer showing: the action chips fold away while an answer waits for Replace / Insert below / Discard. (4) A checkbox, list, quote, heading, table, divider, picture or template added with the cursor inside a code block landed inside it as raw `- [ ]`: block edits now step out below the closing fence first (`MarkdownEdit.leaveFence`, 3 tests; an unclosed fence is closed). (5) The Gallery / Camera / Scan and Doodle / Table / Record / Audio / Link / Code rows have a short name under each icon | DONE, CI green `d73afc7` |
| 19.29 | **Attendance redesign (README 08, 04).** Holidays and weekends off are hatched circles; Present / Absent / Total are three tinted tiles in one row with big animated numbers; "Last 6 months" capsule chart (hatched empty part, filled part by attendance %, viewed month solid with a % bubble, months under the threshold in the error colour, tap opens that month). New `Hatch.kt`, `MonthBars.kt` | DONE, CI green `d73afc7`. Left for Attendance: per-subject list as tinted rows, striped progress bar (04) |
| 19.30 | **Owner 2026-10-30 (syllabus screen, pic 1).** Too cramped, long titles at headline size: units are now medium titles (three lines at most, ellipsis), chapters body size, a thin progress bar under a unit, 36 dp menu buttons, 40 dp status marks, a rounded guide line for nested rows. The AI outline prompt now asks for short titles (long run-on sentences become a short title with the listed things indented below) | DONE, CI green `fd87162`. Re-import the 89-page PDF to see the effect |
| 19.31 | **Topic dropdown became a sidebar** (pic 2 to pic 3 style): `TopicSidebar` springs in from the left over a dimmed page; "All topics" then units, chapters and topics with note counts, foldable units, chosen one on a tinted pill, swipe left / tap outside / back to close, "Open the syllabus" at the bottom. The editor's Uncategorized chip keeps the small dropdown | DONE, CI green `fd87162` |
| 19.32 | **Outline import takes a document and/or up to 5 pictures** (with or without typed text). Each source is read on its own (PDF whole, Word or text as text, pictures in one request), then `SyllabusTree.mergeOutlines` joins them so a unit named twice appears once; if one source fails and another works the working one is shown. 1 new unit test | DONE, CI green `fd87162`, test added after |
| 19.33 | **Attendance and settings polish.** The 6-month chart scrolls sideways over every month with marks (the viewed month is centred); the calendar swipes left / right to change month with a slide; in Liquid Glass the joining band is cut out where a bead sits (it showed through as lines); "Frosted popups too" is now "Frosted Popups"; two presets, **Sage & Oxblood** (#A5A79B #E8D2B5 #590907) and **Orchid & Ember** (#E7B0DE #400849 #FF5A0A), each with a night and a day variant; search and AI-panel hints stay on one line. **Timetable day view (README 11):** week strip of rounded squares with dates, the lecture in progress is a highlighted card with time left and a progress bar, a "Now" line, past lectures fade, hatched breaks of 30 min or more | DONE, CI green `fd87162` and `93975a4` |
| 19.34 | **AI Hub as a chat (README 05, 13).** Your messages on the right, answers on the left as formatted text with Copy / Delete, "Thinking" bubble with the loading shape, suggestion chips (Explain simply, Make a quiz, Plan revision, Summarise), input pill with mic and send / stop, the last two answers go along as context, "AI is off" bar with Open settings. Requests made elsewhere in the app still appear in the chat | DONE, CI green `ed71aeb` |
| 19.35 | **Owner 2026-10-30/31, bugs batch.** (1) **Syllabus import** now asks the AI for nested JSON (`{"items":[{"title","children"}]}`), reads it into an exact tree (`SyllabusTree.parseReply`), drops course code / hours / credits / scope / objectives / outcomes / prerequisites / marks / references with everything under them (`tidy`), merges the batches (`mergeOutlines`); falls back to the indented-text reader. Rows are smaller; tapping a unit opens a screen with only that unit's tree (`SyllabusScreen(rootId)`). (2) **"The provider's reply was empty."** Gemini thinking tokens count toward `maxOutputTokens`, so the cap is no longer sent for Gemini; empty or blocked replies are retried once and the provider's finish reason is named; image-limit errors ("up to N images") make the caller send smaller batches. (3) **One AI error, not two:** inline texts became a short pointer line and the bottom card is the only place for the reason; the card stays 5 s (was 10 s) and swipes left or right to dismiss. (4) **Add entry:** the date picker is no longer hidden behind the dialog; the absent-date choice now files the note under that date (with a hint line); every "Kind of notes" option has its own strict layout in the prompt (`NoteTemplate`). (5) **Markup tokens:** media, tables and style markers can no longer be cut in half by typing, backspacing or inserting next to them (`NoteBlocks.safeCursor`, `repair`, `insertEmbed`; unit tests). (6) Empty pages start on a random morph shape; the subject folder pill and button shrink on empty pages (the pill counts finger scroll); pulling down at the top of a subject folder opens the drafts popup for that subject only (`DraftPull`); the main FAB and action pills have a shadow; the photo editor has a "Reset rotation" button | DONE, CI green `86482cd` |
| 19.36 | **Absencium AI (renamed from AI Hub), owner 2026-10-30/31.** Gemini-style chat drawn with the app's theme and Material 3 Expressive: **sidebar** (new chat, search, pinned first, a closed read-only "App prompts" folder with what notes / syllabus / homework asked, settings icon to the AI options); the **model name on top** opens a provider + model chooser (only providers with a saved key; model list fetch; typed names; per chat); **"..." menu**: Share conversation (plain text), Pin, Rename, Delete; **long-press your message**: Copy / Edit (edit replaces it and what followed); answers are formatted text with Copy and Answer again; a greeting with a random morph shape and suggestion chips; failed answers say so briefly with Retry (the reason is in the bottom card). Chats are a JSON file on the phone (`AiChats`), never uploaded. **Interlinked with the app, with permission:** per-area switches (Subjects, Timetable, Exams, Holidays, Attendance, Notes titles) all **off by default** (`AiPermissions`); only allowed areas are put in the prompt (`AiChatPrompt`; note text is never sent); the AI proposes changes as a fenced `absencium-actions` JSON block which becomes **cards the student confirms** (Apply / Dismiss / Apply all): add subject, add note, add / move timetable lecture, add / change exam, add holiday, mark attendance (`AiActions`, `AiActionRunner`; unknown or invalid items are dropped, names must match exactly one subject). Tests for the parser, chat JSON and prompt privacy | DONE, CI green `86482cd`. To-do list actions wait for the tasks feature (Step 8) |
| 19.37 | **Per-provider API keys and AI settings redesign (owner 2026-10-30/31).** Each provider keeps its **own key** in the Keystore vault (`ApiKeyVault`, the old single key is adopted by the provider it was saved for), URL and model (`AiProfiles`); switching never asks for a key again. AI & Integrations is now: a status card, the AI Features switch (consent popup kept), **Your providers** cards (tap to use, "In use" pill, edit), **Add a provider** (one form: provider chips, URL, key, model with Fetch models, API style for a custom endpoint, **Test connection**, Save and use, Remove), **What Absencium AI can use** switches, image options, integrations. Onboarding uses the same form | DONE, CI green `86482cd` |
| 19.38 | **Share and export (owner 2026-10-31).** Only **PDF** remains, with a Material 3 Expressive toggle icon button "Themed PDF" that exports with the current theme (light / dark / colours from Appearance); **Sharewave** is shown greyed out until Step 17 (`ShareFormatDialog`, `PdfTheme`) | DONE, CI green `86482cd` |
| 19.39 | **Absencium AI roadmap (owner 2026-10-31, 15 points)** added to Step 11 as rows 11.6 to 11.24, each marked done / partly done / pending. LastChat (AGPL-3.0) is only an idea reference: no code is copied | see Step 11 |
| 19.40 | **Absencium AI, intelligence and background work (owner 2026-10-31 / 11-01):** answers continue when you leave the chat, the screen or the app (`AiChatService`, `AiWorkService`); general-purpose smart prompt, multi-turn chat requests, longer outputs; notes written in full by the note writer at the requested length with the whole editor markup; optional Web search, Thinking and Memory; attachments (pictures, PDFs, text, recordings, videos, notes); assistants with import / export; study difficulty; multiple keys per provider with key then provider failover; token statistics; new chat screen, chat list and "+" sheet; archive; retry with another model; continue. Details in rows 11.6 to 11.27; the notification for a finished answer is documented in 5.6 and waits for the notification groundwork | DONE, CI green `dd2321b` |
| 19.41 | **Absencium AI fixes, reading-mode search, content access (owner 2026-11-01).** (1) **Empty pages:** the Drafts popup and the Search page use `ExpressiveEmptyState`. (2) **Selection scroll:** `KeepScrollOnSelection` keeps the page where it is while the selection pill appears, in the editor (`pageScroll`) and in reading mode. (3) **Reading mode find:** the Search icon opens a find bar over the written words (`FindState` in `MarkdownUi`: per-block match counting, highlights, scroll to the active match, tables included) and a separate Sparkle icon asks the AI. (4) **Absencium AI UI:** greeting and suggestions no longer overlap; incognito chat (`AiChat.incognito`, never saved, no memory) replaces the new-chat icon on the empty page; chat content runs under the bars (`ScreenScaffold(contentUnderBars)`) with soft fades instead of a solid block; the model chooser is a compact bottom sheet with fixed-height provider pills; "Send a note" and study material use one compact picker (`ContentPickerDialog`); the thinking glow became the loading shape; the sidebar slides in and swallows touches. (5) **Facts:** the context tags each subject `[SAFE]` / `[BELOW REQUIREMENT]` and the prompt forbids "needs attention" for safe subjects. (6) **Raw colour tags:** `AiNoteCleaner.repairTags` closes `{color:..|text}` / `{size:..|text}` that lack `|}` (on writing, on load and on display), the note prompt insists on the closer. (7) **Content access:** scope `MATERIAL`, action `read_content` (kind note or material), the card describes what is requested and **Apply** sends the note text or the material file to the AI as the next message (`contentReader`); nothing is sent before approval. (8) References saved in `docs/ui overhaul/Ai/Generical-reference/` | WAITING FOR CI |
