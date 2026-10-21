# Absencium: plan and status

**What we are building.** Absencium is an offline-first Android study companion (Kotlin, Jetpack Compose, Material 3 Expressive, Room, minSdk 26). It tracks attendance, timetable, exams, holidays, tasks and reminders, and keeps notes and study material in a Vault of subject folders. A block-based note editor and a per-subject syllabus tree link notes, past papers and guides to topics. Optional AI (your own API keys) helps with extraction and notes. Everything works without a network; sync and sharing come after 1.0. Built only by GitHub Actions in `skywa1ker404/Absencium-Dev`; the stable `Absencium` repo stays empty until you approve 1.0.0. GPL-3.0.

**Status words.** DONE = compiled and unit-tested on CI (commit given), **not tried on a device**. WRITTEN = in the working tree, CI has not run. Nothing is marked DONE otherwise. Current work is in [`CURRENT.md`](CURRENT.md).

**Rules.** Bug fixes first. Replaced requirements are ~~struck through~~. Docs-only commits use `[skip ci]`. Dev work and CI stay in `Absencium-Dev`. No secrets read or printed; no releases, repos, force-pushes or key changes without you.

Updated: 2026-10-21.

## Done (Steps 0 to 1g, all CI green, not device-tested)
Dev/stable application IDs and signing split, CI builds artifacts only (manual dev-release workflow), APK verifier, identity guard; first bug fixes (calendar month/year popups, app date chooser, About link); licence clean-up (icons replaced by Material paths, original art, Monokai preset and unofficial YouTube captions removed, in-app licences screen, upgrade key removed from the tree); pill fixes (look kept, light-bending lens kept, last-tab drag shake, clipping); Liquid Glass tuning; lag fixes (flat glass cards, popup glass layer, lazy bounded model list, tap guard, Present/Absent ghost = ripple removed). Details live in git history.

## Plan to 1.0.0 (Steps 2 to 10 and 14 to 16, in order, bug fixes first)

### Step 2. Open bugs
- 2.1 Device check of everything CI-green so far (pill drag, licences screen, lag fixes, ghost fix): **pending, needs you**
- 2.2 Present/Absent ghost: DONE (`795afcc`)
- 2.5 Lag on other screens: pending; the frame monitor writes slow frames to debug.log as evidence
- 2.6 Stale saves and accurate save indicator: partly DONE (`795afcc`) · 2.7 Double-tap guard: partly DONE (`795afcc`)

### Step 3. Repository audit (no code)
Draft in `docs/FEATURE_AUDIT.md`; **waiting for your approval**. Repos whose licences do not fit GPL-3.0 stay "idea only".

### Step 4. Data foundation (mostly done, `795afcc`)
DONE: Room v2 migration with stable ids and tombstones (4.1), Trash and undo for notes, exams, holidays, lectures (4.2), versioned manifest export (4.3), backup and staged restore with rollback (4.4), optional password encryption, PBKDF2 + AES-256-GCM (4.5), 3 daily snapshots (4.6). Partly: Transport and scoped-view seams (4.7), test setup (4.8), preference recovery (4.9).

### Step 5. Android integration base (pending)
5.1 per-app language · 5.2 strings to resources, RTL · 5.3 one clock/time-zone provider · 5.4 insets, Predictive Back · 5.5 process-death recovery · 5.6 notification groundwork (channels, POST_NOTIFICATIONS, inexact alarms by default, reboot rescheduling) · 5.7 permission-denial pattern · 5.8 HEX palette · 5.9 reduced motion and haptics.

### Step 6. Timetable overhaul (pending)
6.1 teacher field · 6.2 start time wheel picker · 6.3 optional duration · 6.4 subject dropdown · 6.5 add-lecture popup with day button · 6.6 pen/delete icons · 6.7 delete confirm + Undo toast · 6.8 swipe to delete · 6.9 import timetable (popup style) · 6.10 AI scan options (room, teacher, gaps) · 6.11 A/B weeks, replacement classes · 6.12 docs.

