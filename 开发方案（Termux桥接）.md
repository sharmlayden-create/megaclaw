# Megaclaw 后续开发方案

> 架构核心：**OpenClaw (Node.js) 运行在 Termux 中负责 AI 决策，Megaclaw App 通过本地 HTTP BridgeServer 提供屏幕感知与操作执行能力**。

---

## 一、已完成清单（Phase 0 — UI 层）

| 模块 | 文件 | 状态 |
|------|------|------|
| 项目骨架 | Gradle + Hilt + Compose + Retrofit + Room 依赖已配置 | ✅ |
| 应用图标 | `ic_megaclaw.xml` 矢量图 + Adaptive Icon | ✅ |
| 欢迎页 | `WelcomeHero.kt` — 矢量图标 + 呼吸缩放动效 | ✅ |
| 输入框 | `GradientBorderInputBar.kt` — 彩色流光渐变边框 + 辉光动效 | ✅ |
| 聊天气泡 | `ChatBubble.kt` — 用户黑底 / AI 毛玻璃 + 弹入动画 | ✅ |
| 聊天主屏 | `ChatScreen.kt` — LazyColumn + 径向渐变背景 + 自动滚动 | ✅ |
| ViewModel | `ChatViewModel.kt` — 消息列表管理，当前硬编码 AI 回复 | ✅ |
| 数据模型 | `ChatMessage.kt` — id / role / content / timestamp | ✅ |
| 主题 | `Theme.kt` — Material3 浅色，背景色 #FFF8F8 | ✅ |
| 包结构 | `ui/`, `data/`, `engine/`, `perception/`, `executor/`, `network/`, `di/` 目录已创建 | ✅ |

**当前能力边界**：UI 可交互，消息收发流程跑通，但 AI 回复是假数据。无 Termux 环境、无 BridgeServer、无屏幕感知、无自动化执行。

---

## 二、整体架构

```
┌──────────────────────────────────────────────────────────────────┐
│                         Android 设备                              │
│                                                                  │
│  ┌─────────────────────┐         ┌────────────────────────────┐  │
│  │   Megaclaw App      │         │   Termux 环境               │  │
│  │   (Kotlin/Compose)  │         │   (proot Linux)            │  │
│  │                     │         │                            │  │
│  │  ┌───────────────┐  │  HTTP   │  ┌──────────────────────┐  │  │
│  │  │  Chat UI      │  │ ◄─────► │  │  OpenClaw (Node.js)  │  │  │
│  │  │  [已完成]      │  │         │  │                      │  │  │
│  │  └───────────────┘  │         │  │  - CoT ActionLoop     │  │  │
│  │                     │         │  │  - Prompt Engine       │  │  │
│  │  ┌───────────────┐  │         │  │  - Tool Definitions   │  │  │
│  │  │ BridgeServer  │  │         │  │                      │  │  │
│  │  │ 127.0.0.1:    │  │         │  └──────┬───────────────┘  │  │
│  │  │   18800       │  │         │         │                  │  │
│  │  │               │  │         │         ↕ HTTPS            │  │
│  │  │ /action       │  │         │  SiliconFlow API           │  │
│  │  │ /screenshot   │  │         │  (Qwen-2.5-72B)            │  │
│  │  │ /screen_tree  │  │         └────────────────────────────┘  │
│  │  │ /status       │  │                                        │
│  │  └───────────────┘  │                                        │
│  │                     │                                        │
│  │  ┌───────────────┐  │                                        │
│  │  │Accessibility  │  │                                        │
│  │  │Service        │  │                                        │
│  │  │- ScreenAnalyzer│ │                                        │
│  │  │- Automator    │  │                                        │
│  │  └───────────────┘  │                                        │
│  │                     │                                        │
│  │  ┌───────────────┐  │                                        │
│  │  │MediaProjection│  │                                        │
│  │  │- Screenshot   │  │                                        │
│  │  └───────────────┘  │                                        │
│  └─────────────────────┘                                        │
└──────────────────────────────────────────────────────────────────┘
```

