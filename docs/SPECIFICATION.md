NOTE: This is the product specification, copied from the original master specification.
The app is named Absencium and the database is Absencium.db (see docs/IDENTITY.md).
Everything else is as originally written. See docs/PROGRESS.md for what is built and the decisions made while implementing it.

================================================================================
ABSENCIUM APP - EXHAUSTIVE MASTER SPECIFICATION & BLUEPRINT
================================================================================

1. CORE PHILOSOPHY & ARCHITECTURE:
- 100% offline-first Android student companion app with zero cloud dependency, zero telemetry, and zero notification spam.
- Powered by a single unified local SQLite/Room database named `Absencium.db`.
- Adaptive progressive disclosure UI design where elements appear contextually and dynamically based on current user state.

2. FIRST-LAUNCH GATED ONBOARDING WIZARD:
- Mandatory multi-step onboarding gating wizard that must be completed before unlocking the main dashboard interface.
- Step 1 (Theme & Appearance Setup - Mandatory): Choose Theme Mode (Light Mode, Dark OLED Mode, System Auto) and Color Engine Scheme (Solid Accent with custom color picker, Dynamic Wallpaper Material You Monet, or Liquid Glass translucent acrylic blur).
- Step 2 (Notes Storage Directory - Mandatory): Choose between Default App-External Storage (`Android/data/[package_name]/files/notes`) or a Custom User-Selected Folder via Android Storage Access Framework (SAF).
- Step 3 (AI & Optional Features - Skippable): Input custom API key for Ask AI (skippable, configurable later in Settings) and toggle optional modules on/off.

3. ADAPTIVE UI & PROGRESSIVE DISCLOSURE RULES:
- Contextual Home Cards: Normal days show attendance gauge, next timetable slot, and daily recall prompt. Emergency Cram Banners auto-surface at the top of the dashboard when an exam is <= 48 hours away. Low-attendance warnings expand actionable insights only when approaching the custom threshold.
- Focus Mode Minimalist State: When the 10-Minute Study Timer or Focus Guard is active, non-essential widgets strip away for a clean writing workspace.
- Collapsible Vault & Drawers: Subject folders collapse when inactive; advanced tools (Ask AI, Exam Inspectors) open via smooth bottom sheets.

4. HOME SUMMARY DASHBOARD & SEARCH:
- Attendance % Gauge & Safe Margin: Live calculated attendance percentage paired with a Bunk Buffer safety margin forecasting calculation.
- Timetable "Next Up": Displays immediate upcoming lecture time, subject name, and classroom room number.
- Exam Countdowns: Dynamic countdown cards linked directly to scheduled calendar exams.
- 10-Minute Daily Study Timer: Manual start/pause control, auto-accumulates active focus time inside Notes Writing Mode, auto-pauses on app-switching to prevent false time inflation, and unlocks a daily study streak badge at >= 10 minutes of focus.
- Daily Recall Micro-Prompt UI & Behavior: Ultra non-intrusive home interface that defaults to a compact, collapsed card displaying only a small brain/sparkle question-mark icon paired side-by-side with your active Correct Answer Streak counter. Tapping the icon smoothly expands the card to reveal the question and answer input field.
- Daily Recall Spaced Repetition Rules: Uses the Sticky Lock Rule (question never changes or vanishes until answered regardless of days passed), a 7-day spaced repetition loop (correct answers schedule review for exactly 1 week later), a 🔁 Repeat Badge for resurfaced items, and a dual-card "1 of 2 questions" queue when multiple items are due.
- Global Fuzzy & OCR Search: Top-speed search bar spanning typed note text, handwritten page OCR text, and imported PDF titles.

5. NOTES VAULT & WRITING SYSTEM:
- Subject-Based Folders: Workspace categorization per subject (e.g., Pharmaceutics, Pharmacology).
- Self-Cleaning Subfolders: `📂 Finished / Written Notes` auto-archives notes marked complete. `📂 Guides & Question Banks` stores local PDFs for question banks, model answers, and syllabus guidelines.
- Notes Focus Guard (Distraction Interceptor): Toggled per session. Triggers after a configurable grace period (default: 5 minutes) when leaving notes. Displays strict system overlay with two exact options: `[ Return to Notes ]` or `[ Wait X More Mins ]`. Inactive when Focus Mode is OFF.
- Contextual "Ask AI" Assistant: Highlighting text inside notes reveals a native `[ ✨ Ask AI ]` action in the text-selection menu. Opens a non-intrusive bottom drawer over the note using an encrypted local API key. Automatically logs queries in Profile under AI Query History with a `[ 🔗 Jump to Source Note ]` deep-link opening the exact note and highlighting source text.
- Audio Lecture Memos: Direct local `.m4a` audio recordings tied to specific note entries with tap-to-add timestamp bookmarking.

