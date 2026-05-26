package com.autonomous.phone.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.autonomous.phone.core.AutonomousEngine
import com.autonomous.phone.core.VisionAnalyzer
import com.autonomous.phone.device.DeviceController
import com.autonomous.phone.device.ScreenCapture
import com.autonomous.phone.model.ModelManager
import com.autonomous.phone.script.ScriptExecutor
import com.autonomous.phone.script.ScriptManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AutoControlScreen()
                }
            }
        }
    }
}

data class LogEntry(
    val message: String,
    val type: LogType = LogType.INFO,
    val timestamp: Long = System.currentTimeMillis()
)

enum class LogType {
    INFO, SUCCESS, ERROR, ACTION, AI
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoControlScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var isAccessibilityEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var isScreenCaptureEnabled by remember { mutableStateOf(ScreenCapture.isAvailable()) }
    var currentTab by remember { mutableStateOf(Tab.CONTROLS) }
    var logEntries by remember { mutableStateOf(mutableStateListOf<LogEntry>()) }
    var screenElements by remember { mutableStateOf(emptyList<com.autonomous.phone.device.ScreenElement>()) }
    var currentAppPackage by remember { mutableStateOf<String?>(null) }
    var isExecutingScript by remember { mutableStateOf(false) }
    var currentScriptProgress by remember { mutableStateOf(0 to 0) }
    var isAiRunning by remember { mutableStateOf(false) }
    var aiGoal by remember { mutableStateOf("") }
    var aiStep by remember { mutableStateOf(0) }
    var aiMessage by remember { mutableStateOf("") }
    var modelStatus by remember { mutableStateOf<ModelManager.ModelStatus?>(null) }
    
    val scriptExecutor = remember { ScriptExecutor() }
    
    LaunchedEffect(Unit) {
        ModelManager.init(context)
        modelStatus = ModelManager.getModelStatus(context, ModelManager.TEST_MODEL)
        
        while (isActive) {
            isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
            if (isAccessibilityEnabled) {
                currentAppPackage = DeviceController.getCurrentAppPackage()
            }
            delay(1000)
        }
    }
    
    val addLog: (String, LogType) -> Unit = { message, type ->
        logEntries.add(LogEntry(message, type))
        if (logEntries.size > 100) {
            logEntries.removeFirst()
        }
    }
    
