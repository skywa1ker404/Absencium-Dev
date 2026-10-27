# Master technical specification, original text (as received 2026-10-16). ARCHIVE: superseded by [../MASTER_SPEC.md](../MASTER_SPEC.md)

Kept unchanged below for history. It still contains plans the owner has since cancelled (cloud backup, sync, pairing, accounts, free-tier backend, local OCR). Do not build from this file; build from the current spec.

---

MASTER TECHNICAL SPECIFICATION — ABSENCIUM

Privacy-First Android Application, Storage Modes, Cloud Architecture, Security, UX and Stability

You are modifying my existing Android application, Absencium. Treat this as an update and completion of an existing project, not permission to rebuild it from scratch, remove existing features, replace established architecture unnecessarily, or redesign the entire application according to your personal preferences.

First inspect the complete project , existing source code, database schemas, features, assets, themes, navigation, animations, dependencies, build configuration and existing documentation. Understand what already works before making changes.

Preserve all existing working features and their behavior unless a change is explicitly required by this specification. Reuse existing implementations where appropriate. Do not create duplicate screens, repositories, database entities, utilities or competing implementations of the same feature.

Implement actual functionality, not just UI mockups, placeholders, simulated operations or documentation describing features that do not exist.

If an important requirement cannot be implemented correctly with the available architecture, APIs, dependencies or infrastructure, explain the limitation and implement the safest compatible alternative. Never invent APIs, claim tests passed when they did not run, or silently omit requirements.

---

1. CORE ARCHITECTURE AND DESIGN PHILOSOPHY

Platform and distribution (if u have something else in mind suggest or something else has already been implemented tell me or suggest me about it) 

- Platform: native Android only.
- Language: Kotlin.
- UI: Jetpack Compose and Material 3 Expressive.
- Architecture: clean architecture, MVVM, repositories, StateFlow and Kotlin Coroutines/Flow.
- Local persistence: Room, DataStore and Android app-private file storage.
- Background work: WorkManager only where appropriate for durable, deferrable operations.
- Distribution: GitHub repository, GitHub Releases and my own website.
- No Google Play Store release is planned.
- Drop the F-Droid distribution requirement entirely.
- No advertising SDKs, analytics, telemetry, billing libraries, mandatory cloud accounts or mandatory internet connection.
- Do not introduce a proprietary Google Play Services dependency into the local-only core unless genuinely required by an optional integration.

The project must remain maintainable, offline-first, privacy-focused and compatible with supported Android versions. Follow the existing minimum SDK and target SDK unless a justified change is necessary.

Existing design language

Preserve Absencium's established proprietary design language, color engine, custom visuals, layouts, navigation structure, bespoke animation curves and existing feature behavior.

Continue using the existing theme options and palettes, including Monet mode, Liquid Glass, the fixed default palette and the theme-preset system, wherever already implemented or specified.

Use modern Material 3 Expressive shapes, pill components, animations and responsive layouts consistently with the established design. Do not introduce arbitrary colors, generic template screens or unnecessary redesigns.

The Vault must gain a subtle, functional file-explorer aesthetic featuring directory trees or breadcrumbs where appropriate, metadata badges, multi-selection, contextual actions and clear file organization. These additions must not remove or replace any existing Vault functionality.

---

2. THREE STORAGE MODES

Implement three distinct modes controlled through Settings.

Mode 1 — Local-Only Mode

This is the default mode immediately after installation.

Requirements:

- No account creation, login, email, CAPTCHA, 2FA or cloud authorization.
- No app-data upload, cloud synchronization or remote tracking.
- All existing core features must work offline.
- Store databases, drafts, notes, attendance records, reminders, calendar entries, settings, AI chat history and supported attachments locally.
- Use Room, DataStore and internal app-private storage.
- Never depend on a cloud provider to open or edit locally available content.

Allow users to select a custom storage directory through Android's Storage Access Framework.

When selecting a custom directory, show a dialog offering:

1. Create and use an ".Absencium" subfolder.
2. Use the selected directory directly.

Explain the implications before changing storage locations. Persist the granted directory permission where supported, handle revoked permissions safely and never claim that a ".nomedia" file makes a directory private.

Sensitive databases, encryption keys and authentication tokens must remain in appropriate private storage. Do not relocate sensitive internal database files into a user-accessible directory merely because a custom folder was selected.

