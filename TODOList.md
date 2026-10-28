Absencium: plan and status

**What we are building.** Absencium is an offline-first Android study companion (Kotlin, Jetpack Compose, Material 3 Expressive, Room, minSdk 26). It tracks attendance, timetable, exams, holidays, tasks and reminders, and keeps notes and study material in a Vault of subject folders. A block-based note editor and a per-subject syllabus tree link notes, past papers and guides to topics. Optional AI (your own API keys) helps with extraction and notes. Everything works without a network. There is no sync; sharing comes after 1.0. Built only by GitHub Actions in `skywa1ker404/Absencium-Dev`; the stable `Absencium` repo stays empty until you approve 1.0.0. GPL-3.0.

# Absencium TODO list

Single source of truth for what is done, in progress, pending and planned. **Every specification given so far is merged here** (the master specification, the earlier roadmap, the improvements and features list of 2026-10-18, the Sharing and Sharewave specification and the repository audit request). New requirements are added to the step they belong to, not as a new document; a requirement that changes is struck through and its replacement follows it. The section "Why this order" at the end explains why the steps run in this order (the old roadmap file is merged into it); the steps say *what* and *how far along*. Updated after each set of features, not after every edit.

**Status words.** DONE means it compiled on CI (and is named with the commit). WAITING FOR CI means it is written and pushed but CI has not confirmed it. Nothing is marked DONE that has not compiled. Nothing here has been tested on a device.

**Rules this list follows**
- Bug fixes come before new features.
- A specification that was replaced is ~~struck through~~ and the new one follows it immediately.
- Steps are in implementation order; each heading shows how many tasks the step holds.
- Development, CI, testing and reports happen in `skywa1ker404/Absencium-Dev`. `skywa1ker404/Absencium` stays empty until a stable 1.0.0 is recommended, tested and explicitly approved by Anakin.

Last updated: 2026-10-29 (Home drawer, pinned Subjects button, attendance popup, folder pill like the main pill; CI green `91daba9`). Before that 2026-10-28 (Step 19 batch 3 in progress: notes, study material, syllabus, Home, Today, Trash, Subjects redesigned; keyboard gap and stacked-style bugs fixed; floating folder pill and buttons; CI green `79fbc9f`). Stable-repo secrets and variable: Anakin reports they are set in `Absencium` settings (not verifiable from here; no values were read).

---

## Ledger: everything still pending (owner rule 2026-10-28: never drop a pending task when new requests arrive)

Update this list whenever something is finished or added. Each line points to the row that holds the details.

| Pending | Where |
| --- | --- |
| Liquid Glass batch 1 rest: press / drag squash and highlight on floating buttons, toggles, sliders; calmer tiles inside popups; places that ignore "Apply frosted glass on other elements" (dialogs, menus, pickers) | 19.1 |
| Material 3 Expressive batch 2: unread guide pages (need pasted text), apply the full component set | 19.3 |
| Screen redesign batch 3 from images 3 to 15. **Done:** notes list, study material, syllabus tree, Home, Today, Trash, Subjects. **Left:** Appearance (dense text, theme previews), Vault grid (even folder tiles, image 07), Attendance (calendar grid, stats, images 08 / 04 / 13), Timetable day view with current-lecture highlight (image 11), AI Hub as chat with suggestion chips (images 05 / 13), Settings hub quick-start grid (image 10), profile card, onboarding, editor toolbar as connected toggle grid with an "Ask AI to..." pill (images 03 / 14), reading mode selection pill (image 14), AI options popup restyle and the tree-style topic picker (screens 17 / 18) | 19.4 |
| Owner rule 2026-10-29: open `docs/ui overhaul/README.md` before starting every new UI task, and use Material 3 Expressive components and springy animation on every new or changed element | 19.3, 19.6 |
| Add / import buttons that are still inline buttons and should be floating buttons (owner 2026-10-28): "Add subjects" in the Home and Vault empty states, Test Lab, the Storage screen actions. **Done:** subject folder, Subjects, Syllabus | 19.9 |
| The folder's bottom controls now use the main pill itself; the same pattern is still to be applied to the note editor and reading screens where it fits | 19.9, 19.11 |
| Device re-test of 19.11 to 19.14 (Home drawer, attendance popup, folder pill and scroll hiding, calendar ring, import dialog) | 19.11 to 19.14 |
| Device re-test needed (nothing is device-tested): keyboard gap fix (19.7), stacked styles (19.8), AI options popup (19.2), folder bottom pill and buttons (19.9) | 19.2, 19.7 to 19.9 |
| Reading mode: markup tools saved with the note (the reading themes and Eye comfort were removed on 2026-10-28 at your request) | 10.26 |
| Audio notes: play / pause in the mini-player (the player is now a docked bar above the pill, no floating bubble; changed 2026-10-28) | 10.29 |
| Hide the mic button while scrolling | 10.28 |
| "Paste" in the multi-select toolbar | 10.32 |
| "AI is disabled. Enable it or ignore" on every AI button, consent popup | 10.34 |
| Subject folder: sidebar, sort menu, card view (the 3-tab pill is now a floating glass pill at the bottom with a button above it, and the topic filter is a menu on the subject name; CI green `79fbc9f`) | 10.15. **2026-10-28 (CI green `79fbc9f`, not device-tested):** the tabs are a floating glass pill at the bottom, the main action is an extended floating button above it on the right, and the topic filter is a menu that opens from the subject name in the title bar (see 19.9, 19.10) |
| Syllabus: practical syllabus option (needs a migration), "generate notes for this topic" and the syllabus summary card in the profile | 10.16, 10.18 |
| Batch D: pages, doodle shapes and layers, syllabus-aware AI (10.36), text reflow beside media | 10.36 onward |
| Pre-1.0 audit with your consent; Steps 14 to 16 | Steps 3, 14, 15, 16 |
| Waiting on you: device testing of everything marked "not device-tested", audit and cleanup approval, the open decisions in `docs/MASTER_SPEC.md` | CURRENT.md |

## Step 0. Build, signing and release separation (6 tasks)

