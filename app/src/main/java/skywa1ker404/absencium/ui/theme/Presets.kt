package skywa1ker404.absencium.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import skywa1ker404.absencium.data.settings.ThemePreset

/** A palette with a dark variant and, where the scheme has one, a light variant. */
class ThemePalette(
    val name: String,
    /** Names of the variants, shown under the preset name, for example "Mocha · Latte". */
    val variants: String,
    val dark: PaletteVariant,
    val light: PaletteVariant?,
) {
    /** The variant used when light or dark is asked for. A dark-only scheme answers dark for both. */
    fun variantFor(isDark: Boolean): Pair<Boolean, PaletteVariant> =
        if (!isDark && light != null) false to light else true to dark

    val hasLight: Boolean get() = light != null
}

private fun c(hex: Long) = Color(0xFF000000 or hex)

/** The default theme: Everforest, medium contrast, in its dark and light variants (colours from Everforest's palette.md). */
object DefaultPalette {
    val light: PaletteVariant get() = Everforest.light!!
    val dark: PaletteVariant get() = Everforest.dark
}

/**
 * Colours are the schemes' published values: Catppuccin palette.json, Dracula README,
 * Gruvbox gruvbox.vim, Nord's colour page, onedark.nvim, poimandres.nvim, Rosé Pine's neovim palette and
 * tokyonight.nvim. Nord, Dracula and Poimandres have no official light scheme in those sources; Nord's light
 * variant uses its Snow Storm colours as backgrounds, Dracula and Poimandres stay dark.
 *
 * Added later, colour values only, each read from the scheme's own repository (all MIT; the notices are in
 * docs/THIRD_PARTY_LICENSES.md): altercation/solarized, rebelot/kanagawa.nvim, savq/melange-nvim,
 * ribru17/bamboo.nvim, ayu-theme/ayu-colors, jolaleye/horizon-theme-vscode, kaicataldo/material.vim,
 * sdras/night-owl-vscode-theme, wesbos/Cobalt2-VSCode, ahmadawais/shades-of-purple-vscode,
 * robb0wen/synthwave-vscode, NLKNguyen/papercolor-theme and sainnhe/sonokai.
 */
fun ThemePreset.palette(): ThemePalette = when (this) {
    ThemePreset.AYU_MIRAGE -> AyuMirage
    ThemePreset.BAMBOO -> Bamboo
    ThemePreset.CATPPUCCIN -> Catppuccin
    ThemePreset.COBALT2 -> Cobalt2
    ThemePreset.DRACULA -> Dracula
    ThemePreset.GRUVBOX -> Gruvbox
    ThemePreset.HORIZON -> Horizon
    ThemePreset.KANAGAWA -> Kanagawa
    ThemePreset.MATERIAL_OCEAN -> MaterialOcean
    ThemePreset.MELANGE -> Melange
    ThemePreset.NIGHT_OWL -> NightOwl
    ThemePreset.NORD -> Nord
    ThemePreset.ONE_DARK -> OneDark
    ThemePreset.PAPERCOLOR -> PaperColor
    ThemePreset.POIMANDRES -> Poimandres
    ThemePreset.ROSE_PINE -> RosePine
    ThemePreset.SHADES_OF_PURPLE -> ShadesOfPurple
    ThemePreset.SOLARIZED -> Solarized
    ThemePreset.SONOKAI -> Sonokai
    ThemePreset.SYNTHWAVE_84 -> SynthWave84
    ThemePreset.TOKYO_NIGHT -> TokyoNight
    ThemePreset.SAGE_OXBLOOD -> SageOxblood
    ThemePreset.ORCHID_EMBER -> OrchidEmber
    ThemePreset.NO_COLOUR -> NoColour
}

/**
 * Built from three colours the owner chose, #A5A79B (sage), #E8D2B5 (sand) and #590907 (oxblood). Light: sand-tinted paper, oxblood
 * as the main colour, sage and bronze as accents. Dark: a near-black oxblood-brown, sand as the main colour, sage as the second.
 */
