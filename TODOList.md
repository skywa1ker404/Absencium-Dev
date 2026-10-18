# Absencium TODO list

Single source of truth for what is done, in progress, pending and planned. **Every specification given so far is merged here** (the master specification, the earlier roadmap, the improvements and features list of 2026-10-18, the Sharing and Sharewave specification, the sync-mode idea and the repository audit request). New requirements are added to the step they belong to, not as a new document; a requirement that changes is struck through and its replacement follows it. `docs/ROADMAP.md` explains *why* the steps are in this order; this file says *what* and *how far along*. Updated after each set of features, not after every edit.

**Status words.** DONE means it compiled on CI (and is named with the commit). WAITING FOR CI means it is written and pushed but CI has not confirmed it. Nothing is marked DONE that has not compiled. Nothing here has been tested on a device.

**Rules this list follows**
- Bug fixes come before new features.
- A specification that was replaced is ~~struck through~~ and the new one follows it immediately.
- Steps are in implementation order; each heading shows how many tasks the step holds.
- Development, CI, testing and reports happen in `skywa1ker404/Absencium-Dev`. `skywa1ker404/Absencium` stays empty until a stable 1.0.0 is recommended, tested and explicitly approved by Anakin.

Last updated: 2026-10-18 (unified plan). Stable-repo secrets and variable: Anakin reports they are set in `Absencium` settings (not verifiable from here; no values were read).

---

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
| 1f.1 | Settings, Appearance: "Glossy glass" switch, shown only while Liquid Glass is on, **off by default**; adds a shine band and a glint to glass cards and buttons (not to the pill or floating bars) | WAITING FOR CI |

## Step 2. Open bugs and checks on what already compiled (7 tasks)

Bugs come before features. Everything marked DONE above compiled on CI but has not been tried on a phone.

| # | Task | Status |
| - | --- | --- |
| 2.1 | Device check of the CI-green changes: pill drag to the last tab (1e.2), licences screen and scrollbar drag, API style chips, No colour preset, glossy glass | pending |
| 2.2 | **Bug:** Present / Absent Today buttons: animations misbehave (needs your screen recording or exact steps before I change it) | pending |
| 2.3 | ~~Navigation pill label clipping~~ the owner prefers the current pill look (1e.1); re-open only if the clipping is seen again after the drag fix | pending |
| 2.4 | ~~Garbled text under the glass navigation bar~~ withdrawn the same way; the title-area and veil changes of 1d.1 / 1d.3 were reverted | superseded |
| 2.5 | Lag with a lot of text or many entries on screens other than the licences screen, subject folders and search (for example typing in a very long note): measure on the device first, then fix the measured cause | pending |
| 2.6 | Prevent stale saves overwriting newer edits, make the save-status indicator accurate, predictable draft creation and deletion (data-loss class, from the reliability list) | pending |
| 2.7 | Rapid-tap guard: prevent duplicate actions from double taps on save, delete, mark present/absent | pending |

**Tests:** Manual device script; a regression test for each fixed bug where logic is involved. · **Effort:** S to M

## Step 3. Audit of the candidate repositories (audit only, no code) (4 tasks)

Nothing is copied or added until this report is approved. It decides which proven designs to follow and which licences rule code reuse out.

| # | Task | Status |
| - | --- | --- |
| 3.1 | Inspect Absencium-Dev first (source, this file, `docs/MASTER_SPEC.md`, `docs/ROADMAP.md`, `docs/SPECIFICATION.md`) and list existing, partial, planned and missing features | pending |
| 3.2 | Audit 16 repositories: ImageToolbox, June, Kori, Reef, Tomato, FaceDown, Self-Attendance, baca., LastChat, Damier, bubble-notice-android, WikiReader, yearly-progress, Keshu, Etar Calendar, Agora. For each: README, source, actual LICENSE files (check version differences), dependencies, assets, the inspected commit, unresolved uncertainties. Special licence checks: Yearly Progress AGPL/GPL discrepancy, Damier Commons Clause, Reef additional terms, Kori licence, FaceDown and Self-Attendance reuse permission. No code with unclear permission is copied; AGPL code is not assumed to become GPL-only | pending |
| 3.3 | Comparison matrix (repository, verified licence, feature, existing equivalent, benefit, difficulty, risks, recommendation) and a recommended / deferred list per area (Vault, AI Hub, Attendance, Reminders, Tasks, Calendar, focus tools, widgets) | pending |
| 3.4 | Save the report as `docs/FEATURE_AUDIT.md`, update the steps below only where it changes them, and wait for approval | pending |

