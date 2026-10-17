# Pre-licence audit (GPL-3.0)

Date: 2026-10-16. Commit audited: `40bd035` plus the audit commits after it. This is an engineering review of evidence in
the repository and the upstream projects, not legal advice. No LICENSE file was created or changed.

## Readiness status: READY AFTER YOUR FIVE DECISIONS (updated 2026-10-17)

Nothing found makes GPL-3.0 impossible: every library in the release build declares Apache-2.0, MIT or BSD-3-Clause, and
no GPL-incompatible, proprietary or unlicensed component was found. The items that could be fixed in code, art and notices
have been fixed (table below). What remains cannot be settled from the repository; it needs a statement from you
("Decisions for you"). This is an engineering review of evidence, not legal advice. No LICENSE file was created or changed.

## What was inspected

- All tracked files: Kotlin sources (about 17,000 lines), resources, fonts, vector drawables, docs, CI workflows,
  Gradle files, wrapper, README, `.github` images and the committed Room schema.
- The full git history (authors, commit trailers, files that ever held keys or passwords).
- The Gradle dependency graph of the release build, with the licence read from each library's own POM:
  [DEPENDENCY_LICENSES.md](DEPENDENCY_LICENSES.md).
- The real LICENSE file of every directly used library and of the source of every colour scheme, the embedded licence
  metadata of the font files, and Google's Material icon repository.

## Findings

### Fixed