private val SageOxblood = ThemePalette(
    name = "Sage & Oxblood",
    variants = "Night · Day",
    dark = PaletteVariant(
        background = c(0x1B1110), text = c(0xE8D2B5), textMuted = c(0xB7A48C), outline = c(0x6E5C55),
        primary = c(0xE8D2B5), secondary = c(0xA5A79B), tertiary = c(0xC9605A), error = c(0xFF8A80),
        primaryContainer = c(0x590907), secondaryContainer = c(0x3A3C35),
    ),
    light = PaletteVariant(
        background = c(0xF7EEE1), text = c(0x2B1110), textMuted = c(0x6B5651), outline = c(0x8E7F76),
        primary = c(0x590907), secondary = c(0x6A6D60), tertiary = c(0x8C6A45), error = c(0xB3261E),
        primaryContainer = c(0xE8D2B5), secondaryContainer = c(0xDCDDD2),
    ),
)

/**
 * Built from three colours the owner chose, #E7B0DE (orchid), #400849 (deep purple) and #FF5A0A (ember orange). Dark: purple-black
 * with ember orange as the main colour and orchid as the second. Light: pale orchid paper with deep purple as the main colour.
 */
private val OrchidEmber = ThemePalette(
    name = "Orchid & Ember",
    variants = "Night · Day",
    dark = PaletteVariant(
        background = c(0x1A0420), text = c(0xF3DCEE), textMuted = c(0xC9A5C2), outline = c(0x7A4F82),
        primary = c(0xFF5A0A), secondary = c(0xE7B0DE), tertiary = c(0xB57BDB), error = c(0xFF7A8A),
        primaryContainer = c(0x400849), secondaryContainer = c(0x5A2A62),
    ),
    light = PaletteVariant(
        background = c(0xFBF1F8), text = c(0x2A0530), textMuted = c(0x6E4A75), outline = c(0x9A7AA0),
        primary = c(0x400849), secondary = c(0xC83F00), tertiary = c(0x9C4A91), error = c(0xB3261E),
        primaryContainer = c(0xE7B0DE), secondaryContainer = c(0xFFD9C7),
    ),
)

private val Catppuccin = ThemePalette(
    name = "Catppuccin",
    variants = "Mocha · Latte",
    dark = PaletteVariant(
        background = c(0x1E1E2E), text = c(0xCDD6F4), textMuted = c(0xA6ADC8), outline = c(0x6C7086),
        primary = c(0xCBA6F7), secondary = c(0x89B4FA), tertiary = c(0x94E2D5), error = c(0xF38BA8),
    ),
    light = PaletteVariant(
        background = c(0xEFF1F5), text = c(0x4C4F69), textMuted = c(0x5C5F77), outline = c(0x6C6F85),
        primary = c(0x8839EF), secondary = c(0x1E66F5), tertiary = c(0x179299), error = c(0xD20F39),
    ),
)

private val Dracula = ThemePalette(
    name = "Dracula",
    variants = "Dark only",
    dark = PaletteVariant(
        background = c(0x282A36), text = c(0xF8F8F2), outline = c(0x6272A4),
        primary = c(0xBD93F9), secondary = c(0xFF79C6), tertiary = c(0x8BE9FD), error = c(0xFF5555),
    ),
    light = null,
)

private val Everforest = ThemePalette(
    name = "Everforest",
    variants = "Medium dark · Medium light",
    dark = PaletteVariant(
        background = c(0x2D353B), text = c(0xD3C6AA), textMuted = c(0x9DA9A0), outline = c(0x7A8478),
        primary = c(0xA7C080), secondary = c(0x83C092), tertiary = c(0xDBBC7F), error = c(0xE67E80),
    ),
    light = PaletteVariant(
        background = c(0xFDF6E3), text = c(0x5C6A72), textMuted = c(0x939F91), outline = c(0x829181),
        primary = c(0x8DA101), secondary = c(0x35A77C), tertiary = c(0x3A94C5), error = c(0xF85552),
    ),
)

