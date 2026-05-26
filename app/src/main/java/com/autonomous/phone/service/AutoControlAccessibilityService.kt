package com.autonomous.phone.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class AutoControlAccessibilityService : AccessibilityService() {
    
    companion object {
        var instance: AutoControlAccessibilityService? = null
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }
    
    override fun onInterrupt() {
    }
}
