# 多人实时竞技与攻击反馈系统 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans` 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 实现多人消消乐实时对战的攻击反馈机制，基于 OkHttp WebSocket 保证 <100ms 延迟同步，应用非线性攻击算法，具备健壮的断线指数退避重连与服务器端优雅离线判定逻辑。（已包含 Android 端与 Cloudflare Workers 后端同步修改，移除单元测试）。

**架构：**
1. **客户端 (Android)**：复用现有的 OkHttpClient 长连接，基于 `Kotlinx.serialization` 进行 JSON 解析，设计 `WebSocketManager` 进行连接/重连生命周期管理；
2. **服务端 (Cloudflare Workers)**：在 `index.ts` 路由中拦截并升级 WebSocket 握手，维护内存中轻量 `GameSession`，提供对战包转发、100ms 操作流控和 15s 指数退避及重连倒计时判负逻辑；
3. **攻击转换**：对数增长曲线 $Attack = Base \times (1 + \ln(Combo))$ 进行伤害转换；

**技术栈：** Kotlin Coroutines, StateFlow/SharedFlow, OkHttp 3 WebSocket, Kotlinx.serialization, TypeScript, Cloudflare Workers WebSockets

---

## Proposed Changes

### 1. 客户端网络与配置模块 (core)

#### [MODIFY] [AppConfig.kt](file:///e:/file/sheeps/app/core/src/main/java/com/example/sheeps/core/AppConfig.kt)
添加 WebSocket 连接地址常量 `WS_URL`。

#### [NEW] [GameCommand.kt](file:///e:/file/sheeps/app/core/src/main/java/com/example/sheeps/core/multiplayer/model/GameCommand.kt)
定义通用的联机对局数据指令模型，包括 `CommandType` 和 `CommandPayload`。

#### [NEW] [AttackCalculator.kt](file:///e:/file/sheeps/app/core/src/main/java/com/example/sheeps/core/multiplayer/AttackCalculator.kt)
实现非线性攻击力计算与物理间隔滑动窗口限流判断。

#### [NEW] [WebSocketManager.kt](file:///e:/file/sheeps/app/core/src/main/java/com/example/sheeps/core/multiplayer/WebSocketManager.kt)
实现基于 OkHttp WebSocket 的核心连接类，支持心跳发送、断线检测、基于指数退避算法的自动重连协商。

### 2. 服务端实时同步模块 (server)

#### [MODIFY] [index.ts](file:///e:/file/sheeps/server/src/index.ts)
1. 拦截 HTTP Upgrade WebSocket 请求，提取 `gameId` 和 `playerId` 参数进行对战房间的内存配对。
2. 捕获客户端指令并支持：
   - 100ms 操作速率限流。
   - 消除指令合法性校验模拟。
   - 攻击指令二次非线性校准计算。
3. 建立 15 秒连接丢失重连窗口期。若超时，宣判离线方负，在线方胜，更新 D1 数据库。

---

## Tasks

### 任务 1：Android 客户端网络基础配置
**文件：**
- 修改：`app/core/src/main/java/com/example/sheeps/core/AppConfig.kt`

- [ ] **步骤 1：在 AppConfig.kt 中添加 WS_URL**
  ```kotlin
  // 在 AppConfig object 内部添加：
  const val WS_URL = "wss://xqh.cc.cd/api/ws"
  ```
- [ ] **步骤 2：验证项目能够正常编译**
  运行：`./gradlew.bat :app:core:assembleDebug`

---

### 任务 2：实现 Android 客户端 GameCommand 数据模型
**文件：**
- 创建：`app/core/src/main/java/com/example/sheeps/core/multiplayer/model/GameCommand.kt`

- [ ] **步骤 1：编写 GameCommand.kt 实体代码**
  ```kotlin
  package com.example.sheeps.core.multiplayer.model

  import kotlinx.serialization.Serializable

  @Serializable
  data class GameCommand(
      val gameId: String,
      val seqId: Long,
      val timestamp: Long,
      val senderId: String,
      val type: CommandType,
      val payload: CommandPayload
  )

  enum class CommandType {
      ELIMINATE,
      ATTACK,
      BUFF,
      RECONNECT_RESUME,
      SYSTEM_EVENT
  }

  @Serializable
  data class CommandPayload(
      val tilesEliminated: List<String>? = null,
      val comboCount: Int = 0,
      val attackPower: Double = 0.0,
      val obstacleType: String? = null,
      val targetPlayerId: String? = null,
      val activeBuffId: String? = null,
      val systemMessage: String? = null
  )
  ```
- [ ] **步骤 2：编译项目确保 GameCommand 编译正常**
  运行：`./gradlew.bat :app:core:assembleDebug`

---

### 任务 3：实现 Android 客户端非线性攻击力计算与滑动窗口限流
**文件：**
- 创建：`app/core/src/main/java/com/example/sheeps/core/multiplayer/AttackCalculator.kt`

- [ ] **步骤 1：编写 AttackCalculator.kt 实现**
  ```kotlin
  package com.example.sheeps.core.multiplayer

  import kotlin.math.ln

  object AttackCalculator {
      /**
       * 非线性攻击力转换公式: Attack = Base * (1 + ln(Combo))
       */
      fun calculateAttackPower(combo: Int, base: Double = 10.0): Double {
          if (combo <= 1) return base
          return base * (1.0 + ln(combo.toDouble()))
      }

      /**
       * 滑动窗口流控限流器，用于防刷接口
       */
      class RateLimiter(private val minIntervalMs: Long = 100) {
          private var lastActionTime: Long = 0L

          @Synchronized
          fun isActionAllowed(currentTimeMs: Long): Boolean {
              if (currentTimeMs - lastActionTime >= minIntervalMs) {
                  lastActionTime = currentTimeMs
                  return true
              }
              return false
          }
      }
  }
  ```
