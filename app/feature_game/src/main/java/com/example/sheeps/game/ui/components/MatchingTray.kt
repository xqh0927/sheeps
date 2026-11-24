package com.example.sheeps.game.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sheeps.data.model.Tile
import com.example.sheeps.game.R

@Composable
internal fun MatchingTray(
    tiles: List<Tile>,
    currentSkin: String,
    flyingTileIds: Set<String>,
    slotGlobalPositions: MutableMap<Int, Offset>,
    capacity: Int = 7,
    action: (@Composable () -> Unit)? = null
) {
    val palette = MaterialTheme.colorScheme
    val accent by animateColorAsState(
        if (tiles.size >= capacity - 1) palette.error else palette.primary,
        label = "trayCapacity"
    )
    var previousSize by remember { mutableIntStateOf(tiles.size) }
    val countPulse = remember { Animatable(1f) }
    LaunchedEffect(tiles.size) {
        val changed = tiles.size != previousSize
        previousSize = tiles.size
        if (changed) countPulse.snapTo(1.18f)
        countPulse.animateTo(1f, tween(220))
    }
    Column(
        Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.match_tray), modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium, color = palette.onSurfaceVariant)
            Text("${tiles.size} / $capacity",
                modifier = Modifier.graphicsLayer { scaleX = countPulse.value; scaleY = countPulse.value },
                style = MaterialTheme.typography.labelMedium,
                color = accent, fontWeight = FontWeight.Bold)
            if (action != null) {
                Spacer(Modifier.width(10.dp))
                action()
            }
        }
        Spacer(Modifier.height(4.dp))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val gap = 4.dp
            val cellSize = (maxWidth - gap * 6) / 7
            repeat(7) { index ->
                val locked = index >= capacity
                val fill by animateColorAsState(
                    if (locked) palette.errorContainer else palette.surfaceContainerHigh,
                    label = "slotSurface"
                )
                Box(
                    Modifier.offset(x = (cellSize + gap) * index).size(cellSize)
                        .onGloballyPositioned { slotGlobalPositions[index] = it.positionOnScreen() }
                        .background(fill, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (locked) Icon(Icons.Default.Lock, null, Modifier.size(16.dp), palette.onErrorContainer)
                }
            }
            tiles.forEachIndexed { index, tile ->
                key(tile.id) {
                    val x by animateDpAsState((cellSize + gap) * index, spring(), label = "traySlide")
                    Box(Modifier.offset(x = x).size(cellSize).alpha(if (tile.id in flyingTileIds) 0f else 1f)) {
                        TileView(tile, onClick = {}, currentSkin = currentSkin, tileSize = cellSize)
                    }
                }
            }
        }
    }
}
