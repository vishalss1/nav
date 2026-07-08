package com.example.nav

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.nav.overlay.FloatingBubbleService
import com.example.nav.voice.TTSManager
import com.example.nav.voice.VoiceInfo

class MainActivity : ComponentActivity() {

    private var ttsManager: TTSManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var apiKey by remember { mutableStateOf(getSavedApiKey()) }
                    var selectedVoiceId by remember { mutableStateOf(getSavedVoiceId()) }
                    val context = LocalContext.current
                    
                    var availableVoices by remember { mutableStateOf(emptyList<VoiceInfo>()) }
                    
                    // Initialize TTS to get voices
                    DisposableEffect(Unit) {
                        ttsManager = TTSManager(context) { success ->
                            if (success) {
                                availableVoices = ttsManager?.getAvailableVoices() ?: emptyList()
                            }
                        }
                        onDispose {
                            ttsManager?.destroy()
                        }
                    }

                    var hasMicPermission by remember { 
                        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                    }

                    val micPermissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        hasMicPermission = isGranted
                    }
                    
                    OnboardingScreen(
                        apiKey = apiKey,
                        onApiKeyChange = { 
                            apiKey = it
                            saveApiKey(it)
                        },
                        availableVoices = availableVoices,
                        selectedVoiceId = selectedVoiceId,
                        onVoiceChange = {
                            selectedVoiceId = it
                            saveVoiceId(it)
                            ttsManager?.speak("This is a sample of the selected voice", it)
                        },
                        hasMicPermission = hasMicPermission,
                        onGrantMic = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        onGrantOverlay = { requestOverlayPermission() },
                        onGrantAccessibility = { requestAccessibilityPermission() },
                        onSetDefaultAssistant = { requestDefaultAssistantSetting() },
                        onStartAssistant = { startAssistantService() }
                    )
                }
            }
        }
    }

    private fun requestDefaultAssistantSetting() {
        val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
        startActivity(intent)
    }

    private fun getSavedApiKey(): String {
        val prefs = getSharedPreferences("nav_prefs", MODE_PRIVATE)
        return prefs.getString("groq_api_key", "") ?: ""
    }

    private fun saveApiKey(key: String) {
        val prefs = getSharedPreferences("nav_prefs", MODE_PRIVATE)
        prefs.edit().putString("groq_api_key", key).apply()
    }

    private fun getSavedVoiceId(): String {
        val prefs = getSharedPreferences("nav_prefs", MODE_PRIVATE)
        return prefs.getString("preferred_voice_id", "") ?: ""
    }

    private fun saveVoiceId(voiceId: String) {
        val prefs = getSharedPreferences("nav_prefs", MODE_PRIVATE)
        prefs.edit().putString("preferred_voice_id", voiceId).apply()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    availableVoices: List<VoiceInfo>,
    selectedVoiceId: String,
    onVoiceChange: (String) -> Unit,
    hasMicPermission: Boolean,
    onGrantMic: () -> Unit,
    onGrantOverlay: () -> Unit,
    onGrantAccessibility: () -> Unit,
    onSetDefaultAssistant: () -> Unit,
    onStartAssistant: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text("Welcome to NaV", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            label = { Text("Groq API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // Voice Selection Dropdown
        var expanded by remember { mutableStateOf(false) }
        val currentVoiceName = availableVoices.find { it.id == selectedVoiceId }?.displayName ?: "Select Voice"

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = currentVoiceName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Assistant Voice") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                availableVoices.forEach { voice ->
                    DropdownMenuItem(
                        text = { Text(voice.displayName) },
                        onClick = {
                            onVoiceChange(voice.id)
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text("NaV needs these permissions:", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onGrantOverlay, 
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("1. Grant Overlay Permission")
        }
        Text("Allows showing the floating bubble on top of other apps.", style = MaterialTheme.typography.bodySmall)
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Button(
            onClick = onGrantAccessibility, 
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("2. Enable Accessibility Service")
        }
        Text("Allows NaV to read the screen context.", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onGrantMic, 
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (hasMicPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
        ) {
            Text(if (hasMicPermission) "3. Microphone Granted" else "3. Grant Microphone Permission")
        }
        Text("Allows you to talk to the assistant.", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onSetDefaultAssistant, 
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("4. Set as Default Assistant")
        }
        Text("Unlocks background listening for seamless help.", style = MaterialTheme.typography.bodySmall)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onStartAssistant, 
            modifier = Modifier.fillMaxWidth(),
            enabled = apiKey.isNotBlank() && hasMicPermission
        ) {
            Text("Start Assistant")
        }
    }
}
