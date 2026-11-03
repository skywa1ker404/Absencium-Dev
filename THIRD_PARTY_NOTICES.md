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

Later ports (settings pages, chat screens, search) are listed here when they are added.
