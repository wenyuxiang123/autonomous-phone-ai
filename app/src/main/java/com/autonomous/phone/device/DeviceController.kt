package com.autonomous.phone.device

import android.graphics.Rect
import com.autonomous.phone.service.AutonomousAccessibilityService

data class ScreenElement(
    val text: String?,
    val contentDescription: String?,
    val className: String?,
    val bounds: Rect,
    val nodeId: Long = System.currentTimeMillis()
)

class DeviceController {

    var accessibilityService: AutonomousAccessibilityService? = null

    fun performClick(x: Float, y: Float): Boolean {
        return accessibilityService?.performClick(x, y) ?: false
    }

    fun performSwipe(x1: Float, y1: Float, x2: Float, y2: Float, duration: Long = 300): Boolean {
        return accessibilityService?.performSwipe(x1, y1, x2, y2, duration) ?: false
    }

    fun performTextInput(text: String): Boolean {
        return accessibilityService?.performTextInput(text) ?: false
    }

    fun pressHome(): Boolean {
        return accessibilityService?.performGlobalAction(AutonomousAccessibilityService.GLOBAL_ACTION_HOME) ?: false
    }

    fun pressBack(): Boolean {
        return accessibilityService?.performGlobalAction(AutonomousAccessibilityService.GLOBAL_ACTION_BACK) ?: false
    }

    fun pressRecent(): Boolean {
        return accessibilityService?.performGlobalAction(AutonomousAccessibilityService.GLOBAL_ACTION_RECENTS) ?: false
    }

    fun openNotifications(): Boolean {
        return accessibilityService?.performGlobalAction(AutonomousAccessibilityService.GLOBAL_ACTION_NOTIFICATIONS) ?: false
    }

    fun getScreenElements(): List<ScreenElement> {
        return accessibilityService?.getScreenElements() ?: emptyList()
    }

    fun findElementByText(text: String): ScreenElement? {
        return getScreenElements().find {
            it.text?.contains(text, ignoreCase = true) == true ||
            it.contentDescription?.contains(text, ignoreCase = true) == true
        }
    }

    fun clickElementByText(text: String): Boolean {
        val element = findElementByText(text)
        if (element != null) {
            val centerX = element.bounds.centerX().toFloat()
            val centerY = element.bounds.centerY().toFloat()
            return performClick(centerX, centerY)
        }
        return false
    }

    fun scrollUp(): Boolean {
        val service = accessibilityService ?: return false
        val displayMetrics = service.resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        return performSwipe(
            width / 2f,
            height * 0.3f,
            width / 2f,
            height * 0.7f,
            500
        )
    }

    fun scrollDown(): Boolean {
        val service = accessibilityService ?: return false
        val displayMetrics = service.resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        return performSwipe(
            width / 2f,
            height * 0.7f,
            width / 2f,
            height * 0.3f,
            500
        )
    }
}
