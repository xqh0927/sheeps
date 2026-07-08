package com.example.sheeps.game.viewmodel.delegates

import com.example.sheeps.core.game.GameEngine.calculateBlockedStates
import com.example.sheeps.data.model.Tile
import com.example.sheeps.data.model.TileState
import com.example.sheeps.game.state.GameViewEffect
import com.example.sheeps.game.state.GameViewState
import com.example.sheeps.game.state.GameHistoryState
import com.example.sheeps.game.state.GameStatus
import com.example.sheeps.game.state.SoundType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 游戏道具逻辑委派类
 * 处理撤销、移出、洗牌、提示、炸弹、万能牌、双倍积分等逻辑
 */
class GameToolDelegate @Inject constructor() {

    /**
     * 处理撤销操作
     */
    fun handleUseUndo(
        state: GameViewState,
        historyStack: MutableList<GameHistoryState>,
        updateState: (GameViewState.() -> GameViewState) -> Unit,
        setEffect: (GameViewEffect) -> Unit,
        onToolUsed: () -> Unit
    ) {
        if (state.undoCount <= 0 || historyStack.isEmpty()) return

        val prev = historyStack.removeAt(historyStack.lastIndex)
        onToolUsed()

        updateState {
            copy(
                boardTiles = calculateBlockedStates(prev.boardTiles),
                slotTiles = prev.slotTiles,
                movedOutTiles = prev.movedOutTiles,
                sealedClearCount = prev.sealedClearCount,
                sealedUnlockedIds = prev.sealedUnlockedIds,
                undoCount = state.undoCount - 1
            )
        }
        setEffect(GameViewEffect.PlaySound(SoundType.CLICK))
    }

    /**
     * 处理移出槽位操作
     */
    fun handleUseMoveOut(
        state: GameViewState,
        updateState: (GameViewState.() -> GameViewState) -> Unit,
        setEffect: (GameViewEffect) -> Unit,
        onToolUsed: () -> Unit
    ) {
        if (state.moveOutCount <= 0 || state.slotTiles.isEmpty()) return

        val toMove = state.slotTiles.take(3).map { it.copy(state = TileState.MOVED_OUT) }
        val remainingSlot = state.slotTiles.drop(3)
        val newMovedOut = state.movedOutTiles + toMove
        onToolUsed()

        updateState {
            copy(
                slotTiles = remainingSlot,
                movedOutTiles = newMovedOut,
                moveOutCount = state.moveOutCount - 1,
                boardTiles = calculateBlockedStates(state.boardTiles)
            )
        }
        setEffect(GameViewEffect.PlaySound(SoundType.CLICK))
    }

    /**
     * 处理洗牌操作
     */
    fun handleUseShuffle(
        state: GameViewState,
        updateState: (GameViewState.() -> GameViewState) -> Unit,
        setEffect: (GameViewEffect) -> Unit,
        onToolUsed: () -> Unit
    ) {
        if (state.shuffleCount <= 0) return

        val activeTiles = state.boardTiles.filter { it.state == TileState.NORMAL || it.state == TileState.BLOCKED }
        if (activeTiles.isEmpty()) return

        val shuffledTypes = activeTiles.map { it.type }.shuffled()
        onToolUsed()

        var idx = 0
        val newBoard = state.boardTiles.map { tile ->
            if (tile.state == TileState.NORMAL || tile.state == TileState.BLOCKED) {
                tile.copy(type = shuffledTypes[idx++])
            } else {
                tile
            }
        }

        updateState {
            copy(
                boardTiles = calculateBlockedStates(newBoard),
                shuffleCount = state.shuffleCount - 1
            )
        }
        setEffect(GameViewEffect.PlaySound(SoundType.CLICK))
    }

