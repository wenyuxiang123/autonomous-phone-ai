package com.autonomous.phone.device

import android.graphics.Rect

data class ScreenElement(
    val text: String? = null,
    val contentDescription: String? = null,
    val className: String? = null,
    val bounds: Rect,
    val isClickable: Boolean = false,
    val isScrollable: Boolean = false,
    val isEditable: Boolean = false,
    val isCheckable: Boolean = false,
    val isChecked: Boolean = false,
    val isSelected: Boolean = false
) {
    fun getDisplayText(): String {
        return when {
            !text.isNullOrBlank() -> text
            !contentDescription.isNullOrBlank() -> contentDescription
            else -> className ?: "Unknown"
        }
    }
    
    fun getTypeIcon(): String {
        return when {
            isEditable -> "✏️"
            isCheckable && isChecked -> "✅"
            isCheckable -> "☐"
            isClickable -> "🔘"
            isScrollable -> "📜"
            else -> "📄"
        }
    }
}
