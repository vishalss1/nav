package com.example.nav.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class VoiceInputManager(private val context: Context) {
    private val TAG = "VoiceInputManager"
    private var speechRecognizer: SpeechRecognizer? = null
    
    interface VoiceListener {
        fun onReadyForSpeech()
        fun onBeginningOfSpeech()
        fun onRmsChanged(rmsdB: Float)
        fun onBufferReceived(buffer: ByteArray?)
        fun onEndOfSpeech()
        fun onError(error: Int)
        fun onResults(results: String)
        fun onPartialResults(partialResults: String)
    }

    private var listener: VoiceListener? = null

    private fun initRecognizer() {
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying previous recognizer", e)
        }
        
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech Recognition not available")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { listener?.onReadyForSpeech() }
                override fun onBeginningOfSpeech() { listener?.onBeginningOfSpeech() }
                override fun onRmsChanged(rmsdB: Float) { listener?.onRmsChanged(rmsdB) }
                override fun onBufferReceived(buffer: ByteArray?) { listener?.onBufferReceived(buffer) }
                override fun onEndOfSpeech() { listener?.onEndOfSpeech() }
                override fun onError(error: Int) {
                    Log.e(TAG, "Speech Error: $error")
                    // If busy or client error, force a cleanup
                    if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT) {
                        cancel()
                    }
                    listener?.onError(error)
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        Log.d(TAG, "Speech Result: ${matches[0]}")
                        listener?.onResults(matches[0])
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        listener?.onPartialResults(matches[0])
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    fun startListening(language: String = "en-US", listener: VoiceListener) {
        this.listener = listener
        if (speechRecognizer == null) initRecognizer()

        // Always cancel before starting to prevent "BUSY" errors
        speechRecognizer?.cancel()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // Extra sensitivity
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        }
        
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening, re-initializing...", e)
            initRecognizer()
            speechRecognizer?.startListening(intent)
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    fun cancel() {
        speechRecognizer?.cancel()
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
