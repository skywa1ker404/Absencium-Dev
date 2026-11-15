# Settings audit (2026-11-15, owner: "many duplicate settings and many missing settings")

Method: every settings screen of the app (`ui/settings`) and of the AI hub (`aihub/app/.../ui/pages/setting`) was listed and compared; stored settings (`AppSettings`) were checked for a screen and a setter. This file is the working list: nothing here is removed until the owner has picked a side.

## Duplicates (the same choice in two places)

| # | In the app | In the AI hub | Proposed single place |
|---|------------|---------------|-----------------------|
| 1 | Appearance > Display size (whole-app size) | Display > UI customisation > Font size, per-assistant font size ratio, Fonts page (size, width, letter spacing, roundness) | App Appearance for size and style; the hub keeps only chat-only options (code font, message width) |
| 2 | Appearance > Liquid Glass blur and Visual quality | Display > UI customisation > Enable blur effect | App Appearance (one switch for both) |
| 3 | Appearance > Colour / Presets / Monet | Per-assistant "Material You colours" and colour index | App Appearance; hub assistants follow the app theme |
| 4 | AI > Set up (provider, address, model) | Providers and Models pages | Hub Providers and Models; the app page becomes a link to them |
| 5 | Storage > Backup and restore | Chat storage and the hub's own backup page | App Storage (one backup that includes hub chats), hub keeps only "clear cache" |
| 6 | Reminders (lectures, exams, attendance) | Display > Notification when a message is generated, spontaneous messages | Keep both (different things), but group all notifications under one "Notifications" page |
| 7 | Settings hub > Developer | Display > Developer mode, About | One Developer page |
| 8 | Profile shape (Appearance > Avatars) | Per-assistant avatar shapes | App value only; hidden in Pixelated and Dot matrix (done 2026-11-15) |

## Stored but not shown, or shown without a setter

- `aiApiStyle` has no screen (the AI > Set up page picks it from the provider); decide whether to show it under Advanced.
- `introSeen`, `onboardingComplete`: internal flags, correct to leave out.
- `cornerRadiusDp`, `glassBlurDp`: shown only as sliders inside Appearance; there is no reset to default.
- `lastBackupAt`, `lastBackupNote`: shown as text only.
- Removed 2026-11-15: attendance target on the Profile (the 75% rule stays in Settings > General > Attendance and in onboarding).

## Missing settings (candidates, to confirm with the owner)

- A Pixelated/Dot matrix intensity (icon grid size, pixel step size).
- Bottom bar: auto-hide on scroll for Anchored (row 45.21).
- AI: a maximum tool set per request and a "light mode" (row 45.29) so cost is visible and adjustable.
- AI actions: keep history for N days / clear all (the folder only deletes one chat at a time).
- Haptics: one switch for the whole app, with the hub's generation haptics as a child.
- Reduce motion switch separate from Visual quality.

Status: open, see TODOList row 45.28.
