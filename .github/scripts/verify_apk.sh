#!/usr/bin/env bash
# Prints what is really inside an APK: application ID, version, signing certificate, native ABIs and SHA-256.
# Usage: verify_apk.sh <apk> <expected application id>. Exits 1 if the application ID or the signature is wrong.
set -euo pipefail
apk="$1"; expected="$2"
bt=$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)
echo "APK: $apk"
echo "SHA-256: $(sha256sum "$apk" | cut -d' ' -f1)"
echo "Size: $(stat -c %s "$apk") bytes"
badging=$("$bt/aapt2" dump badging "$apk" 2>/dev/null | head -1)
echo "$badging" | sed -E "s/ platformBuildVersion.*//"
echo "$badging" | grep -q "name='$expected'" || { echo "ERROR: application ID is not $expected"; exit 1; }
echo "Native ABIs: $("$bt/aapt2" dump badging "$apk" 2>/dev/null | grep -E '^native-code' || echo 'none (no native code, runs on every ABI)')"
"$bt/apksigner" verify --verbose --print-certs "$apk" | grep -E "Verifies|Verified using|certificate (DN|SHA-256)|WARNING" || { echo "ERROR: signature does not verify"; exit 1; }
