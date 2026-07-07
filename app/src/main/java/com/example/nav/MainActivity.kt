package com.example.nav

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.nav.overlay.FloatingBubbleService

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var apiKey by remember { mutableStateOf(getSavedApiKey()) }
                    
                    OnboardingScreen(
                        apiKey = apiKey,
                        onApiKeyChange = { 
                            apiKey = it
                            saveApiKey(it)
                        },
                        onGrantOverlay = { requestOverlayPermission() },
                        onGrantAccessibility = { requestAccessibilityPermission() },
                        onStartAssistant = { startAssistantService() }
                    )
                }
            }
        }
    }

    private fun getSavedApiKey(): String {
        val prefs = getSharedPreferences("nav_prefs", MODE_PRIVATE)
        return prefs.getString("groq_api_key", "") ?: ""
    }

    private fun saveApiKey(key: String) {
        val prefs = getSharedPreferences("nav_prefs", MODE_PRIVATE)
        prefs.edit().putString("groq_api_key", key).apply()
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun startAssistantService() {
        if (Settings.canDrawOverlays(this)) {
            val intent = Intent(this, FloatingBubbleService::class.java)
            startForegroundService(intent)
        }
    }
}

@Composable
fun OnboardingScreen(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    onGrantOverlay: () -> Unit,
    onGrantAccessibility: () -> Unit,
    onStartAssistant: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome to NaV", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            label = { Text("Groq API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("NaV needs these permissions:", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = onGrantOverlay, modifier = Modifier.fillMaxWidth()) {
            Text("1. Grant Overlay Permission")
        }
        Text("Allows showing the floating bubble on top of other apps.", style = MaterialTheme.typography.bodySmall)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = onGrantAccessibility, modifier = Modifier.fillMaxWidth()) {
            Text("2. Enable Accessibility Service")
        }
        Text("Allows NaV to read the screen context.", style = MaterialTheme.typography.bodySmall)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onStartAssistant, 
            modifier = Modifier.fillMaxWidth(),
            enabled = apiKey.isNotBlank()
        ) {
            Text("Start Assistant")
        }
    }
}
