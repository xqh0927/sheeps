package com.example.sheeps.menu.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sheeps.core.R
import com.example.sheeps.ui.components.SheepsTopAppBar
import com.example.sheeps.data.model.Notice
import com.example.sheeps.menu.ui.dialogs.NoticeDetailDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 公告列表页（全屏）。
 * 以列表形式展示系统公告（标题/类型/时间），点击条目弹出详情对话框。
 *
 * @param notices 公告列表 [com.example.sheeps.data.model.Notice]，由调用方（Screen/ViewModel）提供。
 * @param onBack 返回上一页回调。
 *
 * 说明：
 * - 无状态（Stateless）屏幕组件：数据来自 [notices]，交互（返回、查看详情）通过回调上抛，
 *   符合单向数据流；列表通过 `LazyColumn` 惰性布局，仅在**主线程**组合与滚动。
 * - 详情弹窗 [com.example.sheeps.menu.ui.dialogs.NoticeDetailDialog] 由本地 `remember` 状态控制显隐。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeListScreen(
    notices: List<Notice>,
    onBack: () -> Unit
) {
    var selectedNotice by remember { mutableStateOf<Notice?>(null) }

    Scaffold(
        topBar = {
            SheepsTopAppBar(
                title = stringResource(id = R.string.notice_list_title),
                onBack = onBack
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            if (notices.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.notice_empty),
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notices) { notice ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedNotice = notice }
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = notice.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val typeText = when (notice.type) {
                                        "SYSTEM" -> stringResource(id = R.string.notice_type_system)
                                        "ACTIVITY" -> stringResource(id = R.string.notice_type_activity)
                                        else -> stringResource(id = R.string.notice_type_general)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = typeText,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(notice.created_at)),
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedNotice != null) {
        NoticeDetailDialog(
            notice = selectedNotice!!,
            onDismiss = { selectedNotice = null }
        )
    }
}
