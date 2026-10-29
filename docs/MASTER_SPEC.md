# Absencium master specification (current)

Last rewritten 2026-10-27. This is what the app is meant to be **now**. The plan and status of every item is in [TODOList.md](../TODOList.md); the design reference for the UI is in [ui overhaul/](ui%20overhaul/README.md). The original text of 2026-10-16, which still contained cancelled plans, is kept unchanged for history in [archive/MASTER_SPEC_2026-10-16_original.md](archive/MASTER_SPEC_2026-10-16_original.md). **Do not build from the archive.**

Standing rules: preserve working features; never invent APIs; never claim something works unless CI compiled it (and a device test, where one is needed); never silently discard user data; GPL-3.0 compatible everything; no ads, telemetry or mandatory accounts or network.

---

## 1. Platform and architecture
- Native Android, Kotlin, Jetpack Compose, **Material 3 Expressive**, MVVM with repositories, StateFlow and Coroutines/Flow. Room (`Absencium.db`), DataStore, app-private files. WorkManager only for durable deferrable work. minSdk 26.
- Distribution: GitHub repository and GitHub Releases, plus the owner's website. No Google Play, no F-Droid. Builds only on GitHub Actions; the optimised (R8) APK is the only artefact.
- Offline-first: every core feature works with no network. No Google Play Services in the core.
- Dev repository `skywa1ker404/Absencium-Dev` (package `skywa1ker404.absencium.dev`). The stable repository stays untouched until the owner approves 1.0.0.

## 2. Design language (revised 2026-10-27)
- **Full UI overhaul** from the reference images in `docs/ui overhaul/`. Everything may change **except the 3-button navigation pill** (its shape, light-bending lens and selected bead).
- Themes: Default, Monet, Presets, Liquid Glass. Monet and Liquid Glass are greyed out below Android 12.
- Liquid Glass follows the Kyant0 Backdrop reference (clear blur, lens refraction, a flat veil of the theme colour, one thin edge, soft shadow, **no glow or haze**). Performance mode keeps plain surfaces.
- Material 3 Expressive everywhere: the expressive motion scheme, springy and bouncy motion on every screen including loading and empty states, loaders that morph between M3 shapes with haptics, shape morphing on selection, the expressive component set (button groups, toggle and split buttons, floating toolbars, FAB menu, wavy progress, flexible app bars, carousels, etc.).
- Floating controls follow the main screen: a screen with tabs or a main action keeps them at the bottom as glass over the page, never as bars inside the list. The tabs are the main navigation pill itself (`BottomPill`: it shrinks to icons and the button slides away while the list scrolls down), the main action is an extended floating button above it on the right, the Home screen has a pinned squircle Subjects button beside the main pill (the AI Hub is a Home quick action and is in the profile sheet), and add / import actions are floating buttons (not inline buttons). They are placed in the screen's overlay layer so they can sample the page behind them.
- Menus (3-dot, selection, topic) and dialogs use the app's own themed components; a subject's name in a folder is a menu button for its syllabus topics.
- The activity resizes for the keyboard (`adjustResize`, edge-to-edge); popups move with the keyboard through `imePadding` only.
- Display: dynamic refresh rate, capped at 90 Hz. Reduced-motion and haptics settings respected.

## 3. Storage (one mode: local)
- All data stays on the phone: Room, DataStore and app-private storage. No account, login or cloud authorisation.
- Optional custom notes folder through the Storage Access Framework, with the choice of an `.Absencium` subfolder or the folder itself, an explanation first, persisted permission, safe handling of revoked permission. Sensitive databases, keys and tokens stay in private storage.
- **Export and restore:** versioned `.zip` with manifest, integrity hashes and schema version; choose the categories; optional password (PBKDF2-SHA256 and AES-256-GCM from the Android platform); import validates first and never overwrites without confirmation; restore offered in onboarding and Settings. API keys and secrets are never exported.
- Data foundation: stable `uuid`, `updatedAt`, `deletedAt` tombstones, a Trash with 30-day retention and undo, additive non-destructive Room migrations with exported schemas.

