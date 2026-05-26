package com.autonomous.phone.core

import android.content.Context
import android.util.Log
import com.autonomous.phone.device.DeviceController
import com.autonomous.phone.device.ScreenCapture
import com.autonomous.phone.device.ScreenElement
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

object AutonomousEngine {
    private const val TAG = "AutonomousEngine"
    
    data class EngineConfig(
        val maxSteps: Int = 50,
        val stepDelay: Long = 1000L,
        val confidenceThreshold: Float = 0.8f,
        val maxRetries: Int = 3
    )
    
    data class Decision(
        val action: String,
        val x: Int = 0,
        val y: Int = 0,
        val duration: Long = 0,
        val confidence: Float = 0.0f,
        val description: String = ""
    )
    
    data class ExecutionResult(
        val success: Boolean,
        val stepsCompleted: Int,
        val message: String,
        val finalState: String? = null
    )
    
    data class TaskState(
        val goal: String,
        val currentStep: Int = 0,
        val history: List<Decision> = emptyList(),
        val isRunning: Boolean = false
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
    ): ExecutionResult {
        if (isRunning) {
            return ExecutionResult(false, 0, "Engine is already running")
        }
        
        stepCallback = callback
        isRunning = true
        currentTask = TaskState(goal = goal, isRunning = true)
        
        try {
            return internalExecute(context, goal)
        } finally {
            isRunning = false
            currentTask?.isRunning = false
            stepCallback = null
        }
    }
    
    private suspend fun internalExecute(context: Context, goal: String): ExecutionResult {
        var stepsCompleted = 0
        var retries = 0
        
        while (stepsCompleted < currentConfig.maxSteps && isActive) {
            try {
                val screenshot = ScreenCapture.capture()
                
                val analysis = if (screenshot != null) {
                    VisionAnalyzer.analyzeImage(context, screenshot)
                } else {
                    null
                }
                
                val elements = DeviceController.getScreenElements()
                
                val decision = makeDecision(goal, analysis, elements, stepsCompleted)
                
                if (decision.confidence < currentConfig.confidenceThreshold) {
                    retries++
                    if (retries >= currentConfig.maxRetries) {
                        return ExecutionResult(
                            success = false,
                            stepsCompleted = stepsCompleted,
                            message = "Failed to make confident decision after $retries retries"
                        )
                    }
                    delay(500)
                    continue
                }
                
                executeDecision(decision)
                
                stepsCompleted++
                currentTask?.currentStep = stepsCompleted
                currentTask?.history = currentTask?.history.orEmpty() + decision
                
                stepCallback?.invoke(stepsCompleted, decision.description)
                
                if (isGoalAchieved(goal, analysis, stepsCompleted)) {
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
            }
        }
        
        return ExecutionResult(
            success = false,
            stepsCompleted = stepsCompleted,
            message = "Reached maximum steps (${currentConfig.maxSteps}) without achieving goal"
        )
    }
    
    private fun makeDecision(
        goal: String,
        analysis: VisionAnalyzer.AnalysisResult?,
        elements: List<ScreenElement>,
        step: Int
    ): Decision {
        val lowerGoal = goal.lowercase()
        
        return when {
            lowerGoal.contains("home") || lowerGoal.contains("返回主页") -> {
                Decision(
                    action = "press_home",
                    confidence = 0.95f,
                    description = "Pressing Home button"
                )
            }
            lowerGoal.contains("back") || lowerGoal.contains("返回") -> {
                Decision(
                    action = "press_back",
                    confidence = 0.95f,
                    description = "Pressing Back button"
                )
            }
            lowerGoal.contains("scroll") || lowerGoal.contains("滑动") || lowerGoal.contains("翻页") -> {
                val direction = if (lowerGoal.contains("up")) "up" else "down"
                Decision(
                    action = "scroll_$direction",
                    confidence = 0.9f,
                    description = "Scrolling $direction"
                )
            }
            lowerGoal.contains("click") || lowerGoal.contains("点击") -> {
                val centerX = 500
                val centerY = 500
                Decision(
                    action = "click",
                    x = centerX,
                    y = centerY,
                    confidence = 0.85f,
                    description = "Clicking at ($centerX, $centerY)"
                )
            }
            lowerGoal.contains("douyin") || lowerGoal.contains("抖音") -> {
                if (step == 0) {
                    Decision(
                        action = "open_app",
                        x = 0, y = 0,
                        confidence = 0.9f,
                        description = "Opening Douyin app"
                    )
                } else {
                    Decision(
                        action = "scroll_up",
                        confidence = 0.9f,
                        description = "Scrolling to next video"
                    )
                }
            }
            lowerGoal.contains("video") || lowerGoal.contains("刷") -> {
                Decision(
                    action = "scroll_up",
                    confidence = 0.85f,
                    description = "Scrolling to next content"
                )
            }
            else -> {
                Decision(
                    action = "wait",
                    confidence = 0.5f,
                    description = "Waiting for more context"
                )
            }
        }
    }
    
    private suspend fun executeDecision(decision: Decision) {
        when (decision.action) {
            "press_home" -> DeviceController.pressHome()
            "press_back" -> DeviceController.pressBack()
            "press_recent" -> DeviceController.pressRecent()
            "click" -> DeviceController.click(decision.x, decision.y)
            "long_click" -> DeviceController.longClick(decision.x, decision.y)
            "scroll_up" -> DeviceController.scrollUp()
            "scroll_down" -> DeviceController.scrollDown()
            "scroll_left" -> DeviceController.scrollLeft()
            "scroll_right" -> DeviceController.scrollRight()
            "open_app" -> DeviceController.openApp(decision.description)
            "wait" -> delay(1000)
            else -> Log.w(TAG, "Unknown action: ${decision.action}")
        }
    }
    
    private fun isGoalAchieved(goal: String, analysis: VisionAnalyzer.AnalysisResult?, steps: Int): Boolean {
        if (steps >= currentConfig.maxSteps) return true
        if (goal.lowercase().contains("stop") || goal.lowercase().contains("结束")) return true
        return false
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
        sb.append("Goal: $goal\n")
        sb.append("Screen content: ${screenAnalysis?.text ?: "No analysis"}\n")
        sb.append("Detected elements: ${elements.size}\n")
        elements.take(5).forEach { sb.append("  - ${it.text} (${it.bounds})\n") }
        return sb.toString()
    }
}