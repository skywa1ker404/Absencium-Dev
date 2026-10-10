package com.trajectory.vault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trajectory.vault.logging.DebugLogger

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LoggerStatus(logPath = DebugLogger.logFilePath)
                }
            }
        }
    }
}

@Composable
private fun LoggerStatus(logPath: String?) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(text = "Trajectory", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Debug log",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = logPath ?: "Logger is not initialized",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
