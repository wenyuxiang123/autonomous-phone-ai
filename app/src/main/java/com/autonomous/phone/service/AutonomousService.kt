package com.autonomous.phone.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.autonomous.phone.core.AutonomousEngine
import com.autonomous.phone.core.VisionAnalyzer
import com.autonomous.phone.device.ScreenCapture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AutonomousService : Service() {
    private val TAG = "AutonomousService"
    
    companion object {
        const val CHANNEL_ID = "AutonomousServiceChannel"
        const val NOTIFICATION_ID = 1
        
        private val _serviceState = MutableStateFlow(ServiceState.IDLE)
        val serviceState = _serviceState.asStateFlow()
        
        private val _currentGoal = MutableStateFlow<String?>(null)
        val currentGoal = _currentGoal.asStateFlow()
        
        private val _stepProgress = MutableStateFlow(0)
        val stepProgress = _stepProgress.asStateFlow()
        
        private val _lastMessage = MutableStateFlow("")
        val lastMessage = _lastMessage.asStateFlow()
        
        fun startService(context: Context, goal: String) {
            val intent = Intent(context, AutonomousService::class.java).apply {
                putExtra("goal", goal)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, AutonomousService::class.java)
            context.stopService(intent)
        }
    }
    
    private lateinit var serviceScope: CoroutineScope
    private var currentGoal: String? = null
    
    enum class ServiceState {
        IDLE,
        RUNNING,
        PAUSED,
        ERROR
    }
    
    override fun onCreate() {
        super.onCreate()
        serviceScope = CoroutineScope(Dispatchers.IO)
        createNotificationChannel()
        Log.d(TAG, "AutonomousService created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra("goal")?.let { goal ->
            currentGoal = goal
            _currentGoal.value = goal
            startExecution(goal)
        }
        
        startForeground(NOTIFICATION_ID, createNotification("Starting..."))
        
        return START_STICKY
    }
    
    private fun startExecution(goal: String) {
        _serviceState.value = ServiceState.RUNNING
        
        serviceScope.launch {
            try {
                val result = AutonomousEngine.executeTask(applicationContext, goal) { step, message ->
                    _stepProgress.value = step
                    _lastMessage.value = message
                    updateNotification("Step $step: $message")
                    Log.d(TAG, "Step $step: $message")
                }
                
                _serviceState.value = if (result.success) ServiceState.IDLE else ServiceState.ERROR
                _lastMessage.value = result.message
                updateNotification(result.message)
                
            } catch (e: Exception) {
                Log.e(TAG, "Execution failed", e)
                _serviceState.value = ServiceState.ERROR
                _lastMessage.value = "Error: ${e.message}"
            }
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Autonomous Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "AI自主控制服务"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AI自主控制")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun updateNotification(content: String) {
        val notification = createNotification(content)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        AutonomousEngine.stop()
        ScreenCapture.release()
        _serviceState.value = ServiceState.IDLE
        _currentGoal.value = null
        Log.d(TAG, "AutonomousService destroyed")
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    suspend fun analyzeScreen(): VisionAnalyzer.AnalysisResult? {
        val bitmap = ScreenCapture.capture()
        return bitmap?.let { VisionAnalyzer.analyzeImage(applicationContext, it) }
    }
    
    fun pauseExecution() {
        _serviceState.value = ServiceState.PAUSED
        AutonomousEngine.stop()
    }
    
    fun resumeExecution() {
        currentGoal?.let {
            _serviceState.value = ServiceState.RUNNING
            startExecution(it)
        }
    }
}