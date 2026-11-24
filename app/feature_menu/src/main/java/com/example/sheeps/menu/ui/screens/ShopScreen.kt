package com.example.sheeps.menu.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sheeps.menu.R as MenuR
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.menu.ui.components.CatalogTitle
import com.example.sheeps.menu.ui.components.LocalMenuBottomInset
import com.example.sheeps.menu.ui.components.CatalogSwitch
import com.example.sheeps.menu.ui.components.CoinCanvas
import com.example.sheeps.menu.ui.components.ShopItemCard
import com.example.sheeps.menu.ui.components.SkinFan
import com.example.sheeps.ui.R

@Composable
fun ShopScreen(
    state: MenuViewState,
    onLoginClick: () -> Unit,
    onExchangeClick: (Int, Int) -> Unit,
    onChangeSkin: (String) -> Unit
) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val itemScroll = rememberLazyListState()
    val skinScroll = rememberLazyListState()
    val colors = MaterialTheme.colorScheme
    val equipment = remember(state.shopItems) { state.shopItems.filterNot { it.item_type.startsWith("SKIN_") } }
    val series = remember(state.shopItems) {
        state.shopItems.filter { it.item_type.startsWith("SKIN_") }
            .groupBy { it.group?.takeIf(String::isNotBlank) }
    }
    val loginGate = if (state.isLoggedIn) null else onLoginClick
    val owned = remember(state.backpackItems) { state.backpackItems.associate { it.item_type to it.count } }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            CatalogTitle(stringResource(R.string.shop_title), Modifier.weight(1f))
            Row(Modifier.background(colors.surface, RoundedCornerShape(24.dp))
                .padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                CoinCanvas()
                Text(stringResource(R.string.points_suffix, state.points), style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
        }

        CatalogSwitch(listOf(stringResource(R.string.shop_item_magic), stringResource(R.string.shop_item_skin)), selected) { selected = it }
        Crossfade(targetState = selected, modifier = Modifier.fillMaxSize(), label = "catalogSection") { page ->
            LazyColumn(state = if (page == 0) itemScroll else skinScroll, contentPadding = PaddingValues(top = 16.dp, bottom = LocalMenuBottomInset.current + 18.dp)) {
                if (!state.isLoggedIn) {
                    item {
                        Surface(modifier = Modifier.padding(bottom = 18.dp), shape = RoundedCornerShape(22.dp), color = colors.surface) {
                            Row(Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(stringResource(R.string.shop_login_prompt), color = colors.onSurface,
                                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Button(onLoginClick) { Text(stringResource(R.string.shop_btn_login_sync)) }
                                }
                                SkinFan(state.currentSkin, Modifier.width(104.dp).height(100.dp))
                            }
                        }
                    }
                }
                if (state.shopItems.isEmpty() && state.isShopLoading) {
                    item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                } else if ((page == 0 && equipment.isEmpty()) || (page == 1 && series.isEmpty())) {
                    item { Text(stringResource(MenuR.string.catalog_empty), color = colors.onSurfaceVariant, modifier = Modifier.padding(12.dp)) }
                } else if (page == 0) {
                    itemsIndexed(equipment, key = { _, item -> item.id }) { index, item ->
                        ShopItemCard(item, owned[item.item_type] ?: 0, state.currentSkin, state.points,
                            onExchange = { onExchangeClick(item.id, it) }, onApplySkin = onChangeSkin,
                            onLoginRequired = loginGate, firstInGroup = index == 0, lastInGroup = index == equipment.lastIndex)
                    }
                } else {
                    series.forEach { (group, skins) ->
                        item {
                            Text(group ?: stringResource(MenuR.string.catalog_other_series),
                                fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge,
                                color = colors.onBackground, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
                        }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
                                items(skins, key = { it.id }) { item ->
                                    Box(Modifier.width(228.dp)) {
                                        ShopItemCard(item, owned[item.item_type] ?: 0, state.currentSkin, state.points,
                                            onExchange = { onExchangeClick(item.id, it) }, onApplySkin = onChangeSkin,
                                            onLoginRequired = loginGate)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
