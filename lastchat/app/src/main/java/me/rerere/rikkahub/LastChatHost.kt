package me.rerere.rikkahub

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import me.rerere.ai.core.Tool

/**
 * What the app that embeds the chat (Absencium) plugs in. Everything is optional; without it the chat uses its own theme.
 */
object LastChatHost {
    /**
     * Wraps every chat screen in the host's theme engine. The lambda receives the content and says whether the theme is dark,
     * so the chat's own extra colours (code, message bubbles) follow.
     */
    var theme: (@Composable (content: @Composable (dark: Boolean) -> Unit) -> Unit)? = null

    /** Extra tools the host app offers the chat (Absencium's attendance, timetable, notes and so on). Asked again for every request. */
    var tools: () -> List<Tool> = { emptyList() }

    /**
     * Words for the approval card of one of the host's tools: a title and the lines of what it is about to do, in plain words.
     * Null for a tool the host does not describe (the card then shows the raw arguments).
     */
    var describe: (toolName: String, arguments: String) -> Pair<String, String>? = { _, _ -> null }
}

/** True while the host runs Liquid Glass: the chat then lets its backgrounds and panes show the host's wallpaper through. */
val LocalHostGlass = staticCompositionLocalOf { false }
