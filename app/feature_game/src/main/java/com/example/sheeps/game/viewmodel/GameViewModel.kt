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
import com.example.sheeps.game.viewmodel.delegates.GameLogicDelegate
import com.example.sheeps.game.viewmodel.delegates.GameToolDelegate
import com.example.sheeps.game.viewmodel.delegates.ScoreDelegate
import com.example.sheeps.game.viewmodel.helpers.GameLevelGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject
import com.example.sheeps.core.game.GameEngine.calculateBlockedStates
import kotlinx.coroutines.CoroutineScope

/**
 * 游戏主逻辑 ViewModel
 * 核心逻辑已拆分至 Logic、Tool、Score 等 Delegate 中，保持主类逻辑清晰。
 */
@HiltViewModel
class GameViewModel @Inject constructor(
    private val apiService: ApiService,
    private val prefs: UserPreferences,
    private val json: Json,
    private val localDao: LocalDao,
    private val syncRepository: SyncRepository,
    // 逻辑拆分
    private val levelGenerator: GameLevelGenerator,
    private val logicDelegate: GameLogicDelegate,
    private val toolDelegate: GameToolDelegate,
    private val scoreDelegate: ScoreDelegate
) : BaseMviViewModel<GameViewState, GameViewIntent, GameViewEffect>(GameViewState()) {

    private var levelStartTime: Long = 0
    private var carryItemsJsonStr: String? = null
    private var itemsUsedCount: Int = 0

    // 历史状态栈（用于撤销功能）
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
            is GameViewIntent.ClickTile -> logicDelegate.handleClickTile(
                viewModelScope, intent.tile, currentState, ::saveHistoryState, ::updateState, ::setEffect, ::processSlotMatchAndCheckEndGame
            )
            is GameViewIntent.UseUndo -> toolDelegate.handleUseUndo(currentState, historyStack, ::updateState, ::setEffect) { itemsUsedCount++ }
            is GameViewIntent.UseMoveOut -> toolDelegate.handleUseMoveOut(currentState, ::updateState, ::setEffect) { itemsUsedCount++ }
            is GameViewIntent.UseShuffle -> toolDelegate.handleUseShuffle(currentState, ::updateState, ::setEffect) { itemsUsedCount++ }
            is GameViewIntent.Revive -> handleRevive()
            is GameViewIntent.UseHint -> toolDelegate.handleUseHint(currentState, ::getLocalizedString, ::updateState, ::setEffect) { itemsUsedCount++ }
            is GameViewIntent.UseBomb -> toolDelegate.handleUseBomb(currentState, ::getLocalizedString, ::updateState, ::setEffect, { itemsUsedCount++ }, ::processSlotMatchAndCheckEndGame)
            is GameViewIntent.UseJoker -> toolDelegate.handleUseJoker(currentState, ::getLocalizedString, ::updateState, ::setEffect, { itemsUsedCount++ }, ::saveHistoryState, ::processSlotMatchAndCheckEndGame)
            is GameViewIntent.UseDoublePoints -> handleUseDoublePoints()
            is GameViewIntent.LoadLeaderboard -> handleLoadLeaderboard(intent.levelId)
            is GameViewIntent.RestartLevel -> handleLoadLevel(currentState.currentLevelId, carryItemsJsonStr)
            is GameViewIntent.GoBackToMenu -> updateState { copy(gameStatus = GameStatus.MENU, unlockedLevel = prefs.getUnlockedLevel()) }
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
                try { apiService.register(RegisterRequest(prefs.getUserId(), prefs.getUsername())) } catch (e: Exception) {}
            }
        }
    }

    private fun handleAgreePrivacy() {
        prefs.setPrivacyAccepted(true)
        updateState { copy(isPrivacyAccepted = true, gameStatus = GameStatus.MENU) }
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
                setEffect(GameViewEffect.ShowToast(getLocalizedString("昵称本地已修改", "Nickname updated locally", "暱稱本地已修改", "ローカルで変更完了", "닉네임이 로컬에서 변경됨")))
            }
        }
    }

    private fun handleLoadLevel(levelId: Int, carryItemsJson: String?) {
        updateState { copy(isLoading = true, currentLevelId = levelId) }
        historyStack.clear()
        levelStartTime = System.currentTimeMillis()
        itemsUsedCount = 0
        carryItemsJsonStr = carryItemsJson

        val carryMap = try {
            if (!carryItemsJson.isNullOrEmpty()) json.decodeFromString<Map<String, Int>>(carryItemsJson) else emptyMap()
        } catch (e: Exception) { emptyMap() }

        viewModelScope.launch(Dispatchers.IO) {
            // 扣除携带道具库存
            val now = System.currentTimeMillis()
            carryMap.forEach { (type, count) ->
                if (count > 0) {
                    val current = localDao.getAllItems().find { it.itemType == type }
                    localDao.insertItem(BackpackItemEntity(type, maxOf(0, (current?.count ?: 0) - count), true, now))
                }
            }
            syncRepository.syncDirtyData()

            try {
                val finalTiles = calculateBlockedStates(apiService.getLevel(levelId).map { it.copy(state = TileState.NORMAL) })
                updateBoardState(finalTiles, carryMap, false)
            } catch (e: Exception) {
                val finalTiles = calculateBlockedStates(levelGenerator.generateSolvableLevelLocal(levelId))
                updateBoardState(finalTiles, carryMap, true)
            }
        }
    }

    private fun updateBoardState(tiles: List<Tile>, carryMap: Map<String, Int>, isOffline: Boolean) {
        updateState {
            copy(
                isLoading = false, boardTiles = tiles, slotTiles = emptyList(), movedOutTiles = emptyList(),
                undoCount = carryMap["UNDO"] ?: 0, shuffleCount = carryMap["SHUFFLE"] ?: 0,
                moveOutCount = carryMap["MOVEOUT"] ?: 0, reviveCount = carryMap["REVIVE"] ?: 0,
                hintCount = carryMap["HINT"] ?: 0, bombCount = carryMap["BOMB"] ?: 0,
                jokerCount = carryMap["JOKER"] ?: 0, doublePointsCount = carryMap["DOUBLE_POINTS"] ?: 0,
                isDoublePointsActive = false, score = 0, gameStatus = GameStatus.PLAYING, currentSkin = prefs.getCurrentSkin()
            )
        }
        if (isOffline) setEffect(GameViewEffect.ShowToast(getLocalizedString("网络连接失败，已切换至单机模式", "Offline mode active", "網絡連接失敗，已切換至單機模式", "オフラインモード", "오프라인 모드")))
    }

    private suspend fun processSlotMatchAndCheckEndGame(board: List<Tile>, slot: List<Tile>, movedOut: List<Tile>) {
        logicDelegate.processSlotMatchAndCheckEndGame(
            board, slot, movedOut, currentState.score, currentState.isDoublePointsActive, ::updateState, ::setEffect
        ) {
            scoreDelegate.submitScoreOnline(CoroutineScope(Dispatchers.IO), currentState.currentLevelId, levelStartTime, itemsUsedCount, currentState.isDoublePointsActive, ::getLocalizedString, ::setEffect)
        }
    }

    private fun handleRevive() {
        val state = currentState
        if (state.gameStatus != GameStatus.LOST || state.reviveCount <= 0) return
        val toMove = state.slotTiles.take(3).map { it.copy(state = TileState.MOVED_OUT) }
        itemsUsedCount++
        updateState {
            copy(
                slotTiles = state.slotTiles.drop(3), movedOutTiles = state.movedOutTiles + toMove,
                reviveCount = state.reviveCount - 1, gameStatus = GameStatus.PLAYING,
                boardTiles = calculateBlockedStates(state.boardTiles)
            )
        }
        viewModelScope.launch { setEffect(GameViewEffect.PlaySound(SoundType.CLICK)) }
    }

    private fun handleUseDoublePoints() {
        if (currentState.isDoublePointsActive || currentState.doublePointsCount <= 0) return
        updateState { copy(isDoublePointsActive = true, doublePointsCount = doublePointsCount - 1) }
        itemsUsedCount++
        setEffect(GameViewEffect.ShowToast(getLocalizedString("双倍积分已激活", "Double points active", "雙倍積分已激活", "ダブルポイント有効", "더블 포인트 활성화")))
    }

    private fun handleLoadLeaderboard(levelId: Int) {
        updateState { copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val res = apiService.getLeaderboard(levelId)
                updateState { copy(isLoading = false, rankings = res.rankings) }
            } catch (e: Exception) {
                updateState { copy(isLoading = false, rankings = emptyList()) }
                setEffect(GameViewEffect.ShowToast(getLocalizedString("加载排行榜失败", "Load failed", "加載排行榜失敗", "読み込み失敗", "로드 실패")))
            }
        }
    }

    private fun saveHistoryState() {
        historyStack.add(Triple(currentState.boardTiles.map { it.copy() }, currentState.slotTiles.map { it.copy() }, currentState.movedOutTiles.map { it.copy() }))
    }

    private fun getLocalizedString(zh: String, en: String, tw: String, ja: String, ko: String): String {
        return when (prefs.getLanguage()) {
            "en" -> en; "tw" -> tw; "ja" -> ja; "ko" -> ko; else -> zh
        }
    }
}