    val screenCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            ScreenCapture.initialize(context, result.resultCode, result.data!!)
            isScreenCaptureEnabled = true
            addLog("屏幕录制权限已获取", LogType.SUCCESS)
        } else {
            addLog("屏幕录制权限未获取", LogType.ERROR)
        }
    }
    
    fun requestScreenCapture() {
        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? android.media.projection.MediaProjectionManager
        if (projectionManager != null) {
            try {
                screenCaptureLauncher.launch(projectionManager.createScreenCaptureIntent())
            } catch (e: Exception) {
                addLog("请求屏幕录制权限失败: ${e.message}", LogType.ERROR)
            }
        } else {
            addLog("无法获取 MediaProjection 服务", LogType.ERROR)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("手机自主AI") },
                actions = {
                    currentAppPackage?.let { pkg ->
                        Text(
                            text = pkg.split(".").lastOrNull() ?: pkg,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = currentTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                TabItem.values().forEachIndexed { index, item ->
                    Tab(
                        selected = currentTab == item.tab,
                        onClick = { currentTab = item.tab },
                        text = { Text(item.label) },
                        icon = { Icon(item.icon, contentDescription = null) }
                    )
                }
            }
            
            Box(modifier = Modifier.weight(1f)) {
                when (currentTab) {
                    Tab.CONTROLS -> ControlsTab(
                        isAccessibilityEnabled = isAccessibilityEnabled,
                        isScreenCaptureEnabled = isScreenCaptureEnabled,
                        modelStatus = modelStatus,
                        addLog = addLog,
                        onRequestScreenCapture = { requestScreenCapture() },
                        onDownloadModel = {
                            coroutineScope.launch {
                                addLog("开始下载模型...", LogType.INFO)
                                val success = ModelManager.downloadModel(context, ModelManager.TEST_MODEL) { progress ->
                                    // Update progress on main thread
                                    withContext(Dispatchers.Main) {
                                        addLog("下载进度: ${progress.percentage}% (${progress.downloaded}/${progress.total})", LogType.INFO)
                                    }
                                }
                                if (success) {
                                    modelStatus = ModelManager.getModelStatus(context, ModelManager.TEST_MODEL)
                                    addLog("模型下载完成", LogType.SUCCESS)
                                } else {
                                    addLog("模型下载失败", LogType.ERROR)
                                }
                            }
                        }
                    )
                    Tab.AI -> AiControlTab(
                        isAccessibilityEnabled = isAccessibilityEnabled,
                        isScreenCaptureEnabled = isScreenCaptureEnabled,
                        isAiRunning = isAiRunning,
                        aiGoal = aiGoal,
                        aiStep = aiStep,
                        aiMessage = aiMessage,
                        addLog = addLog,
                        onRequestScreenCapture = { requestScreenCapture() },
                        onStartAi = { goal ->
                            aiGoal = goal
                            isAiRunning = true
                            aiStep = 0
                            aiMessage = "启动中..."
                            
                            addLog("AI任务开始: $goal", LogType.AI)
                            
                            coroutineScope.launch(Dispatchers.IO) {
                                val result = AutonomousEngine.executeTask(context, goal) { step, message ->
                                    aiStep = step
                                    aiMessage = message
                                    addLog("AI [$step]: $message", LogType.AI)
                                }
                                
                                isAiRunning = false
                                if (result.success) {
                                    addLog("AI任务完成: ${result.message}", LogType.SUCCESS)
                                } else {
                                    addLog("AI任务失败: ${result.message}", LogType.ERROR)
                                }
                                aiMessage = result.message
                            }
                        },
                        onStopAi = {
                            AutonomousEngine.stop()
                            isAiRunning = false
                            addLog("AI任务已停止", LogType.INFO)
                        },
                        onAnalyzeScreen = {
                            coroutineScope.launch(Dispatchers.IO) {
                                if (!ScreenCapture.isAvailable()) {
                                    addLog("请先获取屏幕录制权限", LogType.ERROR)
                                    return@launch
                                }
                                
                                addLog("开始分析屏幕...", LogType.INFO)
                                val bitmap = ScreenCapture.capture()
                                if (bitmap != null) {
                                    val analysis = VisionAnalyzer.analyzeImage(context, bitmap)
                                    addLog("屏幕分析完成", LogType.SUCCESS)
                                    addLog("分析结果: ${analysis.text}", LogType.INFO)
                                    analysis.objects.forEach { obj ->
                                        addLog("检测到: ${obj.name} (${obj.confidence})", LogType.INFO)
                                    }
                                    bitmap.recycle()
                                } else {
                                    addLog("屏幕捕获失败", LogType.ERROR)
                                }
                            }
                        }
                    )
                    Tab.SCRIPTS -> ScriptsTab(
                        isAccessibilityEnabled = isAccessibilityEnabled,
                        addLog = addLog,
                        scriptExecutor = scriptExecutor,
                        onExecutionStateChange = { executing, progress ->
                            isExecutingScript = executing
                            currentScriptProgress = progress
                        }
                    )
                    Tab.ELEMENTS -> ElementsTab(
                        isAccessibilityEnabled = isAccessibilityEnabled,
                        onRefresh = { screenElements = DeviceController.getScreenElements() },
                        screenElements = screenElements,
                        addLog = addLog
                    )
                    Tab.LOGS -> LogsTab(logEntries = logEntries)
                }
            }
            
            if (isExecutingScript || isAiRunning) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            if (isAiRunning) {
                                Text("AI执行中...", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "$aiMessage",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                Text("执行脚本中...", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "进度: ${currentScriptProgress.first + 1}/${currentScriptProgress.second}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class TabItem(val tab: Tab, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    CONTROLS(Tab.CONTROLS, "控制", Icons.Default.ArrowBack),
    AI(Tab.AI, "AI", Icons.Default.Settings),
    SCRIPTS(Tab.SCRIPTS, "脚本", Icons.Default.PlayArrow),
    ELEMENTS(Tab.ELEMENTS, "元素", Icons.Default.List),
    LOGS(Tab.LOGS, "日志", Icons.Default.Home)
}

enum class Tab {
    CONTROLS, AI, SCRIPTS, ELEMENTS, LOGS
}

@Composable
fun ControlsTab(
    isAccessibilityEnabled: Boolean,
    isScreenCaptureEnabled: Boolean,
    modelStatus: ModelManager.ModelStatus?,
    addLog: (String, LogType) -> Unit,
    onRequestScreenCapture: () -> Unit,
    onDownloadModel: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("无障碍服务", style = MaterialTheme.typography.titleMedium)
                    if (isAccessibilityEnabled) {
                        Text("已开启", color = MaterialTheme.colorScheme.primary)
                    }
                }
                
                Text(if (isAccessibilityEnabled) "已开启" else "未开启")
                
                if (!isAccessibilityEnabled) {
                    Button(
                        onClick = { openAccessibilitySettings(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("前往设置")
                    }
                }
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("屏幕录制", style = MaterialTheme.typography.titleMedium)
                    if (isScreenCaptureEnabled) {
                        Text("已开启", color = MaterialTheme.colorScheme.primary)
                    }
                }
                
                Text(if (isScreenCaptureEnabled) "已获取权限" else "未获取权限")
                
                if (!isScreenCaptureEnabled) {
                    Button(
                        onClick = onRequestScreenCapture,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("获取权限")
                    }
                }
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AI模型", style = MaterialTheme.typography.titleMedium)
                }
                
                if (modelStatus != null) {
                    Text(
                        "模型: ${modelStatus.modelName}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        if (modelStatus.downloaded) "已下载" else "未下载",
                        color = if (modelStatus.downloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    
                    if (!modelStatus.downloaded) {
                        Button(
                            onClick = onDownloadModel,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("下载模型 (约3GB)")
                        }
                    }
                }
            }
        }
        
        HorizontalDivider()
        
        Text("快捷操作", style = MaterialTheme.typography.titleMedium)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (isAccessibilityEnabled) {
                        DeviceController.pressHome()
                        addLog("按下 Home 键", LogType.ACTION)
                    }
                },
                enabled = isAccessibilityEnabled,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Home, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Home")
            }
            Button(
                onClick = {
                    if (isAccessibilityEnabled) {
                        DeviceController.pressBack()
                        addLog("按下 Back 键", LogType.ACTION)
                    }
                },
                enabled = isAccessibilityEnabled,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Back")
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (isAccessibilityEnabled) {
                        DeviceController.pressRecent()
                        addLog("按下 Recent 键", LogType.ACTION)
                    }
                },
                enabled = isAccessibilityEnabled,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.List, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("最近")
            }
            Button(
                onClick = {
                    if (isAccessibilityEnabled) {
                        DeviceController.pressQuickSettings()
                        addLog("按下快捷设置", LogType.ACTION)
                    }
                },
                enabled = isAccessibilityEnabled,
                modifier = Modifier.weight(1f)
            ) {
                Text("快捷")
            }
        }
        
        HorizontalDivider()
        
        Text("手势操作", style = MaterialTheme.typography.titleMedium)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (isAccessibilityEnabled) {
                        DeviceController.scrollUp()
                        addLog("向上滑动", LogType.ACTION)
                    }
                },
                enabled = isAccessibilityEnabled,
                modifier = Modifier.weight(1f)
            ) {
                Text("上滑")
            }
            Button(
                onClick = {
                    if (isAccessibilityEnabled) {
                        DeviceController.scrollDown()
                        addLog("向下滑动", LogType.ACTION)
                    }
                },
                enabled = isAccessibilityEnabled,
                modifier = Modifier.weight(1f)
            ) {
                Text("下滑")
            }
        }
        
        Button(
            onClick = {
                if (isAccessibilityEnabled) {
                    DeviceController.takeScreenshot()
                    addLog("截取屏幕", LogType.ACTION)
                }
            },
            enabled = isAccessibilityEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("截图")
        }
    }
}

