package com.autonomous.phone.script

import com.autonomous.phone.device.DeviceController
import kotlinx.coroutines.delay

sealed class Action {
    abstract suspend fun execute()
    abstract fun describe(): String
}

data class ClickAction(
    val x: Float,
    val y: Float,
    val description: String? = null
) : Action() {
    override suspend fun execute() {
        DeviceController.performClick(x, y)
    }
    override fun describe(): String = description ?: "Click at ($x, $y)"
}

data class LongClickAction(
    val x: Float,
    val y: Float,
    val duration: Long = 1000,
    val description: String? = null
) : Action() {
    override suspend fun execute() {
        DeviceController.performLongClick(x, y, duration)
    }
    override fun describe(): String = description ?: "Long click at ($x, $y) for ${duration}ms"
}

data class SwipeAction(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val duration: Long = 500,
    val description: String? = null
) : Action() {
    override suspend fun execute() {
        DeviceController.performSwipe(x1, y1, x2, y2, duration)
    }
    override fun describe(): String = description ?: "Swipe from ($x1, $y1) to ($x2, $y2)"
}

data class ScrollUpAction(
    val description: String? = null
) : Action() {
    override suspend fun execute() {
        DeviceController.scrollUp()
    }
    override fun describe(): String = description ?: "Scroll up"
}

data class ScrollDownAction(
    val description: String? = null
) : Action() {
    override suspend fun execute() {
        DeviceController.scrollDown()
    }
    override fun describe(): String = description ?: "Scroll down"
}

data class PressHomeAction(
    val description: String? = null
) : Action() {
    override suspend fun execute() {
        DeviceController.pressHome()
    }
    override fun describe(): String = description ?: "Press home"
}

data class PressBackAction(
    val description: String? = null
) : Action() {
    override suspend fun execute() {
        DeviceController.pressBack()
    }
    override fun describe(): String = description ?: "Press back"
}

data class PressRecentAction(
    val description: String? = null
) : Action() {
    override suspend fun execute() {
        DeviceController.pressRecent()
    }
    override fun describe(): String = description ?: "Press recent"
}

data class DelayAction(
    val duration: Long,
    val description: String? = null
) : Action() {
    override suspend fun execute() {
        delay(duration)
    }
    override fun describe(): String = description ?: "Delay for ${duration}ms"
}

data class ClickByTextAction(
    val text: String,
    val caseSensitive: Boolean = false,
    val description: String? = null
) : Action() {
    override suspend fun execute() {
        DeviceController.clickElementByText(text, caseSensitive)
    }
    override fun describe(): String = description ?: "Click by text: \"$text\""
}

data class ClickByDescriptionAction(
    val description: String,
    val caseSensitive: Boolean = false,
    val customDescription: String? = null
) : Action() {
    override suspend fun execute() {
        DeviceController.clickElementByDescription(description, caseSensitive)
    }
    override fun describe(): String = customDescription ?: "Click by description: \"$description\""
}

data class LongClickByTextAction(
    val text: String,
    val caseSensitive: Boolean = false,
    val duration: Long = 1000,
    val description: String? = null
) : Action() {
    override suspend fun execute() {
        DeviceController.longClickElementByText(text, caseSensitive, duration)
    }
    override fun describe(): String = description ?: "Long click by text: \"$text\""
}

data class PressQuickSettingsAction(
    val description: String? = null
) : Action() {
    override suspend fun execute() {
        DeviceController.pressQuickSettings()
    }
    override fun describe(): String = description ?: "Press quick settings"
}

data class PressPowerDialogAction(
    val description: String? = null
) : Action() {
    override suspend fun execute() {
        DeviceController.pressPowerDialog()
    }
    override fun describe(): String = description ?: "Press power dialog"
}

data class TakeScreenshotAction(
    val description: String? = null
) : Action() {
    override suspend fun execute() {
        DeviceController.takeScreenshot()
    }
    override fun describe(): String = description ?: "Take screenshot"
}
