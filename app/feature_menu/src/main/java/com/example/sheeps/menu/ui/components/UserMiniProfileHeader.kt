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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.theme.*
import com.example.sheeps.ui.components.AnimatedCounter
import com.example.sheeps.ui.components.PrimaryButton

@Composable
fun UserMiniProfileHeader(
    state: MenuViewState,
    onLoginClick: () -> Unit
) {
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
                        Gold_Subtle.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        colors = listOf(Gold_Subtle.copy(alpha = 0.6f), Gold_Primary.copy(alpha = 0.3f), Gold_Subtle.copy(alpha = 0.6f))
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
                        color  = Gold_Primary.copy(alpha = glowAlpha * 0.3f),
                        radius = this.size.width * 0.45f
                    )
                }
                // 头像圆
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Crimson_Primary, Crimson_PrimaryDark)
                            ),
                            shape = CircleShape
                        )
                        .border(
                            BorderStroke(1.5.dp, Gold_Subtle.copy(alpha = glowAlpha)),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = if (state.username.isNotEmpty()) state.username.first().toString() else "侠",
                        color      = Gold_Primary,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = if (state.isLoggedIn) state.username else "逑客云游",
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
                            tint = Gold_Primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        AnimatedCounter(
                            count = state.points,
                            style = MaterialTheme.typography.titleSmall.copy(color = Gold_Primary)
                        )
                        Text(
                            text  = " 积分",
                            style = MaterialTheme.typography.labelSmall,
                            color = Gold_Primary.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    Text(
                        text  = "登录同步存档 · 解锁演化关卡",
                        style = MaterialTheme.typography.bodySmall,
                        color = Text_Secondary_Dark
                    )
                }
            }

            if (!state.isLoggedIn) {
                PrimaryButton(
                    text     = "登录",
                    onClick  = onLoginClick,
                    height   = 36.dp,
                    modifier = Modifier.widthIn(min = 64.dp)
                )
            }
        }
    }
}