@Composable
fun AiControlTab(
    isAccessibilityEnabled: Boolean,
    isScreenCaptureEnabled: Boolean,
    isAiRunning: Boolean,
    aiGoal: String,
    aiStep: Int,
    aiMessage: String,
    addLog: (String, LogType) -> Unit,
    onRequestScreenCapture: () -> Unit,
    onStartAi: (String) -> Unit,
    onStopAi: () -> Unit,
    onAnalyzeScreen: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("AI自主控制", style = MaterialTheme.typography.titleLarge)
                
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("输入你想让AI做的事情") },
                    placeholder = { Text("例如: 打开抖音，刷5个视频") },
                    readOnly = isAiRunning,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (!isAiRunning) {
                    Button(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                if (!isAccessibilityEnabled) {
                                    addLog("请先开启无障碍服务", LogType.ERROR)
                                    return@Button
                                }
                                if (!isScreenCaptureEnabled) {
                                    addLog("请先获取屏幕录制权限", LogType.ERROR)
                                    return@Button
                                }
                                onStartAi(inputText)
                            }
                        },
                        enabled = inputText.isNotBlank() && isAccessibilityEnabled && isScreenCaptureEnabled,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("开始执行")
                    }
                } else {
                    Button(
                        onClick = onStopAi,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("停止AI")
                    }
                }
            }
        }
        
        if (isAiRunning) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("执行状态", style = MaterialTheme.typography.titleMedium)
                    Text("目标: $aiGoal")
                    Text("步骤: $aiStep")
                    Text("状态: $aiMessage")
                }
            }
        }
        
        HorizontalDivider()
        
        Text("测试功能", style = MaterialTheme.typography.titleMedium)
        
        Button(
            onClick = onAnalyzeScreen,
            enabled = isScreenCaptureEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Search, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("分析当前屏幕")
        }
        
        if (!isScreenCaptureEnabled) {
            Button(
                onClick = onRequestScreenCapture,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("获取屏幕录制权限")
            }
        }
        
        HorizontalDivider()
        
        Text("示例任务", style = MaterialTheme.typography.titleMedium)
        
        val examples = listOf(
            "打开抖音，刷视频",
            "返回主页",
            "向上滑动页面",
            "点击屏幕中央"
        )
        
        examples.forEach { example ->
            Button(
                onClick = {
                    if (!isAccessibilityEnabled) {
                        addLog("请先开启无障碍服务", LogType.ERROR)
                        return@Button
                    }
                    if (!isScreenCaptureEnabled) {
                        addLog("请先获取屏幕录制权限", LogType.ERROR)
                        return@Button
                    }
                    onStartAi(example)
                },
                enabled = !isAiRunning && isAccessibilityEnabled && isScreenCaptureEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(example)
            }
        }
    }
}

