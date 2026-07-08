package com.example.nav.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AssistantPanel(
    onDismiss: () -> Unit,
    onSend: (String) -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    response: String?,
    isThinking: Boolean,
    isListening: Boolean,
    voiceTranscript: String,
    rmsLevel: Float
) {
    var textInput by remember { mutableStateOf("") }
    var showKeyboardInput by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color.Black.copy(alpha = 0.95f))
            .animateContentSize(spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isListening) Color.Green else if (isThinking) Color.Yellow else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isListening) "Listening" else if (isThinking) "Thinking" else "NaV",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Content Area (Voice or Text)
            if (!showKeyboardInput) {
                // Voice Mode (Default)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = voiceTranscript.ifBlank { if (isListening) "I'm listening..." else "Tap mic to speak" },
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    DynamicWaveform(rmsLevel, isListening)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = { if (isListening) onStopVoice() else onStartVoice() },
                            modifier = Modifier
                                .size(56.dp)
                                .background(if (isListening) Color.Red.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f), RoundedCornerShape(28.dp))
                        ) {
                            Icon(
                                Icons.Default.Mic, 
                                contentDescription = "Voice", 
                                tint = if (isListening) Color.Red else Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(24.dp))
                        
                        IconButton(
                            onClick = { showKeyboardInput = true },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                        ) {
                            Icon(Icons.Default.Keyboard, contentDescription = "Keyboard", tint = Color.Gray)
                        }
                    }
                }
            } else {
                // Text Input Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp)),
                        placeholder = { Text("Type here...", color = Color.Gray) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.1f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    IconButton(
                        onClick = { showKeyboardInput = false },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Back to Voice", tint = Color.Gray)
                    }

                    IconButton(
                        onClick = { 
                            onSend(textInput)
                            textInput = ""
                        },
                        enabled = textInput.isNotBlank() && !isThinking,
                        modifier = Modifier.size(40.dp)
                    ) {
                        if (isThinking) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                        }
                    }
                }
            }

            // Response Area
            if (response != null && !isListening) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(12.dp)
                ) {
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        item {
                            Text(
                                text = response,
                                color = Color.LightGray,
                                fontSize = 15.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DynamicWaveform(rmsLevel: Float, isListening: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    
    Row(
        modifier = Modifier
            .height(60.dp)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val barCount = 15
        for (i in 0 until barCount) {
            val baseHeight = if (isListening) (rmsLevel.coerceIn(0f, 10f) * 4 + 4) else 4f
            val animScale by if (isListening) {
                infiniteTransition.animateFloat(
                    initialValue = 0.5f,
                    targetValue = 1.5f,
                    animationSpec = infiniteRepeatable(
                        tween(300 + i * 50, easing = LinearEasing),
                        RepeatMode.Reverse
                    ),
                    label = "bar_$i"
                )
            } else {
                remember { mutableFloatStateOf(1f) }
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .width(4.dp)
                    .height((baseHeight * animScale).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))
                        )
                    )
            )
        }
    }
}
