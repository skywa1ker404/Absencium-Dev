package com.trajectory.vault.ui

import com.trajectory.vault.data.settings.ColorEngine
import com.trajectory.vault.data.settings.ThemeMode

fun ThemeMode.label(): String = when (this) {
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK_OLED -> "Dark OLED"
    ThemeMode.SYSTEM_AUTO -> "System Auto"
}

fun ThemeMode.description(): String = when (this) {
    ThemeMode.LIGHT -> "High contrast for daytime and outdoor use"
    ThemeMode.DARK_OLED -> "Pitch-black background for night study"
    ThemeMode.SYSTEM_AUTO -> "Follows your phone's light or dark setting"
}

fun ColorEngine.label(): String = when (this) {
    ColorEngine.SOLID_ACCENT -> "Solid Accent"
    ColorEngine.MONET_WALLPAPER -> "Monet Wallpaper"
    ColorEngine.LIQUID_GLASS -> "Liquid Glass"
}

fun ColorEngine.description(): String = when (this) {
    ColorEngine.SOLID_ACCENT -> "A full palette built from one accent color you choose"
    ColorEngine.MONET_WALLPAPER -> "Colors taken from your wallpaper (Material You)"
    ColorEngine.LIQUID_GLASS -> "Frosted translucent surfaces over a soft color backdrop"
}

const val REQUIRES_ANDROID_12 = "Needs Android 12 or newer"