Local export and restore

Provide a complete export system that allows users to choose which supported data categories to include.

Support:

- Notes, drafts and folders.
- Attendance, assignments, reminders, calendar data and settings.
- Supported Vault entries and attachments.
- AI chat histories and locally stored AI memories, if present.
- Other user-created content supported by the current application.

Use a versioned ".zip"-based archive format with a manifest, integrity verification and schema-version information.

Offer optional password-based encryption using a reviewed AES-256-GCM implementation and a suitable password-based key derivation function. Do not invent cryptographic primitives.

Provide Export Backup, Import Backup and Restore Data from a Local Backup actions.

Offer local backup restoration during initial onboarding and from Settings. Never overwrite existing data without clear confirmation.

Never export AI provider API keys, OAuth access tokens, refresh tokens, session credentials or other secrets.

Mode 2 — Backup Mode

Backup Mode provides unidirectional, versioned snapshots to an explicitly selected cloud provider.

Google Drive is the first implementation, using the narrowest suitable OAuth scope:

"https://www.googleapis.com/auth/drive.appdata"

Use Google's supported authorization flow and current API documentation. Do not assume the scope eliminates OAuth configuration, verification requirements, quotas or future policy changes.

Requirements:

- Backup is optional and requires explicit user consent.
- The initial implementation must not require an Absencium-operated server.
- Store encrypted snapshots and supported attachments in Google Drive's application-specific data area.
- Do not automatically download remote content during normal use.
- Allow the user to restore a cloud snapshot during onboarding or manually through Settings.
- Provide a backup history, last successful backup time, backup size, status and actionable error messages.
- Retain configurable older snapshots and avoid replacing the last verified backup with an incomplete or invalid upload.
- Allow users to disconnect Google Drive without deleting their local data.
- Offer separate, explicit controls for deleting remote backup data.

Efficient incremental backups

Use stable object IDs, canonical serialization, revision metadata, content hashes and a durable local change log to identify changes.

SHA-256 may be used for integrity and change detection. Do not treat a hash alone as proof of revision ordering or authenticity.

Skip unchanged objects and attachments where possible. Handle deletions using explicit tombstones or an equivalent mechanism so deleted records do not unexpectedly reappear after restoration.

For interrupted uploads, retry safely and avoid duplicate objects. Verify uploaded data before updating the manifest identifying the latest valid snapshot.

Network controls

Implement the following settings:

- Backup only on Wi-Fi.
- Do not transfer large attachments over cellular networks.
- Configurable large-file threshold, defaulting to 5 MB.
- Manual backup and restore.
- Configurable retention policy.
- Optional automatic scheduled backups.

Scheduled backups must respect Android background-work restrictions, battery saver, thermal state and network constraints. Automatic scheduled backup is disabled until explicitly enabled by the user.

Use configurable backoff, cancellation, quota handling and resumable transfers where supported by the provider.

Compression and document conversion

Offer optional compression choices without promising impossible quality guarantees.

- Preserve original files by default.
- Support lossless compression where applicable.
- Offer adjustable lossy image compression.
- Display estimated output size and quality implications when practical.
- Never automatically replace an original image or PDF with a lossy or converted derivative.
- Structured text and JSON extraction must be optional, versioned and reversible where practical.
- Retain the original document alongside extracted content.

Do not claim that every 5–6 MB image can be reduced to 500–800 KB without meaningful quality loss.

Mode 3 — Pure Cloud / Sync Mode (EXPERIMENTAL)

Clearly label this mode Experimental.

Display a warning during setup advising users not to use their primary or otherwise important Google account for experimental testing if they are uncomfortable with potential data loss.

This mode provides explicit multi-device synchronization, with Room and local files acting as the cache and offline workspace.

When enabled:

- Cloud state is authoritative for successfully synchronized data.
- Local changes must still be saved immediately.
- Offline edits must be preserved in a durable pending-change queue.
- Cached content remains available offline.
- The app must clearly distinguish locally saved, pending, synchronized and failed changes.
- No pending edits may be silently discarded.
- The user must explicitly initiate synchronization by default.
- Returning online must not silently upload queued edits.
- A separate opt-in automatic-sync setting may be offered later.

