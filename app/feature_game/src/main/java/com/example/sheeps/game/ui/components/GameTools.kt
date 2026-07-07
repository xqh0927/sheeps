package com.example.sheeps.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.example.sheeps.theme.Text_Disabled_Dark
import com.example.sheeps.theme.Text_Secondary_Dark
import com.example.sheeps.ui.components.ItemAnimationIcon
import androidx.compose.ui.res.stringResource
import com.example.sheeps.core.R
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
    val carriedItems = remember(state) {
        val all = listOf(
            "UNDO" to state.undoCount,
            "SHUFFLE" to state.shuffleCount,
            "MOVEOUT" to state.moveOutCount,
            "HINT" to state.hintCount,
            "BOMB" to state.bombCount,
            "JOKER" to state.jokerCount,
            "DOUBLE_POINTS" to state.doublePointsCount,
            "REVIVE" to state.reviveCount
        ).filter { it.second > 0 }

        val result = all.toMutableList()
        while (result.size < 5) result.add("" to 0)
        result
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .border(
                0.5.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.carried_magic_items),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            carriedItems.forEach { (type, count) ->
                CarriedItemIcon(
                    type = type,
                    count = count,
                    onClick = { if (type.isNotEmpty()) onToolClick(type) })
            }
        }
    }
}

@Composable
private fun CarriedItemIcon(
    type: String,
    count: Int,
    onClick: () -> Unit
) {
    val isActive = type.isNotEmpty() && type != "REVIVE"
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(enabled = isActive && count > 0) { if (isActive) onClick() }
    ) {
        // 图标部分
        if (count > 0) {
            ItemAnimationIcon(
                itemType = type,
                size = 36.dp,
                isGray = false
            )
            // 间距
            Spacer(Modifier.height(2.dp))
            // 显示名称
            Text(
                text = getLocalizedItemName(type),
                fontSize = 10.sp,
                color = Text_Secondary_Dark,
                fontWeight = FontWeight.Medium
            )
            // 显示数量
            Text(
                text = "x$count",
                fontSize = 9.sp,
                color = Text_Secondary_Dark.copy(alpha = 0.8f)
            )
        } else {
            // 空槽位样式
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(id = R.string.slot_empty), fontSize = 10.sp, color = Text_Disabled_Dark)
            }
            Spacer(Modifier.height(2.dp))
            Text(stringResource(id = R.string.slot_not_carried), fontSize = 10.sp, color = Text_Disabled_Dark)
        }
    }
}