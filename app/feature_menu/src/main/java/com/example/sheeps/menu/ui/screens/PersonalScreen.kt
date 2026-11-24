package com.example.sheeps.menu.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.sheeps.lib_base.router.RouterPath
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.menu.ui.components.*
import com.example.sheeps.menu.ui.dialogs.ExchangeHistoryDialog
import com.example.sheeps.menu.ui.dialogs.PointHistoryDialog
import com.example.sheeps.menu.R as MenuR
import com.example.sheeps.ui.R
import com.therouter.TheRouter

@Composable
fun PersonalScreen(
    state: MenuViewState,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onSignInClick: () -> Unit,
    onClaimTask: (String) -> Unit,
    onApplySkin: (String) -> Unit,
    onGoToPlay: () -> Unit
) {
    val context = LocalContext.current
    var showPointHistory by rememberSaveable { mutableStateOf(false) }
    var showExchangeHistory by rememberSaveable { mutableStateOf(false) }
    var selected by rememberSaveable { mutableIntStateOf(0) }
    if (showPointHistory) PointHistoryDialog(state.pointsHistory, state.shopItems) { showPointHistory = false }
    if (showExchangeHistory) ExchangeHistoryDialog(state.exchangeHistory, state.shopItems) { showExchangeHistory = false }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = LocalMenuBottomInset.current + 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { CatalogTitle(stringResource(R.string.tab_me)) }
        item {
            UserProfileCard(state, onLoginClick, onSignInClick) {
                TheRouter.build(RouterPath.Menu.USER_INFO).navigation(context)
            }
        }
        if (state.isLoggedIn) {
            item {
                CatalogSwitch(listOf(stringResource(R.string.tasks_title), stringResource(R.string.backpack_title)), selected) { selected = it }
            }
            item {
                AnimatedContent(selected, label = "playerSection") { section ->
                    if (section == 0) DailyTasksCard(state, onClaimTask)
                    else BackpackCard(state, state.shopItems, onApplySkin, onGoToPlay)
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                CatalogLink(Icons.Default.Settings, stringResource(MenuR.string.catalog_settings_title), {
                    TheRouter.build(RouterPath.Menu.SETTINGS).navigation(context)
                })
            }
        }
        if (state.isLoggedIn) {
            item { ActionButtonsSection(state, { showPointHistory = true }, { showExchangeHistory = true }, onLogoutClick) }
        }
    }
}
