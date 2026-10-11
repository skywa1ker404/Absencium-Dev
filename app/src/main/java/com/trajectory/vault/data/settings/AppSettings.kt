package com.trajectory.vault.data.settings

import com.trajectory.vault.data.ai.AiProvider
import com.trajectory.vault.data.ai.ApiStyle

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
    val aiProvider: AiProvider = AiProvider.OPENAI,
    /** Empty until the user saves a connection. Never has a trailing slash. */
    val aiBaseUrl: String = "",
    val aiModel: String = "",
    /** Only used when [aiProvider] is [AiProvider.CUSTOM]; presets carry their own style. */
    val aiApiStyle: ApiStyle = ApiStyle.OPENAI_COMPATIBLE,
    val momentumTrackerEnabled: Boolean = false,
    val semesterManagerEnabled: Boolean = false,
    val lowStorageCompressorEnabled: Boolean = false,
) {
    /** The API style actually used for requests. */
    val effectiveAiStyle: ApiStyle
        get() = if (aiProvider == AiProvider.CUSTOM) aiApiStyle else aiProvider.style

    /** Provider, URL and model are filled in. Whether a key is needed and saved is checked separately. */
    val aiConnectionSaved: Boolean
        get() = aiBaseUrl.isNotBlank() && aiModel.isNotBlank()

    companion object {
        val DEFAULT_ACCENT: Int = 0xFF6750A4.toInt()
        const val MIN_ATTENDANCE_THRESHOLD = 60
        const val MAX_ATTENDANCE_THRESHOLD = 85
    }
}