**数据流**：

```
用户在 Megaclaw UI 输入指令
    ↓ (App 内部)
ChatViewModel 通过 HTTP 发送到 Termux 中的 OpenClaw
    ↓ (Termux 内)
OpenClaw CoT 循环启动，调用 Qwen-2.5-72B
    ↓ 模型返回 { Thought, Action, Status }
OpenClaw 通过 HTTP 调用 BridgeServer 端点
    ↓
    ├─ GET /screen_tree → AccessibilityService 抓取 UI 树 → 返回 JSON
    ├─ POST /screenshot → MediaProjection 截图 → 返回 base64
    ├─ POST /action → AccessibilityAutomator 执行 click/type/scroll
    └─ GET /status → 返回服务状态
    ↓
OpenClaw 根据执行结果决定 continue/completed
    ↓ 实时推送 thought/status 回 App
ChatViewModel 更新 UI 气泡
```

---

## 三、开发阶段

### Phase 1：Termux 环境集成

**目标**：App 内嵌 Termux 运行时，能在 x86_64 设备上启动 Linux shell。

#### 1.1 获取 x86_64 Termux Bootstrap

这是**最关键的前置依赖**。三条路径（按优先级）：

| 方案 | 操作 | 风险 |
|------|------|------|
| A. 官方 Release | 从 `github.com/termux/termux-packages/releases` 下载 `bootstrap-x86_64.zip` | 可能不存在预编译包 |
| B. APK 导出 | 在 x86_64 模拟器上安装 Termux APK，从 `$PREFIX` 目录打包导出 | 稳定可靠，推荐 |
| C. 交叉编译 | clone `termux-packages`，使用官方构建脚本交叉编译 x86_64 | 耗时最长但最可控 |

#### 1.2 Bootstrap 集成到 App

```
com.megaclaw.termux/
├── TermuxInstaller.kt        # 首次启动时解压 bootstrap zip 到 App 私有目录
├── TermuxSession.kt          # 管理 shell 进程生命周期
├── TermuxConstants.kt        # PREFIX、HOME、BIN 等路径常量
└── assets/
    └── bootstrap-x86_64.zip  # 打包在 APK assets 中（~50-80MB）
```

**关键逻辑**：

```kotlin
class TermuxInstaller(private val context: Context) {
    private val prefixDir = File(context.filesDir, "usr")

    suspend fun install() {
        if (prefixDir.exists()) return  // 已安装
        val zipStream = context.assets.open("bootstrap-x86_64.zip")
        unzipTo(zipStream, context.filesDir)
        // chmod +x 所有 $PREFIX/bin/ 下的二进制
        fixPermissions(File(prefixDir, "bin"))
    }
}
```

#### 1.3 Shell 进程管理

```kotlin
class TermuxSession(private val prefixDir: File) {
    private var process: Process? = null

    fun startShell(): Process {
        val env = arrayOf(
            "HOME=${prefixDir.parent}/home",
            "PREFIX=${prefixDir.absolutePath}",
            "PATH=${prefixDir}/bin:${'$'}PATH",
            "LD_LIBRARY_PATH=${prefixDir}/lib",
            "LANG=en_US.UTF-8"
        )
        process = Runtime.getRuntime().exec(
            arrayOf("${prefixDir}/bin/sh"),
            env,
            File(prefixDir.parent, "home")
        )
        return process!!
    }

    fun execute(command: String): String {
        // 通过 stdin/stdout 与 shell 交互
    }

    fun destroy() { process?.destroy() }
}
```

#### 1.4 验收标准

- App 启动后自动解压 bootstrap
- 能执行 `sh -c "echo hello"` 并读取输出
- `$PREFIX/bin/` 下主要工具可用（sh, ls, cat, curl 等）

---

### Phase 2：Node.js + OpenClaw 安装

**目标**：在 Termux 环境中安装 Node.js 和 OpenClaw，能通过命令行运行。

#### 2.1 Node.js 安装

