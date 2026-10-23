# Feature audit of 16 reference repositories

Status: **DRAFT for owner approval.** Nothing from these repositories has been copied, vendored or added as a dependency.
No code, asset, string or dependency from any repo below may be used until Anakin approves this report (owner decision 2026-10-18, item 9).

Absencium is GPL-3.0 (LICENSE added by the owner, identical to gnu.org's text). Anything reused must be compatible with GPL-3.0 and must keep its notices.

## How this was done (and what it does not prove)

- Each repo was shallow-cloned and read at the commit listed below. Licence texts were read from the repo's own file, not from the README badge.
- Features were confirmed by finding the implementing source file (paths below). A README claim with no source file found is marked **README only**.
- Nothing was built or run. "Works" is never claimed here.
- ScanCode has **not** been run (PyPI is unreachable from the workspace); Anakin will run it. File-level headers, bundled fonts/images and transitive dependencies are therefore not audited.
- Not legal advice. Items marked "ask the author" need the copyright holder, not a guess.

## Licence verdicts

| # | Repo | Commit (date) | Licence as found | Verdict for Absencium (GPL-3.0) |
|---|------|---------------|------------------|---------------------------------|
| 1 | Agora | 93dc80029f (2026-10-17) | `LICENSE` byte-identical to GPL-3.0. README: v2.2.0+ GPL-3.0, v2.1.0 and earlier MIT. Name/logo/screenshots reserved. Bundled Termux/proot payloads carry their own GPLv3 terms. | **Compatible** (GPL-3.0 → GPL-3.0). Only take code from the current tree, not from MIT-era history. Do not use name or logo. |
| 2 | Damier | b417dce04e (2026-10-13) | Apache-2.0 **plus Commons Clause** (no right to "Sell"). | **Do not reuse code.** Commons Clause is not an open-source licence and conflicts with GPL-3.0 section 10 (no further restrictions). Ideas only. |
| 3 | Etar | 51bf4ecf0b (2026-10-14) | `LICENSE` is GPL-3.0 operative text (the "How to Apply" appendix is missing, terms are intact); `LICENSE.apache2` for AOSP-derived parts. | **Compatible** (GPL-3.0 and Apache-2.0 parts). Keep Apache notices on AOSP-derived files. |
| 4 | FaceDown | 64fda0f84a (2026-07-05) | **No licence file.** README says it is a showcase of a private app with sanitised snippets. | **Do not reuse.** No licence means all rights reserved. Ideas only. |
| 5 | ImageToolbox | 92245d926c (2026-10-18) | Apache-2.0 (`LICENSE`, README agrees). | **Compatible** (Apache-2.0 → GPL-3.0 allowed). Keep NOTICE/attribution. Very large; per-module dependency licences not audited. |
| 6 | June | f750117221 (2026-10-17) | GPL-3.0, byte-identical. | **Compatible.** |
| 7 | Keshu | 39b9de0e46 (2026-09-11) | GPL-3.0-only (README badge and `LICENSE` byte-identical). `TRADEMARKS.md` reserves name and icon. | **Compatible.** Do not use name or icon. See note on "-only" below. |
| 8 | Kori | bca6471cbc (2026-08-05) | **AGPL-3.0** (`LICENSE.txt`). Sold on Google Play/App Store; free on GitHub. | **Conditional.** GPL-3.0 §13 allows combining with AGPL-3.0, but the combined work then carries AGPL network-use obligations for those parts. Safer: ideas only, or ask the owner whether AGPL parts are acceptable. |
| 9 | LastChat | b8a2cb5231 (2026-09-30) | AGPL-3.0 text **plus a "Segmented Dual Licensing" notice** restricting open-source use to non-commercial / ≤10 users, otherwise commercial licence. Fork of RikkaHub. | **Do not reuse.** The added restriction is a further restriction not permitted by AGPL/GPL, and its true terms are unclear. Ideas only. |
| 10 | Reef | 044358fb26 (2026-10-11) | **No licence file.** README says MIT, then adds mandatory attribution link, and bans the name "Reef" and icon in derivatives. A MIT text is in the README. | **Ask the author** or treat as ideas only. A README-only licence with extra conditions is ambiguous. |
| 11 | Self-Attendance | 30ba939106 (2026-05-29) | **No licence file, no licence statement found** in README. Also contains build logs and large stray folders. | **Do not reuse.** No licence means all rights reserved. Ideas only. |
| 12 | Tomato | 98fa69ed9e (2026-10-18) | GPL-3.0, byte-identical. | **Compatible.** |
| 13 | WikiReader | fc5c0177bd (2026-01-11) | GPL-3.0, byte-identical. | **Compatible.** |
| 14 | baca | 1534f02cae (2026-07-21) | MIT. | **Compatible.** Keep MIT notice. Capacitor/web app, so little direct code reuse. |
| 15 | bubble-notice-android | c2adb00f18 (2026-10-12) | GPL-3.0 (`LICENSE.txt`, byte-identical). | **Compatible.** |
| 16 | yearly-progress | 33f79d7076 (2026-09-13) | `LICENSE` is **AGPL-3.0**, README says **GPL-3.0**. | **Discrepancy.** Treat as AGPL-3.0 (the file wins) or ask the author to fix. Same conditional verdict as Kori. |

Notes:
- "GPL-3.0-only" (Keshu) can be combined with Absencium because Absencium is GPL-3.0 without an "or later" clause. If Absencium ever wanted "GPL-3.0-or-later", Keshu-derived files would have to stay under 3.0 only. Decide Absencium's SPDX id before reusing anything.
- Reusing any code also means copying its copyright notices and recording it in `docs/THIRD_PARTY_LICENSES.md` and the in-app licences screen.

## Verified features and what Absencium has today

"Has today" is from a source search of `app/src/main` for AlarmManager, WorkManager, BiometricPrompt, PdfDocument and AppWidget: none are present. Other items below are from the existing TODOList.

| Feature | Seen in (source path, verified) | Absencium today | Benefit | Difficulty | Risk | Reuse? |
|---------|----------------------------------|-----------------|---------|-----------|------|--------|
| Class reminders with AlarmManager and notification actions (mark attended) | Self-Attendance `notification/TimetableAlarmScheduler.kt`, `ClassNotificationScheduler.kt` | None (Step 8) | High | Medium | Exact-alarm permission rules on 12/13/14 | **Ideas only** (no licence). Write our own. |
| Offline PDF attendance report with `PdfDocument` | Self-Attendance `export/pdf/AttendancePdfGenerator.kt`, `PdfPageManager.kt` | None | Medium | Low-Medium | None technical | **Ideas only.** `PdfDocument` is a platform API, trivial to write ourselves. |
| Smart bunk/attend-to-reach-target maths, unmarked-day automation | Self-Attendance README only (not traced to a file) | Partly (Step 7) | High | Low | Rounding rules | Ideas only |
| WebDAV sync provider | June `core/.../data/sync/WebDAVProvider.kt`, `SyncVM.kt` | None (sync was removed from the plan on 2026-10-22) | High | Medium-High | Conflict handling, credentials | **Compatible (GPL-3.0).** Candidate for adaptation after approval and after the `Transport` seam exists. |
| WebDAV backup with round-trip test | LastChat `androidTest/.../WebdavSyncBackupRoundTripTest.kt` | None | Medium | n/a | Licence problem | **Ideas only** (restricted licence). |
| Biometric/PIN lock | June `MainActivity.kt` | None | Medium | Medium | Lockout recovery | Compatible; AndroidX `biometric` is Apache-2.0 and enough on its own. |
| Network toggle (no external access unless on) | June README only | Planned (Step 14, off by default) | High (matches our consent rule) | Low | none | Idea |
| Home-screen widgets (Glance) | Keshu `widget/DashboardWidgetReceiver.kt`, `ScheduleWidgetReceiver.kt`; yearly-progress `feature/widgets/ui/BaseWidget.kt` | None (deferred to 1.x) | Medium | Medium-High | Glance dependency size, update frequency | Keshu compatible; yearly-progress conditional. Deferred. |
| Android 16 Live Updates / `ProgressStyle` | Tomato `service/TimerService.kt`, `di/androidModules.kt` | None (Step 5/8 with fallbacks) | Medium | Medium | API 36 only, needs version gate and fallback | **Compatible (GPL-3.0).** Good reference for gating. Adapt after approval. |
| Do Not Disturb during focus (NotificationPolicy / interruption filter) | Reef `accessibility/FocusModeService.kt`, `util/Permissions.kt` | None (Quiet Mode in Step 8) | Medium | Medium | Needs notification-policy access grant | Licence unclear. **Ideas only** unless the author clarifies. |
| Bubbles / notification access | bubble-notice `ui/screen/*`, `BubbleActivity.kt` | None | Low for attendance | High | Notification-listener permission is intrusive | Not recommended |
| Calendar recurrence, ICS export | Etar `calendar/icalendar/IcalExporter.kt` | Calendar exists; no ICS | Medium | Medium | AOSP-derived code, heavy | Compatible; recommend writing a small ICS export ourselves, not importing Etar code. |
| Encrypted local secrets | Agora `util/SecretCrypto.kt`, `ShellCrypto.kt` | `ApiKeyVault` exists | Low (we have one) | n/a | n/a | Compare approach only |
| NFC check-in | Damier `nfc/presentation/NfcViewModel.kt` | None | Low | Medium | Licence | **Excluded** (Commons Clause) |
| Markdown/LaTeX/Mermaid note formats, outline side sheet, find and replace, templates | Kori `presentation/component/note/markdown/MarkdownActionRow.kt`, `Constants.kt` | Vault editor exists (Step 10) | Medium | High | AGPL | Ideas only |
| Journal media, tags, streaks, reminders | June README; calendar/streak UI | Partial | Low | n/a | n/a | Ideas |
| E-book reader, Wikipedia reader, image tools, habit tracker, yearly progress, AI chat clients | baca, WikiReader, ImageToolbox, Damier, yearly-progress, Agora, LastChat | Out of scope for 1.0 | Low | High | Scope creep | Not recommended for 1.0 |

## Recommendations

Recommended for 1.0 (after approval, own implementation unless noted):
1. Step 8 reminders and notification actions: **own implementation**, informed by Self-Attendance's approach (ideas only) and Android docs. Version-gate Android 12/13/14/16 behaviours.
2. Android 16 Live Updates for the timer or "class now" state: adapt gating pattern from **Tomato** (GPL-3.0 compatible), with a normal-notification fallback below API 36.
3. PDF attendance report: own code on the platform `PdfDocument`.
4. Network off by default and consent screens: own implementation.

Recommended for 1.x:
5. WebDAV sync: dropped from the plan on 2026-10-22 (owner removed sync).
6. Widgets: Glance, referencing **Keshu**.
7. ICS export for exams and holidays.

Not recommended / excluded: Damier code (Commons Clause), LastChat code (restricted AGPL), FaceDown, Self-Attendance and Reef code (no clear licence), bubble-notice features, NFC check-in, GPS attendance.

## Dependencies worth knowing (not yet approved)

Only those seen in these repos and relevant to the plan: Koin (June, Damier), Retrofit/OkHttp (June), Coil, Media3, MapLibre, MaterialKolor (already used by Absencium). None is being added. Any new dependency needs its own licence check and a row in `docs/DEPENDENCY_LICENSES.md`.

## Unresolved questions for Anakin

1. Approve this report (and which items in "Recommended" may proceed)?
2. Accept AGPL-3.0 sources (Kori, yearly-progress) as reuse candidates, or treat them as ideas only? (Default here: ideas only.)
3. Should Absencium's SPDX id stay GPL-3.0-only (needed if any Keshu code is reused)?
4. Ask the Reef and yearly-progress authors to clarify licensing? (Default: no reuse until clarified.)
5. Run ScanCode on this repo before the stable release, as agreed.

## Ordered plan

1. Owner reviews this report.
2. Approved items get a row in `docs/THIRD_PARTY_LICENSES.md` before any code lands.
3. Reminders and notification work (Step 8) starts own-code, with version gates.
4. Sync: removed from the plan on 2026-10-22.
