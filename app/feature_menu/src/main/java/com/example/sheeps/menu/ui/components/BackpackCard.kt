package com.example.sheeps.menu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sheeps.core.R
import com.example.sheeps.core.utils.getLocalizedItemName
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.theme.CrimsonRed

/**
 * 背包物品卡片组件
 * 横向展示用户拥有的道具及其数量
 *
 * @param state 界面状态数据，包含背包物品列表
 */
@Composable
fun BackpackCard(
    state: MenuViewState
) {
    if (!state.isLoggedIn || state.backpackItems.isEmpty()) return

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(id = R.string.backpack_title),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = CrimsonRed,
                modifier = Modifier.padding(bottom = 12.dp),
                fontFamily = FontFamily.Serif
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                state.backpackItems.forEach { item ->
                    BackpackItem(item = item)
                }
            }
        }
    }
}

/**
 * 单个背包物品条目
 */
@Composable
private fun BackpackItem(
    item: com.example.sheeps.data.model.UserItem
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(8.dp)
            .width(72.dp)
    ) {
        Text(
            text = getLocalizedItemName(item.item_type),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = CrimsonRed
        )
        Text(
            text = stringResource(id = R.string.backpack_stock, item.count),
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.DarkGray
        )
    }
}