@Composable
fun ScriptsTab(
    isAccessibilityEnabled: Boolean,
    addLog: (String, LogType) -> Unit,
    scriptExecutor: ScriptExecutor,
    onExecutionStateChange: (Boolean, Pair<Int, Int>) -> Unit
) {
    val scripts = ScriptManager.getAllScripts()
    var selectedScript by remember { mutableStateOf<com.autonomous.phone.script.Script?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        scriptExecutor.onActionExecuted = { index, action ->
            addLog("执行: ${action.describe()}", LogType.ACTION)
            selectedScript?.let { script ->
                onExecutionStateChange(true, index to script.actions.size)
            }
        }
        scriptExecutor.onScriptComplete = {
            addLog("脚本执行完成", LogType.SUCCESS)
            onExecutionStateChange(false, 0 to 0)
        }
        scriptExecutor.onError = { error ->
            addLog("执行错误: ${error.message}", LogType.ERROR)
            onExecutionStateChange(false, 0 to 0)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("可用脚本", style = MaterialTheme.typography.titleMedium)
        
        scripts.forEach { script ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedScript = script },
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedScript?.id == script.id) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(script.name, style = MaterialTheme.typography.titleMedium)
                        Button(
                            onClick = {
                                if (isAccessibilityEnabled && !scriptExecutor.isRunning) {
                                    selectedScript = script
                                    addLog("开始执行脚本: ${script.name}", LogType.INFO)
                                    coroutineScope.launch {
                                        scriptExecutor.execute(script)
                                    }
                                }
                            },
                            enabled = isAccessibilityEnabled && !scriptExecutor.isRunning
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("运行")
                        }
                    }
                    Text(script.description, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "${script.actions.size} 个动作",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
        
        if (scriptExecutor.isRunning) {
            Button(
                onClick = {
                    scriptExecutor.stop()
                    addLog("停止脚本执行", LogType.INFO)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("停止")
            }
        }
    }
}

@Composable
fun ElementsTab(
    isAccessibilityEnabled: Boolean,
    onRefresh: () -> Unit,
    screenElements: List<com.autonomous.phone.device.ScreenElement>,
    addLog: (String, LogType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("屏幕元素", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = onRefresh,
                enabled = isAccessibilityEnabled
            ) {
                Text("刷新")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (screenElements.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无元素", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(screenElements.filter { 
                    it.text != null || it.contentDescription != null
                }) { element ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (element.isClickable) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .clickable(enabled = isAccessibilityEnabled && element.isClickable) {
                                    DeviceController.clickElement(element)
                                    addLog("点击元素: ${element.getDisplayText()}", LogType.ACTION)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = element.getTypeIcon(),
                                fontSize = 20.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = element.getDisplayText(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "${element.bounds.left},${element.bounds.top} - ${element.bounds.right},${element.bounds.bottom}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogsTab(logEntries: List<LogEntry>) {
    if (logEntries.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("暂无日志", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            reverseLayout = true
        ) {
            items(logEntries.size, key = { index -> logEntries[logEntries.size - 1 - index].timestamp }) { index ->
                val entry = logEntries[logEntries.size - 1 - index]
                val bgColor = when (entry.type) {
                    LogType.INFO -> MaterialTheme.colorScheme.surfaceVariant
                    LogType.SUCCESS -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    LogType.ERROR -> MaterialTheme.colorScheme.errorContainer
                    LogType.ACTION -> MaterialTheme.colorScheme.tertiaryContainer
                    LogType.AI -> MaterialTheme.colorScheme.secondaryContainer
                }
                val textColor = when (entry.type) {
                    LogType.ERROR -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onSurface
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = bgColor)
                ) {
                    Text(
                        text = entry.message,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    )
    return enabledServices?.contains(context.packageName) == true
}

private fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    context.startActivity(intent)
}
