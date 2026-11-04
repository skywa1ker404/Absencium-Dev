package me.rerere.rikkahub.utils

import android.content.Context

/** The embedded chat has no on-device model runtime, so a failure is shown with its own message. */
@Suppress("UNUSED_PARAMETER")
fun Throwable.toLocalInferenceUserMessage(context: Context): String? = message