    /**
     * 处理提示操作
     */
    fun handleUseHint(
        state: GameViewState,
        getLocalizedString: (String, String, String, String, String) -> String,
        updateState: (GameViewState.() -> GameViewState) -> Unit,
        setEffect: (GameViewEffect) -> Unit,
        onToolUsed: () -> Unit
    ) {
        if (state.hintCount <= 0) return

        val active = state.boardTiles.filter { it.state == TileState.NORMAL || it.state == TileState.BLOCKED }
        if (active.isEmpty()) {
            setEffect(GameViewEffect.ShowToast(getLocalizedString("棋盘上已无成组之卡牌！", "No more matching groups on board!", "棋盤上已無成組之卡牌！", "ボード上に一致するグループはありません！", "보드에 일치하는 그룹이 없습니다!")))
            return
        }

        // 1. 优先从已有卡槽（slotTiles）中的牌寻找匹配
        var targetIds = emptySet<String>()
        val slotCounts = state.slotTiles.groupBy { it.type }.mapValues { it.value.size }
        val sortedSlotTypes = slotCounts.entries.sortedByDescending { it.value }

        for (entry in sortedSlotTypes) {
            val type = entry.key
            val needed = 3 - entry.value
            if (needed <= 0) continue

            // 寻找棋盘上该图案的卡牌
            val boardTilesOfType = active.filter { it.type == type }
            if (boardTilesOfType.size >= needed) {
                // 优先选择 NORMAL（未遮挡）的卡牌，其次选择 BLOCKED（遮挡）的
                val sortedBoardTiles = boardTilesOfType.sortedWith(compareBy {
                    if (it.state == TileState.NORMAL) 0 else 1
                })
                targetIds = sortedBoardTiles.take(needed).map { it.id }.toSet()
                break
            }
        }

        // 2. 如果卡槽中没有可匹配的牌，或者卡槽为空，则寻找棋盘上能凑成 3 张的组合
        if (targetIds.isEmpty()) {
            val groups = active.groupBy { it.type }.values.filter { it.size >= 3 }
            if (groups.isNotEmpty()) {
                // 评分机制：计算每组 3 张牌中，有多少张是处于 NORMAL (可直接点击) 状态的
                // 优先高亮最容易点击、被遮挡最少的组合
                val bestGroup = groups.maxByOrNull { group ->
                    val sorted = group.sortedWith(compareBy {
                        if (it.state == TileState.NORMAL) 0 else 1
                    }).take(3)
                    sorted.count { it.state == TileState.NORMAL }
                }
                if (bestGroup != null) {
                    val sortedGroup = bestGroup.sortedWith(compareBy {
                        if (it.state == TileState.NORMAL) 0 else 1
                    })
                    targetIds = sortedGroup.take(3).map { it.id }.toSet()
                }
            }
        }

        // 3. 兜底寻找：任意一组 3 张的卡牌
        if (targetIds.isEmpty()) {
            val fallbackGroup = active.groupBy { it.type }.values.find { it.size >= 3 }
            if (fallbackGroup != null) {
                targetIds = fallbackGroup.take(3).map { it.id }.toSet()
            }
        }

        if (targetIds.isEmpty()) {
            setEffect(GameViewEffect.ShowToast(getLocalizedString("棋盘上已无成组之卡牌！", "No more matching groups on board!", "棋盤上已無成組之卡牌！", "ボード上に一致するグループはありません！", "보드에 일치하는 그룹이 없습니다!")))
            return
        }

        onToolUsed()
        updateState {
            copy(
                highlightedTileIds = targetIds,
                hintCount = state.hintCount - 1
            )
        }
        setEffect(GameViewEffect.ShowToast(getLocalizedString("天眼符开启：已为您高亮出可消卡牌！", "Hint activated: matching group highlighted!", "天眼符開啟：已為您高亮出可消卡牌！", "ヒント有効：一致するグループをハイライトしました！", "힌트 활성화: 일치하는 그룹이 하이라이트되었습니다!")))
    }

    /**
     * 处理炸弹操作
     */
    fun handleUseBomb(
        state: GameViewState,
        getLocalizedString: (String, String, String, String, String) -> String,
        updateState: (GameViewState.() -> GameViewState) -> Unit,
        setEffect: (GameViewEffect) -> Unit,
        onToolUsed: () -> Unit,
        processSlotMatch: suspend () -> Unit
    ) {
        if (state.bombCount <= 0) return

        if (state.slotTiles.size < 2) {
            setEffect(GameViewEffect.ShowToast(getLocalizedString("卡槽内卡牌不足两张，无法使用雷震子！", "Need at least 2 cards in tray to use Bomb!", "卡槽內卡牌不足兩張，無法使用雷震子！", "爆弾を使用するにはトレイに少なくとも2枚のカードが必要です！", "폭탄을 사용하려면 트레이에 카드가 2장 이상 있어야 합니다!")))
            return
        }

        val newSlot = state.slotTiles.dropLast(2)
        onToolUsed()
        updateState {
            copy(
                slotTiles = newSlot,
                bombCount = state.bombCount - 1
            )
        }
        setEffect(GameViewEffect.Vibrate)
        setEffect(GameViewEffect.ShowToast(getLocalizedString("雷震子炸裂！直接销毁卡槽最后两张牌！", "Bomb exploded! Destroyed the last two cards!", "雷震子炸裂！直接銷毀卡槽最後兩張牌！", "爆弾爆発！最後の2枚のカードが破壊されました！", "폭탄 폭발! 마지막 카드 2장이 제거되었습니다!")))
        
        // 炸弹后需要重新触发匹配逻辑检查（虽然这里是销毁，但也可能触发生命周期）
    }

