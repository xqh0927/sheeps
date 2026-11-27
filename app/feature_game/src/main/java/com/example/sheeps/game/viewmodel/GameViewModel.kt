package com.example.sheeps.game.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.sheeps.lib_base.base.BaseMviViewModel
import com.example.sheeps.core.game.GameEngine.calculateBlockedStates
import com.example.sheeps.data.preference.UserPreferences
import com.example.sheeps.data.local.LocalDao
import com.example.sheeps.data.local.PendingStageStart
import com.example.sheeps.data.local.PendingStageScore
import com.example.sheeps.data.model.RenameRequest
import com.example.sheeps.data.model.TileState
import com.example.sheeps.data.network.ApiService
import com.example.sheeps.data.repository.SyncRepository
import com.example.sheeps.data.repository.StageSaveSession
import com.example.sheeps.data.repository.StageSaves
import com.example.sheeps.data.repository.StageSnapshot
import com.example.sheeps.game.state.GameStatus
import com.example.sheeps.game.state.GameViewEffect
import com.example.sheeps.game.state.GameHistoryState
import com.example.sheeps.game.state.GameViewIntent
import com.example.sheeps.game.state.elapsedAt
import com.example.sheeps.game.state.GameViewState
import com.example.sheeps.game.state.SoundType
import com.example.sheeps.game.viewmodel.delegates.GameLogicDelegate
import com.example.sheeps.game.viewmodel.delegates.GameToolDelegate
import com.example.sheeps.game.viewmodel.delegates.ScoreDelegate
import com.example.sheeps.data.repository.StageBoards
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * 游戏主逻辑 ViewModel
 * 核心逻辑已拆分至 Logic、Tool、Score 等 Delegate 中，保持主类逻辑清晰。
 */
