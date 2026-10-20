# Privacy and threat model

Status: written alongside the Step 4 data foundation. It describes what the code does; it has **not** had an outside security
review, and nothing here has been device-tested.

## What stays on the phone

Absencium is offline-first. It makes no network request unless the user turns on a feature that needs one (AI import, a
future sync target). Those features are off by default and are never contacted without consent.

## Backups

A backup is one file: a zip with `database/Absencium.db`, optionally `attachments/*`, `settings.json` and a `manifest.json`
written last, which lists every other entry with its size and SHA-256.

- **What is in it:** the database, the settings, and (if chosen) photos, documents and recordings of entries.
- **What is not in it:** the AI API key (it lives in its own vault and never leaves it), "last backup" bookkeeping, and the
  notes folder if the user keeps notes as files in a folder of their own.
- **Settings include `ai_base_url`.** That is a server address the user typed and could identify a private server. A plain
  (unencrypted) backup therefore reveals it. Use a password if that matters.

### Password protection

Chosen by the user, optional. If a password is given the zip is wrapped as follows.

- Key: PBKDF2-HMAC-SHA256, 600,000 iterations, 16-byte random salt, from the Android platform's crypto provider.
- Data: AES-256-GCM from the platform provider, in 64 KiB chunks, because GCM on the platform buffers a whole message and a
  backup with photos is too large for that.
- Nonce: 8 random bytes (per file) plus a 4-byte chunk counter, so no nonce repeats under one key.
- The header and an "is this the last chunk" flag are authenticated with every chunk, so a changed header, reordered chunks
  or a file cut short at a chunk boundary fail to decrypt.
- The password is never stored. **A forgotten password cannot be recovered.**

Known limits (be honest about them):

- **The framing is custom** around standard primitives. It follows a well-known chunked-AEAD pattern, but it is our own code
  and has not been reviewed by a cryptographer. Treat it as "good protection against casual access to a file", not as
  certified. Unit tests cover round trips, wrong passwords, bit flips, truncation, reordering and odd sizes; they run in CI.
- A weak password is weak: 600k PBKDF2 iterations slow guessing down but cannot save "1234".
- File size and the number of chunks are visible. The header (iteration count, chunk size) is not secret.
- An unencrypted backup is only checksummed, which detects damage and accidental edits but **does not stop** someone who
  rebuilds the zip with a matching manifest. Checksums are not authentication.
- The password lives briefly in memory as a `CharArray`/`String` typed into a text field; Android gives no way to guarantee it
  is wiped.

### Restore safety

A restore is staged first: the package is read once into a private staging folder, every entry name is validated (no paths
that could escape the folder), every size and SHA-256 is compared with the manifest, the SQLite header and
`PRAGMA quick_check` are verified and the schema version must not be newer than this app's. Only then is it parked next to
the live data. The swap happens on the next start, before the database opens; the previous database and attachments are kept
as `.bak`, and an interrupted swap is rolled back from them. Only one `.bak` generation is kept.

Restoring settings accepts only a fixed list of known keys with the right type; unknown keys are ignored.

## Local snapshots

An optional daily copy of the database and settings (no photos), the newest three kept in the app's private storage. They are
not encrypted: they have the same protection as the app's own database, i.e. Android's app sandbox. Anyone who can read the
app's private storage (a rooted phone, an unencrypted adb backup, a device backup the user allows) can read them.

## Trash

Deleted notes, exams, holidays and lectures are kept for 30 days, then removed for good (with a note's attachment files).
Subjects and attendance entries are deleted immediately, not trashed. Deleting a subject removes its Trash entries too.

## Sync and sharing (not built)

`Transport`, `ChangeSet` and `ReadOnlyScope` are interfaces only, with a local-folder transport. Nothing reaches a network.
When sync arrives it will be off by default, send only encrypted blobs, and ask for consent before the first connection.
Sharing will hand out a scoped read-only view that is empty unless the user fills it in.
