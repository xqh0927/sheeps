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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import com.example.sheeps.core.game.GameEngine.calculateBlockedStates
import com.example.sheeps.core.game.GameEngine.isTileBlocked

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
    private var itemsUsedCount: Int = 0

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
                setEffect(GameViewEffect.ShowToast(getLocalizedString("昵称修改成功", "Nickname changed successfully", "暱稱修改成功", "ニックネーム変更完了", "닉네임이 성공적으로 변경되었습니다")))
            } catch (e: Exception) {
                setEffect(GameViewEffect.ShowToast(getLocalizedString("昵称本地已修改（网络同步失败）", "Nickname updated locally (sync failed)", "暱稱本地已修改（網絡同步失敗）", "ローカルで変更完了（同期失敗）", "닉네임이 로컬에서 변경됨 (동기화 실패)")))
            }
        }
    }

    private fun handleLoadLevel(levelId: Int, carryItemsJson: String?) {
        updateState { copy(isLoading = true, currentLevelId = levelId) }
        historyStack.clear()
        levelStartTime = System.currentTimeMillis()
        gameScore = 0
        itemsUsedCount = 0
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
                        gameStatus = GameStatus.PLAYING,
                        currentSkin = prefs.getCurrentSkin()
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
                        gameStatus = GameStatus.PLAYING,
                        currentSkin = prefs.getCurrentSkin()
                    )
                }
                setEffect(GameViewEffect.ShowToast(getLocalizedString("网络加载失败，已切换至单机模式", "Network load failed, switched to offline mode", "網絡加載失敗，已切換至單機模式", "ネットワークエラー、オフラインモードに切り替えました", "네트워크 로드 실패, 오프라인 모드로 전환됨")))
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
        itemsUsedCount++

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
        itemsUsedCount++

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
        itemsUsedCount++

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
        itemsUsedCount++

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
            setEffect(GameViewEffect.ShowToast(getLocalizedString("棋盘上已无成组之卡牌！", "No more matching groups on board!", "棋盤上已無成組之卡牌！", "ボード上に一致するグループはありません！", "보드에 일치하는 그룹이 없습니다!")))
            return
        }

        val targetIds = targetGroup.take(3).map { it.id }.toSet()
        itemsUsedCount++
        updateState {
            copy(
                highlightedTileIds = targetIds,
                hintCount = state.hintCount - 1
            )
        }
        setEffect(GameViewEffect.ShowToast(getLocalizedString("天眼符开启：已为您高亮出一组可消卡牌！", "Hint activated: matching group highlighted!", "天眼符開啟：已為您高亮出一組可消卡牌！", "ヒント有効：一致するグループをハイライトしました！", "힌트 활성화: 일치하는 그룹이 하이라이트되었습니다!")))
    }

    private fun handleUseBomb() {
        val state = currentState
        if (state.gameStatus != GameStatus.PLAYING || state.bombCount <= 0) return

        if (state.slotTiles.size < 2) {
            setEffect(GameViewEffect.ShowToast(getLocalizedString("卡槽内卡牌不足两张，无法使用雷震子！", "Need at least 2 cards in tray to use Bomb!", "卡槽內卡牌不足兩張，無法使用雷震子！", "爆弾を使用するにはトレイに少なくとも2枚のカードが必要です！", "폭탄을 사용하려면 트레이에 카드가 2장 이상 있어야 합니다!")))
            return
        }

        // Destroy the last two tiles from the slot
        val newSlot = state.slotTiles.dropLast(2)
        itemsUsedCount++
        updateState {
            copy(
                slotTiles = newSlot,
                bombCount = state.bombCount - 1
            )
        }
        setEffect(GameViewEffect.ShowToast(getLocalizedString("雷震子炸裂！直接销毁卡槽最后两张牌！", "Bomb exploded! Destroyed the last two cards!", "雷震子炸裂！直接銷毀卡槽最後兩張牌！", "爆弾爆発！最後の2枚のカードが破壊されました！", "폭탄 폭발! 마지막 카드 2장이 제거되었습니다!")))
        
        // Re-run end game checking just in case
        viewModelScope.launch {
            processSlotMatchAndCheckEndGame(state.boardTiles, newSlot, state.movedOutTiles)
        }
    }

    private fun handleUseJoker() {
        val state = currentState
        if (state.gameStatus != GameStatus.PLAYING || state.jokerCount <= 0) return

        if (state.slotTiles.isEmpty()) {
            setEffect(GameViewEffect.ShowToast(getLocalizedString("卡槽为空，太极牌无处幻化！", "Tray is empty, Joker cannot morph!", "卡槽為空，太極牌無處幻化！", "トレイが空です、ワイルドカードは変化できません！", "트레이가 비어 있어 조커를 변환할 수 없습니다!")))
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
        itemsUsedCount++
        updateState {
            copy(
                jokerCount = state.jokerCount - 1
            )
        }
        setEffect(GameViewEffect.ShowToast(getLocalizedString("太极牌显灵！幻化为同名图案凑成消除！", "Joker activated! Morphed to match and eliminate!", "太極牌顯靈！幻化為同名圖案湊成消除！", "ワイルドカード有効！一致させて消去しました！", "조커 활성화! 일치하도록 변환되어 제거되었습니다!")))

        viewModelScope.launch {
            processSlotMatchAndCheckEndGame(state.boardTiles, newSlot, state.movedOutTiles)
        }
    }

    private fun handleUseDoublePoints() {
        val state = currentState
        if (state.gameStatus != GameStatus.PLAYING || state.doublePointsCount <= 0) return
        if (state.isDoublePointsActive) {
            setEffect(GameViewEffect.ShowToast(getLocalizedString("双倍积分已在激活状态中", "Double points already active", "雙倍積分已在激活狀態中", "ダブルポイントは既に有効です", "더블 포인트가 이미 활성화되어 있습니다")))
            return
        }

        updateState {
            copy(
                isDoublePointsActive = true,
                doublePointsCount = state.doublePointsCount - 1
            )
        }
        itemsUsedCount++
        setEffect(GameViewEffect.ShowToast(getLocalizedString("双倍积分符激活！通关时成绩积分将翻倍", "Double points active! Clearing level will double your score", "雙倍積分符激活！通關時成績積分將翻倍", "ダブルポイント有効！クリア時にスコアが2倍になります", "더블 포인트 활성화! 클리어 시 점수가 2배가 됩니다")))
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
                setEffect(GameViewEffect.ShowToast(getLocalizedString("无法加载排行榜，请检查网络连接", "Failed to load leaderboard, check network connection", "無法加載排行榜，請檢查網絡連接", "ランキングの読み込みに失敗しました。接続を確認してください", "랭킹 로드 실패, 네트워크 연결을 확인하세요")))
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

        val timeInSeconds = clearTime / 1000L
        val difficultyCoeff = when (levelId) {
            1 -> 1
            2 -> 2
            3 -> 3
            else -> 4
        }
        val calculatedScore = maxOf(100L, (1000L - timeInSeconds * 2) - itemsUsedCount * 50L) * difficultyCoeff
        val baseScore = calculatedScore.toInt()
        val finalScore = if (currentState.isDoublePointsActive) baseScore * 2 else baseScore

        // 使用全局协程生命周期 scope 提交，防止 Activity.finish 时被 viewModelScope 取消导致云端没记录
        CoroutineScope(Dispatchers.IO).launch {
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
                    if (pointsReward > 0) {
                        getLocalizedString("恭喜通关！首次通关获得50积分，进度已安全存储！", "Congratulations! First clear rewarded 50 points, progress saved!", "恭喜通關！首次通關獲得50積分，進度已安全存儲！", "クリアおめでとうございます！初回クリアで50ポイント獲得、セーブしました！", "클리어를 축하합니다! 최초 클리어로 50포인트를 획득했으며, 저장되었습니다!")
                    } else {
                        getLocalizedString("通关成功！进度已安全存储！", "Cleared! Progress saved!", "通關成功！進度已安全存儲！", "クリア成功！セーブしました！", "클리어 성공! 저장되었습니다!")
                    }
                ))
            } catch (e: Exception) {
                setEffect(GameViewEffect.ShowToast(
                    getLocalizedString("通关成功！已离线保存进度，恢复连接后自动同步", "Cleared! Progress saved offline, will sync when reconnected", "通關成功！已離線保存進度，恢復連接後自動同步", "クリア成功！オフラインで保存しました。再接続時に同期されます", "클리어 성공! 오프라인으로 저장되었으며, 재연결 시 동기화됩니다")
                ))
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
        val numTypes = if (levelId == 1) 3 else minOf(16, (3 + 3 * Math.log(levelId.toDouble())).toInt())

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
            val maxCards = if (levelId == 2) 36 else 36 + (levelId - 2) * 12
            val possible = mutableListOf<Point3D>()
            val layersCount = if (levelId == 2) 4 else minOf(12, (12 - 8 / Math.sqrt((levelId - 1).toDouble())).toInt())

            val baseSize = 6 + levelId / 20
            for (z in 0 until layersCount) {
                val size = maxOf(3, baseSize - z / 3)
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
                if (levelId % 10 == 0) {
                    if (r < 0.15) {
                        isBlind = true
                    } else if (r < 0.30) {
                        sealed = 1
                    }
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
    private data class LocalNode(val index: Int, val coord: Point3D, var assignedType: Int)

    private fun lcg(seed: Long): () -> Double {
        var s = seed
        return {
            s = (s * 1664525L + 1013904223L) % 4294967296L
            s.toDouble() / 4294967296.0
        }
    }
}
