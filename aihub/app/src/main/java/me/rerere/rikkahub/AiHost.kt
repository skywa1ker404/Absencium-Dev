package me.rerere.rikkahub

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import me.rerere.ai.core.Tool

/**
 * What the app that embeds the chat (Absencium) plugs in. Everything is optional; without it the chat uses its own theme.
 */
object AiHost {
    /**
     * Wraps every chat screen in the host's theme engine. The lambda receives the content and says whether the theme is dark,
     * so the chat's own extra colours (code, message bubbles) follow.
     */
    var theme: (@Composable (content: @Composable (dark: Boolean) -> Unit) -> Unit)? = null

    /**
     * What one assistant allows of the host's tools: tools it switched [off], tools that run by themselves ([auto]) and tools that
     * always ask first ([ask]). Anything in neither list keeps its own default (looking things up runs by itself, changes ask first).
     */
    data class ToolPolicy(val off: Set<String> = emptySet(), val auto: Set<String> = emptySet(), val ask: Set<String> = emptySet())

    /** One of the host's tools as listed in the assistant's Tools page, under its category folder. */
    data class ToolInfo(val name: String, val title: String, val description: String, val category: String, val asksByDefault: Boolean)

    /** Extra tools the host app offers the chat (Absencium's attendance, timetable, notes and so on). Asked again for every request. */
    var tools: (ToolPolicy, String) -> List<Tool> = { _, _ -> emptyList() }

    /**
     * Chats the host's AI makes for its own actions (a note written from Add Entry, an outline imported, and so on) belong to this
     * reserved assistant id. They are left out of every normal chat list, listed in the AI actions folder, and cannot be written to.
     */
    val ACTIONS_ASSISTANT_ID: kotlin.uuid.Uuid = kotlin.uuid.Uuid.parse("5b0b5b0e-ab5c-4c1e-9b2d-ab5e1c1a0001")

    /** Which of the host's tools an action chat may use, and which of those run by themselves (nobody is there to approve). */
    var actionToolPolicy: () -> ToolPolicy = { ToolPolicy() }

    /** What a hidden action chat ended with: its id, the assistant's last words, or the reason it failed. */
    data class TaskResult(val chatId: String, val reply: String, val error: String?)

    /** Every tool the host offers, for the assistant's Tools page (grouped by category, searchable). */
    var toolCatalog: () -> List<ToolInfo> = { emptyList() }

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

    /** Opens the host's own AI settings page. When set, the chat has no settings hub of its own: its gear goes here. */
    var openAppSettings: ((android.content.Context) -> Unit)? = null

    /** Three invisible characters the host puts between its fixed instructions and the request itself; the chat draws only what follows. */
    const val TASK_MARK = "\u2063\u2063\u2063"

    /** Leaves the chat for the host when Back is pressed on the first page of the chat's own stack (nothing is left to pop). */
    var onExit: (() -> Unit)? = null

    /** Whether the Imagine (image generation) entry shows in the chat drawer. Null: always shown. */
    var imagineEnabled: kotlinx.coroutines.flow.Flow<Boolean>? = null

    var describe: (toolName: String, arguments: String) -> Pair<String, String>? = { _, _ -> null }

    /** Something in the host's Vault that can be linked to a message. [reference] is the line put into the message; nothing is uploaded. */
    data class VaultLink(val kind: String, val title: String, val subtitle: String, val reference: String, val subject: String = "")

    /** What the Vault option of the attach menu offers. When null, the menu has no Vault option. */
    var vaultLinks: (suspend () -> List<VaultLink>)? = null

    /** Text the app wants in the chat box next time the chat shows (a note the student asked about); the box takes it once. */
    val pendingDraft = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    /**
     * The ids of the hidden action chats that made or changed something the host can show (the Open button). When null the chat has no
     * AI actions folder; the folder itself lists the chats of [ACTIONS_ASSISTANT_ID].
     */
    var actionTargets: kotlinx.coroutines.flow.Flow<Set<String>>? = null

    /** Shows what an action chat made or changed in the host (and leaves the chat). */
    var openActionTarget: (android.content.Context, String) -> Unit = { _, _ -> }

    /** Forgets what the host kept about an action chat (called when the chat is deleted). */
    var forgetAction: (String) -> Unit = {}

    /** The steps of a plan-like tool call, one line each, so the approval card can show a tick box per step. Null when the call has none. */
    var planSteps: (toolName: String, arguments: String) -> List<String>? = { _, _ -> null }

    /** Tells the host which steps (0-based) the student unticked, just before the call is approved. */
    var skipPlanSteps: (toolName: String, arguments: String, skipped: Set<Int>) -> Unit = { _, _, _ -> }
}

/** True while the host runs Liquid Glass: the chat then lets its backgrounds and panes show the host's wallpaper through. */
val LocalHostGlass = staticCompositionLocalOf { false }

/** Set while the chat is drawn inside the host app's own window (not its own activity): the host's dark flag. The host then already owns the theme and the system bars. */
val LocalEmbeddedDark = staticCompositionLocalOf<Boolean?> { null }
