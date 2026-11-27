package com.example.sheeps.menu.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sheeps.core.utils.getLocalizedItemName
import com.example.sheeps.core.utils.getLocalizedSource
import com.example.sheeps.core.utils.getShopItemName
import com.example.sheeps.data.model.ExchangeRecord
import com.example.sheeps.data.model.PointRecord
import com.example.sheeps.data.model.ShopItem
import com.example.sheeps.menu.state.PersonalLoadState
import com.example.sheeps.menu.ui.components.PersonalSectionFeedback
import com.example.sheeps.menu.ui.components.CatalogDivider
import com.example.sheeps.menu.ui.components.CatalogSheet
import com.example.sheeps.ui.R
import java.text.DateFormat
import java.util.Date

@Composable
fun PointHistoryDialog(history: List<PointRecord>, shopItems: List<ShopItem>, status: PersonalLoadState?,
    onRetry: () -> Unit, onLogin: () -> Unit, onDismiss: () -> Unit) {
    val namesByType = rememberCatalogNames(shopItems)
    LedgerDialog(R.string.points_history_title, onDismiss) {
        PersonalSectionFeedback(status, history.isEmpty(), R.string.no_points_history, onRetry, onLogin)
        if (history.isNotEmpty()) LazyColumn(Modifier.fillMaxWidth()) {
            items(history) { record ->
                LedgerLine(getLocalizedSource(record.source, namesByType), record.created_at,
                    (if (record.type == "IN") "+" else "−") + record.amount)
            }
        }
    }
}

@Composable
fun ExchangeHistoryDialog(
    history: List<ExchangeRecord>,
    shopItems: List<ShopItem>,
    status: PersonalLoadState?,
    onRetry: () -> Unit,
    onLogin: () -> Unit,
    onDismiss: () -> Unit
) {
    val namesByType = rememberCatalogNames(shopItems)
    val itemsById = remember(shopItems) { shopItems.associateBy { it.id } }
    LedgerDialog(R.string.exchange_history_title, onDismiss) {
        PersonalSectionFeedback(status, history.isEmpty(), R.string.no_exchange_history, onRetry, onLogin)
        if (history.isNotEmpty()) LazyColumn(Modifier.fillMaxWidth()) {
            items(history) { record ->
                val item = itemsById[record.shop_item_id]
                val name = if (item != null && item.name.isNotBlank() &&
                    item.item_type.equals(record.item_type, ignoreCase = true)
                ) {
                    getShopItemName(item)
                } else {
                    getLocalizedItemName(record.item_type, namesByType)
                }
                LedgerLine(stringResource(R.string.exchange_record_desc, record.count, name),
                    record.created_at, "−${record.points_cost}")
            }
        }
    }
}

@Composable
private fun rememberCatalogNames(items: List<ShopItem>) = remember(items) {
    items.filter { it.name.isNotBlank() }.associate { it.item_type.uppercase() to it.name }
}

@Composable
private fun LedgerDialog(title: Int, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    CatalogSheet(stringResource(title), onDismiss) { content() }
}

@Composable
private fun LedgerLine(title: String, timestamp: Long, amount: String) {
    val locale = LocalConfiguration.current.locales[0]
    val date = remember(locale, timestamp) {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale).format(Date(timestamp))
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
            Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(amount, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
            color = if (amount.startsWith("+")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
    CatalogDivider(0.dp)
}
