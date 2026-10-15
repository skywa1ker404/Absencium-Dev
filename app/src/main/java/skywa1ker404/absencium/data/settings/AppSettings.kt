package skywa1ker404.absencium.data.settings

import skywa1ker404.absencium.data.ai.AiProvider
import skywa1ker404.absencium.data.ai.ApiStyle

enum class ThemeMode {
    LIGHT,

    /** A regular dark theme with tinted dark grey surfaces. */
    DARK,

    /** Pitch-black surfaces (#0D0D0D) that switch off OLED pixels. */
    DARK_OLED,
    SYSTEM_AUTO,
}

enum class ColorEngine {
    /** The built-in olive and grey palette, in light and dark. Works on every Android version. */
    DEFAULT_PALETTE,

    /** The colours of the wallpaper chosen in the system settings. Needs Android 12 (API 31) or newer. */
    MONET_WALLPAPER,

    /** One of the schemes on the Presets page, see [AppSettings.themePreset]. */
    PRESET,
}

/** Every user setting that is not stored in the database. */
data class AppSettings(
    /** The first-launch intro has been shown (or skipped). It plays once, before the setup wizard. */
    val introSeen: Boolean = false,
    val onboardingComplete: Boolean = false,
    /** Shown on the profile avatar and greeting. Empty until the user sets one. */
    val displayName: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM_AUTO,
    val colorEngine: ColorEngine = ColorEngine.MONET_WALLPAPER,
    /** Used while [colorEngine] is [ColorEngine.PRESET]; remembered otherwise. */
    val themePreset: ThemePreset = ThemePreset.CATPPUCCIN,
    /** Liquid Glass on top of whichever theme is active (blur and refraction need Android 12). */
    val liquidGlass: Boolean = false,
    /** With Liquid Glass on: also frost the profile, search, Add Entry, import and other overlays. Off keeps them solid. */
    val frostedOverlays: Boolean = true,
    /** Blur radius of the Liquid Glass surfaces, in dp. */
    val glassBlurDp: Int = DEFAULT_GLASS_BLUR,
    /** Starts at the level recommended for the phone; the user can change it at any time. */
    val visualQuality: VisualQuality = VisualQuality.QUALITY,
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
    /** Saturdays count as holidays: no lectures are expected and the timetable hides the day. */
    val saturdayHoliday: Boolean = true,
    /** Sundays count as holidays: no lectures are expected and the timetable hides the day. */
    val sundayHoliday: Boolean = true,
) {
    /** The API style actually used for requests. */
    val effectiveAiStyle: ApiStyle
        get() = if (aiProvider == AiProvider.CUSTOM) aiApiStyle else aiProvider.style

    /** Provider, URL and model are filled in. Whether a key is needed and saved is checked separately. */
    val aiConnectionSaved: Boolean
        get() = aiBaseUrl.isNotBlank() && aiModel.isNotBlank()

    companion object {
        const val DEFAULT_GLASS_BLUR = 4
        const val MIN_GLASS_BLUR = 0
        const val MAX_GLASS_BLUR = 30
        const val MIN_ATTENDANCE_THRESHOLD = 60
        const val MAX_ATTENDANCE_THRESHOLD = 85
    }
}
