package com.trajectory.vault.ui

import com.trajectory.vault.data.settings.ColorEngine
import com.trajectory.vault.data.settings.ThemeMode
import com.trajectory.vault.data.settings.VisualQuality

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
    ColorEngine.DEFAULT_PALETTE -> "Soft olive and grey tones, in light and dark"
    ColorEngine.MONET_WALLPAPER -> "The colours of the wallpaper you picked in your phone's settings (Material You)"
    ColorEngine.PRESET -> "Catppuccin, Dracula, Nord, Gruvbox and more"
}

const val REQUIRES_ANDROID_12 = "Needs Android 12 or newer"

fun VisualQuality.label(): String = when (this) {
    VisualQuality.PERFORMANCE -> "Performance mode"
    VisualQuality.QUALITY -> "Quality mode"
}

fun VisualQuality.description(): String = when (this) {
    VisualQuality.PERFORMANCE ->
        "Smoothest. Solid bars instead of glass, calmer animations. Every feature stays. Best for phones with 6 GB of RAM or less."
    VisualQuality.QUALITY ->
        "The full look: real liquid glass that blurs and bends what is behind it (lens bending needs Android 13), and springy animations."
}
