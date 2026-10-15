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
 */
fun ThemePreset.palette(): ThemePalette = when (this) {
    ThemePreset.CATPPUCCIN -> Catppuccin
    ThemePreset.DRACULA -> Dracula
    ThemePreset.GRUVBOX -> Gruvbox
    ThemePreset.NORD -> Nord
    ThemePreset.ONE_DARK -> OneDark
    ThemePreset.POIMANDRES -> Poimandres
    ThemePreset.ROSE_PINE -> RosePine
    ThemePreset.TOKYO_NIGHT -> TokyoNight
}

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
