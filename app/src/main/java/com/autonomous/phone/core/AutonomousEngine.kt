package com.autonomous.phone.core

import android.content.Context
import android.util.Log
import com.autonomous.phone.device.DeviceController
import com.autonomous.phone.device.ScreenCapture
import com.autonomous.phone.device.ScreenElement
import kotlinx.coroutines.delay
import kotlinx.coroutines.coroutineScope

object AutonomousEngine {
    private const val TAG = "AutonomousEngine"
    
    data class EngineConfig(
        val maxSteps: Int = 50,
        val stepDelay: Long = 1500L,
        val confidenceThreshold: Float = 0.7f,
        val maxRetries: Int = 3
    )
    
    data class Decision(
        val action: String,
        val x: Int = 0,
        val y: Int = 0,
        val duration: Long = 0,
        val confidence: Float = 0.0f,
        val description: String = "",
        val targetElement: String? = null
    )
    
    data class ExecutionResult(
        val success: Boolean,
        val stepsCompleted: Int,
        val message: String,
        val finalState: String? = null
    )
    
    data class TaskState(
        val goal: String,
        var currentStep: Int = 0,
        var history: List<Decision> = emptyList(),
        var isRunning: Boolean = false
    )
    
    private var currentConfig = EngineConfig()
    private var currentTask: TaskState? = null
    private var isRunning = false
    private var stepCallback: ((Int, String) -> Unit)? = null
    
    fun configure(config: EngineConfig) {
        currentConfig = config
    }
    
    suspend fun executeTask(
        context: Context,
        goal: String,
        callback: ((Int, String) -> Unit)? = null
    ): ExecutionResult = coroutineScope {
        if (isRunning) {
            return@coroutineScope ExecutionResult(false, 0, "Engine is already running")
        }
        
        stepCallback = callback
        isRunning = true
        currentTask = TaskState(goal = goal, isRunning = true)
        
        try {
            internalExecute(context, goal)
        } finally {
            isRunning = false
            currentTask?.isRunning = false
            stepCallback = null
        }
    }
    
    private suspend fun internalExecute(context: Context, goal: String): ExecutionResult {
        var stepsCompleted = 0
        var retries = 0
        
        while (stepsCompleted < currentConfig.maxSteps && isRunning) {
            try {
                val screenshot = ScreenCapture.capture()
                
                val analysis = if (screenshot != null) {
                    VisionAnalyzer.analyzeImage(context, screenshot)
                } else {
                    null
                }
                
                val elements = DeviceController.getScreenElements()
                
                val decision = makeIntelligentDecision(goal, analysis, elements, stepsCompleted)
                
                Log.d(TAG, "Step ${stepsCompleted + 1}: Making decision - ${decision.description}")
                
                executeDecision(decision)
                
                stepsCompleted++
                currentTask?.currentStep = stepsCompleted
                currentTask?.history = currentTask?.history.orEmpty() + decision
                
                stepCallback?.invoke(stepsCompleted, decision.description)
                
                if (isGoalAchieved(goal, analysis, elements, stepsCompleted)) {
                    return ExecutionResult(
                        success = true,
                        stepsCompleted = stepsCompleted,
                        message = "Goal achieved in $stepsCompleted steps",
                        finalState = analysis?.text
                    )
                }
                
                delay(currentConfig.stepDelay)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error executing step $stepsCompleted", e)
                retries++
                if (retries >= currentConfig.maxRetries) {
                    return ExecutionResult(
                        success = false,
                        stepsCompleted = stepsCompleted,
                        message = "Error: ${e.message}"
                    )
                }
                delay(1000)
            }
        }
        
        return ExecutionResult(
            success = false,
            stepsCompleted = stepsCompleted,
            message = "Reached maximum steps (${currentConfig.maxSteps}) without achieving goal"
        )
    }
    
