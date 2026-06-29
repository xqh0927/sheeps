package com.example.sheeps.game.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.sheeps.core.base.BaseMviViewModel
import com.example.sheeps.core.preference.UserPreferences
import com.example.sheeps.data.local.BackpackItemEntity
import com.example.sheeps.data.local.LocalDao
import com.example.sheeps.data.model.*
import com.example.sheeps.data.network.ApiService
import com.example.sheeps.data.repository.SyncRepository
import com.example.sheeps.game.state.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max

@HiltViewModel
class GameViewModel @Inject constructor(
    private val apiService: ApiService,
    private val prefs: UserPreferences,
    private val json: Json,
    private val localDao: LocalDao,
    private val syncRepository: SyncRepository
) : BaseMviViewModel<GameViewState, GameViewIntent, GameViewEffect>(GameViewState()) {

    private var levelStartTime: Long = 0
    private var gameScore: Int = 0
    private var carryItemsJsonStr: String? = null

    // Triple of snapshots: board, slot, movedOut
    private val historyStack = mutableListOf<Triple<List<Tile>, List<Tile>, List<Tile>>>()

    init {
        sendIntent(GameViewIntent.InitUser)
    }

    override fun handleIntent(intent: GameViewIntent) {
        when (intent) {
            is GameViewIntent.InitUser -> handleInitUser()
            is GameViewIntent.AgreePrivacy -> handleAgreePrivacy()
            is GameViewIntent.ChangeUsername -> handleChangeUsername(intent.newName)
            is GameViewIntent.LoadLevel -> handleLoadLevel(intent.levelId, intent.carryItemsJson)
            is GameViewIntent.ClickTile -> handleClickTile(intent.tile)
            is GameViewIntent.UseUndo -> handleUseUndo()
            is GameViewIntent.UseMoveOut -> handleUseMoveOut()
            is GameViewIntent.UseShuffle -> handleUseShuffle()
            is GameViewIntent.Revive -> handleRevive()
            is GameViewIntent.UseHint -> handleUseHint()
            is GameViewIntent.UseBomb -> handleUseBomb()
            is GameViewIntent.UseJoker -> handleUseJoker()
            is GameViewIntent.UseDoublePoints -> handleUseDoublePoints()
            is GameViewIntent.LoadLeaderboard -> handleLoadLeaderboard(intent.levelId)
            is GameViewIntent.RestartLevel -> handleLoadLevel(currentState.currentLevelId, carryItemsJsonStr)
            is GameViewIntent.GoBackToMenu -> handleGoBackToMenu()
        }
    }

    private fun handleInitUser() {
        val accepted = prefs.isPrivacyAccepted()
        updateState {
            copy(
                isPrivacyAccepted = accepted,
                username = prefs.getUsername(),
                unlockedLevel = prefs.getUnlockedLevel(),
                gameStatus = if (accepted) GameStatus.MENU else GameStatus.INIT
            )
        }
        if (accepted) {
            viewModelScope.launch {
                try {
                    apiService.register(RegisterRequest(prefs.getUserId(), prefs.getUsername()))
                } catch (e: Exception) {
                    // Offline ignore
                }
            }
        }
    }

    private fun handleAgreePrivacy() {
        prefs.setPrivacyAccepted(true)
        updateState {
            copy(
                isPrivacyAccepted = true,
                gameStatus = GameStatus.MENU
            )
        }
        handleInitUser()
    }

    private fun handleChangeUsername(newName: String) {
        if (newName.isBlank()) return
        prefs.setUsername(newName)
        updateState { copy(username = newName) }
        viewModelScope.launch {
            try {
                apiService.rename(RenameRequest(prefs.getUserId(), newName))
                setEffect(GameViewEffect.ShowToast("昵称修改成功"))
            } catch (e: Exception) {
                setEffect(GameViewEffect.ShowToast("昵称本地已修改（网络同步失败）"))
            }
        }
    }

    private fun handleLoadLevel(levelId: Int, carryItemsJson: String?) {
        updateState { copy(isLoading = true, currentLevelId = levelId) }
        historyStack.clear()
        levelStartTime = System.currentTimeMillis()
        gameScore = 0
        carryItemsJsonStr = carryItemsJson

        // Parse carry map
        val carryMap = try {
            if (!carryItemsJson.isNullOrEmpty()) {
                json.decodeFromString<Map<String, Int>>(carryItemsJson)
            } else emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }

        val undo = carryMap["UNDO"] ?: 0
        val shuffle = carryMap["SHUFFLE"] ?: 0
        val moveOut = carryMap["MOVEOUT"] ?: 0
        val revive = carryMap["REVIVE"] ?: 0
        val hint = carryMap["HINT"] ?: 0
        val bomb = carryMap["BOMB"] ?: 0
        val joker = carryMap["JOKER"] ?: 0
        val double = carryMap["DOUBLE_POINTS"] ?: 0

        viewModelScope.launch(Dispatchers.IO) {
            // Local-First: Deduct selected carry items from database
            val now = System.currentTimeMillis()
            carryMap.forEach { (type, count) ->
                if (count > 0) {
                    val currentItem = localDao.getAllItems().find { it.itemType == type }
                    val remainingCount = max(0, (currentItem?.count ?: 0) - count)
                    localDao.insertItem(BackpackItemEntity(
                        itemType = type,
                        count = remainingCount,
                        isDirty = true,
                        updateTimestamp = now
                    ))
                }
            }
            
            // Sync in background
            syncRepository.syncDirtyData()

            try {
                val serverTiles = apiService.getLevel(levelId)
                val processedTiles = serverTiles.map { it.copy(state = TileState.NORMAL) }
                val finalTiles = calculateBlockedStates(processedTiles)

                updateState {
                    copy(
                        isLoading = false,
                        boardTiles = finalTiles,
                        slotTiles = emptyList(),
                        movedOutTiles = emptyList(),
                        undoCount = undo,
                        moveOutCount = moveOut,
                        shuffleCount = shuffle,
                        reviveCount = revive,
                        hintCount = hint,
                        bombCount = bomb,
                        jokerCount = joker,
                        doublePointsCount = double,
                        isDoublePointsActive = false,
                        highlightedTileIds = emptySet(),
                        score = 0,
                        gameStatus = GameStatus.PLAYING
                    )
                }
            } catch (e: Exception) {
                val localTiles = generateSolvableLevelLocal(levelId)
                val finalTiles = calculateBlockedStates(localTiles)

                updateState {
                    copy(
                        isLoading = false,
                        boardTiles = finalTiles,
                        slotTiles = emptyList(),
                        movedOutTiles = emptyList(),
                        undoCount = undo,
                        moveOutCount = moveOut,
                        shuffleCount = shuffle,
                        reviveCount = revive,
                        hintCount = hint,
                        bombCount = bomb,
                        jokerCount = joker,
                        doublePointsCount = double,
                        isDoublePointsActive = false,
                        highlightedTileIds = emptySet(),
                        score = 0,
                        gameStatus = GameStatus.PLAYING
                    )
                }
                setEffect(GameViewEffect.ShowToast("网络加载失败，已切换至单机模式"))
            }
        }
    }

    private fun handleClickTile(tile: Tile) {
        val state = currentState
        if (state.gameStatus != GameStatus.PLAYING) return

        if (tile.state != TileState.NORMAL && tile.state != TileState.MOVED_OUT) return
        if (tile.state == TileState.NORMAL && isTileBlocked(tile, state.boardTiles)) return

        saveHistoryState()
        
        // Hide active hint on click
        updateState { copy(highlightedTileIds = emptySet()) }

        viewModelScope.launch {
            if (tile.state == TileState.MOVED_OUT) {
                val updatedBoard = state.boardTiles
                val updatedMovedOut = state.movedOutTiles.filter { it.id != tile.id }
                val newSlot = state.slotTiles + tile.copy(state = TileState.IN_SLOT)

                setEffect(GameViewEffect.PlaySound(SoundType.CLICK))
                processSlotMatchAndCheckEndGame(updatedBoard, newSlot, updatedMovedOut)
            } else {
                if (tile.sealedCount > 0) {
                    tile.sealedCount--
                    setEffect(GameViewEffect.PlaySound(SoundType.UNSEAL))
                    setEffect(GameViewEffect.Vibrate)
                    updateState {
                        copy(boardTiles = calculateBlockedStates(state.boardTiles))
                    }
                } else {
                    tile.state = TileState.IN_SLOT
                    val updatedBoard = state.boardTiles
                    val newSlot = state.slotTiles + tile

                    setEffect(GameViewEffect.PlaySound(SoundType.CLICK))
                    processSlotMatchAndCheckEndGame(updatedBoard, newSlot, state.movedOutTiles)
                }
            }
        }
    }

    private suspend fun processSlotMatchAndCheckEndGame(
        board: List<Tile>,
        slot: List<Tile>,
        movedOut: List<Tile>
    ) {
        val finalBoard = calculateBlockedStates(board)
        var finalSlot = slot.toMutableList()

        finalSlot.sortBy { it.type }

        val counts = finalSlot.groupBy { it.type }
        var matched = false
        for ((type, items) in counts) {
            if (items.size >= 3) {
                var removedCount = 0
                finalSlot.removeAll {
                    if (it.type == type && removedCount < 3) {
                        removedCount++
                        true
                    } else false
                }
                gameScore += 100
                matched = true
                break
            }
        }

        if (matched) {
            setEffect(GameViewEffect.PlaySound(SoundType.MATCH))
        }

        val remainingOnBoard = finalBoard.count { it.state == TileState.NORMAL || it.state == TileState.BLOCKED }
        val remainingInMovedOut = movedOut.size

        val newStatus = when {
            remainingOnBoard == 0 && remainingInMovedOut == 0 -> {
                setEffect(GameViewEffect.PlaySound(SoundType.WIN))
                submitScoreOnline()
                GameStatus.WON
            }
            finalSlot.size >= 7 -> {
                setEffect(GameViewEffect.PlaySound(SoundType.LOSE))
                GameStatus.LOST
            }
            else -> GameStatus.PLAYING
        }

        updateState {
            copy(
                boardTiles = finalBoard,
                slotTiles = finalSlot,
                movedOutTiles = movedOut,
                score = gameScore,
                gameStatus = newStatus
            )
        }
    }

    private fun handleUseUndo() {
        val state = currentState
        if (state.gameStatus != GameStatus.PLAYING || state.undoCount <= 0 || historyStack.isEmpty()) return

        val (prevBoard, prevSlot, prevMovedOut) = historyStack.removeLast()

        updateState {
            copy(
                boardTiles = calculateBlockedStates(prevBoard),
                slotTiles = prevSlot,
                movedOutTiles = prevMovedOut,
                undoCount = state.undoCount - 1
            )
        }
        viewModelScope.launch {
            setEffect(GameViewEffect.PlaySound(SoundType.CLICK))
        }
    }

    private fun handleUseMoveOut() {
        val state = currentState
        if (state.gameStatus != GameStatus.PLAYING || state.moveOutCount <= 0 || state.slotTiles.isEmpty()) return

        val toMove = state.slotTiles.take(3).map { it.copy(state = TileState.MOVED_OUT) }
        val remainingSlot = state.slotTiles.drop(3)
        val newMovedOut = state.movedOutTiles + toMove

        updateState {
            copy(
                slotTiles = remainingSlot,
                movedOutTiles = newMovedOut,
                moveOutCount = state.moveOutCount - 1,
                boardTiles = calculateBlockedStates(state.boardTiles)
            )
        }
        viewModelScope.launch {
            setEffect(GameViewEffect.PlaySound(SoundType.CLICK))
        }
    }

    private fun handleUseShuffle() {
        val state = currentState
        if (state.gameStatus != GameStatus.PLAYING || state.shuffleCount <= 0) return

        val activeTiles = state.boardTiles.filter { it.state == TileState.NORMAL || it.state == TileState.BLOCKED }
        if (activeTiles.isEmpty()) return

        val shuffledTypes = activeTiles.map { it.type }.shuffled()

        var idx = 0
        val newBoard = state.boardTiles.map { tile ->
            if (tile.state == TileState.NORMAL || tile.state == TileState.BLOCKED) {
                tile.copy(type = shuffledTypes[idx++])
            } else {
                tile
            }
        }

        updateState {
            copy(
                boardTiles = calculateBlockedStates(newBoard),
                shuffleCount = state.shuffleCount - 1
            )
        }
        viewModelScope.launch {
            setEffect(GameViewEffect.PlaySound(SoundType.CLICK))
        }
    }

    private fun handleRevive() {
        val state = currentState
        if (state.gameStatus != GameStatus.LOST || state.reviveCount <= 0) return

        val toMove = state.slotTiles.take(3).map { it.copy(state = TileState.MOVED_OUT) }
        val remainingSlot = state.slotTiles.drop(3)
        val newMovedOut = state.movedOutTiles + toMove

        updateState {
            copy(
                slotTiles = remainingSlot,
                movedOutTiles = newMovedOut,
                reviveCount = state.reviveCount - 1,
                gameStatus = GameStatus.PLAYING,
                boardTiles = calculateBlockedStates(state.boardTiles)
            )
        }
        viewModelScope.launch {
            setEffect(GameViewEffect.PlaySound(SoundType.CLICK))
        }
    }

    private fun handleUseHint() {
        val state = currentState
        if (state.gameStatus != GameStatus.PLAYING || state.hintCount <= 0) return

        // Find remaining active cards
        val active = state.boardTiles.filter { it.state == TileState.NORMAL || it.state == TileState.BLOCKED }
        val groups = active.groupBy { it.type }
        // Find a card type that has at least 3 matching cards remaining
        val targetGroup = groups.values.find { it.size >= 3 }

        if (targetGroup == null) {
            setEffect(GameViewEffect.ShowToast("棋盘上已无成组之卡牌！"))
            return
        }

        val targetIds = targetGroup.take(3).map { it.id }.toSet()
        updateState {
            copy(
                highlightedTileIds = targetIds,
                hintCount = state.hintCount - 1
            )
        }
        setEffect(GameViewEffect.ShowToast("天眼符开启：已为您高亮出一组可消卡牌！"))
    }

    private fun handleUseBomb() {
        val state = currentState
        if (state.gameStatus != GameStatus.PLAYING || state.bombCount <= 0) return

        if (state.slotTiles.size < 2) {
            setEffect(GameViewEffect.ShowToast("卡槽内卡牌不足两张，无法使用雷震子！"))
            return
        }

        // Destroy the last two tiles from the slot
        val newSlot = state.slotTiles.dropLast(2)
        updateState {
            copy(
                slotTiles = newSlot,
                bombCount = state.bombCount - 1
            )
        }
        setEffect(GameViewEffect.ShowToast("雷震子炸裂！直接销毁卡槽最后两张牌！"))
        
        // Re-run end game checking just in case
        viewModelScope.launch {
            processSlotMatchAndCheckEndGame(state.boardTiles, newSlot, state.movedOutTiles)
        }
    }

    private fun handleUseJoker() {
        val state = currentState
        if (state.gameStatus != GameStatus.PLAYING || state.jokerCount <= 0) return

        if (state.slotTiles.isEmpty()) {
            setEffect(GameViewEffect.ShowToast("卡槽为空，太极牌无处幻化！"))
            return
        }

        saveHistoryState()

        // Joker transforms into the type of the last card in the slot to trigger an instant match
        val targetType = state.slotTiles.last().type
        val jokerTile = Tile(
            id = "joker_${System.currentTimeMillis()}",
            type = targetType,
            x = 0f,
            y = 0f,
            z = 999,
            state = TileState.IN_SLOT
        )

        val newSlot = state.slotTiles + jokerTile
        updateState {
            copy(
                jokerCount = state.jokerCount - 1
            )
        }
        setEffect(GameViewEffect.ShowToast("太极牌显灵！幻化为同名图案凑成消除！"))

        viewModelScope.launch {
            processSlotMatchAndCheckEndGame(state.boardTiles, newSlot, state.movedOutTiles)
        }
    }

    private fun handleUseDoublePoints() {
        val state = currentState
        if (state.gameStatus != GameStatus.PLAYING || state.doublePointsCount <= 0) return
        if (state.isDoublePointsActive) {
            setEffect(GameViewEffect.ShowToast("双倍积分已在激活状态中"))
            return
        }

        updateState {
            copy(
                isDoublePointsActive = true,
                doublePointsCount = state.doublePointsCount - 1
            )
        }
        setEffect(GameViewEffect.ShowToast("双倍积分符激活！通关时成绩积分将翻倍"))
    }

    private fun handleLoadLeaderboard(levelId: Int) {
        updateState { copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val response = apiService.getLeaderboard(levelId)
                updateState {
                    copy(isLoading = false, rankings = response.rankings)
                }
            } catch (e: Exception) {
                updateState { copy(isLoading = false, rankings = emptyList()) }
                setEffect(GameViewEffect.ShowToast("无法加载排行榜，请检查网络连接"))
            }
        }
    }

    private fun handleGoBackToMenu() {
        updateState {
            copy(
                gameStatus = GameStatus.MENU,
                unlockedLevel = prefs.getUnlockedLevel()
            )
        }
    }

    private fun isTileBlocked(tile: Tile, board: List<Tile>): Boolean {
        val W = 1.0f
        val H = 1.0f
        return board.any { other ->
            other.id != tile.id &&
            (other.state == TileState.NORMAL || other.state == TileState.BLOCKED) &&
            other.z > tile.z &&
            abs(other.x - tile.x) < W &&
            abs(other.y - tile.y) < H
        }
    }

    private fun calculateBlockedStates(board: List<Tile>): List<Tile> {
        return board.map { tile ->
            if (tile.state == TileState.NORMAL || tile.state == TileState.BLOCKED) {
                val blocked = isTileBlocked(tile, board)
                tile.copy(state = if (blocked) TileState.BLOCKED else TileState.NORMAL)
            } else {
                tile
            }
        }
    }

    private fun saveHistoryState() {
        val state = currentState
        val boardCopy = state.boardTiles.map { it.copy() }
        val slotCopy = state.slotTiles.map { it.copy() }
        val movedOutCopy = state.movedOutTiles.map { it.copy() }
        historyStack.add(Triple(boardCopy, slotCopy, movedOutCopy))
    }

    private fun submitScoreOnline() {
        val levelId = currentState.currentLevelId
        val clearTime = System.currentTimeMillis() - levelStartTime

        if (levelId == prefs.getUnlockedLevel()) {
            prefs.setUnlockedLevel(levelId + 1)
        }

        val finalScore = if (currentState.isDoublePointsActive) gameScore * 2 else gameScore

        viewModelScope.launch {
            try {
                // Local-First: Save clearing progress and reward points to Room
                val currentProgress = localDao.getAllProgress().find { it.levelId == levelId && it.score > 0 }
                val pointsReward = if (currentProgress == null) 50 else 0
                
                syncRepository.saveProgressAndPointsLocally(
                    levelId = levelId,
                    score = finalScore,
                    clearTime = clearTime,
                    pointsGained = pointsReward
                )

                // Try online leaderboard sync
                val userId = prefs.getUserId()
                val token = prefs.getToken()
                val authHeader = if (token != null) "Bearer $token" else null
                val inputStr = "${userId}_${levelId}_${clearTime}_folklore"
                val sign = sha256(inputStr)

                apiService.submitScore(
                    auth = authHeader,
                    request = ScoreRequest(
                        user_id = userId,
                        level_id = levelId,
                        score = finalScore,
                        clear_time_ms = clearTime,
                        sign = sign
                    )
                )

                setEffect(GameViewEffect.ShowToast(
                    if (pointsReward > 0) "恭喜通关！首次通关获得50积分，进度已安全存储！" 
                    else "通关成功！进度已安全存储！"
                ))
            } catch (e: Exception) {
                setEffect(GameViewEffect.ShowToast("通关成功！已离线保存进度，恢复连接后自动同步"))
            }
        }
    }

    private fun sha256(input: String): String {
        val bytes = input.toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    private fun generateSolvableLevelLocal(levelId: Int): List<Tile> {
        val numTypes = when (levelId) {
            1 -> 3
            2 -> 6
            else -> minOf(12, 6 + levelId / 2)
        }

        val coordinates = mutableListOf<Point3D>()
        if (levelId == 1) {
            coordinates.addAll(listOf(
                Point3D(1.0f, 1.0f, 0), Point3D(2.0f, 1.0f, 0),
                Point3D(1.0f, 2.0f, 0), Point3D(2.0f, 2.0f, 0),
                Point3D(1.5f, 1.5f, 1), Point3D(2.5f, 1.5f, 1),
                Point3D(1.5f, 2.5f, 1), Point3D(2.5f, 2.5f, 1),
                Point3D(2.0f, 2.0f, 2)
            ))
        } else {
            val maxCards = if (levelId == 2) 36 else minOf(144, 72 + (levelId - 3) * 12)
            val possible = mutableListOf<Point3D>()
            val layers = if (levelId == 2) 4 else minOf(8, 4 + levelId / 2)

            for (z in 0 until layers) {
                val size = 6 - z / 2
                val offset = if (z % 2 == 0) 0.0f else 0.5f
                for (r in 0 until size) {
                    for (c in 0 until size) {
                        possible.add(Point3D(c + offset + 1.0f, r + offset + 1.0f, z))
                    }
                }
            }

            val rand = lcg(levelId * 1000L)
            for (i in possible.indices.reversed()) {
                val j = (rand() * (i + 1)).toInt()
                val temp = possible[i]
                possible[i] = possible[j]
                possible[j] = temp
            }

            val count = minOf(possible.size, maxCards) - (minOf(possible.size, maxCards) % 3)
            coordinates.addAll(possible.take(count))
        }

        coordinates.sortBy { it.z }

        val nodes = coordinates.mapIndexed { index, coord ->
            LocalNode(index, coord, -1)
        }

        val unassigned = nodes.toMutableSet()
        val randAssign = lcg(levelId * 1000L + 100)

        val blocks = { a: Point3D, b: Point3D ->
            a.z > b.z && abs(a.x - b.x) < 1.0f && abs(a.y - b.y) < 1.0f
        }

        while (unassigned.isNotEmpty()) {
            val exposed = unassigned.filter { node ->
                unassigned.none { other -> other != node && blocks(other.coord, node.coord) }
            }

            if (exposed.size < 3) {
                val rem = unassigned.toList()
                var k = 0
                while (k + 2 < rem.size) {
                    val t = (randAssign() * numTypes).toInt() + 1
                    rem[k].assignedType = t
                    rem[k + 1].assignedType = t
                    rem[k + 2].assignedType = t
                    unassigned.remove(rem[k])
                    unassigned.remove(rem[k + 1])
                    unassigned.remove(rem[k + 2])
                    k += 3
                }
                for (node in unassigned) {
                    node.assignedType = 1
                }
                unassigned.clear()
                break
            }

            val type = (randAssign() * numTypes).toInt() + 1
            val exposedMutable = exposed.toMutableList()
            for (k in 0 until 3) {
                val idx = (randAssign() * exposedMutable.size).toInt()
                val chosen = exposedMutable.removeAt(idx)
                chosen.assignedType = type
                unassigned.remove(chosen)
            }
        }

        val randProps = lcg(levelId * 1000L + 200)
        return nodes.map { node ->
            var isBlind = false
            var sealed = 0

            if (levelId >= 2) {
                val r = randProps()
                if (r < 0.15) {
                    isBlind = true
                } else if (r < 0.30) {
                    sealed = 1
                }
            }

            Tile(
                id = "tile_${node.index}",
                x = node.coord.x,
                y = node.coord.y,
                z = node.coord.z,
                type = node.assignedType,
                isBlind = isBlind,
                sealedCount = sealed
            )
        }
    }

    private data class Point3D(val x: Float, val y: Float, val z: Int)
    private data class LocalNode(val index: Int, val coord: Point3D, var assignedType: Int)

    private fun lcg(seed: Long): () -> Double {
        var s = seed
        return {
            s = (s * 1664525L + 1013904223L) % 4294967296L
            s.toDouble() / 4294967296.0
        }
    }
}
