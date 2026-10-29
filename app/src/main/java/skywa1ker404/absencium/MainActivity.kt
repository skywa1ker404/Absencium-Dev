package skywa1ker404.absencium

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.graphics.Color
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.ReadingScreenDestination
import skywa1ker404.absencium.ui.vault.ReadingMiniPlayer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import skywa1ker404.absencium.display.RefreshRatePreference
import skywa1ker404.absencium.ui.splash.AppSplash
import skywa1ker404.absencium.ui.components.UndoHost
import skywa1ker404.absencium.ui.about.IntroTourContent
import skywa1ker404.absencium.ui.navigation.ScreenTransitions
import skywa1ker404.absencium.ui.onboarding.OnboardingScreen
import skywa1ker404.absencium.ui.settings.rememberSettingsViewModel
import skywa1ker404.absencium.ui.rememberMainViewModel
import skywa1ker404.absencium.ui.theme.AbsenciumTheme
import skywa1ker404.absencium.ui.theme.isDarkTheme

/** Which full-screen phase the app is in: the first-run setup, the feature tour that follows it, or the app itself. */
private enum class AppPhase { SETUP, TOUR, APP }

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
                  val navController = androidx.navigation.compose.rememberNavController()
                  Box(modifier = Modifier.fillMaxSize()) {
                    // First run: the setup, then the feature tour, then the app. Each hands over to the next with a
                    // smooth fade (the old screen fades out first, then the new one fades in), never a cut.
                    val phase = when {
                        !loaded.onboardingComplete -> AppPhase.SETUP
                        !loaded.introSeen -> AppPhase.TOUR
                        else -> AppPhase.APP
                    }
                    AnimatedContent(
                        targetState = phase,
                        transitionSpec = {
                            fadeIn(tween(durationMillis = 380, delayMillis = 320)) togetherWith fadeOut(tween(durationMillis = 320))
                        },
                        label = "appPhase",
                    ) { current ->
                        when (current) {
                            AppPhase.SETUP -> OnboardingScreen()
                            AppPhase.TOUR -> {
                                val settingsViewModel = rememberSettingsViewModel()
                                IntroTourContent(onFinish = settingsViewModel::setIntroSeen)
                            }
                            AppPhase.APP -> DestinationsNavHost(
                                navGraph = NavGraphs.root,
                                defaultTransitions = ScreenTransitions,
                                navController = navController,
                            )
                        }
                    }
                    if (phase == AppPhase.APP) {
                        val mini by container.readingMini.collectAsStateWithLifecycle()
                        ReadingMiniPlayer(
                            mini = mini,
                            onOpen = { id ->
                                container.readingMini.value = null
                                navController.navigate(ReadingScreenDestination(noteId = id).route)
                            },
                            onDismiss = { container.readingMini.value = null },
                        )
                    }
                    if (phase == AppPhase.APP) UndoHost(container.undoBus)
                    if (phase == AppPhase.APP) skywa1ker404.absencium.ui.components.AiErrorHost(container.aiClient.failures)
                    // The in-app splash sits on top once per launch, in the chosen theme, then fades into the app.
                    var showSplash by rememberSaveable { mutableStateOf(true) }
                    if (showSplash) AppSplash(onFinished = { showSplash = false })
                  }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        refreshRate.start()
        skywa1ker404.absencium.logging.FrameMonitor.start()
    }

    override fun onStop() {
        refreshRate.stop()
        skywa1ker404.absencium.logging.FrameMonitor.stop()
        super.onStop()
    }
}
