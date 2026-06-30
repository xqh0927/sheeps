package com.example.sheeps.menu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sheeps.core.R
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.theme.CrimsonRed

/**
 * 用户信息卡片组件
 * 展示用户基本信息、积分、签到天数和最高关卡，并提供登录/签到及游戏指南入口
 *
 * @param state 界面状态数据
 * @param onLoginClick 登录点击回调
 * @param onSignInClick 签到点击回调
 * @param onShowGameGuide 显示游戏指南回调
 */
@Composable
fun UserProfileCard(
    state: MenuViewState,
    onLoginClick: () -> Unit,
    onSignInClick: () -> Unit,
    onShowGameGuide: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 第一部分：头像与昵称/UID
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(CrimsonRed, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Me",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = if (state.isLoggedIn) state.username else stringResource(id = R.string.user_guest),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonRed,
                            fontFamily = FontFamily.Serif
                        )
                        Text(
                            text = if (state.isLoggedIn) "UID: ${state.phone}" else stringResource(id = R.string.user_not_logged_in),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 第二部分：积分、签到天数、最高关卡数据展示
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(stringResource(id = R.string.points_balance_label), fontSize = 12.sp, color = Color.Gray)
                        Text("${state.points}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = CrimsonRed)
                    }
                    Column {
                        Text(stringResource(id = R.string.streak_label), fontSize = 12.sp, color = Color.Gray)
                        Text(stringResource(id = R.string.streak_days, state.signStreak), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = CrimsonRed)
                    }
                    Column {
                        Text(stringResource(id = R.string.highest_level_label), fontSize = 12.sp, color = Color.Gray)
                        Text(stringResource(id = R.string.level_number, state.highestLevelCleared), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = CrimsonRed)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 第三部分：交互按钮（登录或签到）
                if (state.isLoggedIn) {
                    Button(
                        onClick = onSignInClick,
                        enabled = !state.todaySigned,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CrimsonRed,
                            disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (state.todaySigned) stringResource(id = R.string.btn_signed_today) else stringResource(id = R.string.btn_sign_today),
                            color = if (state.todaySigned) Color.DarkGray else Color.White
                        )
                    }
                } else {
                    Button(
                        onClick = onLoginClick,
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(id = R.string.btn_login_sync), color = Color.White)
                    }
                }
            }
            
            // 游戏指南入口图标
            IconButton(
                onClick = onShowGameGuide,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = stringResource(id = R.string.description_game_guide),
                    tint = CrimsonRed.copy(alpha = 0.8f)
                )
            }
        }
    }
}
