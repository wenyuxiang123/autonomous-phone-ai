package com.autonomous.phone.device

import android.graphics.Rect

data class ScreenElement(
    val text: String? = null,
    val contentDescription: String? = null,
    val className: String? = null,
    val bounds: Rect
)
