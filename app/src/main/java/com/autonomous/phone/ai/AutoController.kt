package com.autonomous.phone.ai

import com.autonomous.phone.device.DeviceController
import kotlinx.coroutines.delay

object AutoController {

    suspend fun autoBrowseDouyin() {
        while (true) {
            DeviceController.scrollDown()
            delay(3000)
        }
    }

    suspend fun autoBrowseTikTok() {
        while (true) {
            DeviceController.scrollDown()
            delay(4000)
        }
    }

    suspend fun demoSequence() {
        delay(1000)
        DeviceController.pressHome()
        delay(2000)
        DeviceController.scrollDown()
        delay(1000)
        DeviceController.scrollUp()
    }
}
