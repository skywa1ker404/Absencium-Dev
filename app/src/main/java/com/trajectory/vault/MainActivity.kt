package com.trajectory.vault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.NavGraphs
import com.trajectory.vault.ui.rememberMainViewModel
import com.trajectory.vault.ui.theme.TrajectoryTheme

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
                TrajectoryTheme(settings = loaded) {
                    DestinationsNavHost(navGraph = NavGraphs.root)
                }
            }
        }
    }
}
