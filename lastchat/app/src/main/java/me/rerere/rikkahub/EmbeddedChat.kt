package me.rerere.rikkahub

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.rerere.rikkahub.data.repository.ConversationRepository
import me.rerere.rikkahub.ui.hooks.readBooleanPreference
import me.rerere.rikkahub.ui.hooks.readStringPreference
import me.rerere.rikkahub.ui.theme.RikkahubTheme
import org.koin.compose.koinInject

/** The chat settings page a short name from the host stands for. */
internal fun settingsScreenFor(name: String?): Screen? = when (name) {
    "settings" -> Screen.Setting
    "models" -> Screen.SettingModels
    "providers" -> Screen.SettingProvider
    "assistants" -> Screen.Assistant
    "search" -> Screen.SettingSearch
    "mcp" -> Screen.SettingMcp
    "skills" -> Screen.SettingSkills()
    "android" -> Screen.SettingAndroidIntegration
    "tts" -> Screen.SettingTTS
    "storage" -> Screen.SettingChatStorage
    "about" -> Screen.SettingAbout
    else -> null
}

/**
 * The chat drawn inside the host app's own window, as one of its layers, instead of in a second activity: opening it is an in-app
 * transition, never a switch to another app. The host owns the theme and the system bars; [dark] and [glass] describe them.
 * With [settingsPage] it opens straight on that settings page, and going back from it leaves the chat.
 */
@Composable
fun EmbeddedLastChat(dark: Boolean, glass: Boolean, settingsPage: String?) {
    val context = LocalContext.current
    remember { LastChatApp.start(context.applicationContext as Application); true }
    CompositionLocalProvider(LocalHostGlass provides glass, LocalEmbeddedDark provides dark) {
        RikkahubTheme {
            val nav = rememberNavController()
            val repo = koinInject<ConversationRepository>()
            var hasReply by remember { mutableStateOf<Boolean?>(null) }
            LaunchedEffect(Unit) { hasReply = withContext(Dispatchers.IO) { repo.hasSuccessfulAssistantReply() } }
            val start = remember {
                Screen.Chat(
                    id = if (context.readBooleanPreference("create_new_conversation_on_start", true)) {
                        java.util.UUID.randomUUID().toString()
                    } else {
                        context.readStringPreference("lastConversationId", java.util.UUID.randomUUID().toString())
                            ?: java.util.UUID.randomUUID().toString()
                    },
                )
            }
            val page = remember(settingsPage) { settingsScreenFor(settingsPage) }
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                ChatAppRoutes(nav, start, hasReply)
            }
            LaunchedEffect(page) {
                // The page replaces the chat on this stack, so there is no chat flash and Back leaves to the host.
                if (page != null) nav.navigate(page) { popUpTo<Screen.Chat> { inclusive = true } }
            }
        }
    }
}
