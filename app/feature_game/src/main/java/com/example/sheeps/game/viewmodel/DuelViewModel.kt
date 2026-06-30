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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import com.example.sheeps.core.game.GameEngine.calculateBlockedStates
import com.example.sheeps.core.game.GameEngine.isTileBlocked

@HiltViewModel
class DuelViewModel @Inject constructor(
    private val apiService: ApiService,
    private val prefs: UserPreferences,
    private val wsManager: WebSocketManager,
    private val json: Json,
    private val localDao: LocalDao
) : BaseMviViewModel<DuelViewState, DuelViewIntent, DuelViewEffect>(DuelViewState()) {

    private var initialTileCount = 0
    private var seqId = 0L
    private var currentLevelId = 2
    private var currentGameSeed = 0
    private var countdownJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            wsManager.connectionState.collectLatest { state ->
                updateState { copy(connectionState = state) }
            }
        }

        viewModelScope.launch {
            wsManager.messageFlow.collect { command ->
                handleRemoteCommand(command)
            }
        }
    }

    override fun handleIntent(intent: DuelViewIntent) {
        when (intent) {
            is DuelViewIntent.Init -> handleInit(intent.gameId, intent.playerId, intent.levelId, intent.seed)
            is DuelViewIntent.ClickTile -> handleClickTile(intent.tile)
            is DuelViewIntent.Restart -> handleRestart()
            is DuelViewIntent.Leave -> handleLeave()
            is DuelViewIntent.CastSpell -> handleCastSpell(intent.spellType)
        }
    }

    private fun handleInit(gameId: String, playerId: String, levelId: Int, seed: Int) {
        currentLevelId = levelId
        currentGameSeed = seed
        updateState { copy(gameId = gameId, playerId = playerId, isLoading = true, totalTileCount = 0, opponentEliminatedCount = 0) }
        wsManager.connect(gameId, playerId)
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val serverTiles = apiService.getLevel(levelId, seed)
                val processedTiles = serverTiles.map { it.copy(state = TileState.NORMAL) }
                val finalTiles = calculateBlockedStates(processedTiles)
                initialTileCount = finalTiles.size

                updateState {
                    copy(
                        isLoading = false,
                        boardTiles = finalTiles,
                        gameStatus = GameStatus.PLAYING,
                        totalTileCount = finalTiles.size
                    )
                }
            } catch (e: Exception) {
                // Fallback to local generation if offline
                val localTiles = generateDuelLevel()
                val finalTiles = calculateBlockedStates(localTiles)
                initialTileCount = finalTiles.size
                updateState {
                    copy(
                        isLoading = false,
                        boardTiles = finalTiles,
                        gameStatus = GameStatus.PLAYING,
                        totalTileCount = finalTiles.size
                    )
                }
            }
        }
    }

    private fun handleClickTile(tile: Tile) {
        val state = currentState
        if (state.gameStatus != GameStatus.PLAYING) return
        val isBlocked = tile.state == TileState.BLOCKED || (tile.state == TileState.NORMAL && isTileBlocked(tile, state.boardTiles))
        if (isBlocked) {
            val blockers = com.example.sheeps.core.game.GameEngine.getBlockingTiles(tile, state.boardTiles)
            val blockerIds = blockers.map { it.id }.toSet()
            if (blockerIds.isNotEmpty()) {
                updateState { copy(shakingTileIds = shakingTileIds + blockerIds) }
                viewModelScope.launch {
                    delay(500)
                    updateState { copy(shakingTileIds = shakingTileIds - blockerIds) }
                }
            }
            return
        }

        if (tile.state != TileState.NORMAL && tile.state != TileState.MOVED_OUT) return

        viewModelScope.launch {
            if (tile.state == TileState.MOVED_OUT) {
                val updatedBoard = state.boardTiles
                val updatedMovedOut = state.movedOutTiles.filter { it.id != tile.id }
                val newSlot = state.slotTiles + tile.copy(state = TileState.IN_SLOT)

                setEffect(DuelViewEffect.PlaySound(SoundType.CLICK))
                processSlotMatch(updatedBoard, newSlot, updatedMovedOut)
            } else {
                if (tile.sealedCount > 0) {
                    tile.sealedCount--
                    setEffect(DuelViewEffect.PlaySound(SoundType.UNSEAL))
                    setEffect(DuelViewEffect.Vibrate)
                    updateState {
                        copy(boardTiles = calculateBlockedStates(state.boardTiles))
                    }
                } else {
                    tile.state = TileState.IN_SLOT
                    val updatedBoard = state.boardTiles
                    val newSlot = state.slotTiles + tile

                    setEffect(DuelViewEffect.PlaySound(SoundType.CLICK))
                    processSlotMatch(updatedBoard, newSlot, state.movedOutTiles)
                }
            }
        }
    }

    private suspend fun processSlotMatch(
        board: List<Tile>,
        slot: List<Tile>,
        movedOut: List<Tile>
    ) {
        val finalBoard = calculateBlockedStates(board)
        val finalSlot = slot.toMutableList()
        finalSlot.sortBy { it.type }

        val counts = finalSlot.groupBy { it.type }
        var matched = false
        var eliminatedIds = emptyList<String>()
        
        for ((type, items) in counts) {
            if (items.size >= 3) {
                eliminatedIds = items.take(3).map { it.id }
                var removedCount = 0
                finalSlot.removeAll {
                    if (it.type == type && removedCount < 3) {
                        removedCount++
                        true
                    } else false
                }
                updateState { 
                    val newEnergy = (currentEnergy + 1).coerceAtMost(10)
                    copy(score = score + 100, combo = combo + 1, currentEnergy = newEnergy) 
                }
                matched = true
                break
            }
        }

        if (matched) {
            setEffect(DuelViewEffect.PlaySound(SoundType.MATCH))
            sendEliminateCommand(eliminatedIds)
            
            // Attack logic: every 3 combo sends an attack
            if (currentState.combo > 0 && currentState.combo % 3 == 0) {
                sendAttackCommand()
            }
        } else if (slot.size > currentState.slotTiles.size) {
            // Added a tile but no match -> reset combo
            updateState { copy(combo = 0) }
        }

        val remainingTotal = finalBoard.count { it.state == TileState.NORMAL || it.state == TileState.BLOCKED } + movedOut.size
        
        if (remainingTotal == 0) {
            sendSystemEvent("WIN")
            updateState { copy(gameStatus = GameStatus.WON, winnerId = playerId) }
        } else if (finalSlot.size >= currentState.maxSlotSize) {
            sendSystemEvent("LOST")
            updateState { copy(gameStatus = GameStatus.LOST) }
        }

        updateState {
            copy(
                boardTiles = finalBoard,
                slotTiles = finalSlot,
                movedOutTiles = movedOut
            )
        }
    }

    private fun handleRemoteCommand(command: GameCommand) {
        if (command.senderId == currentState.playerId) return
        
        when (command.type) {
            CommandType.ELIMINATE -> {
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
            CommandType.ATTACK -> {
                applyAttackEffect()
            }
            CommandType.CAST_SPELL -> {
                applySpellEffect(command.payload.spellType ?: "")
            }
            CommandType.SYSTEM_EVENT -> {
                when (command.payload.systemMessage) {
                    "WIN" -> {
                        updateState { 
                            copy(
                                gameStatus = GameStatus.LOST,
                                winnerId = command.senderId
                            )
                        }
                    }
                    "LOST" -> {
                        updateState {
                            copy(
                                gameStatus = GameStatus.WON,
                                winnerId = playerId
                            )
                        }
                    }
                    "OPPONENT_QUIT" -> {
                        updateState {
                            copy(
                                gameStatus = GameStatus.WON,
                                winnerId = playerId,
                                activeSpellMessage = "对手已认输并退出房间！"
                            )
                        }
                    }
                    "PLAYER_DISCONNECTED" -> {
                        if (command.payload.targetPlayerId != currentState.playerId) {
                            updateState { 
                                copy(incomingAttackMessage = "对手断开连接，等待重连...")
                            }
                        }
                    }
                    "PLAYER_RECONNECTED" -> {
                        if (command.payload.targetPlayerId != currentState.playerId) {
                            updateState { 
                                copy(incomingAttackMessage = "对手已重新连接！")
                            }
                            viewModelScope.launch {
                                delay(3000)
                                if (currentState.incomingAttackMessage == "对手已重新连接！") {
                                    updateState { copy(incomingAttackMessage = null) }
                                }
                            }
                        }
                    }
                    "GAME_OVER_DISCONNECT_WIN" -> {
                        if (command.payload.targetPlayerId != currentState.playerId) {
                            updateState {
                                copy(
                                    gameStatus = GameStatus.WON,
                                    winnerId = playerId,
                                    incomingAttackMessage = null
                                )
                            }
                            setEffect(DuelViewEffect.ShowToast(getLocalizedString(
                                "对手超时未重连，你赢了！", 
                                "Opponent timed out, you win!", 
                                "對手超時未重連，你贏了！", 
                                "相手がタイムアウトしました。あなたの勝ちです！", 
                                "상대방이 시간 초과되었습니다. 당신이 이겼습니다!"
                            )))
                        }
                    }
                }
            }
            else -> {}
        }
    }

    private fun applyAttackEffect() {
        val state = currentState
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
            viewModelScope.launch {
                delay(3000)
                updateState { copy(incomingAttackMessage = null) }
            }
        }
    }

    private fun startSpellCountdown(seconds: Int, spellType: String, onFinished: () -> Unit) {
        countdownJob?.cancel()
        updateState { copy(spellCountdownSeconds = seconds) }
        
        countdownJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                val msg = when (spellType) {
                    "FOG" -> "对手施放了【迷雾障眼】，迷雾消散倒计时：${remaining}秒"
                    "SHRINK" -> "对手施放了【画地为牢】，卡槽解锁倒计时：${remaining}秒"
                    "SILENCE" -> "对手施放了【禁言封印】，技能解封倒计时：${remaining}秒"
                    else -> "诅咒解除倒计时：${remaining}秒"
                }
                updateState { 
                    copy(
                        spellCountdownSeconds = remaining,
                        activeSpellMessage = msg
                    )
                }
                delay(1000)
                remaining--
            }
            updateState { 
                copy(
                    spellCountdownSeconds = 0,
                    activeSpellMessage = if (activeSpellMessage?.contains("倒计时") == true) null else activeSpellMessage
                )
            }
            onFinished()
        }
    }

    private fun handleCastSpell(spellType: String) {
        val state = currentState
        if (state.gameStatus != GameStatus.PLAYING) return
        
        if (state.usedSpells.contains(spellType)) {
            setEffect(DuelViewEffect.ShowToast(getLocalizedString(
                "该大招已使用过，本局无法重复使用！",
                "Spell already used, cannot reuse this match!",
                "該大招已使用過，本局無法重複使用！",
                "このスキルは既に使用されています。この対局では再使用できません！",
                "이 주문은 이미 사용되었습니다. 이번 판에서는 재사용할 수 없습니다!"
            )))
            return
        }
        
        if (state.isSilenced) {
            setEffect(DuelViewEffect.ShowToast(getLocalizedString(
                "你正处于禁魔状态，无法使用大招！",
                "You are silenced, cannot cast spells!",
                "你正處於禁魔狀態，無法使用大招！",
                "現在サイレンス状態です。スキルを使用できません！",
                "침묵 상태이므로 주문을 사용할 수 없습니다!"
            )))
            return
        }

        val cost = when (spellType) {
            "FOG" -> 3
            "SHRINK" -> 6
            "SEAL_ALL" -> 10
            "SHUFFLE" -> 5
            "SILENCE" -> 4
            else -> 999
        }
        if (state.currentEnergy < cost) {
            setEffect(DuelViewEffect.ShowToast(getLocalizedString(
                "能量不足以施法！",
                "Insufficient energy to cast spell!",
                "能量不足以施法！",
                "エネルギーが不足しています！",
                "주문을 시전할 게이지가 부족합니다!"
            )))
            return
        }

        // Deduct energy and lock spell
        updateState { copy(currentEnergy = currentEnergy - cost, usedSpells = usedSpells + spellType) }

        // Send spell cast command to opponent
        wsManager.sendCommand(GameCommand(
            gameId = currentState.gameId,
            seqId = ++seqId,
            timestamp = System.currentTimeMillis(),
            senderId = currentState.playerId,
            type = CommandType.CAST_SPELL,
            payload = CommandPayload(spellType = spellType)
        ))

        setEffect(DuelViewEffect.PlaySound(SoundType.MATCH))
        setEffect(DuelViewEffect.ShowToast(getLocalizedString(
            "施法成功！",
            "Spell casted successfully!",
            "施法成功！",
            "スキル使用成功！",
            "주문 시전 성공!"
        )))
    }

    private fun applySpellEffect(spellType: String) {
        val state = currentState
        if (state.gameStatus != GameStatus.PLAYING) return

        when (spellType) {
            "FOG" -> {
                updateState { copy(isFogActive = true) }
                startSpellCountdown(1, "FOG") {
                    updateState { copy(isFogActive = false) }
                }
            }
            "SHRINK" -> {
                updateState { copy(maxSlotSize = 6) }
                // If currently holding >= 6 cards, immediate defeat
                if (currentState.slotTiles.size >= 6) {
                    sendSystemEvent("LOST")
                    updateState { copy(gameStatus = GameStatus.LOST) }
                }
                startSpellCountdown(1, "SHRINK") {
                    updateState { copy(maxSlotSize = 7) }
                }
            }
            "SEAL_ALL" -> {
                val normalTiles = state.boardTiles.filter { it.state == TileState.NORMAL }
                if (normalTiles.isNotEmpty()) {
                    val newBoard = state.boardTiles.map { tile ->
                        if (tile.state == TileState.NORMAL) {
                            tile.copy(sealedCount = tile.sealedCount + 1)
                        } else tile
                    }
                    updateState {
                        copy(
                            boardTiles = calculateBlockedStates(newBoard),
                            activeSpellMessage = "对手施放了【万重封印】，暴露牌已被封印！"
                        )
                    }
                    viewModelScope.launch {
                        delay(1000)
                        updateState {
                            copy(
                                activeSpellMessage = if (activeSpellMessage?.contains("万重封印") == true) null else activeSpellMessage
                            )
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
                updateState {
                    copy(
                        boardTiles = calculateBlockedStates(newBoard),
                        activeSpellMessage = "对手施放了【乾坤大挪移】，你的牌面类型被打乱了！"
                    )
                }
                viewModelScope.launch {
                    delay(1000)
                    updateState {
                        copy(
                            activeSpellMessage = if (activeSpellMessage?.contains("乾坤大挪移") == true) null else activeSpellMessage
                        )
                    }
                }
            }
            "SILENCE" -> {
                updateState { copy(isSilenced = true) }
                startSpellCountdown(1, "SILENCE") {
                    updateState { copy(isSilenced = false) }
                }
            }
        }
    }

    private fun sendEliminateCommand(ids: List<String>) {
        wsManager.sendCommand(GameCommand(
            gameId = currentState.gameId,
            seqId = ++seqId,
            timestamp = System.currentTimeMillis(),
            senderId = currentState.playerId,
            type = CommandType.ELIMINATE,
            payload = CommandPayload(tilesEliminated = ids, comboCount = currentState.combo)
        ))
    }

    private fun sendAttackCommand() {
        wsManager.sendCommand(GameCommand(
            gameId = currentState.gameId,
            seqId = ++seqId,
            timestamp = System.currentTimeMillis(),
            senderId = currentState.playerId,
            type = CommandType.ATTACK,
            payload = CommandPayload(obstacleType = "SEALED")
        ))
        setEffect(DuelViewEffect.ShowToast(getLocalizedString(
            "发动攻击！封印对手卡牌",
            "Attacking! Sealed opponent tiles",
            "發動攻擊！封印對手卡牌",
            "攻撃発動！相手のカードを封印します",
            "공격 개시! 상대방 카드를 봉인합니다"
        )))
    }

    private fun sendSystemEvent(msg: String) {
        wsManager.sendCommand(GameCommand(
            gameId = currentState.gameId,
            seqId = ++seqId,
            timestamp = System.currentTimeMillis(),
            senderId = currentState.playerId,
            type = CommandType.SYSTEM_EVENT,
            payload = CommandPayload(systemMessage = msg)
        ))
    }

    private fun handleRestart() {
        handleInit(currentState.gameId, currentState.playerId, currentLevelId, currentGameSeed)
    }

    private fun handleLeave() {
        viewModelScope.launch {
            sendSystemEvent("OPPONENT_QUIT")
            delay(500)
            wsManager.disconnect()
            setEffect(DuelViewEffect.ExitGame)
        }
    }



    private fun generateDuelLevel(): List<Tile> {
        // Same logic as GameViewModel's local generation
        val levelId = 2
        val numTypes = 6
        val coordinates = mutableListOf<Point3D>()
        val baseSize = 6
        for (z in 0 until 4) {
            val size = maxOf(3, baseSize - z / 3)
            val offset = if (z % 2 == 0) 0.0f else 0.5f
            for (r in 0 until size) {
                for (c in 0 until size) {
                    coordinates.add(Point3D(c + offset + 1.0f, r + offset + 1.0f, z))
                }
            }
        }
        val count = coordinates.size - (coordinates.size % 3)
        return coordinates.take(count).mapIndexed { index, coord ->
            Tile(
                id = "duel_tile_$index",
                type = (index % numTypes) + 1,
                x = coord.x,
                y = coord.y,
                z = coord.z
            )
        }
    }

    private fun getLocalizedString(zh: String, en: String, tw: String, ja: String, ko: String): String {
        return when (prefs.getLanguage()) {
            "en" -> en
            "tw" -> tw
            "ja" -> ja
            "ko" -> ko
            else -> zh
        }
    }

    private data class Point3D(val x: Float, val y: Float, val z: Int)
}
