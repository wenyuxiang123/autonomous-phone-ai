package com.autonomous.phone.device

import android.graphics.Path
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

object DeviceController {
    
    var accessibilityService: Any? = null
    
    fun performClick(x: Float, y: Float): Boolean {
        val service = accessibilityService ?: return false
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                val path = Path()
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
    
    fun performDoubleClick(x: Float, y: Float): Boolean {
        val service = accessibilityService ?: return false
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                val gestureBuilder = android.accessibilityservice.GestureDescription.Builder()
                
                val path1 = Path()
                path1.moveTo(x, y)
                gestureBuilder.addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(path1, 0, 100))
                
                val path2 = Path()
                path2.moveTo(x, y)
                gestureBuilder.addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(path2, 200, 100))
                
                return (service as android.accessibilityservice.AccessibilityService).dispatchGesture(
                    gestureBuilder.build(), null, null
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
    
    fun performLongClick(x: Float, y: Float, duration: Long = 1000): Boolean {
        val service = accessibilityService ?: return false
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                val path = Path()
                path.moveTo(x, y)
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
    
    fun performSwipe(x1: Float, y1: Float, x2: Float, y2: Float, duration: Long = 500): Boolean {
        val service = accessibilityService ?: return false
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                val path = Path()
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
    
    fun performPinchZoom(startX1: Float, startY1: Float, startX2: Float, startY2: Float, scaleFactor: Float = 1.5f): Boolean {
        val service = accessibilityService ?: return false
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                val gestureBuilder = android.accessibilityservice.GestureDescription.Builder()
                
                val endX1 = startX1 - (startX1 - startX2) * scaleFactor / 2
                val endY1 = startY1 - (startY1 - startY2) * scaleFactor / 2
                val endX2 = startX2 + (startX1 - startX2) * scaleFactor / 2
                val endY2 = startY2 + (startY1 - startY2) * scaleFactor / 2
                
                val path1 = Path()
                path1.moveTo(startX1, startY1)
                path1.lineTo(endX1, endY1)
                
                val path2 = Path()
                path2.moveTo(startX2, startY2)
                path2.lineTo(endX2, endY2)
                
                gestureBuilder.addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(path1, 0, 500))
                gestureBuilder.addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(path2, 0, 500))
                
                return (service as android.accessibilityservice.AccessibilityService).dispatchGesture(
                    gestureBuilder.build(), null, null
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
    
    fun performDrag(x1: Float, y1: Float, x2: Float, y2: Float, duration: Long = 800): Boolean {
        val service = accessibilityService ?: return false
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                val path = Path()
                path.moveTo(x1, y1)
                path.lineTo(x2, y2)
                val gestureBuilder = android.accessibilityservice.GestureDescription.Builder()
                gestureBuilder.addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(path, 100, duration))
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
    
    fun scrollLeft(): Boolean {
        return performSwipe(900f, 960f, 200f, 960f, 400)
    }
    
    fun scrollRight(): Boolean {
        return performSwipe(200f, 960f, 900f, 960f, 400)
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
    
    fun pressQuickSettings(): Boolean {
        val service = accessibilityService ?: return false
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                (service as android.accessibilityservice.AccessibilityService).performGlobalAction(
                    android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS
                )
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
    
    fun pressPowerDialog(): Boolean {
        val service = accessibilityService ?: return false
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                (service as android.accessibilityservice.AccessibilityService).performGlobalAction(
                    android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_POWER_DIALOG
                )
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
    
    fun takeScreenshot(): Boolean {
        val service = accessibilityService ?: return false
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                (service as android.accessibilityservice.AccessibilityService).performGlobalAction(
                    android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT
                )
                return true
            }
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
    
    fun findElementByText(text: String, caseSensitive: Boolean = false): ScreenElement? {
        val elements = getScreenElements()
        return elements.firstOrNull { element ->
            val elementText = element.text ?: return@firstOrNull false
            if (caseSensitive) {
                elementText == text
            } else {
                elementText.equals(text, ignoreCase = true)
            }
        }
    }
    
    fun findElementByDescription(description: String, caseSensitive: Boolean = false): ScreenElement? {
        val elements = getScreenElements()
        return elements.firstOrNull { element ->
            val elementDesc = element.contentDescription ?: return@firstOrNull false
            if (caseSensitive) {
                elementDesc == description
            } else {
                elementDesc.equals(description, ignoreCase = true)
            }
        }
    }
    
    fun findElementsByText(text: String, caseSensitive: Boolean = false): List<ScreenElement> {
        val elements = getScreenElements()
        return elements.filter { element ->
            val elementText = element.text ?: return@filter false
            if (caseSensitive) {
                elementText.contains(text)
            } else {
                elementText.contains(text, ignoreCase = true)
            }
        }
    }
    
    fun findElementByContainsText(text: String, caseSensitive: Boolean = false): ScreenElement? {
        val elements = getScreenElements()
        return elements.firstOrNull { element ->
            val elementText = element.text ?: return@firstOrNull false
            if (caseSensitive) {
                elementText.contains(text)
            } else {
                elementText.contains(text, ignoreCase = true)
            }
        }
    }
    
    fun findElementByContainsDescription(description: String, caseSensitive: Boolean = false): ScreenElement? {
        val elements = getScreenElements()
        return elements.firstOrNull { element ->
            val elementDesc = element.contentDescription ?: return@firstOrNull false
            if (caseSensitive) {
                elementDesc.contains(description)
            } else {
                elementDesc.contains(description, ignoreCase = true)
            }
        }
    }
    
    fun clickElement(element: ScreenElement): Boolean {
        val centerX = (element.bounds.left + element.bounds.right) / 2f
        val centerY = (element.bounds.top + element.bounds.bottom) / 2f
        return performClick(centerX, centerY)
    }
    
    fun clickElementByText(text: String, caseSensitive: Boolean = false): Boolean {
        val element = findElementByText(text, caseSensitive)
        return if (element != null) {
            clickElement(element)
        } else {
            false
        }
    }
    
    fun clickElementByDescription(description: String, caseSensitive: Boolean = false): Boolean {
        val element = findElementByDescription(description, caseSensitive)
        return if (element != null) {
            clickElement(element)
        } else {
            false
        }
    }
    
    fun longClickElement(element: ScreenElement, duration: Long = 1000): Boolean {
        val centerX = (element.bounds.left + element.bounds.right) / 2f
        val centerY = (element.bounds.top + element.bounds.bottom) / 2f
        return performLongClick(centerX, centerY, duration)
    }
    
    fun longClickElementByText(text: String, caseSensitive: Boolean = false, duration: Long = 1000): Boolean {
        val element = findElementByText(text, caseSensitive)
        return if (element != null) {
            longClickElement(element, duration)
        } else {
            false
        }
    }
    
    fun getCurrentAppPackage(): String? {
        val service = accessibilityService ?: return null
        try {
            val rootNode = (service as android.accessibilityservice.AccessibilityService).rootInActiveWindow
            return rootNode?.packageName?.toString()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
    
    private fun collectElements(node: android.view.accessibility.AccessibilityNodeInfo, elements: MutableList<ScreenElement>) {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        
        val isClickable = node.isClickable
        val isScrollable = node.isScrollable
        val isEditable = node.isEditable
        val isCheckable = node.isCheckable
        val isChecked = node.isChecked
        val isSelected = node.isSelected
        
        elements.add(ScreenElement(
            text = node.text?.toString(),
            contentDescription = node.contentDescription?.toString(),
            className = node.className?.toString(),
            bounds = bounds,
            isClickable = isClickable,
            isScrollable = isScrollable,
            isEditable = isEditable,
            isCheckable = isCheckable,
            isChecked = isChecked,
            isSelected = isSelected
        ))
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                collectElements(child, elements)
            }
        }
    }
}
