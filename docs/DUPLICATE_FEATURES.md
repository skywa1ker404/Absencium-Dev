# Duplicate features: one tool for one task (row 43.8)

Found by searching the app (`app`) and Absencium AI (`aihub`) on 2026-11-13. Nothing is changed before the owner decides each item (column "Decision"). GPL-3.0 applies to everything kept.

| # | Task done twice | Where | Decision |
|---|---|---|---|
| D1 | Cropping a picture | App `ui/vault/entry/PhotoCrop.kt` (free crop, templates) and the new Edit profile page; AI `ui/components/crop/` (`CropImageScreen`, `SquareCropImageScreen`, `CropBox`, `SquareCropBox`) | **Decided 2026-11-13:** the app's crop (`PhotoCrop`) becomes the only crop tool: add a square / avatar mode, then delete the AI's two crop screens and use it in the chat attach and avatars |
| D2 | Viewing a picture full screen (zoom) | AI `ImagePreviewDialog`, `ZoomableAsyncImage`; app shows note pictures through its own note blocks and attachment cards | **Decided 2026-11-13:** one image viewer (pinch zoom, swipe between pictures, share, open elsewhere) used by notes, Vault and the AI chat, with improved looks |
| D3 | Reading a PDF | App `PdfViewerDialog` (attachments), reading mode pages in `ReadingScreen`, `TextExtractor`; AI document chips and extraction | **Decided 2026-11-13:** one PDF viewer used by attachments, Vault and the AI chat, with improved looks (reading mode is part of it) |
| D4 | Dictating text (speech to text) | App `VoiceInputButton` (Android speech recogniser); AI speech engines (`asr` module: Sherpa on device, OpenAI-compatible, others) | **Decided 2026-11-13:** Android's speech recogniser everywhere; remove the AI's speech engines (on-device Sherpa, cloud ASR) and their settings, about 31 MB smaller. Text to speech is not part of this decision and stays until the owner says otherwise |
| D5 | Rendering Markdown | App `ui/vault/markdown` (notes, headings, `[[links]]`); AI `ui/components/richtext` (Markdown, LaTeX, Mermaid, code) | **Decided 2026-11-13:** keep both renderers (the editor needs its own live parser) and make them look identical: same fonts, spacing, colours, tables and code style |
| D6 | Audio: record and play | App `AudioRecorder` and the mini-player in reading / editor; AI text to speech players. No audio trimmer exists yet | **Decided 2026-11-13:** add a waveform audio trimmer (two handles, play the selection, save as new or replace) opened from any recording card or the mini-player, also for imported audio |
| D7 | Sharing and exporting | App `ShareHost`, share as PDF / Markdown; AI `ShareSheet` and chat export | **Decided 2026-11-13:** one share sheet and one set of formats (PDF, Markdown, text, image) for notes, chats and reports, in the app's look |
| D8 | Text fields | App `AppTextField`; AI `Input`, `DebouncedTextField` (row 40.1) | **Decided 2026-11-13:** the AI's look everywhere (one input style) |
| D9 | Switches, toggles, dialogs, toasts, swipe-to-delete | App components; AI `HapticSwitch`, `AppToast`, `PhysicsSwipeToDelete` (rows 40.2, 40.4, 40.5) | **Decided 2026-11-13:** the AI's components everywhere: haptic switch, chat-style toasts, physics swipe-to-delete, one dialog style (rows 40.1 to 40.5) |
| D10 | Drawing and markup | App `DoodleDialog`; AI image generation page (a different job: generation, not drawing); listed only to confirm they stay separate | **Decided 2026-11-13:** keep separate |

New tools to add once decided: **audio trimmer** (cut a recording or an imported audio file, with waveform), with the app's look.
