#!/usr/bin/env bash
# Copies the third-party notices into the app's assets so the "Open-source licences" screen can show them.
# CI fails when the copy is out of date. Run this after changing docs/THIRD_PARTY_LICENSES.md or docs/licenses.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
dest=app/src/main/assets/licences
rm -rf "$dest" && mkdir -p "$dest"
cp docs/THIRD_PARTY_LICENSES.md "$dest/"
cp -r docs/licenses "$dest/licenses"
echo "Synced notices into $dest"