```kotlin
// 在 TermuxSession 中执行
session.execute("pkg install -y nodejs-lts")  // Node 22 LTS
session.execute("node --version")  // 验证 v22.x
session.execute("npm --version")   // 验证 10.x
```

如果 `pkg` 不可用（bootstrap 未包含包管理器），则：
- 下载预编译 Node.js x86_64 Linux 二进制
- 解压到 `$PREFIX/` 下
- 手动设置 `PATH`

#### 2.2 OpenClaw 安装

```bash
npm install -g openclaw@latest --ignore-scripts
openclaw --version
```

#### 2.3 OpenClaw 配置

```kotlin
// 写入 ~/.config/openclaw/openclaw.json
val config = """
{
  "provider": "siliconflow",
  "model": "Qwen/Qwen2.5-72B-Instruct",
  "apiKey": "$apiKey",
  "baseUrl": "https://api.siliconflow.cn/v1",
  "bridge": {
    "host": "127.0.0.1",
    "port": 18800,
    "authToken": "$bridgeToken"
  }
}
""".trimIndent()
session.execute("mkdir -p ~/.config/openclaw")
session.writeFile("~/.config/openclaw/openclaw.json", config)
```

#### 2.4 API Key 管理

- 使用 `EncryptedSharedPreferences` 在 App 侧安全存储
- 首次启动引导用户输入
- 写入 OpenClaw 配置时动态注入

#### 2.5 验收标准

- `node --version` 返回 v18+
- `openclaw --version` 返回版本号
- OpenClaw 配置文件写入成功

---

### Phase 3：BridgeServer（App ↔ OpenClaw 通讯桥梁）

**目标**：在 App 内运行一个本地 HTTP 服务，让 Termux 中的 OpenClaw 能调用 Android 原生能力。

#### 3.1 服务端点定义

| 端点 | 方法 | 功能 | 请求体 | 响应体 |
|------|------|------|--------|--------|
| `/status` | GET | 服务状态查询 | - | `{ "status": "ready", "accessibility": true, "mediaProjection": true }` |
| `/screen_tree` | GET | 获取屏幕 UI 树 | - | `{ "package": "...", "elements": [...] }` |
| `/screenshot` | POST | 截取屏幕 | `{ "quality": 80 }` | `{ "image": "base64...", "width": 2560, "height": 1600 }` |
| `/action` | POST | 执行操作 | `{ "type": "click", "x": 540, "y": 960 }` | `{ "success": true }` |
| `/message` | POST | 推送消息到 UI | `{ "role": "ai", "content": "...", "thought": "..." }` | `{ "ok": true }` |

#### 3.2 嵌入式 HTTP 服务器

```
com.megaclaw.bridge/
├── BridgeServer.kt            # NanoHTTPD 或 Ktor 嵌入式 HTTP 服务器
├── BridgeRoutes.kt            # 路由分发
├── BridgeAuthInterceptor.kt   # Token 鉴权（防止其他 App 调用）
└── BridgeConfig.kt            # host、port、authToken 配置
```

技术选择：使用 **NanoHTTPD**（轻量级，单文件嵌入，无额外依赖）或 **Ktor Server Netty**（已在 Kotlin 生态中，协程友好）。

