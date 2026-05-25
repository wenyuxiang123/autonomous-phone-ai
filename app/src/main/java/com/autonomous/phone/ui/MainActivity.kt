package com.autonomous.phone.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autonomous.phone.core.AutonomousEngine
import com.autonomous.phone.core.TaskState
import com.autonomous.phone.device.ScreenCaptureManager
import com.autonomous.phone.model.ModelManager
import com.autonomous.phone.service.AutonomousAccessibilityService
import com.autonomous.phone.service.ScreenCaptureService
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val autonomousEngine: AutonomousEngine,
    private val screenCaptureManager: ScreenCaptureManager,
    private val modelManager: ModelManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _taskState = MutableStateFlow(TaskState())
    val taskState: StateFlow<TaskState> = _taskState.asStateFlow()

    init {
        viewModelScope.launch {
            autonomousEngine.state.collect { state ->
                _taskState.value = state
            }
        }
    }

    fun updateGoal(goal: String) {
        _uiState.value = _uiState.value.copy(currentGoal = goal)
    }

    fun checkPermissions(context: Context): PermissionStatus {
        val accessibilityEnabled = isAccessibilityServiceEnabled(context)
        val overlayEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
        val screenCaptureEnabled = screenCaptureManager.hasProjectionPermission()

        return PermissionStatus(
            accessibility = accessibilityEnabled,
            overlay = overlayEnabled,
            screenCapture = screenCaptureEnabled
        )
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(context.packageName) == true
    }

    fun startTask(context: Context) {
        val goal = _uiState.value.currentGoal
        if (goal.isBlank()) return

        val permissions = checkPermissions(context)
        if (!permissions.allGranted()) {
            _uiState.value = _uiState.value.copy(
                showPermissionDialog = true,
                permissionStatus = permissions
            )
            return
        }

        if (!screenCaptureManager.isCapturing()) {
            screenCaptureManager.startCapture()
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            autonomousEngine.startTask(goal)
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun stopTask() {
        autonomousEngine.stopTask()
    }

    fun setProjectionResult(resultCode: Int, data: Intent?) {
        data?.let {
            screenCaptureManager.setProjectionResult(resultCode, it)
        }
    }

    fun dismissPermissionDialog() {
        _uiState.value = _uiState.value.copy(showPermissionDialog = false)
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        context.startActivity(intent)
    }

    fun openOverlaySettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            context.startActivity(intent)
        }
    }
}

data class MainUiState(
    val currentGoal: String = "",
    val isLoading: Boolean = false,
    val showPermissionDialog: Boolean = false,
    val permissionStatus: PermissionStatus = PermissionStatus()
)

data class PermissionStatus(
    val accessibility: Boolean = false,
    val overlay: Boolean = false,
    val screenCapture: Boolean = false
) {
    fun allGranted(): Boolean = accessibility && overlay && screenCapture
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.setProjectionResult(result.resultCode, result.data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen(viewModel = viewModel) {
                val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as android.media.projection.MediaProjectionManager
                screenCaptureLauncher.launch(projectionManager.createScreenCaptureIntent())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onRequestScreenCapture: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val taskState by viewModel.taskState.collectAsState()

    LaunchedEffect(Unit) {
        val permissions = viewModel.checkPermissions(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("手机自主AI") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PermissionStatusCard(
                permissionStatus = viewModel.checkPermissions(context),
                onOpenAccessibilitySettings = { viewModel.openAccessibilitySettings(context) },
                onOpenOverlaySettings = { viewModel.openOverlaySettings(context) },
                onRequestScreenCapture = onRequestScreenCapture
            )

            TaskInputSection(
                goal = uiState.currentGoal,
                onGoalChange = { viewModel.updateGoal(it) },
                onStart = { viewModel.startTask(context) },
                onStop = { viewModel.stopTask() },
                isRunning = taskState.isRunning,
                isLoading = uiState.isLoading
            )

            TaskStatusSection(
                taskState = taskState
            )

            ExampleTasksSection(
                onSelectTask = { viewModel.updateGoal(it) }
            )
        }
    }

    if (uiState.showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPermissionDialog() },
            title = { Text("需要权限") },
            text = { Text("请授予所有必要权限才能使用此功能") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissPermissionDialog() }) {
                    Text("确定")
                }
            }
        )
    }
}

@Composable
fun PermissionStatusCard(
    permissionStatus: PermissionStatus,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onRequestScreenCapture: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "权限状态",
                style = MaterialTheme.typography.titleMedium
            )

            PermissionItem(
                label = "无障碍服务",
                granted = permissionStatus.accessibility,
                onRequest = onOpenAccessibilitySettings
            )

            PermissionItem(
                label = "悬浮窗",
                granted = permissionStatus.overlay,
                onRequest = onOpenOverlaySettings
            )

            PermissionItem(
                label = "屏幕录制",
                granted = permissionStatus.screenCapture,
                onRequest = onRequestScreenCapture
            )
        }
    }
}

@Composable
fun PermissionItem(
    label: String,
    granted: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        if (granted) {
            Text("✓ 已授予", color = MaterialTheme.colorScheme.primary)
        } else {
            Button(onClick = onRequest) {
                Text("授予")
            }
        }
    }
}

@Composable
fun TaskInputSection(
    goal: String,
    onGoalChange: (String) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    isRunning: Boolean,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "输入任务",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = goal,
                onValueChange = onGoalChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("例如：打开抖音刷视频") },
                maxLines = 3
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isRunning) {
                    Button(
                        onClick = onStop,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("停止")
                    }
                } else {
                    Button(
                        onClick = onStart,
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading && goal.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("开始执行")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskStatusSection(
    taskState: TaskState
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "任务状态",
                style = MaterialTheme.typography.titleMedium
            )

            Text("状态: ${taskState.statusMessage}")

            if (taskState.isRunning) {
                Text("进度: ${taskState.currentStep}/${taskState.totalSteps}")
            }

            taskState.lastAction?.let {
                Text("上次操作: $it")
            }
        }
    }
}

@Composable
fun ExampleTasksSection(
    onSelectTask: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "示例任务",
                style = MaterialTheme.typography.titleMedium
            )

            val examples = listOf(
                "打开抖音，刷视频",
                "打开设置",
                "返回上一页"
            )

            examples.forEach { example ->
                Button(
                    onClick = { onSelectTask(example) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(example)
                }
            }
        }
    }
}