| # | Task | Status |
| - | --- | --- |
| 0.1 | Dev application ID `skywa1ker404.absencium.dev`, stable ID `skywa1ker404.absencium`, chosen by `absenciumChannel` | DONE (CI green `6834a09`, not device-tested) |
| 0.2 | Dev signs with the public throwaway key `app/signing/absencium-dev.jks`; stable signs only from the four `ABSENCIUM_*` secrets | DONE (CI green `6834a09`, not device-tested) |
| 0.3 | ~~CI publishes a `build-N` prerelease on every push~~ CI builds, tests and uploads APK artifacts only; releases come from the manual `dev-release.yml` | DONE (CI green `6834a09`, not device-tested) |
| 0.4 | APK verification script (`verify_apk.sh`: ID, certificate, ABIs, SHA-256) | DONE (CI green `6834a09`, not device-tested) |
| 0.5 | Stable repo templates in `release/stable/` and `scripts/export_stable.sh` with a link check | written, export dry run pending |
| 0.6 | **Bug:** the "Identity and secret guard" matched its own pattern list in `android.yml` and failed `assemble-debug`. Fixed by excluding `.github` and `release` from the scan; error comment no longer empty | DONE (CI green `6834a09`, not device-tested) |

## Step 1. Reported bugs and small UX fixes (8 tasks)

| # | Task | Status |
| - | --- | --- |
| 1.1 | Default Theme description no longer says "Everforest" | DONE (CI green `6834a09`, not device-tested) |
| 1.2 | ~~Default colour engine: Monet~~ the app starts on the Default Theme; it can still be changed in onboarding | DONE (CI green `6834a09`, not device-tested) |
| 1.3 | ~~Restore card: "Close app"~~ "Restart app" relaunches Absencium so the staged restore is applied | DONE (CI green `6834a09`, not device-tested) |
| 1.4 | Liquid Glass options greyed out and not tappable in Performance mode (onboarding tile and panel, Appearance settings switch, blur slider, "Apply frosted glass on other elements") | DONE (CI green `6834a09`, not device-tested) |
| 1.5 | Calendar month forward/back: short fade-through (90 ms out, 210 ms in) while the card animates its height | DONE (CI green `6834a09`, not device-tested) |
| 1.6 | Tap "Month Year" in the calendar header: centred popup with year arrows and a month grid | DONE (CI green `6834a09`, not device-tested) |
| 1.7 | ~~Plain platform date dialogs~~ date chooser is an app popup that follows "Apply frosted glass on other elements" (used by the holiday/exam editor and Add Entry custom date) | DONE (CI green `6834a09`, not device-tested) |
| 1.8 | About: website link https://skywa1ker404.github.io/apps/absencium/ | DONE (CI green `6834a09`, not device-tested) |

## Step 1b. Licence review fixes before GPL-3.0 approval (8 tasks)

| # | Task | Status |
| - | --- | --- |
| 1b.1 | 16 untraceable icons replaced with Google's Material path data (Apache-2.0); Draft uses `edit_note` | DONE (CI green `8099a04`, not device-tested) |
| 1b.2 | ~~AI-generated `hero.png` and `showcase.png`~~ original illustrations (`docs/art/`, rendered by `render.py`), labelled as illustrations | written, images checked by eye; no code involved |
| 1b.3 | ~~Obtainium's badge image~~ plain text badge drawn for the project | written |
| 1b.4 | ~~Monokai Pro preset~~ removed (21 presets); stored selection falls back to the default preset | DONE (CI green `8099a04`, not device-tested) |
| 1b.5 | ~~YouTube captions: unofficial endpoint opt-in switch~~ superseded by 1c.1 | DONE (CI green `8099a04`, not device-tested) |
| 1b.6 | Third-party notices, README licence paragraph, `LICENSE_AUDIT.md` updated; notices no longer link dev-only files | written |
| 1b.7 | Stable export dry run: file set builds, link check passes except `LICENSE` (expected) | DONE for the file set; stable `README.md` drafted (see Step 16) |
| 1b.8 | Decisions needed from Anakin: authorship wording, logo ownership, keep or drop the captions switch, old keys | OPEN |

## Step 1c. Resolved audit decisions (8 tasks)

