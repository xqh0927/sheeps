package com.example.sheeps.game.viewmodel.delegates

import com.example.sheeps.core.game.GameEngine.calculateBlockedStates
import com.example.sheeps.core.game.GameEngine.getBlockingTiles
import com.example.sheeps.core.game.GameEngine.isTileBlocked
import com.example.sheeps.data.model.Tile
import com.example.sheeps.data.model.TileState
import com.example.sheeps.game.state.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 游戏核心逻辑委托类
 * 处理卡牌点击、匹配检测、输赢判定
 */
class GameLogicDelegate @Inject constructor() {

    /**
     * 处理卡牌点击
     */
    fun handleClickTile(
        scope: CoroutineScope,
        tile: Tile,
        state: GameViewState,
        onAddHistory: () -> Unit,
        updateState: (GameViewState.() -> GameViewState) -> Unit,
        setEffect: (GameViewEffect) -> Unit,
        processSlotMatch: suspend (List<Tile>, List<Tile>, List<Tile>) -> Unit
    ) {
        if (state.gameStatus != GameStatus.PLAYING) return

        // 判定卡牌是否被遮挡
        val isBlocked = tile.state == TileState.BLOCKED || (tile.state == TileState.NORMAL && isTileBlocked(tile, state.boardTiles))
        if (isBlocked) {
            val blockers = getBlockingTiles(tile, state.boardTiles)
            val minZ = blockers.minOfOrNull { it.z }
            val directBlockers = blockers.filter { it.z == minZ }
            val blockerIds = directBlockers.map { it.id }.toSet()
            if (blockerIds.isNotEmpty()) {
                updateState { copy(shakingTileIds = shakingTileIds + blockerIds) }
                setEffect(GameViewEffect.Vibrate)
                scope.launch {
                    delay(500)
                    updateState { copy(shakingTileIds = shakingTileIds - blockerIds) }
                }
            }
            return
        }

        if (tile.state != TileState.NORMAL && tile.state != TileState.MOVED_OUT) return

        onAddHistory()
        
        // 隐藏当前的高亮提示
        updateState { copy(highlightedTileIds = emptySet()) }

        // 同步执行：立即将牌标记为 IN_SLOT 并更新 StateFlow，
        // 避免协程内的状态更新晚于飞行动画结束，导致卡牌在原位闪现
        // newSlot 需要在外层声明，供协程内的 processSlotMatch 使用，
        // 否则协程闭包捕获的 state.slotTiles 是旧快照，会覆盖掉此处的正确状态
        var newSlot: List<Tile>? = null
        if (tile.state != TileState.MOVED_OUT && tile.sealedCount == 0) {
            tile.state = TileState.IN_SLOT
            newSlot = insertIntoSlot(state.slotTiles, tile)
            updateState {
                copy(
                    boardTiles = state.boardTiles,
                    slotTiles = newSlot!!
                )
            }
        }

        scope.launch {
            if (tile.state == TileState.MOVED_OUT) {
                // 处理从置物架点击
                val updatedBoard = state.boardTiles
                val updatedMovedOut = state.movedOutTiles.filter { it.id != tile.id }
                val newSlot = insertIntoSlot(state.slotTiles, tile.copy(state = TileState.IN_SLOT))

                setEffect(GameViewEffect.PlaySound(SoundType.CLICK))
                processSlotMatch(updatedBoard, newSlot, updatedMovedOut)
            } else {
                // 处理从棋盘点击
                if (tile.sealedCount > 0) {
                    // 解锁封印
                    tile.sealedCount--
                    setEffect(GameViewEffect.PlaySound(SoundType.UNSEAL))
                    setEffect(GameViewEffect.Vibrate)
                    updateState {
                        copy(boardTiles = calculateBlockedStates(state.boardTiles))
                    }
                } else {
                    // 正常移入槽位 — tile.state 与 updateState 已在协程外同步完成
                    setEffect(GameViewEffect.PlaySound(SoundType.CLICK))
                    processSlotMatch(state.boardTiles, newSlot ?: state.slotTiles, state.movedOutTiles)
                }
            }
        }
    }

    /**
     * 执行槽位匹配检测与输赢判定
     * 
     * @return 最终的得分增加值
     */
    suspend fun processSlotMatchAndCheckEndGame(
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

        // 不需要进行全局排序，以便卡槽中的卡牌保持顺序依次追加，并在插入相同卡牌后面后能够进行平滑移动动画
        // 对卡槽进行花色排序的废弃逻辑：finalSlot.sortBy { it.type }

        // 修复Bug #1：使用while循环，持续检查直到没有更多匹配
        var matched = true
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
                    scoreAdd += if (isDoublePoints) 200 else 100
                    matched = true
                    break  // 消除一组后重新检查
                }
            }
        }

        if (matched) {
            setEffect(GameViewEffect.PlaySound(SoundType.MATCH))
            // 每合并一个自动解锁一个棋盘上的盲盒牌
            val blindTile = finalBoard.firstOrNull { it.isBlind }
            if (blindTile != null) {
                blindTile.isBlind = false
            }
        }

        val remainingOnBoard = finalBoard.count { it.state == TileState.NORMAL || it.state == TileState.BLOCKED }
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
            copy(
                boardTiles = finalBoard,
                slotTiles = finalSlot,
                movedOutTiles = movedOut,
                score = totalScore,
                gameStatus = newStatus
            )
        }
        
        return scoreAdd
    }

    /**
     * 辅助函数：向卡槽中追加卡牌。
     * 如果卡槽中已存在相同图案的牌，则插入到最后一个相同牌的后面；
     * 否则，直接追加到卡槽尾部。
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
}
