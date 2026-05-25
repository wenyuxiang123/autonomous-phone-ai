# 📱 手机自主AI控制框架

完全不经过电脑！手机自己就能：
- 👁️ 看屏幕（视觉理解）
- 🧠 思考（本地AI推理）
- 🤖 动手（自动操作设备）

## 🏗️ 架构

```
┌─────────────────────────────────────────────────┐
│              手机端 APP (完全独立)                │
├─────────────────────────────────────────────────┤
│  ┌─────────────┐    ┌─────────────┐            │
│  │  视觉理解   │ →  │  自主决策   │ →  执行操作  │
│  │ MiniCPM-V   │    │   引擎      │            │
│  └─────────────┘    └─────────────┘            │
│         ↑                 ↓                    │
│  ┌─────────────┐    ┌─────────────┐            │
│  │  屏幕捕获   │    │  设备控制   │            │
│  │   模块      │    │   模块      │            │
│  └─────────────┘    └─────────────┘            │
└─────────────────────────────────────────────────┘
```

## 📂 项目结构

```
autonomous-phone-framework/
├── app/
│   ├── src/main/
│   │   ├── java/com/autonomous/
│   │   │   ├── core/                    # 核心模块
│   │   │   │   ├── AutonomousEngine.kt  # 自主决策引擎
│   │   │   │   └── VisionAnalyzer.kt    # 视觉分析
│   │   │   ├── device/                  # 设备控制
│   │   │   │   ├── DeviceController.kt  # 设备控制器
│   │   │   │   └── ScreenCapture.kt     # 屏幕捕获
│   │   │   ├── service/                 # 后台服务
│   │   │   │   └── AutonomousService.kt # 主服务
│   │   │   ├── ui/                      # 界面
│   │   │   │   └── MainActivity.kt      # 主界面
│   │   │   └── model/                   # 模型管理
│   │   │       └── ModelManager.kt      # 模型管理器
│   │   └── res/                         # 资源文件
│   └── build.gradle.kts
├── README.md
└── build.gradle.kts
```

## 🚀 功能特性

### ✅ 核心能力
- **本地推理**: 使用 llama.cpp 在手机本地运行 AI 模型
- **视觉理解**: MiniCPM-V 2.5 多模态模型理解屏幕内容
- **自主操作**: 无障碍服务实现点击、滑动、输入等操作
- **实时决策**: 根据屏幕状态实时做出决策

### 📱 设备操作
- 点击指定位置
- 滑动手势
- 文字输入
- 系统按键（Home/Back/Recent）
- 打开 APP
- 滚动页面

### 🎯 使用场景
- 自动刷短视频
- 自动回复消息
- 自动签到打卡
- 自动浏览网页
- 自定义任务流程

## 🔧 快速开始

### 1. 环境要求
- Android 8.0+ (API 26+)
- 至少 6GB RAM
- 至少 4GB 存储空间

### 2. 安装步骤

#### 构建 APK
```bash
cd autonomous-phone-framework
./gradlew assembleDebug
```

#### 安装到手机
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 3. 配置权限

打开 APP 后，需要授予以下权限：
- ✅ 无障碍服务
- ✅ 屏幕录制
- ✅ 悬浮窗
- ✅ 存储权限

### 4. 下载模型

首次启动会自动下载 MiniCPM-V 2.5 (Int4) 模型，约 3GB。

## 💡 使用方法

### 基本使用

1. 打开 APP
2. 授予所有权限
3. 在输入框中输入你想让 AI 做的事情
4. 点击"开始执行"
5. AI 会自动理解并操作手机

### 示例任务

```
"打开抖音，刷5个视频"
"打开微信，给张三发消息'你好'"
"打开设置，查看电池使用情况"
"在浏览器中搜索'今天天气'"
```

### 自定义任务

你可以通过 APP 界面创建自定义任务流程：

```
步骤1: 打开 APP [抖音]
步骤2: 等待 2秒
步骤3: 向上滑动 [5次]
步骤4: 完成
```

## 🏗️ 技术实现

### 视觉理解流程

```kotlin
// 1. 捕获屏幕
val screenshot = screenCapture.capture()

// 2. AI 分析屏幕
val analysis = visionAnalyzer.analyze(screenshot)

// 3. 获取界面元素
val elements = deviceController.getScreenElements()

// 4. 结合分析结果
val decision = autonomousEngine.makeDecision(analysis, elements, userGoal)
```

### 自主决策引擎

决策引擎根据以下信息做出判断：
- 当前屏幕内容（视觉分析）
- 界面元素（无障碍服务）
- 用户目标（自然语言）
- 历史操作记录

### 设备控制

使用 Android 无障碍服务 (AccessibilityService) 实现：
- `dispatchGesture()` - 执行手势（点击、滑动）
- `performGlobalAction()` - 系统操作
- `AccessibilityNodeInfo` - 界面元素分析

## 🔒 安全说明

- 所有 AI 推理都在本地完成，不上传任何数据
- 不需要联网（除了首次下载模型）
- 用户可以随时停止 AI 操作
- 建议在测试环境先试用

## 📝 配置说明

### 模型配置

在 `ModelManager.kt` 中配置：
```kotlin
val MODEL_CONFIG = ModelConfig(
    modelName = "MiniCPM-V-2_5-Int4",
    modelPath = "/sdcard/Models/minicpm-v-2_5-int4.gguf",
    maxContextSize = 4096,
    temperature = 0.7f
)
```

### 决策配置

在 `AutonomousEngine.kt` 中调整：
```kotlin
val ENGINE_CONFIG = EngineConfig(
    maxSteps = 50,           // 最大执行步数
    stepDelay = 1000L,       // 每步间隔(ms)
    confidenceThreshold = 0.8 // 决策置信度阈值
)
```

## 🤝 贡献

欢迎提交 PR 和 Issue！

## 📄 License

MIT License
