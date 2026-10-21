#!/usr/bin/env bash
# Builds the file set of the stable "Absencium" repository from this Dev repository into an empty directory.
# It only copies; it never touches GitHub. Usage: scripts/export_stable.sh <empty target dir>
set -euo pipefail
target="${1:?usage: export_stable.sh <empty target dir>}"
[ -d "$target" ] && [ -z "$(ls -A "$target")" ] || { echo "Target must be an existing, empty directory."; exit 1; }
cd "$(git rev-parse --show-toplevel)"

# Required to build: sources, resources, Room schemas, version catalog, wrapper, Gradle files. The dev signing key and the
# retired keys are never copied.
git ls-files app gradle gradlew gradlew.bat build.gradle.kts settings.gradle.kts .gitignore \
  | grep -v -E '^app/signing/' | while read -r f; do mkdir -p "$target/$(dirname "$f")"; cp -p "$f" "$target/$f"; done

# Stable channel and workflows.
cp release/stable/gradle.properties "$target/gradle.properties"
mkdir -p "$target/.github/workflows" "$target/.github/scripts"
cp release/stable/.github/workflows/*.yml "$target/.github/workflows/"
cp .github/scripts/verify_apk.sh "$target/.github/scripts/"
cp .github/FUNDING.yml "$target/.github/"

# Documentation, notices and the assets the README refers to.
mkdir -p "$target/docs"
for f in SIGNING.md IDENTITY.md CONTRIBUTION.md THIRD_PARTY_LICENSES.md DEPENDENCY_LICENSES.md CHNAGELOG.md; do
  [ -f "docs/$f" ] && cp "docs/$f" "$target/docs/$f"
done
cp -r docs/licenses "$target/docs/licenses"
# The stable README and the licence text. Both are required: the export stops if either is missing.
[ -f release/stable/README.md ] || { echo "release/stable/README.md is missing."; exit 1; }
[ -f LICENSE ] || { echo "LICENSE is missing."; exit 1; }
cp release/stable/README.md "$target/README.md"
cp LICENSE "$target/LICENSE"
[ -d release/stable/release-notes ] && cp -r release/stable/release-notes "$target/release-notes"
# README images (only those the stable README links).
if [ -f "$target/README.md" ]; then
  grep -o -E '(src|href)="[^"#:]+"|\]\([^)#:]+\)' "$target/README.md" | sed -E 's/^(src|href)="//; s/^\]\(//; s/["\)]$//' | sort -u | while read -r rel; do
    case "$rel" in http*|mailto*) continue;; esac
    if [ -e "$rel" ] && [ ! -e "$target/$rel" ]; then mkdir -p "$target/$(dirname "$rel")"; cp -r "$rel" "$target/$rel"; fi
  done
fi
echo "Exported to $target. Not copied on purpose: app/signing/, dev workflows, the Dev README, TODOList, CURRENT, MASTER_SPEC, LICENSE_AUDIT, docs/art, caches, logs."
echo "Check links and assets:"
rc=0
python3 scripts/check_links.py "$target" || rc=1
# The stable tree must hold the licence text unchanged, and no keystore of any kind.
cmp -s LICENSE "$target/LICENSE" || { echo "LICENSE differs from the source."; rc=1; }
if find "$target" \( -name '*.jks' -o -name '*.keystore' -o -name '*.p12' -o -name '*.pfx' -o -name '*.pem' \) | grep -q .; then
  echo "A keystore file was copied; that must never happen."; rc=1
fi
exit $rc
