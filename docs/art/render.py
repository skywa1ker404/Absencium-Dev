"""Renders hero.html, showcase.html and badge.html to PNG files in .github/ with Chromium. Run: python3 docs/art/render.py"""
import pathlib, sys
from playwright.sync_api import sync_playwright
here = pathlib.Path(__file__).resolve().parent
out = here.parent.parent / ".github"
jobs = [("hero", 1100, 612, False), ("showcase", 992, None, False), ("badge", 460, 140, True)]
with sync_playwright() as p:
    b = p.chromium.launch(executable_path="/opt/pw-browsers/chromium")
    for name, w, h, transparent in jobs:
        page = b.new_page(viewport={"width": w, "height": h or 800}, device_scale_factor=2)
        page.goto((here / f"{name}.html").as_uri())
        page.evaluate("document.fonts.ready")
        page.wait_for_timeout(300)
        page.screenshot(path=str(out / (f"badge_obtainium.png" if name == "badge" else f"{name}.png")), full_page=h is None, omit_background=transparent)
    b.close()
