package me.rerere.rikkahub.ui.context

import androidx.compose.runtime.compositionLocalOf

/** True inside a chat that can only be read (the hidden chats of the app's AI actions): no editing, regenerating, forking or deleting. */
val LocalReadOnlyChat = compositionLocalOf { false }
