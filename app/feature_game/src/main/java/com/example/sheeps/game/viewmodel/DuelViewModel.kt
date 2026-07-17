package com.example.sheeps.game.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.sheeps.core.base.BaseMviViewModel
import com.example.sheeps.core.multiplayer.WebSocketManager
import com.example.sheeps.core.multiplayer.model.*
import com.example.sheeps.core.preference.UserPreferences
import com.example.sheeps.data.local.LocalDao
import com.example.sheeps.data.model.*
import com.example.sheeps.data.network.ApiService
import com.example.sheeps.game.state.*
import com.example.sheeps.game.viewmodel.delegates.DuelActionDelegate
import com.example.sheeps.game.viewmodel.delegates.DuelCommandHandler
import com.example.sheeps.game.viewmodel.helpers.DuelLevelGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject
import com.example.sheeps.core.game.GameEngine.calculateBlockedStates

/**
 * 对决模式 ViewModel
 * 处理 WebSocket 实时对战逻辑。核心业务分发至 ActionDelegate 和 CommandHandler 实现。
 *
 * 生命周期与资源释放：
 * - [init] 中在 [viewModelScope] 内 `collectLatest`/`collect` 监听 [WebSocketManager.connectionState]
 *   与 `messageFlow`，这些 Flow 收集随 VM 销毁（`viewModelScope` 取消）自动停止，无独立泄漏风险。
 * - ⚠️ 内存隐患：[WebSocketManager] 为单例/应用级连接管理，本类未覆写 `onCleared()`，
 *   WebSocket 仅在用户主动触发 [DuelViewIntent.Leave]（[handleLeave] 内 `wsManager.disconnect()`）时关闭。
 *   若用户直接返回而不走 Leave 流程，连接可能残留。安全建议：在 `onCleared()` 中补 `wsManager.disconnect()`
 *   作为兜底，确保 VM 销毁时必然断连。
 *
 * 信令协议对齐：本类通过 [sendEliminateCommand]/[sendAttackCommand]/[sendCastSpellCommand]/[sendSystemEvent]
 * 构造 [GameCommand] 下发，类型与 `CommandType`（ELIMINATE/ATTACK/CAST_SPELL/SYSTEM_EVENT）严格对应服务端对战信令，
 * 不改变既有协议字段。
 */
