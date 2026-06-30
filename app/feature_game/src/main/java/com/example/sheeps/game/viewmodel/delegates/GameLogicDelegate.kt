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
 * 游戏核心逻辑委派类
 * 处理卡片点击、匹配检测、输赢判定
 */
class GameLogicDelegate @Inject constructor() {

    /**
     * 处理卡片点击
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
            val blockerIds = blockers.map { it.id }.toSet()
            if (blockerIds.isNotEmpty()) {
                updateState { copy(shakingTileIds = shakingTileIds + blockerIds) }
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

        scope.launch {
            if (tile.state == TileState.MOVED_OUT) {
                // 处理从置物架点击
                val updatedBoard = state.boardTiles
                val updatedMovedOut = state.movedOutTiles.filter { it.id != tile.id }
                val newSlot = state.slotTiles + tile.copy(state = TileState.IN_SLOT)

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
                    // 正常移入槽位
                    tile.state = TileState.IN_SLOT
                    val updatedBoard = state.boardTiles
                    val newSlot = state.slotTiles + tile

                    setEffect(GameViewEffect.PlaySound(SoundType.CLICK))
                    processSlotMatch(updatedBoard, newSlot, state.movedOutTiles)
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

        // 排序以方便匹配
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
                scoreAdd += 100
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
}
