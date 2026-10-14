# Absencium identity

These identifiers make an APK "the same app" to Android and to the user's data. They are stable. Do not change them
casually: any change needs an explicit migration plan written in this file first, and CI refuses to build if the
application ID or the database name moved.

| Identifier | Value | Where |
| --- | --- | --- |
| Application ID and namespace | `com.absencium.app` | `app/build.gradle.kts` |
| Launcher label | `Absencium` | `res/values/strings.xml` |
| Database file | `Absencium.db` | `AbsenciumDatabase.DATABASE_NAME` |
| Settings store | `absencium_settings` (DataStore) | `SettingsRepository` |
| Storage prefs | `absencium_storage` | `NotesStorageSettings` |
| API key vault | `absencium_key_vault`, Keystore alias `absencium_api_key` | `ApiKeyVault` |
| Signing key | alias `absencium`, see `SIGNING.md` | `app/signing/` or CI secrets |

Absencium is its own app. It is independent of the earlier "Trajectory" builds (different application ID, so both can be
installed side by side) and does not read their data.

## Upgrades

* `versionCode` is `1000 + the CI run number`, so every CI build is higher than the one before.
* The Room schema starts at version 1. Every change after that bumps `version` and adds a `Migration` in
  `AbsenciumDatabase`. `fallbackToDestructiveMigration` is never used, because it deletes the user's notes and attendance.
* Each version's schema is exported to `app/schemas/`. CI adds the file for a new version automatically and fails the
  build if a file for an existing version changed (the version was not bumped).
* Backups (`DatabaseBackup`) check the database version before restoring.

## If an identifier ever has to change

Write the plan here first: how the old data is carried over (export and import, or a one-off migration app), what
happens to installs that cannot be updated in place, and how users are told. Then change the identifier and update the
CI guard in the same commit.