6. CALENDAR, ATTENDANCE & HOLIDAY ENGINE:
- Daily Attendance Check-In: One-tap logging for Present, Absent, or Cancelled lectures.
- Weekend Auto-Skips: Automatic skipping for Saturdays and Sundays (configurable per schedule).
- Unified Database Holidays: Custom holiday dates, descriptions, and auto-skips stored directly inside `Holidays_Table` within `Absencium.db`.
- Integrated Exam System: Color-coded exam badges rendered on the monthly calendar grid. Tap-to-inspect detail drawer details exam topics, weightage, room maps, syllabus notes, and access to Emergency Cram Mode.
- Emergency Cram Mode: Overrides standard recall logic 24–48 hours before an exam to present a high-intensity review deck of unmastered questions, pinned formulas, and flagged diagrams.

7. PROFILE & ANALYTICS HUB:
- Attendance Logs & Safety Graphs: Detailed subject-by-subject attendance records.
- Recall & Study Streaks: Visual tracking for Daily Recall streaks and 10-Minute Study milestones.
- Recall History Log: Logs correct vs. incorrect micro-prompt attempts and identifies weak topic areas.
- AI Query History: Contextual AI interaction logs with direct deep-links back to source notes.
- Retrospective Post-Study Category Log: Quick 3-chip tag prompt (`[ 🧪 Lab Prep ]`, `[ 📚 Theory Reading ]`, `[ ✍️ Note Transcription ]`) following study timer completion for weekly visual analytics without pre-planning friction.

8. MATERIAL 3 EXPRESSIVE UI & DYNAMIC THEME ENGINE:
- Material 3 Expressive Motion: Spring-physics expanding FABs (`DampingRatioLowBouncy`) and sub-250ms haptic icon path-morphing.
- Theme Mode Selection: Light Mode (high contrast for daytime/outdoor campus), Dark Mode (pitch-black OLED / standard dark for night study), and System Auto (follows phone setting).
- Color Engine Schemes: Solid Accent (custom primary color picker), Dynamic Wallpaper (Android Material You Monet extraction), and Liquid Glass (translucent frosted acrylic surface blur effects via `RenderEffect`).

9. UNIFIED LOCAL DATABASE SCHEMA (`Absencium.db`):
- `Attendance_Table`: Subject IDs, date, status [Present/Absent/Cancelled], notes
- `Holidays_Table`: Holiday ID, date, title, auto_skip_flag
- `Exams_Table`: Exam ID, subject_ID, date, title, weightage, room, syllabus_notes
- `Timetable_Table`: Day of week, subject_ID, start_time, end_time, room_number
- `Notes_Table`: Note ID, subject_ID, title, body_text, status [Active/Finished], created_date
- `Guides_Table`: Guide ID, subject_ID, file_name, local_uri, guide_type [QP/Syllabus/AnswerBank]
- `Audio_Memos_Table`: Memo ID, note_ID, file_path, duration, timestamps_json
- `Recall_Quiz_Table`: Question ID, note_ID, prompt_text, correct_answer, last_answered_date, next_due_date, is_repeat [0/1], streak_count
- `AI_History_Table`: Log ID, note_ID, selected_text, ai_response, timestamp
- `User_Stats_Table`: Date, study_duration_minutes, streak_achieved [0/1], category_tag
- `Momentum_Log_Table`: Date, focus_quality_score, rolling_7day_consistency

10. ORGANIZED 4-SUBMENU SETTINGS HIERARCHY:
- General: Default Attendance Threshold (Custom slider: 60%–85%), Focus Guard Default Grace Timer (e.g., 5 minutes), Exam Mode Auto-Silence Toggle, Subject Muting & Active Status Manager.
- Appearance: Theme Mode Selection [Light / Dark OLED / System Auto], Color Engine Scheme [Solid / Dynamic Wallpaper / Liquid Glass], Custom Accent Color Picker.
- Storage & Data: Notes Storage Directory View & Modifier (Default App Files vs. Custom SAF Folder), Local Database Backup & Restore (`.zip`), [Optional] Semester Transition & Archive Manager, [Optional] Low Storage Media Compressor.
- AI & Integrations: Ask AI Master Toggle & Custom API Key Vault, [Optional] Momentum & Consistency Tracker Toggle.

11. OPTIONAL FEATURES MATRIX:
- Semester Transition & Archive Manager: Freezes active term logs and moves subjects into `📂 Semester Archives` (Default: OFF / Optional).
- Low Storage Media Compressor: Automatically compresses board photos and audio recordings to preserve storage (Default: OFF / Optional).
- Momentum & Consistency Tracker: Measures 7-day rolling study trajectory and focus quality to detect study drift early (Default: OFF / Optional).
================================================================================
