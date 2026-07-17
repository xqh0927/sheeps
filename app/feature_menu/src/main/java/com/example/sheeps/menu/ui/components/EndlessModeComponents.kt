package com.example.sheeps.menu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sheeps.ui.R
import com.example.sheeps.core.game.TileCardBase
import com.example.sheeps.core.game.TileIconProvider
import com.example.sheeps.ui.components.RemoteImage

/**
 * 首页"无尽生存"入口卡片。
 * 与 PvP 入口保持完全相同的样式（圆角、配色、边框、内边距），
 * 通过 [currentSkin] 实现皮肤感知的卡牌预览。
 *
 * 登录门控：未登录时点击仅触发 [onLoginClick]（由调用方跳登录并提示），登录后则打开介绍弹窗。
 *
 * @param currentSkin 当前生效的卡牌皮肤（来自 MenuViewState.currentSkin，天然响应式）
 * @param isLoggedIn  当前是否已登录（用于门控）
 * @param onOpenIntro 已登录时点击入口打开介绍弹窗的回调
 * @param onLoginClick 未登录时点击入口的回调（可空）
 * @param onShowLeaderboard 点击排行榜图标的回调（可空；传入后在箭头旁显示排行榜入口）
 * @param modifier    外部布局修饰（首页中以 fillMaxWidth 传入，撑满整行）
 */
@Composable
fun EndlessModeEntry(
    currentSkin: String,
    isLoggedIn: Boolean,
    onOpenIntro: () -> Unit,
    onLoginClick: (() -> Unit)? = null,
    onShowLeaderboard: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        modifier = modifier.clickable(onClick = {
            if (!isLoggedIn) {
                onLoginClick?.invoke()
            } else {
                onOpenIntro()
            }
        })
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：3 张皮肤感知小卡牌纵向堆叠预览（替代单张小图标，更有"叠塔"感）
            Column(
                modifier = Modifier.size(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
            ) {
                for (type in 1..3) {
                    TileCardBase(
                        skin = currentSkin,
                        modifier = Modifier.size(28.dp)
                    ) {
                        RemoteImage(
                            url = TileIconProvider.getTileUrl(currentSkin, type),
                            fallbackResId = TileIconProvider.getFallbackResId(LocalContext.current, type),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().padding(2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // 中间：标题 + 描述（与 PvP 卡片完全一致的排版）
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.home_endless_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Serif,
                    maxLines = 1
                )
                Text(
                    text = stringResource(id = R.string.home_endless_desc),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 2,
                    lineHeight = 14.sp
                )
            }

            // 右侧：排行榜图标（可选）+ 箭头
            if (onShowLeaderboard != null) {
                Icon(
                    imageVector = Icons.Default.Leaderboard,
                    contentDescription = stringResource(id = R.string.endless_leaderboard_entry),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .clickable { onShowLeaderboard() }
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * 无尽生存模式介绍弹窗。
 * 所有配色均走 MaterialTheme 令牌；展示一行实时（皮肤感知）的卡牌预览以演示动态皮肤。
 *
 * 登录门控：未登录时点击"开始游戏"仅触发 [onLoginClick]；登录后关闭弹窗并 [ onStart ]。
 * 排行榜入口已迁移至首页 [EndlessModeEntry] 卡片右侧，此处不再提供。
 *
 * @param currentSkin 当前生效的卡牌皮肤
 * @param isLoggedIn  当前是否已登录（用于门控"开始游戏"）
 * @param onDismiss   关闭弹窗的回调
 * @param onStart     已登录时点击"开始游戏"直接跳转无尽对局
 * @param onLoginClick 未登录时点击"开始游戏"的回调（可空）
 */
@Composable
fun EndlessIntroDialog(
    currentSkin: String,
    isLoggedIn: Boolean,
    onDismiss: () -> Unit,
    onStart: () -> Unit = {},
    onLoginClick: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(id = R.string.endless_intro_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.endless_intro_desc),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // 实时卡牌预览（演示动态皮肤：随 currentSkin 切换边框与图标）
                SkinTileRow(currentSkin = currentSkin)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isLoggedIn) {
                        onLoginClick?.invoke()
                    } else {
                        onDismiss()
                        onStart()
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.endless_start))
            }
        },
        dismissButton = {

        }
    )
}

/**
 * 弹窗内的一行实时卡牌预览（type 1/2/3，随皮肤变化）。
 */
@Composable
private fun SkinTileRow(currentSkin: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
    ) {
        for (type in 1..3) {
            TileCardBase(
                skin = currentSkin,
                modifier = Modifier.size(48.dp)
            ) {
                RemoteImage(
                    url = TileIconProvider.getTileUrl(currentSkin, type),
                    fallbackResId = TileIconProvider.getFallbackResId(LocalContext.current, type),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(4.dp)
                )
            }
        }
    }
}
