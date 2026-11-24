package com.example.sheeps.game.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import com.example.sheeps.data.model.Tile

@Composable
fun EndlessDock(
    slotTiles: List<Tile>,
    currentSkin: String,
    flyingTileIds: Set<String> = emptySet(),
    slotGlobalPositions: MutableMap<Int, Offset> = remember { mutableMapOf() },
    action: (@Composable () -> Unit)? = null
) {
    MatchingTray(slotTiles, currentSkin, flyingTileIds, slotGlobalPositions, action = action)
}
