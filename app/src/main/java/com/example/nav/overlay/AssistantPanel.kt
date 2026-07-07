package com.example.nav.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AssistantPanel(
    onDismiss: () -> Unit,
    onSend: (String) -> Unit,
    response: String?,
    isThinking: Boolean
) {
    var textInput by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .heightIn(min = 200.dp, max = 500.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text("How can I help?", style = MaterialTheme.typography.headlineSmall)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            TextField(
                value = textInput,
                onValueChange = { textInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g., I want to return this order") }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = { 
                    onSend(textInput)
                    textInput = ""
                },
                enabled = textInput.isNotBlank() && !isThinking,
                modifier = Modifier.align(androidx.compose.ui.Alignment.End)
            ) {
                if (isThinking) {
                    CircularProgressIndicator(size = 20.dp, color = Color.White)
                } else {
                    Text("Ask")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (response != null) {
                Text("Assistant Response:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                LazyColumn {
                    item {
                        Text(response)
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally)
            ) {
                Text("Dismiss")
            }
        }
    }
}

@Composable
fun CircularProgressIndicator(size: androidx.compose.ui.unit.Dp, color: Color) {
    androidx.compose.material3.CircularProgressIndicator(
        modifier = Modifier.size(size),
        color = color,
        strokeWidth = 2.dp
    )
}