- [ ] **步骤 2：编译项目验证语法合法性**
  运行：`./gradlew.bat :app:core:assembleDebug`

---

### 任务 4：实现 Android 客户端 WebSocketManager 连接与指数退避重连
**文件：**
- 创建：`app/core/src/main/java/com/example/sheeps/core/multiplayer/WebSocketManager.kt`

- [ ] **步骤 1：编写 WebSocketManager.kt 代码**
  ```kotlin
  package com.example.sheeps.core.multiplayer

  import com.example.sheeps.core.AppConfig
  import com.example.sheeps.core.multiplayer.model.GameCommand
  import com.apkfuns.logutils.LogUtils
  import kotlinx.coroutines.*
  import kotlinx.coroutines.flow.MutableSharedFlow
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.SharedFlow
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.serialization.json.Json
  import okhttp3.*
  import java.util.concurrent.TimeUnit
  import javax.inject.Inject
  import javax.inject.Singleton

  @Singleton
  class WebSocketManager @Inject constructor(
      private val okHttpClient: OkHttpClient,
      private val json: Json
  ) {
      private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
      private var webSocket: WebSocket? = null
      private var isManualClose = false
      private var reconnectCount = 0
      private var reconnectJob: Job? = null
      
      private var currentGameId: String? = null
      private var currentPlayerId: String? = null

      private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
      val connectionState: StateFlow<ConnectionState> = _connectionState

      private val _messageFlow = MutableSharedFlow<GameCommand>(extraBufferCapacity = 64)
      val messageFlow: SharedFlow<GameCommand> = _messageFlow

      private val backoffCalculator = BackoffCalculator(1000, 8000)

      sealed class ConnectionState {
          object Connecting : ConnectionState()
          object Connected : ConnectionState()
          data class Reconnecting(val attempt: Int) : ConnectionState()
          object Disconnected : ConnectionState()
      }

      fun connect(gameId: String, playerId: String) {
          isManualClose = false
          reconnectCount = 0
          reconnectJob?.cancel()
          currentGameId = gameId
          currentPlayerId = playerId
          
          val url = "${AppConfig.WS_URL}?gameId=$gameId&playerId=$playerId"
          val request = Request.Builder().url(url).build()
          
          _connectionState.value = ConnectionState.Connecting
          webSocket = okHttpClient.newWebSocket(request, createListener())
      }

      fun sendCommand(command: GameCommand): Boolean {
          val ws = webSocket ?: return false
          return try {
              val jsonString = json.encodeToString(GameCommand.serializer(), command)
              ws.send(jsonString)
          } catch (e: Exception) {
              LogUtils.e("WS Send Error", e)
              false
          }
      }

      fun disconnect() {
          isManualClose = true
          reconnectJob?.cancel()
          webSocket?.close(1000, "User logout/exit")
          webSocket = null
          _connectionState.value = ConnectionState.Disconnected
      }

      private fun createListener() = object : WebSocketListener() {
          override fun onOpen(webSocket: WebSocket, response: Response) {
              LogUtils.i("WebSocket Connected Successfully")
              _connectionState.value = ConnectionState.Connected
              reconnectCount = 0
          }

          override fun onMessage(webSocket: WebSocket, text: String) {
              scope.launch {
                  try {
                      val command = json.decodeFromString(GameCommand.serializer(), text)
                      _messageFlow.emit(command)
                  } catch (e: Exception) {
                      LogUtils.e("WS Message Parse Error: $text", e)
                  }
              }
          }

          override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
              LogUtils.w("WebSocket Closing: $code / $reason")
          }

          override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
              LogUtils.i("WebSocket Closed: $code / $reason")
              if (!isManualClose) {
                  triggerReconnect()
              } else {
                  _connectionState.value = ConnectionState.Disconnected
              }
          }

          override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
              LogUtils.e("WebSocket Failure", t)
              if (!isManualClose) {
                  triggerReconnect()
              } else {
                  _connectionState.value = ConnectionState.Disconnected
              }
          }
      }

      private fun triggerReconnect() {
          if (reconnectJob?.isActive == true) return
          val gameId = currentGameId ?: return
          val playerId = currentPlayerId ?: return
          
          _connectionState.value = ConnectionState.Reconnecting(reconnectCount + 1)
          reconnectJob = scope.launch {
              val delayMs = backoffCalculator.getDelay(reconnectCount)
              LogUtils.w("WebSocket reconnecting in $delayMs ms (attempt ${reconnectCount + 1})")
              delay(delayMs)
              reconnectCount++
              
              val url = "${AppConfig.WS_URL}?gameId=$gameId&playerId=$playerId"
              val request = Request.Builder().url(url).build()
              webSocket = okHttpClient.newWebSocket(request, createListener())
          }
      }

      class BackoffCalculator(private val initialDelayMs: Long, private val maxDelayMs: Long) {
          fun getDelay(attempt: Int): Long {
              val delay = initialDelayMs * (1L shl attempt)
              return if (delay > maxDelayMs || delay <= 0) maxDelayMs else delay
          }
      }
  }
  ```
- [ ] **步骤 2：全工程编译包确保无编译阻碍**
  运行：`./gradlew.bat assembleDebug`

---

### 任务 5：实现 Cloudflare Workers 服务端 WebSocket 两人对决中转逻辑
**文件：**
- 修改：`server/src/index.ts`

- [ ] **步骤 1：添加 WebSocket 会话管理定义与处理路由**
  在 `server/src/index.ts` 中升级 fetch 入口函数并实现对战中转、流控和 15s 掉线重连判定。
  
- [ ] **步骤 2：运行 Workers 本地测试或发布校验**
  在 `server/` 目录下运行：
  `npm run deploy`（或本地测试模式）
