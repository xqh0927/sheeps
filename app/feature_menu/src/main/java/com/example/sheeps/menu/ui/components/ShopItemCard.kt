package com.example.sheeps.menu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.sheeps.core.game.TileIconProvider
import com.example.sheeps.core.utils.getShopItemDesc
import com.example.sheeps.core.utils.getShopItemName
import com.example.sheeps.data.model.ShopItem
import com.example.sheeps.menu.R as MenuR
import com.example.sheeps.ui.R
import com.example.sheeps.ui.components.ItemIcon
import com.example.sheeps.ui.components.RemoteImage

@Composable
internal fun SkinFan(skinKey: String, modifier: Modifier = Modifier, tiles: List<String>? = null) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val tile = minOf(maxWidth * .43f, maxHeight * .64f, 88.dp)
        listOf(-1, 1, 0).forEach { position ->
            val index = when (position) { -1 -> 2; 1 -> 3; else -> 1 }
            Surface(
                modifier = Modifier.offset(x = tile * (.52f * position), y = if (position == 0) 2.dp else (-4).dp)
                    .rotate(position * 13f).size(tile),
                shape = RoundedCornerShape(11.dp), color = colors.surface,
                border = BorderStroke(1.dp, colors.outlineVariant), shadowElevation = 3.dp
            ) {
                RemoteImage(tiles?.getOrNull(index - 1) ?: TileIconProvider.getTileUrl(skinKey, index),
                    TileIconProvider.getFallbackResId(context, index), Modifier.fillMaxSize().padding(5.dp))
            }
        }
    }
}

@Composable
fun ShopItemCard(
    item: ShopItem,
    backpackCount: Int,
    currentSkin: String,
    userPoints: Int,
    onExchange: (Int) -> Unit,
    onApplySkin: (String) -> Unit,
    onLoginRequired: (() -> Unit)? = null,
    firstInGroup: Boolean = true,
    lastInGroup: Boolean = true
) {
    val colors = MaterialTheme.colorScheme
    val skin = item.item_type.startsWith("SKIN_")
    val skinKey = item.item_type.removePrefix("SKIN_").lowercase()
    val unlocked = skin && backpackCount > 0
    val applied = unlocked && currentSkin == skinKey
    val name = getShopItemName(item)
    val description = getShopItemDesc(item)
    var exchangeOpen by rememberSaveable(item.id, onLoginRequired == null) { mutableStateOf(false) }
    val action: @Composable () -> Unit = {
        val label = when {
            applied -> R.string.skin_applied
            unlocked -> R.string.skin_apply
            else -> R.string.btn_exchange
        }
        FilledTonalButton(
            onClick = {
                when {
                    onLoginRequired != null -> onLoginRequired()
                    unlocked -> onApplySkin(skinKey)
                    else -> exchangeOpen = true
                }
            },
            enabled = !applied, shape = CircleShape,
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer),
            contentPadding = PaddingValues(horizontal = 17.dp, vertical = 6.dp)
        ) { Text(stringResource(label), fontWeight = FontWeight.Bold) }
    }

    val top = if (skin || firstInGroup) 20.dp else 0.dp
    val bottom = if (skin || lastInGroup) 20.dp else 0.dp
    Surface(color = colors.surface, shape = RoundedCornerShape(top, top, bottom, bottom)) {
        Column(Modifier.fillMaxWidth()) {
            if (skin) {
                SkinFan(skinKey, Modifier.fillMaxWidth().height(124.dp).background(
                    Brush.linearGradient(listOf(colors.primary.copy(alpha = .16f), colors.secondary.copy(alpha = .06f)))), item.tiles)
            }
            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                if (!skin) {
                    Box(Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(colors.primary.copy(alpha = .07f)),
                        contentAlignment = Alignment.Center) {
                        ItemIcon(item.item_type, item.image_url, size = 52.dp)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    if (description.isNotBlank()) {
                        Text(description, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(if (unlocked) stringResource(R.string.skin_unlocked) else stringResource(R.string.points_suffix, item.points_price),
                                fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge, color = colors.onSurface)
                            if (!skin) Text(stringResource(R.string.backpack_stock, backpackCount),
                                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                        action()
                    }
                }
            }
            if (!skin && !lastInGroup) CatalogDivider(91.dp)
        }
    }

    if (exchangeOpen && onLoginRequired == null) {
        ExchangeDialog(item, skin, userPoints, onDismiss = { exchangeOpen = false }, onConfirm = {
            exchangeOpen = false
            onExchange(it)
        })
    }
}

@Composable
private fun ExchangeDialog(item: ShopItem, skin: Boolean, points: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var input by rememberSaveable(item.id) { mutableStateOf("1") }
    val affordable = if (item.points_price > 0) points / item.points_price else Int.MAX_VALUE
    val limit = minOf(if (skin) 1 else Int.MAX_VALUE, item.stock, affordable)
    val qty = input.toIntOrNull() ?: 0
    val valid = qty in 1..limit
    val colors = MaterialTheme.colorScheme
    CatalogSheet(getShopItemName(item), onDismiss) {
        Column(Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(if (skin) MenuR.string.catalog_shop_exchange_skin_desc else MenuR.string.catalog_shop_exchange_item_desc))
            if (!skin) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { input = (qty - 1).toString() }, enabled = qty > 1) {
                        Icon(Icons.Default.Remove, stringResource(MenuR.string.catalog_decrease))
                    }
                    OutlinedTextField(
                        value = input, onValueChange = { value ->
                            if (value.length <= 10 && value.all(Char::isDigit) && (value.isEmpty() || value.toIntOrNull() != null)) input = value
                        },
                        singleLine = true, modifier = Modifier.width(100.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center)
                    )
                    IconButton(onClick = { input = (qty + 1).toString() }, enabled = qty < limit) {
                        Icon(Icons.Default.Add, stringResource(MenuR.string.catalog_increase))
                    }
                }
            }
            Text(stringResource(MenuR.string.catalog_carry_stock_label, item.stock), style = MaterialTheme.typography.bodySmall)
            ReceiptLine(stringResource(MenuR.string.catalog_shop_unit_price_label), stringResource(R.string.points_suffix, item.points_price))
            ReceiptLine(stringResource(MenuR.string.catalog_shop_balance_label), stringResource(R.string.points_suffix, points))
            HorizontalDivider()
            ReceiptLine(stringResource(MenuR.string.catalog_shop_total_label), stringResource(R.string.points_suffix, item.points_price.toLong() * qty))
            if (qty > item.stock || qty > affordable) {
                Text(stringResource(if (qty > item.stock) MenuR.string.catalog_stock_low else MenuR.string.catalog_balance_low),
                    color = colors.error, style = MaterialTheme.typography.labelMedium)
            }
        }
        Button(onClick = { if (valid) onConfirm(qty) }, enabled = valid,
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp).heightIn(min = 48.dp), shape = CircleShape) {
            Text(stringResource(MenuR.string.catalog_shop_exchange_title))
        }
    }
}

@Composable
private fun ReceiptLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}
