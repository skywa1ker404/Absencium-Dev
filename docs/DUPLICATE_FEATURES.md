# Duplicate features: one tool for one task (row 43.8)

Found by searching the app (`app`) and Absencium AI (`aihub`) on 2026-11-13. Nothing is changed before the owner decides each item (column "Decision"). GPL-3.0 applies to everything kept.

| # | Task done twice | Where | Decision |
|---|---|---|---|
| D1 | Cropping a picture | App `ui/vault/entry/PhotoCrop.kt` (free crop, templates) and the new Edit profile page; AI `ui/components/crop/` (`CropImageScreen`, `SquareCropImageScreen`, `CropBox`, `SquareCropBox`) | open |
| D2 | Viewing a picture full screen (zoom) | AI `ImagePreviewDialog`, `ZoomableAsyncImage`; app shows note pictures through its own note blocks and attachment cards | open |
| D3 | Reading a PDF | App `PdfViewerDialog` (attachments), reading mode pages in `ReadingScreen`, `TextExtractor`; AI document chips and extraction | open |
| D4 | Dictating text (speech to text) | App `VoiceInputButton` (Android speech recogniser); AI speech engines (`asr` module: Sherpa on device, OpenAI-compatible, others) | open |
| D5 | Rendering Markdown | App `ui/vault/markdown` (notes, headings, `[[links]]`); AI `ui/components/richtext` (Markdown, LaTeX, Mermaid, code) | open |
| D6 | Audio: record and play | App `AudioRecorder` and the mini-player in reading / editor; AI text to speech players. No audio trimmer exists yet | open |
| D7 | Sharing and exporting | App `ShareHost`, share as PDF / Markdown; AI `ShareSheet` and chat export | open |
| D8 | Text fields | App `AppTextField`; AI `Input`, `DebouncedTextField` (row 40.1) | open |
| D9 | Switches, toggles, dialogs, toasts, swipe-to-delete | App components; AI `HapticSwitch`, `AppToast`, `PhysicsSwipeToDelete` (rows 40.2, 40.4, 40.5) | open |
| D10 | Drawing and markup | App `DoodleDialog`; AI image generation page (a different job: generation, not drawing); listed only to confirm they stay separate | open |

New tools to add once decided: **audio trimmer** (cut a recording or an imported audio file, with waveform), with the app's look.
