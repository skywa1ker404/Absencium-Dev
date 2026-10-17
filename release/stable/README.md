<div align="center">

<h1>Absencium</h1>

<p><strong>Your student companion.</strong></p>

<p>Attendance · Missed-class notes · Calendar · Exam countdown</p>

<img src=".github/hero.png" alt="Illustration for Absencium, a student companion app" width="100%">

<p>A native Android app for tracking attendance, keeping notes for the classes you missed, and seeing what is coming up.</p>

<p>
  <a href="https://github.com/skywa1ker404/Absencium/releases/latest"><img src="https://img.shields.io/github/v/release/skywa1ker404/Absencium?style=flat&logo=github&label=Latest%20Release" alt="Latest release"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/Licence-GPL--3.0-blue?style=flat" alt="GPL-3.0 licence"></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat&logo=android&logoColor=white" alt="Android 8.0 and newer">
  <img src="https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?style=flat&logo=kotlin&logoColor=white" alt="Kotlin and Jetpack Compose">
</p>

</div>

Absencium is a student-focused Android app built to make academic life a little less chaotic. It keeps your attendance, notes and dates in one place, on your own phone, and works without an account.

> [!NOTE]
> Absencium is an independent project and is not affiliated with any educational institution. The pictures in this README are illustrations drawn for the project, not screenshots.

> [!WARNING]
> **This is the first release and it is a young app.** Do not rely on it as the only record of your attendance. Use **Settings → Storage & Data → Backup** regularly, and read the release notes before installing an update.

---

## Preview

<p align="center">
  <img src=".github/showcase.png" alt="Illustration of the Absencium screens" width="100%">
</p>

---

## What it does today

- **Home**: a summary with the subjects to watch, what is next, and an exam countdown.
- **Attendance and calendar**: mark lectures present or absent, see your attendance percentage, manage a timetable, holidays (including Saturday and Sunday switches) and exams.
- **Vault**: notes organised by subject. Write notes, attach photos, save unfinished entries as drafts, and store guide or answer PDFs. A notes-collected tracker covers days you were absent.
- **Search** across notes, PDFs and exams.
- **Optional AI help** (off until you set it up): connect your own provider and key to turn photos, text and YouTube videos into notes (Gemini reads a video link directly; other providers take a pasted transcript). Providers listed in the app: OpenAI, Anthropic Claude, Google Gemini, OpenRouter, Groq, Ollama (your own server) and a custom endpoint.
- **Look and feel**: Material 3 Expressive design, a floating pill navigation bar, light and dark modes, Monet colours on Android 12 and newer, many colour schemes, and an optional frosted "Liquid Glass" style with a Performance mode for slower phones.
- **Backup and restore** of your data to a file you choose.

### Not in this release

Reminders and notifications, home-screen widgets and several API keys per AI provider do not exist yet.

---

## Install

1. Open the [latest release](https://github.com/skywa1ker404/Absencium/releases/latest) and download `Absencium-<version>.apk`.
2. Check the file against `SHA256SUMS.txt` from the same release (`sha256sum -c SHA256SUMS.txt` on a computer). The release notes also give the signing certificate's SHA-256 so you can compare it with the APK's.
3. Allow your browser or file manager to install apps from this source, open the APK and install.

Or add the repository to Obtainium:

<p align="center">
  <a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium%3A%2F%2Fadd%2Fhttps%3A%2F%2Fgithub.com%2Fskywa1ker404%2FAbsencium">
    <img src=".github/badge_obtainium.png" alt="Add Absencium to Obtainium" width="230">
  </a>
</p>

> [!WARNING]
> Download APKs only from this repository. A copy from anywhere else may be modified. Updates install over the app only if they are signed with the same key.

Requires **Android 8.0 (API 26)** or newer. Monet colours and Liquid Glass need Android 12 or newer; the lens effect of the navigation bar needs Android 13.

---

## Privacy

- Your notes, attendance, subjects and settings are stored **on your device**. There is no Absencium account and no server of ours.
- The app has no advertising and no analytics. Its only permission is `INTERNET`, which is used by the optional AI features and by nothing else.
- If you turn on AI, the text, images or transcripts you choose to send go to the provider you picked, under that provider's terms. Your API key is stored encrypted on the device and is not written to the debug log.
- Plain `http://` is allowed so you can use a server on your own network (for example Ollama); the app warns you when you use it.
- Android's own app backup is switched off for Absencium (`allowBackup="false"`); use the in-app backup instead.

---

## Build it yourself

```
./gradlew assembleRelease
```

This produces an **unsigned** release build unless you supply your own signing key through the `ABSENCIUM_KEYSTORE_*` environment variables. The project's real signing key is never in this repository; see [`docs/SIGNING.md`](docs/SIGNING.md). A build you sign yourself is a different app signature and will not install over the official one.

---

## Contributing and bug reports

Bugs and ideas are welcome as [issues](https://github.com/skywa1ker404/Absencium/issues). Never post private academic records, API keys or other secrets. See [`docs/CONTRIBUTION.md`](docs/CONTRIBUTION.md) before sending a pull request.

---

## Licence

Absencium is licensed under the **GNU General Public License v3.0**. The full text is in [`LICENSE`](LICENSE).

Written by the maintainer with AI assistance. Copyright (c) 2026 skywa1ker404 to the extent copyright applies. This notice only names the author; it does not replace or change the GPL-3.0 text, which alone sets the licence terms.

Third-party components (fonts, icons, libraries and colour schemes) keep their own licences. They are credited in [`docs/THIRD_PARTY_LICENSES.md`](docs/THIRD_PARTY_LICENSES.md), with the full texts in [`docs/licenses`](docs/licenses) and a list of library licences in [`docs/DEPENDENCY_LICENSES.md`](docs/DEPENDENCY_LICENSES.md). The same notices are inside the app under **About → Legal → Open-source licences**. The illustrations were drawn for this project (sources in the development repository's `docs/art`).

---

## FAQ

<details>
<summary><strong>Is Absencium free?</strong></summary>

Yes. It is free software under the GPL-3.0. Optional third-party AI services may have their own limits or costs.

</details>

<details>
<summary><strong>Does it work offline?</strong></summary>

Yes, apart from the optional AI features, which need a connection to the provider you chose.

</details>

<details>
<summary><strong>Why isn't it on the Play Store?</strong></summary>

It is distributed through GitHub for now.

</details>

<details>
<summary><strong>Will there be an iOS version?</strong></summary>

No. Absencium stays Android-only.

</details>

<details>
<summary><strong>Who's making Absencium?</strong></summary>

I am a student building this in my free time. I handle the feature planning, design decisions and direction of the project and use AI-assisted tools to help build it.

Written by the maintainer with AI assistance. Copyright (c) 2026 skywa1ker404 to the extent copyright applies.

</details>

---

<div align="center">

If you like it: [Buy me a chai](https://www.buymeachai.in/skywa1ker404) · [Website](https://skywa1ker404.github.io/apps/absencium/) · [Issues](https://github.com/skywa1ker404/Absencium/issues) · [Releases](https://github.com/skywa1ker404/Absencium/releases)

</div>
