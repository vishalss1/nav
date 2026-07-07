package com.example.nav.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.remember
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.nav.R
import kotlin.math.roundToInt

import androidx.compose.foundation.clickable
import androidx.lifecycle.lifecycleScope
import com.example.nav.accessibility.GuideAccessibilityService
import com.example.nav.llm.LLMService
import kotlinx.coroutines.launch

class FloatingBubbleService : LifecycleService(), SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private var bubbleView: ComposeView? = null
    private val llmService = LLMService()
    
    private var isPanelExpanded by mutableStateOf(false)
    private var llmResponse by mutableStateOf<String?>(null)
    private var isThinking by mutableStateOf(false)

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private var params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 0
        y = 100
    }

    private var panelParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.CENTER
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startAsForeground()
        showBubble()
    }

    private fun startAsForeground() {
        val channelId = "floating_bubble_service"
        val channel = NotificationChannel(
            channelId,
            "Floating Bubble Service",
            NotificationManager.IMPORTANCE_LOW
        )
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)

        val notification = Notification.Builder(this, channelId)
            .setContentTitle("NaV Assistant")
            .setContentText("NaV is running")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        startForeground(1, notification)
    }

    private fun showBubble() {
        bubbleView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingBubbleService)
            setViewTreeSavedStateRegistryOwner(this@FloatingBubbleService)
            
            setContent {
                var offsetX by remember { mutableStateOf(0f) }
                var offsetY by remember { mutableStateOf(100f) }

                if (isPanelExpanded) {
                    updateParamsForPanel()
                    AssistantPanel(
                        onDismiss = { 
                            isPanelExpanded = false
                            updateParamsForBubble()
                        },
                        onSend = { intent ->
                            handleUserIntent(intent)
                        },
                        response = llmResponse,
                        isThinking = isThinking
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                            .size(60.dp)
                            .background(Color.Blue, CircleShape)
                            .clickable { isPanelExpanded = true }
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    offsetX += dragAmount.x
                                    offsetY += dragAmount.y
                                    
                                    params.x = offsetX.roundToInt()
                                    params.y = offsetY.roundToInt()
                                    windowManager.updateViewLayout(bubbleView, params)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("NaV", color = Color.White)
                    }
                }
            }
        }

        windowManager.addView(bubbleView, params)
    }

    private fun updateParamsForPanel() {
        params.width = WindowManager.LayoutParams.MATCH_PARENT
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        windowManager.updateViewLayout(bubbleView, params)
    }

    private fun updateParamsForBubble() {
        params.width = WindowManager.LayoutParams.WRAP_CONTENT
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        windowManager.updateViewLayout(bubbleView, params)
    }

    private fun handleUserIntent(intent: String) {
        val screenContext = GuideAccessibilityService.getCurrentContext()
        if (screenContext == null) {
            llmResponse = "Sorry, I can't read the screen right now. Please make sure Accessibility Service is enabled."
            return
        }

        val prefs = getSharedPreferences("nav_prefs", MODE_PRIVATE)
        val apiKey = prefs.getString("groq_api_key", "") ?: ""

        if (apiKey.isBlank()) {
            llmResponse = "Please set your Groq API Key in the app first."
            return
        }

        isThinking = true
        llmResponse = null
        
        lifecycleScope.launch {
            llmResponse = llmService.getNavigationSteps(apiKey, screenContext, intent)
            isThinking = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bubbleView?.let { windowManager.removeView(it) }
    }
}