private val Gruvbox = ThemePalette(
    name = "Gruvbox",
    variants = "Dark · Light",
    dark = PaletteVariant(
        background = c(0x282828), text = c(0xEBDBB2), textMuted = c(0xA89984), outline = c(0x928374),
        primary = c(0xFE8019), secondary = c(0xB8BB26), tertiary = c(0x8EC07C), error = c(0xFB4934),
    ),
    light = PaletteVariant(
        background = c(0xFBF1C7), text = c(0x3C3836), textMuted = c(0x7C6F64), outline = c(0x928374),
        primary = c(0xAF3A03), secondary = c(0x79740E), tertiary = c(0x427B58), error = c(0x9D0006),
    ),
)

private val Nord = ThemePalette(
    name = "Nord",
    variants = "Polar Night · Snow Storm",
    dark = PaletteVariant(
        background = c(0x2E3440), text = c(0xECEFF4), textMuted = c(0xD8DEE9), outline = c(0x4C566A),
        primary = c(0x88C0D0), secondary = c(0x81A1C1), tertiary = c(0xB48EAD), error = c(0xBF616A),
    ),
    light = PaletteVariant(
        background = c(0xECEFF4), text = c(0x2E3440), outline = c(0x4C566A),
        primary = c(0x5E81AC), secondary = c(0x81A1C1), tertiary = c(0xB48EAD), error = c(0xBF616A),
    ),
)

private val OneDark = ThemePalette(
    name = "OneDark",
    variants = "One Dark · One Light",
    dark = PaletteVariant(
        background = c(0x282C34), text = c(0xABB2BF), textMuted = c(0x848B98), outline = c(0x5C6370),
        primary = c(0x61AFEF), secondary = c(0xC678DD), tertiary = c(0x98C379), error = c(0xE86671),
    ),
    light = PaletteVariant(
        background = c(0xFAFAFA), text = c(0x383A42), textMuted = c(0x818387), outline = c(0xA0A1A7),
        primary = c(0x4078F2), secondary = c(0xA626A4), tertiary = c(0x50A14F), error = c(0xE45649),
    ),
)

// Poimandres has no red; the error colour is Material's standard soft red for dark themes.
private val Poimandres = ThemePalette(
    name = "Poimandres",
    variants = "Dark only",
    dark = PaletteVariant(
        background = c(0x1B1E28), text = c(0xE4F0FB), textMuted = c(0xA6ACCD), outline = c(0x767C9D),
        primary = c(0x5DE4C7), secondary = c(0xADD7FF), tertiary = c(0xFCC5E9), error = c(0xF2B8B5),
    ),
    light = null,
)

private val RosePine = ThemePalette(
    name = "Rosé Pine",
    variants = "Main · Dawn",
    dark = PaletteVariant(
        background = c(0x191724), text = c(0xE0DEF4), textMuted = c(0x908CAA), outline = c(0x6E6A86),
        primary = c(0xC4A7E7), secondary = c(0x9CCFD8), tertiary = c(0xEBBCBA), error = c(0xEB6F92),
    ),
    light = PaletteVariant(
        background = c(0xFAF4ED), text = c(0x464261), textMuted = c(0x797593), outline = c(0x9893A5),
        primary = c(0x907AA9), secondary = c(0x286983), tertiary = c(0xD7827E), error = c(0xB4637A),
    ),
)

private val TokyoNight = ThemePalette(
    name = "TokyoNight",
    variants = "Night · Day",
    dark = PaletteVariant(
        background = c(0x1A1B26), text = c(0xC0CAF5), textMuted = c(0xA9B1D6), outline = c(0x545C7E),
        primary = c(0x7AA2F7), secondary = c(0xBB9AF7), tertiary = c(0x7DCFFF), error = c(0xF7768E),
    ),
    light = PaletteVariant(
        background = c(0xE1E2E7), text = c(0x3760BF), textMuted = c(0x6172B0), outline = c(0x8990B3),
        primary = c(0x2E7DE9), secondary = c(0x9854F1), tertiary = c(0x118C74), error = c(0xF52A65),
    ),
)

