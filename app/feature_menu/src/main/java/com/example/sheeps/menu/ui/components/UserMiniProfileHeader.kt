package com.example.sheeps.menu.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sheeps.ui.R
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.ui.theme.*
import com.example.sheeps.ui.components.AnimatedCounter
import com.example.sheeps.ui.components.PrimaryButton

/**
 * 个人中心迷你资料头部（头像 + 昵称/积分）。
 * 头像带呼吸光晕动画；未登录时显示游客名并提供登录入口。
 *
 * @param state 菜单界面状态 [com.example.sheeps.menu.state.MenuViewState]，由 ViewModel 持有并下发。
 * @param onLoginClick 未登录时点击「登录」按钮的回调。
 *
 * 说明：
 * - 展示型组件，状态来自 [state]（单向数据流）；事件通过 [onLoginClick] 上抛。
 * - 光晕动画由 [androidx.compose.animation.core.rememberInfiniteTransition] 驱动，运行于**主线程**。
 */
@Composable
fun UserMiniProfileHeader(
    state: MenuViewState,
    onLoginClick: () -> Unit
) {
    val themeSecondary = MaterialTheme.colorScheme.secondary
    val infiniteTransition = rememberInfiniteTransition(label = "avatarGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue  = 0.85f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatarGlowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeLarge)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        colors = listOf(MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f))
                    )
                ),
                shape = ShapeLarge
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 头像 + 光晕圆框
            Box(contentAlignment = Alignment.Center) {
                // 底层光晕动画
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.size(60.dp)
                ) {
                    drawCircle(
                        color  = themeSecondary.copy(alpha = glowAlpha * 0.3f),
                        radius = this.size.width * 0.45f
                    )
                }
                // 头像圆
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                            ),
                            shape = CircleShape
                        )
                        .border(
                            BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = glowAlpha)),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = if (state.username.isNotEmpty()) state.username.first().toString() else "侠",
                        color      = MaterialTheme.colorScheme.secondary,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = if (state.isLoggedIn) state.username else stringResource(id = R.string.guest_name),
                    style      = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Serif,
                    color      = Text_Primary_Dark
                )
                Spacer(Modifier.height(4.dp))
                if (state.isLoggedIn) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        AnimatedCounter(
                            count = state.points,
                            style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.secondary)
                        )
                        Text(
                            text  = stringResource(id = R.string.unit_points),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    Text(
                        text  = stringResource(id = R.string.login_sync_prompt),
                        style = MaterialTheme.typography.bodySmall,
                        color = Text_Secondary_Dark
                    )
                }
            }

            if (!state.isLoggedIn) {
                PrimaryButton(
                    text     = stringResource(id = R.string.btn_login),
                    onClick  = onLoginClick,
                    height   = 36.dp,
                    modifier = Modifier.widthIn(min = 64.dp)
                )
            }
        }
    }
}
