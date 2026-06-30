package com.example.sheeps.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sheeps.game.state.GameViewState
import com.example.sheeps.theme.Gold_Primary
import com.example.sheeps.theme.Text_Disabled_Dark
import com.example.sheeps.theme.Text_Secondary_Dark
import com.example.sheeps.ui.components.ItemAnimationIcon
import com.example.sheeps.ui.components.ToolItemButton

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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. 顶部道具交互按钮行
        ToolButtonRow(
            state = state,
            onUseMoveOut = onUseMoveOut,
            onUseUndo = onUseUndo,
            onUseShuffle = onUseShuffle,
            onUseHint = onUseHint,
            onUseBomb = onUseBomb,
            onUseJoker = onUseJoker,
            onUseDouble = onUseDouble
        )

        // 2. 已携带法宝展示区域
        CarriedItemsSection(state = state)
    }
}

/**
 * 道具交互按钮行
 */
@Composable
private fun ToolButtonRow(
    state: GameViewState,
    onUseMoveOut: () -> Unit,
    onUseUndo: () -> Unit,
    onUseShuffle: () -> Unit,
    onUseHint: () -> Unit,
    onUseBomb: () -> Unit,
    onUseJoker: () -> Unit,
    onUseDouble: () -> Unit
) {
    val tools = listOf(
        Triple("移出", state.moveOutCount, onUseMoveOut to (state.moveOutCount > 0 && state.slotTiles.isNotEmpty())),
        Triple("撤销", state.undoCount,    onUseUndo   to (state.undoCount > 0)),
        Triple("洗牌", state.shuffleCount, onUseShuffle to (state.shuffleCount > 0)),
        Triple("提示", state.hintCount,    onUseHint   to (state.hintCount > 0)),
        Triple("炸弹", state.bombCount,    onUseBomb   to (state.bombCount > 0 && state.slotTiles.size >= 2)),
        Triple("万能", state.jokerCount,   onUseJoker  to (state.jokerCount > 0 && state.slotTiles.isNotEmpty())),
        Triple("双倍", state.doublePointsCount, onUseDouble to (state.doublePointsCount > 0 && !state.isDoublePointsActive))
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tools.forEach { (label, count, pair) ->
            val (onClick, enabled) = pair
            ToolItemButton(
                label   = label,
                count   = count,
                onClick = onClick,
                enabled = enabled
            )
        }
    }
}

/**
 * 已携带法宝展示区（展示当前关卡携带的所有法宝及其剩余次数）
 */
@Composable
private fun CarriedItemsSection(state: GameViewState) {
    val carriedItems = remember(state) {
        val allItemsList = listOf(
            "UNDO" to ("撤销符" to state.undoCount),
            "SHUFFLE" to ("洗牌咒" to state.shuffleCount),
            "MOVEOUT" to ("移出印" to state.moveOutCount),
            "REVIVE" to ("复活丹" to state.reviveCount),
            "HINT" to ("提示符" to state.hintCount),
            "BOMB" to ("爆裂弹" to state.bombCount),
            "JOKER" to ("万能牌" to state.jokerCount),
            "DOUBLE_POINTS" to ("双倍卡" to state.doublePointsCount)
        )

        // 过滤出已携带的（数量 > 0）
        val activeItems = allItemsList.filter { it.second.second > 0 }

        // 补齐到5个槽位以保持视觉平衡
        val result = activeItems.toMutableList()
        while (result.size < 5) {
            result.add("" to ("未选择" to 0))
        }
        result
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "— 已携带法宝 —",
            style = MaterialTheme.typography.labelSmall,
            color = Gold_Primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            carriedItems.forEach { (type, pair) ->
                val (name, count) = pair
                CarriedItemIcon(type = type, name = name, count = count)
            }
        }
    }
}

@Composable
private fun CarriedItemIcon(type: String, name: String, count: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (count > 0) {
            ItemAnimationIcon(
                itemType = type,
                size = 36.dp,
                isGray = false
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "$name x$count",
                fontSize = 10.sp,
                color = Text_Secondary_Dark,
                fontWeight = FontWeight.Medium
            )
        } else {
            // 空槽位展示
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "空",
                    fontSize = 10.sp,
                    color = Text_Disabled_Dark,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = "未选择",
                fontSize = 10.sp,
                color = Text_Disabled_Dark,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