private val AyuMirage = ThemePalette(
    name = "Ayu Mirage",
    variants = "Dark only",
    dark = PaletteVariant(
        background = c(0x1F2430), text = c(0xCCCAC2), textMuted = c(0x707A8C), outline = c(0x6E7C8F),
        primary = c(0xFFCC66), secondary = c(0x73D0FF), tertiary = c(0x95E6CB), error = c(0xFF6666),
    ),
    light = null,
)

private val Bamboo = ThemePalette(
    name = "Bamboo",
    variants = "Vulgaris · Light",
    dark = PaletteVariant(
        background = c(0x252623), text = c(0xF1E9D2), textMuted = c(0x838781), outline = c(0x5B5E5A),
        primary = c(0x8FB573), secondary = c(0x57A5E5), tertiary = c(0xDBB651), error = c(0xE75A7C),
    ),
    light = PaletteVariant(
        background = c(0xFAFAE0), text = c(0x3A4238), textMuted = c(0x838781), outline = c(0x838781),
        primary = c(0x27850B), secondary = c(0x1745D5), tertiary = c(0xA77B00), error = c(0xC72A3C),
    ),
)

private val Cobalt2 = ThemePalette(
    name = "Cobalt2",
    variants = "Dark only",
    dark = PaletteVariant(
        background = c(0x193549), text = c(0xFFFFFF),
        primary = c(0xFFC600), secondary = c(0x0088FF), tertiary = c(0x80FCFF), error = c(0xFF628C),
    ),
    light = null,
)

// Horizon Bright's own error colour is not in the sources read, so the light variant reuses the dark one.
private val Horizon = ThemePalette(
    name = "Horizon",
    variants = "Dark · Bright",
    dark = PaletteVariant(
        background = c(0x1C1E26), text = c(0xD5D8DA),
        primary = c(0xE95378), secondary = c(0x26BBD9), tertiary = c(0x29D398), error = c(0xF43E5C),
    ),
    light = PaletteVariant(
        background = c(0xFDF0ED), text = c(0x06060C),
        primary = c(0xE84A72), secondary = c(0x26BBD9), tertiary = c(0x1EB980), error = c(0xF43E5C),
    ),
)

private val Kanagawa = ThemePalette(
    name = "Kanagawa",
    variants = "Wave · Lotus",
    dark = PaletteVariant(
        background = c(0x1F1F28), text = c(0xDCD7BA), textMuted = c(0xC8C093), outline = c(0x727169),
        primary = c(0x7E9CD8), secondary = c(0x957FB8), tertiary = c(0x98BB6C), error = c(0xE46876),
    ),
    light = PaletteVariant(
        background = c(0xF2ECBC), text = c(0x545464), outline = c(0x8A8980),
        primary = c(0x4D699B), secondary = c(0x624C83), tertiary = c(0x6F894E), error = c(0xC84053),
    ),
)

private val MaterialOcean = ThemePalette(
    name = "Material Ocean",
    variants = "Dark only",
    dark = PaletteVariant(
        background = c(0x0F111A), text = c(0x8F93A2), outline = c(0x464B5D),
        primary = c(0x82AAFF), secondary = c(0xC792EA), tertiary = c(0xC3E88D), error = c(0xFF5370),
    ),
    light = null,
)

private val Melange = ThemePalette(
    name = "Melange",
    variants = "Dark · Light",
    dark = PaletteVariant(
        background = c(0x292522), text = c(0xECE1D7), textMuted = c(0xC1A78E), outline = c(0x867462),
        primary = c(0xA3A9CE), secondary = c(0xCF9BC2), tertiary = c(0x85B695), error = c(0xD47766),
    ),
    light = PaletteVariant(
        background = c(0xF1F1F1), text = c(0x54433A), textMuted = c(0x7D6658), outline = c(0xA98A78),
        primary = c(0x465AA4), secondary = c(0x904180), tertiary = c(0x3A684A), error = c(0xBF0021),
    ),
)

