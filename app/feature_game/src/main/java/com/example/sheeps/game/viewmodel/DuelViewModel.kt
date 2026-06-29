package com.example.sheeps.game.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.sheeps.core.base.BaseMviViewModel
import com.example.sheeps.core.multiplayer.WebSocketManager
import com.example.sheeps.core.multiplayer.model.*
import com.example.sheeps.core.preference.UserPreferences
import com.example.sheeps.data.local.LocalDao
import com.example.sheeps.data.model.*
import com.example.sheeps.data.network.ApiService
import com.example.sheeps.game.state.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max

@HiltViewModel
class DuelViewModel @Inject constructor(
    private val apiService: ApiService,
    private val prefs: UserPreferences,
    private val wsManager: WebSocketManager,
    private val json: Json,
    private val localDao: LocalDao
) : BaseMviViewModel<DuelViewState, DuelViewIntent, DuelViewEffect>(DuelViewState()) {

    private var initialTileCount = 0
    private var seqId = 0L

    init {
        viewModelScope.launch {
            wsManager.connectionState.collectLatest { state ->
                updateState { copy(connectionState = state) }
            }
        }

        viewModelScope.launch {
            wsManager.messageFlow.collect { command ->
                handleRemoteCommand(command)
            }
        }
    }

    override fun handleIntent(intent: DuelViewIntent) {
        when (intent) {
            is DuelViewIntent.Init -> handleInit(intent.gameId, intent.playerId)
            is DuelViewIntent.ClickTile -> handleClickTile(intent.tile)
            is DuelViewIntent.Restart -> handleRestart()
            is DuelViewIntent.Leave -> handleLeave()
        }
    }

    private fun handleInit(gameId: String, playerId: String) {
        updateState { copy(gameId = gameId, playerId = playerId, isLoading = true) }
        wsManager.connect(gameId, playerId)
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Multi-player duel usually uses a fixed or synchronized seed level
                // For simplicity, we use level 2 as base for duel
                val serverTiles = apiService.getLevel(2)
                val processedTiles = serverTiles.map { it.copy(state = TileState.NORMAL) }
                val finalTiles = calculateBlockedStates(processedTiles)
                initialTileCount = finalTiles.size

                updateState {
                    copy(
                        isLoading = false,
                        boardTiles = finalTiles,
                        gameStatus = GameStatus.PLAYING
                    )
                }
            } catch (e: Exception) {
                // Fallback to local generation if offline
                val localTiles = generateDuelLevel()
                val finalTiles = calculateBlockedStates(localTiles)
                initialTileCount = finalTiles.size
                updateState {
                    copy(
                        isLoading = false,
                        boardTiles = finalTiles,
                        gameStatus = GameStatus.PLAYING
                    )
                }
            }
        }
    }

    private fun handleClickTile(tile: Tile) {
        val state = currentState
        if (state.gameStatus != GameStatus.PLAYING) return
        if (tile.state != TileState.NORMAL && tile.state != TileState.MOVED_OUT) return
        if (tile.state == TileState.NORMAL && isTileBlocked(tile, state.boardTiles)) return

        viewModelScope.launch {
            if (tile.state == TileState.MOVED_OUT) {
                val updatedBoard = state.boardTiles
                val updatedMovedOut = state.movedOutTiles.filter { it.id != tile.id }
                val newSlot = state.slotTiles + tile.copy(state = TileState.IN_SLOT)

                setEffect(DuelViewEffect.PlaySound(SoundType.CLICK))
                processSlotMatch(updatedBoard, newSlot, updatedMovedOut)
            } else {
                if (tile.sealedCount > 0) {
                    tile.sealedCount--
                    setEffect(DuelViewEffect.PlaySound(SoundType.UNSEAL))
                    setEffect(DuelViewEffect.Vibrate)
                    updateState {
                        copy(boardTiles = calculateBlockedStates(state.boardTiles))
                    }
                } else {
                    tile.state = TileState.IN_SLOT
                    val updatedBoard = state.boardTiles
                    val newSlot = state.slotTiles + tile

                    setEffect(DuelViewEffect.PlaySound(SoundType.CLICK))
                    processSlotMatch(updatedBoard, newSlot, state.movedOutTiles)
                }
            }
        }
    }

    private suspend fun processSlotMatch(
        board: List<Tile>,
        slot: List<Tile>,
        movedOut: List<Tile>
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
                updateState { copy(score = score + 100, combo = combo + 1) }
                matched = true
                break
            }
        }

        if (matched) {
            setEffect(DuelViewEffect.PlaySound(SoundType.MATCH))
            sendEliminateCommand(eliminatedIds)
            
            // Attack logic: every 3 combo sends an attack
            if (currentState.combo > 0 && currentState.combo % 3 == 0) {
                sendAttackCommand()
            }
        } else if (slot.size > currentState.slotTiles.size) {
            // Added a tile but no match -> reset combo
            updateState { copy(combo = 0) }
        }

        val remainingTotal = finalBoard.count { it.state == TileState.NORMAL || it.state == TileState.BLOCKED } + movedOut.size
        
        if (remainingTotal == 0) {
            sendSystemEvent("WIN")
            updateState { copy(gameStatus = GameStatus.WON, winnerId = playerId) }
        } else if (finalSlot.size >= 7) {
            sendSystemEvent("LOST")
            updateState { copy(gameStatus = GameStatus.LOST) }
        }

        updateState {
            copy(
                boardTiles = finalBoard,
                slotTiles = finalSlot,
                movedOutTiles = movedOut
            )
        }
    }

    private fun handleRemoteCommand(command: GameCommand) {
        if (command.senderId == currentState.playerId) return
        
        when (command.type) {
            CommandType.ELIMINATE -> {
                // Update opponent progress
                val remaining = command.payload.comboCount // Reusing comboCount as a proxy for "remaining" if needed, or better define in payload
                // Actually let's assume tilesEliminated length/3 * 100 added to score
                updateState { 
                    copy(
                        opponentScore = opponentScore + 100,
                        opponentProgress = opponentProgress + 0.05f // Rough estimate
                    )
                }
            }
            CommandType.ATTACK -> {
                applyAttackEffect()
            }
            CommandType.SYSTEM_EVENT -> {
                if (command.payload.systemMessage == "WIN") {
                    updateState { 
                        copy(
                            gameStatus = GameStatus.LOST,
                            winnerId = command.senderId
                        )
                    }
                }
            }
            else -> {}
        }
    }

    private fun applyAttackEffect() {
        val state = currentState
        val normalTiles = state.boardTiles.filter { it.state == TileState.NORMAL }
        if (normalTiles.isNotEmpty()) {
            val targets = normalTiles.shuffled().take(2)
            val newBoard = state.boardTiles.map { tile ->
                if (targets.any { it.id == tile.id }) {
                    tile.copy(sealedCount = tile.sealedCount + 1)
                } else tile
            }
            updateState { 
                copy(
                    boardTiles = calculateBlockedStates(newBoard),
                    incomingAttackMessage = "受到对手干扰：两张卡牌被封印！"
                )
            }
            viewModelScope.launch {
                delay(3000)
                updateState { copy(incomingAttackMessage = null) }
            }
        }
    }

    private fun sendEliminateCommand(ids: List<String>) {
        wsManager.sendCommand(GameCommand(
            gameId = currentState.gameId,
            seqId = ++seqId,
            timestamp = System.currentTimeMillis(),
            senderId = currentState.playerId,
            type = CommandType.ELIMINATE,
            payload = CommandPayload(tilesEliminated = ids, comboCount = currentState.combo)
        ))
    }

    private fun sendAttackCommand() {
        wsManager.sendCommand(GameCommand(
            gameId = currentState.gameId,
            seqId = ++seqId,
            timestamp = System.currentTimeMillis(),
            senderId = currentState.playerId,
            type = CommandType.ATTACK,
            payload = CommandPayload(obstacleType = "SEALED")
        ))
        setEffect(DuelViewEffect.ShowToast("发动攻击！封印对手卡牌"))
    }

    private fun sendSystemEvent(msg: String) {
        wsManager.sendCommand(GameCommand(
            gameId = currentState.gameId,
            seqId = ++seqId,
            timestamp = System.currentTimeMillis(),
            senderId = currentState.playerId,
            type = CommandType.SYSTEM_EVENT,
            payload = CommandPayload(systemMessage = msg)
        ))
    }

    private fun handleRestart() {
        handleInit(currentState.gameId, currentState.playerId)
    }

    private fun handleLeave() {
        wsManager.disconnect()
    }

    // --- Helper logic (reused from GameViewModel) ---

    private fun isTileBlocked(tile: Tile, board: List<Tile>): Boolean {
        return board.any { other ->
            other.id != tile.id &&
            (other.state == TileState.NORMAL || other.state == TileState.BLOCKED) &&
            other.z > tile.z &&
            abs(other.x - tile.x) < 1.0f &&
            abs(other.y - tile.y) < 1.0f
        }
    }

    private fun calculateBlockedStates(board: List<Tile>): List<Tile> {
        return board.map { tile ->
            if (tile.state == TileState.NORMAL || tile.state == TileState.BLOCKED) {
                val blocked = isTileBlocked(tile, board)
                tile.copy(state = if (blocked) TileState.BLOCKED else TileState.NORMAL)
            } else {
                tile
            }
        }
    }

    private fun generateDuelLevel(): List<Tile> {
        // Same logic as GameViewModel's local generation
        val levelId = 2
        val numTypes = 6
        val coordinates = mutableListOf<Point3D>()
        val baseSize = 6
        for (z in 0 until 4) {
            val size = maxOf(3, baseSize - z / 3)
            val offset = if (z % 2 == 0) 0.0f else 0.5f
            for (r in 0 until size) {
                for (c in 0 until size) {
                    coordinates.add(Point3D(c + offset + 1.0f, r + offset + 1.0f, z))
                }
            }
        }
        val count = coordinates.size - (coordinates.size % 3)
        return coordinates.take(count).mapIndexed { index, coord ->
            Tile(
                id = "duel_tile_$index",
                type = (index % numTypes) + 1,
                x = coord.x,
                y = coord.y,
                z = coord.z
            )
        }
    }

    private data class Point3D(val x: Float, val y: Float, val z: Int)
}
