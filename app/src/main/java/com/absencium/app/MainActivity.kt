package com.absencium.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.graphics.Color
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.NavGraphs
import com.absencium.app.display.RefreshRatePreference
import com.absencium.app.ui.about.IntroTourContent
import com.absencium.app.ui.onboarding.OnboardingScreen
import com.absencium.app.ui.settings.rememberSettingsViewModel
import com.absencium.app.ui.rememberMainViewModel
import com.absencium.app.ui.theme.AbsenciumTheme
import com.absencium.app.ui.theme.isDarkTheme

class MainActivity : ComponentActivity() {

    private val refreshRate by lazy { RefreshRatePreference(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        // The splash screen stays up until the saved settings are read, so the first frame is already in the
        // right theme instead of an empty window.
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        val container = applicationContext.appContainer
        splash.setKeepOnScreenCondition { container.settingsState.value == null }
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
                AbsenciumTheme(settings = loaded) {
                    // First run: the wizard is shown until its last step is finished.
                    if (loaded.onboardingComplete) {
                        DestinationsNavHost(navGraph = NavGraphs.root)
                    } else if (!loaded.introSeen) {
                        // The very first launch: the intro plays, then the setup wizard.
                        val settingsViewModel = rememberSettingsViewModel()
                        IntroTourContent(onFinish = settingsViewModel::setIntroSeen)
                    } else {
                        OnboardingScreen()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        refreshRate.start()
    }

    override fun onStop() {
        refreshRate.stop()
        super.onStop()
    }
}
