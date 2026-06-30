package com.example.sheeps.menu.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
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
import com.example.sheeps.core.utils.getLocalizedItemName
import com.example.sheeps.core.utils.getLocalizedSource
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.menu.ui.dialogs.GameGuideDialog
import com.example.sheeps.theme.*
import com.example.sheeps.ui.components.AnimatedCounter

@Composable
fun PersonalScreen(
    state: MenuViewState,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onSignInClick: () -> Unit,
    onClaimTask: (String) -> Unit,
    onChangeLanguage: (String) -> Unit,
    onThemeChange: () -> Unit
) {
    var showPointHistory by remember { mutableStateOf(false) }
    var showExchangeHistory by remember { mutableStateOf(false) }
    var showGameGuide by remember { mutableStateOf(false) }

    if (showGameGuide) {
        GameGuideDialog(onDismiss = { showGameGuide = false })
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User profile Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                    IconButton(
                        onClick = { showGameGuide = true },
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
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

        // Daily Tasks Box
        if (state.isLoggedIn) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(id = R.string.tasks_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = CrimsonRed,
                            modifier = Modifier.padding(bottom = 12.dp),
                            fontFamily = FontFamily.Serif
                        )

                        if (state.dailyTasks.isEmpty()) {
                            Text(stringResource(id = R.string.tasks_empty), fontSize = 12.sp, color = Color.Gray)
                        } else {
                            state.dailyTasks.forEach { task ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(com.example.sheeps.core.utils.getLocalizedTaskName(task.name), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${com.example.sheeps.core.utils.getLocalizedTaskName(task.description)} (${task.progress}/${task.target_count})",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    if (task.is_rewarded) {
                                        Text(stringResource(id = R.string.task_rewarded), fontSize = 12.sp, color = Color.Gray)
                                    } else {
                                        Button(
                                            onClick = { onClaimTask(task.task_id) },
                                            enabled = task.is_completed,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CrimsonRed,
                                                disabledContainerColor = Color.LightGray
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text(
                                                text = if (task.is_completed) stringResource(id = R.string.task_claim, task.points_reward) else stringResource(id = R.string.task_incomplete),
                                                fontSize = 10.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Backpack items detail grid
        if (state.isLoggedIn && state.backpackItems.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(id = R.string.backpack_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = CrimsonRed,
                            modifier = Modifier.padding(bottom = 12.dp),
                            fontFamily = FontFamily.Serif
                        )

                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            state.backpackItems.forEach { item ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                        .width(72.dp)
                                ) {
                                    Text(
                                        text = getLocalizedItemName(item.item_type),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CrimsonRed
                                    )
                                    Text(
                                        text = stringResource(id = R.string.backpack_stock, item.count),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Language settings item
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.language_settings),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = CrimsonRed,
                        modifier = Modifier.padding(bottom = 12.dp),
                        fontFamily = FontFamily.Serif
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val languages = listOf(
                            "" to stringResource(id = R.string.lang_zh),
                            "en" to stringResource(id = R.string.lang_en),
                            "tw" to stringResource(id = R.string.lang_tw),
                            "ja" to stringResource(id = R.string.lang_ja),
                            "ko" to stringResource(id = R.string.lang_ko)
                        )
                        
                        languages.forEach { (code, name) ->
                            val isSelected = state.language == code
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (isSelected) CrimsonRed else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        onChangeLanguage(code)
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Theme settings item
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.theme_settings_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = CrimsonRed,
                        modifier = Modifier.padding(bottom = 12.dp),
                        fontFamily = FontFamily.Serif
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val currentTheme = ThemeManager.currentTheme.collectAsState().value
                        val themes = listOf(
                            AppTheme.QING_RI_CHUN to stringResource(id = R.string.theme_light),
                            AppTheme.MO_YE_GOLD to stringResource(id = R.string.theme_gold),
                            AppTheme.DARK_MODE to stringResource(id = R.string.theme_dark)
                        )
                        
                        themes.forEach { (theme, name) ->
                            val isSelected = currentTheme == theme
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (isSelected) CrimsonRed else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        if (currentTheme != theme) {
                                            ThemeManager.setTheme(theme)
                                            onThemeChange()
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action Logs & Buttons
        if (state.isLoggedIn) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showPointHistory = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(0.5.dp, CrimsonRed),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(id = R.string.btn_view_points_log), color = CrimsonRed)
                    }

                    Button(
                        onClick = { showExchangeHistory = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(0.5.dp, CrimsonRed),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(id = R.string.btn_view_exchange_log), color = CrimsonRed)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onLogoutClick,
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(id = R.string.btn_logout), color = Color.White)
                    }
                }
            }
        }
    }


    // Modal point history dialog
    if (showPointHistory) {
        AlertDialog(
            onDismissRequest = { showPointHistory = false },
            title = { Text(stringResource(id = R.string.points_history_title), fontWeight = FontWeight.Bold, color = CrimsonRed) },
            text = {
                if (state.pointsHistory.isEmpty()) {
                    Text(stringResource(id = R.string.no_points_history))
                } else {
                    LazyColumn(modifier = Modifier.height(240.dp)) {
                        items(state.pointsHistory) { record ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(getLocalizedSource(record.source), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(record.created_at)),
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                                Text(
                                    text = if (record.type == "IN") "+${record.amount}" else "-${record.amount}",
                                    color = if (record.type == "IN") Color(0xFF4CAF50) else CrimsonRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPointHistory = false }) { Text(stringResource(id = R.string.btn_cancel)) }
            }
        )
    }

    // Modal exchange history dialog
    if (showExchangeHistory) {
        AlertDialog(
            onDismissRequest = { showExchangeHistory = false },
            title = { Text(stringResource(id = R.string.exchange_history_title), fontWeight = FontWeight.Bold, color = CrimsonRed) },
            text = {
                if (state.exchangeHistory.isEmpty()) {
                    Text(stringResource(id = R.string.no_exchange_history))
                } else {
                    LazyColumn(modifier = Modifier.height(240.dp)) {
                        items(state.exchangeHistory) { record ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(stringResource(id = R.string.exchange_record_desc, record.count, getLocalizedItemName(record.item_type)), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(record.created_at)),
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                                Text(
                                    text = "-${record.points_cost}",
                                    color = CrimsonRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExchangeHistory = false }) { Text(stringResource(id = R.string.btn_cancel)) }
            }
        )
    }
}
