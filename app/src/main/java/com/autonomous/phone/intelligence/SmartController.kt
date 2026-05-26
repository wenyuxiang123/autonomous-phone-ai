package com.autonomous.phone.intelligence

import com.autonomous.phone.device.DeviceController
import kotlinx.coroutines.delay

object SmartController {
    
    suspend fun waitForElementByText(
        text: String,
        timeoutMs: Long = 5000,
        checkIntervalMs: Long = 500,
        caseSensitive: Boolean = false
    ): Boolean {
        val startTime = System.currentTimeMillis()
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val element = DeviceController.findElementByText(text, caseSensitive)
            if (element != null) {
                return true
            }
            delay(checkIntervalMs)
        }
        
        return false
    }
    
    suspend fun waitForElementByDescription(
        description: String,
        timeoutMs: Long = 5000,
        checkIntervalMs: Long = 500,
        caseSensitive: Boolean = false
    ): Boolean {
        val startTime = System.currentTimeMillis()
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val element = DeviceController.findElementByDescription(description, caseSensitive)
            if (element != null) {
                return true
            }
            delay(checkIntervalMs)
        }
        
        return false
    }
    
    suspend fun waitForContainsText(
        text: String,
        timeoutMs: Long = 5000,
        checkIntervalMs: Long = 500,
        caseSensitive: Boolean = false
    ): Boolean {
        val startTime = System.currentTimeMillis()
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val element = DeviceController.findElementByContainsText(text, caseSensitive)
            if (element != null) {
                return true
            }
            delay(checkIntervalMs)
        }
        
        return false
    }
    
    suspend fun clickWhenFound(
        text: String,
        timeoutMs: Long = 5000,
        caseSensitive: Boolean = false
    ): Boolean {
        val found = waitForElementByText(text, timeoutMs, caseSensitive = caseSensitive)
        if (found) {
            return DeviceController.clickElementByText(text, caseSensitive)
        }
        return false
    }
    
    suspend fun clickDescriptionWhenFound(
        description: String,
        timeoutMs: Long = 5000,
        caseSensitive: Boolean = false
    ): Boolean {
        val found = waitForElementByDescription(description, timeoutMs, caseSensitive = caseSensitive)
        if (found) {
            return DeviceController.clickElementByDescription(description, caseSensitive)
        }
        return false
    }
    
    fun checkCurrentApp(targetPackage: String): Boolean {
        val currentPackage = DeviceController.getCurrentAppPackage()
        return currentPackage == targetPackage
    }
    
    suspend fun waitForApp(
        targetPackage: String,
        timeoutMs: Long = 10000,
        checkIntervalMs: Long = 500
    ): Boolean {
        val startTime = System.currentTimeMillis()
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (checkCurrentApp(targetPackage)) {
                return true
            }
            delay(checkIntervalMs)
        }
        
        return false
    }
    
    suspend fun scrollUntilFindText(
        text: String,
        maxScrolls: Int = 10,
        scrollDelayMs: Long = 500,
        caseSensitive: Boolean = false
    ): Boolean {
        repeat(maxScrolls) {
            if (DeviceController.findElementByText(text, caseSensitive) != null) {
                return true
            }
            DeviceController.scrollDown()
            delay(scrollDelayMs)
        }
        return DeviceController.findElementByText(text, caseSensitive) != null
    }
    
    fun findAllClickableElements(): List<com.autonomous.phone.device.ScreenElement> {
        return DeviceController.getScreenElements().filter { it.isClickable }
    }
    
    fun findAllEditableElements(): List<com.autonomous.phone.device.ScreenElement> {
        return DeviceController.getScreenElements().filter { it.isEditable }
    }
    
    fun screenContainsText(text: String, caseSensitive: Boolean = false): Boolean {
        val elements = DeviceController.getScreenElements()
        return elements.any { element ->
            val elementText = element.text ?: return@any false
            if (caseSensitive) {
                elementText == text
            } else {
                elementText.equals(text, ignoreCase = true)
            }
        }
    }
    
    fun screenContainsDescription(description: String, caseSensitive: Boolean = false): Boolean {
        val elements = DeviceController.getScreenElements()
        return elements.any { element ->
            val elementDesc = element.contentDescription ?: return@any false
            if (caseSensitive) {
                elementDesc == description
            } else {
                elementDesc.equals(description, ignoreCase = true)
            }
        }
    }
}
