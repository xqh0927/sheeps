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
     */
    fun handleClickTile(
        scope: CoroutineScope,
        tile: Tile,
        state: GameViewState,
        onAddHistory: () -> Unit,
        updateState: (GameViewState.() -> GameViewState) -> Unit,
        setEffect: (GameViewEffect) -> Unit,
        processSlotMatch: suspend () -> Unit
    ) {
        if (state.gameStatus != GameStatus.PLAYING) return

        // 判断卡牌是否被遮挡
        val isBlocked = tile.state == TileState.BLOCKED || isTileBlocked(tile, state.boardTiles)

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
            scope.launch {
                setEffect(GameViewEffect.PlaySound(SoundType.CLICK))
                processSlotMatch()
            }
        } else {
            // 从棋盘点击
            if (tile.sealedCount > 0) {
                // 解锁封印
                tile.sealedCount--
                // 碎冰连锁：若封印完全解除，相邻封印牌溅射 -1 层
                if (tile.sealedCount == 0) {
                    val shattered = mutableSetOf(tile.id)
                    shatterSealedNeighbors(tile, state.boardTiles, shattered)
                }
                setEffect(GameViewEffect.PlaySound(SoundType.UNSEAL))
                setEffect(GameViewEffect.Vibrate)
                updateState {
                    copy(boardTiles = calculateBlockedStates(state.boardTiles))
                }
            } else {
                // 普通棋盘牌落槽
                tile.isBlind = false
                tile.state = TileState.IN_SLOT
                val newSlot = insertIntoSlot(state.slotTiles, tile)
                updateState {
                    copy(
                        boardTiles = state.boardTiles,
                        slotTiles = newSlot
                    )
                }
                scope.launch {
                    setEffect(GameViewEffect.PlaySound(SoundType.CLICK))
                    delay(FLY_ANIMATION_DELAY_MS)
                    processSlotMatch()
                }
            }
        }
    }

    /**
     * 执行槽位匹配及输赢判定
     *
     * @return 本次所获得的得分增量值
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

        // 进行匹配消除检查，循环直到没有可以消除的为止
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
                    break
                }
            }
        }

        if (matched) {
            setEffect(GameViewEffect.PlaySound(SoundType.MATCH))
        }

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
                gameStatus = newStatus
            )
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
