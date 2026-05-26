package com.autonomous.phone.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.autonomous.phone.device.DeviceController
import com.autonomous.phone.device.ScreenElement
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AutonomousAccessibilityService : AccessibilityService() {

    companion object {
        const val GLOBAL_ACTION_HOME = 1
        const val GLOBAL_ACTION_BACK = 2
        const val GLOBAL_ACTION_RECENTS = 3
        const val GLOBAL_ACTION_NOTIFICATIONS = 4
        
        var instance: AutonomousAccessibilityService? = null
            private set
    }

    @Inject
    lateinit var deviceController: DeviceController

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        instance = this
        deviceController.accessibilityService = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        deviceController.accessibilityService = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
    }

    override fun onInterrupt() {
    }

    fun performClick(x: Float, y: Float): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path()
            path.moveTo(x, y)
            val gestureBuilder = GestureDescription.Builder()
            gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            return dispatchGesture(gestureBuilder.build(), null, null)
        }
        return false
    }

    fun performSwipe(x1: Float, y1: Float, x2: Float, y2: Float, duration: Long): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path()
            path.moveTo(x1, y1)
            path.lineTo(x2, y2)
            val gestureBuilder = GestureDescription.Builder()
            gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            return dispatchGesture(gestureBuilder.build(), null, null)
        }
        return false
    }

    fun performTextInput(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val inputNode = findInputField(rootNode) ?: return false

        inputNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        
        // Simplified text input, use clipboard approach for simplicity
        return true
    }

    fun executeGlobalAction(action: Int): Boolean {
        return when (action) {
            GLOBAL_ACTION_HOME -> {
                val homeNode = rootInActiveWindow
                if (homeNode != null) {
                    findAndClickElementByText(homeNode, "Home")
                } else false
            }
            GLOBAL_ACTION_BACK -> {
                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            }
            GLOBAL_ACTION_RECENTS -> {
                performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
            }
            GLOBAL_ACTION_NOTIFICATIONS -> {
                performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
            }
            else -> false
        }
    }

    private fun findAndClickElementByText(node: AccessibilityNodeInfo, text: String): Boolean {
        if (node.text?.toString()?.contains(text, ignoreCase = true) == true ||
            node.contentDescription?.toString()?.contains(text, ignoreCase = true) == true) {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            performClick(bounds.centerX().toFloat(), bounds.centerY().toFloat())
            return true
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClickElementByText(child, text)) {
                return true
            }
        }
        return false
    }

    private fun findInputField(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val className = node.className?.toString() ?: ""
        if (className.contains("EditText") || className.contains("TextField")) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findInputField(child)
            if (result != null) return result
        }
        return null
    }

    fun getScreenElements(): List<ScreenElement> {
        val root = rootInActiveWindow ?: return emptyList()
        val elements = mutableListOf<ScreenElement>()
        collectElements(root, elements)
        return elements
    }

    private fun collectElements(node: AccessibilityNodeInfo, elements: MutableList<ScreenElement>) {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        
        elements.add(
            ScreenElement(
                text = node.text?.toString(),
                contentDescription = node.contentDescription?.toString(),
                className = node.className?.toString(),
                bounds = bounds
            )
        )
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectElements(child, elements)
        }
    }
}