### Step 7. Attendance correctness (pending)
7.1 no divide-by-zero, rounding · 7.2 present, absent, cancelled, extra, bunked · 7.3 weighted lectures · 7.4 safe-absence projections · 7.5 semesters and archive · 7.6 fill past attendance backwards · 7.7 undo and corrections · 7.8 what-if planner · 7.9 profile and log cards · 7.10 next-class reminder (after Step 8). Unit tests for every formula.

### Step 8. Reminders, tasks, Quiet Mode (pending)
8.1 tasks and reminders · 8.2 reboot/time-zone-safe scheduling · 8.3 Quiet Mode · 8.4 per-Android-version notification actions (Android 16 progress notifications where supported, with fallback; Android 17 details unverified) · 8.6 "Next up" opens lecture details · 8.5 Eisenhower view and Deadline Radar: 1.x.

### Step 9. Calendar, search, navigation (pending)
9.1 calendar views · 9.2 To-Do entry icon · 9.3 ICS import/export · 9.4 search categories · 9.5 `InlineDropdown` everywhere, empty states · 9.6 AI adds calendar entries from media.

### Step 10. Vault, editor, files (in progress)
| # | Task | Status |
| - | --- | --- |
| 10.1 | Markdown editor, templates, tags, `[[note]]` links, save indicator | first version DONE (`24af814`); layout replaced by 10.8 to 10.14, engine kept |
| 10.2 | Vault explorer: breadcrumbs, badges, multi-select, batch actions | first version DONE (`24af814`); layout replaced by 10.15; large-image and unreadable-file handling not done |
| 10.3 | Local OCR with Tesseract4Android (Apache-2.0, `cz.adaptech.tesseract4android:tesseract4android:4.9.0` via JitPack); PDF text; blurry/rotated images; downloadable language data | unblocked; to do: confirm CI resolves JitPack, check bundled Leptonica/libjpeg/libpng licences, update `DEPENDENCY_LICENSES.md` |
| 10.4 | "Extract Text" popup (AI or local); finish Add Entry actions (Document, Audio, Homework, AI notes) | pending |
| 10.5 | Timetable OCR into the Step 6 import preview | pending |
| 10.6 | Export and share: PDF, Markdown, text, ZIP via Sharesheet and `ShareService` | DONE (`24af814`); PDF has no images yet |
| 10.7 | Audio memos with timestamps | recorder and timestamp helpers pushed (`17b801c`, CI not confirmed); UI comes with 10.11 |
| 10.8 | Editor modes: no Edit/Preview buttons; tap the note to edit, ✓ to preview; opening from a folder = preview. Edit bar: back, undo, redo, ✓. Preview bar: page adjustment, ⋮ | pending |
| 10.9 | Header: date, character count, category picker ("Uncategorized" until linked, needs 10.16) | pending |
| 10.10 | Bottom toolbar: AI, Aa, align/lists, checklist, images, +. Aa tabs: Text style, Symbol, Divider, **Templates**. Bold, italic, underline, strike, highlight, size, colour. Selection popup (AI, Cut, Copy, Select all, Share). Images: Images/videos, Photo, Scan. +: Doodle, Table, Recording, Audio | pending |
| 10.11 | Block document: text, images, tables, doodles, audio; movable, resizable, aligned, text flows around them. Image menu: Large mode, Copy, Share, Save, Delete, Crop, Rotate. PDF export draws images; ZIP export strips EXIF | pending |
| 10.12 | Doodle canvas: pens, eraser, colour grid/spectrum, size, opacity, undo/redo, artboard lines; stays editable | pending |
| 10.13 | ⋮ menu: Notes info popup, Delete (to Trash), Share, Export. Page adjustment: paper style, texture, margin. Reminder, Lock, Widget only once those features exist | pending |
| 10.14 | Editor AI: prompt bar with send; AI actions (summary, polish, shorten, extend, continue, grammar, to-dos, translate); ⋮ opens AI history | pending |
| 10.15 | Subject folder redesign: title = subject name with arrowhead sidebar (All notes, Absent notes by write-next order, filter by unit/chapter/topic); list/card view in sort menu; FAB: All opens editor, Absent opens date picker then editor; search and FAB in main-menu positions; bottom pill (same style) with Notes, Syllabus, Study material; AI button right of the pill | pending |
| 10.16 | Syllabus tree per subject (unit, chapter, topic, subtopic, status, progress); AI import; AI-generate notes for a topic; main Vault FAB "Syllabus" for all subjects; needs DB v3 | pending |
| 10.17 | Study material tab: PYQ, answer book, guide | pending |
| 10.18 | Link notes, PYQs, materials to a syllabus node via "Uncategorized"; profile summary card with the Step 14 profile overhaul | pending |
| 10.19 | Vault page: "Drafts" text becomes a trash icon opening the Trash | WRITTEN, CI not run |
| 10.20 | Remove the glossy-glass option (supersedes old 1f.1) | WRITTEN, CI not run |