Important architecture constraint: Google Drive's "appDataFolder" is file storage, not a transactional database or a real-time synchronization backend. Implement Drive-based synchronization using versioned encrypted objects, manifests, revision checks and conflict handling. Do not advertise true real-time synchronization when only manual or periodic reconciliation is implemented.

If a requirement needs a trusted server, server-side account authentication, global session revocation or guaranteed concurrent-writer coordination, document that limitation. Do not silently introduce a paid backend or pretend Drive provides these capabilities.

Cloud sync data

Synchronize supported user data, including:

- Notes, drafts, titles and folders.
- Attendance and assignments.
- Reminders, holidays and calendar entries.
- Supported Vault metadata.
- AI chat histories and AI memories when explicitly included.
- User-selected structured text and JSON derivatives.
- Attachment references, revisions and supported file contents.
- Compatible user settings.

AI provider API keys, OAuth tokens and authentication secrets must never be synchronized.

Large-file routing

For image-rich PDFs and attachments above a configurable threshold, initially 5–10 MB:

- Do not route large binary contents through ordinary record synchronization.
- Store encrypted binary objects separately.
- Download files on demand.
- Keep locally cached files usable offline.
- Track the local and remote revisions independently.
- Require explicit confirmation before replacing a remote master file when appropriate.
- Upload and verify the replacement before updating the remote reference.
- Preserve the previous valid remote version until replacement verification succeeds.

Support safe retry, cancellation, cache cleanup and storage-space checks.

---

3. DATA MODEL, OFFLINE QUEUE AND CONFLICT RESOLUTION

Keep a local Room database for immediate UI access, durable edits and the pending-change queue.

Every synchronizable record must have:

- A stable identifier.
- A record type and schema version.
- A revision or equivalent version identifier.
- Modification metadata.
- A deletion tombstone where applicable.
- Enough ancestry or revision information to detect conflicting edits.

Do not rely solely on device timestamps. Device clocks may be incorrect, and two devices can edit the same record while offline.

Implement the following behavior:

- Changes to different records can be merged.
- A change with no competing revision can be accepted.
- Concurrent changes to the same record must be preserved and reconciled safely.
- Conflicting note content must never be silently discarded.
- Deletes must not accidentally resurrect old data.
- Repeated requests must be safe where practical.
- An interrupted transfer must resume or restart without corrupting existing data.
- A failed synchronization must preserve pending local changes.

If automatic merging is unsafe, retain both versions and provide a conflict-resolution screen.

Keep the synchronization engine independent of the UI and cloud provider.

---

4. DEVICE PAIRING AND SESSION MANAGEMENT

Apply these requirements only where supported by the chosen cloud architecture.

Primary and companion devices

If account-based multi-device access is implemented:

- A primary device may authorize companion devices.
- Provide a QR pairing flow with short-lived, single-use pairing credentials.
- Offer an eight-digit one-time pairing code with a short expiration period.
- Use cryptographically secure randomness.
- Apply rate limiting, expiration and replay protection.
- Do not transmit account passwords or long-lived tokens through QR codes or pairing codes.
- Companion pairing must use a secure authenticated channel.

The primary device may display active sessions, device labels, Android versions where available, last-seen information and session-revocation controls.

Companion devices may view their own status and the permitted primary-session status, but must not automatically gain access to other companion devices' administrative details.

Allow companion users to log out at any time.

Primary device inactivity

Do not implement the original 14-day automatic logout rule as a blind local timer.

If a trusted service exists, it may enforce a configurable session-expiry policy. If only Google Drive is available, do not pretend Drive provides reliable global session authority.

If the experimental primary-device policy is retained, it must be clearly documented, recoverable and designed so that an offline primary device does not destroy unsynchronized data or make all companion data permanently inaccessible.

Session revocation must invalidate relevant authorization credentials and pending operations without deleting user content.

---

5. AUTHENTICATION AND ACCOUNT RECOVERY

Show Google sign-in or account setup only when the user selects Backup Mode or Experimental Cloud/Sync Mode on boarding required options or in settings and a compatible account-based provider requires it. and the profile personal info should update with logins (optional) but can be edited

Local-Only Mode must never show a login screen.

Google Drive authorization

Request the minimum necessary scope and explain what the app can access.

