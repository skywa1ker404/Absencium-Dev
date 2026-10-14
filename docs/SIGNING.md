# Signing

Android installs an APK over an installed app only when both are signed with the same key. Every Absencium APK must
therefore use one key for the life of the app.

## Current setup

* `app/signing/absencium-upgrade.jks` (alias `absencium`, RSA 4096, valid for 100 years) signs the debug and the
  optimised build. It lives in this **private** repository so that builds installed from CI can always be updated.
  Its password is in `app/build.gradle.kts`. This is acceptable for a private sideloaded app and **not** for a store
  release.
* When the GitHub Actions secrets below exist, CI signs with that key instead, and the repository key is not used.

## Moving the key into GitHub secrets (recommended)

Settings, Secrets and variables, Actions, New repository secret:

| Secret | Value |
| --- | --- |
| `ABSENCIUM_KEYSTORE_BASE64` | `base64 -w0 absencium-upgrade.jks` |
| `ABSENCIUM_KEYSTORE_PASSWORD` | the store password |
| `ABSENCIUM_KEY_ALIAS` | `absencium` |
| `ABSENCIUM_KEY_PASSWORD` | the key password |

Use **the same key** that the installed builds were signed with. After the secrets work, delete the keystore and the
passwords from the repository (they stay in git history, so treat that key as retired only if you also start a new
app identity). Keep an offline backup of the key: if it is lost, no update can ever be installed over existing installs.

## Store release

For a Play Store release use a separate upload key kept outside the repository, and Play App Signing.
