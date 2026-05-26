package com.autonomous.phone.script

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class ScriptExecutor {
    var isRunning: Boolean = false
        private set
    
    var currentActionIndex: Int = -1
        private set
    
    var onActionExecuted: ((Int, Action) -> Unit)? = null
    var onScriptComplete: (() -> Unit)? = null
    var onError: ((Throwable) -> Unit)? = null
    
    suspend fun execute(script: Script) {
        if (isRunning) return
        
        isRunning = true
        currentActionIndex = 0
        
        try {
            for ((index, action) in script.actions.withIndex()) {
                if (!isRunning) break
                
                currentActionIndex = index
                action.execute()
                onActionExecuted?.invoke(index, action)
                
                if (index < script.actions.size - 1) {
                    delay(500)
                }
            }
            onScriptComplete?.invoke()
        } catch (e: Exception) {
            onError?.invoke(e)
        } finally {
            isRunning = false
            currentActionIndex = -1
        }
    }
    
    fun stop() {
        isRunning = false
    }
}