```kotlin
class BridgeServer(
    private val port: Int = 18800,
    private val authToken: String,
    private val screenAnalyzer: ScreenAnalyzer,
    private val automator: AccessibilityAutomator,
    private val screenshotProvider: ScreenshotProvider,
    private val onMessage: (String, String) -> Unit  // role, content → 更新 UI
) : NanoHTTPD("127.0.0.1", port) {

    override fun serve(session: IHTTPSession): Response {
        // 验证 Authorization header
        if (session.headers["authorization"] != "Bearer $authToken") {
            return newFixedLengthResponse(Status.UNAUTHORIZED, MIME_JSON, """{"error":"unauthorized"}""")
        }

        return when (session.uri) {
            "/status"      -> handleStatus()
            "/screen_tree" -> handleScreenTree()
            "/screenshot"  -> handleScreenshot(session)
            "/action"      -> handleAction(session)
            "/message"     -> handleMessage(session)
            else           -> newFixedLengthResponse(Status.NOT_FOUND, MIME_JSON, """{"error":"not found"}""")
        }
    }

    private fun handleScreenTree(): Response {
        val json = screenAnalyzer.captureScreen().toJson()
        return newFixedLengthResponse(Status.OK, MIME_JSON, json)
    }

    private fun handleAction(session: IHTTPSession): Response {
        val body = parseBody(session)
        when (body.type) {
            "click"     -> automator.click(body.x!!, body.y!!)
            "type_text" -> automator.typeText(body.text!!)
            "scroll"    -> automator.scroll(body.direction!!)
            "wait"      -> Thread.sleep(body.ms ?: 500)
        }
        return newFixedLengthResponse(Status.OK, MIME_JSON, """{"success":true}""")
    }

    private fun handleMessage(session: IHTTPSession): Response {
        val body = parseBody(session)
        onMessage(body.role, body.content)  // 推送到 ChatViewModel
        return newFixedLengthResponse(Status.OK, MIME_JSON, """{"ok":true}""")
    }
}
```

#### 3.3 安全措施

- 仅监听 `127.0.0.1`（本机回环），外部不可访问
- 每次启动生成随机 `authToken`，写入 OpenClaw 配置
- 所有端点要求 `Authorization: Bearer <token>` 头

#### 3.4 Foreground Service

BridgeServer 必须在 Foreground Service 中运行，保证后台不被杀：

```kotlin
class BridgeService : Service() {
    private lateinit var server: BridgeServer

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        server = BridgeServer(...)
        server.start()
        return START_STICKY
    }

    override fun onDestroy() {
        server.stop()
        super.onDestroy()
    }
}
```

#### 3.5 验收标准

- BridgeServer 启动在 `127.0.0.1:18800`
- 从 Termux shell 中 `curl http://127.0.0.1:18800/status` 返回 JSON
- `/screen_tree` 返回当前屏幕的无障碍树
- `/action` 能执行点击操作

---

### Phase 4：感知层 + 执行层（Android 原生能力）

**目标**：为 BridgeServer 提供真正的屏幕读写后端。

#### 4.1 无障碍服务

```xml
<!-- AndroidManifest.xml -->
<service android:name=".perception.MegaclawAccessibilityService"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
    android:exported="false">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_config" />
</service>
```

```xml
<!-- res/xml/accessibility_config.xml -->
<accessibility-service
    android:accessibilityEventTypes="typeAllMask"
    android:accessibilityFlags="flagReportViewIds|flagRetrieveInteractiveWindows"
    android:canPerformGestures="true"
    android:canRetrieveWindowContent="true"
    android:notificationTimeout="100" />
```

#### 4.2 ScreenAnalyzer（为 /screen_tree 端点服务）

```
com.megaclaw.perception/
├── MegaclawAccessibilityService.kt
├── ScreenAnalyzer.kt
└── ScreenSnapshot.kt
```

- 递归遍历 `AccessibilityNodeInfo` 树
- 过滤不可见、面积为 0 的节点
- 提取：`text`, `contentDescription`, `className`, `bounds`, `clickable`, `scrollable`, `viewIdResourceName`
- 隐私过滤：password 类型节点内容替换为 `[REDACTED]`
- 输出精简 JSON，控制在 4000 token 以内

```json
{
  "package": "com.android.settings",
  "activity": "Settings",
  "screenSize": [2560, 1600],
  "elements": [
    { "id": "wifi_item", "text": "WLAN", "type": "LinearLayout",
      "bounds": [0, 200, 1080, 320], "clickable": true }
  ]
}
```

#### 4.3 AccessibilityAutomator（为 /action 端点服务）

```
com.megaclaw.executor/
├── AccessibilityAutomator.kt
└── GestureBuilder.kt
```

