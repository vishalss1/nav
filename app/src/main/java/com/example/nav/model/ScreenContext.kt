package com.example.nav.model

data class ScreenContext(
    val appPackage: String,
    val appName: String,
    val visibleTexts: List<String>,
    val clickableElements: List<ClickableElement>,
    val scrollable: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
