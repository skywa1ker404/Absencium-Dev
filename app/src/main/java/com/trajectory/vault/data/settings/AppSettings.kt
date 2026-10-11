package com.trajectory.vault.data.settings

enum class ThemeMode {
    LIGHT,
    DARK_OLED,
    SYSTEM_AUTO,
}

enum class ColorEngine {
    SOLID_ACCENT,

    /** Needs Android 12 (API 31) or newer. */
    MONET_WALLPAPER,

    /** Needs Android 12 (API 31) or newer. */
    LIQUID_GLASS,
}

/** Every user setting that is not stored in the database. [accentColor] is an ARGB int. */
data class AppSettings(
    val onboardingComplete: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM_AUTO,
    val colorEngine: ColorEngine = ColorEngine.SOLID_ACCENT,
    val accentColor: Int = DEFAULT_ACCENT,
    val attendanceThresholdPercent: Int = 75,
    val focusGraceMinutes: Int = 5,
    val examModeAutoSilence: Boolean = false,
    val askAiEnabled: Boolean = false,
    val momentumTrackerEnabled: Boolean = false,
    val semesterManagerEnabled: Boolean = false,
    val lowStorageCompressorEnabled: Boolean = false,
) {
    companion object {
        val DEFAULT_ACCENT: Int = 0xFF6750A4.toInt()
        const val MIN_ATTENDANCE_THRESHOLD = 60
        const val MAX_ATTENDANCE_THRESHOLD = 85
    }
}
