package com.example.sheeps.leaderboard

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.apkfuns.logutils.LogUtils
import com.example.sheeps.core.R
import com.example.sheeps.core.base.BaseActivity
import com.example.sheeps.core.preference.UserPreferences
import com.example.sheeps.data.model.RankingEntry
import com.example.sheeps.data.network.ApiService
import com.example.sheeps.theme.CrimsonRed
import com.example.sheeps.theme.GoldenBronze
import com.example.sheeps.theme.ImperialYellow
import com.example.sheeps.theme.SheepsTheme
import com.hjq.toast.Toaster
import com.therouter.router.Route
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@Route(path = "/leaderboard/show")
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
                var selectedTab by remember { mutableStateOf("history") } // 可选标签：daily (每日榜), weekly (每周榜), history (总榜)
                var rankings by remember { mutableStateOf<List<RankingEntry>>(emptyList()) }
                var isLoading by remember { mutableStateOf(false) }

                val loadRankings = {
                    isLoading = true
                    lifecycleScope.launch {
                        try {
                            LogUtils.d("Fetching rankings for level: $levelId, type: $selectedTab")
                            val response =
                                apiService.getLeaderboardPaged(levelId, selectedTab, 1, 50)
                            if (response.success) {
                                rankings = response.rankings
                            } else {
                                Toaster.show(getString(R.string.leaderboard_data_error))
                            }
                        } catch (e: Exception) {
                            LogUtils.e(e)
                            Toaster.show(getString(R.string.leaderboard_load_error))
                        } finally {
                            isLoading = false
                        }
                    }
                }

                LaunchedEffect(selectedTab) {
                    loadRankings()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()) {
                        LeaderboardAppBar(
                            levelId = levelId,
                            onBack = { finish() }
                        )

                        LeaderboardTabs(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it }
                        )

                        if (isLoading) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = CrimsonRed)
                            }
                        } else {
                            if (rankings.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.leaderboard_empty),
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                        fontSize = 15.sp
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    itemsIndexed(rankings) { index, entry ->
                                        RankingRow(
                                            index = index,
                                            entry = entry,
                                            isCurrentUser = entry.username == userPrefs.getUsername()
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardAppBar(
    levelId: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.leaderboard_title_format, levelId),
                fontWeight = FontWeight.Bold,
                color = CrimsonRed,
                fontSize = 20.sp
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(id = R.string.btn_back),
                    tint = CrimsonRed
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
fun LeaderboardTabs(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(4.dp)
    ) {
        val tabs = listOf(
            "daily" to stringResource(id = R.string.leaderboard_tab_daily),
            "weekly" to stringResource(id = R.string.leaderboard_tab_weekly),
            "history" to stringResource(id = R.string.leaderboard_tab_history)
        )
        tabs.forEach { (tabKey, tabLabel) ->
            val isSelected = selectedTab == tabKey
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) CrimsonRed else Color.Transparent)
                    .border(
                        width = if (isSelected) 1.dp else 0.dp,
                        color = if (isSelected) GoldenBronze else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onTabSelected(tabKey) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tabLabel,
                    color = if (isSelected) ImperialYellow else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun RankingRow(
    index: Int,
    entry: RankingEntry,
    isCurrentUser: Boolean
) {
    val medalColor = when (index) {
        0 -> Color(0xFFFFD700) // 金牌
        1 -> Color(0xFFC0C0C0) // 银牌
        2 -> Color(0xFFCD7F32) // 铜牌
        else -> Color.Transparent
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isCurrentUser) CrimsonRed.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isCurrentUser) 1.dp else 0.dp,
                color = if (isCurrentUser) CrimsonRed else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = if (index < 3) medalColor else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (index + 1).toString(),
                    color = if (index < 3) CrimsonRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.username,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrentUser) CrimsonRed else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(
                        id = R.string.leaderboard_clear_time_format,
                        entry.clear_time_ms / 1000.0
                    ),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Box(
                modifier = Modifier
                    .border(1.dp, CrimsonRed.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.leaderboard_score_format, entry.score),
                    color = CrimsonRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