```kotlin
class AccessibilityAutomator(private val service: AccessibilityService) {

    fun click(x: Int, y: Int) {
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 100)
        service.dispatchGesture(
            GestureDescription.Builder().addStroke(stroke).build(), null, null
        )
    }

    fun typeText(text: String) {
        // 方案 A：焦点节点 ACTION_SET_TEXT
        // 方案 B：InputConnection 逐字输入（兼容性更好）
    }

    fun scroll(direction: String) {
        // 构建滑动轨迹，距离 = 屏幕尺寸 * 40%
    }
}
```

#### 4.4 ScreenshotProvider（为 /screenshot 端点服务）

```kotlin
class ScreenshotProvider(private val mediaProjection: MediaProjection) {
    fun capture(): Bitmap {
        // MediaProjection → ImageReader → Bitmap
        // 压缩为 JPEG quality 80
        // 返回 base64 编码
    }
}
```

- 需要 `MediaProjection` 权限，启动时弹出系统授权弹窗
- Android 14+ 支持持久化授权

#### 4.5 用户中断机制

- 悬浮窗监测用户触摸 → `isUserInterrupted = true`
- BridgeServer 在 `/action` 端点执行前检查中断标志
- 中断时返回 `{ "success": false, "reason": "user_interrupted" }`
- OpenClaw 侧收到中断信号后停止 ActionLoop

#### 4.6 验收标准

- `/screen_tree` 返回准确的 UI 元素 JSON
- `/screenshot` 返回当前屏幕 base64 图片
- `/action` 执行 click 后屏幕确实发生变化
- 用户触摸屏幕时操作立即中断

---

### Phase 5：OpenClaw 适配层（Fork 改造）

**目标**：让 OpenClaw 的 Computer Use 模块通过 BridgeServer 调用 Android 能力，而非桌面端 Python 截图。

#### 5.1 需要 Fork 的原因

OpenClaw 原生的 tool 实现（截图、点击、输入）绑定桌面端 API（如 `pyautogui`、Puppeteer 等）。需要 fork 一份，将这些 tool 的底层调用替换为 HTTP 请求到 BridgeServer。

#### 5.2 Tool 适配映射

| OpenClaw 原始 Tool | 原实现 | Megaclaw 适配 |
|-------------------|--------|--------------|
| `computer.screenshot()` | Python/桌面截图 | `GET http://127.0.0.1:18800/screenshot` |
| `computer.click(x, y)` | `pyautogui.click()` | `POST /action { "type": "click", "x": x, "y": y }` |
| `computer.type(text)` | `pyautogui.write()` | `POST /action { "type": "type_text", "text": text }` |
| `computer.scroll(dir)` | `pyautogui.scroll()` | `POST /action { "type": "scroll", "direction": dir }` |
| `computer.screen_tree()` | 无（新增） | `GET /screen_tree` |

#### 5.3 桥接客户端模块

在 OpenClaw fork 中新增 `megaclaw-bridge-client`：

```javascript
// megaclaw-bridge-client.js
const http = require('http');
const config = require(process.env.HOME + '/.config/openclaw/openclaw.json');

const bridge = {
  host: config.bridge.host,
  port: config.bridge.port,
  token: config.bridge.authToken
};

async function callBridge(path, method = 'GET', body = null) {
  return new Promise((resolve, reject) => {
    const options = {
      hostname: bridge.host,
      port: bridge.port,
      path: path,
      method: method,
      headers: {
        'Authorization': `Bearer ${bridge.token}`,
        'Content-Type': 'application/json'
      }
    };
    const req = http.request(options, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => resolve(JSON.parse(data)));
    });
    req.on('error', reject);
    if (body) req.write(JSON.stringify(body));
    req.end();
  });
}

module.exports = {
  screenshot: () => callBridge('/screenshot', 'POST', { quality: 80 }),
  screenTree: () => callBridge('/screen_tree'),
  action: (payload) => callBridge('/action', 'POST', payload),
  status: () => callBridge('/status'),
  message: (role, content) => callBridge('/message', 'POST', { role, content })
};
```

