# Signing

Android installs an APK over an installed app only when both have the same application ID **and** are signed with the same
certificate. Absencium therefore uses two separate keys.

| Key | Where | Used for | Secret? |
| --- | --- | --- | --- |
| Dev key `app/signing/absencium-dev.jks` (alias `absenciumdev`, password `absencium-dev`) | Absencium-Dev repository only | every debug build, and the optimised build of the Dev repository | **No.** It is public on purpose and protects nothing. |
| Stable key | GitHub Actions secrets of the **Absencium** repository only | stable releases (`v1.0.0` and later) | **Yes.** Never in any repository, never in chat. |

The two channels are also different apps on a phone: Dev installs as `skywa1ker404.absencium.dev` ("Absencium Dev"), stable as
`skywa1ker404.absencium`. A dev build can never update or overwrite the stable app.

## The retired keys

Two earlier keystores were committed to a public repository, so they are **compromised** and nothing may be signed with them:
`app/signing/absencium-upgrade.jks` (removed from the current tree on 2026-10-17) and the older `app/signing/trajectory-debug.jks`
(removed earlier). No password for either is written anywhere in the current tree. They remain in Git history, which has not been
rewritten (see "Removing the old key"). Nothing in the build uses them; a scan of the tree on 2026-10-17 found no other copy of a
keystore (`.jks`, `.keystore`, `.p12`, `.pfx`, `.pem`) apart from the public Dev key, and `.gitignore` now blocks new ones.

## Create the stable key (you do this, on your own computer)

1. Install a JDK so `keytool` exists, then run (choose your own strong passwords when asked; do not paste them anywhere):

   `keytool -genkeypair -v -keystore absencium-stable.jks -storetype PKCS12 -alias absencium -keyalg RSA -keysize 4096 -validity 10950`

2. Back up `absencium-stable.jks` and both passwords somewhere offline (a password manager and an encrypted USB stick). If the
   key is lost, no update can ever be installed over installs signed with it.
3. Print its fingerprint (it is public information) and keep the line `SHA256:`:

   `keytool -list -v -keystore absencium-stable.jks -alias absencium`

4. In the **Absencium** repository: Settings, Secrets and variables, Actions.
   **Secrets** tab, New repository secret, four times:

   | Secret | Value |
   | --- | --- |
   | `ABSENCIUM_KEYSTORE_BASE64` | output of `base64 -w0 absencium-stable.jks` (on Windows: `certutil -encode`, or use Git Bash) |
   | `ABSENCIUM_KEYSTORE_PASSWORD` | the store password |
   | `ABSENCIUM_KEY_ALIAS` | `absencium` |
   | `ABSENCIUM_KEY_PASSWORD` | the key password |

   **Variables** tab, New repository variable: `ABSENCIUM_CERT_SHA256` = the SHA256 fingerprint from step 3 (a variable, not a
   secret). The release workflow refuses to publish a draft whose APK certificate does not match it.
5. Tell me when it is done. I will not ask for, read or print any of the values.

## How the build uses them

`app/build.gradle.kts` reads the stable key only from the environment variables `ABSENCIUM_KEYSTORE_FILE`,
`ABSENCIUM_KEYSTORE_PASSWORD`, `ABSENCIUM_KEY_ALIAS` and `ABSENCIUM_KEY_PASSWORD` that the release workflow fills from the
secrets. Without them a stable release build is unsigned and the workflow stops.

## Updating existing installs: what is and is not possible

* APKs shared so far, by git history of `app/build.gradle.kts` (the old APK files are no longer available to inspect):
  builds before 2026-10-12 used an automatically generated debug key of the CI machine (different on every run);
  from commit `a6f1b27` (2026-10-12) they used `trajectory-debug.jks` with the ID `com.trajectory.vault`;
  from commit `861eb40` (2026-10-15) they used `absencium-upgrade.jks` with `com.absencium.app`, and after `1993eed` the same
  key with `skywa1ker404.absencium`.
* The stable app uses the ID `skywa1ker404.absencium`, the same as the latest builds that were shared, but a **new key**. Android
  will **refuse to install it over those builds** ("package conflicts with an existing package"). Those users must uninstall
  first, which deletes the app's data. Tell them to export a backup (Settings, Storage) before uninstalling.
* Android does support key rotation (APK Signature Scheme v3 with a proof-of-rotation lineage, `apksigner rotate`), but it
  works only on Android 9 and newer, while Absencium supports Android 8. It would also need the old, compromised key to sign
  the lineage, and anyone holding that key could sign a competing lineage, so it is not recommended here. For a handful of
  testers, uninstall and reinstall is the safer path. Nothing about a new key is guaranteed to update an existing install.
* From v1.0.0 onwards the stable key must never change.

## Removing the old key

The two keystores and their passwords are gone from the current tree (2026-10-17). That only hides them from new commits. Anyone
who already cloned the repository, and the Git history, still have them. Treat the keys as public forever, which is fine as long
as nothing signs with them. Rewriting history is not needed once nothing trusts them, and it breaks every clone, so it is not done
without a separate explicit decision.

## Plan for the APKs already given to friends

1. Builds shared so far are signed with one of the retired keys or, before 2026-10-12, with a throwaway CI debug key (see "Updating existing installs"). They keep working; nothing is taken away from them.
2. Dev builds (`skywa1ker404.absencium.dev`) are a different app and install next to them. They never update those APKs.
3. Stable 1.0.0 (`skywa1ker404.absencium`, new key) cannot be installed over a build signed with a retired key. For each friend:
   export a backup (Settings, Storage), uninstall the old Absencium, install 1.0.0, then Restore the backup.
4. From 1.0.0 the stable key never changes, so every later version updates in place.
5. Until 1.0.0 exists, friends should not install anything that claims to be an Absencium update: only APKs from the Dev
   repository's artifacts or releases, with the SHA-256 listed beside them.

## Play Store

For Google Play use a separate upload key kept outside any repository and Play App Signing.
