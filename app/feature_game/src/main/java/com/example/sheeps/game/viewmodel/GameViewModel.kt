package com.example.sheeps.game.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.sheeps.lib_base.base.BaseMviViewModel
import com.example.sheeps.core.game.GameEngine.calculateBlockedStates
import com.example.sheeps.data.preference.UserPreferences
import com.example.sheeps.data.local.LocalDao
import com.example.sheeps.data.model.RenameRequest
import com.example.sheeps.data.model.Tile
import com.example.sheeps.data.model.TileState
import com.example.sheeps.data.network.ApiService
import com.example.sheeps.data.repository.SyncRepository
import com.example.sheeps.game.state.BoardBounds
import com.example.sheeps.game.state.GameStatus
import com.example.sheeps.game.state.GameViewEffect
import com.example.sheeps.game.state.GameHistoryState
import com.example.sheeps.game.state.GameViewIntent
import com.example.sheeps.game.state.GameViewState
import com.example.sheeps.game.state.SoundType
import com.example.sheeps.game.viewmodel.delegates.GameLogicDelegate
import com.example.sheeps.game.viewmodel.delegates.GameToolDelegate
import com.example.sheeps.game.viewmodel.delegates.ScoreDelegate
import com.example.sheeps.game.viewmodel.helpers.GameLevelGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
    private val levelGenerator: GameLevelGenerator,
    private val logicDelegate: GameLogicDelegate,
    private val toolDelegate: GameToolDelegate,
    private val scoreDelegate: ScoreDelegate
) : BaseMviViewModel<GameViewState, GameViewIntent, GameViewEffect>(GameViewState()) {

    private companion object {
        const val LEVEL_REQUEST_BUDGET_MS = 2_000L
    }

    private var levelStartTime: Long = 0
    private val runs = LevelRuns(savedState)
    private var levelLoad: Job? = null
    private var restartPicker: Job? = null
    private var itemsUsedCount: Int = 0
    private var tickJob: kotlinx.coroutines.Job? = null
    private var hasSubmittedThisRun: Boolean = false

    // 历史状态栈（用于撤销功能）
    private val historyStack = mutableListOf<GameHistoryState>()

    init {
        sendIntent(GameViewIntent.InitUser)
    }

    override fun handleIntent(intent: GameViewIntent) {
        when (intent) {
            is GameViewIntent.InitUser -> handleInitUser()
            is GameViewIntent.AgreePrivacy -> handleAgreePrivacy()
            is GameViewIntent.ChangeUsername -> handleChangeUsername(intent.newName)
            is GameViewIntent.LoadLevel -> if (!currentState.isLoading || currentState.currentLevelId != intent.levelId) {
                handleLoadLevel(runs.start(intent.levelId, intent.carryItemsJson))
            }
            is GameViewIntent.ClickTile -> viewModelScope.launch {
                logicDelegate.handleClickTile(
                    intent.tile,
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
                viewModelScope,
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
            is GameViewIntent.RestartLevel -> {
                submitFailureRecordIfNeeded()
                handleTriggerRestartFlow()
            }
            is GameViewIntent.TriggerRestartFlow -> {
                submitFailureRecordIfNeeded()
                handleTriggerRestartFlow()
            }
            is GameViewIntent.UpdateTempCarryItem -> handleUpdateTempCarryItem(
                intent.itemType,
                intent.change
            )

            is GameViewIntent.ConfirmRestartWithCarry -> handleConfirmRestartWithCarry()
            is GameViewIntent.DismissCarrySelection -> updateState { copy(showCarrySelection = false) }

            is GameViewIntent.GoBackToMenu -> {
                levelLoad?.cancel()
                restartPicker?.cancel()
                tickJob?.cancel()
                submitFailureRecordIfNeeded()
                updateState {
                    copy(
                        gameStatus = GameStatus.MENU, isLoading = false, showCarrySelection = false,
                        unlockedLevel = prefs.getUnlockedLevel()
                    )
                }
            }
        }
    }

    fun openLevel(levelId: Int, carryJson: String?) {
        runs.open(levelId, carryJson)?.let(::handleLoadLevel)
    }

    fun saveRemainingCarry() {
        val state = currentState
        if (state.isLoading || state.gameStatus == GameStatus.INIT || state.gameStatus == GameStatus.MENU) return
        runs.saveRemaining(mapOf(
            "UNDO" to state.undoCount, "MOVEOUT" to state.moveOutCount, "SHUFFLE" to state.shuffleCount,
            "REVIVE" to state.reviveCount, "HINT" to state.hintCount, "BOMB" to state.bombCount,
            "JOKER" to state.jokerCount, "DOUBLE_POINTS" to state.doublePointsCount,
        ))
    }

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
     * - 计时已解耦到 UI 本地（[com.example.sheeps.game.ui.components.GameStatusBar] 基于 `levelStartTimestamp` 自增）；
     *   [viewModelScope] 的计时协程仅作占位、不写全局状态，避免整树每秒重组；
     * - Retrofit/Room 挂起调用不阻塞主线程；生成和遮挡计算在 [Dispatchers.Default] 完成。
     * - 服务端布局超过预算时沿用本局种子回退本地，棋盘可用后独立上传待同步记录。
     *
     * @param run 已保存的开局信息；明确重玩或进入下一关时使用新的开局 ID。
     */
    private fun handleLoadLevel(run: LevelRun) {
        levelLoad?.cancel()
        restartPicker?.cancel()
        tickJob?.cancel()
        updateState { copy(isLoading = true, currentLevelId = run.level, showCarrySelection = false) }
        historyStack.clear()
        itemsUsedCount = 0
        // Stable after process recreation. Keep the LCG seed in the Worker's positive seed range.
        val seed = Math.floorMod(run.id.hashCode(), 1_000_000) + 1
        val owner = prefs.accountId()
        val deviceId = prefs.getUserId()
        levelLoad = viewModelScope.launch {
            try {
                val remote = try {
                    withTimeoutOrNull(LEVEL_REQUEST_BUDGET_MS) { apiService.getLevel(run.level, seed) }
                        ?.takeIf { it.isNotEmpty() }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
                val tiles = withContext(Dispatchers.Default) {
                    val layout = remote?.map { it.copy(state = TileState.NORMAL) }
                        ?: levelGenerator.generateSolvableLevelLocal(run.level, seed.toLong(), deviceId.toIntOrNull() ?: 0)
                    check(layout.isNotEmpty())
                    calculateBlockedStates(layout)
                }
                val requested = try {
                    run.carryJson?.let { json.decodeFromString<Map<String, Int>>(it) }.orEmpty()
                } catch (_: IllegalArgumentException) {
                    emptyMap()
                }
                check(prefs.accountId() == owner)
                val allocated = localDao.consumeForRun(run.id, owner, requested, deviceId)
                ensureActive()
                val carry = run.remaining?.let { remaining ->
                    allocated.mapValues { (type, count) -> minOf(count, remaining[type] ?: 0) }
                } ?: allocated
                updateBoardState(tiles, carry, remote == null)
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

    /**
     * 启动计时器：仅在棋盘数据加载完毕、状态切换为 PLAYING 后调用。
     * 记录关卡开始时间并写入一次 [GameViewState.levelStartTimestamp]（运行于 [viewModelScope] 主线程）；
     * 不再按秒更新 [GameViewState.elapsedMs]，避免整棵 GameScreen 每秒重组。
     * VM 销毁时由框架自动取消协程；先 cancel 旧 Job 防止并发计时器叠加。
     */
    private fun startTimer() {
        levelStartTime = System.currentTimeMillis()
        // 解耦计时：不再每秒写全局 elapsedMs（否则整棵 GameScreen 每秒重组、约 300 张 TileView 重绘 + 300 次 onGloballyPositioned）。
        // 仅写入一次关卡起始时间戳，供 GameStatusBar 基于本地计时器自增显示；结算时仍用 levelStartTime 计算最终 elapsedMs。
        updateState { copy(levelStartTimestamp = levelStartTime) }
        // 保留可被取消的占位协程，使游戏结束时 tickJob?.cancel() 生命周期语义与原有保持一致；不执行任何周期性状态写入。
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            awaitCancellation()
        }
    }

    /** 将加载得到的卡牌写入 State：计算棋盘边界 [BoardBounds]、封印解锁顺序，并切换至 PLAYING 状态。运行于主线程。 */
    private fun updateBoardState(
        tiles: List<Tile>,
        carryMap: Map<String, Int>,
        isOffline: Boolean
    ) {
        val sanitizedTiles = tiles.map { tile ->
            val t = tile.copy()
            if (t.sealedCount > 0) {
                t.sealedCount = 1
            }
            t
        }
        val bounds = if (sanitizedTiles.isEmpty()) {
            BoardBounds()
        } else {
            BoardBounds(
                minX = sanitizedTiles.minOf { it.x },
                maxX = sanitizedTiles.maxOf { it.x },
                minY = sanitizedTiles.minOf { it.y },
                maxY = sanitizedTiles.maxOf { it.y }
            )
        }
        val sealedOrder = sanitizedTiles
            .filter { it.sealedCount > 0 }
            .sortedByDescending { it.z }
            .map { it.id }

        hasSubmittedThisRun = false
        itemsUsedCount = 0

        updateState {
            copy(
                isLoading = false,
                boardTiles = sanitizedTiles,
                boardBounds = bounds,
                slotTiles = emptyList(),
                movedOutTiles = emptyList(),
                undoCount = carryMap["UNDO"] ?: 0,
                shuffleCount = carryMap["SHUFFLE"] ?: 0,
                moveOutCount = carryMap["MOVEOUT"] ?: 0,
                reviveCount = carryMap["REVIVE"] ?: 0,
                hintCount = carryMap["HINT"] ?: 0,
                bombCount = carryMap["BOMB"] ?: 0,
                jokerCount = carryMap["JOKER"] ?: 0,
                doublePointsCount = carryMap["DOUBLE_POINTS"] ?: 0,
                isDoublePointsActive = false,
                score = 0,
                gameStatus = GameStatus.PLAYING,
                sealedClearCount = 0,
                sealedUnlockThreshold = 3,
                sealedUnlockedIds = emptySet(),
                sealedOrder = sealedOrder,
                currentSkin = prefs.getCurrentSkin()
            )
        }
        if (isOffline) setEffect(
            GameViewEffect.ShowToast(
                getLocalizedString(
                    "网络连接失败，已切换至单机模式",
                    "Offline mode active",
                    "網絡連接失敗，已切換至單機模式",
                    "オフラインモード",
                    "오프라인 모드"
                )
            )
        )
        // 棋盘加载完成、状态已切换为 PLAYING，此时用户方可点击卡牌，才开始计时
        startTimer()
    }

    /**
     * 处理卡槽三消匹配与胜负判定（挂起函数）。
     * 委托 [GameLogicDelegate.processSlotMatchAndCheckEndGame] 计算消除与门控解锁；
     * 胜利时计算通关结算积分（含双倍倍率与难度系数）并触发 [ScoreDelegate] 云端提交。
     * 游戏结束（WON/LOST）时取消计时器 Job。
     */
    private suspend fun processSlotMatchAndCheckEndGame() {
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
            val elapsedMs = System.currentTimeMillis() - levelStartTime
            val timeInSeconds = elapsedMs / 1000L
            val difficultyCoeff = when (currentState.currentLevelId) {
                1 -> 1; 2 -> 2; 3 -> 3; else -> 4
            }
            val baseScore = (maxOf(100L, (1000L - timeInSeconds * 2) - itemsUsedCount * 50L) * difficultyCoeff).toInt()
            val computedFinalScore = if (currentState.isDoublePointsActive) baseScore * 2 else baseScore
            updateState { copy(finalScore = computedFinalScore, elapsedMs = elapsedMs) }

            hasSubmittedThisRun = true
            scoreDelegate.submitScoreOnline(
                CoroutineScope(Dispatchers.IO),
                currentState.currentLevelId,
                computedFinalScore,
                elapsedMs,
                itemsUsedCount,
                1, // isWin = 1
                ::getLocalizedString,
                ::setEffect
            )
        }
        // 游戏结束时停止计时器
        val updatedState = currentState
        if (updatedState.gameStatus == GameStatus.WON || updatedState.gameStatus == GameStatus.LOST) {
            tickJob?.cancel()
            if (updatedState.gameStatus == GameStatus.LOST && updatedState.reviveCount <= 0) {
                submitFailureRecordIfNeeded()
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
        viewModelScope.launch { setEffect(GameViewEffect.PlaySound(SoundType.CLICK)) }
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
        val carryJson = json.encodeToString<Map<String, Int>>(currentState.tempCarryItems)
        updateState {
            copy(showCarrySelection = false)
        }
        handleLoadLevel(runs.start(currentState.currentLevelId, carryJson))
    }

    private fun submitFailureRecordIfNeeded() {
        if (hasSubmittedThisRun || currentState.isLoading) return
        val status = currentState.gameStatus
        if (status != GameStatus.PLAYING && status != GameStatus.LOST) return
        hasSubmittedThisRun = true
        val elapsedMs = System.currentTimeMillis() - levelStartTime
        scoreDelegate.submitScoreOnline(
            CoroutineScope(Dispatchers.IO),
            currentState.currentLevelId,
            0,
            elapsedMs,
            itemsUsedCount,
            0, // isWin = 0
            ::getLocalizedString,
            ::setEffect
        )
    }
}