    private fun makeIntelligentDecision(
        goal: String,
        analysis: VisionAnalyzer.AnalysisResult?,
        elements: List<ScreenElement>,
        step: Int
    ): Decision {
        val lowerGoal = goal.lowercase()
        val screenText = analysis?.text?.lowercase() ?: ""
        val elementTexts = elements.mapNotNull { it.text?.lowercase() }
        
        if (step == 0 && (lowerGoal.contains("打开") || lowerGoal.contains("open") || lowerGoal.contains("启动"))) {
            val appName = extractAppName(goal)
            val targetElement = findElementByText(elements, appName) ?: findClosestElement(elements, appName)
            if (targetElement != null) {
                return Decision(
                    action = "click_element",
                    x = (targetElement.bounds.left + targetElement.bounds.right) / 2,
                    y = (targetElement.bounds.top + targetElement.bounds.bottom) / 2,
                    confidence = 0.95f,
                    description = "Clicking on $appName",
                    targetElement = targetElement.text
                )
            }
        }
        
        if (lowerGoal.contains("主页") || lowerGoal.contains("home")) {
            return Decision(
                action = "press_home",
                confidence = 0.95f,
                description = "Pressing Home button"
            )
        }
        
        if (lowerGoal.contains("返回") || lowerGoal.contains("back")) {
            return Decision(
                action = "press_back",
                confidence = 0.95f,
                description = "Pressing Back button"
            )
        }
        
        if (lowerGoal.contains("上滑") || lowerGoal.contains("刷视频") || lowerGoal.contains("scroll up")) {
            return Decision(
                action = "scroll_up",
                confidence = 0.9f,
                description = "Scrolling up to next content"
            )
        }
        
        if (lowerGoal.contains("下滑") || lowerGoal.contains("scroll down")) {
            return Decision(
                action = "scroll_down",
                confidence = 0.9f,
                description = "Scrolling down"
            )
        }
        
        if (lowerGoal.contains("左滑") || lowerGoal.contains("scroll left")) {
            return Decision(
                action = "scroll_left",
                confidence = 0.9f,
                description = "Scrolling left"
            )
        }
        
        if (lowerGoal.contains("右滑") || lowerGoal.contains("scroll right")) {
            return Decision(
                action = "scroll_right",
                confidence = 0.9f,
                description = "Scrolling right"
            )
        }
        
        if (lowerGoal.contains("双击") || lowerGoal.contains("double click")) {
            val targetText = extractTargetText(goal, listOf("双击", "double click"))
            val targetElement = findElementByText(elements, targetText)
            if (targetElement != null) {
                return Decision(
                    action = "double_click",
                    x = (targetElement.bounds.left + targetElement.bounds.right) / 2,
                    y = (targetElement.bounds.top + targetElement.bounds.bottom) / 2,
                    confidence = 0.9f,
                    description = "Double clicking on $targetText",
                    targetElement = targetText
                )
            }
        }
        
        if (lowerGoal.contains("长按") || lowerGoal.contains("long press")) {
            val targetText = extractTargetText(goal, listOf("长按", "long press"))
            val targetElement = findElementByText(elements, targetText)
            if (targetElement != null) {
                return Decision(
                    action = "long_click",
                    x = (targetElement.bounds.left + targetElement.bounds.right) / 2,
                    y = (targetElement.bounds.top + targetElement.bounds.bottom) / 2,
                    duration = 1500,
                    confidence = 0.9f,
                    description = "Long pressing on $targetText",
                    targetElement = targetText
                )
            }
        }
        
        if (lowerGoal.contains("点击") || lowerGoal.contains("click") || lowerGoal.contains("tap")) {
            val targetText = extractTargetText(goal, listOf("点击", "click", "tap"))
            val targetElement = findElementByText(elements, targetText) ?: findClosestElement(elements, targetText)
            if (targetElement != null) {
                return Decision(
                    action = "click_element",
                    x = (targetElement.bounds.left + targetElement.bounds.right) / 2,
                    y = (targetElement.bounds.top + targetElement.bounds.bottom) / 2,
                    confidence = 0.85f,
                    description = "Clicking on ${targetElement.text ?: targetElement.contentDescription ?: "element"}",
                    targetElement = targetElement.text
                )
            } else {
                val centerX = 540
                val centerY = 960
                return Decision(
                    action = "click",
                    x = centerX,
                    y = centerY,
                    confidence = 0.6f,
                    description = "Clicking at center ($centerX, $centerY)"
                )
            }
        }
        
        if (lowerGoal.contains("停止") || lowerGoal.contains("stop") || lowerGoal.contains("结束")) {
            return Decision(
                action = "stop",
                confidence = 1.0f,
                description = "Stopping task execution"
            )
        }
        
        return Decision(
            action = "wait",
            confidence = 0.5f,
            description = "Waiting for more context - analyzing screen"
        )
    }
    
    private fun findElementByText(elements: List<ScreenElement>, text: String): ScreenElement? {
        if (text.isBlank()) return null
        return elements.firstOrNull { element ->
            element.text?.contains(text, ignoreCase = true) == true ||
            element.contentDescription?.contains(text, ignoreCase = true) == true
        }
    }
    
