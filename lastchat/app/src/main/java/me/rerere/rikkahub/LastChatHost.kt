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
    /** Chips for an empty chat, built by the host from its own data. */
    var suggestions: suspend () -> List<String> = { emptyList() }

    /** The host's profile name. When set, it replaces the chat's own user name everywhere (drawer, greeting, {{user}}). */
    var userName: kotlinx.coroutines.flow.Flow<String>? = null

    /** The host's profile avatar, drawn for the user in the chat drawer. When set, the avatar is not editable inside the chat. */
    var userAvatar: (@Composable (name: String, modifier: androidx.compose.ui.Modifier) -> Unit)? = null

    var describe: (toolName: String, arguments: String) -> Pair<String, String>? = { _, _ -> null }

    /** The steps of a plan-like tool call, one line each, so the approval card can show a tick box per step. Null when the call has none. */
    var planSteps: (toolName: String, arguments: String) -> List<String>? = { _, _ -> null }

    /** Tells the host which steps (0-based) the student unticked, just before the call is approved. */
    var skipPlanSteps: (toolName: String, arguments: String, skipped: Set<Int>) -> Unit = { _, _, _ -> }
}

/** True while the host runs Liquid Glass: the chat then lets its backgrounds and panes show the host's wallpaper through. */
val LocalHostGlass = staticCompositionLocalOf { false }