Use the supported Android authorization flow and current Google documentation. Do not collect Google passwords directly or build an unofficial Google login screen.

Store authorization credentials securely using supported platform mechanisms. Exclude them from backups, logs and exports.

Email/password accounts

Only implement email/password authentication if a real, documented authentication backend is available.

Use established authentication libraries or a properly secured service with rate limiting, secure password storage, short-lived access tokens and protected refresh-token rotation.

Do not invent a REST endpoint, use hardcoded credentials or claim that Drive itself supplies account authentication.

Biometric lock

Implement optional Android "BiometricPrompt" app locking where supported.

Biometrics protect local app access; they are not a substitute for account recovery, remote session revocation or recovery of an end-to-end encryption key.

Two-factor authentication

Do not make TOTP a mandatory dependency of Local-Only or Google Drive-only operation.

If a compatible authentication backend supports TOTP, make it optional and implement enrollment, secure secret storage, recovery and rate limiting properly.

Use a verified recovery email when an account backend exists, or a user-controlled high-entropy recovery key or recovery code for encrypted data.

Clearly explain that account recovery and decryption-key recovery are separate processes.

Never claim that a password reset automatically recovers an encryption key.

---

6. ENCRYPTION AND PRIVACY

Use a reviewed cryptographic implementation and a documented threat model.

- Encrypt sensitive cloud-bound content on the device before upload.
- Use authenticated encryption such as AES-256-GCM with correct nonce management.
- Use Android Keystore to protect appropriate local key material.
- Use a suitable password-based key derivation function for password-protected archives.
- Separate account authentication from data encryption.
- Never store the only recovery key solely on the original device.
- Never log note contents, passwords, encryption keys, OAuth tokens or API keys.
- Keep filenames and sensitive metadata out of plaintext cloud manifests wherever practical.
- Validate all imported data and treat downloaded content as untrusted input.
- Do not introduce unnecessary network endpoints or background telemetry.

Explain the recovery consequences of lost passwords or encryption keys. Cloud encryption does not protect against every form of deletion, account loss or device compromise.

---

7. FILE HANDLING, OCR AND PDF PROCESSING

All Local OCR, extraction and document parsing must run locally on the device in the current specification. Separated from Ai extractions

Do not send user documents to remote OCR or AI services in local mode 

Tesseract OCR

- Exclude Tesseract language binaries from the base APK where feasible.
- Offer optional language-pack downloads.
- Display exact package sizes before downloading.
- Allow users to remove downloaded language packs.
- Handle missing language packs and insufficient storage gracefully.

PDF processing

- Preserve source PDFs.
- Parse suitable documents into Markdown or structured JSON only when requested or when an explicitly enabled feature calls for it.
- Use size thresholds as performance hints, not absolute indicators of whether a PDF is scanned or text-based.
- Detect scanned/image-rich documents and offer local OCR when appropriate.
- Preserve page order, text structure and available layout metadata.
- Never silently replace a source PDF with extracted text or a converted document.

EXIF and metadata handling

For sanitized exports and cloud-transfer derivatives, remove sensitive location metadata and other unnecessary identifying information.

Do not silently destroy the user's original image metadata. Keep originals unchanged by default unless the user explicitly chooses permanent sanitization.

Apply metadata scrubbing consistently to generated exports and derived attachments.

---

8. VAULT, NOTE SHARING AND PORTABLE EXPORTS

Preserve all existing Vault functionality.

Add:

- Functional folder navigation and breadcrumbs where appropriate.
- File-type and metadata indicators.
- Long-press multi-selection.
- Batch move, export, compression and cache-clearing actions.
- Clear confirmation dialogs for destructive actions.

Share note

Add a Share Note action for each supported note.

Offer:

1. PDF: a polished, readable document suitable for printing and sharing.
2. Markdown (".md"): editable text and formatting.
3. ZIP: a portable package containing the selected note, supported attachments and required metadata.

The user must be able to choose which attachments and supported private fields to include.

PDF export should preserve headings, paragraphs, lists, checkboxes, tables and images where supported. Avoid silently omitting embedded attachments.

Export locally by default through Android's share sheet and Storage Access Framework. Do not upload an export automatically.

Exclude API keys, access tokens, refresh tokens, encryption secrets and internal session data from every exported package.

Complete data export

