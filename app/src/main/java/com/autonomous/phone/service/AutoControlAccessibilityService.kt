package com.autonomous.phone.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.autonomous.phone.device.DeviceController

class AutoControlAccessibilityService : AccessibilityService() {
    
    companion object {
        var instance: AutoControlAccessibilityService? = null
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        DeviceController.accessibilityService = this
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        DeviceController.accessibilityService = null
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 暂时不需要处理事件
    }
    
    override fun onInterrupt() {
        // 处理中断
    }
}