private val NightOwl = ThemePalette(
    name = "Night Owl",
    variants = "Night Owl · Light Owl",
    dark = PaletteVariant(
        background = c(0x011627), text = c(0xD6DEEB), textMuted = c(0x5F7E97), outline = c(0x5F7E97),
        primary = c(0x82AAFF), secondary = c(0xC792EA), tertiary = c(0x21C7A8), error = c(0xEF5350),
    ),
    light = PaletteVariant(
        background = c(0xFBFBFB), text = c(0x403F53), outline = c(0x93A1A1),
        primary = c(0x288ED7), secondary = c(0xD6438A), tertiary = c(0x2AA298), error = c(0xE64D49),
    ),
)

private val PaperColor = ThemePalette(
    name = "PaperColor",
    variants = "Dark · Light",
    dark = PaletteVariant(
        background = c(0x1C1C1C), text = c(0xD0D0D0), textMuted = c(0x808080), outline = c(0x808080),
        primary = c(0x5FAFD7), secondary = c(0xAF87D7), tertiary = c(0x5FAF00), error = c(0xAF005F),
    ),
    light = PaletteVariant(
        background = c(0xEEEEEE), text = c(0x444444), textMuted = c(0x878787), outline = c(0x878787),
        primary = c(0x005FAF), secondary = c(0x8700AF), tertiary = c(0x008700), error = c(0xAF0000),
    ),
)

private val ShadesOfPurple = ThemePalette(
    name = "Shades of Purple",
    variants = "Dark only",
    dark = PaletteVariant(
        background = c(0x2D2B55), text = c(0xFFFFFF), textMuted = c(0xA599E9),
        primary = c(0xB362FF), secondary = c(0xFAD000), tertiary = c(0x80FCFF), error = c(0xEC3A37),
    ),
    light = null,
)

private val Solarized = ThemePalette(
    name = "Solarized",
    variants = "Dark · Light",
    dark = PaletteVariant(
        background = c(0x002B36), text = c(0x839496), outline = c(0x586E75),
        primary = c(0x268BD2), secondary = c(0x2AA198), tertiary = c(0xB58900), error = c(0xDC322F),
    ),
    light = PaletteVariant(
        background = c(0xFDF6E3), text = c(0x657B83), textMuted = c(0x93A1A1), outline = c(0x93A1A1),
        primary = c(0x268BD2), secondary = c(0x2AA198), tertiary = c(0x6C71C4), error = c(0xDC322F),
    ),
)

private val Sonokai = ThemePalette(
    name = "Sonokai",
    variants = "Default · Dark only",
    dark = PaletteVariant(
        background = c(0x2C2E34), text = c(0xE2E2E3), textMuted = c(0x7F8490), outline = c(0x7F8490),
        primary = c(0x76CCE0), secondary = c(0xB39DF3), tertiary = c(0x9ED072), error = c(0xFC5D7C),
    ),
    light = null,
)

private val SynthWave84 = ThemePalette(
    name = "SynthWave '84",
    variants = "Dark only",
    dark = PaletteVariant(
        background = c(0x262335), text = c(0xFFFFFF), textMuted = c(0x848BBD),
        primary = c(0xFF7EDB), secondary = c(0x03EDF9), tertiary = c(0xFEDE5D), error = c(0xFE4450),
    ),
    light = null,
)

/** Neutral greys only (every channel equal), so Liquid Glass over it has no colour of its own. Not taken from any scheme. */
private val NoColour = ThemePalette(
    name = "No colour",
    variants = "Plain glass · Dark · Light",
    dark = PaletteVariant(
        background = c(0x141414), text = c(0xEDEDED), textMuted = c(0xA3A3A3), outline = c(0x707070),
        primary = c(0xDCDCDC), secondary = c(0xBEBEBE), tertiary = c(0x9E9E9E), error = c(0xF2B8B5),
    ),
    light = PaletteVariant(
        background = c(0xF2F2F2), text = c(0x1C1C1C), textMuted = c(0x5E5E5E), outline = c(0x8C8C8C),
        primary = c(0x3A3A3A), secondary = c(0x5A5A5A), tertiary = c(0x7A7A7A), error = c(0xB3261E),
    ),
)