Use the common versioned backup format for local export, cloud snapshots and compatible transfers.

Validate archive versions, manifests, attachment references, sizes and integrity before import.

Never overwrite existing data without explicit user confirmation.

---

9. ACCOUNT DELETION AND DATA RETENTION

Keep these actions separate:

- Disconnect cloud authorization.
- Delete remote backup snapshots.
- Delete an account, if a genuine account backend exists.
- Clear local cache.
- Delete all local application data.
- Export data before deletion.

Use a multi-step confirmation wizard for destructive operations.

Before deleting cloud data, explain whether local records, exported files and other remote copies remain.

Offer an optional local export before account deletion.

If the user chooses to preserve encrypted remote files, clearly explain that those files may become unrecoverable if the only encryption recovery key is lost.

Do not claim that deleting an app account automatically deletes copies stored outside the app's control.

---

10. OPERATIONS DASHBOARD

Add a persistent Operations Card in the Profile tab.

It must display actual ongoing operations, such as:

- Cloud backup and restore.
- Manual synchronization.
- Large-file downloads and uploads.
- OCR and compression tasks.
- Batch archive export/import.

Tapping it opens an Operations Sheet with:

- Real progress where measurable.
- Current operation and item name.
- Bytes transferred and total size where available.
- Transfer speed when a meaningful measurement exists.
- Pause, resume and cancel actions where supported.
- Retry actions for recoverable failures.
- Clear success, warning and error states.

Never fabricate progress, network speed or completion status.

Operations must survive process recreation where necessary, and interrupted work must leave data in a consistent state.

---

11. EDITOR, WIDGETS AND QUALITY-OF-LIFE FEATURES

Preserve existing editor behavior and add the following where not already implemented.

Floating Markdown toolbar

Provide a formatting toolbar above the keyboard with shortcuts for headings, bold, lists, checkboxes and code blocks.

Include a floating edge-bubble toggle that collapses or expands the toolbar. Ensure the toolbar respects keyboard insets, system navigation and accessibility settings.

Home-screen widgets

Using Jetpack Glance where appropriate:

- 1×1 Quick Capture widget.
-all other specific iced grids in past specification
- Expanded dashboard widget displaying supported note previews, recent activity and quick actions.

Widgets must use existing data repositories and avoid unnecessary database work. Respect Android background restrictions and user privacy, especially when displaying note previews on the home screen.

Rolling local snapshots

Retain the last three valid local database snapshots by default, with an optional setting to disable retention.

Use a consistent SQLite backup mechanism that correctly handles transactions and WAL state. Validate snapshots before replacing older valid copies.

Smart power and thermal handling

Respect Android battery saver, thermal status, storage constraints and network restrictions.

Pause or defer expensive OCR, compression and background transfers when the system indicates that the device should reduce work.

A configurable low-battery threshold may default to 15%, but must not interfere with immediate local saves or risk losing user edits.

Countdown, Focus Mode and tagging

Preserve existing countdown features and improve their implementation without changing their intended behavior.

Preserve the specified Focus Mode capabilities, including study-session locking, display-timeout control and protection against accidental navigation, within Android's actual permission and system-policy limits.

Implement real-time local tag auto-completion using an efficient index. Do not introduce remote search or upload tags without consent.

Multi-selection

Support long-press multi-selection in note lists and the Vault, with batch move, export, compression and cache-clearing actions. Destructive actions must require appropriate confirmation.

---

12. IN-APP UPDATE ENGINE

GitHub Releases is the only planned application distribution and update channel.

Provide a Settings option to enable or disable update checks.

When enabled:

- Query the appropriate GitHub Releases API endpoint.
- Compare valid release versions using proper version comparison.
- Distinguish stable releases from prereleases if both exist.
- Download the correct APK asset for the device.
- Verify the download and confirm the expected package identity and signing certificate.
- Use Android's supported package-installation flow.
- Request unknown-app installation permission only when required, explaining why.
- Let the user cancel installation.
- Handle failed downloads, missing assets, network failures and incompatible APKs gracefully.

Do not promise silent updates or bypass Android installation safeguards.

Do not install APKs from arbitrary URLs or trust an asset solely because it has the correct filename.

Ensure the release APK is signed consistently with the installed application. A signature mismatch must fail safely and explain the problem.

---

