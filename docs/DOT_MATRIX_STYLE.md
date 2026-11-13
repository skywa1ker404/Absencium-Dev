# Dot matrix (Nothing-inspired) style: research and plan

Owner request, Batch 43 row 43.10. Only the look is inspired; no Nothing logo, wordmark, font file or icon is used.

## Findings
- Nothing's own typefaces (Ndot 55/57, Nothing Font) are proprietary to Nothing, so they are NOT bundled.
- **Doto** (Oliver Lalan, https://github.com/oliverlalan/Doto): a variable dot-matrix font, SIL Open Font License 1.1, weights 100 to 900 and a roundness axis. OFL fonts may be bundled in a GPL-3.0 app when the font stays a separate, unmodified file and its licence text ships with it (done: `docs/licenses/Doto-OFL-1.1.txt`, listed in `THIRD_PARTY_LICENSES.md`). Bundled as the static `Doto.ttf` (about 170 KB).
- Pixelated style candidate: **Pixelify Sans** (OFL 1.1), to be checked and bundled when that style is built.

## Look rules (owner choices)
- Black and white only, one red accent for alerts and errors; no colour presets and no Monet in this style.
- Doto for all text, dotted icons and dotted progress bars, dot-sweep animations instead of springs (this style only).
- Only one style at a time (Default, Liquid Glass, Pixelated, Dot matrix); the Appearance page shows only the chosen style's settings.

## Build order
1. Style setting (one choice replacing the Liquid Glass switch) with migration. 2. Dot matrix theme: colours, Doto typography. 3. Dotted icons, progress, loading. 4. Pixelated style. 5. Appearance page redesign (picture 29).
