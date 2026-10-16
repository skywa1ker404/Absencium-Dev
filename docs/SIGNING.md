# Signing

Android installs an APK over an installed app only when both have the same application ID **and** are signed with the same
certificate. Absencium therefore uses two separate keys.

| Key | Where | Used for | Secret? |
| --- | --- | --- | --- |
| Dev key `app/signing/absencium-dev.jks` (alias `absenciumdev`, password `absencium-dev`) | Absencium-Dev repository only | every debug build, and the optimised build of the Dev repository | **No.** It is public on purpose and protects nothing. |
| Stable key | GitHub Actions secrets of the **Absencium** repository only | stable releases (`v1.0.0` and later) | **Yes.** Never in any repository, never in chat. |

The two channels are also different apps on a phone: Dev installs as `skywa1ker404.absencium.dev` ("Absencium Dev"), stable as
`skywa1ker404.absencium`. A dev build can never update or overwrite the stable app.

## The retired key

`app/signing/absencium-upgrade.jks` and its password were committed to a public repository and are **compromised**. So is the
older `app/signing/trajectory-debug.jks` (password `trajectory-debug`) in git history. Nothing in the build uses either of
them any more. They stay in the tree only until you approve their removal (the history keeps them either way; see
"Removing the old key").

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

Deleting `absencium-upgrade.jks`, `trajectory-debug.jks` and the old passwords from the tree only hides them from new commits.
Anyone who already cloned the repository, and the Git history of the old repository, still have them. Treat the keys as
public forever, which is fine as long as nothing signs with them. Rewriting history is not needed once nothing trusts them, and
it breaks every clone, so I will not do it without your explicit decision.

## Play Store

For Google Play use a separate upload key kept outside any repository and Play App Signing.
