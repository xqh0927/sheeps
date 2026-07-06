package com.example.sheeps.menu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.sheeps.core.R
import com.example.sheeps.menu.state.MenuViewState

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
    onShowGameGuide: () -> Unit,
    onChangeAvatar: () -> Unit = {},
    onUpdateNickname: (String) -> Unit = {}
) {
    var showNicknameDialog by remember { mutableStateOf(false) }
    var editNickname by remember { mutableStateOf(state.username) }
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
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable(enabled = state.isLoggedIn) { onChangeAvatar() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = state.avatarUrl,
                                contentDescription = "头像",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Me",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (state.isLoggedIn) state.username else stringResource(id = R.string.user_guest),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = FontFamily.Serif
                            )
                            if (state.isLoggedIn) {
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        editNickname = state.username
                                        showNicknameDialog = true
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "编辑昵称",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
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
                        Text("${state.points}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Column {
                        Text(stringResource(id = R.string.streak_label), fontSize = 12.sp, color = Color.Gray)
                        Text(stringResource(id = R.string.streak_days, state.signStreak), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Column {
                        Text(stringResource(id = R.string.highest_level_label), fontSize = 12.sp, color = Color.Gray)
                        Text(stringResource(id = R.string.level_number, state.highestLevelCleared), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 第三部分：交互按钮（登录或签到）
                if (state.isLoggedIn) {
                    Button(
                        onClick = onSignInClick,
                        enabled = !state.todaySigned,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
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
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
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
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
            }
        }
    }

    // 昵称编辑对话框
    if (showNicknameDialog) {
        Dialog(onDismissRequest = { showNicknameDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "修改昵称",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editNickname,
                        onValueChange = { editNickname = it },
                        label = { Text("新昵称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showNicknameDialog = false }) {
                            Text("取消")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (editNickname.isNotBlank()) {
                                    onUpdateNickname(editNickname.trim())
                                    showNicknameDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("保存", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}