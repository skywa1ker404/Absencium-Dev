package com.trajectory.vault.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trajectory.vault.AppContainer
import com.trajectory.vault.appContainer
import com.trajectory.vault.data.ai.AiProvider
import com.trajectory.vault.data.ai.ApiStyle
import com.trajectory.vault.data.settings.AppSettings
import com.trajectory.vault.data.settings.ColorEngine
import com.trajectory.vault.data.settings.SettingsRepository
import com.trajectory.vault.data.settings.ThemeMode
import com.trajectory.vault.logging.DebugLogger
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setThemeMode(mode: ThemeMode) = launch { repository.setThemeMode(mode) }

    fun setColorEngine(engine: ColorEngine) = launch { repository.setColorEngine(engine) }

    fun setAccentColor(argb: Int) = launch { repository.setAccentColor(argb) }

    fun setGlassBlur(dp: Int) = launch { repository.setGlassBlur(dp) }

    fun setAttendanceThreshold(percent: Int) = launch { repository.setAttendanceThreshold(percent) }

    fun setFocusGraceMinutes(minutes: Int) = launch { repository.setFocusGraceMinutes(minutes) }

    fun setExamModeAutoSilence(value: Boolean) = launch { repository.setExamModeAutoSilence(value) }

    fun setAskAiEnabled(value: Boolean) = launch { repository.setAskAiEnabled(value) }

    fun setAiConnection(provider: AiProvider, baseUrl: String, model: String, style: ApiStyle) = launch {
        repository.setAiConnection(provider, baseUrl, model, style)
        DebugLogger.i("AI", "AI connection saved: provider=${provider.name} url=$baseUrl model=$model style=${style.name}")
    }

    fun setMomentumTrackerEnabled(value: Boolean) = launch { repository.setMomentumTrackerEnabled(value) }

    fun setSemesterManagerEnabled(value: Boolean) = launch { repository.setSemesterManagerEnabled(value) }

    fun setLowStorageCompressorEnabled(value: Boolean) =
        launch { repository.setLowStorageCompressorEnabled(value) }

    fun completeOnboarding() = launch {
        repository.setOnboardingComplete(true)
        DebugLogger.i("Onboarding", "Onboarding finished")
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}

private class SettingsViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SettingsViewModel(container.settingsRepository) as T
}

@Composable
fun rememberSettingsViewModel(): SettingsViewModel {
    val container = LocalContext.current.appContainer
    return viewModel(factory = SettingsViewModelFactory(container))
}
