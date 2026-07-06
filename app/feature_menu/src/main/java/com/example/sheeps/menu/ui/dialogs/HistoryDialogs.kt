package com.example.sheeps.menu.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sheeps.core.R
import com.example.sheeps.core.utils.getLocalizedItemName
import com.example.sheeps.core.utils.getLocalizedSource
import com.example.sheeps.data.model.ExchangeRecord
import com.example.sheeps.data.model.PointRecord
import java.text.SimpleDateFormat
import java.util.*

/**
 * 积分历史记录对话框
 */
@Composable
fun PointHistoryDialog(
    history: List<PointRecord>,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = stringResource(id = R.string.points_history_title),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                if (history.isEmpty()) {
                    Text(stringResource(id = R.string.no_points_history))
                } else {
                    LazyColumn(modifier = Modifier.height(240.dp)) {
                        items(history) { record ->
                            PointRecordItem(record = record)
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(id = R.string.btn_cancel))
                }
            }
        )
    }
}

/**
 * 兑换历史记录对话框
 */
@Composable
fun ExchangeHistoryDialog(
    history: List<ExchangeRecord>,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = stringResource(id = R.string.exchange_history_title),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                if (history.isEmpty()) {
                    Text(stringResource(id = R.string.no_exchange_history))
                } else {
                    LazyColumn(modifier = Modifier.height(240.dp)) {
                        items(history) { record ->
                            ExchangeRecordItem(record = record)
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(id = R.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
private fun PointRecordItem(record: PointRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = getLocalizedSource(record.source),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formatTimestamp(record.created_at),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = if (record.type == "IN") "+${record.amount}" else "-${record.amount}",
            color = if (record.type == "IN") Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun ExchangeRecordItem(record: ExchangeRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(
                    id = R.string.exchange_record_desc,
                    record.count,
                    getLocalizedItemName(record.item_type)
                ),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formatTimestamp(record.created_at),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "-${record.points_cost}",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
