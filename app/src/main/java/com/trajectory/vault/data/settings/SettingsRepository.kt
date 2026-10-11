package com.trajectory.vault.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.trajectory.vault.data.ai.AiProvider
import com.trajectory.vault.data.ai.ApiStyle
import com.trajectory.vault.logging.DebugLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore by preferencesDataStore(name = "trajectory_settings")

/**
 * Source of truth for [AppSettings]. Reads are a Flow so the UI follows every change,
 * and writes are suspend functions that finish once the value is on disk.
 */
class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    val settings: Flow<AppSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                DebugLogger.e(TAG, "Could not read settings, using defaults", error)
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { it.toAppSettings() }

    suspend fun setOnboardingComplete(value: Boolean) = write(Keys.ONBOARDING_COMPLETE, value)

    suspend fun setDisplayName(value: String) = write(Keys.DISPLAY_NAME, value.trim().take(40))

    suspend fun setThemeMode(value: ThemeMode) = write(Keys.THEME_MODE, value.name)

    suspend fun setColorEngine(value: ColorEngine) = write(Keys.COLOR_ENGINE, value.name)

    suspend fun setAccentColor(argb: Int) = write(Keys.ACCENT_COLOR, argb)

    suspend fun setGlassBlur(dp: Int) = write(
        Keys.GLASS_BLUR,
        dp.coerceIn(AppSettings.MIN_GLASS_BLUR, AppSettings.MAX_GLASS_BLUR),
    )

    suspend fun setAttendanceThreshold(percent: Int) = write(
        Keys.ATTENDANCE_THRESHOLD,
        percent.coerceIn(AppSettings.MIN_ATTENDANCE_THRESHOLD, AppSettings.MAX_ATTENDANCE_THRESHOLD),
    )

    suspend fun setFocusGraceMinutes(minutes: Int) = write(Keys.FOCUS_GRACE_MINUTES, minutes.coerceIn(1, 60))

    suspend fun setExamModeAutoSilence(value: Boolean) = write(Keys.EXAM_MODE_AUTO_SILENCE, value)

    suspend fun setAskAiEnabled(value: Boolean) = write(Keys.ASK_AI_ENABLED, value)

    /** Saves the whole AI connection in one write so the settings never hold half of it. */
    suspend fun setAiConnection(provider: AiProvider, baseUrl: String, model: String, style: ApiStyle) {
        dataStore.edit {
            it[Keys.AI_PROVIDER] = provider.name
            it[Keys.AI_BASE_URL] = baseUrl
            it[Keys.AI_MODEL] = model
            it[Keys.AI_API_STYLE] = style.name
        }
    }

    suspend fun setMomentumTrackerEnabled(value: Boolean) = write(Keys.MOMENTUM_ENABLED, value)

    suspend fun setSemesterManagerEnabled(value: Boolean) = write(Keys.SEMESTER_MANAGER_ENABLED, value)

    suspend fun setLowStorageCompressorEnabled(value: Boolean) = write(Keys.LOW_STORAGE_COMPRESSOR_ENABLED, value)

    private suspend fun <T> write(key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }

    private object Keys {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val COLOR_ENGINE = stringPreferencesKey("color_engine")
        val ACCENT_COLOR = intPreferencesKey("accent_color")
        val GLASS_BLUR = intPreferencesKey("glass_blur_dp")
        val ATTENDANCE_THRESHOLD = intPreferencesKey("attendance_threshold_percent")
        val FOCUS_GRACE_MINUTES = intPreferencesKey("focus_grace_minutes")
        val EXAM_MODE_AUTO_SILENCE = booleanPreferencesKey("exam_mode_auto_silence")
        val ASK_AI_ENABLED = booleanPreferencesKey("ask_ai_enabled")
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val AI_BASE_URL = stringPreferencesKey("ai_base_url")
        val AI_MODEL = stringPreferencesKey("ai_model")
        val AI_API_STYLE = stringPreferencesKey("ai_api_style")
        val MOMENTUM_ENABLED = booleanPreferencesKey("momentum_tracker_enabled")
        val SEMESTER_MANAGER_ENABLED = booleanPreferencesKey("semester_manager_enabled")
        val LOW_STORAGE_COMPRESSOR_ENABLED = booleanPreferencesKey("low_storage_compressor_enabled")
    }

    private fun Preferences.toAppSettings(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            onboardingComplete = this[Keys.ONBOARDING_COMPLETE] ?: defaults.onboardingComplete,
            displayName = this[Keys.DISPLAY_NAME] ?: defaults.displayName,
            themeMode = enumOrDefault(this[Keys.THEME_MODE], defaults.themeMode),
            colorEngine = enumOrDefault(this[Keys.COLOR_ENGINE], defaults.colorEngine),
            accentColor = this[Keys.ACCENT_COLOR] ?: defaults.accentColor,
            glassBlurDp = this[Keys.GLASS_BLUR] ?: defaults.glassBlurDp,
            attendanceThresholdPercent = this[Keys.ATTENDANCE_THRESHOLD] ?: defaults.attendanceThresholdPercent,
            focusGraceMinutes = this[Keys.FOCUS_GRACE_MINUTES] ?: defaults.focusGraceMinutes,
            examModeAutoSilence = this[Keys.EXAM_MODE_AUTO_SILENCE] ?: defaults.examModeAutoSilence,
            askAiEnabled = this[Keys.ASK_AI_ENABLED] ?: defaults.askAiEnabled,
            aiProvider = enumOrDefault(this[Keys.AI_PROVIDER], defaults.aiProvider),
            aiBaseUrl = this[Keys.AI_BASE_URL] ?: defaults.aiBaseUrl,
            aiModel = this[Keys.AI_MODEL] ?: defaults.aiModel,
            aiApiStyle = enumOrDefault(this[Keys.AI_API_STYLE], defaults.aiApiStyle),
            momentumTrackerEnabled = this[Keys.MOMENTUM_ENABLED] ?: defaults.momentumTrackerEnabled,
            semesterManagerEnabled = this[Keys.SEMESTER_MANAGER_ENABLED] ?: defaults.semesterManagerEnabled,
            lowStorageCompressorEnabled =
                this[Keys.LOW_STORAGE_COMPRESSOR_ENABLED] ?: defaults.lowStorageCompressorEnabled,
        )
    }

    private inline fun <reified E : Enum<E>> enumOrDefault(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default

    private companion object {
        const val TAG = "Settings"
    }
}
