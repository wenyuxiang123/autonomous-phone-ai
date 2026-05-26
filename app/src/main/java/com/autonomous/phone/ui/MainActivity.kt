package com.autonomous.phone.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autonomous.phone.device.DeviceController
import com.autonomous.phone.script.Action
import com.autonomous.phone.script.Script
import com.autonomous.phone.script.ScriptExecutor
import com.autonomous.phone.script.ScriptManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

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
    INFO, SUCCESS, ERROR, ACTION
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoControlScreen() {
    val context = LocalContext.current
    
    var isAccessibilityEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var currentTab by remember { mutableStateOf(Tab.CONTROLS) }
    var logEntries by remember { mutableStateOf(mutableStateListOf<LogEntry>()) }
    var screenElements by remember { mutableStateOf(emptyList<com.autonomous.phone.device.ScreenElement>()) }
    var currentAppPackage by remember { mutableStateOf<String?>(null) }
    var isExecutingScript by remember { mutableStateOf(false) }
    var currentScriptProgress by remember { mutableStateOf(0 to 0) }
    
    val scriptExecutor = remember { ScriptExecutor() }
    
    LaunchedEffect(Unit) {
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
                        addLog = addLog
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
            
            if (isExecutingScript) {
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

enum class TabItem(val tab: Tab, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    CONTROLS(Tab.CONTROLS, "控制", Icons.Default.ArrowBack),
    SCRIPTS(Tab.SCRIPTS, "脚本", Icons.Default.PlayArrow),
    ELEMENTS(Tab.ELEMENTS, "元素", Icons.Default.List),
    LOGS(Tab.LOGS, "日志", Icons.Default.Home)
}

enum class Tab {
    CONTROLS, SCRIPTS, ELEMENTS, LOGS
}

@Composable
fun ControlsTab(isAccessibilityEnabled: Boolean, addLog: (String, LogType) -> Unit) {
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
                        onClick = { openAccessibilitySettings(androidx.compose.ui.platform.LocalContext.current) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("前往设置")
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
fun ScriptsTab(
    isAccessibilityEnabled: Boolean,
    addLog: (String, LogType) -> Unit,
    scriptExecutor: ScriptExecutor,
    onExecutionStateChange: (Boolean, Pair<Int, Int>) -> Unit
) {
    val scripts = ScriptManager.getAllScripts()
    var selectedScript by remember { mutableStateOf<Script?>(null) }
    
    LaunchedEffect(scriptExecutor.isRunning) {
        onExecutionStateChange(
            scriptExecutor.isRunning,
            if (scriptExecutor.isRunning) {
                scriptExecutor.currentActionIndex to (selectedScript?.actions?.size ?: 0)
            } else {
                0 to 0
            }
        )
    }
    
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
                                    kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
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
                Icon(Icons.Default.Stop, contentDescription = null)
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
                onClick = {
                    onRefresh()
                    addLog("刷新元素列表", LogType.INFO)
                },
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
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(logEntries.size, key = { index -> logEntries[index].timestamp }) { index ->
                val entry = logEntries[index]
                val bgColor = when (entry.type) {
                    LogType.INFO -> MaterialTheme.colorScheme.surfaceVariant
                    LogType.SUCCESS -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    LogType.ERROR -> MaterialTheme.colorScheme.errorContainer
                    LogType.ACTION -> MaterialTheme.colorScheme.tertiaryContainer
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
