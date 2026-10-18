#!/usr/bin/env python3
"""Checks that every relative link and image in the README and docs of a directory points at a file that exists."""
import os, re, sys
root = sys.argv[1] if len(sys.argv) > 1 else "."
bad = 0
# Verbatim copies of docs/ bundled into the app as plain text. They are shown as text, their relative links are not
# navigable, and the originals are checked in docs/, so checking the copies only gives false alarms.
SKIP_DIRS = (os.path.join("app", "src", "main", "assets", "licences"), os.path.join("release", "stable"))  # the stable README is checked inside the exported tree
for dirpath, _, files in os.walk(root):
    if ".git" in dirpath.split(os.sep) or "/build" in dirpath:
        continue
    if any(os.path.relpath(dirpath, root).startswith(skip) for skip in SKIP_DIRS):
        continue
    for name in files:
        if not name.endswith(".md"):
            continue
        path = os.path.join(dirpath, name)
        text = open(path, encoding="utf-8", errors="replace").read()
        links = re.findall(r'\]\(([^)\s]+)\)', text) + re.findall(r'(?:src|href)="([^"]+)"', text)
        for link in links:
            if re.match(r'^(https?:|mailto:|#)', link):
                continue
            target = link.split("#")[0]
            if not target:
                continue
            full = os.path.normpath(os.path.join(dirpath, target))
            if not os.path.exists(full):
                print(f"BROKEN in {os.path.relpath(path, root)}: {link}")
                bad += 1
print("All relative links resolve." if not bad else f"{bad} broken relative link(s).")
sys.exit(1 if bad else 0)
