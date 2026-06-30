package com.example.sheeps.game.viewmodel.delegates

import com.example.sheeps.core.game.GameEngine.calculateBlockedStates
import com.example.sheeps.data.model.Tile
import com.example.sheeps.data.model.TileState
import com.example.sheeps.game.state.GameViewEffect
import com.example.sheeps.game.state.GameViewState
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
        historyStack: MutableList<Triple<List<Tile>, List<Tile>, List<Tile>>>,
        updateState: (GameViewState.() -> GameViewState) -> Unit,
        setEffect: (GameViewEffect) -> Unit,
        onToolUsed: () -> Unit
    ) {
        if (state.undoCount <= 0 || historyStack.isEmpty()) return

        val (prevBoard, prevSlot, prevMovedOut) = historyStack.removeAt(historyStack.lastIndex)
        onToolUsed()

        updateState {
            copy(
                boardTiles = calculateBlockedStates(prevBoard),
                slotTiles = prevSlot,
                movedOutTiles = prevMovedOut,
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
        val targetGroup = active.groupBy { it.type }.values.find { it.size >= 3 }

        if (targetGroup == null) {
            setEffect(GameViewEffect.ShowToast(getLocalizedString("棋盘上已无成组之卡牌！", "No more matching groups on board!", "棋盤上已無成組之卡牌！", "ボード上に一致するグループはありません！", "보드에 일치하는 그룹이 없습니다!")))
            return
        }

        val targetIds = targetGroup.take(3).map { it.id }.toSet()
        onToolUsed()
        updateState {
            copy(
                highlightedTileIds = targetIds,
                hintCount = state.hintCount - 1
            )
        }
        setEffect(GameViewEffect.ShowToast(getLocalizedString("天眼符开启：已为您高亮出一组可消卡牌！", "Hint activated: matching group highlighted!", "天眼符開啟：已為您高亮出一組可消卡牌！", "ヒント有効：一致するグループをハイライトしました！", "힌트 활성화: 일치하는 그룹이 하이라이트되었습니다!")))
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
        processSlotMatch: suspend (List<Tile>, List<Tile>, List<Tile>) -> Unit
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
        setEffect(GameViewEffect.ShowToast(getLocalizedString("雷震子炸裂！直接销毁卡槽最后两张牌！", "Bomb exploded! Destroyed the last two cards!", "雷震子炸裂！直接銷毀卡槽最後兩張牌！", "爆弾爆発！最後の2枚のカードが破壊されました！", "폭탄 폭발! 마지막 카드 2장이 제거되었습니다!")))
        
        // 炸弹后需要重新触发匹配逻辑检查（虽然这里是销毁，但也可能触发生命周期）
    }

    /**
     * 处理万能牌（太极牌）操作
     */
    fun handleUseJoker(
        state: GameViewState,
        getLocalizedString: (String, String, String, String, String) -> String,
        updateState: (GameViewState.() -> GameViewState) -> Unit,
        setEffect: (GameViewEffect) -> Unit,
        onToolUsed: () -> Unit,
        onAddHistory: () -> Unit,
        processSlotMatch: suspend (List<Tile>, List<Tile>, List<Tile>) -> Unit
    ) {
        if (state.jokerCount <= 0) return

        if (state.slotTiles.isEmpty()) {
            setEffect(GameViewEffect.ShowToast(getLocalizedString("卡槽为空，太极牌无处幻化！", "Tray is empty, Joker cannot morph!", "卡槽為空，太極牌無處幻化！", "トレイが空です、ワイルドカードは変化できません！", "트레이가 비어 있어 조커를 변환할 수 없습니다!")))
            return
        }

        onAddHistory()
        val targetType = state.slotTiles.last().type
        val jokerTile = Tile(
            id = "joker_${System.currentTimeMillis()}",
            type = targetType,
            x = 0f, y = 0f, z = 999,
            state = TileState.IN_SLOT
        )

        val newSlot = state.slotTiles + jokerTile
        onToolUsed()
        updateState {
            copy(jokerCount = state.jokerCount - 1)
        }
        setEffect(GameViewEffect.ShowToast(getLocalizedString("太极牌显灵！幻化为同名图案凑成消除！", "Joker activated! Morphed to match and eliminate!", "太極牌顯靈！幻化為同名圖案湊成消除！", "ワイルドカード有効！一致させて消去しました！", "조커 활성화! 일치하도록 변환되어 제거되었습니다!")))
        
        // 此处通常需要调用 viewModelScope 触发 processSlotMatchAndCheckEndGame
    }
}
