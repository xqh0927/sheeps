package com.example.sheeps.game.viewmodel.delegates

import com.example.sheeps.core.game.GameEngine.calculateBlockedStates
import com.example.sheeps.core.multiplayer.model.CommandType
import com.example.sheeps.core.multiplayer.model.GameCommand
import com.example.sheeps.data.model.TileState
import com.example.sheeps.game.state.DuelViewEffect
import com.example.sheeps.game.state.DuelViewState
import com.example.sheeps.game.state.GameStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 对决模式远程指令处理委派类
 * 负责接收并应用来自对手或服务器的指令（攻击、施法、系统事件）
 */
class DuelCommandHandler @Inject constructor() {

    private var countdownJob: Job? = null

    /**
     * 处理远程指令
     */
    fun handleRemoteCommand(
        scope: CoroutineScope,
        command: GameCommand,
        state: DuelViewState,
        updateState: (DuelViewState.() -> DuelViewState) -> Unit,
        setEffect: (DuelViewEffect) -> Unit,
        getLocalizedString: (String, String, String, String, String) -> String,
        sendSystemEvent: (String) -> Unit
    ) {
        if (command.senderId == state.playerId) return
        
        when (command.type) {
            CommandType.ELIMINATE -> {
                handleOpponentEliminate(command, state, updateState)
            }
            CommandType.ATTACK -> {
                applyAttackEffect(scope, state, updateState)
            }
            CommandType.CAST_SPELL -> {
                applySpellEffect(scope, command.payload.spellType ?: "", state, updateState, sendSystemEvent)
            }
            CommandType.SYSTEM_EVENT -> {
                handleSystemEvent(command, state, updateState, setEffect, getLocalizedString)
            }
            else -> {}
        }
    }

    private fun handleOpponentEliminate(
        command: GameCommand,
        state: DuelViewState,
        updateState: (DuelViewState.() -> DuelViewState) -> Unit
    ) {
        val eliminatedTilesCount = command.payload.tilesEliminated?.size ?: 3
        updateState { 
            val newOpponentEliminatedCount = opponentEliminatedCount + eliminatedTilesCount
            val newOpponentProgress = if (totalTileCount > 0) {
                (newOpponentEliminatedCount.toFloat() / totalTileCount.toFloat()).coerceIn(0f, 1f)
            } else opponentProgress
            copy(
                opponentScore = opponentScore + 100,
                opponentEliminatedCount = newOpponentEliminatedCount,
                opponentProgress = newOpponentProgress
            )
        }
    }

    private fun applyAttackEffect(
        scope: CoroutineScope,
        state: DuelViewState,
        updateState: (DuelViewState.() -> DuelViewState) -> Unit
    ) {
        val normalTiles = state.boardTiles.filter { it.state == TileState.NORMAL }
        if (normalTiles.isNotEmpty()) {
            val targets = normalTiles.shuffled().take(2)
            val newBoard = state.boardTiles.map { tile ->
                if (targets.any { it.id == tile.id }) {
                    tile.copy(sealedCount = tile.sealedCount + 1)
                } else tile
            }
            updateState { 
                copy(
                    boardTiles = calculateBlockedStates(newBoard),
                    incomingAttackMessage = "受到对手干扰：两张卡牌被封印！"
                )
            }
            scope.launch {
                delay(3000)
                updateState { copy(incomingAttackMessage = null) }
            }
        }
    }

