# Backup with full choice: design (row 45.33)

Owner request (2026-11-15): "the backup option has many customisations, like what to back up and what not to; think very carefully and also design it". Status: design only, nothing built yet. Built in the phases at the end, one CI build each.

## 1. What exists today (read from the code, 2026-11-18)

- `data/backup/BackupManager.kt`: one zip package, `manifest.json` (last entry, with size and SHA-256 of every entry), `settings.json`, `database/Absencium.db` (a consistent copy of the whole database) and `attachments/<file>` (all photos, documents, recordings, sketches, only if "Include files" is on). Optional AES-256 seal (`BackupCrypto.kt`, extension `.absbak`).
- Restore: the package is checked, staged next to the database, and applied at the next start (`applyStagedRestore`), keeping one safety copy. It always replaces everything.
- The only choice today is "Include files" and the password. The AI hub has its own separate backup pages (WebDAV and local zip) with its own options (`BackupContentOption`: characters, chats, providers, API keys, workspaces, settings).
- Database `AbsenciumDatabase` (version 5 at phase 1, 19 tables; the original read of version 3 listed 15) has these tables: `Subjects_Table`, `Attendance_Table`, `Holidays_Table`, `Exams_Table`, `Timetable_Table`, `Notes_Table`, `Guides_Table`, `Audio_Memos_Table`, `Recall_Quiz_Table`, `AI_History_Table`, `User_Stats_Table`, `Momentum_Log_Table`, `Recall_Attempts_Table`, `Attachments_Table`, `Syllabus_Nodes_Table`, `Note_Versions_Table`, `Flashcards_Table`, `Study_Goals_Table`, `Events_Table`. The AI hub keeps its chats in its own database (`aihub_ai`) and its settings in its own preferences and secure store.

## 2. Goals

1. The owner sees every part of the data, with its size, and switches each on or off.
2. The same list is shown when restoring, so a backup can be restored in part.
3. Nothing is silently left out or silently included: the file's summary says exactly what it holds.
4. Old packages (format 1) still restore exactly as today.
5. Offline, no new permissions, no new libraries, GPL-3.0 compatible, no telemetry.

## 3. The parts

