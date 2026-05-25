package com.autonomous.phone

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AutonomousApp : Application() {

    companion object {
        const val CHANNEL_ID_SCREEN_CAPTURE = "screen_capture"
        const val CHANNEL_ID_AUTONOMOUS = "autonomous_service"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val screenCaptureChannel = NotificationChannel(
                CHANNEL_ID_SCREEN_CAPTURE,
                "屏幕录制",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "用于屏幕录制的通知"
            }

            val autonomousChannel = NotificationChannel(
                CHANNEL_ID_AUTONOMOUS,
                "自主控制服务",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "自主AI控制服务通知"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(screenCaptureChannel)
            notificationManager.createNotificationChannel(autonomousChannel)
        }
    }
}
