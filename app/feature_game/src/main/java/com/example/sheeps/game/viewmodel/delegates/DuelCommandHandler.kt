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
 *
 * 说明：本委派类运行于 ViewModel 层且不直接持有 Context，因此用户可见文案统一通过
 * getLocalizedString(zh, en, tw, ja, ko) 运行时多语言助手进行国际化（与文件内
 * handleOpponentTimeout 的既有用法保持一致），而非 Android 字符串资源 R.string.*。
 * 这样既无需为委派类注入 Context，也能在任意语言环境下正确展示与清理提示文案。
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
                applyAttackEffect(scope, state, updateState, getLocalizedString)
            }
            CommandType.CAST_SPELL -> {
                applySpellEffect(
                    scope,
                    command.payload.spellType ?: "",
                    state,
                    updateState,
                    sendSystemEvent,
                    getLocalizedString
                )
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
        updateState: (DuelViewState.() -> DuelViewState) -> Unit,
        getLocalizedString: (String, String, String, String, String) -> String
    ) {
        val normalTiles = state.boardTiles.filter { it.state == TileState.NORMAL }
        if (normalTiles.isNotEmpty()) {
            val targets = normalTiles.shuffled().take(2)
            val newBoard = state.boardTiles.map { tile ->
                if (targets.any { it.id == tile.id }) {
                    tile.copy(sealedCount = tile.sealedCount + 1)
                } else tile
            }
            val sealedMessage = getLocalizedString(
                "受到对手干扰：两张卡牌被封印！",
                "Under opponent's interference: two cards sealed!",
                "受到對手干擾：兩張卡牌被封印！",
                "相手の妨害を受けました：2枚のカードが封印されました！",
                "상대방의 방해: 카드 2장이 봉인되었습니다!"
            )
            updateState { 
                copy(
                    boardTiles = calculateBlockedStates(newBoard),
                    incomingAttackMessage = sealedMessage
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
        sendSystemEvent: (String) -> Unit,
        getLocalizedString: (String, String, String, String, String) -> String
    ) {
        if (state.gameStatus != GameStatus.PLAYING) return

        when (spellType) {
            "FOG" -> {
                updateState { copy(isFogActive = true) }
                startSpellCountdown(scope, 1, "FOG", updateState, getLocalizedString) {
                    updateState { copy(isFogActive = false) }
                }
            }
            "SHRINK" -> {
                updateState { copy(maxSlotSize = 6) }
                if (state.slotTiles.size >= 6) {
                    sendSystemEvent("LOST")
                    updateState { copy(gameStatus = GameStatus.LOST) }
                }
                startSpellCountdown(scope, 1, "SHRINK", updateState, getLocalizedString) {
                    updateState { copy(maxSlotSize = 7) }
                }
            }
            "SEAL_ALL" -> {
                val normalTiles = state.boardTiles.filter { it.state == TileState.NORMAL }
                if (normalTiles.isNotEmpty()) {
                    val newBoard = state.boardTiles.map { tile ->
                        if (tile.state == TileState.NORMAL) tile.copy(sealedCount = tile.sealedCount + 1) else tile
                    }
                    val sealMessage = getLocalizedString(
                        "对手施放了【万重封印】，暴露牌已被封印！",
                        "Opponent cast [Endless Seal]: exposed tiles are sealed!",
                        "對手施放了【萬重封印】，暴露牌已被封印！",
                        "相手が【万重封印】を唱えました。暴露した牌が封印されました！",
                        "상대방이 [만중봉인]을 사용했습니다. 노출된 패가 봉인되었습니다!"
                    )
                    updateState {
                        copy(
                            boardTiles = calculateBlockedStates(newBoard),
                            activeSpellMessage = sealMessage
                        )
                    }
                    scope.launch {
                        delay(1000)
                        updateState {
                            copy(activeSpellMessage = if (activeSpellMessage == sealMessage) null else activeSpellMessage)
                        }
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
                val shuffleMessage = getLocalizedString(
                    "对手施放了【乾坤大挪移】，你的牌面类型被打乱了！",
                    "Opponent cast [Cosmic Shift]: your tile types are shuffled!",
                    "對手施放了【乾坤大挪移】，你的牌面類型被打亂了！",
                    "相手が【乾坤大挪移】を唱えました。あなたの牌の種類がシャッフルされました！",
                    "상대방이 [건곤대나이]를 사용했습니다. 당신의 패 종류가 섞였습니다!"
                )
                updateState {
                    copy(
                        boardTiles = calculateBlockedStates(newBoard),
                        activeSpellMessage = shuffleMessage
                    )
                }
                scope.launch {
                    delay(1000)
                    updateState {
                        copy(activeSpellMessage = if (activeSpellMessage == shuffleMessage) null else activeSpellMessage)
                    }
                }
            }
            "SILENCE" -> {
                updateState { copy(isSilenced = true) }
                startSpellCountdown(scope, 1, "SILENCE", updateState, getLocalizedString) {
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
        getLocalizedString: (String, String, String, String, String) -> String,
        onFinished: () -> Unit
    ) {
        val fogMessage = getLocalizedString(
            "对手施放了【迷雾障眼】，迷雾消散倒计时：%1\$d秒",
            "Opponent cast [Fog Veil]: fog clears in %1\$d sec",
            "對手施放了【迷霧障眼】，迷霧消散倒計時：%1\$d秒",
            "相手が【迷霧障眼】を唱えました。霧が晴れるまで：%1\$d秒",
            "상대방이 [미안장안]을 사용했습니다. 안개 해제까지: %1\$d초"
        )
        val shrinkMessage = getLocalizedString(
            "对手施放了【画地为牢】，卡槽解锁倒计时：%1\$d秒",
            "Opponent cast [Circle Prison]: slot unlocks in %1\$d sec",
            "對手施放了【畫地為牢】，卡槽解鎖倒計時：%1\$d秒",
            "相手が【画地為牢】を唱えました。スロット解放まで：%1\$d秒",
            "상대방이 [화지위뢰]를 사용했습니다. 슬롯 해제까지: %1\$d초"
        )
        val silenceMessage = getLocalizedString(
            "对手施放了【禁言封印】，技能解封倒计时：%1\$d秒",
            "Opponent cast [Silence Seal]: skill unlocks in %1\$d sec",
            "對手施放了【禁言封印】，技能解封倒計時：%1\$d秒",
            "相手が【禁言封印】を唱えました。スキル解放まで：%1\$d秒",
            "상대방이 [금언봉인]을 사용했습니다. 스킬 해제까지: %1\$d초"
        )
        val curseMessage = getLocalizedString(
            "诅咒解除倒计时：%1\$d秒",
            "Curse lifts in %1\$d sec",
            "詛咒解除倒計時：%1\$d秒",
            "呪いが解けるまで：%1\$d秒",
            "저주 해제까지: %1\$d초"
        )
        // 倒数结束时的清理判断：原实现依赖中文子串 contains("倒计时")，在切换语言后
        // 会失效。改为与最后一次实际展示的本地化文案做值相等判断，任意语言下均正确。
        val finalCountdownMessage = when (spellType) {
            "FOG" -> String.format(fogMessage, 1)
            "SHRINK" -> String.format(shrinkMessage, 1)
            "SILENCE" -> String.format(silenceMessage, 1)
            else -> String.format(curseMessage, 1)
        }
        countdownJob?.cancel()
        countdownJob = scope.launch {
            var remaining = seconds
            while (remaining > 0) {
                val msg = when (spellType) {
                    "FOG" -> String.format(fogMessage, remaining)
                    "SHRINK" -> String.format(shrinkMessage, remaining)
                    "SILENCE" -> String.format(silenceMessage, remaining)
                    else -> String.format(curseMessage, remaining)
                }
                updateState { copy(spellCountdownSeconds = remaining, activeSpellMessage = msg) }
                delay(1000)
                remaining--
            }
            updateState {
                copy(
                    spellCountdownSeconds = 0,
                    activeSpellMessage = if (activeSpellMessage == finalCountdownMessage) null else activeSpellMessage
                )
            }
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
            "OPPONENT_QUIT" -> updateState {
                copy(
                    gameStatus = GameStatus.WON,
                    winnerId = state.playerId,
                    activeSpellMessage = getLocalizedString(
                        "对手已认输并退出房间！",
                        "Opponent surrendered and left the room!",
                        "對手已認輸並退出房間！",
                        "相手が降伏して退出しました！",
                        "상대방이 항복하고 방을 나갔습니다!"
                    )
                )
            }
            "PLAYER_DISCONNECTED" -> if (command.payload.targetPlayerId != state.playerId) updateState {
                copy(
                    incomingAttackMessage = getLocalizedString(
                        "对手断开连接，等待重连...",
                        "Opponent disconnected, waiting for reconnection...",
                        "對手斷開連接，等待重連...",
                        "相手の接続が切れました。再接続を待っています...",
                        "상대방 연결 끊김, 재연결 대기 중..."
                    )
                )
            }
            "PLAYER_RECONNECTED" -> if (command.payload.targetPlayerId != state.playerId) {
                handleOpponentReconnected(updateState, getLocalizedString)
            }
            "GAME_OVER_DISCONNECT_WIN" -> handleOpponentTimeout(state, updateState, setEffect, getLocalizedString)
        }
    }

    private fun handleOpponentReconnected(
        updateState: (DuelViewState.() -> DuelViewState) -> Unit,
        getLocalizedString: (String, String, String, String, String) -> String
    ) {
        val reconnectedMessage = getLocalizedString(
            "对手已重新连接！",
            "Opponent reconnected!",
            "對手已重新連接！",
            "相手が再接続しました！",
            "상대방이 다시 연결되었습니다!"
        )
        updateState { copy(incomingAttackMessage = reconnectedMessage) }
        // 自动隐藏消息逻辑通常由 ViewModel 处理，这里仅更新状态
    }

    private fun handleOpponentTimeout(
        state: DuelViewState,
        updateState: (DuelViewState.() -> DuelViewState) -> Unit,
        setEffect: (DuelViewEffect) -> Unit,
        getLocalizedString: (String, String, String, String, String) -> String
    ) {
        updateState { copy(gameStatus = GameStatus.WON, winnerId = state.playerId, incomingAttackMessage = null) }
        setEffect(
            DuelViewEffect.ShowToast(
                getLocalizedString(
                    "对手超时未重连，你赢了！",
                    "Opponent timed out, you win!",
                    "對手超時未重連，你贏了！",
                    "相手がタイムアウトしました。あなたの勝ちです！",
                    "상대방이 시간 초과되었습니다. 당신이 이겼습니다!"
                )
            )
        )
    }
}