**Tests:** Every claim cites a file or commit; anything only read from a README is labelled as such. · **Effort:** L (research only) · **Gate:** Anakin approves the report

## Step 4. Data foundation: one Room migration and one export format (9 tasks)

Almost every later step (undo, backup, sync, sharing, widgets) needs stable ids, change times, tombstones and a versioned export. Doing it once now means the later steps add code instead of rewriting it.

| # | Task | Status |
| - | --- | --- |
| 4.1 | Room version 2 as a real `Migration` with an exported schema (never destructive): stable `uuid` per syncable row, `updatedAt` revision, `deletedAt` tombstone, plus the `teacher` column the timetable needs. Later versions add columns the same way | pending |
| 4.2 | Trash and undo: deleted rows are tombstoned, shown in a Trash screen, restorable, purged after a set time; the undo toast of the timetable work uses it | pending |
| 4.3 | Versioned export format: manifest (format version, app version, schema version, created, counts, SHA-256 per file), the database, attachments, settings without secrets; import validates manifest and hashes first | pending |
| 4.4 | Backup and restore on top of it: attachments included, restore validation, duplicate detection, interrupted-import recovery, low-storage check, backup status shown in Settings | pending |
| 4.5 | Optional password encryption of backups (AES-256-GCM, key derived with a reviewed library, no hand-written crypto) with a threat model written in `docs/PRIVACY.md` | pending |
| 4.6 | Rolling local snapshots (3) built on the same export code | pending |
| 4.7 | Seams for later steps, written as interfaces with one local implementation: a `Transport` (sends or receives an export or a change set), a change-set builder fed by `updatedAt`/tombstones, and a scoped read-only repository view. No server, no networking code yet | pending |
| 4.8 | Test infrastructure: Room migration tests, export/import round-trip tests, preference-recovery test; CI runs them | pending |
| 4.9 | Safe preference recovery (corrupt DataStore falls back to defaults) and preserve data when subjects are renamed or moved | pending |

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
| 8.5 | Eisenhower task view and Deadline Radar (confirm release scope first) | needs decision |
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

## Step 10. Vault, editor and file handling (7 tasks)

The editor and file tools come before study tools, widgets and sharing because those all read notes and attachments.

| # | Task | Status |
| - | --- | --- |
| 10.1 | Text Notes editor with a floating Markdown toolbar, templates, tags with autocomplete, related-note links, reliable autosave with an accurate indicator | pending |
| 10.2 | Vault as a file explorer (breadcrumbs, badges, long-press multi-select, batch actions), drafts folded in; duplicate filenames, deleted subjects, empty folders, large images, inaccessible files and low storage handled | pending |
| 10.3 | Local OCR (Tesseract with downloadable language packs, separate from AI), PDF text extraction, EXIF scrubbing on export; blurry and rotated image handling, unsupported-language message, editable OCR preview | pending |
| 10.4 | "Extract Text" popup (With AI or Locally); finish the Add Entry actions (Document or file, Audio note, Solve homework, Generate AI notes); link missed classes to notes and attachments | pending |
| 10.5 | Timetable OCR into the editable import preview of Step 6 | pending |
| 10.6 | Note export and share: PDF, Markdown, ZIP, supported document formats, through Android's Sharesheet with a `FileProvider`. Written as a small reusable `ShareService` so Step 17 extends it instead of replacing it. Consider PDF viewing and basic annotation afterwards | pending |
| 10.7 | Audio memos with timestamps (from the master specification) | pending |

**Tests:** Large-image and low-storage runs, a note with 50 k characters, share to two apps and to a missing app. · **Effort:** XL

## Step 11. AI Hub and AI settings (5 tasks)

