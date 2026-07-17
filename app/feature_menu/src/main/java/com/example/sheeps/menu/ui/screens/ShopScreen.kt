package com.example.sheeps.menu.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sheeps.data.model.ShopItem
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.menu.ui.components.ShopItemCard
import com.example.sheeps.ui.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput

/**
 * 道具商店界面。
 * 允许用户使用积分兑换游戏道具（如洗牌、撤销等）以及各种主题皮肤。
 * 
 * @param state 菜单界面状态
 * @param onLoginClick 点击登录按钮回调
 * @param onExchangeClick 确认兑换商品回调 (itemId, count)
 * @param onChangeSkin 切换当前应用皮肤回调
 */
@Composable
fun ShopScreen(
    state: MenuViewState,
    onLoginClick: () -> Unit,
    onExchangeClick: (Int, Int) -> Unit,
    onChangeSkin: (String) -> Unit
) {
    // 当前选中的二级 Tab：0 - 神奇道具, 1 - 角色/图标皮肤
    var selectedSubTab by remember { mutableIntStateOf(0) }

    // 根据选中的 Tab 过滤展示 childhood skins 商品
    val displayItems = remember(selectedSubTab, state.shopItems) {
        if (selectedSubTab == 0) {
            // 过滤出非皮肤类道具
            state.shopItems.filter { !it.item_type.startsWith("SKIN_") }
        } else {
            state.shopItems.filter { it.item_type.startsWith("SKIN_") }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // --- 顶部栏：标题与余额 ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.shop_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    text = stringResource(id = R.string.points_balance, state.points),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // --- 分类切换页签 ---
            TabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    text = { Text(stringResource(id = R.string.shop_item_magic), fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    text = { Text(stringResource(id = R.string.shop_item_skin), fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                )
            }

            // --- 商品列表展示 ---
            if (state.shopItems.isEmpty() && selectedSubTab == 0) {
                // 加载中状态
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                if (selectedSubTab == 0) {
                    // 道具类商品：两列网格展示
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(displayItems) { item ->
                            val backpackCount = state.backpackItems.find { it.item_type == item.item_type }?.count ?: 0

                            ShopItemCard(
                                item = item,
                                backpackCount = backpackCount,
                                currentSkin = state.currentSkin,
                                userPoints = state.points,
                                onExchange = { count ->
                                    onExchangeClick(item.id, count)
                                },
                                onApplySkin = { skin ->
                                    onChangeSkin(skin)
                                }
                            )
                        }
                    }
                } else {
                    // 皮肤类商品：按 group 主题系列分组纵向排列，每组内部横向滚动
                    // 分组头完全由服务端 ShopItem.group 动态驱动（多语言动态下发，方案 A）。
                    // 保留服务端返回顺序；未分组的皮肤归入稳定的「其他系列」兜底分组。
                    val groupedSkins = remember(displayItems) {
                        val ungroupedLabel = "其他系列"
                        val orderedHeaders = displayItems.mapNotNull { it.group }.distinct()
                        val groupsWithItems = orderedHeaders.mapNotNull { header ->
                            val itemsInGroup = displayItems.filter { it.group == header }
                            if (itemsInGroup.isEmpty()) null else (header to itemsInGroup)
                        }.toMutableList()
                        val ungrouped = displayItems.filter { it.group.isNullOrBlank() }
                        if (ungrouped.isNotEmpty()) {
                            groupsWithItems.add(ungroupedLabel to ungrouped)
                        }
                        groupsWithItems
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        groupedSkins.forEach { (header, itemsInGroup) ->
                            // 分组头：展示系列名
                            Text(
                                text = header,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                lazyItems(itemsInGroup) { item ->
                                    Box(modifier = Modifier.width(160.dp)) {
                                        val backpackCount = state.backpackItems.find { it.item_type == item.item_type }?.count ?: 0
                                        ShopItemCard(
                                            item = item,
                                            backpackCount = backpackCount,
                                            currentSkin = state.currentSkin,
                                            userPoints = state.points,
                                            onExchange = { count -> onExchangeClick(item.id, count) },
                                            onApplySkin = { skin -> onChangeSkin(skin) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 未登录状态的锁定蒙层（拦截所有触摸事件）---
        if (!state.isLoggedIn) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // 阻断穿透点击事件
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                    .padding(32.dp),
                contentAlignment = Alignment.Center // 1. 确保 Card 在 Box 中水平+垂直居中
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth() // 2. 【关键修正】让 Column 撑满 Card 的宽度，确保对齐基准线正确
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, // 3. 确保子组件水平居中
                        verticalArrangement = Arrangement.Center            // 4. 【关键修正】确保子组件在垂直方向也居中
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "锁定",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(id = R.string.shop_vault_locked),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Serif,
                            textAlign = TextAlign.Center // 5. 【关键修正】加上多行文字居中！防止字数多折行时默认左对齐
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(id = R.string.shop_login_prompt),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center, // 保持原有的文本居中
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onLoginClick,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.shop_btn_login_sync),
                                color = MaterialTheme.colorScheme.onPrimary,
                                textAlign = TextAlign.Center // 6. 【可选优化】确保按钮内的文字也绝对居中
                            )
                        }
                    }
                }
            }
        }
    }
}
