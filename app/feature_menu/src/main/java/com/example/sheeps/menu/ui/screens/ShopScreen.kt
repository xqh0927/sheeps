package com.example.sheeps.menu.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
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
import com.example.sheeps.theme.CrimsonRed
import com.example.sheeps.theme.GoldenBronze

@Composable
fun ShopScreen(
    state: MenuViewState,
    onLoginClick: () -> Unit,
    onExchangeClick: (Int, Int) -> Unit,
    onChangeSkin: (String) -> Unit
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }

    val displayItems = remember(selectedSubTab, state.shopItems) {
        if (selectedSubTab == 0) {
            state.shopItems.filter { !it.item_type.startsWith("SKIN_") }
        } else {
            val skins = mutableListOf<ShopItem>()
            skins.add(
                ShopItem(
                    id = -1,
                    name = "经典国风 (卡牌皮肤)",
                    description = "默认解锁的古典朱红金边卡牌样式",
                    image_url = "",
                    item_type = "CLASSIC",
                    points_price = 0,
                    stock = 9999
                )
            )
            skins.addAll(state.shopItems.filter { it.item_type.startsWith("SKIN_") })
            skins
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "福禄聚宝阁",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = CrimsonRed,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    text = "积分: ${state.points}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CrimsonRed
                )
            }

            // 二级分类页签
            TabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = Color.Transparent,
                contentColor = CrimsonRed,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    text = { Text("法宝神器", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    text = { Text("奇门皮肤", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                )
            }

            if (state.shopItems.isEmpty() && selectedSubTab == 0) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CrimsonRed)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayItems) { item ->
                        val backpackCount = if (item.item_type == "CLASSIC") 1 else {
                            state.backpackItems.find { it.item_type == item.item_type }?.count ?: 0
                        }

                        ShopItemCard(
                            item = item,
                            backpackCount = backpackCount,
                            currentSkin = state.currentSkin,
                            onExchange = { count ->
                                onExchangeClick(item.id, count)
                            },
                            onApplySkin = { skin ->
                                onChangeSkin(skin)
                            }
                        )
                    }
                }
            }
        }

        // Locked mask if guest mode
        if (!state.isLoggedIn) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.85f))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF9)),
                    border = BorderStroke(1.dp, GoldenBronze),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "锁定",
                            tint = CrimsonRed,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "金库封锁",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonRed,
                            fontFamily = FontFamily.Serif
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "请先登录游戏账号，登录后可用积分在此阁内兑换破阵之法宝神器。",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onLoginClick,
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("立即登录", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