Builds on the existing providers, model picker and key vault; adds to them rather than replacing them.

| # | Task | Status |
| - | --- | --- |
| 11.1 | Several API keys per provider with a setup guide dropdown | pending |
| 11.2 | Show Gemini as recommended (can read YouTube links) and OpenRouter as recommended for its free/paid listing; look up and verify free models of the other providers before showing a recommended all-rounder for each; if none can be verified use the provider's current Flash-Lite model, name taken from the fetched model list, never typed from memory; other providers under an "Other models" dropdown; guide for getting the chosen provider's key | pending |
| 11.3 | Clear explanation of what data is sent to an AI provider; AI failures never block local features; AI stays optional and off by default | pending |
| 11.4 | Ask AI inside notes with query history and jump-to-source (from the master specification); voice prompt on long-press of the AI icon; optional AI Memory (off by default) | pending |
| 11.5 | Design reference from the repository audit (Agora, LastChat) for conversation management, context handling and tool calling, decided after the audit | pending |

**Tests:** Provider list fetched offline and online; a failing key leaves notes untouched. · **Effort:** L

## Step 12. Study and focus tools (5 tasks)

| # | Task | Status |
| - | --- | --- |
| 12.1 | Study timer that survives backgrounding, screen lock and process death, one notification, no duplicates | pending |
| 12.2 | Daily Recall and spaced repetition (7-day loop, sticky lock, repeat badge) as specified; flashcards; Emergency Cram Mode before exams | pending |
| 12.3 | Semester-linked exam preparation and syllabus tracking; optional AI-generated study material | pending |
| 12.4 | Retrospective study category log and Momentum tracker (optional, off by default) | pending |
| 12.5 | Gradebook and GPA, and optional GPS attendance assistance (marks present after 5 minutes in a classroom area): **scope to be confirmed** before any work; the GPS part needs location permission, so it is opt-in per subject and never background by default | needs decision |

**Tests:** Timer survives a force-stop; recall questions respect the lock; spaced dates correct across time-zone change. · **Effort:** L

## Step 13. Widgets, shortcuts and quick actions (3 tasks)

Needs attendance, tasks and the timer to exist.

| # | Task | Status |
| - | --- | --- |
| 13.1 | Home-screen widgets: 1x1 quick capture, then 1x2 up to 4x4 (attendance, next up, deadlines); theme-aware, resizable, refresh and stale-data handling. The widget framework (Jetpack Glance or RemoteViews) is chosen after weighing APK size | pending |
| 13.2 | Launcher shortcuts, Quick Settings tile, Quick Add, notification actions | pending |
| 13.3 | Customizable, smart dashboard summaries and start-screen choice | pending |

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

Proposed: 1.0.0 ships after Steps 2 to 10 and 14 to 16; the AI Hub, study tools and widgets (Steps 11 to 13) follow as 1.x. **Decision needed.** Nothing is published without your explicit approval.

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

## Step 17. Sharing and Sharewave (very last for now) (6 tasks)

Needs the export format, scoped repository view and Transport seam of Step 4 and the `ShareService` of Step 10, so it adds code rather than rewriting. Android Sharesheet first, then Sharewave. Order of the two final steps can be swapped on request.

| # | Task | Status |
| - | --- | --- |
| 17.1 | Audit the repository, current sharing code and permissions, then report the plan, risks, compatibility limits and required permissions before significant changes | pending |
| 17.2 | Android Sharesheet: `ACTION_SEND` / `ACTION_SEND_MULTIPLE` for notes, text, images, PDFs, documents, selected files, exported timetables and folder archives; correct MIME types; `FileProvider` content URIs; URI permissions, temporary files, missing apps, unsupported formats and errors handled; native Sharesheet stays the universal fallback; no assumption that other apps expose chats or contacts | pending |
| 17.3 | Sharewave research and prototype on real devices: discovery (BLE or local network), pairing (QR and manual fallback), transfer over Wi-Fi Direct, local Wi-Fi or hotspot; Google Nearby Connections only as an optional transport; document Android-version permissions (location only if a mechanism truly needs it) and manufacturer limits before choosing | pending |
| 17.4 | Quick Send: send entries, notes, images, files and folders with structure and metadata; recipient must accept; progress, cancel, retry, recovery; authenticated pairing and encrypted transfer; filename, size, type and path validation; never overwrite silently, never execute received files | pending |
| 17.5 | Shared Vault Access: owner exposes only selected folders or entries for a limited time; recipient browses, previews, caches, downloads or imports with conflict handling; active-session list with devices, paths, permissions, status and expiry; manual revoke and automatic expiry (copies already taken stay); live access needs the owner's device | pending |
| 17.6 | Security, compatibility, accessibility and failure-path tests (interrupted transfer, low storage, denied permission, corrupted data, expired or revoked session); update specs, roadmap, README and user docs | pending |