#### 5.4 OpenClaw 侧修改清单

1. 替换 `computer` tool 的底层实现，指向 `megaclaw-bridge-client`
2. 新增 `screen_tree` tool，让模型能读取结构化 UI 树（比纯截图更高效）
3. 修改 System Prompt，增加 Android 上下文描述（屏幕尺寸、可用操作等）
4. 调整 ActionLoop 的 `status` 回调，通过 `/message` 端点实时推送 thought 到 Megaclaw UI

#### 5.5 验收标准

- OpenClaw 启动后能通过 BridgeServer 获取屏幕信息
- 输入"打开设置"→ OpenClaw 分析屏幕 → 找到设置图标 → 执行点击 → 设置 App 打开
- 整个 CoT 过程的 thought 实时显示在 Megaclaw 聊天气泡中

---

### Phase 6：App ↔ OpenClaw 全链路串联

**目标**：用户在 Megaclaw UI 输入指令 → OpenClaw 执行 → 结果回显到 UI。

#### 6.1 ChatViewModel 改造

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val termuxSession: TermuxSession,
    private val bridgeServer: BridgeServer
) : ViewModel() {

    val messages = mutableStateListOf<ChatMessage>()

    fun send(text: String) {
        if (text.isBlank()) return
        messages.add(ChatMessage(role = Role.USER, content = text.trim()))

        viewModelScope.launch {
            // 通过 Termux shell 向 OpenClaw 发送指令
            termuxSession.execute("openclaw run '${text.trim()}'")
            // OpenClaw 会通过 /message 端点回推结果
        }
    }

    // BridgeServer 收到 /message 时调用此方法
    fun onBridgeMessage(role: String, content: String, thought: String?) {
        if (thought != null) {
            messages.add(ChatMessage(role = Role.AI, content = "💭 $thought"))
        }
        if (content.isNotBlank()) {
            messages.add(ChatMessage(role = Role.AI, content = content))
        }
    }
}
```

#### 6.2 进程编排（启动顺序）

```
App 启动
  ↓
1. TermuxInstaller.install()       — 解压 bootstrap（仅首次）
  ↓
2. BridgeService.start()           — 启动 Foreground Service + BridgeServer
  ↓
3. 检查无障碍服务是否开启           — 未开启则引导
  ↓
4. 检查 MediaProjection 权限       — 未授权则请求
  ↓
5. TermuxSession.startShell()      — 启动 Termux shell
  ↓
6. session.execute("openclaw")     — 启动 OpenClaw 守护
  ↓
7. UI 就绪，等待用户输入
```

#### 6.3 进程保活

| 组件 | 保活策略 |
|------|---------|
| BridgeServer | Foreground Service + START_STICKY |
| Termux Shell | 由 BridgeService 管理子进程，crash 后自动重启 |
| AccessibilityService | 系统管理，断开时通过 UI 提示用户重新开启 |
| MediaProjection | 通过 Callback 持久化授权 (Android 14+) |

#### 6.4 验收标准

- 完整链路跑通：UI 输入 → OpenClaw 决策 → BridgeServer 执行 → UI 回显
- 进程后台运行稳定，不会被系统杀死
- 用户触摸中断后，恢复到可交互状态

---

### Phase 7：权限引导 + 体验打磨

**目标**：首次启动顺畅，操作过程可视化。

#### 7.1 首次启动引导流

```
WelcomeScreen（展示 Megaclaw 图标 + 呼吸动效）
    ↓ "开始使用"
Step 1: 输入 SiliconFlow API Key → 验证连通性
    ↓ 验证通过
Step 2: 安装 Termux 环境 → 进度条（解压 bootstrap + 安装 Node.js + 安装 OpenClaw）
    ↓ 安装完成
Step 3: 开启无障碍服务 → 跳转系统设置，轮询检测状态
    ↓ 检测到已开启
Step 4: 授权屏幕录制 → MediaProjection 弹窗
    ↓
Step 5: 开启悬浮窗权限 → Settings.canDrawOverlays()
    ↓
