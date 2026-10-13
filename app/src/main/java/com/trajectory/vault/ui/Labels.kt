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
    ColorEngine.SOLID_ACCENT -> "Solid Accent"
    ColorEngine.MONET_WALLPAPER -> "Monet Wallpaper"
    ColorEngine.LIQUID_GLASS -> "Liquid Glass"
}

fun ColorEngine.description(): String = when (this) {
    ColorEngine.SOLID_ACCENT -> "A full palette built from one accent color you choose"
    ColorEngine.MONET_WALLPAPER -> "Colors taken from your wallpaper (Material You)"
    ColorEngine.LIQUID_GLASS -> "Frosted glass bars with colors taken from your wallpaper (Material You)"
}

const val REQUIRES_ANDROID_12 = "Needs Android 12 or newer"

fun VisualQuality.label(): String = when (this) {
    VisualQuality.LOW -> "Low"
    VisualQuality.MID -> "Mid"
    VisualQuality.HIGH -> "High"
    VisualQuality.ULTRA -> "Ultra"
}

fun VisualQuality.description(): String = when (this) {
    VisualQuality.LOW -> "Fastest. Solid bars instead of blur, no tab slide and calmer motion. Best for phones with 4 GB of RAM or less."
    VisualQuality.MID -> "Lighter blur without grain, gentler motion. A balance for mid-range phones."
    VisualQuality.HIGH -> "The full look: frosted glass blur and springy animations."
    VisualQuality.ULTRA -> "High plus a lit glass edge and extra grain. Uses the most graphics power."
}
