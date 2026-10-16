#!/usr/bin/env python3
"""Lists the licence of every library that ends up in the app, read from each library's own POM file.

Input: the output of `./gradlew :app:dependencies --configuration releaseRuntimeClasspath` (path in argv[1]).
Output: a Markdown table (path in argv[2]). Libraries whose POM declares no licence (even in a parent POM) are listed
separately as "no licence declared" so they get checked by hand instead of being guessed.
"""
import re
import sys
import urllib.request
import xml.etree.ElementTree as ET

REPOS = [
    "https://dl.google.com/dl/android/maven2",
    "https://repo.maven.apache.org/maven2",
]
LINE = re.compile(r"([A-Za-z0-9_.\-]+):([A-Za-z0-9_.\-]+)(?::([^\s]+))?(?: -> ([^\s]+))?")


def fetch_pom(group, artifact, version):
    path = f"{group.replace('.', '/')}/{artifact}/{version}/{artifact}-{version}.pom"
    for repo in REPOS:
        try:
            with urllib.request.urlopen(f"{repo}/{path}", timeout=30) as response:
                return response.read(), repo
        except Exception:
            continue
    return None, None


def local(tag):
    return tag.split("}", 1)[-1]


def child(node, name):
    for item in node:
        if local(item.tag) == name:
            return item
    return None


def licences_of(group, artifact, version, depth=0):
    """Returns (licences, source) where licences is a list of (name, url)."""
    data, repo = fetch_pom(group, artifact, version)
    if data is None:
        return None, "POM not found"
    root = ET.fromstring(data)
    box = child(root, "licenses")
    found = []
    if box is not None:
        for lic in box:
            name = child(lic, "name")
            url = child(lic, "url")
            found.append(((name.text or "").strip() if name is not None else "", (url.text or "").strip() if url is not None else ""))
    if found:
        return found, ("own POM" if depth == 0 else f"parent POM ({group}:{artifact}:{version})")
    parent = child(root, "parent")
    if parent is not None and depth < 6:
        pg, pa, pv = (child(parent, n) for n in ("groupId", "artifactId", "version"))
        if pg is not None and pa is not None and pv is not None:
            return licences_of(pg.text.strip(), pa.text.strip(), pv.text.strip(), depth + 1)
    return [], "no licence in POM chain"


def main():
    deps = {}
    for raw in open(sys.argv[1], encoding="utf-8", errors="replace"):
        # Only the tree lines ("+--- ", "\\--- ", with "|" indents) name libraries; "(c)" lines are constraints.
        if "---" not in raw or "(c)" in raw or "project :" in raw:
            continue
        match = LINE.search(raw.split("---", 1)[1])
        if not match:
            continue
        group, artifact, version, resolved = match.groups()
        final = resolved or version
        if not final:
            continue
        deps[(group, artifact, final)] = True
    rows, missing = [], []
    for group, artifact, version in sorted(deps):
        found, source = licences_of(group, artifact, version)
        label = f"{group}:{artifact}:{version}"
        if found:
            rows.append((label, "; ".join(f"{n} <{u}>" if u else n for n, u in found), source))
        else:
            missing.append((label, source))
    with open(sys.argv[2], "w", encoding="utf-8") as out:
        out.write(f"{len(deps)} libraries resolved in the release runtime classpath.\n\n")
        out.write("| Library | Licence declared in its POM | Where declared |\n| --- | --- | --- |\n")
        for label, lic, src in rows:
            out.write(f"| `{label}` | {lic} | {src} |\n")
        out.write("\n### No licence declared in the POM (check by hand)\n\n")
        if missing:
            for label, src in missing:
                out.write(f"- `{label}`: {src}\n")
        else:
            out.write("None.\n")


main()
