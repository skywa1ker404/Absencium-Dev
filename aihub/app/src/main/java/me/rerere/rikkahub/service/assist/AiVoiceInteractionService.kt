package me.rerere.rikkahub.service.assist

import android.service.voice.VoiceInteractionService

/**
 * Root voice-interaction service. Registering this (plus the session service below)
 * makes Absencium AI selectable as the device "Default digital assistant app".
 *
 * The system binds this service when the user picks Absencium AI as their assistant; the
 * actual per-invocation UI lives in [AiVoiceInteractionSessionService].
 */
class AiVoiceInteractionService : VoiceInteractionService()
