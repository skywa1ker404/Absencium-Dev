# Roadmap

This file explains **why the steps in [`TODOList.md`](../TODOList.md) are in the order they are** and records the technical
routes behind them. `TODOList.md` is the single list of tasks and their status; nothing is duplicated here. Nothing is claimed to
work until a CI build compiled it and, where it needs a device, you have tried it.

## What exists today (the base to extend, not rebuild)

| Area | State |
| --- | --- |
| Local database (Room v1 `Absencium.db`, schema guard in CI) | exists; no stable ids, revisions or tombstones yet |
| Backup file | partly: the database is zipped; no manifest, version, hash or encryption; staged restore plus **Restart app** exist |
| Storage folder | partly: app files or a SAF folder |
| API key storage | exists: `ApiKeyVault`, one key per provider |
| AI | exists: providers, model list and picker, photo-to-text, YouTube video notes (Gemini link reading or pasted transcript), holiday import |
| Home, Attendance, Vault, drafts, subjects, timetable, exams, holidays, search, profile, themes (21 presets and No colour), Liquid Glass, onboarding, About, Open-source licences | exist |
| Diagnostics | partly: debug logger and Test Lab |
| Tasks and reminders, notifications, widgets, undo and trash, OCR, PDF handling, sharing, sync, app lock, translations | planned (no code) |

## Order and reasons

| Step | Area | Why it sits here |
| --- | --- | --- |
| 2 | Bugs and checks on what compiled | bugs first; nothing built so far has run on a phone |
| 3 | Audit of 16 candidate repositories | audit only; decides which designs to follow before code is written, and which licences forbid copying |
| 4 | Data foundation | ids, tombstones, one export format and the test setup that undo, backup, widgets, sharing and sync all depend on |
| 5 | Android integration base | language, time zones, notifications, process death: needed by reminders, widgets and translations |
| 6 | Timetable overhaul | needs the teacher column from the Step 4 migration; attendance calculations read the timetable |
| 7 | Attendance and academic correctness | the numbers every other feature shows |
| 8 | Reminders, tasks, Quiet Mode | needs Step 5 notifications and Step 7 attendance |
| 9 | Calendar, search, navigation | needs time zones (Step 5) and the data model (Steps 4, 7) |
| 10 | Vault, editor, files, note sharing | study tools, widgets and sharing all read notes and attachments |
| 11 | AI Hub | **deferred to 1.x**; extends the existing providers |
| 12 | Study and focus tools | **deferred to 1.x** (gradebook/GPA also 1.x; GPS attendance excluded) |
| 13 | Widgets, shortcuts, quick actions | **deferred to 1.x** |
| 14 | Settings, privacy, accessibility, polish | a sweep that needs most features in place |
| 15 | Performance pass | measure after the features exist; fix only measured causes |
| 16 | Final testing and stable 1.0.0 | approved scope: Steps 2 to 10 and 14 to 16, in order; nothing is published until device tests and release checks pass and you approve |
| 17 | Sharing and Sharewave | after the stable release; needs the export format, scoped repository view and `ShareService` |
| 18 | Optional cloud: sync (WebDAV/Nextcloud) and Google Drive backup | after Step 17; needs the change data of Step 4; off by default, no network without consent |

### Why sharing and sync do not need a rewrite later

Step 4 builds the pieces they will both use, with only a local implementation behind each:

- stable `uuid`, `updatedAt` and `deletedAt` on every syncable row (the change set for sync, the identity for import conflict handling);
- one versioned export with a hashed manifest (what Quick Send transfers and what a sync snapshot contains);
- a `Transport` interface (send or receive an export or change set) so Sharesheet, Sharewave, WebDAV and a home server are interchangeable back ends;
- a scoped, read-only repository view (what a Shared Vault session exposes, and what a widget reads).

Step 10 adds a small `ShareService` for note export through Android's Sharesheet; Step 17 extends it.

## Minimum Android version

