package com.example.sheeps.game.viewmodel.delegates

import com.example.sheeps.core.game.GameEngine.calculateBlockedStates
import com.example.sheeps.core.game.GameEngine.getBlockingTiles
import com.example.sheeps.core.game.GameEngine.isTileBlocked
import com.example.sheeps.data.model.Tile
import com.example.sheeps.data.model.TileState
import com.example.sheeps.game.state.GameStatus
import com.example.sheeps.game.state.GameViewEffect
import com.example.sheeps.game.state.GameViewState
import com.example.sheeps.game.state.SoundType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** 卡牌飞入卡槽的动画时长，与 GameAnimations.FlyTweenSpec (350ms) 保持一致 */
private const val FLY_ANIMATION_DELAY_MS = 360L

/**
 * 游戏核心逻辑委派类
 * 处理卡牌点击、匹配消除及输赢判断
 */
class GameLogicDelegate @Inject constructor() {

    /**
     * 处理卡牌点击逻辑
     * （闯关模式）线程边界：主流程在调用方主线程执行；被遮挡/封印时的「抖动反馈」通过 `scope.launch { delay(500) }` 在 [viewModelScope] 内延时复位 `shakingTileIds`，VM 销毁时该协程随 scope 取消，无泄漏风险。门控未解锁的封印牌在此拦截，避免产生无效 Undo 步骤。
     */
    /**
     * 处理卡牌点击事件（挂起函数）。
     * 主流程在调用方主线程执行，移除 thread switching 上下文切换以实现极致零延迟点击响应。
     */
    suspend fun handleClickTile(
        tile: Tile,
        state: GameViewState,
        onAddHistory: () -> Unit,
        updateState: (GameViewState.() -> GameViewState) -> Unit,
        setEffect: (GameViewEffect) -> Unit,
        processSlotMatch: suspend () -> Unit
    ) {
        if (state.gameStatus != GameStatus.PLAYING) return

        // 1. 判断卡牌是否被遮挡
        val isBlocked = tile.state == TileState.BLOCKED || isTileBlocked(tile, state.boardTiles)

        if (isBlocked) {
            val blockers = getBlockingTiles(tile, state.boardTiles)
            val minZ = blockers.minOfOrNull { it.z }
            val directBlockers = blockers.filter { it.z == minZ }
            val blockerIds = directBlockers.map { it.id }.toSet()
            if (blockerIds.isNotEmpty()) {
                updateState { copy(shakingTileIds = shakingTileIds + blockerIds) }
                setEffect(GameViewEffect.Vibrate)
                // 启动后台定时器清理抖动状态，防止阻塞主协程通道
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) {
                    delay(500)
                    updateState { copy(shakingTileIds = shakingTileIds - blockerIds) }
                }
            }
            return
        }

        // 2. 封印门控判定
        if (tile.sealedCount > 0 && tile.id !in state.sealedUnlockedIds) {
            val remaining = maxOf(
                1,
                state.sealedUnlockThreshold - (state.sealedClearCount % state.sealedUnlockThreshold)
            )
            setEffect(GameViewEffect.ShowToast("还需消除 $remaining 张正常卡牌"))
            setEffect(GameViewEffect.Vibrate)
            return
        }

        if (tile.state != TileState.NORMAL && tile.state != TileState.MOVED_OUT) return

        onAddHistory()
        // 清除当前的高亮提示
        updateState { copy(highlightedTileIds = emptySet()) }

