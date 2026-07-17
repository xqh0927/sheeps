package com.example.sheeps.menu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sheeps.ui.R

/**
 * 离线（无网络）警告横幅。
 * 点击后跳转系统设置页（[android.provider.Settings.ACTION_SETTINGS]），引导用户恢复网络。
 *
 * 说明：
 * - 无状态展示型组件，不依赖 ViewModel，运行于**主线程**。
 * - ⚠️ 内存隐患提示：内部通过 [androidx.compose.ui.platform.LocalContext] 获取 Context，
 *   并在 `clickable` 回调中 `startActivity`。该 Context 通常为 Activity，但回调生命周期随组合（Composition）
 *   销毁而结束，不会长期持有引用，故**不会**造成 Activity 泄漏。注意不要在回调闭包中长期缓存 Context。
 * - 跳转失败（部分深度定制系统无 Settings 模块）已被 try/catch 吞掉，仅静默忽略。
 */
@Composable
fun OfflineWarnBanner() {
    val context = androidx.compose.ui.platform.LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .clickable {
                try {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // 忽略设置页面跳转失败的异常（可能在某些深度定制的系统上没有 Settings 模块）
                }
            }
            .padding(vertical = 10.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.no_net_warn),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}
