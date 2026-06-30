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

/**
 * 多人联机实时对局 WebSocket 管理器。
 * 负责客户端的握手连接、心跳监测、自动退避重连以及消息的统一收发。
 */
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
    
    // 当前连接的上下文参数，用于掉线重连
    private var currentGameId: String? = null
    private var currentPlayerId: String? = null

    /**
     * 暴露给 UI 订阅的连接状态流。
     */
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    /**
     * 暴露给 UI 订阅的实时对局指令数据流。
     */
    private val _messageFlow = MutableSharedFlow<GameCommand>(extraBufferCapacity = 64)
    val messageFlow: SharedFlow<GameCommand> = _messageFlow

    // 退避重连延迟计算器
    private val backoffCalculator = BackoffCalculator(1000L, 8000L)

    /**
     * WebSocket 连接状态描述。
     */
    sealed class ConnectionState {
        /** 正在尝试连接 */
        object Connecting : ConnectionState()
        /** 已建立连接 */
        object Connected : ConnectionState()
        /** 掉线并正在尝试重连 */
        data class Reconnecting(val attempt: Int) : ConnectionState()
        /** 已断开连接 */
        object Disconnected : ConnectionState()
    }

    /**
     * 发起 WebSocket 对局连接。
     * @param gameId 游戏对局 ID
     * @param playerId 玩家个人 ID
     */
    fun connect(gameId: String, playerId: String) {
        isManualClose = false
        reconnectCount = 0
        reconnectJob?.cancel()
        currentGameId = gameId
        currentPlayerId = playerId
        
        val url = "${AppConfig.WS_URL}?gameId=$gameId&playerId=$playerId"
        val request = Request.Builder()
            .url(url)
            .build()
        
        _connectionState.value = ConnectionState.Connecting
        LogUtils.i("Connecting to WebSocket: $url")
        webSocket = okHttpClient.newWebSocket(request, createListener())
    }

    /**
     * 发送同步对局指令。
     * @param command 要发送的对局指令对象
     * @return 发送是否成功
     */
    fun sendCommand(command: GameCommand): Boolean {
        val ws = webSocket
        if (ws == null) {
            LogUtils.w("WS send failed: webSocket is null")
            return false
        }
        return try {
            val jsonString = json.encodeToString(GameCommand.serializer(), command)
            ws.send(jsonString)
        } catch (e: Exception) {
            LogUtils.e("WS Send JSON Error", e)
            false
        }
    }

    /**
     * 主动断开连接。
     * 正常退出房间或注销登录时应当调用，避免自动重连。
     */
    fun disconnect() {
        isManualClose = true
        reconnectJob?.cancel()
        webSocket?.close(1000, "User explicitly disconnected")
        webSocket = null
        _connectionState.value = ConnectionState.Disconnected
        currentGameId = null
        currentPlayerId = null
        LogUtils.i("WebSocket disconnected manually")
    }

    private fun createListener() = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            LogUtils.i("WebSocket connected successfully")
            _connectionState.value = ConnectionState.Connected
            reconnectCount = 0
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            scope.launch {
                try {
                    val command = json.decodeFromString(GameCommand.serializer(), text)
                    _messageFlow.emit(command)
                } catch (e: Exception) {
                    LogUtils.e("WebSocket message decode failed: $text", e)
                }
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            LogUtils.w("WebSocket closing: $code / $reason")
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            LogUtils.i("WebSocket closed: $code / $reason")
            if (!isManualClose) {
                triggerReconnect()
            } else {
                _connectionState.value = ConnectionState.Disconnected
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            LogUtils.e("WebSocket connection failure", t)
            if (!isManualClose) {
                triggerReconnect()
            } else {
                _connectionState.value = ConnectionState.Disconnected
            }
        }
    }

    /**
     * 自动触发指数退避重连流程。
     */
    private fun triggerReconnect() {
        if (reconnectJob?.isActive == true) return
        val gameId = currentGameId
        val playerId = currentPlayerId
        if (gameId == null || playerId == null) {
            LogUtils.w("Cannot reconnect: missing gameId or playerId context")
            return
        }
        
        _connectionState.value = ConnectionState.Reconnecting(reconnectCount + 1)
        reconnectJob = scope.launch {
            val delayMs = backoffCalculator.getDelay(reconnectCount)
            LogUtils.w("WebSocket reconnecting in $delayMs ms (attempt ${reconnectCount + 1})")
            delay(delayMs)
            reconnectCount++
            
            val url = "${AppConfig.WS_URL}?gameId=$gameId&playerId=$playerId"
            val request = Request.Builder()
                .url(url)
                .build()
            webSocket = okHttpClient.newWebSocket(request, createListener())
        }
    }

    /**
     * 指数退避算法计算器。
     * 用于生成随尝试次数增加而指数级增长的重连延迟。
     */
    class BackoffCalculator(private val initialDelayMs: Long, private val maxDelayMs: Long) {
        /**
         * 获取当前重连尝试对应的延迟时间。
         * @param attempt 当前尝试次数（从 0 开始）
         * @return 应当延迟的毫秒数
         */
        fun getDelay(attempt: Int): Long {
            // 避免整型溢出，安全进行位移计算
            val factor = if (attempt >= 30) (1 shl 30) else (1 shl attempt)
            val delay = initialDelayMs * factor
            return if (delay > maxDelayMs || delay <= 0) maxDelayMs else delay
        }
    }
}
