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
 * 对决模式操作委派类
 * 处理玩家点击卡牌、匹配检测及施放法术逻辑
 */
class DuelActionDelegate @Inject constructor() {

    /**
     * 处理卡牌点击逻辑
     */
    fun handleClickTile(
        scope: CoroutineScope,
        tile: Tile,
        state: DuelViewState,
        updateState: (DuelViewState.() -> DuelViewState) -> Unit,
        setEffect: (DuelViewEffect) -> Unit,
        processSlotMatch: suspend (List<Tile>, List<Tile>, List<Tile>) -> Unit
    ) {
        if (state.gameStatus != GameStatus.PLAYING) return

        // 遮挡检测
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

        scope.launch {
            if (tile.state == TileState.MOVED_OUT) {
                // 从置物架点击
                val updatedBoard = state.boardTiles
                val updatedMovedOut = state.movedOutTiles.filter { it.id != tile.id }
                val newSlot = state.slotTiles + tile.copy(state = TileState.IN_SLOT)

                setEffect(DuelViewEffect.PlaySound(SoundType.CLICK))
                processSlotMatch(updatedBoard, newSlot, updatedMovedOut)
            } else {
                // 从棋盘点击
                if (tile.sealedCount > 0) {
                    // 解锁封印
                    tile.sealedCount--
                    setEffect(DuelViewEffect.PlaySound(SoundType.UNSEAL))
                    setEffect(DuelViewEffect.Vibrate)
                    updateState { copy(boardTiles = calculateBlockedStates(state.boardTiles)) }
                } else {
                    // 移入槽位
                    tile.state = TileState.IN_SLOT
                    val updatedBoard = state.boardTiles
                    val newSlot = state.slotTiles + tile

                    setEffect(DuelViewEffect.PlaySound(SoundType.CLICK))
                    processSlotMatch(updatedBoard, newSlot, state.movedOutTiles)
                }
            }
        }
    }

    /**
     * 执行槽位匹配
     */
    suspend fun processSlotMatch(
        state: DuelViewState,
        board: List<Tile>,
        slot: List<Tile>,
        movedOut: List<Tile>,
        updateState: (DuelViewState.() -> DuelViewState) -> Unit,
        setEffect: (DuelViewEffect) -> Unit,
        onMatchSuccess: (List<String>) -> Unit,
        onVictory: () -> Unit
    ) {
        val finalBoard = calculateBlockedStates(board)
        val finalSlot = slot.toMutableList()
        finalSlot.sortBy { it.type }

        val counts = finalSlot.groupBy { it.type }
        var matched = false
        var eliminatedIds = emptyList<String>()
        
        for ((type, items) in counts) {
            if (items.size >= 3) {
                eliminatedIds = items.take(3).map { it.id }
                var removedCount = 0
                finalSlot.removeAll {
                    if (it.type == type && removedCount < 3) {
                        removedCount++
                        true
                    } else false
                }
                updateState { 
                    copy(
                        score = score + 100, 
                        combo = combo + 1, 
                        currentEnergy = (currentEnergy + 1).coerceAtMost(10)
                    ) 
                }
                matched = true
                break
            }
        }

        if (matched) {
            setEffect(DuelViewEffect.PlaySound(SoundType.MATCH))
            onMatchSuccess(eliminatedIds)
        } else if (slot.size > state.slotTiles.size) {
            // 新增了牌但未消除，重置连击
            updateState { copy(combo = 0) }
        }

        val remainingTotal = finalBoard.count { it.state == TileState.NORMAL || it.state == TileState.BLOCKED } + movedOut.size
        
        val newStatus = when {
            remainingTotal == 0 -> {
                setEffect(DuelViewEffect.PlaySound(SoundType.WIN))
                onVictory()
                GameStatus.WON
            }
            finalSlot.size >= state.maxSlotSize -> {
                setEffect(DuelViewEffect.PlaySound(SoundType.LOSE))
                GameStatus.LOST
            }
            else -> GameStatus.PLAYING
        }

        updateState {
            copy(
                boardTiles = finalBoard,
                slotTiles = finalSlot,
                movedOutTiles = movedOut,
                gameStatus = newStatus,
                winnerId = if (newStatus == GameStatus.WON) playerId else winnerId
            )
        }
    }

    /**
     * 处理施放法术逻辑
     */
    fun handleCastSpell(
        state: DuelViewState,
        spellType: String,
        getLocalizedString: (String, String, String, String, String) -> String,
        updateState: (DuelViewState.() -> DuelViewState) -> Unit,
        setEffect: (DuelViewEffect) -> Unit,
        onCastSuccess: (Int) -> Unit
    ) {
        if (state.gameStatus != GameStatus.PLAYING) return
        
        // 校验是否可用
        if (state.usedSpells.contains(spellType)) {
            setEffect(DuelViewEffect.ShowToast(getLocalizedString("该大招已使用过！", "Spell already used!", "該大招已使用過！", "既に使用されています！", "이미 사용되었습니다!")))
            return
        }
        if (state.isSilenced) {
            setEffect(DuelViewEffect.ShowToast(getLocalizedString("禁魔状态中！", "Silenced!", "禁魔狀態中！", "サイレンス状態です！", "침묵 상태입니다!")))
            return
        }

        val cost = when (spellType) {
            "FOG" -> 3; "SHRINK" -> 6; "SEAL_ALL" -> 10; "SHUFFLE" -> 5; "SILENCE" -> 4; else -> 999
        }
        if (state.currentEnergy < cost) {
            setEffect(DuelViewEffect.ShowToast(getLocalizedString("能量不足！", "Insufficient energy!", "能量不足！", "エネルギー不足！", "게이지 부족!")))
            return
        }

        updateState { copy(currentEnergy = currentEnergy - cost, usedSpells = usedSpells + spellType) }
        onCastSuccess(cost)
        
        setEffect(DuelViewEffect.PlaySound(SoundType.MATCH))
        setEffect(DuelViewEffect.ShowToast(getLocalizedString("施法成功！", "Spell casted!", "施法成功！", "スキル発動！", "주문 성공!")))
    }
}
