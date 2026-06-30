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
import com.example.sheeps.core.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun ShopScreen(
    state: MenuViewState,
    onLoginClick: () -> Unit,
    onExchangeClick: (Int, Int) -> Unit,
    onChangeSkin: (String) -> Unit
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }

    val classicName = stringResource(id = R.string.item_skin_classic)
    val classicDesc = stringResource(id = R.string.item_skin_classic_desc)

    val displayItems = remember(selectedSubTab, state.shopItems, classicName, classicDesc) {
        if (selectedSubTab == 0) {
            state.shopItems.filter { !it.item_type.startsWith("SKIN_") }
        } else {
            val skins = mutableListOf<ShopItem>()
            // 默认经典皮肤
            skins.add(
                ShopItem(
                    id = -1,
                    name = classicName,
                    description = classicDesc,
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
                    text = stringResource(id = R.string.shop_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = CrimsonRed,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    text = stringResource(id = R.string.points_balance, state.points),
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
                    text = { Text(stringResource(id = R.string.shop_item_magic), fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    text = { Text(stringResource(id = R.string.shop_item_skin), fontSize = 14.sp, fontWeight = FontWeight.Bold) }
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
                if (selectedSubTab == 0) {
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
                    // 皮肤横向显示，一套的皮肤堆叠显示
                    val traditionalSkins = displayItems.filter {
                        it.item_type == "CLASSIC" || it.item_type == "SKIN_INK" || it.item_type == "SKIN_CYBER"
                    }
                    val provinceSkins = displayItems.filter {
                        it.item_type.startsWith("SKIN_") && it.item_type != "SKIN_INK" && it.item_type != "SKIN_CYBER"
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Section 1: Traditional Themes
                        Text(
                            text = stringResource(id = R.string.shop_section_traditional),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonRed,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            lazyItems(traditionalSkins) { item ->
                                Box(modifier = Modifier.width(160.dp)) {
                                    val backpackCount = if (item.item_type == "CLASSIC") 1 else {
                                        state.backpackItems.find { it.item_type == item.item_type }?.count ?: 0
                                    }
                                    ShopItemCard(
                                        item = item,
                                        backpackCount = backpackCount,
                                        currentSkin = state.currentSkin,
                                        onExchange = { count -> onExchangeClick(item.id, count) },
                                        onApplySkin = { skin -> onChangeSkin(skin) }
                                    )
                                }
                            }
                        }

                        // Section 2: Province Gourmet Skins
                        if (provinceSkins.isNotEmpty()) {
                            Text(
                                text = stringResource(id = R.string.shop_section_provinces),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CrimsonRed,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                lazyItems(provinceSkins) { item ->
                                    Box(modifier = Modifier.width(160.dp)) {
                                        val backpackCount = state.backpackItems.find { it.item_type == item.item_type }?.count ?: 0
                                        ShopItemCard(
                                            item = item,
                                            backpackCount = backpackCount,
                                            currentSkin = state.currentSkin,
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
                            text = stringResource(id = R.string.shop_vault_locked),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonRed,
                            fontFamily = FontFamily.Serif
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(id = R.string.shop_login_prompt),
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
                            Text(stringResource(id = R.string.shop_btn_login_sync), color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