Ready → 进入主聊天界面
```

#### 7.2 操作可视化 Overlay

- 悬浮 Overlay 层显示 AI 正在点击的位置（红色涟漪动效）
- 底部半透明卡片展示当前 thought
- 随时可点击的中断按钮

#### 7.3 设置页

- API Key 管理
- Termux 环境状态（已安装 / Node 版本 / OpenClaw 版本）
- BridgeServer 状态（运行中 / 端口 / 连接数）
- 模型选择
- 最大循环次数
- 是否显示思考过程

#### 7.4 数据持久化

- Room 存储对话历史
- DataStore 存储用户配置

---

### Phase 8：安全与健壮性

#### 8.1 敏感操作拦截

OpenClaw 发来的 `/action` 请求，BridgeServer 在执行前检查：
- 目标节点是否包含"删除"、"发送"、"支付"、"转账"等敏感词
- 命中时通过 `/message` 推送确认对话框到 UI
- 用户点击"允许"后才继续执行

#### 8.2 隐私保护

- ScreenAnalyzer 导出时剔除 password 类型节点内容
- 截图发送前模糊处理通知栏敏感信息
- BridgeServer 仅监听 127.0.0.1 + Token 鉴权

#### 8.3 错误恢复

| 错误场景 | 处理策略 |
|----------|---------|
| Termux 进程崩溃 | BridgeService 自动重启 shell + OpenClaw |
| BridgeServer 端口被占 | 尝试备选端口 18801-18810 |
| OpenClaw 安装失败 | 重试 + 提供手动安装引导 |
| 无障碍服务断开 | UI 提示 + 跳转设置页 |
| API 超时 | OpenClaw 内部重试（模型侧处理） |
| 操作后屏幕无变化 | OpenClaw 内部告知模型"屏幕未变化" |

---

## 四、文件规划总览

```
com.megaclaw/
├── MegaclawApp.kt                          [已有]
├── ui/
│   ├── theme/Theme.kt                      [已有]
│   ├── MainActivity.kt                     [已有]
│   ├── ChatScreen.kt                       [已有] → Phase 6 改造
│   ├── ChatViewModel.kt                    [已有] → Phase 6 改造
│   ├── SettingsScreen.kt                   Phase 7
│   ├── OnboardingScreen.kt                 Phase 7
│   ├── navigation/NavGraph.kt              Phase 7
│   └── components/
│       ├── WelcomeHero.kt                  [已有]
│       ├── GradientBorderInputBar.kt       [已有]
│       ├── ChatBubble.kt                   [已有]
│       ├── ThinkingBubble.kt               Phase 6（思考过程气泡）
│       └── OperationOverlay.kt             Phase 7（操作可视化）
├── termux/
│   ├── TermuxInstaller.kt                  Phase 1
│   ├── TermuxSession.kt                    Phase 1
│   └── TermuxConstants.kt                  Phase 1
├── bridge/
│   ├── BridgeServer.kt                     Phase 3
│   ├── BridgeService.kt                    Phase 3（Foreground Service）
│   ├── BridgeRoutes.kt                     Phase 3
│   ├── BridgeAuthInterceptor.kt            Phase 3
│   └── BridgeConfig.kt                     Phase 3
├── perception/
│   ├── MegaclawAccessibilityService.kt     Phase 4
│   ├── ScreenAnalyzer.kt                   Phase 4
│   ├── ScreenSnapshot.kt                   Phase 4
│   └── ScreenshotProvider.kt               Phase 4
├── executor/
│   ├── AccessibilityAutomator.kt           Phase 4
│   └── GestureBuilder.kt                   Phase 4
├── data/
│   ├── ChatMessage.kt                      [已有]
│   ├── db/MessageEntity.kt                 Phase 7
│   ├── db/MessageDao.kt                    Phase 7
│   ├── db/MegaclawDatabase.kt              Phase 7
│   └── SettingsRepository.kt               Phase 7
├── di/
│   ├── TermuxModule.kt                     Phase 1
│   ├── BridgeModule.kt                     Phase 3
│   └── DatabaseModule.kt                   Phase 7
└── security/
    ├── SensitiveWordFilter.kt              Phase 8
    └── ConfirmationInterceptor.kt          Phase 8

