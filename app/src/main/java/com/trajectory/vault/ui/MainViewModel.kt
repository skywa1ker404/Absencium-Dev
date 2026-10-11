package com.trajectory.vault.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trajectory.vault.AppContainer
import com.trajectory.vault.appContainer
import com.trajectory.vault.data.settings.AppSettings
import com.trajectory.vault.data.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Exposes the settings the app shell needs; null until the first read from disk completes. */
class MainViewModel(repository: SettingsRepository) : ViewModel() {

    val settings: StateFlow<AppSettings?> = repository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

private class MainViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        MainViewModel(container.settingsRepository) as T
}

@Composable
fun rememberMainViewModel(): MainViewModel {
    val container = LocalContext.current.appContainer
    return viewModel(factory = MainViewModelFactory(container))
}
