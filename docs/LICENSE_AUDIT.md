# Pre-licence audit (GPL-3.0)

Date: 2026-10-16. Commit audited: `40bd035` plus the audit commits after it. This is an engineering review of evidence in
the repository and the upstream projects, not legal advice. No LICENSE file was created or changed.

## Readiness status: NOT YET CLEARED; THREE DECISIONS LEFT (updated 2026-10-17)

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
| **YouTube captions** (undocumented `youtubei/v1/player` endpoint sent with the YouTube Android app's client name and user agent) | Removed entirely (your option b, 2026-10-17): no caption fetch, no unofficial YouTube access, no setting. The app still reads a video's public title, channel and thumbnail through YouTube's documented oEmbed endpoint. Notes come from Gemini reading a public link (Gemini API `file_data`) or from a transcript pasted into a new box; other providers need the pasted transcript. |
| **Old signing keystores in the tree** | `app/signing/absencium-upgrade.jks` removed (the earlier `trajectory-debug.jks` was already only in history), `.gitignore` blocks keystores, no password for either remains in the tree, no other keystore copy found (scan of `.jks .keystore .p12 .pfx .pem` files and key-like text). Git history is untouched. Plan for friends' APKs in `docs/SIGNING.md`. |
| **Authorship wording** | Your approved sentence is in the README FAQ ("Who's making Absencium?") and the README licence section, stated as a notice that does not replace the GPL-3.0 text. |
| CI published a `build-N` prerelease on every push | Removed; releases only through the manual `dev-release.yml` (Dev) and the tag-driven draft workflow (stable). |

### Still open

1. **Old keys remain in Git history** (not rewritten, by your decision). They are public for good; nothing signs with them.
   History cleanup needs a separate approval from you and breaks every clone, so it is not recommended once nothing trusts them.
2. **Contributor terms** (`docs/CONTRIBUTION.md`): unchanged until you approve exact wording (proposal below).
3. **Licence notices inside the APK**: no "Open-source licences" screen yet (`TODOList.md`, Step 4, task 10).
4. **The source-licence scanner could not run.** ScanCode (and similar tools) are installed from PyPI, and the sandbox cannot
   reach PyPI, so no automated scan of the Kotlin sources for copied code or foreign licence headers was done. What was done
   instead is manual and limited: a text search for copyright headers, "adapted from" notes and pasted URLs, which found none,
   plus the dependency and notice checks in the Verification table. This is **not** a complete verification. Running ScanCode
   once on your own computer would close the gap.
5. **Logo**: see below.

### Logo

- **What you told me:** you generated the logo using GPT (ChatGPT/OpenAI). I have no file, prompt, date or tool record of it, so
  this is recorded as your statement, not as something I verified.
- **What the repository shows** (only what can be observed): `app/src/main/res/drawable/ic_logo_a.xml` and the launcher
  foreground and monochrome layers are Android vector drawables made of straight line segments with two-decimal coordinates. They
  were committed on 2026-10-15 in `acb29f8` under your name. I do not know how the image became this vector; the earlier wording
  in this file ("traced vector") described my assumption, not a recorded fact.
- **OpenAI's terms** (https://openai.com/policies/terms-of-use/, read 2026-10-17): "you (a) retain your ownership rights in Input
  and (b) own the Output", with OpenAI assigning "all our right, title, and interest, if any" in the Output, "to the extent
  permitted by applicable law". They also say "output may not be unique and other users may receive similar output", and that the
  assignment "does not extend to other users' output or any Third Party Output".
- **Assessment:** keeping the logo is reasonable on that basis, with three limits. (1) Whether a purely AI-generated image is
  protected by copyright at all is unsettled and differs by country, so "you own it" may mean little in practice. (2) Similar
  images may exist; I cannot run a reverse image search from here, so I cannot rule out a resemblance to someone else's mark.
  (3) The vector file has no provenance record. None of this shows a problem; the position is "likely fine, not proven".
- **Proposal, not applied:** an original replacement mark drawn from basic shapes (two rounded strokes forming an "A" with a tick
  as its crossbar, in the Default Theme's green and yellow on its dark background; the preview was sent to you separately). If you
  approve it I will build the launcher icon layers, the in-app mark and the splash from it. If you keep the current logo, the only
  change needed is a note in the README and `THIRD_PARTY_LICENSES.md` stating your account of how it was made.
- Separately from copyright, you may want to say that the name and logo are not licensed for reuse under the GPL (a
  trademark-style statement). That is your choice; nothing was added.

### Contributor licence: what the proposed sentence would mean

Proposed wording, for you to approve or change (nothing in `CONTRIBUTION.md` has been edited):

> By submitting a contribution you agree that it is licensed under the same GNU General Public License v3.0 as the project, and
> that you have the right to submit it.

In plain language:
- **Contributors keep their copyright.** There is no copyright assignment and no transfer of ownership to the maintainer.
- **No extra permissions.** The contributor grants what the GPL-3.0 already grants everyone (use, study, change and share under the
  same terms). It does not let you relicense their work under a different licence, sell it under a closed licence, or put it in a
  proprietary version.
- **What it adds:** an explicit statement that the contribution comes under GPL-3.0 and that the contributor has the right to
  submit it (it is theirs, or they may share it). GitHub's terms already say contributions to a public repository come under the
  repository's licence, so this mainly makes that explicit.
- **Trade-off:** because contributors keep their copyright, moving the whole project to another licence later, or adding a
  non-GPL version, would need every contributor's agreement. Keeping that freedom needs a Contributor Licence Agreement or an
  assignment, which is heavier and discourages small contributions. I would not recommend it for a small student project.
- A lighter alternative many projects use is the Developer Certificate of Origin (each commit is signed off with `Signed-off-by:`),
  which also leaves ownership with the contributor. I am not a lawyer; this describes how such sentences usually work.

### YouTube captions: terms and reliability (resolved by removal)

- What the removed code did: it posted to `https://www.youtube.com/youtubei/v1/player` claiming to be the YouTube Android app
  (`clientName` "ANDROID", the app's user agent) and downloaded the caption file from the address that came back. Its own comments
  called this unofficial.
- Terms: I could not read YouTube's main Terms of Service (the page refuses automated reading, and I did not work around that).
  The API Services Terms I did read say the developer may "only access (or attempt to access) the YouTube API Services to develop
  and operate your API Client(s)" (section 3.1) and grant no right to "make audiovisual content available in any manner other than
  through the use of the YouTube API Services" (16.3). The official captions download of the Data API only works for the video's
  owner, so there was no official way to read other people's captions. Whether the old code breached the terms was not something I
  could decide; removing it avoids the question.
- Reliability (why it was fragile): a hard-coded client version, an endpoint and caption addresses that YouTube can change or
  refuse at any time, and videos without captions.
- What remains: the documented oEmbed endpoint (public title, channel, thumbnail), Gemini's supported YouTube-link reading, and a
  pasted transcript for any provider. Other AI features (photo text, Ask AI, model lists) are untouched.

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
| Source licence scanner (ScanCode and similar) | **not run**: the sandbox cannot reach PyPI. Verification is therefore incomplete; see "Still open", item 4 |
| Keystore scan of the current tree | only the public Dev key remains (checked 2026-10-17) |
| OpenAI terms for the logo | read; assignment "if any" and "to the extent permitted by applicable law" |
| Monokai Pro licence | read (monokai.pro/license); redistribution prohibited, so removed |
| Device testing | not done |

Nothing here means the project is legally cleared; it means no known blocker was found in the evidence listed above.

## What you must do

1. Decide the logo (keep with a note, or approve the original replacement) and approve or edit the contributor sentence.
2. Run a source licence scanner once on your own computer if you want that assurance; I could not.
3. Test the Dev APK, then approve 1.0.0. Only then add the official GPL-3.0 text as `LICENSE` through GitHub (Add file, Create new
   file, name it `LICENSE`, then "Choose a license template" and pick GNU General Public License v3.0).