## 4. Privacy and security
- Local only by default; no telemetry, no ads. Optional app lock with BiometricPrompt (protects local access only). Optional AI is off until the user sets it up and switches it on; the user's own API key is stored encrypted with the Android Keystore and never exported or logged.
- Logs never contain note text, passwords, keys or tokens. Redacted diagnostics export. A privacy dashboard in Settings.
- Documentation (README, `docs/PRIVACY.md`) says plainly what stays local, what an optional AI request sends, what is excluded from backups, and how to export, restore and delete. No claim of GDPR, DPDP or CCPA compliance and no claim of end-to-end encryption.
- Deleting data is local only: clear caches, delete everything on the phone (multi-step confirmation, offer an export first), restore from a backup.

## 5. AI (optional, the user's own keys)
- The **Ask AI** switch turns every AI feature off: when it is off or no key and model are set up, AI buttons say so ("Ask AI is disabled" or "API key/model not set up") and are greyed in menus. Nothing is sent without the user choosing an AI action; the existing "AI can make mistakes" notice is shown.
- **There is no local OCR** (removed 2026-10-25: it did not work well and added about 4 MB and native libraries). Text from pictures, PDFs, Word files and recordings is read by the user's AI only. Prompts and filters are subject-neutral.
- AI Hub: history of requests and answers (local, deletable), voice prompt input, optional Memory (off by default, explicit consent, view, edit, clear). Several keys per provider and provider guides are 1.x.
- AI notes follow the typed prompt above all defaults; the syllabus path (unit, chapter, topic) is passed to AI features when a topic is chosen.

