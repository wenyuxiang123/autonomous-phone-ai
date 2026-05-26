package com.autonomous.phone.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.autonomous.phone.device.DeviceController
import com.autonomous.phone.service.AutoControlAccessibilityService
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoControlScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var isAccessibilityEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var isAutoBrowsing by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        while (true) {
            isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
            delay(500)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("手机自主AI") })
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PermissionCard(
                enabled = isAccessibilityEnabled,
                title = "无障碍服务",
                description = if (isAccessibilityEnabled) "已开启" else "未开启",
                onEnable = { openAccessibilitySettings(context) }
            )
            
            Divider()
            
            Text("测试功能", style = MaterialTheme.typography.titleMedium)
            
            Button(
                onClick = {
                    scope.launch {
                        if (isAccessibilityEnabled) {
                            DeviceController.performClick(540f, 1000f)
                        }
                    }
                },
                enabled = isAccessibilityEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("测试点击")
            }
            
            Button(
                onClick = {
                    scope.launch {
                        if (isAccessibilityEnabled) {
                            DeviceController.scrollDown()
                        }
                    }
                },
                enabled = isAccessibilityEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("测试滑动")
            }
            
            Button(
                onClick = {
                    scope.launch {
                        if (isAccessibilityEnabled) {
                            DeviceController.pressHome()
                        }
                    }
                },
                enabled = isAccessibilityEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("返回首页")
            }
            
            Button(
                onClick = {
                    scope.launch {
                        if (isAccessibilityEnabled) {
                            DeviceController.pressBack()
                        }
                    }
                },
                enabled = isAccessibilityEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("返回上一页")
            }
            
            Divider()
            
            Text("自动功能", style = MaterialTheme.typography.titleMedium)
            
            if (isAutoBrowsing) {
                Button(
                    onClick = { isAutoBrowsing = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("停止自动刷抖音")
                }
                
                LaunchedEffect(Unit) {
                    while (isActive && isAutoBrowsing) {
                        if (isAccessibilityEnabled) {
                            DeviceController.scrollDown()
                        }
                        delay(3000)
                    }
                }
            } else {
                Button(
                    onClick = { isAutoBrowsing = true },
                    enabled = isAccessibilityEnabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("自动刷抖音")
                }
            }
        }
    }
}

@Composable
fun PermissionCard(
    enabled: Boolean,
    title: String,
    description: String,
    onEnable: () -> Unit
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
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (enabled) {
                    Text("已开启", color = MaterialTheme.colorScheme.primary)
                }
            }
            
            Text(description)
            
            if (!enabled) {
                Button(onClick = onEnable, modifier = Modifier.fillMaxWidth()) {
                    Text("前往设置")
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
