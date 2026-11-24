package com.example.sheeps.menu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sheeps.core.utils.getLocalizedItemDesc
import com.example.sheeps.core.utils.getLocalizedItemName
import com.example.sheeps.data.model.ShopItem
import com.example.sheeps.data.model.UserItem
import com.example.sheeps.menu.R as MenuR
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.ui.R
import com.example.sheeps.ui.components.ItemIcon
import com.hjq.toast.Toaster

@Composable
fun BackpackCard(state: MenuViewState, shopItems: List<ShopItem>, onApplySkin: (String) -> Unit, onGoToPlay: () -> Unit) {
    if (!state.isLoggedIn) return
    val shopByType = remember(shopItems) { shopItems.associateBy { it.item_type } }
    val categories = remember(state.backpackItems) { state.backpackItems.groupBy { it.item_type.startsWith("SKIN_") } }
    var selectedType by rememberSaveable { mutableStateOf<String?>(null) }
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (categories.isEmpty()) {
            Text(stringResource(MenuR.string.catalog_backpack_empty), Modifier.padding(14.dp), color = colors.onSurfaceVariant)
        }
        listOf(false, true).forEach { skins ->
            val owned = categories[skins].orEmpty()
            if (owned.isNotEmpty()) {
                Text(stringResource(if (skins) R.string.backpack_skins_section else R.string.backpack_magic_section),
                    color = colors.onBackground, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 8.dp)) {
                    items(owned, key = { it.item_type }) { item ->
                        val shop = shopByType[item.item_type]
                        Surface(onClick = { selectedType = item.item_type }, modifier = Modifier.width(150.dp),
                            shape = RoundedCornerShape(18.dp), color = colors.surface) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                InventoryArt(item, shop, Modifier.fillMaxWidth().height(85.dp))
                                Text(shop?.name?.takeIf(String::isNotBlank) ?: getLocalizedItemName(item.item_type),
                                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text(stringResource(R.string.backpack_stock, item.count),
                                    style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
    state.backpackItems.firstOrNull { it.item_type == selectedType }?.let { item ->
        val shop = shopByType[item.item_type]
        val skin = item.item_type.startsWith("SKIN_")
        val skinKey = item.item_type.removePrefix("SKIN_").lowercase()
        val applied = skin && skinKey == state.currentSkin
        val context = LocalContext.current
        CatalogSheet(shop?.name?.takeIf(String::isNotBlank) ?: getLocalizedItemName(item.item_type),
            onDismiss = { selectedType = null }) {
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InventoryArt(item, shop, Modifier.fillMaxWidth().height(134.dp))
                Text(stringResource(R.string.backpack_stock, item.count), color = colors.primary, style = MaterialTheme.typography.labelLarge)
                Text(shop?.description?.takeIf(String::isNotBlank) ?: getLocalizedItemDesc(item.item_type, null))
            }
            Button(onClick = {
                selectedType = null
                if (skin) {
                    onApplySkin(skinKey)
                    Toaster.show(context.getString(R.string.skin_apply_success))
                } else onGoToPlay()
            }, enabled = !applied, modifier = Modifier.fillMaxWidth().padding(top = 14.dp).heightIn(min = 48.dp)) {
                Text(stringResource(when { applied -> R.string.skin_applied; skin -> R.string.skin_apply; else -> R.string.btn_go_to_play }))
            }
        }
    }
}

@Composable
private fun InventoryArt(item: UserItem, shop: ShopItem?, modifier: Modifier) {
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
        if (item.item_type.startsWith("SKIN_")) {
            SkinFan(item.item_type.removePrefix("SKIN_").lowercase(), Modifier.fillMaxSize(), shop?.tiles)
        } else {
            ItemIcon(item.item_type, shop?.image_url ?: item.image_url, size = 54.dp)
        }
    }
}