        if (tile.state == TileState.MOVED_OUT) {
            // 从置物架移回卡槽
            val updatedMovedOut = state.movedOutTiles.filter { it.id != tile.id }
            val newSlot = insertIntoSlot(
                state.slotTiles,
                tile.copy(state = TileState.IN_SLOT, isBlind = false)
            )
            updateState {
                copy(
                    movedOutTiles = updatedMovedOut,
                    slotTiles = newSlot
                )
            }
            setEffect(GameViewEffect.PlaySound(SoundType.CLICK))
            delay(FLY_ANIMATION_DELAY_MS)
            processSlotMatch()
        } else {
            // 从棋盘点击
            if (tile.sealedCount > 0) {
                // 解锁封印
                val newSealedCount = tile.sealedCount - 1
                if (newSealedCount == 0) {
                    // 封印完全解除，应用碎冰连锁
                    val clickedTile = tile.copy(sealedCount = 0, isBlind = false, state = TileState.IN_SLOT)
                    val newSlot = insertIntoSlot(state.slotTiles, clickedTile)

                    val tempBoard = state.boardTiles.map { it.copy() }
                    tempBoard.find { it.id == tile.id }?.let { t ->
                        t.sealedCount = 0
                        t.state = TileState.IN_SLOT
                        val shattered = mutableSetOf(t.id)
                        shatterSealedNeighbors(t, tempBoard, shattered)
                    }
                    val newBoard = calculateBlockedStates(tempBoard)

                    updateState {
                        copy(
                            boardTiles = newBoard,
                            slotTiles = newSlot
                        )
                    }
                    setEffect(GameViewEffect.PlaySound(SoundType.CLICK))
                    delay(FLY_ANIMATION_DELAY_MS)
                    processSlotMatch()
                } else {
                    // 未完全解封，播放解封音效并更新层数
                    val tempBoard = state.boardTiles.map { t ->
                        if (t.id == tile.id) t.copy(sealedCount = newSealedCount) else t.copy()
                    }
                    val newBoard = calculateBlockedStates(tempBoard)
                    setEffect(GameViewEffect.PlaySound(SoundType.UNSEAL))
                    setEffect(GameViewEffect.Vibrate)
                    updateState {
                        copy(boardTiles = newBoard)
                    }
                }
            } else {
                // 普通棋盘牌落槽
                val clickedTile = tile.copy(isBlind = false, state = TileState.IN_SLOT)
                val newSlot = insertIntoSlot(state.slotTiles, clickedTile)
                val newBoard = state.boardTiles.map { t ->
                    if (t.id == tile.id) clickedTile else t.copy()
                }
                updateState {
                    copy(
                        boardTiles = newBoard,
                        slotTiles = newSlot
                    )
                }
                setEffect(GameViewEffect.PlaySound(SoundType.CLICK))
                delay(FLY_ANIMATION_DELAY_MS)
                processSlotMatch()
            }
        }
    }

    /**
     * 执行槽位匹配及输赢判定
     * （挂起函数，闯关模式核心）负责循环消除三同花、封印门控计数与跨阈值解锁（sealed-unlock-mechanism-design.md §6）、软锁兜底自动解锁，以及 WON/LOST/PLAYING 状态判定。纯不可变状态更新，规避引用泄漏。
     *
     * @return 本次所获得的得分增量值
     * （非双倍 100 / 双倍 200 每张）
     */
    suspend fun processSlotMatchAndCheckEndGame(
        state: GameViewState,
        board: List<Tile>,
        slot: List<Tile>,
        movedOut: List<Tile>,
        currentScore: Int,
        isDoublePoints: Boolean,
        updateState: (GameViewState.() -> GameViewState) -> Unit,
        setEffect: (GameViewEffect) -> Unit,
        onVictory: () -> Unit
    ): Int {
        val finalBoard = calculateBlockedStates(board)
        val finalSlot = slot.toMutableList()
        var scoreAdd = 0

        // 进行匹配消除检查，循环直到没有可以消除的为止
        var matched = true
        var totalRemoved = 0
        while (matched) {
            matched = false
            val counts = finalSlot.groupBy { it.type }

            for ((type, items) in counts) {
                if (items.size >= 3) {
                    var removedCount = 0
                    finalSlot.removeAll {
                        if (it.type == type && removedCount < 3) {
                            removedCount++
                            true
                        } else false
                    }
                    totalRemoved += removedCount
                    scoreAdd += if (isDoublePoints) 200 else 100
                    matched = true
                    break
                }
            }
        }

        if (matched) {
            setEffect(GameViewEffect.PlaySound(SoundType.MATCH))
        }

        // ===== 封印门控：计数累加 + 跨阈值解锁（sealed-unlock-mechanism-design.md §6）=====
        val prevClear = state.sealedClearCount
        val prevUnlockedSize = state.sealedUnlockedIds.size
        val gateThreshold = state.sealedUnlockThreshold
        val sealOrder = state.sealedOrder
        val newClear = prevClear + totalRemoved
        val newUnlocked = state.sealedUnlockedIds.toMutableSet()
        while (newUnlocked.size < sealOrder.size
            && newClear >= (newUnlocked.size + 1) * gateThreshold
        ) {
            val nextUnlockId = sealOrder[newUnlocked.size]
            newUnlocked.add(nextUnlockId)

            // 直接解封：把该封印牌的 sealedCount 设为 0，使其恢复为普通牌并消除带锁图标
            finalBoard.find { it.id == nextUnlockId }?.let { unlockedTile ->
                if (unlockedTile.sealedCount > 0) {
                    unlockedTile.sealedCount = 0
                    val shattered = mutableSetOf(unlockedTile.id)
                    shatterSealedNeighbors(unlockedTile, finalBoard, shattered)
                }
            }
        }
        // 软锁兜底：盘面上已无可交互正常牌、置物架上无正常牌，且仍有未解锁封印牌 → 自动解锁剩余
        val hasClearableNormal = finalBoard.any { it.sealedCount == 0 && it.state == TileState.NORMAL } || movedOut.isNotEmpty()
        if (!hasClearableNormal && newUnlocked.size < sealOrder.size) {
            val toUnlockIds = sealOrder.drop(newUnlocked.size)
            newUnlocked.addAll(toUnlockIds)
            for (unlockId in toUnlockIds) {
                finalBoard.find { it.id == unlockId }?.let { unlockedTile ->
                    if (unlockedTile.sealedCount > 0) {
                        unlockedTile.sealedCount = 0
                        val shattered = mutableSetOf(unlockedTile.id)
                        shatterSealedNeighbors(unlockedTile, finalBoard, shattered)
                    }
                }
            }
        }
        val unlockedNow = newUnlocked.size - prevUnlockedSize

        val remainingOnBoard =
            finalBoard.count { it.state == TileState.NORMAL || it.state == TileState.BLOCKED }
        val remainingInMovedOut = movedOut.size

        val newStatus = when {
            remainingOnBoard == 0 && remainingInMovedOut == 0 -> {
                setEffect(GameViewEffect.PlaySound(SoundType.WIN))
                onVictory()
                GameStatus.WON
            }

            finalSlot.size >= 7 -> {
                setEffect(GameViewEffect.PlaySound(SoundType.LOSE))
                GameStatus.LOST
            }

            else -> GameStatus.PLAYING
        }

        val totalScore = currentScore + scoreAdd
        updateState {
            val mergedBoard = boardTiles.map { currentTile ->
                val recomputed = finalBoard.find { it.id == currentTile.id }
                if (recomputed != null &&
                    (currentTile.state == TileState.NORMAL || currentTile.state == TileState.BLOCKED) &&
                    recomputed.state != currentTile.state
                ) {
                    currentTile.copy(state = recomputed.state)
                } else {
                    currentTile
                }
            }
            copy(
                boardTiles = mergedBoard,
                slotTiles = finalSlot,
                movedOutTiles = movedOut,
                score = totalScore,
                gameStatus = newStatus,
                sealedClearCount = newClear,
                sealedUnlockedIds = newUnlocked
            )
        }

        if (unlockedNow > 0) {
            setEffect(GameViewEffect.PlaySound(SoundType.UNSEAL))
            setEffect(GameViewEffect.ShowToast("封印解锁！已解锁 $unlockedNow 张封印牌"))
        }

        return scoreAdd
    }

    /**
     * 插入棋子至托盘匹配槽。
     * 如果匹配槽已存在相同图案的棋子，排在该同图案棋子的最末尾；
     * 否则追加至末尾。
     */
    private fun insertIntoSlot(slot: List<Tile>, newTile: Tile): List<Tile> {
        val result = slot.toMutableList()
        val lastIndex = result.indexOfLast { it.type == newTile.type }
        return if (lastIndex == -1) {
            result.add(newTile)
            result
        } else {
            result.add(lastIndex + 1, newTile)
            result
        }
    }

    /**
     * 碎冰连锁：当一张卡牌的封印被完全解除时，相邻的同层封印牌也会受到溅射，封印层数 -1。
     * 若溅射后封印也归零，则继续连锁（递归）。
     *
     * @param centerTile 本次被解除封印的卡牌
     * @param board      当前棋盘所有卡牌
     * @param visited    已处理过的卡牌 id，防止循环
     */
    private fun shatterSealedNeighbors(centerTile: Tile, board: List<Tile>, visited: MutableSet<String>) {
        val neighbors = board.filter { other ->
            other.id != centerTile.id &&
            other.sealedCount > 0 &&
            other.id !in visited &&
            other.z == centerTile.z &&
            kotlin.math.abs(other.x - centerTile.x) <= 1.5f &&
            kotlin.math.abs(other.y - centerTile.y) <= 1.5f
        }

        for (neighbor in neighbors) {
            neighbor.sealedCount--
            visited.add(neighbor.id)
            if (neighbor.sealedCount == 0) {
                shatterSealedNeighbors(neighbor, board, visited)
            }
        }
    }
}
