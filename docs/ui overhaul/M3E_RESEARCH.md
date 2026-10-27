# Material 3 Expressive research (batch 2, started 2026-10-28)

Honest status: what was read, what could not be read, what is verified. Nothing here is a claim that a component works in Absencium until CI compiled it (and, for looks and feel, until the owner tested it on a device).

## 1. Sources and what happened

| Source | Result |
| --- | --- |
| `developer.android.com/design/ui/mobile/guides` (and `/guides/home`) | **Could not be fetched** (the site refused the request). The guide pages, their images and GIFs are **not read yet**. Needs the owner to paste pages, or another route |
| `m3.material.io/blog/m3-expressive-motion-theming` | **Not readable**: the page needs JavaScript; only the title and the one-line summary ("a new customizable motion theming system") came back. Linked pages not read |
| Compose Material 3 release notes (`developer.android.com/jetpack/androidx/releases/compose-material3`) | Read through a summariser, 1.5.0-alpha22 up to 1.5.0-beta02 only (see section 2). The 1.4.0-alpha11 to 1.5.0-alpha21 entries were not in what came back. The summary is **not verified against the page itself** |
| `developer.android.com/develop/ui/compose/components/*` (button, progress, fab) and the Material 3 design-system page | Read. They document only the **standard** components; Expressive ones (button groups, toggle and split buttons, wavy progress, loading indicator, FAB menu, `MaterialExpressiveTheme`, `MotionScheme`) are not on them |
| Repositories Minus, June, PixelPlayer | Cloned (shallow) into the scratchpad and searched for the Expressive APIs they use (section 3) |
| Web search for the guide pages | Only returned the Wear OS guides; no Mobile guide pages |

## 2. Compose Material 3 changes that matter to us (from the release notes summary; app uses 1.5.0-alpha29)

- 1.5.0-alpha23 (2026-07-01): Expressive `TimePicker`; Expressive list items; **flexible app bars graduated from experimental** (`LargeFlexibleTopAppBar`, `MediumFlexibleTopAppBar`, `FlexibleBottomAppBar`, ...); expanded search bars graduated.
- alpha24: `SearchBarState` and slot-based `SearchBar` stable; new `material3-ripple` library; `SplitButton` fix.
- alpha25: `ButtonGroupScope` sealed; `TonalToggleButton` renamed `FilledTonalToggleButton`; `ToggleButtonDefaults.shapes` deprecated for `shapesFor`; `SplitButtonLayout` deprecated for `SplitButton`; `BasicAlertDialog` stable; menu item colours.
- alpha26: toggle button defaults objects; `BottomAppBar` stable; wide navigation rail fixes; `SearchBarScrollState`.
- alpha27: `LocalMotionScheme` removed, use `MaterialTheme.motionScheme`; expressive toggle button overloads with `ButtonSize` and an `icon` slot; Vibrant time picker.
- alpha28: carousel parallax is now the `carouselParallaxScrollEffect` modifier; stateless `Slider` deprecated in favour of the state version.
- alpha29 (our version): `onValueChange` required in `Slider` / `RangeSlider`.
- 1.5.0-beta01 (2026-10-07) and beta02 (2026-10-21) exist: `PolygonShape` / `CornerRounding` API changes, `HorizontalCenteredHeroCarousel.preferredItemWidth`, `CarouselDefaults.ItemSpacing` / `ContentPadding`. **Decision:** stay on alpha29 while the redesign is built (CI verified); try beta02 as one separate commit afterwards.

## 3. The reference repositories

| Repo | Licence | May we reuse code? | Expressive APIs it uses (counted by search) |
| --- | --- | --- | --- |
| Minus (`isaacsa51/Minus`) | Apache-2.0 | Yes, patterns and code with notice (compatible with GPL-3.0) | `ToggleButton`, `MaterialShapes`, `Morph` (shape morphing, `androidx.graphics.shapes`), `MotionScheme.expressive()` in `MaterialExpressiveTheme`, `CircularWavyProgressIndicator`, `ContainedLoadingIndicator`. Its settings lists use group-position corner shapes that morph on selection |
| June (`DenserMeerkat/June`) | GPL-3.0 | Yes (same licence as ours); keep notices | `MaterialShapes`, `CircularWavyProgressIndicator`, `ContainedLoadingIndicator`, toggle buttons, `FilledIconToggleButton` with `IconButtonDefaults.toggleableShapes()` |
| PixelPlayer (`PixelPlayerHQ/PixelPlayer`) | **Proprietary**: study and personal use only, no redistribution of the software or any derivative | **No. Nothing is copied from it.** Only the public idea of which Material components exist | `LoadingIndicator`, `ContainedLoadingIndicator`, `LinearWavyProgressIndicator`, `HorizontalFloatingToolbar`, `LargeFlexibleTopAppBar`, `MediumFlexibleTopAppBar`, `FlexibleBottomAppBar`, `MotionScheme` |

## 4. What Absencium already compiles today (CI green)

`ToggleButton`, `ButtonGroupDefaults.connected*ButtonShapes()`, `ConnectedSpaceBetween`, `LoadingIndicator`, `HorizontalFloatingToolbar`, `MaterialExpressiveTheme` with `MotionScheme.expressive()`, `LinearWavyProgressIndicator(progress = {})`, `MaterialShapes` (cookie badge). Not available at our version: `ToggleButtonDefaults.toggleButtonColors`.

## 5. Adoption plan (each item compiled by CI before it is called done)

1. Shape-morphing loaders: `LoadingIndicator` / `ContainedLoadingIndicator` with expressive colours on every loading state; shape-morph of empty states with `MaterialShapes` and a gentle haptic on each change.
2. Wavy progress (`LinearWavyProgressIndicator`, `CircularWavyProgressIndicator`) for syllabus progress, attendance goals, AI work, import and export.
3. `ToggleButton` and connected button groups for every segmented choice (filters, themes, language, length), `FilledIconToggleButton` with `toggleableShapes()` for toggles.
4. Flexible app bars (`LargeFlexibleTopAppBar`, `MediumFlexibleTopAppBar`) for screen headers, `FlexibleBottomAppBar` where a bottom bar is needed (the 3-button pill stays).
5. FAB menu (`FloatingActionButtonMenu`, `ToggleFloatingActionButton`): name and signature to be checked by compiling; used for Vault actions if it compiles.
6. Carousels for Home (next classes, watched subjects) and the Vault: `HorizontalMultiBrowseCarousel` / `HorizontalUncontainedCarousel` (verify by compiling).
7. Settings lists with grouped, position-shaped rows that morph on press and selection.
8. Motion: everything reads `MaterialTheme.motionScheme` (spatial and effects springs); Performance mode and reduced motion keep working.

## 6. Still unread (needs another route)

- Every page, image and GIF under `developer.android.com/design/ui/mobile/guides`.
- `m3.material.io/blog/m3-expressive-motion-theming` and the pages it links to; the Material 3 Expressive component pages.
- Release notes 1.4.0-alpha11 to 1.5.0-alpha21.
- Android 16 / 17 specific design changes.
If the owner can paste the text of those pages (or a Markdown export), they will be added here.
