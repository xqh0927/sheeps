package com.example.sheeps.game.viewmodel

import com.example.sheeps.data.model.Tile
import com.example.sheeps.data.repository.StagePosition
import com.example.sheeps.data.repository.StageSnapshot
import com.example.sheeps.game.state.BoardBounds
import com.example.sheeps.game.state.GameStatus
import com.example.sheeps.game.state.GameViewState

internal fun initialSnapshot(tiles: List<Tile>, carry: Map<String, Int>): StageSnapshot {
    val board = tiles.map { it.copy(sealedCount = it.sealedCount.coerceIn(0, 1)) }
    return StageSnapshot(
        position = StagePosition(board, emptyList(), emptyList(), 0, emptySet()),
        history = emptyList(), carry = carry,
        sealOrder = board.filter { it.sealedCount > 0 }.sortedByDescending { it.z }.map { it.id },
        sealThreshold = 3, highlighted = emptySet(), doublePoints = false, score = 0,
        itemsUsed = 0, elapsedMs = 0, awaitingRevive = false
    )
}

internal fun GameViewState.snapshot(history: List<StagePosition>, itemsUsed: Int, elapsed: Long) =
    StageSnapshot(
        position = StagePosition(boardTiles, slotTiles, movedOutTiles, sealedClearCount, sealedUnlockedIds),
        history = history.toList(),
        carry = mapOf("UNDO" to undoCount, "MOVEOUT" to moveOutCount, "SHUFFLE" to shuffleCount,
            "REVIVE" to reviveCount, "HINT" to hintCount, "BOMB" to bombCount,
            "JOKER" to jokerCount, "DOUBLE_POINTS" to doublePointsCount),
        sealOrder = sealedOrder, sealThreshold = sealedUnlockThreshold, highlighted = highlightedTileIds,
        doublePoints = isDoublePointsActive, score = score, itemsUsed = itemsUsed, elapsedMs = elapsed,
        awaitingRevive = gameStatus == GameStatus.LOST
    )

internal fun GameViewState.restore(snapshot: StageSnapshot, level: Int, skin: String): GameViewState {
    val board = snapshot.position.boardTiles
    val bounds = if (board.isEmpty()) BoardBounds() else BoardBounds(
        board.minOf { it.x }, board.maxOf { it.x }, board.minOf { it.y }, board.maxOf { it.y }
    )
    return copy(
        isLoading = false, currentLevelId = level, currentSkin = skin,
        boardTiles = board, boardBounds = bounds, slotTiles = snapshot.position.slotTiles,
        movedOutTiles = snapshot.position.movedOutTiles, sealedClearCount = snapshot.position.sealedClearCount,
        sealedUnlockedIds = snapshot.position.sealedUnlockedIds, sealedOrder = snapshot.sealOrder,
        sealedUnlockThreshold = snapshot.sealThreshold, highlightedTileIds = snapshot.highlighted,
        undoCount = snapshot.carry["UNDO"] ?: 0, moveOutCount = snapshot.carry["MOVEOUT"] ?: 0,
        shuffleCount = snapshot.carry["SHUFFLE"] ?: 0, reviveCount = snapshot.carry["REVIVE"] ?: 0,
        hintCount = snapshot.carry["HINT"] ?: 0, bombCount = snapshot.carry["BOMB"] ?: 0,
        jokerCount = snapshot.carry["JOKER"] ?: 0, doublePointsCount = snapshot.carry["DOUBLE_POINTS"] ?: 0,
        isDoublePointsActive = snapshot.doublePoints, score = snapshot.score, elapsedMs = snapshot.elapsedMs,
        gameStatus = if (snapshot.awaitingRevive) GameStatus.LOST else GameStatus.PLAYING,
        shakingTileIds = emptySet(), showCarrySelection = false, levelStartTimestamp = 0, activeSinceNanos = null, finalScore = 0
    )
}