## 6. Vault, editor, reading and syllabus (1.0)
- Vault: one folder per subject with breadcrumbs, metadata badges, multi-select with a floating toolbar (move, finish, share, delete), long-press actions, drafts, Trash. Notes / Study material / Syllabus tabs inside a subject (a floating pill at the bottom, with a floating button for the tab's main action), and a topic filter that opens from the subject's name.
- Study material: previous year papers, answer books, guides and syllabus documents, each filed under a syllabus topic.
- Syllabus tree per subject (unit, chapter, topic, subtopic) with status, progress, import of an outline from text or a file (AI), linking of notes and material.
- Note editor: live Markdown with hidden markers, toolbars as M3 Expressive button groups, stacked styles (the innermost colour or size wins; applying a style again removes it from the pair that encloses the selection, splitting the pair for a partial selection, and never nests a second pair; colour and size replace each other through other styles; one block prefix per line, after the alignment tag, so list kinds, quote and heading replace each other), tables, checklists, images, doodles, recordings, AI panel with actions, languages for translation.
- Reading mode with selection pill (Explain, Translate, Meaning, Ask), themes, bookmarks, a mini player that can be minimised to a bubble.
- Share note as PDF, Markdown or ZIP (EXIF-clean), chosen attachments, never uploaded automatically. Sharing between phones (Sharewave) is after 1.0.

## 7. Attendance, timetable, calendar, reminders (1.0)
- Attendance with present, absent, cancelled, extra and bunked classes, holidays and cancelled classes excluded, safe-absence projections and bunk buffer per subject.
- Timetable with lectures, rooms and teachers; calendar with exams and holidays; reminders and tasks with snooze and quiet mode; universal search; undo for destructive actions; drafts saved automatically with a "Saved" indicator.
- Quality of life: scroll position and filters remembered, useful empty states, haptics with a setting, date format, first weekday and 12/24 h choices, "Reset app preferences" that keeps data, predictive back, accessibility (font scaling, TalkBack, contrast), a first-launch tour that can be skipped.

## 8. About and support
- About has a "Buy Me a Chai" card with `https://www.buymeachai.in/skywa1ker404`. It is the only support link. Report-a-bug and request-a-feature open a prefilled GitHub issue.

---

## Not in 1.0 (kept on the 1.x list, not cancelled)
AI Hub extras (several keys, provider guides, Memory), study timer and daily recall, flashcards, Emergency Cram, exam preparation, Gradebook and GPA, Eisenhower view and Deadline Radar, retrospective study log, Momentum, home-screen widgets, shortcuts, quick-settings tile, customisable dashboard. Sharing and Sharewave (Step 17) come after the stable release.

## Cancelled (do not build, do not mention as planned)
| Cancelled | When and why |
| --- | --- |
| Backup Mode (Google Drive snapshots, `drive.appdata`, Wi-Fi-only rules, cloud retention) | Removed 2026-10-22 with all sync and cloud code; the owner asked for it to go (TODO Step 18). Manual local backup and restore stay |
| Pure Cloud / Sync Mode (experimental), pending-change queue, conflict screen, cloud large-file routing | Same decision |
| Device pairing, QR and eight-digit codes, session management, primary-device inactivity rule | Only made sense with sync |
| Accounts, email and password, TOTP, recovery email, account deletion, Google sign-in | Only made sense with a cloud; local mode never shows a login |
| Cloud-bound encryption design and recovery-key rules | No cloud; local backups keep the optional password |
| Free-tier cloud backend (Cloudflare Workers + D1, object storage), backend security, billing rules | No backend |
| Rolling local snapshots (3) and the daily interval backup | Removed 2026-10-22 on the owner's request (TODO 4.6) |
| Local OCR with Tesseract, language-pack downloads, "Locally (Without AI)" extraction | Removed 2026-10-25 |
| GPS attendance assistance | Excluded; manual attendance is enough and it avoids location permission |
| F-Droid distribution | Dropped in the original text |
| YouTube caption scraping | Removed (TODO 1c.1); Gemini link reading and pasted transcripts stay |
| "Glossy glass" option | Removed (TODO 10.20) |
| Any support link other than Buy Me a Chai | Owner decision |

## Open items that need an owner decision
These were in the original text but are not in the plan, so nothing is built for them until you say so.
1. **In-app update checker** (GitHub Releases API, signature check, package installer). It needs internet, so it would be an opt-in setting. Suggested: 1.x, after the stable release.
2. **Operations card** in Profile for long-running work. It existed for cloud transfers; today long jobs (export, import, AI extraction) show their own progress. Suggested: drop it, or later add a small card for local jobs.
3. **Power and thermal handling** for expensive background work (TODO 5.9 covers reduced motion, haptics and refresh rate; deferring work on battery saver is not scheduled).
4. **Focus mode** and **countdown** improvements are in Step 12 (1.x).

## Test expectations (kept from the original)
Fresh install and onboarding without internet; export and restore with and without a password; wrong password, corrupted archive and unsupported version; missing attachments and low storage; process death during export or restore; no API key or secret in backups or logs; rapid taps; midnight, month and leap-day boundaries; upgrade with data. Tests are reported only if they ran; CI compiles and runs the JVM tests, and nothing is device-tested until the owner tests it.

## Addendum 2026-10-29 (owner requests, see TODOList 19.15 to 19.21)
- Extended action buttons fold to their icon while the list scrolls down; the pill shrinks only when the list really scrolls.
- One topic menu is used wherever a note or material is filed or filtered; the editor shows the note's topic as a dropdown.
- A single selected note can be renamed and re-dated from the selection toolbar; study material has the same long-press selection; Notes and Study material can be shown as cards or as a compact list.
- Importing a syllabus outline reads only the chosen subject out of a longer document, lets the student choose what to import, and never adds a title that already sits under the same parent.
- Planned for the profile (built when the profile is overhauled): a syllabus card showing the percent of the whole syllabus done; it opens a menu of every subject's syllabus with import, and the AI imports only the syllabus of subjects that already exist, each into its own folder.

