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
import com.example.sheeps.ui.R
import com.example.sheeps.core.utils.getLocalizedItemName
import com.example.sheeps.core.utils.getLocalizedSource
import com.example.sheeps.data.model.ExchangeRecord
import com.example.sheeps.data.model.PointRecord
import java.text.SimpleDateFormat
import java.util.*

/**
 * 积分历史记录对话框（基于 Material [AlertDialog]）。
 *
 * 以滚动列表（[LazyColumn]）展示用户累计的积分变动明细（签到、对局、兑换等来源），
 * 列表项由 [PointRecordItem] 渲染。点击"取消"或点外部即关闭（经由 [onDismiss]）。
 *
 * 触发来源：个人中心/积分页面（ProfileScreen）调用；本对话框为纯只读展示，
 * 进入后无写入操作，不回写 ViewModel。
 *
 * 线程约束：[history] 已在外部加载完成并整体传入，本对话框内部不发起任何
 * 网络/IO 请求；[onDismiss] 在主线程（UI 线程）回调。
 *
 * @param history 积分记录列表（[PointRecord]），为空时展示"暂无积分记录"。
 * @param onDismiss 关闭对话框的回调（取消按钮或点击外部触发）。
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
 * 兑换历史记录对话框（基于 Material [AlertDialog]）。
 *
 * 以滚动列表（[LazyColumn]）展示用户道具兑换明细，列表项由 [ExchangeRecordItem] 渲染。
 * 点击"取消"或点外部即关闭（经由 [onDismiss]）。
 *
 * 触发来源：商城/兑换记录页面（ProfileScreen）调用；纯只读展示，不回写 ViewModel。
 *
 * 线程约束：[history] 已在外部加载完成并整体传入，内部不发起网络/IO 请求；
 * [onDismiss] 在主线程（UI 线程）回调。
 *
 * @param history 兑换记录列表（[ExchangeRecord]），为空时展示"暂无兑换记录"。
 * @param onDismiss 关闭对话框的回调（取消按钮或点击外部触发）。
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

/** 单条积分记录列表项：左侧展示来源名称与格式化时间，右侧展示增减数量（IN 为绿色加号，其余为减号）。 */
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

/** 单条兑换记录列表项：左侧展示兑换描述（道具名+数量）与格式化时间，右侧展示消耗的积分。 */
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

/** 将时间戳（单位：毫秒）格式化为 "yyyy-MM-dd HH:mm" 字符串，使用默认 Locale。 */
private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
