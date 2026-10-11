package com.trajectory.vault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.graphics.Color
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.NavGraphs
import com.trajectory.vault.ui.onboarding.OnboardingScreen
import com.trajectory.vault.ui.rememberMainViewModel
import com.trajectory.vault.ui.theme.TrajectoryTheme
import com.trajectory.vault.ui.theme.isDarkTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel = rememberMainViewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            // Wait for the first read of the saved settings so the app never flashes the wrong theme.
            val loaded = settings
            if (loaded != null) {
                // Status and navigation bar icons must follow the app's theme, not only the phone's.
                val dark = isDarkTheme(loaded)
                DisposableEffect(dark) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                        navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    )
                    onDispose { }
                }
                TrajectoryTheme(settings = loaded) {
                    // First run: the wizard is shown until its last step is finished.
                    if (loaded.onboardingComplete) {
                        DestinationsNavHost(navGraph = NavGraphs.root)
                    } else {
                        OnboardingScreen()
                    }
                }
            }
        }
    }
}
