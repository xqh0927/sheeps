package com.example.sheeps.game.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.sheeps.core.base.BaseMviViewModel
import com.example.sheeps.core.game.GameEngine.calculateBlockedStates
import com.example.sheeps.core.preference.UserPreferences
import com.example.sheeps.data.local.BackpackItemEntity
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
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
    private var tickJob: kotlinx.coroutines.Job? = null

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
            is GameViewIntent.LoadLevel -> handleLoadLevel(intent.levelId, intent.carryItemsJson)
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
            is GameViewIntent.RestartLevel -> handleTriggerRestartFlow()
            is GameViewIntent.TriggerRestartFlow -> handleTriggerRestartFlow()
            is GameViewIntent.UpdateTempCarryItem -> handleUpdateTempCarryItem(
                intent.itemType,
                intent.change
            )

            is GameViewIntent.ConfirmRestartWithCarry -> handleConfirmRestartWithCarry()
            is GameViewIntent.DismissCarrySelection -> updateState { copy(showCarrySelection = false) }

            is GameViewIntent.GoBackToMenu -> updateState {
                copy(
                    gameStatus = GameStatus.MENU,
                    unlockedLevel = prefs.getUnlockedLevel()
                )
            }
        }
    }

    /** 初始化用户信息：读取隐私协议状态、昵称与已解锁关卡，并进入 MENU 或 INIT 状态。运行于主线程。 */
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
     * - 道具库存扣减与关卡网络请求运行在 [Dispatchers.IO]（`apiService.getLevel` / `localDao`）；
     * - 离线回退时改由本地生成器生成可解关卡，避免阻塞 UI。
     *
     * @param levelId 目标关卡 ID
     * @param carryItemsJson 进入关卡前选择的携带道具 JSON（可空）
     */
    private fun handleLoadLevel(levelId: Int, carryItemsJson: String?) {
        updateState { copy(isLoading = true, currentLevelId = levelId) }
        historyStack.clear()
        itemsUsedCount = 0
        carryItemsJsonStr = carryItemsJson

        val carryMap = try {
            if (!carryItemsJson.isNullOrEmpty()) json.decodeFromString<Map<String, Int>>(
                carryItemsJson
            ) else emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }

        viewModelScope.launch(Dispatchers.IO) {
            // 扣除携带道具库存
            val now = System.currentTimeMillis()
            carryMap.forEach { (type, count) ->
                if (count > 0) {
                    val current = localDao.getAllItems().find { it.itemType == type }
                    localDao.insertItem(
                        BackpackItemEntity(
                            type,
                            maxOf(0, (current?.count ?: 0) - count),
                            true,
                            now
                        )
                    )
                }
            }
            syncRepository.syncDirtyData()

            try {
                // 每次进入关卡随机生成种子，保证同关卡不同局图案不同
                val gameSeed = (System.currentTimeMillis() % 1000000).toInt()
                val finalTiles = calculateBlockedStates(
                    apiService.getLevel(levelId, seed = gameSeed).map { it.copy(state = TileState.NORMAL) })

                updateBoardState(finalTiles, carryMap, false)
            } catch (e: Exception) {
                // 离线回退：使用当前真实用户ID和随机种子保持一致性
                val numericUserId = prefs.getUserId().toIntOrNull() ?: 0
                val finalTiles =
                    calculateBlockedStates(levelGenerator.generateSolvableLevelLocal(levelId, System.currentTimeMillis(), numericUserId))
                updateBoardState(finalTiles, carryMap, true)
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
        val sealedOrder = tiles
            .filter { it.sealedCount > 0 }
            .sortedByDescending { it.z }
            .map { it.id }

        updateState {
            copy(
                isLoading = false,
                boardTiles = tiles,
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

            scoreDelegate.submitScoreOnline(
                // ⚠️ 内存隐患：此处每次通关都新建一个「未绑定 viewModelScope」的 CoroutineScope(Dispatchers.IO)。
                // 该 Scope 没有持有者引用、也从未调用 cancel()，理论上需依赖 submitScoreOnline 内部
                // scope.launch 自行结束；若后续误在此处长期持有该 Scope，会造成协程泄漏。
                // 安全建议：改为复用 viewModelScope（VM 销毁时由框架统一取消），避免匿名 Scope 逃逸。
                CoroutineScope(Dispatchers.IO),
                currentState.currentLevelId,
                computedFinalScore,
                elapsedMs,
                ::getLocalizedString,
                ::setEffect
            )
        }
        // 游戏结束时停止计时器
        if (currentState.gameStatus == GameStatus.WON || currentState.gameStatus == GameStatus.LOST) {
            tickJob?.cancel()
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
        viewModelScope.launch(Dispatchers.IO) {
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
        val carryJson = json.encodeToString<Map<String, Int>>(currentState.tempCarryItems)
        updateState {
            copy(showCarrySelection = false)
        }
        handleLoadLevel(currentState.currentLevelId, carryJson)
    }
}
