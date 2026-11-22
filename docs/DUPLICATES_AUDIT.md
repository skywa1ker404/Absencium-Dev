# Duplicates audit (45.36)

Scope: copied composables and repeated components in the app UI and the AI hub. Decided duplicate features are in `DUPLICATE_FEATURES.md` (D1-D10) and duplicate settings in `SETTINGS_AUDIT.md`; they are not repeated here. Method: grep of every composable name and of repeated shape/colour patterns, then reading the candidates. Nothing is removed without keeping its capability. One item per CI build.

## App UI

| # | Duplicate | Where | One home | Risk |
|---|-----------|-------|----------|------|
| A1 (done, CI `44983609146`) | Three small pills: `InfoPill`, `CompactPill`, `StatPill` | `profile/ProfileScreen.kt:246` (clickable, labelLarge, 14x6dp), `home/HomeTab.kt:426` (labelSmall, 9x4dp), `components/Tiles.kt:40` (labelLarge, 12x7dp, glass) | `StatPill` with a `compact` flag and an optional `onClick`; `InfoPill` and `CompactPill` call it | Low. Glass style would now reach the two copies, which is wanted |
| A2 | Choice card with icon, title and body | `vault/AttachmentUi.kt:206` `ChoiceCard`, `onboarding/OnboardingScreen.kt:507` `OptionTile` | `AppCard(highlighted = ...)` for both; keep each one's content layout | Low |
| A3 | "One text field + Save/Cancel" dialog written six times | `syllabus/SyllabusScreen.kt:492` `TextDialog`, `vault/SubjectFolderScreen.kt:786`, `vault/MarkdownUi.kt`, `settings/SubjectsScreen.kt`, `profile/EditProfileScreen.kt`, `calendar/EntrySheets.kt` | `TextInputDialog(title, label, initial, multiline, allowBlank, onSave)` in `components/AppDialog.kt` | Medium: each has small differences (validation, extra buttons); only the plain ones move |
| A4 | Section header text | `components/Components.kt:302` `SectionTitle`, `profile/ProfileScreen.kt:262` `SectionLink` (title with arrow), `settings/AiSettingsScreen.kt:157` `AdvancedHeader`, `onboarding/OnboardingScreen.kt:316` `PageHeader` | `SectionTitle` for plain titles; `SectionLink` stays because it is a different control; `AdvancedHeader` reuses `SectionTitle` | Low |
| A5 (done, CI `44986880186`) | Pill background written by hand (`clip(pillShape()).background(...)`) 97 times in 25 files | `vault/AttachmentUi.kt` (10), `study/PlannerScreen.kt` (8), `calendar/TimetableDay.kt` (8), `vault/DoodleDialog.kt` (7), `main/ShellBars.kt` (7) | A `Modifier.pill(color)` helper so the glass and Pixelated looks apply from one place | Low, but many files; do it with A1 |
| A6 | `ChoiceTile` (Appearance) | `settings/AppearanceSettingsScreen.kt:298` | Keep: a picture tile, not a card | None |
| A7 | `PickedChip` (Syllabus) | `syllabus/SyllabusScreen.kt:694` | Keep: removable row, no equivalent in shared code; may move to `components` if used elsewhere | None |
| A8 | `AlertDialog` | `components/AppDialog.kt:71` | Already one wrapper (no change); `BlurBehindDialog` is private to it | None |

## AI hub

| # | Duplicate | Where | One home | Risk |
|---|-----------|-------|----------|------|
| H1 | Two `SectionHeader` | `ui/components/message/ContextSourcesSheet.kt:140` (labelLarge), `ui/pages/setting/localstt/SettingLocalSttPage.kt:270` (titleSmall) | One `SectionHeader` in `ui/components/ui` | Low |
| H2 | Two `MemoryDebugger` (same job: type a query, see what is retrieved) | `ui/pages/assistant/detail/AssistantMemorySubPage.kt:1308`, `AssistantRagMemorySubPage.kt:262` | One component taking the results as a list; differ in surface, text field and result type | Medium: different state types, so pass a small result model |
| H3 | Two `MemoryItem` | `ContextSourcesSheet.kt:150`, `AssistantMemorySubPage.kt:1138` | Keep separate unless the row layout turns out the same after reading | Unknown |
| H4 | Text fields: `TextField` and `OutlinedTextField` mixed across hub pages | the two `MemoryDebugger` copies and others | Use the host's `AppTextField` through `HubStyle` | Medium |

## Order of merging (one per CI build)
1. A1 + A5 (pills), 2. A3 (text dialogs), 3. A2 + A4, 4. H1 + H2 + H4.

## Not checked yet
Same-destination navigation entry points (two buttons to one page) and the hub's duplicate routes.