    private fun findClosestElement(elements: List<ScreenElement>, text: String): ScreenElement? {
        if (text.isBlank()) return null
        return elements.filter { element ->
            element.text?.contains(text, ignoreCase = true) == true ||
            element.contentDescription?.contains(text, ignoreCase = true) == true ||
            element.className?.contains(text, ignoreCase = true) == true
        }.firstOrNull()
    }
    
    private fun extractAppName(goal: String): String {
        val patterns = listOf(
            Regex("打开(.+?)应用"),
            Regex("open (.+?) app"),
            Regex("启动(.+?)"),
            Regex("启动(.+?)应用"),
            Regex("打开(.+)")
        )
        
        for (pattern in patterns) {
            val match = pattern.find(goal)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }
        return goal.replace(Regex("(打开|启动|open|启动)"), "").trim()
    }
    
    private fun extractTargetText(goal: String, keywords: List<String>): String {
        for (keyword in keywords) {
            val index = goal.lowercase().indexOf(keyword.lowercase())
            if (index != -1) {
                val afterKeyword = goal.substring(index + keyword.length).trim()
                return afterKeyword.split(Regex("[\\s,，。、]")).firstOrNull() ?: ""
            }
        }
        return goal.replace(Regex(".*(点击|click|tap|双击|double click|长按|long press)"), "").trim()
    }
    
    private fun isGoalAchieved(
        goal: String,
        analysis: VisionAnalyzer.AnalysisResult?,
        elements: List<ScreenElement>,
        steps: Int
    ): Boolean {
        if (steps >= currentConfig.maxSteps) return true
        if (goal.lowercase().contains("stop") || goal.lowercase().contains("结束")) return true
        if (goal.lowercase().contains("停止")) return true
        
        val screenText = analysis?.text?.lowercase() ?: ""
        val lowerGoal = goal.lowercase()
        
        val appName = extractAppName(lowerGoal)
        if (appName.isNotBlank() && screenText.contains(appName)) {
            return true
        }
        
        return false
    }
    
    private suspend fun executeDecision(decision: Decision) {
        when (decision.action) {
            "press_home" -> DeviceController.pressHome()
            "press_back" -> DeviceController.pressBack()
            "press_recent" -> DeviceController.pressRecent()
            "click" -> DeviceController.performClick(decision.x.toFloat(), decision.y.toFloat())
            "click_element" -> DeviceController.performClick(decision.x.toFloat(), decision.y.toFloat())
            "double_click" -> DeviceController.performDoubleClick(decision.x.toFloat(), decision.y.toFloat())
            "long_click" -> DeviceController.performLongClick(decision.x.toFloat(), decision.y.toFloat(), decision.duration)
            "scroll_up" -> DeviceController.scrollUp()
            "scroll_down" -> DeviceController.scrollDown()
            "scroll_left" -> DeviceController.scrollLeft()
            "scroll_right" -> DeviceController.scrollRight()
            "drag" -> DeviceController.performDrag(decision.x.toFloat(), decision.y.toFloat(), decision.x.toFloat() + 200, decision.y.toFloat())
            "wait" -> delay(2000)
            "stop" -> stop()
            else -> Log.w(TAG, "Unknown action: ${decision.action}")
        }
    }
    
    fun stop() {
        isRunning = false
        currentTask?.isRunning = false
    }
    
    fun isRunning(): Boolean = isRunning
    
    fun getCurrentTask(): TaskState? = currentTask
    
    fun analyzeSituation(
        goal: String,
        screenAnalysis: VisionAnalyzer.AnalysisResult?,
        elements: List<ScreenElement>
    ): String {
        val sb = StringBuilder()
        sb.append("Goal: $goal\n\n")
        sb.append("Screen Analysis:\n")
        sb.append("  - Detected text: ${screenAnalysis?.text?.take(200) ?: "None"}\n")
        sb.append("  - UI Elements: ${elements.size}\n")
        sb.append("  - Confidence: ${(screenAnalysis?.confidence ?: 0.0f) * 100}%\n\n")
        sb.append("Interactive Elements:\n")
        elements.filter { it.isClickable }.take(10).forEach { element ->
            sb.append("  - ${element.text ?: element.contentDescription ?: "Unnamed"} (${element.className})\n")
        }
        return sb.toString()
    }
}