| Issue | Change |
| --- | --- |
| Notices missing for MaterialKolor, FileKit, Compose Destinations, icons, wrapper, colour schemes; Backdrop copyright wrong | `THIRD_PARTY_LICENSES.md` rewritten from each component's own files. |
| Fonts credited by link only | Upstream OFL texts added under `docs/licenses/`. |
| No dependency licence list | Manual "Dependency licence report" workflow and `DEPENDENCY_LICENSES.md`. |
| **16 icons with no traceable source** (CalendarMonth, Search, Contrast, Palette, Storage, AutoAwesome, Science, Tune, Person, Draft, Info, History, PlayCircle, PhotoCamera, Mic, Description) | Replaced with the exact path data of Google's `materialicons` set (Apache-2.0) from `google/material-design-icons` at commit `49d4db35df`. Draft now uses Material's `edit_note` because Material Icons has no "draft" glyph. All 32 icons now come from one named source. Shapes differ very slightly from before. |
| **`hero.png` and `showcase.png`**: AI-generated mock-ups with garbled text ("Adreranty", "Ocroper 2026"), no source or prompt in the repository, and claims the app did not meet ("Real screens", home-screen widgets) | Replaced with original illustrations drawn in HTML/CSS and rendered with Chromium (`docs/art/`). They use the app's own colours and the bundled OFL fonts, name only features that exist, and say they are illustrations, not screenshots. |
| **Obtainium badge**: Obtainium's repository (GPL-3.0) says nothing about reusing its badge; no permission could be found | Replaced with a plain text badge drawn for this project. The link to Obtainium's own redirect service is unchanged. |
| **Monokai Pro preset**: values came from an MIT neovim port of a commercial theme. Monokai's licence (monokai.pro/license) says "Monokai Pro may not be sub-licensed, resold, or redistributed", keeps all other rights, and covers derivative works containing it; the port's README shows no permission from Monokai | Preset removed (21 presets remain), with its notices. Anyone who had it selected falls back to the default preset. |
| **YouTube captions** (undocumented `youtubei/v1/player` endpoint sent with the YouTube Android app's client name and user agent) | Now off by default behind an explicit switch, "Read YouTube captions directly", in AI & Integrations, with a plain warning. See "YouTube captions" below. |
| CI published a `build-N` prerelease on every push | Removed; releases only through the manual `dev-release.yml` (Dev) and the tag-driven draft workflow (stable). |

### Still open

1. **The old signing keystores are public** (`app/signing/absencium-upgrade.jks` and its password in git history,
   `trajectory-debug.jks` in history). The build no longer uses them: Dev signs with a public throwaway key, stable only
   with GitHub secrets. They are still tracked, and removing them needs your approval (see `docs/SIGNING.md`).
2. **Contributor terms**: `docs/CONTRIBUTION.md` does not say under which licence contributions are accepted. Usual
   wording: contributions are licensed under the project's licence (GPL-3.0). I did not add it; it is a term you set.
3. **Licence notices inside the APK**: the app has no "Open-source licences" screen yet (Step 4, task 10 in `TODOList.md`).
4. **No source scanner was run** (ScanCode needs PyPI, which the sandbox cannot reach). Run ScanCode or FOSSA once on your
   own machine if you want that assurance.

### Decisions for you

1. **Authorship and AI-generated code.** 39 of the first 65 commits are authored as `Claude <noreply@anthropic.com>`,
   17 are yours through the GitHub editor and 8 are `skywa1ker404`. Whether AI-generated code is copyrightable, and who
   holds the copyright, is unsettled and differs by country. The README says AI tools were used. Decision: approve this
   wording for the README and licence header: "Written by the maintainer with AI assistance. Copyright (c) 2026 skywa1ker404
   to the extent copyright applies." (or give me yours).
2. **The app logo** (`ic_logo_a.xml` and the launcher layers): a traced vector of the "A" mark, committed on 2026-10-15
   from an image you supplied. The repository has no source file or record of who drew it or with what tool. Decision:
   confirm you made it, or that its creator or tool allows this use. If you cannot, say so and I will draw an original mark
   (a new launcher icon and splash) in its place.
3. **YouTube captions.** Decide whether the optional switch stays at all. Options: (a) keep it, off by default (current);
   (b) remove the unofficial fetch entirely and keep only Gemini's supported YouTube-link reading plus a "paste the
   transcript" box; (c) keep it on by default (not recommended).
4. **Monokai Pro.** Done (removed). If you hold a Monokai Pro licence or written permission to use its palette in a GPL
   app, tell me and I can restore it, but I recommend leaving it out.
5. **Anything you want credited differently** in `THIRD_PARTY_LICENSES.md` (for example your preferred copyright line).

### YouTube captions: terms and reliability

- What the code does: it posts to `https://www.youtube.com/youtubei/v1/player` claiming to be the YouTube Android app
  (`clientName` "ANDROID", the app's user agent) and downloads the caption file from the address that comes back. Its own
  comments call this unofficial.
- Terms: I could not read YouTube's main Terms of Service (the page refuses automated reading, and I did not work around
  that). The API Services Terms I did read say the developer may "only access (or attempt to access) the YouTube API
  Services to develop and operate your API Client(s)" (section 3.1) and grant no right to "make audiovisual content
  available in any manner other than through the use of the YouTube API Services" (16.3). The official captions download
  call of the Data API only works for the video's owner, so there is no official way for this app to read other people's
  captions. This is a terms question, not a copyright one; whether it breaches them for a user's own request is not
  something I can decide.
- Reliability: the client version (`20.10.38`) is hard-coded, and the endpoint and caption URLs can change or be refused
  at any time. Videos without captions have nothing to read. The app turns each failure into a plain message.
- Supported alternative already in the app: Gemini can read a public YouTube link directly (`file_data` in the Gemini API).
  With the switch off, that path works; other providers need the switch or a pasted transcript.

### Checked and fine

- GPL-3.0 compatibility of the dependency licences: Apache-2.0 "is compatible with version 3 of the GNU GPL" (and not with
  GPLv2), MIT/Expat and BSD-3-Clause are GPL-compatible, per the FSF licence list. Using GPL-3.0 for the project is
  therefore consistent with them. The only copyleft-style licence, the SIL OFL on two fonts, applies to the font files
  only (OFL FAQ Q1.2 and Q1.3); the FSF page describes it as a free copyleft licence for fonts but does not state GPL
  compatibility in the text read, so the fonts are best seen as separate bundled files. Keep their licence texts with them.
- The README already says the project is GPL-3.0 and links `LICENSE`; that link works once you add the file.
- No other licence declaration conflicts with it: no `LICENSE`, SPDX tags or headers exist in the source, and the only
  other licence notices are Gradle's own Apache-2.0 headers in `gradlew` and `gradlew.bat`.
- No API keys, tokens or private keys other than the keystore were found in the source or history.
- Build-only tools (Android Gradle Plugin, Kotlin compiler, KSP) are not distributed in the APK.

## Third-party components

190 libraries in the release build: see [DEPENDENCY_LICENSES.md](DEPENDENCY_LICENSES.md). By licence: Apache-2.0 (AndroidX,
Kotlin, kotlinx, JetBrains Compose, Room, Okio, Poko, JSpecify, Compose Destinations, Kyant0 Backdrop and Shapes),
MIT (MaterialKolor, ColorMath, FileKit), BSD-3-Clause (the protobuf copy inside DataStore). Fonts: Inter and Outfit (OFL-1.1).
Icons: Google Material icons (Apache-2.0). Colour schemes: see [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md).

## Verification

| Check | Result |
| --- | --- |
| `Android Build` on `40bd035` (debug and optimised builds) | passed |
| `Dependency licence report` workflow (190 libraries, POM licences) | passed; every library declares a licence |
| Upstream LICENSE files of 31 repositories | read; all MIT or Apache-2.0 (Gruvbox states MIT/X11 only in README/package.json) |
| Font metadata (Inter, Outfit) | OFL 1.1, copyright and licence strings present |
| Icon path data against Google's repository | all 32 now taken from it (16 replaced) |
| Gradle wrapper checksum against gradle.org | matches 8.14.x wrapper JAR |
| Unit tests | none exist in the repository |
| Source licence scanner (ScanCode and similar) | not run: no network to PyPI from the sandbox |
| Device testing | not done |

Nothing here means the project is legally cleared; it means no known blocker was found in the evidence listed above.

## What you must do

1. Make the decisions above (at least 1, 2 and 3).
2. Approve removing the old keystores from the tree (docs/SIGNING.md explains what that does and does not fix).
3. Test the Dev APK, then approve 1.0.0. Only then add the official GPL-3.0 text as `LICENSE` through GitHub (Add file,
   Create new file, name it `LICENSE`, then "Choose a license template" and pick GNU General Public License v3.0).
