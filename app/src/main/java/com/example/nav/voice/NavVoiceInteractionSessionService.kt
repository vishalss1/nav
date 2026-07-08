package com.example.nav.voice

import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService

class NavVoiceInteractionSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(bundle: Bundle?): VoiceInteractionSession {
        return object : VoiceInteractionSession(this) {}
    }
}