13. LEGAL AND PRIVACY DOCUMENTATION

Do not claim automatic GDPR, India DPDP Act or California privacy-law compliance.

Provide an accurate privacy policy and document:

- Which data remains local.
- Which data can be uploaded to a selected provider.
- What authentication information is used.
- What encryption protects.
- What data is excluded from backups.
- How to export, restore, disconnect and delete data.
- What happens when the cloud provider changes its policies or becomes unavailable.
- Which features require internet access.
- Which optional AI operations may send data to an external provider, if such features exist.

Implement the consent, disclosure, access, deletion and retention controls applicable to the project's actual data flows and legal obligations.

The application must not falsely advertise itself as completely offline when cloud features are enabled, or as end-to-end encrypted unless the implemented architecture actually provides that guarantee.

---

14. BUILD, TESTING AND DELIVERY REQUIREMENTS

Before implementation:

1. Inspect the existing project and current build status.
2. Identify existing functionality, reusable code, dependencies and known defects.
3. Map every requirement to an existing implementation or a planned change.
4. Identify incompatible requirements and explain the safest resolution before introducing major architectural changes.

During implementation:

- Make small, coherent changes.
- Preserve existing database contents and provide safe Room migrations.
- Avoid duplicate implementations.
- Use structured error handling and redacted logging.
- Add unit tests, integration tests and UI tests where appropriate.
- Do not remove working features merely to make the build pass.
- Do not hardcode secrets, tokens, credentials or test-only production behavior.

Required test scenarios include:

- Fresh installation and Local-Only onboarding without internet.
- Export and restore with and without password protection.
- Incorrect password, corrupted archive and unsupported backup version.
- Cloud authorization cancellation and revocation.
- Quota exhaustion, interrupted transfers and provider unavailability.
- Offline edits and synchronization after reconnection.
- Concurrent edits and deletion conflicts across devices.
- App process death during backup, restore or synchronization.
- Missing attachments and insufficient storage.
- Android battery saver and thermal restrictions.
- Update downloads, signing mismatch and installation cancellation.
- Verification that no API keys or authentication secrets enter backups or logs.

Do not report tests as successful unless they actually ran. Clearly distinguish passed tests, failed tests, skipped tests and tests that could not be executed.

Final deliverables

Provide:

1. The guide for all necessary configuration and setups.
2. Do Required database migrations and tests 
3. Updated setup instructions for optional Google Drive integration.
4. GitHub Releases build link and signing instructions.
5. A clear list of implemented features and any remaining limitations.
6. A summary of security decisions and known risks.
7. Do not make the UI layout cluttery and position everything smartly accordingly to the apps current layout designs and positions nothing should feel intrusive everything should be dynamic.
8. Updated README and privacy documentation.
9.Add setup instructions for the selected ai's api in a dropdown under ai api options and add capability of saving more than 1 api entry from which one can be selected for each ai
10. look through our whole conversation to find out unfinished or forgoten features that are not implemented while respecting newer specifications so that the features don't conflict with one another or do opposite things or break anything ,Plan feature implementation smartly by implementing features first that are needed by other features in the future first and implement features according to the future compatibility so major code changes isn't required and make already existing features compatible with the planned feature implementations always optimize everything do not introduce visual glitches jittery or buggy animations weird and unnecessary artifacts or shaped and plan at what point should the app be ready for its first initial 1.0 releases and if u were interrupted because of me or ur usage limits continue from exactly where u were and from which code on line u were interrupted from and add essential and optional features as u see fit in the on-boarding menu as u see fit throughout the building process the codes must be optimized and integrate any new feature with existing feature wherever applicable everything should be consistent fluid and rich and while u are inspecting current features overhaul them if needed and wherever applicable features should be interconnected in current availabile features and planned features and holding the Ai icon near the navigation pill should open a popup that can use voice as promt text(when that feature is implemented) disabling Ask ai toggle removea everything related to Ai and if cloud backup or synchronization is activated grey out the ai option for syncing and backup from every menu and re enabling it brings them back even in local backup. update the past specifications with this and all other specifications if they have specifications of a similar feature.

Add a card in about u can support me on a buy me chai https://www.buymeachai.in/skywa1ker404

