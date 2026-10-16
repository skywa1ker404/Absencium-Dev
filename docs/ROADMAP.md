# Roadmap

How [MASTER_SPEC.md](MASTER_SPEC.md) maps onto what exists today, in the order it can be built. Nothing below is claimed to
work until a CI build compiled it and, where it needs a device, you have tried it. Status words: **exists** (in the code
now), **partly** (some of it exists), **planned**.

## What exists today (the base to extend, not rebuild)

| Area | State |
| --- | --- |
| Local database (Room, v1 `Absencium.db`, schema guard in CI) | exists; no stable sync ids, revisions or tombstones yet |
| Backup file | partly: `DatabaseBackup` zips the database; no manifest, versioning, integrity hash or encryption; staged restore on next start exists |
| Storage folder | partly: `StorageFolder` over app files or a SAF folder; no ".Absencium subfolder or directory itself" choice dialog |
| API key storage | exists: `ApiKeyVault`; one key per provider only |
| AI | exists: providers, model list and picker, text extraction from photos, YouTube transcript notes, holiday import |
| Vault, drafts, notes, subjects, timetable, exams, holidays | exists; the Text Notes editor and four Add Entry actions (Document or file, Audio note, Solve homework, Generate AI notes) are unfinished |
| Search, profile, popups, themes, glass, onboarding | exists |
| Diagnostics | partly: debug logger and Test Lab; no redacted export |
| Widgets, Operations card, update engine, Drive, sync, pairing, 2FA, OCR, PDF parsing, EXIF scrubbing, Share note, Markdown toolbar, snapshots, focus mode, undo/trash, tag autocomplete | planned |

## Order of work (each phase needs the one before it)

### Phase 0: before anything is released
- Replace the public signing key and put the new one in GitHub secrets (LICENSE_AUDIT.md finding 1). Google sign-in later
  needs the signing certificate's SHA-1, so the key must be final first.
- Decide the open items in LICENSE_AUDIT.md; add the GPL-3.0 `LICENSE` through GitHub.
- Add an in-app Open-source licences screen and the About "Buy Me a Chai" card
  (https://www.buymeachai.in/skywa1ker404).

### Phase 1: the data foundation (everything else sits on it)
1. Stable ids, `updatedAt` revision, `deletedAt` tombstone on every syncable table; one Room migration with exported
   schema; a trash with restore (this is also the QoL undo).
2. Operation queue table (pending changes) independent of the UI and of any cloud provider.
3. One versioned export format: manifest (format version, app version, schema version, created, item counts, SHA-256 per
   file), the database, attachments, settings without secrets. Import validates the manifest and hashes first.
4. Optional password encryption (AES-256-GCM with PBKDF2 or Argon2 from a reviewed library, never hand-written crypto),
   with a threat model in `docs/PRIVACY.md`.
5. Rolling local snapshots (3) built on the same export code.

### Phase 2: Local-only mode, the default (no account anywhere)
1. Storage directory dialog: ".Absencium subfolder" or "this directory directly"; the database and secrets stay private.
2. Export and restore in Settings and in onboarding, reusing Phase 1.
3. Delete-everything wizard with optional export first.
4. Operations dashboard card in Profile plus the Operations sheet, fed by the real queue, never invented progress.
5. Optional biometric lock (BiometricPrompt).

### Phase 3: file handling (local, no AI needed)
1. PDF text extraction, Tesseract OCR with downloadable language packs (separate from AI), EXIF scrubbing on export.
2. "Extract Text" popup: With AI or Locally, with the disclaimers; AI options greyed out with a toast when Ask AI is off or no
   key or model is set; turning Ask AI off hides everything AI.
3. Finish the Add Entry actions (Document or file, Audio note, Solve homework, Generate AI notes) on top of these.

### Phase 4: the editor and the Vault
1. Text Notes editor with a floating Markdown toolbar; tag autocomplete; autosave indicator.
2. Vault as a file explorer: breadcrumbs, badges, long-press multi-select, batch actions; drafts folded into it.
3. Share note as PDF, Markdown or ZIP.

### Phase 5: calendar, tasks, widgets
1. Calendar list view and To-Do; smarter reminders; countdown and Focus mode.
2. Home-screen widgets: 1x1 quick capture, then 1x2 up to 4x4.
3. Permissions onboarding; power and thermal handling.

### Phase 6: Backup mode (Google Drive, optional)
Needs: Phase 1 export and encryption, a Google Cloud project with an OAuth client for `skywa1ker404.absencium` and the
final signing key (only you can create these).
1. `drive.appdata` scope only, explained before the request; Google sign-in only when this mode is chosen.
2. Encrypted incremental snapshots (hashes, tombstones), Wi-Fi only, 5 MB large-file threshold, retention, scheduled backup
   off by default, disconnect and delete-remote controls.
3. Greyed-out AI sync and backup items when cloud backup or sync is active.

### Phase 7: Experimental sync (only if a backend exists)
Needs a backend you approve; free tier only, no billing without your say-so (spec section 15). Until one is configured the
mode stays disabled. Covers: conflict screen, explicit sync, device pairing and sessions, TOTP, account deletion, backend
security tests. No real-time claims.

### Phase 8: updates and polish
1. GitHub Releases updater (check, download, verify signature and hash, hand off to the Android installer; no silent
   install claims), with the Settings switch.
2. Privacy docs, README rewrite, in-app Diagnostics screen with redacted log export.
3. The QoL list: universal search, quick actions, bulk selection, accessibility, scroll-position memory, haptics setting,
   date and time format settings, swipe actions and tab swipes, first-launch gesture guide, predictive back, long-press
   shortcuts, reset preferences. Several are cheap and can land earlier whenever a screen is already open for work.

Also open from earlier requests: multiple API keys per provider with a setup-instructions dropdown, replacing the last
custom dropdowns with `InlineDropdown`, nav pill label clipping, garbled text under the glass nav, voice-prompt popup on
long-press of the AI icon (after Phase 3), optional AI Memory (off by default), CHANGELOG and docs refresh.

## Conflicts and how they are resolved

- Spec: "no F-Droid, GitHub Releases and own website only". The README mentions Obtainium; that is a GitHub-releases
  client, so both are consistent.
- Spec: Google sign-in, device pairing and 2FA "only where a backend supports them". They stay out of Local-only mode and
  are built only in Phases 6 and 7.
- Spec: "never export API keys". The existing `DatabaseBackup` zips the whole database; the keys live in `ApiKeyVault`, not
  in the database, but this must be re-checked when Phase 1 defines the export contents.
- The CI publishes a prerelease on every push to `main`. For a public repository this should become tag-driven before the
  first real release.
- Changing the application ID made the new app a different app from `com.absencium.app`; there is no upgrade path from it.

## When is it 1.0?

Phases 0 to 4 done and used on a real phone for a few weeks: Local-only mode with export, import and snapshots; a
migrated database with trash; the file tools; the editor and Vault; a verified signing key; a GitHub release built from a tag;
privacy docs that match what the app does. Backup mode (Phase 6) can follow as 1.1; experimental sync stays labelled
experimental and is not needed for 1.0.
