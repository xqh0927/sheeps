package com.example.sheeps.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.heightIn
import com.example.sheeps.game.state.GameViewState
import com.example.sheeps.ui.components.ItemIcon
import com.example.sheeps.core.game.TileIconProvider
import androidx.compose.ui.res.stringResource
import com.example.sheeps.ui.R
import com.example.sheeps.core.utils.getLocalizedItemName

/**
 * 游戏道具栏组件
 * 包含：可使用的道具按钮行、已携带法宝展示区
 */
@Composable
fun GameTools(
    state: GameViewState,
    onUseMoveOut: () -> Unit,
    onUseUndo: () -> Unit,
    onUseShuffle: () -> Unit,
    onUseHint: () -> Unit,
    onUseBomb: () -> Unit,
    onUseJoker: () -> Unit,
    onUseDouble: () -> Unit
) {
    // 映射逻辑
    val toolActions = mapOf(
        "MOVEOUT" to onUseMoveOut,
        "UNDO" to onUseUndo,
        "SHUFFLE" to onUseShuffle,
        "HINT" to onUseHint,
        "BOMB" to onUseBomb,
        "JOKER" to onUseJoker,
        "DOUBLE_POINTS" to onUseDouble
    )

    // 直接调用已携带法宝区域，不再包含 ToolButtonRow
    CarriedItemsSection(state = state, onToolClick = { type -> toolActions[type]?.invoke() })
}

@Composable
private fun CarriedItemsSection(state: GameViewState, onToolClick: (String) -> Unit) {
    val carriedItems = listOf(
        "UNDO" to state.undoCount, "SHUFFLE" to state.shuffleCount,
        "MOVEOUT" to state.moveOutCount, "HINT" to state.hintCount,
        "BOMB" to state.bombCount, "JOKER" to state.jokerCount,
        "DOUBLE_POINTS" to state.doublePointsCount, "REVIVE" to state.reviveCount
    ).filter { it.second > 0 }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(
            stringResource(R.string.carried_magic_items),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (carriedItems.isEmpty()) {
            Text(
                stringResource(R.string.slot_not_carried),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        carriedItems.forEach { (type, count) ->
            Row(
                modifier = Modifier.gameControlSurface()
                    .clickable(enabled = type != "REVIVE") { onToolClick(type) }
                    .heightIn(min = 44.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                ItemIcon(itemType = type, imageUrl = TileIconProvider.getItemIconUrl(type), size = 28.dp)
                Column {
                    Text(getLocalizedItemName(type), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text("×$count", style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/** A small raised control, lit from above using the current theme's surfaces. */
@Composable
internal fun Modifier.gameControlSurface(): Modifier {
    val colors = MaterialTheme.colorScheme
    val fill = remember(colors) {
        Brush.verticalGradient(listOf(colors.surface, colors.surfaceContainerHigh))
    }
    return shadow(1.dp, CircleShape).clip(CircleShape).background(fill)
}