Final instruction: Preserve Absencium's existing identity and feature set. Prioritize data integrity, offline usability, privacy, recoverability and correct behavior over adding unnecessary complexity. Never silently discard user data, invent unsupported capabilities or claim an implementation is complete when it is not.

15. OPTIONAL FREE-TIER CLOUD BACKEND POLICY

Evaluate a cloud database and backend only when its current free-tier terms meet the project's requirements.

Preferred candidate: Cloudflare Workers + D1, with a compatible object-storage provider evaluated separately for large attachments. Google Drive must remain an independent optional backup provider.

Requirements:

- Verify current official pricing, quotas, account requirements, billing requirements, storage limits, data-transfer rules, inactivity policies and over-limit behavior before implementation.
- Do not assume that a free tier is unlimited, permanent or suitable for production without restrictions.
- Do not enable paid plans, paid add-ons, automatic upgrades or billable services.
- Do not require a payment card or billing account unless I explicitly approve that requirement.
- If a service requires billing setup or payment verification that conflicts with these requirements, do not integrate it as the default backend.
- Enforce application-level usage safeguards where measurable and document provider-enforced limits.
- When a free quota is exhausted, stop or defer cloud operations safely, preserve local edits and queued changes, and show a clear explanation.
- Never delete local data, pending changes or the last valid backup because a quota has been reached.
- Do not promise that all cloud features remain available after free-tier limits are exhausted.
- Keep local-only operation fully functional regardless of backend availability.
- Encrypt sensitive content before uploading it and minimize server-visible metadata.
- Keep backend authentication, database operations and attachment storage behind provider-independent interfaces.
- Never embed privileged server credentials or administrative API keys in the Android APK.
- Include a documented procedure for exporting all cloud data and migrating to another provider.
- Treat any change that introduces billing, a payment method, a new external data processor or a material privacy-policy change as requiring my explicit approval.

If no suitable provider meets these conditions, ship Local-Only Mode and Google Drive Backup Mode first. Keep Experimental Cloud/Sync Mode disabled until a compatible backend is configured and tested. or if there's workaround for using Google drive legally by complying it's terms and services 

Before integrating a backend, report the selected provider, exact free-tier limits, account and billing requirements, privacy implications, attachment-storage plan, quota-exhaustion behavior and migration strategy. Do not provision paid infrastructure or create billable resources without my explicit approval.

16. BACKEND SECURITY

Even with a backend, Absencium must remain a client, not a public database client with unrestricted credentials embedded in the APK. Authenticate users, authorize every request server-side, enforce strict per-user data isolation, and prevent unauthorized access to records or attachments. Never embed privileged keys or trust client-side checks alone. Test that one user cannot access, modify, or delete another user's data. If these security requirements cannot be met, keep cloud features disabled and preserve Local-Only Mode.

17. No surprise billing: Never enable paid services or automatic upgrades without approval.
Offline safety: If the backend fails, preserve local data and pending changes.
Conflict protection: Never silently overwrite newer notes during sync or restore.
Backup recovery: Test that backups can actually be restored before claiming they’re reliable.
Privacy by default: No mandatory account, telemetry, ads, or cloud access for local use.
Safe deletion: Explain what will be deleted locally and remotely before confirming account deletion.
One especially important addition: never claim end-to-end encryption unless it is genuinely implemented and tested. Also test account isolation and unauthorized access, following OWASP’s API security guidance. 