**Tests:** Two or more real phones from different makers; transfer with Wi-Fi off and airplane mode on; tampered and oversized files rejected. · **Effort:** XL · **Gate:** Anakin approves the plan and the transport choice

## Step 18. Sync mode with a home server (very last, only when the app is stable) (5 tasks)

Optional and off by default; costs nothing when off. Not started without your approval. Route and difficulty are in `docs/ROADMAP.md`.

| # | Task | Status |
| - | --- | --- |
| 18.1 | Decide the route with the owner: A) encrypted snapshots to a WebDAV or Nextcloud folder, B) own small server in Docker with a change-feed API, C) both, A first | needs decision |
| 18.2 | Route A: scheduled or manual upload and download of encrypted snapshots using the Step 4 export and the `Transport` seam; restore-with-merge screen; Wi-Fi only, off by default | pending |
| 18.3 | Route B: server (Docker image, SQLite or Postgres, token auth, TLS through a reverse proxy or a private network), change feed from `updatedAt`/tombstones, per-row conflict screen, explicit sync; no real-time claims; server security tests | pending |
| 18.4 | Optional Google Drive `drive.appdata` transport (needs an OAuth client and the final signing certificate, which only the owner can create) | needs decision |
| 18.5 | Greyed-out AI sync and backup items while a sync mode is active; disconnect and delete-remote controls; docs for self-hosting | pending |

**Tests:** Two devices converging after offline edits; server down; wrong token; clock skew. · **Effort:** XL · **Gate:** Anakin approves

---

## Cleanup proposal (nothing is deleted without your approval)

The repository should not grow documents for every list you send. After you approve this file as the single plan:

| File | Proposal |
| --- | --- |
| `docs/SPECIFICATION.md` | still the master specification copy; keep until its content is folded into the steps above, then remove |
| `docs/MASTER_SPEC.md` | the 711-line original text; keep as the reference for the database, sync and Drive details until Steps 4 and 18 are built, then archive |
| `docs/ROADMAP.md` | kept, short: explains the order and the technical routes |
| `docs/CHNAGELOG.md` | contains only "working on it" and its name is misspelt; replace by `CHANGELOG.md` when the changelog is refreshed |
| `docs/LICENSE_AUDIT.md` | keep until the audit is cleared, then fold into `THIRD_PARTY_LICENSES.md` |
| `docs/IDENTITY.md`, `docs/SIGNING.md`, `docs/CONTRIBUTION.md`, `docs/THIRD_PARTY_LICENSES.md`, `docs/DEPENDENCY_LICENSES.md`, `docs/licenses/`, `docs/art/` | keep |

## Known bugs
- No unit tests exist yet; `testDebugUnitTest` runs with nothing to test (fixed by the test infrastructure of Step 4).
- Everything is compile-verified only; device behaviour is unknown until you test.

## Open decisions for Anakin
1. Whether 1.0.0 is Steps 2 to 10 and 14 to 16 (proposed) or includes Steps 11 to 13.
2. Scope of gradebook / GPA, GPS attendance, Eisenhower view and Deadline Radar (Steps 8 and 12).
3. Route for sync mode (Step 18) and whether Sharing (Step 17) comes before it.
4. Cleanup proposal above.
5. Approval of the repository audit report before any code or dependency from it is used.
6. Minimum Android version: recommendation is to keep 26 (see `docs/ROADMAP.md`).
