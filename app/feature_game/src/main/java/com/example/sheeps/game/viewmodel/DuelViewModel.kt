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

    private fun handleInit(gameId: String, playerId: String, levelId: Int, seed: Int) {
        currentLevelId = levelId
        currentGameSeed = seed
        updateState { copy(gameId = gameId, playerId = playerId, isLoading = true, totalTileCount = 0, opponentEliminatedCount = 0) }
        wsManager.connect(gameId, playerId)
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tiles = calculateBlockedStates(apiService.getLevel(levelId, seed).map { it.copy(state = TileState.NORMAL) })
                updateState { copy(isLoading = false, boardTiles = tiles, gameStatus = GameStatus.PLAYING, totalTileCount = tiles.size) }
            } catch (e: Exception) {
                val tiles = calculateBlockedStates(levelGenerator.generateDuelLevel())
                updateState { copy(isLoading = false, boardTiles = tiles, gameStatus = GameStatus.PLAYING, totalTileCount = tiles.size) }
            }
        }
    }

    private suspend fun processSlotMatch(board: List<Tile>, slot: List<Tile>, movedOut: List<Tile>) {
        actionDelegate.processSlotMatch(currentState, board, slot, movedOut, ::updateState, ::setEffect,
            onMatchSuccess = { eliminatedIds ->
                sendEliminateCommand(eliminatedIds)
                if (currentState.combo > 0 && currentState.combo % 3 == 0) sendAttackCommand()
            },
            onVictory = { sendSystemEvent("WIN") }
        )
    }

    // --- WebSocket 指令发送 ---

    private fun sendEliminateCommand(ids: List<String>) {
        wsManager.sendCommand(GameCommand(currentState.gameId, ++seqId, System.currentTimeMillis(), currentState.playerId, CommandType.ELIMINATE, CommandPayload(tilesEliminated = ids, comboCount = currentState.combo)))
    }

    private fun sendAttackCommand() {
        wsManager.sendCommand(GameCommand(currentState.gameId, ++seqId, System.currentTimeMillis(), currentState.playerId, CommandType.ATTACK, CommandPayload(obstacleType = "SEALED")))
        setEffect(DuelViewEffect.ShowToast(getLocalizedString("发动攻击！", "Attacking!", "發動攻擊！", "攻撃！", "공격!")))
    }

    private fun sendCastSpellCommand(spellType: String) {
        wsManager.sendCommand(GameCommand(currentState.gameId, ++seqId, System.currentTimeMillis(), currentState.playerId, CommandType.CAST_SPELL, CommandPayload(spellType = spellType)))
    }

    private fun sendSystemEvent(msg: String) {
        wsManager.sendCommand(GameCommand(currentState.gameId, ++seqId, System.currentTimeMillis(), currentState.playerId, CommandType.SYSTEM_EVENT, CommandPayload(systemMessage = msg)))
    }

    private fun handleRestart() = handleInit(currentState.gameId, currentState.playerId, currentLevelId, currentGameSeed)

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