### Step 14. Settings, privacy, accessibility, polish (pending)
14.1 privacy dashboard, optional app lock · 14.2 report-a-bug / request-a-feature buttons · 14.3 hidden debug page · 14.4 accessibility (font scale, TalkBack, contrast) · 14.5 consistency sweep · 14.6 refresh outdated docs, changelog (`CHNAGELOG.md` is misspelt and empty).

### Step 15. Performance (pending)
15.1 measure startup and navigation on a device, fix only measured causes · 15.2 no queries during recomposition, large imports · 15.3 decide on a Baseline Profile.

### Step 16. Final testing and stable 1.0.0
16.1 regression and device test list · 16.2 CI green on both jobs · 16.3 signing key and four secrets (owner reports set) · 16.4 READMEs and release notes (drafted) · 16.5 `scripts/export_stable.sh` dry run · 16.6 licence audit (ScanCode by you) · 16.7 GPL-3.0 LICENSE (done by you) · 16.8 publish to `Absencium` **only on your approval** · 16.9 cleanup of unused files **only after you approve a file-by-file list**.

1.0 means: done, used on a real phone for a few weeks, verified signing key, release built from a tag, privacy docs matching the app, your explicit approval.

## After 1.0
- **1.x:** Step 11 AI Hub (several keys, provider guidance, ask-AI history), Step 12 study tools (timer, recall, gradebook/GPA, semester exam prep), Step 13 widgets and shortcuts. GPS attendance: excluded.
- **Step 17 Sharing and Sharewave:** Sharesheet, nearby transfer, Quick Send, Shared Vault. Extends `ShareService`, the export format and the scoped read-only view.
- **Step 18 Optional cloud (off by default, no network without consent):** sync via your own WebDAV/Nextcloud first (encrypted snapshots, offline queue, conflict screen); Google Drive backup later, only within Google's policy (needs an OAuth client and the final signing certificate from you). Own-server option deferred.
- **Step 19 Liquid Glass overhaul (future):** restyle after the reference glass kit, no glow, keeping themes, animations, quality options and Performance mode.

## Decisions
1. 1.0.0 = Steps 2 to 10 and 14 to 16; syllabus tracking moved into 1.0 (10.16 to 10.18).
2. minSdk 26. Monet and Liquid Glass (Android 12) are greyed out below with "Not supported by your Android version".
3. Support link: https://www.buymeachai.in/skywa1ker404. Everything added must be GPL-3.0 compatible.
4. Why this order: Step 4 builds stable ids, one export format and the seams (`Transport`, scoped view, `ShareService`) so sharing, sync and widgets later need no rewrite; reminders need Step 5 notifications and Step 7 attendance; the timetable needs the Step 4 teacher column; performance is measured after features exist.
5. Application ID changed from `com.absencium.app`; no upgrade path (see `docs/SIGNING.md`).

## Waiting on you
Device test of Step 2.1 (and debug.log frame lines if lag remains) · approve `docs/FEATURE_AUDIT.md` · run ScanCode · approve cleanup list · Google Drive as sync target (decided at Step 18) · authorship wording and old-key questions from the licence review.

## Known gaps
No device testing yet. PDF export has no images; EXIF scrubbing not implemented; local OCR not started.
