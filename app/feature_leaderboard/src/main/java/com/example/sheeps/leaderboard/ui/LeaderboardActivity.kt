package com.example.sheeps.leaderboard.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apkfuns.logutils.LogUtils
import com.example.sheeps.ui.R
import com.example.sheeps.lib_base.base.BaseActivity
import com.example.sheeps.data.preference.UserPreferences
import com.example.sheeps.data.model.RankingEntry
import com.example.sheeps.data.network.ApiService
import com.example.sheeps.ui.components.SheepsTopAppBar
import com.example.sheeps.ui.theme.SheepsTheme
import com.example.sheeps.lib_base.router.RouterPath
import com.hjq.toast.Toaster
import com.therouter.router.Route
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject

@Route(path = RouterPath.Leaderboard.SHOW)
@AndroidEntryPoint
class LeaderboardActivity : BaseActivity() {

    @Inject
    lateinit var apiService: ApiService

    @Inject
    lateinit var userPrefs: UserPreferences

    private var levelId: Int = 1

    override fun initView(savedInstanceState: Bundle?) {
        levelId = intent.getIntExtra("levelId", 1)
        showLeaderboardContent()
    }

    override fun initData() {
        // 异步请求与加载已被迁移到 Composable 屏幕组件生命周期内处理
    }

    private fun showLeaderboardContent() {
        setContent {
            SheepsTheme {
                var selectedTab by remember { mutableStateOf("history") }
                val currentPlayerId = remember { userPrefs.accountId() ?: userPrefs.getUserId() }
                var rankings by remember { mutableStateOf<List<RankingEntry>>(emptyList()) }
                var isLoading by remember { mutableStateOf(false) }
                var showRulesDialog by remember { mutableStateOf(false) }

                LaunchedEffect(selectedTab) {
                    isLoading = true
                    rankings = emptyList()
                    try {
                        val response = apiService.getLeaderboardPaged(levelId, selectedTab, 1, 50)
                        if (response.success) rankings = response.rankings
                        else Toaster.show(getString(R.string.leaderboard_data_error))
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        LogUtils.e(e)
                        Toaster.show(getString(R.string.leaderboard_load_error))
                    } finally {
                        isLoading = false
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        LeaderboardAppBar(
                            levelId = levelId,
                            onBack = { finish() }
                        )

                        // 积分规则说明弹窗
                        if (showRulesDialog) {
                            AlertDialog(
                                onDismissRequest = { showRulesDialog = false },
                                title = { Text(stringResource(id = R.string.leaderboard_rules_title)) },
                                text = {
                                    Text(
                                        stringResource(id = R.string.leaderboard_rules_text)
                                    )
                                },
                                confirmButton = {
                                    androidx.compose.material3.TextButton(onClick = {
                                        showRulesDialog = false
                                    }) {
                                        Text(stringResource(id = R.string.daily_popup_btn))
                                    }
                                }
                            )
                        }

                        LeaderboardTabs(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it },
                            onShowRules = { showRulesDialog = true }
                        )

                        if (isLoading) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            if (rankings.isEmpty()) {
                                RankingNotice(stringResource(R.string.leaderboard_empty), Modifier.weight(1f).fillMaxWidth())
                            } else {
                                RankingBoard(rankings, currentPlayerId, Modifier.weight(1f).fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardAppBar(
    levelId: Int,
    onBack: () -> Unit
) {
    SheepsTopAppBar(
        title = stringResource(id = R.string.leaderboard_title_format, levelId),
        onBack = onBack,
        containerColor = Color.Transparent
    )
}

@Composable
fun LeaderboardTabs(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onShowRules: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tabs = listOf(
            "daily" to stringResource(R.string.leaderboard_tab_daily),
            "weekly" to stringResource(R.string.leaderboard_tab_weekly),
            "history" to stringResource(R.string.leaderboard_tab_history)
        )
        Row(
            Modifier.weight(1f).background(colors.surfaceContainerHigh, CircleShape)
                .selectableGroup().padding(3.dp)
        ) {
            tabs.forEach { (tabKey, tabLabel) ->
                val selected = selectedTab == tabKey
                val pill = if (selected) Modifier.shadow(2.dp, CircleShape).background(colors.surface, CircleShape)
                    else Modifier
                Box(
                    Modifier.weight(1f).then(pill).clip(CircleShape)
                        .selectable(selected, role = Role.Tab) { onTabSelected(tabKey) }
                        .heightIn(min = 42.dp).padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(tabLabel, color = if (selected) colors.onSurface else colors.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onShowRules) {
            Icon(Icons.Default.Info, stringResource(R.string.leaderboard_rules_title),
                tint = colors.primary, modifier = Modifier.size(22.dp))
        }
    }
}
