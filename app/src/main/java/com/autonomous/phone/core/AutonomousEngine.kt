package com.autonomous.phone.core

import android.graphics.Bitmap
import com.autonomous.phone.device.DeviceController
import com.autonomous.phone.device.ScreenCaptureManager
import com.autonomous.phone.device.ScreenElement
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TaskState(
    val isRunning: Boolean = false,
    val currentStep: Int = 0,
    val totalSteps: Int = 0,
    val statusMessage: String = "就绪",
    val lastAction: String? = null
)

data class Decision(
    val actionType: ActionType,
    val parameters: Map<String, Any>,
    val confidence: Float,
    val reasoning: String
)

enum class ActionType {
    CLICK,
    SWIPE,
    INPUT_TEXT,
    PRESS_HOME,
    PRESS_BACK,
    SCROLL_UP,
    SCROLL_DOWN,
    WAIT,
    FINISH,
    NONE
}

class AutonomousEngine(
    private val deviceController: DeviceController,
    private val screenCaptureManager: ScreenCaptureManager,
    private val visionAnalyzer: VisionAnalyzer
) {

    private val _state = MutableStateFlow(TaskState())
    val state: Flow<TaskState> = _state.asStateFlow()

    private var isRunning = false
    private var currentGoal: String = ""

    suspend fun startTask(goal: String, maxSteps: Int = 50) {
        if (isRunning) return

        isRunning = true
        currentGoal = goal
        _state.value = TaskState(
            isRunning = true,
            totalSteps = maxSteps,
            statusMessage = "开始执行任务"
        )

        try {
            for (step in 1..maxSteps) {
                if (!isRunning) break

                _state.value = _state.value.copy(
                    currentStep = step,
                    statusMessage = "执行第 $step 步"
                )

                val result = executeStep()

                if (result == ExecutionResult.FINISHED) {
                    _state.value = _state.value.copy(
                        isRunning = false,
                        statusMessage = "任务完成"
                    )
                    break
                }

                delay(800)
            }
        } finally {
            isRunning = false
            _state.value = _state.value.copy(
                isRunning = false,
                statusMessage = if (_state.value.currentStep >= _state.value.totalSteps) 
                    "达到最大步数" else "任务已停止"
            )
        }
    }

    fun stopTask() {
        isRunning = false
    }

    private suspend fun executeStep(): ExecutionResult {
        val screenshot = screenCaptureManager.capture()
        val screenElements = deviceController.getScreenElements()
        val analysis = screenshot?.let { visionAnalyzer.analyze(it) }

        val decision = makeDecision(currentGoal, screenElements, analysis)

        when (decision.actionType) {
            ActionType.CLICK -> {
                val x = decision.parameters["x"] as? Float ?: 0f
                val y = decision.parameters["y"] as? Float ?: 0f
                deviceController.performClick(x, y)
                updateLastAction("点击 ($x, $y)")
            }
            ActionType.SWIPE -> {
                val x1 = decision.parameters["x1"] as? Float ?: 0f
                val y1 = decision.parameters["y1"] as? Float ?: 0f
                val x2 = decision.parameters["x2"] as? Float ?: 0f
                val y2 = decision.parameters["y2"] as? Float ?: 0f
                deviceController.performSwipe(x1, y1, x2, y2)
                updateLastAction("滑动")
            }
            ActionType.INPUT_TEXT -> {
                val text = decision.parameters["text"] as? String ?: ""
                deviceController.performTextInput(text)
                updateLastAction("输入: $text")
            }
            ActionType.PRESS_HOME -> {
                deviceController.pressHome()
                updateLastAction("按Home键")
            }
            ActionType.PRESS_BACK -> {
                deviceController.pressBack()
                updateLastAction("按返回键")
            }
            ActionType.SCROLL_UP -> {
                deviceController.scrollUp()
                updateLastAction("向上滚动")
            }
            ActionType.SCROLL_DOWN -> {
                deviceController.scrollDown()
                updateLastAction("向下滚动")
            }
            ActionType.WAIT -> {
                val duration = decision.parameters["duration"] as? Long ?: 1000L
                delay(duration)
                updateLastAction("等待 ${duration}ms")
            }
            ActionType.FINISH -> {
                return ExecutionResult.FINISHED
            }
            ActionType.NONE -> {
                updateLastAction("无操作")
            }
        }

        return ExecutionResult.CONTINUE
    }

    private fun makeDecision(
        goal: String,
        screenElements: List<ScreenElement>,
        analysis: ScreenAnalysis?
    ): Decision {
        val lowerGoal = goal.lowercase()

        if (lowerGoal.contains("抖音") || lowerGoal.contains("刷")) {
            return makeDouyinDecision(screenElements)
        }

        if (lowerGoal.contains("打开") && (lowerGoal.contains("设置") || lowerGoal.contains("setting"))) {
            return makeOpenSettingsDecision(screenElements)
        }

        if (lowerGoal.contains("返回") || lowerGoal.contains("back")) {
            return Decision(
                actionType = ActionType.PRESS_BACK,
                parameters = emptyMap(),
                confidence = 0.9f,
                reasoning = "目标要求返回"
            )
        }

        if (screenElements.isNotEmpty()) {
            val firstClickable = screenElements.find { 
                it.text != null && it.text.isNotEmpty()
            }
            if (firstClickable != null && firstClickable.bounds.width() > 10) {
                return Decision(
                    actionType = ActionType.CLICK,
                    parameters = mapOf(
                        "x" to firstClickable.bounds.centerX().toFloat(),
                        "y" to firstClickable.bounds.centerY().toFloat()
                    ),
                    confidence = 0.6f,
                    reasoning = "点击第一个可点击元素: ${firstClickable.text}"
                )
            }
        }

        return Decision(
            actionType = ActionType.FINISH,
            parameters = emptyMap(),
            confidence = 0.5f,
            reasoning = "无法确定下一步操作"
        )
    }

    private fun makeDouyinDecision(screenElements: List<ScreenElement>): Decision {
        return Decision(
            actionType = ActionType.SCROLL_DOWN,
            parameters = emptyMap(),
            confidence = 0.8f,
            reasoning = "刷抖音时向下滑动"
        )
    }

    private fun makeOpenSettingsDecision(screenElements: List<ScreenElement>): Decision {
        val settingsElement = screenElements.find {
            it.text?.contains("设置", ignoreCase = true) == true ||
            it.contentDescription?.contains("设置", ignoreCase = true) == true
        }

        if (settingsElement != null) {
            return Decision(
                actionType = ActionType.CLICK,
                parameters = mapOf(
                    "x" to settingsElement.bounds.centerX().toFloat(),
                    "y" to settingsElement.bounds.centerY().toFloat()
                ),
                confidence = 0.9f,
                reasoning = "找到设置按钮"
            )
        }

        return Decision(
            actionType = ActionType.PRESS_HOME,
            parameters = emptyMap(),
            confidence = 0.7f,
            reasoning = "先返回桌面再找设置"
        )
    }

    private fun updateLastAction(action: String) {
        _state.value = _state.value.copy(lastAction = action)
    }
}

enum class ExecutionResult {
    CONTINUE,
    FINISHED
}
