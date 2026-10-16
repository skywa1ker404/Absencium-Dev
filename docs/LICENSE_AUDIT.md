# Pre-licence audit (GPL-3.0)

Date: 2026-10-16. Commit audited: `40bd035` plus the audit commits after it. This is an engineering review of evidence in
the repository and the upstream projects, not legal advice. No LICENSE file was created or changed.

## Readiness status: READY AFTER MANUAL REVIEW

Nothing found makes GPL-3.0 impossible: every library in the release build declares Apache-2.0, MIT or BSD-3-Clause, and
no GPL-incompatible, proprietary or unlicensed component was found. The items under "Needs your decision" cannot be
settled from the repository and should be looked at before the first public release. One security problem (the public
signing key) is separate from licensing but must be fixed before anything is distributed.

## What was inspected

- All 130 tracked files: 85 Kotlin sources (about 17,000 lines), resources, fonts, vector drawables, docs, CI workflow,
  Gradle files, wrapper, README, `.github` images and the committed Room schema.
- The full git history (authors, commit trailers, files that ever held keys or passwords).
- The Gradle dependency graph of the release build (`releaseRuntimeClasspath`), resolved on CI, with the licence read from
  each library's own POM: [DEPENDENCY_LICENSES.md](DEPENDENCY_LICENSES.md).
- The real LICENSE file of every directly used library and of the source of every colour scheme (shallow clones of the
  upstream repositories), the embedded licence metadata of the font files, and Google's Material icon repository.

## Findings

### Fixed in this audit

| Issue | Change |
| --- | --- |
| `THIRD_PARTY_LICENSES.md` credited only Inter, Outfit and Backdrop. MaterialKolor, FileKit, Compose Destinations, the Material-style icons, the Gradle wrapper and all colour schemes were missing. | Rewritten with each licence and copyright line read from the component's own files. |
| Backdrop was credited as "Copyright Kyant0". Its LICENSE says "Copyright 2025 Kyant". | Corrected. |
| The Presets header comment named five MIT sources loosely and no licences. | Comment now lists the exact repositories; notices are in `docs/licenses/MIT-components.txt`. |
| The fonts were credited with a link only. The OFL FAQ (Q1.20) says a mobile app should include the copyright statement, licence notice and licence text. | Upstream licence files added unchanged under `docs/licenses/`. |
| Apache-2.0 attribution for the Material-style icons was missing. | Credited; Google's LICENSE file added. |
| No machine-readable dependency licence list existed. | Added the manual `Dependency licence report` workflow and its output, `DEPENDENCY_LICENSES.md`. |

### Confirmed and still open

1. **The signing keystore and its password are public.** The repository is public (GitHub API: `private: false`).
   `app/signing/absencium-upgrade.jks` is tracked, and its password is written in `app/build.gradle.kts`. An older debug
   keystore, `app/signing/trajectory-debug.jks`, with the password `trajectory-debug`, is still in git history. Anyone can
   build an APK that Android accepts as an update to Absencium. Not a licence issue, but a blocker for any release.
   Next step: generate a new key, store it as the four `ABSENCIUM_*` GitHub secrets (docs/SIGNING.md), then remove the
   key and the password from the repository. The key cannot be cleaned from history without rewriting it. Because the
   key is public, treat it as burned and do not publish releases signed with it.
2. **CI publishes a release on every push to `main`.** `.github/workflows/android.yml` creates a `build-N` prerelease with
   debug and optimised APKs (currently `build-38` and later). On a public repository these are public downloads. All audit
   commits were pushed with `[skip ci]` or touched only docs, so the audit created none, but the feature commit
   `40bd035` that preceded it did publish one automatically.

### Needs your decision (cannot be verified from here)

1. **Authorship and AI-generated code.** 39 of the 65 commits up to `40bd035` are authored as `Claude <noreply@anthropic.com>` and carry
   `Co-Authored-By: Claude` and a session link in their messages; 17 are yours through the GitHub web editor and 8 are
   authored as `skywa1ker404`. The README states that AI tools were used. Whether AI-generated code is copyrightable,
   and who holds the copyright for the parts, is unsettled law and varies by country, so the repository cannot honestly
   assert more than "written by the maintainer with AI assistance". The audit also found no copied code (no foreign
   copyright headers, no "adapted from" notices, no pasted URLs beyond API endpoints), but a text search cannot rule out
   that generated code resembles some existing project. No ScanCode-style scanner could be run: the sandbox cannot reach
   PyPI. If this matters to you, run ScanCode or FOSSA once on your own machine.