@HiltViewModel
class DuelViewModel @Inject constructor(
    private val apiService: ApiService,
    private val prefs: UserPreferences,
    private val wsManager: WebSocketManager,
    private val json: Json,
    private val localDao: LocalDao,
    // 逻辑委派
    private val levelGenerator: DuelLevelGenerator,
    private val actionDelegate: DuelActionDelegate,
    private val commandHandler: DuelCommandHandler
) : BaseMviViewModel<DuelViewState, DuelViewIntent, DuelViewEffect>(DuelViewState()) {

    private var seqId = 0L
    private var currentLevelId = 2
    private var currentGameSeed = 0

    init {
        // 观察连接状态
        viewModelScope.launch {
            wsManager.connectionState.collectLatest { state -> updateState { copy(connectionState = state) } }
        }
        // 观察远程指令
        viewModelScope.launch {
            wsManager.messageFlow.collect { command ->
                commandHandler.handleRemoteCommand(viewModelScope, command, currentState, ::updateState, ::setEffect, ::getLocalizedString, ::sendSystemEvent)
                // 自动清理重连成功消息的逻辑
                if (command.type == CommandType.SYSTEM_EVENT && command.payload.systemMessage == "PLAYER_RECONNECTED") {
                    viewModelScope.launch { delay(3000); if (currentState.incomingAttackMessage?.contains("重新连接") == true) updateState { copy(incomingAttackMessage = null) } }
                }
            }
        }
    }

    override fun handleIntent(intent: DuelViewIntent) {
        when (intent) {
            is DuelViewIntent.Init -> handleInit(intent.gameId, intent.playerId, intent.levelId, intent.seed)
            is DuelViewIntent.ClickTile -> actionDelegate.handleClickTile(viewModelScope, intent.tile, currentState, ::updateState, ::setEffect, ::processSlotMatch)
            is DuelViewIntent.Restart -> handleRestart()
            is DuelViewIntent.Leave -> handleLeave()
            is DuelViewIntent.CastSpell -> actionDelegate.handleCastSpell(currentState, intent.spellType, ::getLocalizedString, ::updateState, ::setEffect) {
                sendCastSpellCommand(intent.spellType)
            }
        }
    }

    /**
     * 初始化对局：写入房间/玩家信息，建立 WebSocket 连接并加载相同种子的关卡。
     *
     * 线程边界：
     * - `wsManager.connect` 在主线程调用，底层 WebSocket 收发在 SDK 自有线程；
     * - 关卡数据请求运行在 [Dispatchers.IO]，成功/失败均切回主线程更新 State。
     *
     * @param gameId 房间 ID
     * @param playerId 本地玩家 ID
     * @param levelId 关卡 ID
     * @param seed 关卡随机种子（双方一致以保证公平）
     */
    private fun handleInit(gameId: String, playerId: String, levelId: Int, seed: Int) {
        currentLevelId = levelId
        currentGameSeed = seed
        updateState { copy(gameId = gameId, playerId = playerId, isLoading = true, totalTileCount = 0, opponentEliminatedCount = 0, currentSkin = prefs.getCurrentSkin()) }
        // ⚠️ 内存隐患：建立 WebSocket 连接；若后续未通过 handleLeave → wsManager.disconnect() 关闭，
        // 连接会残留（见类文档 onCleared 兜底建议）。
        wsManager.connect(gameId, playerId)
        
        viewModelScope.launch(Dispatchers.IO) {
            // ===== 对决模式关卡数据加载逻辑（网络优先 + 离线兜底） =====
            try {
                // 1. 网络优先：向服务器 API 请求对决中双方一致的关卡布局
                val rawTiles = apiService.getLevel(levelId, seed).map { it.copy(state = TileState.NORMAL) }
                
                // 遵循一致性规则：直接采用接口返回的特殊牌（isBlind）分布，
                // 仅为了客户端“单次解锁”规则，在 sealedCount > 0 时通过浅拷贝净化重置其值为 1
                val sanitizedTiles = rawTiles.map { tile ->
                    val t = tile.copy()
                    if (t.sealedCount > 0) {
                        t.sealedCount = 1
                    }
                    t
                }
                val tiles = calculateBlockedStates(sanitizedTiles)
                val bounds = if (tiles.isEmpty()) {
                    BoardBounds()
                } else {
                    BoardBounds(
                        minX = tiles.minOf { it.x },
                        maxX = tiles.maxOf { it.x },
                        minY = tiles.minOf { it.y },
                        maxY = tiles.maxOf { it.y }
                    )
                }
                updateState { copy(isLoading = false, boardTiles = tiles, boardBounds = bounds, gameStatus = GameStatus.PLAYING, totalTileCount = tiles.size) }
            } catch (e: Exception) {
                // 2. 离线兜底：若对决模式发生 API 加载异常，回退至本地对决生成算法
                val rawTiles = levelGenerator.generateDuelLevel()
                val sanitizedTiles = rawTiles.map { tile ->
                    val t = tile.copy()
                    if (t.sealedCount > 0) {
                        t.sealedCount = 1
                    }
                    t
                }
                val tiles = calculateBlockedStates(sanitizedTiles)
                val bounds = if (tiles.isEmpty()) {
                    BoardBounds()
                } else {
                    BoardBounds(
                        minX = tiles.minOf { it.x },
                        maxX = tiles.maxOf { it.x },
                        minY = tiles.minOf { it.y },
                        maxY = tiles.maxOf { it.y }
                    )
                }
                updateState { copy(isLoading = false, boardTiles = tiles, boardBounds = bounds, gameStatus = GameStatus.PLAYING, totalTileCount = tiles.size) }
            }
        }
    }

    /**
     * 卡槽三消匹配（委托 [DuelActionDelegate.processSlotMatch]）。
     * 匹配成功后向服务端下发 ELIMINATE 指令并据连击触发 ATTACK；胜利时下发 SYSTEM_EVENT(WIN)。
     */
    private suspend fun processSlotMatch(board: List<Tile>, slot: List<Tile>, movedOut: List<Tile>) {
        actionDelegate.processSlotMatch(currentState, board, slot, movedOut, ::updateState, ::setEffect,
            onMatchSuccess = { eliminatedIds ->
                sendEliminateCommand(eliminatedIds)
                if (currentState.combo > 0 && currentState.combo % 3 == 0) sendAttackCommand()
            },
            onVictory = { sendSystemEvent("WIN") }
        )
    }

    // 指令发送：以下方法构造 GameCommand 下发，类型与 server 对战信令协议严格对齐，字段不可随意增删。
    // --- WebSocket 指令发送 ---

    /** 向服务端下发 ELIMINATE 指令：上报本次消除的卡牌 id 与当前连击数（seqId 自增保证消息有序）。 */
    private fun sendEliminateCommand(ids: List<String>) {
        wsManager.sendCommand(GameCommand(currentState.gameId, ++seqId, System.currentTimeMillis(), currentState.playerId, CommandType.ELIMINATE, CommandPayload(tilesEliminated = ids, comboCount = currentState.combo)))
    }

    /** 向服务端下发 ATTACK 指令：对对手施加封印干扰（SEALED）。连击每满 3 次由 [processSlotMatch] 触发。 */
    private fun sendAttackCommand() {
        wsManager.sendCommand(GameCommand(currentState.gameId, ++seqId, System.currentTimeMillis(), currentState.playerId, CommandType.ATTACK, CommandPayload(obstacleType = "SEALED")))
        setEffect(DuelViewEffect.ShowToast(getLocalizedString("发动攻击！", "Attacking!", "發動攻擊！", "攻撃！", "공격!")))
    }

    /** 向服务端下发 CAST_SPELL 指令：广播本玩家的恶搞/诅咒大招类型。 */
    private fun sendCastSpellCommand(spellType: String) {
        wsManager.sendCommand(GameCommand(currentState.gameId, ++seqId, System.currentTimeMillis(), currentState.playerId, CommandType.CAST_SPELL, CommandPayload(spellType = spellType)))
    }

    /** 向服务端下发 SYSTEM_EVENT 指令（如 WIN / OPPONENT_QUIT 等状态同步）。 */
    private fun sendSystemEvent(msg: String) {
        wsManager.sendCommand(GameCommand(currentState.gameId, ++seqId, System.currentTimeMillis(), currentState.playerId, CommandType.SYSTEM_EVENT, CommandPayload(systemMessage = msg)))
    }

    /** 重开对局：复用当前房间与种子重新 [handleInit]。 */
    private fun handleRestart() = handleInit(currentState.gameId, currentState.playerId, currentLevelId, currentGameSeed)

    /**
     * 离开房间：先下发 OPPONENT_QUIT 系统事件，短暂延迟后 [WebSocketManager.disconnect] 断开连接并退出界面。
     * 这是当前唯一的 WebSocket 主动关闭点（见类文档内存隐患说明）。
     */
    private fun handleLeave() {
        viewModelScope.launch {
            sendSystemEvent("OPPONENT_QUIT")
            delay(500)
            wsManager.disconnect()
            setEffect(DuelViewEffect.ExitGame)
        }
    }

    private fun getLocalizedString(zh: String, en: String, tw: String, ja: String, ko: String): String {
        return when (prefs.getLanguage()) { "en" -> en; "tw" -> tw; "ja" -> ja; "ko" -> ko; else -> zh }
    }
}
