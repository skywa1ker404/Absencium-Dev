# Third-party notices

Absencium is GPL-3.0. Parts of it are ported from the projects below, which are licensed under the GNU Affero General Public License
version 3 (AGPL-3.0, text in `lastchat/LICENSE-AGPL-3.0`). GPL-3.0 section 13 and AGPL-3.0 section 13 allow the two to be combined; the
ported files stay under AGPL-3.0 and the AGPL network clause applies to the combined work.

## LastChat (https://github.com/Cocolalilal/LastChat), a fork of RikkaHub (https://github.com/rikkahub/rikkahub)

Copyright (c) the RikkaHub and LastChat authors.

| Where | Taken from | Changes |
| --- | --- | --- |
| `lastchat/shared/` | `shared/src/commonMain/kotlin` | none to the sources; built as a plain Android library instead of Kotlin Multiplatform (iOS code left out) |
| `lastchat/common/` | `common/src/main/java` | `AndroidPlatformJwtSigner.kt`: the PEM header literal is split in two so the repository's secret guard does not mistake it for a key; same build change |
| `lastchat/shared/` (`PlatformLog.kt`) | same file | debug and info lines are dropped because they carried the whole request (the student's questions and files) |
| `lastchat/ai/` | `ai/src/main/java` | `ProviderDispatcher.kt`: the expect / actual pair became one declaration (`Dispatchers.IO`); a comment in `ServiceAccountTokenProvider.kt` no longer spells a PEM header |
| `lastchat/search/` | `search/src/main/java`, `search/src/test/java` | `SearchDispatcher.kt`: the expect / actual pair became one declaration (`Dispatchers.IO`); built as a plain Android library. Only the keyless search is used by Absencium: for a question with web search on, its words go to the public sources named in `KeylessSearchService.backends` (Firecrawl keyless, DuckDuckGo, Qwant, Bing, Google News, Wikipedia, Open-Meteo, wttr.in and others); weather questions may also look up the approximate place of the connection. The paid-key services in the module are not reachable from the app |
| `lastchat/highlight/` | `highlight` | built with the repository's Gradle setup (compose plugin, minSdk 26); sources and the bundled `prism.js` unchanged |
| `lastchat/document/` | `document` | built with the repository's Gradle setup (minSdk 26); sources unchanged. Bundles MuPDF (AGPL-3.0, Artifex Software) as Java sources and `libmupdf_java.so` for arm64-v8a and x86_64 |
| `lastchat/ui-core/` | `ui-core/src/commonMain`, `commonTest` | Kotlin Multiplatform became a plain Android library; Compose Multiplatform resources (`google_sans_flex.ttf`, launcher foreground) became Android resources and their two call sites use `R.font` / `R.drawable`; the four preset colour schemes (Ocean, Spring, Autumn, Black) that its Gradle script generated from the app are committed as `GeneratedPresetColorSchemes.kt` |
| `lastchat/tts/` | `tts/src/main/java`, tests | Kotlin Multiplatform became a plain Android library (iOS sources left out; the expect / actual `TtsDispatcher` became one declaration); minSdk 28 |
| `lastchat/speech/` | `speech/src/main/java`, tests, `assets/sherpa_stt_catalog.json` | built with the repository's Gradle setup; the official sherpa-onnx AAR (Apache-2.0, k2-fsa) is downloaded at build time from its GitHub release and checked against a pinned SHA-256; speech models are downloaded only when the student asks for them |
| `lastchat/app/` | `app/src/main` (chat screen, input bar, attachments, pickers, settings, export, Koin start, own database `lastchat_ai`) | built as a plain Android library (minSdk 28) that Absencium opens as one screen; own manifest without launcher, share-sheet, assistant, notification-listener or widget entries. **Removed:** Firebase Crashlytics and Remote Config, update check, local web server and device discovery, WebDAV network sync, home-screen widgets, `workspace` and `local-llm` features. The model catalog refresh is a button the user presses (one request to raw.githubusercontent.com); nothing runs in the background. Uses JitPack only for the LaTeX renderer. |

Later ports (settings pages, chat screens, search) are listed here when they are added.
