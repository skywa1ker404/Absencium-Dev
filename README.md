# Absencium Dev

Development repository of **Absencium**, a student companion for Android (attendance, missed-class notes, calendar, exam countdown).

> [!WARNING]
> **This is the development repository, not the stable app.**
> - Builds from here are **unstable and untested on most devices**. Do not rely on them for real attendance data.
> - They install as a **different app** (`skywa1ker404.absencium.dev`, shown as "Absencium Dev"), so they never replace or touch the stable app.
> - They are signed with a **public throwaway key** that is in this repository on purpose (`app/signing/absencium-dev.jks`). It proves nothing about who built an APK. Never treat a Dev APK as a trusted release.
> - Stable releases will come only from the separate `skywa1ker404/Absencium` repository, signed with a key that is not stored in any repository.

The stable repository's README is staged in [`release/stable/README.md`](release/stable/README.md) and is published there only after the 1.0.0 release is approved.

## Status

| What | State |
| --- | --- |
| Debug and optimised builds | built by GitHub Actions on every push that touches the app or build files |
| Unit tests | none exist yet (`testDebugUnitTest` runs with nothing to test) |
| Device testing | **not yet done**; CI compiling a change does not mean it works on a phone |
| Open-source licence audit | **not cleared**: the source-licence scanner could not be run; see [`docs/LICENSE_AUDIT.md`](docs/LICENSE_AUDIT.md) |
| Plan and progress | [`TODOList.md`](TODOList.md) and [`docs/ROADMAP.md`](docs/ROADMAP.md) |

## Getting a Dev APK

1. Open the **Actions** tab, choose the latest green *Android Build* run and download `absencium-dev-debug-apk` or `absencium-dev-optimized-apk` (a GitHub sign-in is needed to download artifacts). Each contains `apk-report.txt`, the output of [`verify_apk.sh`](.github/scripts/verify_apk.sh) (application ID, certificate, ABIs, SHA-256).
2. Or use a prerelease made by the manual *Dev release* workflow (it never runs on its own).
3. Install over the previous Dev build; every CI build has a higher version code, so it updates in place. Android 8.0 (API 26) or newer is required.

## What to test

- Navigation pill: drag across it and tap; labels must not be cut or show text from the page behind.
- About → Legal → Open-source licences: scrolling stays smooth, the scrollbar appears, links open, text can be selected.
- Settings → AI & Integrations → Custom endpoint: the API style chips wrap instead of squeezing.
- Long lists (a subject folder with many notes, the AI model list, search) stay responsive.
- Settings → Storage & Data: backup, restore, then **Restart app**.
- Add Entry → YouTube Video Notes: paste a transcript.

Report what you see with the phone model, Android version, and what **Settings → Test Lab** reports (remove any personal data first).

## How the build works

- Build only through GitHub Actions ([`.github/workflows/android.yml`](.github/workflows/android.yml)); path filters mean documentation-only changes do not start a build.
- `absenciumChannel=dev` is set in `gradle.properties`; the stable repository sets `stable`.
- Secrets are never needed here. The identity and secret guard fails the build if a keystore or credential pattern appears outside the allowed files.
- Signing details and the plan for APKs already given to friends: [`docs/SIGNING.md`](docs/SIGNING.md).

## Contributing

Read [`docs/CONTRIBUTION.md`](docs/CONTRIBUTION.md), including the section on the licence of contributions. Do not post API keys, private records or keystores in issues or pull requests.

## Licence

GPL-3.0. The full text is in [`LICENSE`](LICENSE).

Written by the maintainer with AI assistance. Copyright (c) 2026 skywa1ker404 to the extent copyright applies. This notice only names the author; it does not replace or change the GPL-3.0 text, which alone sets the licence terms.

Third-party notices: [`docs/THIRD_PARTY_LICENSES.md`](docs/THIRD_PARTY_LICENSES.md) (texts in [`docs/licenses`](docs/licenses), libraries in [`docs/DEPENDENCY_LICENSES.md`](docs/DEPENDENCY_LICENSES.md)). The notices are also bundled in the app. The README pictures are illustrations drawn for the project (sources in [`docs/art`](docs/art)).