@HiltViewModel
class GameViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val apiService: ApiService,
    private val prefs: UserPreferences,
    private val json: Json,
    private val localDao: LocalDao,
    private val syncRepository: SyncRepository,
    // 逻辑拆分
    private val stageBoards: StageBoards,
    private val logicDelegate: GameLogicDelegate,
    private val toolDelegate: GameToolDelegate,
    private val scoreDelegate: ScoreDelegate,
    private val saves: StageSaves
) : BaseMviViewModel<GameViewState, GameViewIntent, GameViewEffect>(GameViewState()) {


    private var foreground = true
    private var session: StageSaveSession? = null
    private val runs = LevelRuns(savedState)
    private var levelLoad: Job? = null
    private var restartPicker: Job? = null
    private var itemsUsedCount: Int = 0
    private var hasSubmittedThisRun: Boolean = false

    // 历史状态栈（用于撤销功能）
    private val historyStack = mutableListOf<GameHistoryState>()

    init {
        sendIntent(GameViewIntent.InitUser)
    }

    override fun handleIntent(intent: GameViewIntent) {
        val before = currentState
        val isMove = when (intent) {
            is GameViewIntent.ClickTile, GameViewIntent.UseUndo, GameViewIntent.UseMoveOut,
            GameViewIntent.UseShuffle, GameViewIntent.UseHint, GameViewIntent.UseBomb,
            GameViewIntent.UseJoker, GameViewIntent.UseDoublePoints, GameViewIntent.Revive -> true
            else -> false
        }
        if (isMove && (before.isLoading || session?.current != true || session?.owner != (prefs.accountId() ?: prefs.getUserId()) ||
            (before.gameStatus != GameStatus.PLAYING && intent != GameViewIntent.Revive))) return
        when (intent) {
            is GameViewIntent.InitUser -> handleInitUser()
            is GameViewIntent.AgreePrivacy -> handleAgreePrivacy()
            is GameViewIntent.ChangeUsername -> handleChangeUsername(intent.newName)
            is GameViewIntent.LoadLevel -> if (!currentState.isLoading || currentState.currentLevelId != intent.levelId) {
                handleLoadLevel(runs.start(intent.levelId, intent.carryItemsJson))
            }
            is GameViewIntent.ClickTile -> {
                val tile = (if (intent.tile.state == TileState.MOVED_OUT) before.movedOutTiles else before.boardTiles)
                    .firstOrNull { it.id == intent.tile.id } ?: return
                logicDelegate.handleClickTile(
                    tile,
                    currentState,
                    ::saveHistoryState,
                    ::updateState,
                    ::setEffect,
                    ::processSlotMatchAndCheckEndGame
                )
            }

            is GameViewIntent.UseUndo -> toolDelegate.handleUseUndo(
                currentState,
                historyStack,
                ::updateState,
                ::setEffect
            ) { itemsUsedCount++ }

            is GameViewIntent.UseMoveOut -> toolDelegate.handleUseMoveOut(
                currentState,
                ::updateState,
                ::setEffect
            ) { itemsUsedCount++ }

            is GameViewIntent.UseShuffle -> toolDelegate.handleUseShuffle(
                currentState,
                ::updateState,
                ::setEffect
            ) { itemsUsedCount++ }

            is GameViewIntent.Revive -> handleRevive()
            is GameViewIntent.UseHint -> toolDelegate.handleUseHint(
                currentState,
                ::getLocalizedString,
                ::updateState,
                ::setEffect
            ) { itemsUsedCount++ }

            is GameViewIntent.UseBomb -> toolDelegate.handleUseBomb(
                currentState,
                ::getLocalizedString,
                ::updateState,
                ::setEffect,
                { itemsUsedCount++ },
                ::processSlotMatchAndCheckEndGame
            )

            is GameViewIntent.UseJoker -> toolDelegate.handleUseJoker(
                currentState,
                ::getLocalizedString,
                ::updateState,
                ::setEffect,
                { itemsUsedCount++ },
                ::saveHistoryState,
                ::processSlotMatchAndCheckEndGame
            )

            is GameViewIntent.UseDoublePoints -> handleUseDoublePoints()
            is GameViewIntent.LoadLeaderboard -> handleLoadLeaderboard(intent.levelId)
            GameViewIntent.RestartLevel, GameViewIntent.TriggerRestartFlow -> handleTriggerRestartFlow()
            is GameViewIntent.UpdateTempCarryItem -> handleUpdateTempCarryItem(
                intent.itemType,
                intent.change
            )

            is GameViewIntent.ConfirmRestartWithCarry -> handleConfirmRestartWithCarry()
            is GameViewIntent.DismissCarrySelection -> updateState { copy(showCarrySelection = false) }

            is GameViewIntent.GoBackToMenu -> {
                levelLoad?.cancel()
                restartPicker?.cancel()
                pausePlay()
                session = null
                updateState {
                    copy(
                        gameStatus = GameStatus.MENU, isLoading = false, showCarrySelection = false,
                        unlockedLevel = prefs.getUnlockedLevel()
                    )
                }
            }
        }
        if (isMove && currentState !== before) checkpoint()
    }

    fun openLevel(levelId: Int, carryJson: String?, resumeId: String? = null) {
        val run = if (resumeId == null) runs.open(levelId, carryJson) else runs.open(levelId, carryJson, resumeId)
        run?.let(::handleLoadLevel)
    }

    private fun elapsedMs() = currentState.elapsedAt(System.nanoTime())

    private fun stopClock() {
        updateState { copy(elapsedMs = elapsedAt(System.nanoTime()), activeSinceNanos = null) }
    }

    fun pausePlay() {
        foreground = false
        stopClock()
        checkpoint()
    }

    fun resumePlay() {
        foreground = true
        val active = session ?: return
        if (!active.current || active.owner != (prefs.accountId() ?: prefs.getUserId())) {
            session = null
            updateState { copy(gameStatus = GameStatus.MENU) }
            setEffect(GameViewEffect.LevelLoadFailed(resumeUnavailable()))
            return
        }
        if (currentState.gameStatus == GameStatus.PLAYING && currentState.activeSinceNanos == null) {
            updateState { copy(activeSinceNanos = System.nanoTime()) }
        }
    }

    private fun checkpoint() {
        val active = session ?: return
        val state = currentState
        if (state.isLoading || hasSubmittedThisRun) return
        if (state.gameStatus != GameStatus.PLAYING && !(state.gameStatus == GameStatus.LOST && state.reviveCount > 0)) return
        saves.checkpoint(active, state.snapshot(historyStack, itemsUsedCount, elapsedMs())) {
            setEffect(GameViewEffect.ShowToast(getLocalizedString("本次进度保存失败", "Could not save this move",
                "本次進度儲存失敗", "進捗を保存できませんでした", "진행 상황을 저장하지 못했습니다")))
        }
    }

    private fun resumeUnavailable() = getLocalizedString(
        "此对局已结束或存档不可用", "This game has ended or its save is unavailable",
        "此對局已結束或存檔無法使用", "このゲームは終了したか、セーブを利用できません",
        "게임이 끝났거나 저장된 게임을 열 수 없습니다"
    )

    /** 初始化用户信息：读取隐私协议状态、昵称与已解锁关卡，并进入 MENU 或 INIT 状态。运行于主线程。 */
    private fun handleInitUser() {
        val accepted = prefs.isPrivacyAccepted()
        updateState {
            copy(
                isPrivacyAccepted = accepted,
                username = prefs.getUsername(),
                unlockedLevel = prefs.getUnlockedLevel(),
                isLoggedIn = prefs.isLoggedIn(),
                gameStatus = if (gameStatus == GameStatus.INIT || gameStatus == GameStatus.MENU) {
                    if (accepted) GameStatus.MENU else GameStatus.INIT
                } else gameStatus
            )
        }
    }

    private fun handleAgreePrivacy() {
        prefs.setPrivacyAccepted(true)
        updateState { copy(isPrivacyAccepted = true, gameStatus = GameStatus.MENU) }
        handleInitUser()
    }

    /**
     * 修改昵称并同步至服务端。
     * 线程边界：本地写入主线程完成，昵称上报网络请求运行在 [viewModelScope] 默认调度器（主线程 IO 挂起）。
     * 失败仅提示本地已修改，不阻塞流程。
     */
    private fun handleChangeUsername(newName: String) {
        if (newName.isBlank()) return
        prefs.setUsername(newName)
        updateState { copy(username = newName) }
        viewModelScope.launch {
            try {
                apiService.rename(RenameRequest(newName))
                setEffect(
                    GameViewEffect.ShowToast(
                        getLocalizedString(
                            "昵称修改成功",
                            "Nickname changed successfully",
                            "暱稱修改成功",
                            "ニックネーム変更完了",
                            "닉네임이 성공적으로 변경되었습니다"
                        )
                    )
                )
            } catch (e: Exception) {
                setEffect(
                    GameViewEffect.ShowToast(
                        getLocalizedString(
                            "昵称本地已修改",
                            "Nickname updated locally",
                            "暱稱本地已修改",
                            "ローカルで変更完了",
                            "닉네임이 로컬에서 변경됨"
                        )
                    )
                )
            }
        }
    }

    /**
     * 加载指定关卡数据并初始化本局状态（MVI Intent: [GameViewIntent.LoadLevel]）。
     *
     * 线程边界：
     * - 状态栏在前台自行刷新显示，存档与结算只累计前台游戏时间。
     * - Retrofit/Room 挂起调用不阻塞主线程；生成和遮挡计算在 [Dispatchers.Default] 完成。
     * - 服务端布局超过预算时沿用本局种子回退本地，棋盘可用后独立上传待同步记录。
     *
     * @param run 已保存的开局信息；明确重玩或进入下一关时使用新的开局 ID。
     */
    private fun handleLoadLevel(run: LevelRun) {
        levelLoad?.cancel()
        restartPicker?.cancel()
        session = null
        updateState { copy(isLoading = true, currentLevelId = run.level, showCarrySelection = false) }
        historyStack.clear()
        itemsUsedCount = 0
        val owner = prefs.accountId()
        val deviceId = prefs.getUserId()
        levelLoad = viewModelScope.launch {
            val opening = StageSaveSession(owner ?: deviceId, run.id, run.level)
            try {
                val restored = saves.restore(opening)
                if (restored != null) {
                    check(prefs.accountId() == owner)
                    install(opening, restored)
                    return@launch
                }
                if (run.requiresSnapshot) {
                    updateState { copy(isLoading = false, gameStatus = GameStatus.MENU) }
                    setEffect(GameViewEffect.LevelLoadFailed(resumeUnavailable()))
                    return@launch
                }
                val board = stageBoards.resolve(run.level, run.id, deviceId.toIntOrNull() ?: 0)
                val tiles = withContext(Dispatchers.Default) {
                    calculateBlockedStates(board.tiles.map { it.copy(state = TileState.NORMAL) })
                }
                val requested = try {
                    run.carryJson?.let { json.decodeFromString<Map<String, Int>>(it) }.orEmpty()
                } catch (_: IllegalArgumentException) {
                    emptyMap()
                }
                val snapshot = saves.begin(opening, onReplace = { previous, old ->
                    localDao.recordStageResult(PendingStageScore(
                        ownerId = opening.owner, runId = previous.id, levelId = previous.level,
                        score = 0, clearTimeMs = old.elapsedMs, itemsUsed = old.itemsUsed, won = false,
                        completedAt = System.currentTimeMillis(), abandoned = !old.awaitingRevive
                    ), true)
                }) {
                    check(prefs.accountId() == owner)
                    val allocated = localDao.consumeForRun(run.id, owner, requested, deviceId)
                    val carry = run.remaining?.let { remaining ->
                        allocated.mapValues { (type, count) -> minOf(count, remaining[type] ?: 0) }
                    } ?: allocated
                    localDao.enqueueStageStart(PendingStageStart(opening.owner, run.id, run.level, board.healthVersion, System.currentTimeMillis()))
                    check(prefs.accountId() == owner)
                    initialSnapshot(tiles, carry)
                }
                scoreDelegate.scheduleUpload(opening.owner, "start-${run.id}")
                check(prefs.accountId() == owner)
                install(opening, snapshot)
                if (board.offline) setEffect(GameViewEffect.ShowToast(getLocalizedString(
                    "网络连接失败，已切换至单机模式", "Offline mode active", "網絡連接失敗，已切換至單機模式",
                    "オフラインモード", "오프라인 모드"
                )))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                updateState { copy(isLoading = false, gameStatus = GameStatus.MENU) }
                setEffect(GameViewEffect.LevelLoadFailed(getLocalizedString(
                    "关卡加载失败，请返回后重试", "Couldn’t open this level. Please try again.",
                    "關卡載入失敗，請返回後重試", "ステージを読み込めませんでした。もう一度お試しください。",
                    "스테이지를 열지 못했습니다. 다시 시도해 주세요."
                )))
                return@launch
            }
            // Room already recorded this run's allocation. Network acknowledgement can finish independently.
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    syncRepository.syncDirtyData()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    com.apkfuns.logutils.LogUtils.w("道具同步延后重试: ${error.message}")
                }
            }
        }
    }

    private fun install(opening: StageSaveSession, snapshot: StageSnapshot) {
        session = opening
        runs.markInstalled()
        hasSubmittedThisRun = false
        itemsUsedCount = snapshot.itemsUsed
        historyStack.clear()
        historyStack.addAll(snapshot.history)
        updateState { restore(snapshot, opening.level, prefs.getCurrentSkin()) }
        if (foreground) resumePlay()
    }

    /**
     * 在同一次输入处理中完成三消和胜负判定。
     * 委托 [GameLogicDelegate.processSlotMatchAndCheckEndGame] 计算消除与门控解锁；
     * 胜利时计算通关结算积分（含双倍倍率与难度系数）并触发 [ScoreDelegate] 云端提交。
     * 游戏结束时冻结有效游戏时间。
     */
    private fun processSlotMatchAndCheckEndGame() {
        val state = currentState
        logicDelegate.processSlotMatchAndCheckEndGame(
            state,
            state.boardTiles,
            state.slotTiles,
            state.movedOutTiles,
            state.score,
            state.isDoublePointsActive,
            ::updateState,
            ::setEffect
        ) {
            // 计算通关结算积分（与 ScoreDelegate 同样公式）
            val elapsedMs = elapsedMs()
            val timeInSeconds = elapsedMs / 1000L
            val difficultyCoeff = when (currentState.currentLevelId) {
                1 -> 1; 2 -> 2; 3 -> 3; else -> 4
            }
            val baseScore = (maxOf(100L, (1000L - timeInSeconds * 2) - itemsUsedCount * 50L) * difficultyCoeff).toInt()
            val computedFinalScore = if (currentState.isDoublePointsActive) baseScore * 2 else baseScore
            updateState { copy(finalScore = computedFinalScore, elapsedMs = elapsedMs, activeSinceNanos = null) }

            submitResult(computedFinalScore, elapsedMs, won = true)
        }
        // 游戏结束时停止计时器
        val updatedState = currentState
        if (updatedState.gameStatus == GameStatus.WON || updatedState.gameStatus == GameStatus.LOST) {
            stopClock()
            if (updatedState.gameStatus == GameStatus.LOST && updatedState.reviveCount <= 0) {
                submitResult(0, elapsedMs(), won = false)
            }
        }
    }

    /** 复活逻辑：将卡槽前 3 张移入置物架并恢复 PLAYING；仅在 LOST 且仍有复活次数时生效。运行于主线程。 */
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
        if (foreground) resumePlay()
        setEffect(GameViewEffect.PlaySound(SoundType.CLICK))
    }

    private fun handleUseDoublePoints() {
        if (currentState.isDoublePointsActive || currentState.doublePointsCount <= 0) return
        updateState { copy(isDoublePointsActive = true, doublePointsCount = doublePointsCount - 1) }
        itemsUsedCount++
        setEffect(
            GameViewEffect.ShowToast(
                getLocalizedString(
                    "双倍积分已激活",
                    "Double points active",
                    "雙倍積分已激活",
                    "ダブルポイント有効",
                    "더블 포인트 활성화"
                )
            )
        )
    }

    /** 加载关卡排行榜：拉取本地/云端排行榜数据；失败降级为空列表并 Toast 提示。运行于 [viewModelScope]。 */
    private fun handleLoadLeaderboard(levelId: Int) {
        updateState { copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val res = apiService.getLeaderboard(levelId)
                updateState { copy(isLoading = false, rankings = res.rankings) }
            } catch (e: Exception) {
                updateState { copy(isLoading = false, rankings = emptyList()) }
                setEffect(
                    GameViewEffect.ShowToast(
                        getLocalizedString(
                            "加载排行榜失败",
                            "Load failed",
                            "加載排行榜失敗",
                            "読み込み失敗",
                            "로드 실패"
                        )
                    )
                )
            }
        }
    }

    /** 将当前棋盘/卡槽/置物架与封印进度快照压入撤销栈（[historyStack]），供 Undo 回退。深拷贝避免引用泄漏。 */
    private fun saveHistoryState() {
        val undo = currentState.undoCount
        if (undo <= 0) return
        while (historyStack.size >= undo) historyStack.removeAt(0)
        historyStack.add(
            GameHistoryState(
                boardTiles = currentState.boardTiles.map { it.copy() },
                slotTiles = currentState.slotTiles.map { it.copy() },
                movedOutTiles = currentState.movedOutTiles.map { it.copy() },
                sealedClearCount = currentState.sealedClearCount,
                sealedUnlockedIds = currentState.sealedUnlockedIds
            )
        )
    }

    private fun getLocalizedString(
        zh: String,
        en: String,
        tw: String,
        ja: String,
        ko: String
    ): String {
        return when (prefs.getLanguage()) {
            "en" -> en; "tw" -> tw; "ja" -> ja; "ko" -> ko; else -> zh
        }
    }

    /** 触发重开流程：在 [Dispatchers.IO] 读取背包库存并打开携带道具选择弹窗。运行于 [viewModelScope]。 */
    private fun handleTriggerRestartFlow() {
        restartPicker?.cancel()
        restartPicker = viewModelScope.launch {
            val list = localDao.getAllItems()
            val stocks = list.associate { it.itemType to it.count }
            updateState {
                copy(
                    backpackItemStocks = stocks,
                    tempCarryItems = emptyMap(),
                    showCarrySelection = true
                )
            }
        }
    }

    /** 调整临时携带道具数量（受库存上限与 0 下限约束）。运行于主线程。 */
    private fun handleUpdateTempCarryItem(itemType: String, change: Int) {
        val currentSelected = currentState.tempCarryItems[itemType] ?: 0
        val stock = currentState.backpackItemStocks[itemType] ?: 0
        val newSelected = (currentSelected + change).coerceIn(0, stock)

        val newMap = currentState.tempCarryItems.toMutableMap()
        if (newSelected > 0) {
            newMap[itemType] = newSelected
        } else {
            newMap.remove(itemType)
        }
        updateState {
            copy(tempCarryItems = newMap)
        }
    }

    /** 确认重开：将临时携带道具序列化为 JSON 并重新 [handleLoadLevel]。运行于主线程。 */
    private fun handleConfirmRestartWithCarry() {
        if (!currentState.showCarrySelection) return
        checkpoint()
        val carryJson = json.encodeToString<Map<String, Int>>(currentState.tempCarryItems)
        updateState {
            copy(showCarrySelection = false)
        }
        handleLoadLevel(runs.start(currentState.currentLevelId, carryJson))
    }

    private fun submitResult(score: Int, elapsed: Long, won: Boolean) {
        val active = session ?: return
        if (hasSubmittedThisRun || !active.current) return
        hasSubmittedThisRun = true
        val result = PendingStageScore(active.owner, active.id, active.level, score, elapsed,
            itemsUsedCount, won, System.currentTimeMillis())
        saves.settle(active, record = { scoreDelegate.record(result) }, onSaved = {
            scoreDelegate.upload(result, ::getLocalizedString, ::setEffect)
        }) { previous ->
            viewModelScope.launch {
                if (session !== active || !active.current || active.owner != (prefs.accountId() ?: prefs.getUserId())) return@launch
                if (previous != null) {
                    install(active, previous)
                    setEffect(GameViewEffect.ShowToast(getLocalizedString(
                        "成绩保存失败，已恢复上一步，可以继续游戏", "Result could not be saved. Continue from your previous move.",
                        "成績儲存失敗，已恢復上一步，可以繼續遊戲", "結果を保存できませんでした。直前の手から続けられます。",
                        "결과를 저장하지 못했습니다. 이전 수부터 이어서 플레이할 수 있습니다."
                    )))
                } else {
                    session = null
                    updateState { copy(gameStatus = GameStatus.MENU) }
                    setEffect(GameViewEffect.LevelLoadFailed(getLocalizedString(
                        "成绩保存失败，请返回首页重试续局", "Could not save the result. Return to the menu and retry your saved game.",
                        "成績儲存失敗，請返回首頁重試續局", "結果を保存できませんでした。メニューから再開してください。",
                        "결과를 저장하지 못했습니다. 메뉴에서 이어하기를 다시 시도해 주세요."
                    )))
                }
            }
        }
    }
}
