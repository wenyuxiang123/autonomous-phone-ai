package com.autonomous.phone.device

import android.graphics.Rect

object DeviceController {
    
    var accessibilityService: Any? = null
    
    fun performClick(x: Float, y: Float): Boolean {
        val service = accessibilityService ?: return false
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                val path = android.graphics.Path()
                path.moveTo(x, y)
                val gestureBuilder = android.accessibilityservice.GestureDescription.Builder()
                gestureBuilder.addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 100))
                return (service as android.accessibilityservice.AccessibilityService).dispatchGesture(
                    gestureBuilder.build(), null, null
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
    
    fun performSwipe(x1: Float, y1: Float, x2: Float, y2: Float, duration: Long = 500): Boolean {
        val service = accessibilityService ?: return false
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                val path = android.graphics.Path()
                path.moveTo(x1, y1)
                path.lineTo(x2, y2)
                val gestureBuilder = android.accessibilityservice.GestureDescription.Builder()
                gestureBuilder.addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, duration))
                return (service as android.accessibilityservice.AccessibilityService).dispatchGesture(
                    gestureBuilder.build(), null, null
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
    
    fun scrollUp(): Boolean {
        return performSwipe(540f, 1800f, 540f, 400f, 400)
    }
    
    fun scrollDown(): Boolean {
        return performSwipe(540f, 400f, 540f, 1800f, 400)
    }
    
    fun pressHome(): Boolean {
        val service = accessibilityService ?: return false
        try {
            (service as android.accessibilityservice.AccessibilityService).performGlobalAction(
                android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
            )
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
    
    fun pressBack(): Boolean {
        val service = accessibilityService ?: return false
        try {
            (service as android.accessibilityservice.AccessibilityService).performGlobalAction(
                android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
            )
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
    
    fun pressRecent(): Boolean {
        val service = accessibilityService ?: return false
        try {
            (service as android.accessibilityservice.AccessibilityService).performGlobalAction(
                android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS
            )
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
    
    fun getScreenElements(): List<ScreenElement> {
        val service = accessibilityService ?: return emptyList()
        val elements = mutableListOf<ScreenElement>()
        try {
            val rootNode = (service as android.accessibilityservice.AccessibilityService).rootInActiveWindow
            if (rootNode != null) {
                collectElements(rootNode, elements)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return elements
    }
    
    private fun collectElements(node: android.view.accessibility.AccessibilityNodeInfo, elements: MutableList<ScreenElement>) {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        
        elements.add(ScreenElement(
            text = node.text?.toString(),
            contentDescription = node.contentDescription?.toString(),
            className = node.className?.toString(),
            bounds = bounds
        ))
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                collectElements(child, elements)
            }
        }
    }
}
