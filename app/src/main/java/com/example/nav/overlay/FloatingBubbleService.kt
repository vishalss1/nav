package com.example.nav.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.PixelFormat
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import android.speech.SpeechRecognizer
import android.util.Log
import com.example.nav.R
import com.example.nav.accessibility.GuideAccessibilityService
import com.example.nav.llm.LLMService
import com.example.nav.voice.TTSManager
import com.example.nav.voice.VoiceInputManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FloatingBubbleService : LifecycleService(), SavedStateRegistryOwner {

    private val TAG = "FloatingBubbleService"
    private lateinit var windowManager: WindowManager
    private var islandView: ComposeView? = null
    private val llmService = LLMService()
    private lateinit var voiceInputManager: VoiceInputManager
    private lateinit var ttsManager: TTSManager
    private lateinit var systemActionExecutor: SystemActionExecutor
    
    private var isIslandExpanded by mutableStateOf(false)
    private var llmResponse by mutableStateOf<String?>(null)
    private var isThinking by mutableStateOf(false)
    private var isListening by mutableStateOf(false)
    private var isSpeaking by mutableStateOf(false)
    private var voiceTranscript by mutableStateOf("")
    private var rmsLevel by mutableStateOf(0f)

    private var restartJob: Job? = null

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private var params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        y = 40
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        voiceInputManager = VoiceInputManager(this)
        systemActionExecutor = SystemActionExecutor(this)
        ttsManager = TTSManager(this) { success ->
            if (success) Log.d(TAG, "TTS ready in Service")
        }
        startAsForeground()
        showIsland()
    }

    private fun startAsForeground() {
        val channelId = "nav_island_service"
        val channel = NotificationChannel(channelId, "NaV", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)

        val notification = Notification.Builder(this, channelId)
            .setContentTitle("NaV Assistant")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
        startForeground(1, notification)
    }

    private fun showIsland() {
        islandView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingBubbleService)
            setViewTreeSavedStateRegistryOwner(this@FloatingBubbleService)
            
            setContent {
                if (isIslandExpanded) {
                    updateParamsForPanel()
                    AssistantPanel(
                        onDismiss = { 
                            isIslandExpanded = false
                            stopVoiceInput()
                            ttsManager.stop()
                            updateParamsForIsland()
                        },
                        onSend = { intent -> handleUserIntent(intent) },
                        onStartVoice = { startVoiceInput() },
                        onStopVoice = { stopVoiceInput() },
                        response = llmResponse,
                        isThinking = isThinking,
                        isListening = isListening,
                        voiceTranscript = voiceTranscript,
                        rmsLevel = rmsLevel
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .widthIn(min = 100.dp)
                            .height(38.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .background(Color.Black)
                            .clickable { 
                                isIslandExpanded = true 
                                startVoiceInput()
                            }
                            .animateContentSize(spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            if (isThinking) {
                                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.Yellow))
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = if (isThinking) "NaV..." else "NaV", 
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        windowManager.addView(islandView, params)
    }

    private fun updateParamsForPanel() {
        params.width = WindowManager.LayoutParams.MATCH_PARENT
        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        windowManager.updateViewLayout(islandView, params)
    }

    private fun updateParamsForIsland() {
        params.width = WindowManager.LayoutParams.WRAP_CONTENT
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        windowManager.updateViewLayout(islandView, params)
    }

    private fun startVoiceInput() {
        if (isThinking || isListening || !isIslandExpanded || isSpeaking) return
        
        isListening = true
        voiceTranscript = ""
        restartJob?.cancel()
        
        voiceInputManager.startListening(listener = object : VoiceInputManager.VoiceListener {
            override fun onReadyForSpeech() { Log.d(TAG, "Speech Ready") }
            override fun onBeginningOfSpeech() { Log.d(TAG, "Speech Start") }
            override fun onRmsChanged(rmsdB: Float) { rmsLevel = rmsdB }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { 
                isListening = false 
                Log.d(TAG, "Speech End")
            }
            override fun onError(error: Int) { 
                isListening = false 
                Log.e(TAG, "Speech Error: $error")
                
                if (isIslandExpanded && !isThinking && !isSpeaking) {
                    val delayMillis = when(error) {
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 1500L
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 500L
                        SpeechRecognizer.ERROR_NO_MATCH -> 500L
                        else -> 1000L
                    }
                    restartJob = lifecycleScope.launch {
                        delay(delayMillis)
                        if (isIslandExpanded && !isThinking && !isListening && !isSpeaking) {
                            Log.d(TAG, "Auto-restarting voice input...")
                            startVoiceInput()
                        }
                    }
                }
            }
            override fun onResults(results: String) {
                isListening = false
                voiceTranscript = results
                handleUserIntent(results)
            }
            override fun onPartialResults(partialResults: String) {
                voiceTranscript = partialResults
            }
        })
    }

    private fun stopVoiceInput() {
        restartJob?.cancel()
        voiceInputManager.stopListening()
        isListening = false
    }

    private fun handleUserIntent(intent: String) {
        if (intent.isBlank()) return
        
        val screenContext = GuideAccessibilityService.getCurrentContext()
        val prefs = getSharedPreferences("nav_prefs", MODE_PRIVATE)
        val apiKey = prefs.getString("groq_api_key", "") ?: ""
        val voiceId = prefs.getString("preferred_voice_id", null)

        if (apiKey.isBlank()) {
            val msg = "Set Groq API Key in NaV app."
            llmResponse = msg
            ttsManager.speak(msg, voiceId)
            return
        }

        isThinking = true
        llmResponse = null
        stopVoiceInput()
        ttsManager.stop()
        
        lifecycleScope.launch {
            Log.d(TAG, "Sending to LLM: $intent")
            val response = llmService.getNavigationSteps(apiKey, screenContext ?: fallbackContext(), intent)
            Log.d(TAG, "LLM Response: $response")
            
            if (response.startsWith("ACTION: OPEN_APP:")) {
                val appName = response.substringAfterLast(":").trim()
                val speakMsg = "Opening $appName..."
                llmResponse = speakMsg
                isSpeaking = true
                ttsManager.speak(speakMsg, voiceId)
                
                delay(1500) // Let TTS speak
                isSpeaking = false
                systemActionExecutor.execute(response.removePrefix("ACTION:").trim())
                isIslandExpanded = false
                updateParamsForIsland()
            } else {
                llmResponse = response
                isSpeaking = true
                ttsManager.speak(response, voiceId)
                // We'd need a callback from TTS to reset isSpeaking properly
                // For now, a rough estimate or we can add a listener to TTSManager
                delay(5000) 
                isSpeaking = false
                if (isIslandExpanded && !isListening) startVoiceInput()
            }
            isThinking = false
        }
    }

    private fun fallbackContext() = com.example.nav.model.ScreenContext("unknown", "Home", emptyList(), emptyList(), false)

    override fun onDestroy() {
        super.onDestroy()
        islandView?.let { windowManager.removeView(it) }
        voiceInputManager.destroy()
        ttsManager.destroy()
    }
}
