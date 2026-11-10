package me.rerere.rikkahub.service.assist

import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService

/**
 * Factory for assist sessions. The framework calls [onNewSession] each time the
 * assist gesture fires while Absencium AI is the default assistant.
 */
class AiVoiceInteractionSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        return AiVoiceInteractionSession(this)
    }
}