# OpenClaw Fork（Termux 内运行，独立仓库）
openclaw-megaclaw/
├── ...原有 OpenClaw 代码...
├── tools/
│   └── megaclaw-bridge-client.js           Phase 5（替换桌面端 tool）
└── prompts/
    └── android-system-prompt.txt           Phase 5（Android 专用 Prompt）
```

---

## 五、风险矩阵

| # | 风险 | 概率 | 影响 | 应对 |
|---|------|------|------|------|
| 1 | **x86_64 Termux bootstrap 不可用** | 中 | 致命 | 优先在 x86_64 模拟器上验证方案 B（APK 导出） |
| 2 | OpenClaw Computer Use 模块深度耦合桌面端 | 中 | 高 | 尽早 fork 并评估改造工作量，准备 Plan B（仅用其 Prompt 引擎） |
| 3 | Termux 进程被 Android 系统杀死 | 高 | 中 | Foreground Service + START_STICKY + 电池优化白名单引导 |
| 4 | Node.js x86_64 在 Termux 中不稳定 | 低 | 高 | 备选：使用静态编译的 Node.js 二进制 |
| 5 | API 延迟高（72B 模型 3-5s/步） | 高 | 中 | UI 思考态动效 + 允许用户切换更快的小模型 |
| 6 | MediaProjection 在 Android 16 上的限制 | 中 | 中 | 查阅 API 36 文档，screen_tree 作为无截图的降级方案 |
| 7 | AI 误操作 | 中 | 高 | 敏感操作二次确认 + 用户触摸中断机制 |

---

## 六、里程碑节奏

| 阶段 | 里程碑 | 验收标准 |
|------|--------|---------|
| Phase 1 | Termux 可用 | App 内 bootstrap 解压成功，shell 命令可执行 |
| Phase 2 | OpenClaw 可用 | Node.js + OpenClaw 安装完成，`openclaw --version` 正常 |
| Phase 3 | 桥梁跑通 | BridgeServer 启动，Termux 中 curl 各端点返回正确数据 |
| Phase 4 | 感知+执行 | `/screen_tree` 返回 UI 树，`/action` 能执行点击 |
| Phase 5 | OpenClaw 适配 | Fork 后的 OpenClaw 能通过 Bridge 完成一次屏幕操作 |
| Phase 6 | 全链路串联 | 用户输入 → OpenClaw 决策 → 屏幕操作 → 结果回显 UI |
| Phase 7 | 体验打磨 | 引导流 + 设置页 + 操作可视化 + 对话持久化 |
| Phase 8 | 安全加固 | 敏感拦截 + 隐私脱敏 + 错误自恢复 |

---

## 七、关键决策记录

**Q：为什么选 Termux + OpenClaw 桥接方案？**

- 直接复用 OpenClaw 成熟的 CoT 引擎、Prompt 工程和 tool 体系，避免在 Kotlin 中重写
- OpenClaw 社区持续迭代，fork 后可同步上游改进
- Termux 提供完整 Linux 环境，Node.js 生态（npm 包、调试工具）可直接使用
- 桥接架构解耦清晰：App 只负责"眼睛和手"（感知+执行），OpenClaw 负责"大脑"（决策）

**Q：x86_64 bootstrap 获取不到怎么办？**

Plan B：在 x86_64 Android 模拟器上安装官方 Termux APK，运行一段时间后从 `/data/data/com.termux/files/` 导出整个 `usr/` 目录打包。这是最稳妥的路径。

**Q：OpenClaw 无法适配怎么办？**

Plan B：仅提取 OpenClaw 的 System Prompt 和 tool schema 定义，在 Kotlin 侧自行实现轻量版 ActionLoop，直接调用 SiliconFlow API。此时 Termux 层可去掉，退化为原生方案。
