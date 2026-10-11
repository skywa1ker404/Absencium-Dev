# Trajectory

An offline-first Android companion for students. Trajectory keeps attendance, timetable, exams, notes, and spaced-repetition recall in a single local app, with no account, no cloud service, and no telemetry.

**Status:** early development. The foundation (logging, local database, notes storage) is built. The user-facing screens are not built yet. See [Project status](#project-status).

## Contents

- [Design principles](#design-principles)
- [Planned features](#planned-features)
- [Project status](#project-status)
- [Technology](#technology)
- [Compatibility](#compatibility)
- [Project structure](#project-structure)
- [Data and storage](#data-and-storage)
- [Building](#building)
- [Debug logging](#debug-logging)
- [Documentation](#documentation)
- [License](#license)

## Design principles

- **Offline first.** Everything works without a network connection. There is no cloud dependency and no telemetry.
- **Local ownership of data.** All records live in one local database. Notes and media live in a folder the user chooses.
- **Quiet by default.** No notification spam. Interface elements appear only when they are relevant, such as exam banners within 48 hours of an exam or attendance warnings near the chosen threshold.
- **Optional extras stay optional.** The Ask AI assistant uses the user's own API key and can be skipped. The app is fully usable without it.

## Planned features

| Area | Highlights |
| --- | --- |
| Onboarding | A required first-launch wizard for theme, color scheme, and notes storage location. A skippable step for the optional AI key and optional modules. |
| Home dashboard | Attendance gauge with a safe-margin forecast, next lecture, exam countdowns, a 10-minute daily study timer, a compact daily recall card, and global search across notes, handwritten pages, and PDF titles. |
| Notes vault | Subject folders, a Finished archive, a Guides and Question Banks folder for PDFs, audio lecture memos with timestamp bookmarks, a focus guard that discourages leaving notes mid-session, and a contextual Ask AI drawer. |
| Calendar and attendance | One-tap present, absent, or cancelled logging. Weekend and holiday auto-skip. Exam badges on a monthly grid with a detail drawer and an Emergency Cram Mode. |
| Profile | Per-subject attendance records and safety graphs, study and recall streaks, recall history, AI query history with links back to source notes, and a post-study category log. |
| Appearance | Light, dark OLED, and system-following themes. Solid accent, Material You wallpaper colors, or a translucent blur surface style. |
| Settings | Four sections: General, Appearance, Storage and Data, and AI and Integrations. |
| Optional modules | Semester archive, low-storage media compressor, and a momentum and consistency tracker. All off by default. |

The complete product specification is in [docs/SPECIFICATION.md](docs/SPECIFICATION.md).

## Project status

| Module | Status |
| --- | --- |
| Project setup and continuous integration | Done |
| Debug file logger | Done |
| Local database (`Trajectory.db`) | Done |
| Notes storage layer (app files or a chosen folder) | Done |
| Onboarding wizard and Settings hub | Not started |
| Home dashboard | Not started |
| Notes vault | Not started |
| Calendar and attendance | Not started |
| Profile hub | Not started |
| Optional modules | Not started |

Detailed status, decisions, and next steps are in [docs/PROGRESS.md](docs/PROGRESS.md).

## Technology

| Component | Version |
| --- | --- |
| Language | Kotlin 2.4.10 |
| UI | Jetpack Compose (UI and Foundation 1.12.1) with Material 3 1.4.0 |
| Local database | Room 2.8.5, with KSP 2.3.11 |
| Asynchronous work | kotlinx.coroutines 1.11.0 |
| Folder access | AndroidX DocumentFile 1.1.0 (Storage Access Framework) |
| Build | Android Gradle Plugin 9.4.0, Gradle 9.8.1, JDK 17 |

## Compatibility

- Minimum supported version: Android 8.0 (API 26).
- Compile SDK 37, target SDK 36.
- Material You wallpaper colors and the blur surface style require Android 12 (API 31) or higher. They are planned to be shown as unavailable on earlier versions.

## Project structure

```
app/src/main/java/com/trajectory/vault/
  TrajectoryApp.kt          Application entry point; starts the logger and startup checks
  MainActivity.kt           Single activity hosting the Compose UI
  logging/
    DebugLogger.kt          File logger, crash capture, lifecycle logging
  data/
    db/
      Entities.kt           Table definitions
      Daos.kt               Queries
      Converters.kt         Date and time converters
      TrajectoryDatabase.kt Database class and open verification
    storage/
      NotesStorageSettings.kt   Remembers the storage choice and folder permission
      NotesStorage.kt           Resolves the notes root folder
      StorageFolder.kt          Folder abstraction for app files and chosen folders
docs/
  SPECIFICATION.md          Product specification
  PROGRESS.md               Status, decisions, and how to resume work
.github/workflows/
  android.yml               Continuous integration build
```

## Data and storage

### Database

A single Room database named `Trajectory.db` holds all structured data:

`Subjects_Table`, `Attendance_Table`, `Holidays_Table`, `Exams_Table`, `Timetable_Table`, `Notes_Table`, `Guides_Table`, `Audio_Memos_Table`, `Recall_Quiz_Table`, `AI_History_Table`, `User_Stats_Table`, `Momentum_Log_Table`

Dates and times are stored as ISO-8601 text. A subject that still has linked records cannot be deleted. Audio memos and recall questions are removed together with their note. AI history entries are kept if their source note is deleted.

### Notes storage

The user chooses one of two locations during onboarding:

- **App files (default):** `Android/data/com.trajectory.vault/files/notes`. No permission is required.
- **A chosen folder:** selected through the Android system folder picker. Read and write access is stored so it survives restarts.

## Building

### Continuous integration

Every push to `main` runs the workflow in `.github/workflows/android.yml`. It builds the debug APK with `./gradlew assembleDebug` and uploads it as the `trajectory-debug-apk` artifact on the workflow run. If the build fails, the last 200 lines of Gradle output are posted as a comment on the commit.

### Local build

Requirements:

- JDK 17
- Android SDK with platform 37 and build tools 36.0.0

```
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Debug logging

The app writes a running log to:

```
Android/data/com.trajectory.vault/files/debug.log
```

The log records unhandled exceptions with full stack traces, activity lifecycle events, database open and migration results, and the notes storage state. When the file passes 2 MB it is moved to `debug.log.1` at the next launch and a fresh file is started.

On Android 11 and later, most file managers cannot open the `Android/data` folder. Use `adb pull` to retrieve the file:

```
adb pull /sdcard/Android/data/com.trajectory.vault/files/debug.log
```

## Documentation

- [Product specification](docs/SPECIFICATION.md)
- [Progress and resume guide](docs/PROGRESS.md)

## License

No license has been specified yet.
