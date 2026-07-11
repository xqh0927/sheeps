package com.example.sheeps.leaderboard

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.apkfuns.logutils.LogUtils
import com.example.sheeps.core.R
import com.example.sheeps.core.base.BaseActivity
import com.example.sheeps.core.preference.UserPreferences
import com.example.sheeps.data.model.RankingEntry
import com.example.sheeps.data.network.ApiService
import com.example.sheeps.ui.components.SheepsTopAppBar
import com.example.sheeps.theme.SheepsTheme
import com.hjq.toast.Toaster
import com.therouter.router.Route
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 无尽生存排行榜页面（总榜单，无 Tab）。
 *
 * - [com.therouter.router.Route] 路径 "/endless/leaderboard"，由介绍弹窗的"排行榜"按钮跳转。
 * - 调用 [ApiService.getLeaderboard]（levelId=0 表示无尽模式无关卡概念，game_mode=1 区分无尽榜），
 *   一次拉取前 100 名总榜。
 * - 数据加载后自动定位到当前登录用户所在行（[UserPreferences.getUsername]）。
 * - 复用同包下的 [RankingRow] 渲染每一行，并高亮当前用户。
 *
 * 所有配色均走 MaterialTheme.colorScheme 令牌。
 */
@Route(path = "/endless/leaderboard")
@AndroidEntryPoint
class EndlessLeaderboardActivity : BaseActivity() {

    @Inject
    lateinit var apiService: ApiService

    @Inject
    lateinit var userPrefs: UserPreferences

    override fun initView(savedInstanceState: Bundle?) {
        setContent {
            SheepsTheme { EndlessLeaderboardContent() }
        }
    }

    override fun initData() {}




    @Composable
    private fun EndlessLeaderboardContent() {
        var rankings by remember { mutableStateOf<List<RankingEntry>>(emptyList()) }
        var isLoading by remember { mutableStateOf(false) }
        var isDisabled by remember { mutableStateOf(false) }
        val listState = rememberLazyListState()
        val context = LocalContext.current

        // 加载总榜单（无分页 / 无 Tab；levelId=0 表示无尽模式无关卡概念，game_mode=1 区分无尽榜）
        LaunchedEffect(Unit) {
            isLoading = true
            lifecycleScope.launch {
                try {
                    val resp = apiService.getLeaderboard(0, 100, 1)
                    if (resp.success) {
                        rankings = resp.rankings
                        isDisabled = resp.disabled
                        // 自动定位到当前用户
                        val myIndex = rankings.indexOfFirst { it.username == userPrefs.getUsername() }
                        if (myIndex >= 0) {
                            listState.scrollToItem(myIndex)
                        }
                    } else {
                        Toaster.show(context.getString(R.string.leaderboard_data_error))
                    }
                } catch (e: Exception) {
                    LogUtils.e(e)
                    Toaster.show(context.getString(R.string.leaderboard_load_error))
                } finally {
                    isLoading = false
                }
            }
        }

        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                SheepsTopAppBar(
                    title = stringResource(id = R.string.endless_leaderboard_title),
                    onBack = { finish() },
                    containerColor = Color.Transparent
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (isDisabled) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.leaderboard_endless_disabled),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            fontSize = 15.sp
                        )
                    }
                } else if (rankings.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
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
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
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
