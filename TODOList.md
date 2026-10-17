# Absencium TODO list

Single source of truth for what is done, in progress, pending and planned. Updated after each set of features, not after every edit.

**Status words.** DONE means it compiled on CI (and is named with the commit). WAITING FOR CI means it is written and pushed but CI has not confirmed it. Nothing is marked DONE that has not compiled. Nothing here has been tested on a device.

**Rules this list follows**
- Bug fixes come before new features.
- A specification that was replaced is ~~struck through~~ and the new one follows it immediately.
- Steps are in implementation order; each heading shows how many tasks the step holds.
- Development, CI, testing and reports happen in `skywa1ker404/Absencium-Dev`. `skywa1ker404/Absencium` stays empty until a stable 1.0.0 is recommended, tested and explicitly approved by Anakin.

Last updated: 2026-10-17. Stable-repo secrets and variable: Anakin reports they are set in `Absencium` settings (not verifiable from here; no values were read).

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
| 1b.7 | Stable export dry run: file set builds, link check passes except `LICENSE` (expected) | DONE for the file set; stable `README.md` still to write (Step 7) |
| 1b.8 | Decisions needed from Anakin: authorship wording, logo ownership, keep or drop the captions switch, old keys | OPEN |

## Step 1c. Resolved audit decisions (7 tasks)

| # | Task | Status |
| - | --- | --- |
| 1c.1 | YouTube option (b): unofficial caption fetch and its setting removed; Gemini link reading kept; paste-the-transcript box for other providers; tile and screen renamed "YouTube Video Notes" | WAITING FOR CI |
| 1c.2 | Authorship sentence added to the README FAQ and licence section (not a substitute for the GPL text) | written |
| 1c.3 | `absencium-upgrade.jks` removed from the tree, `.gitignore` blocks keystores, passwords out of the docs, migration plan for friends' APKs in `docs/SIGNING.md`; history not rewritten | WAITING FOR CI |
| 1c.4 | Stable CI guard: commits must be authored by skywa1ker404 (staged in `release/stable`, not published) | written, runs only after publishing |
| 1c.5 | Logo provenance recorded (GPT, by the owner's account); OpenAI terms read; original replacement proposed, **not applied** | OPEN: needs Anakin's choice |
| 1c.6 | Contributor-licence sentence explained; `CONTRIBUTION.md` **not changed** | OPEN: needs Anakin's wording |
| 1c.7 | `LICENSE_AUDIT.md` and notices updated; source scanner could not run (no PyPI), so verification is incomplete | DONE (documents) |

## Step 2. Timetable overhaul (11 tasks) — pending, needs Room v2 migration

New `teacher` column: bump the database to version 2 with a real `Migration` and an exported schema. Never destructive.

1. Teacher (optional) field when adding or editing a lecture.
2. ~~Start time as a text field~~ Start time is a button that opens a frosted wheel picker (HH : MM, AM/PM for 12 h, 1–24 for 24 h, infinite scroll, minutes 0–59). Required.
3. ~~End time~~ Lecture duration is an optional button, shown only after a start time is chosen. Wheel of 0–12 h and 0–59 min, default 0:00, no AM/PM. Both buttons re-open to edit.
4. Subject chosen from a dropdown, ~~horizontal chips~~.
5. "Add a lecture on Monday" becomes a button that opens a popup with a day button (phone's first day by default) and a dropdown (Mon–Fri or Mon–Sun by the app setting).
6. Edit (pen) and delete icons on entries, ~~"Edit" / "Remove" text~~.
7. Delete asks for confirmation, then a toast "Entry deleted" with Undo for about 4–5 s.
8. Swipe an entry left to delete.
9. "Import Lecture Timetable" with the same popup style as the other two imports.
10. After AI scanning: tick options for room number, teacher name, and time gaps between classes as duration (yes/no).
11. Docs and changelog for the above.

## Step 3. Navigation and search (3 tasks) — pending

1. "Next up" opens the lecture details; the next lectures are an expandable dropdown there (not in the detail popup).
2. Main search popup gets a horizontally scrolling category row: ALL, NOTES, CALENDAR (AI CHAT when it exists).
3. Replace remaining custom dropdowns with `InlineDropdown`.

## Step 4. Unfinished items from earlier prompts (12 tasks) — pending

1. Text Notes editor.
2. Remaining Add Entry actions.
3. Vault and drafts overhaul.
4. Widgets.
5. Permissions onboarding.
6. Backup mode.
7. Several API keys per AI provider.
8. Navigation pill label clipping.
9. Garbled text under the glass navigation bar.
10. In-app "Open-source licences" screen (needed for Apache-2.0, MIT and OFL notices in the APK).
11. About "Buy Me a Chai" card (https://www.buymeachai.in/skywa1ker404).
12. Outdated docs refresh: `IDENTITY.md`, changelog file and in-app changelog, README, intro text.

## Step 5. Calendar tab (Roadmap phase 5) (9 tasks) — planned

1. Views: list/grid, schedule, day, 3-day, week, month.
2. Entries ordered by date; today's entry first when a page fills.
3. Optional auto-delete of past-day entries.
4. Tap for details; long-press for an action menu (edit, delete, rename).
5. Top-left tick icon in a non-pointy Material 3 Expressive polygon (more than three sides) opening the To-Do list.
6. "Today" icon showing the current date, no background.
7. Search icon, no background.
8. Three-dot menu opening a sidebar with view modes and categories in two sections, plus a settings button.
9. AI can add day-by-day entries from uploaded media.

## Step 6. Profile and attendance logs (4 tasks) — planned

1. Attendance logs menu with circular progress cards; tap for details.
2. Home's "watch these subjects", the X % present card (Present / Absent / Total) and the bunk buffer card all open it.
3. A two-leaf-clover button left of the profile icon opens the log as a popup.
4. Dragging the popup up about 85 % makes it full screen.

## Step 7. Stable 1.0.0 preparation (9 tasks) — pending, nothing published

1. Verify the Dev CI is green (both jobs, APK report).
2. Signing audit write-up and the exact manual steps for the new key and the four secrets plus the `ABSENCIUM_CERT_SHA256` variable.
3. Dev README (status, testing, APK downloads, risks, contribution) and stable README (verified links and badges only).
4. `release-notes/v1.0.0.md`.
5. Run `scripts/export_stable.sh` into a temporary folder and check every link.
6. ~~Remove the old public keys from the tree (needs approval).~~ Done in Step 1c (approved); Git history stays as it is unless Anakin approves a rewrite separately.
7. Decide the remaining licence items: the logo and the contributor sentence (Step 1c). Icons, hero/showcase, badge, Monokai Pro, captions, authorship wording and old keys are handled in Steps 1b and 1c.
8. Add the GPL-3.0 `LICENSE` (**needs approval**).
9. On approval only: publish to `Absencium`, run the stable workflow, publish the verified 1.0.0 release, update `data/projects.json` in `skywa1ker404.github.io` (text only).

---

## Known bugs
- ~~Dev debug build failed on `0b1ab56` because of the guard (task 0.6).~~ Fixed; CI green on `6834a09`. A first compile error in the month fade (`using` outside its scope) was fixed the same way.
- No unit tests exist; `testDebugUnitTest` runs with nothing to test.

## Open decisions for Anakin
Old signing keys in the repository, the licence items in `docs/LICENSE_AUDIT.md`, and the stable release approval.