(Don't mind the numbers in this whole promt they are just specification number and sometimes its not in serial or have same numbers because I copy pasted 

QOL: 1. Universal search
Search notes, subjects, attachments, assignments, reminders, and dates from one place. Support recent searches and instant results.
2. Undo for destructive actions
Undo accidental deletions, archive actions, bulk edits, and attendance changes wherever safe. Use a trash bin for recoverable deletions.
3. Automatic draft saving
Save edits continuously, restore unfinished entries after crashes, and show a subtle “Saved” or “Saving…” indicator.
4. Quick actions
Add a note, mark attendance, create a reminder, or start a study timer with minimal taps. Support a home-screen quick-capture widget.
5. Smarter reminders
Offer sensible default times, snooze, rescheduling, recurring reminders, and a clear list of overdue tasks. Prevent duplicate notifications. 
6. Bulk selection
Select multiple notes, attachments, or tasks to move, tag, export, archive, or delete them in one operation, with confirmation for destructive actions.
7. Accessibility and comfort
Respect system font scaling, TalkBack, reduced-motion preferences, keyboard navigation, and Android dark mode where appropriate. Keep touch targets comfortable.
8. Reliable file handling
Remember the last-used folder, show attachment thumbnails, handle duplicate filenames safely, report failed imports clearly, and never overwrite originals silently.
9. Lightweight performance
Load large vaults progressively, generate thumbnails in the background, avoid unnecessary recomposition, and keep animations smooth without ignoring battery-saver or thermal restrictions.
10. Better troubleshooting
Add a diagnostics screen, meaningful error messages, exportable redacted logs, and a copyable app/build/database version summary. Never include private note contents or secrets in logs.
Small details that make the app feel polished
Remember each screen’s scroll position when navigating back.
Preserve search filters and sort order when reopening a vault.
Support swipe gestures only when they don’t conflict with Android back navigation.
Make empty states useful, with a clear action to create the first item.
Add haptic feedback where appropriate, with a setting to disable it.
Keep confirmation dialogs for genuinely risky actions; use undo or snackbars for reversible ones.
Make exports include clear filenames, dates, and formats.
Show meaningful progress for long-running imports, OCR, backups, and restores, with cancellation where safe.
Let users choose date format, first day of the week, and 12/24-hour time.
Provide a “Reset app preferences” option that doesn't erase notes or attendance data.
Swipe between main tabs
Swipe horizontally to switch between Dashboard,Attendance. and animate transitions naturally.
Pull down for quick access
At the top of the Dashboard, allow a deliberate pull-down gesture to reveal recent drafts, quick capture, or pinned actions. Avoid hijacking normal list scrolling or Android's notification shade gesture.
Swipe actions on items
Swipe a task to complete or snooze it.
Swipe a reminder to reschedule it.
Swipe a note to reveal pin or archive actions.
Use a confirmation or undo for destructive actions.
Keep these actions discoverable and provide buttons as alternatives.
 On first launch there should be a promt saying guide me through every menu gesture and or skip it 
 Respect Android's system back gesture, including predictive-back animations where supported. Don't intercept edge swipes to switch tabs or break system navigation.
 6. Expandable sheets
Drag bottom sheets between compact and expanded states for filters, calendar details, attachment previews, and quick actions. Preserve their scroll positions.
7. Long-press shortcuts
Long-press a note, subject, or task to open a contextual menu with relevant actions. Support entering multi-select mode without requiring a separate toolbar button.
Implement proper AI availability handling throughout Absencium. If the AI API key/model is not configured or the Ask AI toggle is OFF, all AI-dependent actions must use existing local tools wherever possible for images, PDFs, documents, OCR, text extraction, and other supported tasks. If an action requires AI and no local alternative exists, tapping it should show a toast saying either “API key/Model not set up” or “Ask AI is disabled”, depending on the situation. If the action has its own menu, dialog, or popup, grey out and disable AI-dependent options and clearly indicate whether AI is not set up or disabled; never silently invoke AI or block local functionality. When AI is enabled and properly configured, tapping “Extract Text” should open an animated Material 3 Expressive popup with two options: “With AI” and “Locally (Without AI)”. Use matching Material 3 Expressive entrance, exit, and transition animations consistent with the existing app, including extending the current popup animations. Show the existing “AI can make mistakes” disclaimer only after the user selects the AI extraction option, and show a separate disclaimer only after selecting local extraction: “Local extraction can make mistakes. Review the extracted text for accuracy.” Preserve existing features, themes, offline-first behavior, and local processing. In the future AI Hub, add an optional, OFF-by-default Memory feature that lets users explicitly enable, view, edit, clear, and disable AI memory. Obtain consent before saving information, explain what is stored and why, and never save private notes, documents, or extracted content to memory without explicit permission. Keep memory local where feasible, provide deletion controls, and never make memory or cloud access mandatory for AI features.


---
AMENDMENT 2026-10-25 (owner decision): all local OCR (Tesseract) was removed because it did not work well and added about 4 MB plus native libraries to the app. Text extraction from pictures, PDFs, Word files and recordings is done only with the user's AI; where AI is off or not set up, those actions say so. This overrides every earlier mention of local OCR, "Locally (Without AI)" and Tesseract in this file.