2. **Icons.** `ui/icons/AppIcons.kt` has 32 icons. The path data of 16 is identical (same coordinates) to Google's Material icons
   (Apache-2.0), now credited. The other 16 (CalendarMonth, Search, Contrast, Palette, Storage, AutoAwesome, Science, Tune, Person,
   Draft, Info, History, PlayCircle, PhotoCamera, Mic, Description) are Material-style but match no icon in that
   repository, so their origin is not established. Options: keep them (they are simple pictograms, but I cannot prove
   their origin), or redraw them yourself. Nothing was deleted.
3. **App logo and the three `.github` images.** `ic_logo_a.xml` and the launcher icons are a traced vector of the
   "A" mark. `hero.png` and `showcase.png` look like AI-generated mock-ups (several garbled words such as "Presences",
   and the showcase says "Real screens" though they are not real screenshots). The repository holds no source file,
   prompt or tool record for them. You need to confirm you created them and that the tool you used allows this use.
   `badge_obtainium.png` is Obtainium's "Get it on" badge, which belongs to that project; check its repository for the
   terms of reuse (I could not verify them) or replace it with a plain text link.
4. **Monokai Pro preset.** Its values come from `loctvl842/monokai-pro.nvim` (MIT), a port of the commercial Monokai Pro
   theme. The MIT licence covers that port's code; whether the commercial theme's owner allows reuse of its palette is not
   something I could check. Options: keep, or remove or rename the preset. The same question applies, more weakly, to the
   other schemes: a colour list is facts, not code, and every source repository is MIT or Apache-2.0.
5. **Poimandres source.** The neovim port the values were first read from, `olivercederborg/poimandres.nvim`, has no
   LICENSE file. The same hex values do appear in the original, `drcmda/poimandres-theme` (MIT), which is now the credited
   source, so this is resolved unless you disagree.
6. **YouTube captions.** `data/ai/YouTube.kt` calls YouTube's undocumented app endpoint (`youtubei/v1/player`) with an
   Android client name, version and user agent. That is a Terms-of-Service question, not a copyright one, and the same
   technique is used in other open-source projects, so I cannot say where the approach originated. The code itself is
   short and its comments say the endpoint is unofficial.
7. **Licence notices inside the APK.** Apache-2.0, MIT and OFL ask that recipients get the licence text and notices. The
   source repository now has them, but the installed app has no "Open-source licences" screen. This is a small code
   change (an About entry that shows `THIRD_PARTY_LICENSES.md`). It was not made here because any code push publishes a
   CI prerelease; say if you want it.
8. **Contributor terms.** `docs/CONTRIBUTION.md` does not say under which licence contributions are accepted. The usual
   wording is that contributions are licensed under the project's licence (GPL-3.0). I did not add it, since that is a
   term you set.
9. **Gradle wrapper.** `gradle-wrapper.jar` matches Gradle's published wrapper checksum for 8.14.x, so it is genuine, while
   the build downloads Gradle 9.8.1. This works. Running `./gradlew wrapper --gradle-version 9.8.1` would align them and
   `distributionSha256Sum` can pin the download.

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
| Icon path data against Google's repository | 16 of 32 identical |
| Gradle wrapper checksum against gradle.org | matches 8.14.x wrapper JAR |
| Unit tests | none exist in the repository |
| Source licence scanner (ScanCode and similar) | not run: no network to PyPI from the sandbox |
| Device testing | not done |

Nothing here means the project is legally cleared; it means no known blocker was found in the evidence listed above.

## What you must do

1. Replace the signing key (finding 1) before publishing anything.
2. Decide the items under "Needs your decision", at least 1, 3 and 4.
3. Add the official GPL-3.0 text as `LICENSE` through GitHub (Add file, Create new file, name it `LICENSE`, then use the
   "Choose a license template" button and pick GNU General Public License v3.0).
