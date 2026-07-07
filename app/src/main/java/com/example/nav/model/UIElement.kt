package com.example.nav.model

import android.graphics.Rect

data class ClickableElement(
    val text: String,
    val bounds: Rect,
    val contentDescription: String?,
    val id: String? = null
)

data class UIElement(
    val text: String?,
    val contentDescription: String?,
    val bounds: Rect,
    val isClickable: Boolean,
    val isScrollable: Boolean,
    val className: String?
)
