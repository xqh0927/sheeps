package com.example.sheeps.game.viewmodel.delegates

import com.apkfuns.logutils.LogUtils
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

/**
 * ��Ϸ�����߼�ί����
 * �����Ƶ����ƥ���⡢��Ӯ�ж�
 */
class GameLogicDelegate @Inject constructor() {

    /**
     * �����Ƶ��
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

        // �ж������Ƿ��ڵ���spacing=46���ص�>0.25px��Ϊ�ڵ���
        val isBlocked = tile.state == TileState.BLOCKED || isTileBlocked(tile, state.boardTiles)
        LogUtils.d(
            "BlockingDebug：点击 ${tile.id}: state=${tile.state}, isBlockedByEngine=${
                isTileBlocked(
                    tile,
                    state.boardTiles
                )
            }, 最终isBlocked=$isBlocked"
        )

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

        // ���ص�ǰ�ĸ�����ʾ
        updateState { copy(highlightedTileIds = emptySet()) }

        // ͬ��ִ�У��������Ʊ��Ϊ IN_SLOT ������ StateFlow��
        // ����Э���ڵ�״̬�������ڷ��ж������������¿�����ԭλ����
        // newSlot ��Ҫ�������������Э���ڵ� processSlotMatch ʹ�ã�
        // ����Э�̱հ������ state.slotTiles �Ǿɿ��գ��Ḳ�ǵ��˴�����ȷ״̬
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
                // ���������ܵ��
                val updatedBoard = state.boardTiles
                val updatedMovedOut = state.movedOutTiles.filter { it.id != tile.id }
                val newSlot = insertIntoSlot(state.slotTiles, tile.copy(state = TileState.IN_SLOT))

                setEffect(GameViewEffect.PlaySound(SoundType.CLICK))
                processSlotMatch(updatedBoard, newSlot, updatedMovedOut)
            } else {
                // ��������̵��
                if (tile.sealedCount > 0) {
                    // ������ӡ
                    tile.sealedCount--
                    setEffect(GameViewEffect.PlaySound(SoundType.UNSEAL))
                    setEffect(GameViewEffect.Vibrate)
                    updateState {
                        copy(boardTiles = calculateBlockedStates(state.boardTiles))
                    }
                } else {
                    // ���������λ �� tile.state �� updateState ����Э����ͬ�����
                    setEffect(GameViewEffect.PlaySound(SoundType.CLICK))
                    processSlotMatch(
                        state.boardTiles,
                        newSlot ?: state.slotTiles,
                        state.movedOutTiles
                    )
                }
            }
        }
    }

    /**
     * ִ�в�λƥ��������Ӯ�ж�
     * 
     * @return ���յĵ÷�����ֵ
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

        // ����Ҫ����ȫ�������Ա㿨���еĿ��Ʊ���˳������׷�ӣ����ڲ�����ͬ���ƺ�����ܹ�����ƽ���ƶ�����
        // �Կ��۽��л�ɫ����ķ����߼���finalSlot.sortBy { it.type }

        // �޸�Bug #1��ʹ��whileѭ�����������ֱ��û�и���ƥ��
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
                    break  // ����һ������¼��
                }
            }
        }

        if (matched) {
            setEffect(GameViewEffect.PlaySound(SoundType.MATCH))
            // ÿ�ϲ�һ���Զ�����һ�������ϵ�ä����
            val blindTile = finalBoard.firstOrNull { it.isBlind }
            if (blindTile != null) {
                blindTile.isBlind = false
            }
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
            // �ϲ����ԣ��������µ� state (this)��ֻ�������������ϵ� tile ���ڵ�״̬
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
     * �����������򿨲���׷�ӿ��ơ�
     * ����������Ѵ�����ͬͼ�����ƣ�����뵽���һ����ͬ�Ƶĺ��棻
     * ����ֱ��׷�ӵ�����β����
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
