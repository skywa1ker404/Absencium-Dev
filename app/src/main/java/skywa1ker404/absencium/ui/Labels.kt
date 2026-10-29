package skywa1ker404.absencium.ui

import skywa1ker404.absencium.data.settings.ColorEngine
import skywa1ker404.absencium.data.settings.ThemeMode
import skywa1ker404.absencium.data.settings.VisualQuality

fun ThemeMode.label(): String = when (this) {
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
    ThemeMode.DARK_OLED -> "OLED Dark"
    ThemeMode.SYSTEM_AUTO -> "System Auto"
}

fun ThemeMode.description(): String = when (this) {
    ThemeMode.LIGHT -> "High contrast for daytime and outdoor use"
    ThemeMode.DARK -> "Dark grey surfaces that are easy on the eyes"
    ThemeMode.DARK_OLED -> "Pitch-black background for night study"
    ThemeMode.SYSTEM_AUTO -> "Follows your phone's light or dark setting"
}

fun ColorEngine.label(): String = when (this) {
    ColorEngine.DEFAULT_PALETTE -> "Default Theme"
    ColorEngine.MONET_WALLPAPER -> "Monet"
    ColorEngine.PRESET -> "Presets"
}

fun ColorEngine.description(): String = when (this) {
    ColorEngine.DEFAULT_PALETTE -> "Calm greens and earth tones"
    ColorEngine.MONET_WALLPAPER -> "Colours from your wallpaper (Material You)"
    ColorEngine.PRESET -> "Catppuccin, Dracula, Nord and more"
}

const val REQUIRES_ANDROID_12 = "Not supported by your Android version (needs Android 12 or newer)"

fun VisualQuality.label(): String = when (this) {
    VisualQuality.PERFORMANCE -> "Performance mode"
    VisualQuality.LOW -> "Low quality mode"
    VisualQuality.QUALITY -> "Quality mode"
}

fun VisualQuality.description(): String = when (this) {
    VisualQuality.PERFORMANCE -> "Smoothest. Solid bars and calmer motion."
    VisualQuality.LOW -> "Keeps blur and colour. No shader effects."
    VisualQuality.QUALITY -> "The full look: liquid glass and springy motion."
}
