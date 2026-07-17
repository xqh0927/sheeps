package com.example.sheeps.menu.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sheeps.ui.R
import com.example.sheeps.data.model.Notice
import com.example.sheeps.ui.theme.ShapeMedium
import com.example.sheeps.ui.theme.ShapeSmall
import kotlinx.coroutines.delay

/**
 * 系统公告轮播横幅（Ticker）。
 * 取最新两条公告，每 4 秒自动切换一次并带滑入/淡出转场；点击跳转公告列表页。
 *
 * @param notices 公告列表（[com.example.sheeps.data.model.Notice]），由调用方（Screen/ViewModel）提供。
 * @param onClick 点击横幅跳转到公告列表页的回调。
 *
 * 说明：
 * - 无状态（Stateless）组件，展示数据来自 [notices]，交互通过 [onClick] 上抛。
 * - 自动轮播由 `LaunchedEffect(latestTwo)` 内的 `while(true) { delay(4000) }` 协程驱动。
 *   ⚠️ 该协程**绑定于组合生命周期**：离开组合（Composable 退出屏幕/页面关闭）时会被自动取消，
 *   不会泄漏；`delay` 为主线程挂起操作，不阻塞 UI 线程。
 */
@Composable
fun AnnouncementsBanner(
    notices: List<Notice>,
    onClick: () -> Unit
) {
    val latestTwo = remember(notices) { notices.take(2) }
    var currentIndex by remember { mutableIntStateOf(0) }

    // ⚠️ 协程生命周期：该 LaunchedEffect 在 latestTwo 变化时重启；其内部 while(true) 无限循环随组合销毁自动取消，
    // 不会泄漏。delay(4000) 为主线程挂起，不阻塞 UI。
    LaunchedEffect(latestTwo) {
        if (latestTwo.size > 1) {
            while (true) {
                delay(4000)
                currentIndex = (currentIndex + 1) % latestTwo.size
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeMedium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
                shape = ShapeMedium
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "系统公告",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(id = R.string.notice_banner_title),
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Serif,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (notices.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.notice_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                AnimatedContent(
                    targetState = currentIndex,
                    transitionSpec = {
                        (slideInVertically { height -> height } + fadeIn()).togetherWith(
                            slideOutVertically { height -> -height } + fadeOut()
                        )
                    },
                    label = "noticeTicker"
                ) { index ->
                    val notice = latestTwo.getOrNull(index)
                    if (notice != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(ShapeSmall)
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .border(0.5.dp, MaterialTheme.colorScheme.outline, ShapeSmall)
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = notice.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = notice.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