**Decision (2026-10-18): keep `minSdk` 26 (Android 8.0)** unless the repository audit finds a compelling compatibility reason. Monet (Android 12) and Liquid Glass (Android 12) are greyed out below their versions with "Not supported by your Android version".

Why 26 works: Reasons that are facts about the platform: `java.time` and notification
channels are part of API 26, so no desugaring or compatibility code is needed for dates, time zones and notification control.
Newer features are used only where present and have a fallback: Monet colours need Android 12, the pill's lens needs
Android 13 (both already gated), exact alarms changed in Android 12 and 13, runtime notification permission came in
Android 13, and foreground-service type rules came in Android 14. I did not look up install-base percentages; check Google's
current Android distribution figures before deciding to raise it.

### Notification features by Android version

| Version | Feature | Plan |
| --- | --- | --- |
| 8.0+ (26) | notification channels, actions | baseline for everything |
| 12+ (31), 13+ (33) | exact-alarm permission changes; runtime `POST_NOTIFICATIONS` | inexact by default; exact only on opt-in; explain a denied permission |
| 16 (36), **chosen** | progress-centric notifications and Live Updates (`Notification.ProgressStyle`) for ongoing, time-bound activity | study timer and "class in N minutes", with the normal ongoing notification as fallback. Source: [Pro Android Dev](https://proandroiddev.com/live-updates-in-android-16-exploring-the-next-evolution-of-notifications-1a5cf5de2068), [Android Developers Blog](https://android-developers.googleblog.com/2025/01/first-beta-android16.html) |
| 15 and 17 | other notification changes | **not verified**: I found no source for Android 17 specifics; they are checked against developer.android.com when Step 8 starts |

## Cloud features (Step 18, after the stable release, not started)

Both features are **disabled by default and make no network request until the user gives explicit consent**; while off they register no listeners and no background work, so there is no cost in speed or battery.

| Feature | Back end | Notes |
| --- | --- | --- |
| **Sync** (Route A, first) | a WebDAV or Nextcloud server the user provides (self-hosted, free or paid); Google Drive as another target only if it can be done within Google's Terms and API policy | encrypted snapshots built on the Step 4 export; an **offline queue** keeps changes until a connection and consent exist; a **conflict screen** (keep mine, keep theirs, keep both) resolves rows changed on two devices; difficulty medium to large |
| **Routine encrypted backup and restore** | Google Drive only, subject to Google API policy approval | `drive.appdata` scope; scheduled backup off by default, Wi-Fi only; needs an OAuth client and the final signing certificate, which only the owner can create. The manual local backup file stays |
| Route B (own server in Docker with a change-feed API) | later option | server code, security tests and self-hosting docs; very large; not started |

Plain `http://` stays limited to a private network; a public server requires TLS. Credentials for WebDAV are stored with the same encrypted vault as API keys and never exported.

## Conflicts and how they were resolved

- Spec: "no F-Droid, GitHub Releases and own website only". Obtainium reads GitHub Releases, so both agree.
- Spec: Google sign-in, device pairing and 2FA exist only where a backend needs them: they are out of Local-only mode; Google sign-in belongs to Step 18 (backup).
- Spec: "never export API keys". Keys live in `ApiKeyVault`, not the database, and the Step 4 export excludes secrets by definition; re-check when it is written.
- ~~CI publishes a prerelease on every push~~ resolved: CI uploads artifacts only; the manual *Dev release* workflow publishes development prereleases.
- Application ID changed from `com.absencium.app`; there is no upgrade path from it (see `docs/SIGNING.md`).
- Peer-to-peer sharing is deferred until the core app is stable (Step 17), as requested.

## When is it 1.0?

Approved scope: Steps 2 to 10 and 14 to 16, done and used on a real phone for a few weeks, with a verified signing key, a release built from a tag, privacy docs that match the app, and your explicit approval. Steps 11 to 13 follow as 1.x; Steps 17 and 18 come after the stable release.