| Part | Where it lives | Needs |
|------|----------------|-------|
| Subjects | `Subjects_Table` | none (root) |
| Timetable | `Timetable_Table` | Subjects |
| Attendance | `Attendance_Table` | Subjects |
| Exams | `Exams_Table` | Subjects |
| Holidays and events | `Holidays_Table`, `Events_Table` | none |
| Flashcards | `Flashcards_Table` (subject id is not a foreign key) | none |
| Study goals | `Study_Goals_Table` (subject id is not a foreign key) | none |
| Note version history | `Note_Versions_Table` | Notes |
| Syllabus | `Syllabus_Nodes_Table` | Subjects |
| Notes (text) | `Notes_Table` | Subjects |
| Note photos, files and sketches | `Attachments_Table` rows (`note_id`, `file_path`, `size_bytes`) plus files | Notes |
| Documents (Vault) | `Guides_Table` plus the files its `local_uri` points to | Subjects |
| Recordings | `Audio_Memos_Table` (`note_id`, `file_path`) plus files | Notes |
| Recall and quizzes | `Recall_Quiz_Table`, `Recall_Attempts_Table` | Notes |
| Study stats and momentum | `User_Stats_Table`, `Momentum_Log_Table` | none |
| Profile and settings | `settings.json` (the app's DataStore, without secrets) | none |
| AI action history | `AI_History_Table` (`note_id`) | Notes |
| AI chats (hub) | hub database `aihub_ai`, chat files | none |
| AI settings (hub) | hub preferences, providers and assistants | none |
| AI keys | hub secure store (API keys) | AI settings, **and** an encrypted backup |

"Needs" is enforced in the list: switching a part on switches its needs on and greys them; switching a root off switches what needs it off.

## 4. Presets and the saved choice

- Everything, Light without media (no photos, documents, recordings, sketches or AI chats), Data only (subjects, timetable, attendance, exams, holidays, syllabus, notes text).
- A fourth entry, "My choice", is the last switch set the owner saved. It is stored in the app settings as the set of part ids, so the next backup starts from it.
- Keys are never part of a preset: the AI keys switch is off until the owner turns it on, and it is disabled while the backup is not encrypted.

## 5. The size shown before saving

Computed on the phone while the page is open, without reading the whole database into memory:

- Database parts: row counts and the sum of text lengths per table with one `SELECT count(*), sum(length(...))` each.
- File parts: the sum of `size_bytes` of the matching `Attachments_Table` rows; for recordings and Vault documents, which have no size column, the file length on disk.
- Hub parts: the size of its database file share and chat files.
- The page says "about" and shows the total in bytes of the zip before compression; encrypted and compressed sizes are not promised.

## 6. Package format 2

- Same zip, same manifest-last rule, same safe entry names (extended with `parts/` names that are checked by `ManifestRules.isSafeEntryName`).
- The manifest gains `formatVersion = 2` and `parts`: for each included part its id, row count, file count and byte size, so the restore page can list them without reading the data. A format 1 package has no `parts`; the restore page then shows it as "Everything" (what format 1 always was).
- Database: the exporter makes the consistent copy as today, then, in the **copy**, deletes the rows of every part that is off (children before parents) and runs `VACUUM`, so an excluded part truly leaves no trace in the file. The live database is never touched.
- Files: only the files that the included rows point to are added.
- Settings: `settings.json` is written without the profile photo name or keys when those parts are off.
- Hub: chats and settings are exported with the hub's own `BackupArchiveFormat` entries under `hub/`; keys go into `hub/secrets.json`, only inside an encrypted package.

## 7. Restore

1. Choose the file, enter the password if sealed, the package is verified (nothing changes yet).
2. The page lists the parts the file holds with counts and sizes and the date it was made; each part has a switch (all on) and the three presets.
3. Mode per restore: **Replace** (the chosen parts are replaced, parts not chosen stay as they are) or **Merge** (rows missing on the phone are added; a row that exists on both is kept as the newer by its `updated_at` (rows are matched by their `uuid` where the table has one, otherwise by id), ties keep the phone's).
4. Applying keeps today's mechanism: the selection is staged, the app restarts, `applyStagedRestore` runs before the database opens, with one safety copy of the current data. For part restores it opens the backup copy as an attached database and copies the chosen tables in one transaction; a failure rolls back and leaves the safety copy.
5. Dependencies are enforced the same way as in the list (restoring Notes text without Subjects is refused when the phone has no matching subject ids; the page says what to switch on).

## 8. What can go wrong, and the answer

| Risk | Answer |
|------|--------|
| Rows that point to a part that was left out | The dependency rules in section 3; merge skips rows whose parent is missing and says how many. |
| A new database version later | Parts map to tables by name in code, covered by a unit test that fails if a table has no part. |
| Restoring a newer package in an older app | Same refusal as today (`schemaVersion` and `formatVersion` checks). |
| Keys leaking | Keys only in an encrypted package; the page says so; never in presets. |
| Huge files | Streamed with the existing hashed copy; no part is held in memory. |
| Interrupted restore | Staged and verified first; apply is one transaction; safety copy kept. |

## 9. Build phases (one CI build each)

1. **Parts model and estimates**: `BackupPart`, dependency rules, size estimates, unit tests; page lists parts with sizes and switches (export still everything).
2. **Export with choice**: filtered database copy, file filter, manifest `parts`, format 2, summary; presets and the saved choice.
3. **Restore in part, Replace**: parts list on restore, staged part restore.
4. **Merge mode**.
5. **AI parts**: hub chats, hub settings, keys (encrypted only); then retire the hub's own backup pages so there is one backup (audit group c).
6. **Docs and privacy text**: what a file holds, in `docs/PRIVACY.md`.

Each phase ends with Gradle verified, the doc rows updated and the run id recorded.

## 10. Notes the design depends on

- Notes carry `deleted_at` (the Trash): trashed notes are a part of Notes, backed up and restored with their flag, so a restore never revives a trashed note.
- `Notes_Table`, `Guides_Table` and `Attachments_Table` have a `uuid`, which is what makes Merge possible; tables without one (`Attendance_Table`, `Exams_Table`, and so on) are merged by their natural key (subject and date) or, when none exists, only in Replace mode. Phase 4 lists them exactly once the schema is read table by table.