    /**
     * 处理万能牌（太极牌）操作
     */
    fun handleUseJoker(
        scope: CoroutineScope,
        state: GameViewState,
        getLocalizedString: (String, String, String, String, String) -> String,
        updateState: (GameViewState.() -> GameViewState) -> Unit,
        setEffect: (GameViewEffect) -> Unit,
        onToolUsed: () -> Unit,
        onAddHistory: () -> Unit,
        processSlotMatch: suspend () -> Unit
    ) {
        if (state.jokerCount <= 0) return

        // 1. 检查卡槽中是否存在两张相同的卡牌
        val counts = state.slotTiles.groupBy { it.type }
        val targetEntry = counts.entries.lastOrNull { it.value.size == 2 }
        if (targetEntry == null) {
            setEffect(GameViewEffect.ShowToast(getLocalizedString(
                "卡槽中没有两张相同的卡牌，无法使用太极牌！",
                "Needs two identical cards in tray to use Joker!",
                "卡槽中沒有兩張相同的卡牌，無法使用太極牌！",
                "トレイに同じカードが2枚必要です！",
                "조커를 사용하려면 트레이에 동일한 카드가 2장 있어야 합니다!"
            )))
            return
        }

        onAddHistory()
        val targetType = targetEntry.key

        // 2. 准备棋盘与置物架数据
        val newMovedOutTiles = state.movedOutTiles.toMutableList()
        val newBoardTiles = state.boardTiles.map { it.copy() }
        var eliminatedFromOutside = false

        // 优先从移出置物架（movedOutTiles）中寻找 1 张并移除
        val movedOutMatching = newMovedOutTiles.firstOrNull { it.type == targetType }
        if (movedOutMatching != null) {
            newMovedOutTiles.remove(movedOutMatching)
            eliminatedFromOutside = true
        }

        // 如果移出置物架中没有，则从棋盘中找一张（优先找 NORMAL/BLOCKED 状态的）
        if (!eliminatedFromOutside) {
            val boardMatching = newBoardTiles.firstOrNull {
                it.type == targetType && (it.state == TileState.NORMAL || it.state == TileState.BLOCKED)
            }
            if (boardMatching != null) {
                newBoardTiles.find { it.id == boardMatching.id }?.state = TileState.IN_SLOT
                eliminatedFromOutside = true
            }
        }

        // 3. 将卡槽中该图案的 2 张卡牌移除
        val newSlotTiles = state.slotTiles.filter { it.type != targetType }

        onToolUsed()

        // 4. 计算剩余遮挡状态并统计分数与状态
        val finalBoard = calculateBlockedStates(newBoardTiles)
        val remainingOnBoard = finalBoard.count { it.state == TileState.NORMAL || it.state == TileState.BLOCKED }
        val remainingInMovedOut = newMovedOutTiles.size

        val newStatus = when {
            remainingOnBoard == 0 && remainingInMovedOut == 0 -> GameStatus.WON
            newSlotTiles.size >= 7 -> GameStatus.LOST
            else -> GameStatus.PLAYING
        }

        val totalScore = state.score + 100

        updateState {
            copy(
                jokerCount = state.jokerCount - 1,
                slotTiles = newSlotTiles,
                movedOutTiles = newMovedOutTiles,
                boardTiles = finalBoard,
                score = totalScore,
                gameStatus = newStatus
            )
        }

        setEffect(GameViewEffect.PlaySound(SoundType.MATCH))
        setEffect(GameViewEffect.ShowToast(getLocalizedString(
            "太极牌显灵！与卡槽卡牌凑成消除！",
            "Joker activated! Completed match and eliminated!",
            "太極牌顯靈！與卡槽卡牌湊成消除！",
            "ワイルドカード有効！一致させて消去しました！",
            "조커 활성화! 일치하도록 변환되어 제거되었습니다!"
        )))

        // 5. 启动协程触发成绩提交、胜负状态同步
        scope.launch {
            processSlotMatch()
        }
    }
}