| # | Task | Status |
| - | --- | --- |
| 1c.1 | YouTube option (b): unofficial caption fetch and its setting removed; Gemini link reading kept; paste-the-transcript box for other providers; tile and screen renamed "YouTube Video Notes" | DONE (CI green `b6c0288`, not device-tested) |
| 1c.2 | Authorship sentence added to the README FAQ and licence section (not a substitute for the GPL text) | written |
| 1c.3 | `absencium-upgrade.jks` removed from the tree, `.gitignore` blocks keystores, passwords out of the docs, migration plan for friends' APKs in `docs/SIGNING.md`; history not rewritten | DONE (CI green `b6c0288`, not device-tested) |
| 1c.4 | Stable CI guard: commits must be authored by skywa1ker404 (staged in `release/stable`, not published) | written, runs only after publishing |
| 1c.5 | Logo provenance recorded (GPT, by the owner's account); OpenAI terms read. ~~Original replacement proposed~~ Owner decided to keep the current logo | DONE (documents) |
| 1c.6 | ~~Contributor sentence only explained~~ Contributor-licence sentence applied to `CONTRIBUTION.md` | DONE (text; CI green `af7303a`) |
| 1c.8 | In-app "Open-source licences" screen (About, Legal) with notices copied into assets; CI checks the copies match `docs/` | DONE (CI green `af7303a`; not opened on a device) |
| 1c.7 | `LICENSE_AUDIT.md` and notices updated; source scanner could not run (no PyPI), so verification is incomplete | DONE (documents) |

## Step 1d. Reported on-device issues and release docs (7 tasks)

| # | Task | Status |
| - | --- | --- |
| 1d.1 | Navigation pill: page text no longer shows garbled through the pill and the selection bead (denser veil, light 4 dp softening); the bead's lens refraction is kept; label fades in only when fully open | DONE (CI green `525d03b`, not device-tested) |
| 1d.2 | AI & Integrations: API style chips wrap instead of squeezing "Google Gemini" into single letters | DONE (CI green `525d03b`, not device-tested) |
| 1d.3 | Page headers: title area nearly opaque so scrolled text cannot bleed through it | DONE (CI green `525d03b`, not device-tested) |
| 1d.4 | Long lists: licences screen and subject folders are lazy lists with a scrollbar on the licences screen; search waits for a typing pause and scores only the first 20,000 characters of a note; model filter remembered. Other screens with long text not yet profiled | DONE for these screens (CI green `525d03b`); lag on other screens (for example very long notes) **still open, not measured** |
| 1d.5 | "No colour" preset (neutral greys), listed first and offered only while Liquid Glass is on; with glass off it falls back to the default theme | DONE (CI green `525d03b`, not device-tested) |
| 1d.6 | Stable export now includes `LICENSE`, the stable README and the images it links; link check skips the bundled notice copies; export fails on a broken link or a keystore | DONE in a temporary directory (not CI) |
| 1d.7 | Dev README, stable README (`release/stable/README.md`) and `release/stable/release-notes/v1.0.0.md` drafted from source-checked facts; the release notes still need the certificate fingerprint pasted in | written, awaiting Anakin's review |

## Step 1e. Pill drag, title and scrollbar corrections (5 tasks)

| # | Task | Status |
| - | --- | --- |
| 1e.1 | Pill look restored exactly as before step 1d (veil, lens bead, label fade, header fades); the 1d.1 and 1d.3 changes are withdrawn at the owner's request | DONE (CI green `14c41fd`, not device-tested) |
| 1e.2 | **Bug:** dragging the selection pill to the last tab made it shake. Cause (worked out from the geometry, not measured on a device): the pill is centred and narrows as the last tab opens, so its left edge moves under the finger, and the drag position was measured from that edge; near the end the highlight's on-screen position even runs backwards for a few pixels. Fix: measure from the pill's centre and place the finger between each tab's resting spot, which only rises | DONE (CI green `14c41fd`); **needs your device check** |
| 1e.3 | Licences scrollbar can be dragged (28 dp strip at the right edge) | DONE (CI green `14c41fd`, not device-tested) |
| 1e.4 | Liquid Glass look: soft sheen on glass cards and a fourth colour plus a light streak in the glass wallpaper, after the reference pictures; the pill and floating bars are untouched | DONE (CI green `14c41fd`, not device-tested) |
| 1e.5 | Docs-only push test: a documentation-only commit (this file) pushed without `[skip ci]` to see whether CI stays idle | DONE: CI stayed idle for docs-only commit `8068ba0`; the next code push `14c41fd` started a run |

## Step 1f. Optional glossy glass (1 task)

| # | Task | Status |
| - | --- | --- |
| 1f.1 | ~~Settings, Appearance: "Glossy glass" switch, shown only while Liquid Glass is on, **off by default**; adds a shine band and a glint to glass cards and buttons (not to the pill or floating bars)~~ **superseded: the option is removed (10.20)** | DONE (CI green `1d33a15`), removal pending |

## Step 1g. Popups, date picker, chips and glass lag (7 tasks)

| # | Task | Status |
| - | --- | --- |
| 1g.1 | Search and + button square artefact: the slide-away no longer fades (a fade made the glass blur show as a pale square) | DONE (CI green `2fb6710`, not device-tested) |
| 1g.2 | Year list of the date picker had no background (Add Entry for the calendar and for notes): container is now opaque | DONE (CI green `a38c782`, not device-tested) |
| 1g.3 | Lag in popups in Liquid Glass (Add Entry, AI import confirmation list): the glass pane is now its own layer under the content so scrolling, toggling and typing do not redo the blur. Cause is reasoned from the code, not measured | DONE (CI green `a38c782`, not device-tested) |
| 1g.4 | "College Holiday" chip wrapped onto two lines: chips wrap as a row and labels stay on one line | DONE (CI green `a38c782`, not device-tested) |
| 1g.5 | Liquid Glass lag everywhere: cards (`glassSurface`) no longer blur. They only ever sampled the smooth wallpaper, so the blur was invisible but cost a blur pass per card per frame; the glass look, sheen and vibrancy are kept. The floating pill, bars and popups keep their real glass. Cause is reasoned from the code, **not measured** | DONE (CI green `795afcc`, not device-tested) |
| 1g.6 | Present/Absent switch: the translucent selection that showed behind the animation and vanished afterwards came from the highlight being a backdrop-sampling glass control inside a fading, scaling layer. It is now a flat tinted fill with the same sheen. Reasoned from the code, since the recording cannot be seen | DONE (CI green `795afcc`, not device-tested) |
| 1g.7 | Lag in the AI model selector list in Liquid Glass: its rows are plain fills; the card around it was the blurred part, so 1g.5 should cover it. Not separately changed or measured. If it still lags, the next step is to make the list lazy (it composes up to 40 rows at once) | DONE with 1g.5, **needs a device check** |

## Step 2. Open bugs and checks on what already compiled (7 tasks)

Bugs come before features. Everything marked DONE above compiled on CI but has not been tried on a phone.

| # | Task | Status |
| - | --- | --- |
| 2.1 | Device check of the CI-green changes: pill drag to the last tab (1e.2), licences screen and scrollbar drag, API style chips, No colour preset, glossy glass | pending |
| 2.2 | **Bug:** Present / Absent Today buttons: the ghost selection behind the animation is fixed in 1g.6 (owner described it on 2026-10-19). Double taps are also ignored for 450 ms (2.7) | DONE (CI green `795afcc`, not device-tested) |
| 2.3 | ~~Navigation pill label clipping~~ the owner prefers the current pill look (1e.1); re-open only if the clipping is seen again after the drag fix | pending |
| 2.4 | ~~Garbled text under the glass navigation bar~~ withdrawn the same way; the title-area and veil changes of 1d.1 / 1d.3 were reverted | superseded |
| 2.5 | Lag with a lot of text or many entries on screens other than the licences screen, subject folders and search (for example typing in a very long note): measure on the device first, then fix the measured cause | pending |
| 2.6 | Prevent stale saves overwriting newer edits, make the save-status indicator accurate, predictable draft creation and deletion (data-loss class, from the reliability list) | **partly done** (CI green `795afcc`, not device-tested): saves of notes now run one at a time in request order, and saving a note that was deleted while its editor was open updates the Trash row instead of inserting a duplicate; drafts are deleted at once with their files, other entries go to the Trash. **Not done:** the save-status indicator and a check for a note changed elsewhere |
| 2.7 | Rapid-tap guard: prevent duplicate actions from double taps on save, delete, mark present/absent | **partly done** (CI green `795afcc`, not device-tested): every `PillButton` and both halves of the Present/Absent switch ignore a second tap within 450 ms. **Not done:** other clickable rows and icon buttons that save or delete |

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
| 4.1 | Room version 2 as a real `Migration` with an exported schema (never destructive): stable `uuid` per syncable row, `updatedAt` revision, `deletedAt` tombstone, plus the `teacher` column the timetable needs. Later versions add columns the same way | DONE (CI green `795afcc`, not device-tested). The migration SQL was run in SQLite on a copy of the v1 schema and its result is identical to the v2 schema Room exported (columns, types, defaults, indices, foreign keys). `updated_at` is also kept current by database triggers. **Not run on a phone**, and there is no instrumented migration test yet (see 4.8) |
| 4.2 | Trash and undo: deleted rows are tombstoned, shown in a Trash screen, restorable, purged after a set time; the undo toast of the timetable work uses it | DONE for notes, exams, holidays and lectures (CI green `795afcc`, not device-tested): Trash screen (opened from the trash icon in the Vault; removed from Settings on 2026-10-22), 30-day purge at start-up, undo bar. Subjects and attendance entries are **not** trashed (deleted at once) |
| 4.3 | Versioned export format: manifest (format version, app version, schema version, created, counts, SHA-256 per file), the database, attachments, settings without secrets; import validates manifest and hashes first | DONE (CI green `795afcc`, not device-tested). Manifest rules are covered by JVM unit tests that CI runs; the manifest JSON reader/writer uses `org.json` and is **not** unit-tested |
| 4.4 | Backup and restore on top of it: attachments included, restore validation, duplicate detection, interrupted-import recovery, low-storage check, backup status shown in Settings | DONE (CI green `795afcc`, not device-tested): staged restore with rollback, "same as current" detection, low-storage check, last-backup line, restore preview. **Not run end to end on a phone**; there is no automated export-wipe-import test (it needs Android) |
| 4.5 | Optional password encryption of backups (AES-256-GCM, key derived with a reviewed library, no hand-written crypto) with a threat model written in `docs/PRIVACY.md` | DONE with a deviation (CI green `795afcc`, not device-tested): PBKDF2-SHA256 and AES-256-GCM come from the Android platform, not a separate library, and the 64 KiB chunk framing around them is our own code. That framing is unit-tested but **not independently reviewed**; the limits are written in `docs/PRIVACY.md` |
| 4.6 | ~~Rolling local snapshots (3)~~ **Removed 2026-10-22 on the owner's request** (interval backup): the daily snapshot, its Settings switch and its list are gone. The manual backup and restore of 4.4 stays | REMOVED (CI green `a54febc`, not device-tested) |
| 4.7 | ~~Seams for later steps (Transport, change-set builder, read-only scope)~~ **Removed 2026-10-22** with all sync code; Step 17 builds what it needs when it starts | REMOVED (CI green `a54febc`, not device-tested) |
| 4.8 | Test infrastructure: Room migration tests, export/import round-trip tests, preference-recovery test; CI runs them | **partly done** (CI green `795afcc`, not device-tested): JUnit 4 added; JVM tests for the encryption (round trip, wrong password, bit flips, truncation, reordering), manifest rules, Trash retention and the local transport run in `testDebugUnitTest` on CI. CI's log is not readable from here, so the test count is not confirmed beyond the step passing. **Not done:** Room migration test (needs an instrumented runner), full export/import round trip, preference-recovery test |
| 4.9 | Safe preference recovery (corrupt DataStore falls back to defaults) and preserve data when subjects are renamed or moved | **partly done** (CI green `795afcc`, not device-tested): a corrupt DataStore file is replaced with defaults and logged. Rename/move safety is **not verified**: rows refer to subjects by id, so a rename should keep them, but no test or check was done |

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
| 5.6 | Notification groundwork: channels per purpose, POST_NOTIFICATIONS handling with a denied-permission explanation, exact-alarm decision (inexact by default, exact only when the user opts in), reboot rescheduling receiver, per-API feature table in `docs/ROADMAP.md` | pending |
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
| 8.1 | Tasks and reminders with smart filters, recurring items, snooze, quick actions, past-deadline handling, duplicate-notification prevention, quiet hours, notification preferences | pending |
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
| 10.2 | Vault as a file explorer (breadcrumbs, badges, long-press multi-select, batch actions), drafts folded in; duplicate filenames, deleted subjects, empty folders, large images, inaccessible files and low storage handled | first version DONE (CI green `24af814`, not device-tested): breadcrumbs, badges, long-press multi-select with Finish/Move/Delete/Share, drafts in the folder, empty-folder text. Not done: large-image and inaccessible-file handling. **The folder page is redesigned by 10.15**, which replaces this layout |
| 10.3 | Local OCR (Tesseract with downloadable language packs, separate from AI), PDF text extraction, EXIF scrubbing on export; blurry and rotated image handling, unsupported-language message, editable OCR preview | **unblocked 2026-10-21**: you named tesseract-ocr/tesseract (Apache-2.0, verified on its GitHub page). That is the C++ engine, so the app uses the Android wrapper Tesseract4Android (adaptech-cz, Apache-2.0, minSdk 21, bundles Tesseract 5.5.1 and Leptonica; coordinates `cz.adaptech.tesseract4android:tesseract4android:4.9.0` from JitPack, read from its README). To do at build time: confirm CI can resolve it from JitPack, check the bundled Leptonica, libjpeg and libpng licences, and add all of them to `docs/DEPENDENCY_LICENSES.md`. Language data files are downloaded by the user, not bundled. EXIF scrubbing on export does not need it and is planned with 10.11 |
| 10.4 | "Extract Text" popup (With AI or Locally); finish the Add Entry actions; link missed classes to notes and attachments | **Add Entry actions CI green `a54febc`, not device-tested**: Text Notes (empty note, opens the editor), Document / File (any file, listed under the note and opened in another app), Audio Note (recorder with the microphone permission, shows in the note with play), Solve Homework (typed question and/or photos to your AI, answer saved as the entry), Generate AI Notes (topic to your AI). **Not done:** the "Extract Text" popup with the local option (needs 10.3), linking missed classes |
| 10.5 | Timetable OCR into the editable import preview of Step 6 | pending |
| 10.6 | Note export and share: PDF, Markdown, ZIP, text, through Android's Sharesheet with a `FileProvider`, in a small reusable `ShareService` | PDF, Markdown, text and ZIP DONE earlier (CI green `24af814`). **PDF now follows the page (CI green `a54febc`, not device-tested):** headings, lists, alignment, colours, sizes, highlight, quotes, code, tables with borders, and pictures and doodles at their size and alignment; recordings are left out. **Not done:** ZIP export still writes the note text with the `{{image:..}}` lines and does not strip photo metadata (EXIF, with 10.3) |
| 10.7 | Audio memos | **CI green `a54febc`, not device-tested:** recorder (AAC .m4a), the Audio Note stage in Add Entry, and an inline player in the note. **Not done:** timestamps inside notes, voice-to-text |
| 10.8 | ~~Editor modes: Edit and Preview, check mark goes to the preview~~ **Live editor (owner spec 2026-10-22):** one always-editable page that already looks exactly like the preview (headings, lists, check boxes, colours, sizes, alignment, pictures, doodles and tables), no separate preview mode, formatting characters hidden; tapping a check box flips it, tapping a [[link]] opens the note | CI green `a54febc`, not device-tested |
| 10.9 | Smaller header: a smaller title and one small line with date, character count, category and save state. The category says "Uncategorized" until the note is linked to a syllabus topic (needs 10.16) | header CI green `a54febc`, not device-tested; category picker pending |
| 10.10 | Bottom toolbar of icon-only buttons (Text style, Alignment and lists, Checklist, Add pictures, Add more); **every tap puts the keyboard away** so the page and panels can be seen. Text style panel tabs: Style, Symbols, Dividers, Templates; lists panel: left, centre, right, bullets, numbers, letters, quote; Add panel: Table, Doodle, Link a note, Code block. Formatting: bold, italic, underline, strike, highlight, code, colour, size, headings. **Not done:** the AI button, scan document and recording from inside the note, the paper and page adjustment | CI green `a54febc`, not device-tested (icons are drawn from path data written by hand, so they may need a visual pass on a device) |
| 10.11 | Notes are made of blocks: text, pictures, tables, doodles, recordings. Pictures are inserted at the cursor as their own block; tap one to move it up or down, set small, medium, large or full width, align it, crop and rotate (the crop editor), view it full screen or remove it; a round handle drags the width. Tables open a cell editor with add and remove row and column. A thin strip between two blocks adds a line to type on. **Not done:** text flowing beside a picture, Copy / Share / Save of a single picture | CI green `a54febc`, not device-tested |
| 10.12 | Doodle canvas: pen, eraser, six colours, size slider, undo and redo, plain, ruled or squared paper; a saved doodle is a PNG that can be drawn on again | CI green `a54febc`, not device-tested (opacity, colour spectrum and a stylus pressure option are not done) |
| 10.13 | More menu: Notes info, Share or export, Mark as finished, Delete (to the Trash). Undo and redo are in the top bar. **Not done:** page adjustment (paper style, texture, margin); Reminder, Lock and Generate widget (added when Steps 8, 14, 13 exist) | CI green `a54febc`, not device-tested |
| 10.14 | AI in the editor: the AI button opens a prompt bar where you type your own instruction and send; AI creation actions (Summary, Polish, Shorten, Extend, Continue writing, Revise grammar, Extract to-dos, Translate and the like); the three-dot menu of the AI panel opens the AI history. Needs the AI settings to be set up; works with the providers already in the app | pending |
| 10.15 | **Subject folder redesign (owner spec 2026-10-21).** Header shows the subject name with an arrowhead that opens a sidebar: All notes (every kind: absent, normal and others), Absent notes (ordered by which to write next, with finished ones marked), and a filter by syllabus topic, chapter or unit. A sort menu with list and card view. The + button on All opens the editor at once; on Absent it opens the date selector and then the editor. Search and + sit where the main page has them. Bottom: the same pill as the main menu (same animation and theme) with three tabs: Notes, Syllabus (this subject), Study material. The AI button sits to the right of the pill when AI is set up | PARTLY DONE (CI green `045103c`, not device-tested): inside a subject there are now Notes, Study material and Syllabus tabs and a filter by topic (unit, chapter or topic, including everything inside it). **Not built:** the sidebar, Absent notes ordering, sort menu and card view, the pill-style tabs; these are replaced by the layout redesign of Step 19 |
| 10.16 | **Syllabus (moved into 1.0 by the owner; was 12.3).** Per subject a tree: unit, chapter, topic, subtopic, with status (not started, in progress, completed) and progress. Import with AI from a photo, PDF or text, and ask AI to generate notes for a topic. A main Syllabus button on the Vault page's + menu shows every subject (reference layout from the owner, in the app's theme). Needs database version 3 | DONE except AI notes per topic (CI green `045103c`, not device-tested): per-subject tree (unit, chapter, topic, subtopic), status that cycles on tap (a unit or chapter sets everything inside it), progress, add, rename, details, move up and down, delete (filed notes go back to Uncategorized), import from typed text or a pasted page ("Tidy with AI") or a file read with the AI, a Syllabus overview of every subject from the Vault + menu. Database version 3 already holds the table. **Pending:** "generate notes for this topic" (with 10.36) |
| 10.17 | Study material tab per subject: PYQ, answer book, guide (extends the existing Guides and Question Banks) | DONE (CI green `045103c`, not device-tested): Study material tab groups previous year papers, answer books, guides and syllabus documents, each filed under a topic; adding uses the existing Guide / Answer PDF entry |
| 10.18 | Everything connects: notes, PYQs and study material can be linked to a syllabus topic, chapter or unit (the Uncategorized picker); the folder sidebar filters by it; a syllabus summary card in the profile (with the profile overhaul in Step 14) | PARTLY DONE (CI green `045103c`, not device-tested): a note is filed from the editor menu ("File under a topic"; the header and Notes info show the real category) and a guide from its card; the folder filters by topic. **Pending:** syllabus summary card in the profile (Step 14) |
| 10.19 | Vault page: a trash icon opens the Trash | DONE (CI green `656a670`/`02e5d73`, not device-tested) |
| 10.20 | Remove the "Glossy glass" option from Liquid Glass settings | DONE (CI green `8960989`, not device-tested) |
| 10.21 | **Owner spec 2026-10-22:** remove the tag option; no difference between editing and preview; every functional button an unmistakable icon instead of text; a slightly smaller editor header and info; any toolbar tap minimises the keyboard; keep this long TODOList; remove everything related to sync and interval backup; PDF export follows the note exactly (pictures, doodles, tables, everything except audio); finish the other Add Entry actions; remove Trash from Settings; update docs | CI green `a54febc`, not device-tested (all parts written; none confirmed on a phone) |
| 10.22 | Trash removed from the Settings hub (the Vault's trash icon is now the only way in) | CI green `a54febc`, not device-tested |
| 10.23 | **AI generation (owner spec 2026-10-24/25):** answers use the editor's real formatting (headings, tables, comparison tables, lists, checklists, highlights, colour, centred Unicode formulas), choose sections per topic, never show code or LaTeX, sound human. Add Entry options: Notes length dropdown (Moderate / Lengthy / In Depth), Kind of notes (Full note, Key details line by line, Summary, PYQ, Last-minute preparation, Practical observation notes), link to the subject's syllabus tree (unit / chapter / topic), course / year / semester, Tables and graphs, Add previous year questions, Fetch diagrams from the internet (Wikimedia Commons, optional), Generate diagrams with AI (optional), Assignment format for homework. **What the user typed always wins** | pushed `99839b5`, waiting for CI, not device-tested. **Not done:** practical syllabus tree, voice input on prompt fields, same options inside the editor AI panel |
| 10.24 | Extract text / transcribe everywhere: picture, PDF, Word, text and audio files, "With AI" or "Locally", PDF viewer, icon-grid attachment menus (2 or 3 columns), remove attachment | pushed `99839b5`, waiting for CI. **Not done:** the same choice inside Add Entry and Import popups, 4th Import option for documents, recorder text overlap check |
| 10.25 | **Pages:** a note scrolls as page 1, page 2 ...; media can be dragged between pages | NOT STARTED |
| 10.26 | **Reading mode** (3-dot menu): no editing, zoom and scroll, Edit FAB, 3-dot menu with Share, Focus mode, Light / Dark / Eye comfort, Mark as finished (removes it from the preview); markup tools saved with the note; bookmark a page and continue from there | DONE except markup tools (CI green `602e2b2`, not device-tested): reading mode from the editor menu with zoom, themes (Auto, Light, Dark, Eye comfort), bookmark and continue, Edit button, Mark as finished. **Pending:** markup tools saved with the note |
| 10.27 | **Selection pill in reading mode:** Explain with AI, Translate (language chooser, also in the main AI popup), Word meaning, and a search bar to ask about the selection or the whole note; the answer opens as a bottom popup that minimises and maximises | DONE (CI green `602e2b2`, not device-tested): selection pill (Explain, Translate with language chooser, Meaning, Ask), ask bar, answer popup that minimises and closes |
| 10.28 | **Voice search icon** in every AI prompt field, hidden while scrolling | PARTLY DONE (CI green `602e2b2`, not device-tested): mic button (offline speech recogniser preferred) in the AI panel, the AI task prompt and the reading ask bar. **Pending:** hiding it while scrolling |
| 10.29 | **Reading miniplayer:** minimise the reading mode to a miniplayer (thumbnail, note name and page number, chapter / unit / subject from the syllabus link, play / pause when an audio note is playing); swipe left or right pins it as a half-hidden bubble (tap to reopen, drag to move, drag onto X to dismiss); swipe down dismisses; tap or swipe up maximises; shown on every page of the app | PARTLY DONE (CI green `602e2b2`, not device-tested): miniplayer with the bubble, drag to X, swipe gestures and reopen. **Pending:** play / pause for audio notes |
| 10.30 | **Doodle shapes and layers:** many geometric shapes, layers, everything editable again | NOT STARTED |
| 10.31 | **Stacked formatting:** several styles combined on one piece of text must render and edit correctly (example from the owner: alignment + size + strike + underline + highlight + italic + code) | DONE (CI green `20ab275`, `045103c`; JVM tests run by CI, not device-tested): bold-italic, nested colour and size tags pair up, the innermost one wins in the preview as in the editor, code spans no longer break tag pairing, regression test with the owner's example string. **Reopened and rewritten 2026-10-28** (owner: raw codes appeared when a style was applied twice after 2 or 3 others, e.g. `- [ ] a) > a) > - a) > a) *++`{size:12|# **~~`...`): see 19.8 |
| 10.32 | **Multi-select in a subject folder:** holding a note shows a floating navigation pill at the top with the count, Cancel and actions that change with the action (Move, Cancel, Paste ...) | DONE except Paste (CI green `f116f26`, not device-tested): floating Material 3 Expressive toolbar with the count, Finish / Reopen, Share, Move, Delete and Cancel |
| 10.33 | Vault FAB gets "Edit subjects" (opens the Subjects tab) | DONE (CI green `f116f26`, not device-tested) |
| 10.34 | **Rename "Ask AI" to "AI Features".** The switch disables everything AI: the AI Hub button next to the pill disappears (still in Profile with a "disabled" note); every AI button or popup says "AI is disabled. Enable it or ignore"; only local extraction works. Turning it on shows a transparent consent popup (Agree / Decline), the same one the onboarding gets when it is overhauled | PARTLY DONE (CI green `f116f26`, not device-tested): named "AI Features"; the AI Hub button hides when it is off and stays in Profile with a note. **Pending:** "AI is disabled. Enable it or ignore" on every AI button and the consent popup |
| 10.35 | **AI Hub history:** every AI action, input prompt and result is kept in the AI Hub | DONE (CI green `f116f26`, not device-tested): AI Hub keeps every request and answer on the phone (200 newest), delete one or all |
| 10.36 | Every AI feature (summaries, quizzes, extraction, import) becomes syllabus-aware and humanised like 10.23 | NOT STARTED |
| 10.37 | The editor's navigation pill selected icon uses the same selection effect (distinction and light refraction) as the main pill | NOT STARTED |
| 10.38 | All UI elements, layouts and animations consistent across the app | continuous |
| 10.39 | **Before 1.0:** audit all project files; remove junk, loopholes, broken or unfinished code and performance problems (only with your consent, without removing any feature or quality) | Step 16 |

**Tests:** Large-image and low-storage runs, a note with 50 k characters, share to two apps and to a missing app. · **Effort:** XL

## Step 11. AI Hub and AI settings (5 tasks)

**Deferred to 1.x by the owner (2026-10-18), except any dependency the approved core scope needs.**

Builds on the existing providers, model picker and key vault; adds to them rather than replacing them.

| # | Task | Status |
| - | --- | --- |
| 11.1 | Several API keys per provider with a setup guide dropdown | deferred to 1.x |
| 11.2 | Show Gemini as recommended (can read YouTube links) and OpenRouter as recommended for its free/paid listing; look up and verify free models of the other providers before showing a recommended all-rounder for each; for Gemini recommend the Flash-Lite 3.5 model if the fetched model list contains it (otherwise the nearest Flash-Lite entry that list does contain); the name always comes from the fetched list, never typed from memory; other providers under an "Other models" dropdown; guide for getting the chosen provider's key | deferred to 1.x |
| 11.3 | Clear explanation of what data is sent to an AI provider; AI failures never block local features; AI stays optional and off by default | deferred to 1.x |
| 11.4 | Ask AI inside notes with query history and jump-to-source (from the master specification); voice prompt on long-press of the AI icon; optional AI Memory (off by default) | deferred to 1.x |
| 11.5 | Design reference from the repository audit (Agora, LastChat) for conversation management, context handling and tool calling, decided after the audit | deferred to 1.x |

**Tests:** Provider list fetched offline and online; a failing key leaves notes untouched. · **Effort:** L

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

## Step 19. Full UI overhaul: Liquid Glass, Material 3 Expressive, new layouts (owner request 2026-10-27, **now active**)

Replaces the old "restyle after the reference kit" task. Reference material lives in `docs/ui overhaul/` (15 images + README with a screen-by-screen table); every UI change opens the matching image first. **Only the 3-button navigation pill keeps its design** (shape, light-bending lens, bead). Everything else may change, including swapping one kind of element for another. Colours always come from the theme; Liquid Glass off and Performance mode must keep working. GPL-3.0: patterns from Apache-2.0 references (Kyant0 Backdrop, Meticha, Material code) are fine; nothing copied from the mock-up images.

| # | Task | Status |
| - | --- | --- |
| 19.1 | **Batch 1, Liquid Glass.** Go through image 01 (9 screens) grid by grid, then rebuild the glass on the Kyant0 Backdrop patterns (`https://github.com/Kyant0/AndroidLiquidGlass`: backdrop blur, vibrancy, lens refraction, flat theme veil, thin edge, soft shadow, press and drag interaction), **no glow, no haze**. Real backdrop glass on floating controls, FABs, top bars and popups; clean flat tinted panes for scrolling cards (many blurred layers lag). Fix what image 01 shows: content under the pill (bottom padding), stacked identical cards in Trash / Subjects / Today, dense Appearance text, uneven Vault tiles, popups showing unblurred content | IN PROGRESS. Done, CI green `25f76a2`, not device-tested: popups use a stronger blur (18 dp minimum), a denser veil and Kyant's plain edge; card panes have one quiet hairline edge instead of the bright gradient rim; the wallpaper gets one broad soft patch of the theme colour; no glow anywhere. **Also done, CI green `a1a755e`, not device-tested:** every stock dialog (17 files) is now the app's own dialog: large rounded corners, thin edge, see-through in the theme colour with Liquid Glass + frosted overlays and the system blur behind it on Android 12+ where the phone allows it; one themed menu (`AppMenu`) for the 3-dot menus. **Still to do:** Kyant press / drag squash and the pressed-highlight shader on floating buttons, glass toggles and sliders, calmer inner tiles in popups, then the layout fixes for Trash, Subjects, Today, Appearance and Vault (finished in batch 3) |
| 19.2 | **AI options popup (image 02).** Content scrolls with `imePadding`, no fixed heights, chips wrap, field never clipped, glass layer | FIX MADE, CI green `25f76a2`, needs your device check: the popup's size animation could outgrow the space left by the keyboard and cut off the bottom; the height limit now comes first. **Second report 2026-10-28 (any text field low on the screen, popup jumps up and leaves a gap until you type):** see 19.7 |
| 19.3 | **Batch 2, real Material 3 Expressive.** Research first and write the verified results to `docs/ui overhaul/M3E_RESEARCH.md`: every page of `developer.android.com/design/ui/mobile/guides`, `m3.material.io/blog/m3-expressive-motion-theming`, the compose-material3 release notes (1.4.0-alpha11 up to the version in use, 1.5.0-alpha29), Android 16 and 17 design changes, and the repositories Minus, PixelPlayer and June (cloned separately, usage patterns only, licences checked). **Research status 2026-10-28 (see `docs/ui overhaul/M3E_RESEARCH.md`):** Minus (Apache-2.0) and June (GPL-3.0) cloned and searched; PixelPlayer's licence is proprietary, so nothing is copied from it; the developer.android.com guide pages and the m3.material.io blog could not be read (blocked / need JavaScript), so those parts are still open and need pasted text from the owner. Then use the whole component set, not one button type: button groups (standard and connected), toggle and split buttons, extended and menu FABs, floating toolbars, flexible app bars, wavy progress, loading indicator with shape morphing and haptics, carousels, expressive navigation rail and bar, chips, sliders, switches with icons, `MaterialShapes` everywhere, the expressive motion scheme and springs on every screen including loading and empty states | NOT STARTED |
| 19.4 | **Batch 3, layout, shape and typography overhaul of every screen** (**started 2026-10-28, CI green `d56d315`, not device-tested:** notes list as hue-tinted cards with a clean preview and chips, study material as pastel rows with an Open pill and type filter chips, folder Study and Syllabus tabs, the Syllabus tree as one pastel card per unit with round status marks and guide lines, shape-morphing empty states; **still to do:** every other screen) from images 03 to 15 (screen mapping in `docs/ui overhaul/README.md`): Home, Today, Attendance, Timetable, Calendar, Vault and subject folder, note editor, reading mode, Syllabus, AI Hub, Settings, Appearance, Trash, Search and Add entry popups, onboarding. Hue-tinted cards, round icon chips, large light titles, capsule chips, floating bottom controls, generous spacing, one accent per screen | IN PROGRESS. Done (CI green): Trash (hue cards, icon square, "days left" chip that turns red at 3 or fewer, Restore and Delete for good), Subjects (hue card, coloured letter square, Active / Mute chips, 3-dot menu with Rename and Delete), Home, Today lecture cards, folder tabs, notes, study material, syllabus. **Left:** see the ledger |
| 19.5 | Keep the 3-button pill exactly as it is: after every batch compare it with image 01 and the previous build | ongoing |
| 19.6 | Docs: update `docs/ui overhaul/README.md`, MASTER_SPEC section 2, CURRENT.md and the changelog after each batch | ongoing |
| 19.7 | **Keyboard gap bug (owner 2026-10-28, every text field low on the screen).** Cause found by reading the code: `MainActivity` had no `windowSoftInputMode`, so Android chose "pan" for a window it could not tell scrolls, and moved the whole window up on top of the popup's own keyboard padding. Fix: `android:windowSoftInputMode="adjustResize"` on the activity (the app is edge-to-edge, so the insets reach Compose and the popup's `imePadding` is the only thing that moves it) | DONE in code, CI green `79fbc9f`; needs your device check on the Add entry, Add exam and AI options popups |
| 19.8 | **Stacked styles rewritten (owner 2026-10-28).** Causes: the live editor hides the markers, so a selection of the word "bar" in `**bar**` reaches the code starting in front of the hidden `**`; the old code then wrapped again (`****bar****`), and toggling bold off under another style added a second pair inside the first. Line prefixes (list, check, number, letter, quote, heading) each stripped only some of the others and went in front of the alignment tag, so they piled up. New engine `InlineStyle` (+ `MarkdownEdit`): the selection is fitted to the markers (steps in over markers at its edges, reaches out to hold the other half of a pair); a style is removed from the pair that really encloses the selection, splitting the pair when only part is selected; the same style inside the selection is merged instead of nested; colour and size are replaced through other styles; code drops the markers inside it and cannot be styled; `***` keeps the other half. One block prefix per line after the alignment tag; a line that already holds a stack is cleaned up when a new prefix is chosen. The parser reads `*a **b***` and `**a *b***` (nested bold and italic) the same way as the editor. 17 new JVM tests | DONE in code, CI green `79fbc9f` (tests run by CI); needs your device check with the example string |
| 19.9 | **Floating bottom controls (owner 2026-10-28, "like pic 2": AI button, pill and FAB).** The AI Hub button is a squircle (rounded square) beside the pill instead of a circle. The subject folder has its tabs as a floating glass pill at the bottom (`TabPill(floating = true)`) with an extended floating button above it on the right ("New entry" / "Add material" / "Edit syllabus" by tab, hidden while notes are selected); the in-list "New entry" bar, tab bar and "Add study material" button are gone. Subjects: "Add subject" floating button opening a small dialog (the inline field card is gone). Syllabus: "Add unit" and "Import outline" floating buttons. Floating controls live in the screen's `overlay` (outside the glass layer they sample) | DONE, CI green `79fbc9f`, not device-tested. **Left:** the empty-state "Add subjects" buttons on Home and Vault, Test Lab, Storage |
| 19.10 | **Topic filter as a menu on the subject name (owner 2026-10-28).** The "Topic: All" chip is gone; tapping the subject name in the folder's title bar opens a menu with "All topics", the whole syllabus tree indented by level (the chosen topic in the accent colour with a tick), and "Open the syllabus". An active filter shows under the name and as a removable chip above the list. The old "Show notes under" dialog is no longer used from the folder (still used to file a note or guide under a topic) | DONE, CI green `79fbc9f`, not device-tested |
| 19.11 | **Folder bottom controls like the main pill (owner 2026-10-29, pic 5).** The folder now uses the main `BottomPill` itself (lens, bead, label) instead of a copy; it shrinks to icons and the button slides away while the list scrolls down and returns on scrolling up (`PillScroll` / `rememberPillScroll`, the same distances as the main screen). The list now runs to the very bottom under the navigation bar (`ScreenScaffold(edgeToEdgeBottom = true)`), which removes the solid band under the cards | DONE, CI green `91daba9`, not device-tested |
| 19.12 | **Home (owner 2026-10-29, pics 1 to 3).** "Watch these subjects" is no longer an open section: a small arrow with no text in the bottom right corner of the bunk buffer card opens a joined card under it in the same theme colour (hidden by default, the two cards' meeting corners squared and animated). The AI Hub button beside the pill is gone; on Home a pinned squircle **Subjects** button sits there instead (the AI Hub is the third Home quick action when AI Features are on, and stays in the profile sheet). The Home **Attendance** quick action opens a popup with the Attendance card (percentage, Present / Absent today, "Did you collect today's notes?" with the per-subject list, "Open the calendar"); the bunk buffer card still opens the Attendance tab | DONE, CI green `91daba9`, not device-tested |
| 19.13 | **Bugs from screenshots 2026-10-29.** Import an outline dialog (pic 4): the three actions were squeezed into one row and "Use as typed" wrapped letter by letter; "File" and "Tidy with AI" are now buttons under the text field and the dialog keeps one short action. Calendar: the chosen day and a holiday had the same colour; a holiday keeps a stronger warm tone even when chosen and the chosen day gets a ring in the theme's main colour. Note previews (pic 5) no longer show leftover markers or stacked prefixes of notes broken before 19.8 (`{size:12|`, `**`, `a) > a)`) | DONE, CI green `91daba9`, not device-tested |
| 19.14 | Shared pieces added: `AppCard(shape = ...)` for joined cards, `ExpandChevron(tint = ...)`, `AttendancePopup`, `PillScroll`, `ScreenScaffold(titleContent, edgeToEdgeBottom)` | DONE, CI green `91daba9` |
