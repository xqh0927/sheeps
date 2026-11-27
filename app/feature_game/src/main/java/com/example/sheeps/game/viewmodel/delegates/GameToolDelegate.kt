package com.example.sheeps.game.viewmodel.delegates

import com.example.sheeps.core.game.GameEngine.calculateBlockedStates
import com.example.sheeps.data.model.Tile
import com.example.sheeps.data.model.TileState
import com.example.sheeps.game.state.GameViewEffect
import com.example.sheeps.game.state.GameViewState
import com.example.sheeps.game.state.GameHistoryState
import com.example.sheeps.game.state.SoundType
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
                score = prev.score ?: scoreBeforeLegacyUndo(state, prev),
                highlightedTileIds = emptySet(),
                undoCount = state.undoCount - 1
            )
        }
        setEffect(GameViewEffect.PlaySound(SoundType.CLICK))
    }

    // Older snapshots persisted the score only for the live board. Normal triples
    // advanced the seal counter; legacy jokers removed a pair without advancing it.
    private fun scoreBeforeLegacyUndo(state: GameViewState, previous: GameHistoryState): Int {
        val matches = state.sealedClearCount / 3
        val undoneMatches = (matches - previous.sealedClearCount / 3).coerceAtLeast(0)
        val extraHundreds = (state.score / 100 - matches).coerceAtLeast(0)
        if (undoneMatches > 0) {
            val doubles = if (state.isDoublePointsActive) minOf(extraHundreds, undoneMatches) else 0
            return (state.score - 100 * (undoneMatches + doubles)).coerceAtLeast(0)
        }
        val removed = previous.slotTiles.filter { old -> state.slotTiles.none { it.id == old.id } }
        val removedPair = state.slotTiles.size == previous.slotTiles.size - 2 &&
            removed.size == 2 && removed[0].type == removed[1].type
        return if (extraHundreds > 0 && removedPair) state.score - 100 else state.score
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

        val available = state.boardTiles.filter { it.state == TileState.NORMAL && it.sealedCount == 0 } +
            state.movedOutTiles
        val slotCounts = state.slotTiles.groupingBy { it.type }.eachCount()
        val freeSlots = 7 - state.slotTiles.size
        val match = available.groupBy { it.type }.entries
            .sortedByDescending { slotCounts[it.key] ?: 0 }
            .firstNotNullOfOrNull { (type, tiles) ->
                val needed = 3 - (slotCounts[type] ?: 0)
                if (needed in 1..freeSlots && tiles.size >= needed) tiles.take(needed) else null
            }
        if (match == null) {
            setEffect(GameViewEffect.ShowToast(getLocalizedString(
                "当前没有可以立即消除的组合，提示次数已保留。",
                "No immediate match is available. Your hint was saved.",
                "目前沒有可立即消除的組合，提示次數已保留。",
                "今すぐ消せる組み合わせがありません。ヒントは消費されません。",
                "지금 맞출 수 있는 조합이 없습니다. 힌트는 소모되지 않았습니다."
            )))
            return
        }

        onToolUsed()
        updateState {
            copy(
                highlightedTileIds = match.map { it.id }.toSet(),
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
        processSlotMatch: () -> Unit
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
        
        processSlotMatch()
    }

    /**
     * 处理万能牌（太极牌）操作
     * 消除和胜负判定在同一次输入处理中完成。
     * 玩法：优先从置物架/棋盘凑齐与卡槽 2 张同型牌组成消除，再补入卡槽触发匹配。
     */
    fun handleUseJoker(
        state: GameViewState,
        getLocalizedString: (String, String, String, String, String) -> String,
        updateState: (GameViewState.() -> GameViewState) -> Unit,
        setEffect: (GameViewEffect) -> Unit,
        onToolUsed: () -> Unit,
        onAddHistory: () -> Unit,
        processSlotMatch: () -> Unit
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

        val shelfTile = state.movedOutTiles.firstOrNull { it.type == targetType }
        val boardTile = if (shelfTile == null) state.boardTiles.firstOrNull {
            it.type == targetType && (it.state == TileState.NORMAL || it.state == TileState.BLOCKED)
        } else null
        // If bombs removed the last copy, the joker itself supplies the missing face.
        val third = (shelfTile ?: boardTile ?: targetEntry.value.first().copy(id = "wildcard"))
            .copy(state = TileState.IN_SLOT, isBlind = false)
        onToolUsed()
        updateState {
            copy(
                jokerCount = jokerCount - 1,
                slotTiles = slotTiles + third,
                movedOutTiles = movedOutTiles.filterNot { it.id == shelfTile?.id },
                boardTiles = boardTiles.map { if (it.id == boardTile?.id) third else it },
                highlightedTileIds = emptySet()
            )
        }
        setEffect(GameViewEffect.ShowToast(getLocalizedString(
            "太极牌显灵！与卡槽卡牌凑成消除！",
            "Joker activated! Completed match and eliminated!",
            "太極牌顯靈！與卡槽卡牌湊成消除！",
            "ワイルドカード有効！一致させて消去しました！",
            "조커 활성화! 일치하도록 변환되어 제거되었습니다!"
        )))

        // 同步胜负判定与成绩。
        processSlotMatch()
    }
}