    private fun applySpellEffect(
        scope: CoroutineScope,
        spellType: String,
        state: DuelViewState,
        updateState: (DuelViewState.() -> DuelViewState) -> Unit,
        sendSystemEvent: (String) -> Unit
    ) {
        if (state.gameStatus != GameStatus.PLAYING) return

        when (spellType) {
            "FOG" -> {
                updateState { copy(isFogActive = true) }
                startSpellCountdown(scope, 1, "FOG", updateState) {
                    updateState { copy(isFogActive = false) }
                }
            }
            "SHRINK" -> {
                updateState { copy(maxSlotSize = 6) }
                if (state.slotTiles.size >= 6) {
                    sendSystemEvent("LOST")
                    updateState { copy(gameStatus = GameStatus.LOST) }
                }
                startSpellCountdown(scope, 1, "SHRINK", updateState) {
                    updateState { copy(maxSlotSize = 7) }
                }
            }
            "SEAL_ALL" -> {
                val normalTiles = state.boardTiles.filter { it.state == TileState.NORMAL }
                if (normalTiles.isNotEmpty()) {
                    val newBoard = state.boardTiles.map { tile ->
                        if (tile.state == TileState.NORMAL) tile.copy(sealedCount = tile.sealedCount + 1) else tile
                    }
                    updateState {
                        copy(
                            boardTiles = calculateBlockedStates(newBoard),
                            activeSpellMessage = "对手施放了【万重封印】，暴露牌已被封印！"
                        )
                    }
                    scope.launch {
                        delay(1000)
                        updateState { copy(activeSpellMessage = if (activeSpellMessage?.contains("万重封印") == true) null else activeSpellMessage) }
                    }
                }
            }
            "SHUFFLE" -> {
                val boardNormalTiles = state.boardTiles.filter { it.state == TileState.NORMAL || it.state == TileState.BLOCKED }
                val types = boardNormalTiles.map { it.type }.shuffled()
                var typeIdx = 0
                val newBoard = state.boardTiles.map { tile ->
                    if (tile.state == TileState.NORMAL || tile.state == TileState.BLOCKED) {
                        tile.copy(type = types[typeIdx++])
                    } else tile
                }
                updateState {
                    copy(
                        boardTiles = calculateBlockedStates(newBoard),
                        activeSpellMessage = "对手施放了【乾坤大挪移】，你的牌面类型被打乱了！"
                    )
                }
                scope.launch {
                    delay(1000)
                    updateState { copy(activeSpellMessage = if (activeSpellMessage?.contains("乾坤大挪移") == true) null else activeSpellMessage) }
                }
            }
            "SILENCE" -> {
                updateState { copy(isSilenced = true) }
                startSpellCountdown(scope, 1, "SILENCE", updateState) {
                    updateState { copy(isSilenced = false) }
                }
            }
        }
    }

    private fun startSpellCountdown(
        scope: CoroutineScope,
        seconds: Int,
        spellType: String,
        updateState: (DuelViewState.() -> DuelViewState) -> Unit,
        onFinished: () -> Unit
    ) {
        countdownJob?.cancel()
        countdownJob = scope.launch {
            var remaining = seconds
            while (remaining > 0) {
                val msg = when (spellType) {
                    "FOG" -> "对手施放了【迷雾障眼】，迷雾消散倒计时：${remaining}秒"
                    "SHRINK" -> "对手施放了【画地为牢】，卡槽解锁倒计时：${remaining}秒"
                    "SILENCE" -> "对手施放了【禁言封印】，技能解封倒计时：${remaining}秒"
                    else -> "诅咒解除倒计时：${remaining}秒"
                }
                updateState { copy(spellCountdownSeconds = remaining, activeSpellMessage = msg) }
                delay(1000)
                remaining--
            }
            updateState { copy(spellCountdownSeconds = 0, activeSpellMessage = if (activeSpellMessage?.contains("倒计时") == true) null else activeSpellMessage) }
            onFinished()
        }
    }

    private fun handleSystemEvent(
        command: GameCommand,
        state: DuelViewState,
        updateState: (DuelViewState.() -> DuelViewState) -> Unit,
        setEffect: (DuelViewEffect) -> Unit,
        getLocalizedString: (String, String, String, String, String) -> String
    ) {
        when (command.payload.systemMessage) {
            "WIN" -> updateState { copy(gameStatus = GameStatus.LOST, winnerId = command.senderId) }
            "LOST" -> updateState { copy(gameStatus = GameStatus.WON, winnerId = state.playerId) }
            "OPPONENT_QUIT" -> updateState { copy(gameStatus = GameStatus.WON, winnerId = state.playerId, activeSpellMessage = "对手已认输并退出房间！") }
            "PLAYER_DISCONNECTED" -> if (command.payload.targetPlayerId != state.playerId) updateState { copy(incomingAttackMessage = "对手断开连接，等待重连...") }
            "PLAYER_RECONNECTED" -> if (command.payload.targetPlayerId != state.playerId) handleOpponentReconnected(updateState)
            "GAME_OVER_DISCONNECT_WIN" -> handleOpponentTimeout(state, updateState, setEffect, getLocalizedString)
        }
    }

    private fun handleOpponentReconnected(updateState: (DuelViewState.() -> DuelViewState) -> Unit) {
        updateState { copy(incomingAttackMessage = "对手已重新连接！") }
        // 自动隐藏消息逻辑通常由 ViewModel 处理，这里仅更新状态
    }

    private fun handleOpponentTimeout(
        state: DuelViewState,
        updateState: (DuelViewState.() -> DuelViewState) -> Unit,
        setEffect: (DuelViewEffect) -> Unit,
        getLocalizedString: (String, String, String, String, String) -> String
    ) {
        updateState { copy(gameStatus = GameStatus.WON, winnerId = state.playerId, incomingAttackMessage = null) }
        setEffect(DuelViewEffect.ShowToast(getLocalizedString(
            "对手超时未重连，你赢了！", "Opponent timed out, you win!", "對手超時未重連，你贏了！", "相手がタイムアウトしました。あなたの勝ちです！", "상대방이 시간 초과되었습니다. 당신이 이겼습니다!"
        )))
    }
}
