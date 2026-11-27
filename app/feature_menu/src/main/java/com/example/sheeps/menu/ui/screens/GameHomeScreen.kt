package com.example.sheeps.menu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.Toll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sheeps.lib_base.router.RouterPath
import com.example.sheeps.menu.R as MenuR
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.menu.ui.components.*
import com.example.sheeps.menu.ui.dialogs.DuelMatchDialog
import com.example.sheeps.ui.R
import com.hjq.toast.Toaster
import com.therouter.TheRouter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GameHomeScreen(
    state: MenuViewState,
    savedLevel: Int?,
    onLevelClick: (Int) -> Unit,
    onShowLeaderboard: (Int) -> Unit,
    onNoticeClick: () -> Unit,
    onLoginClick: () -> Unit,
    onJoinMatch: () -> Unit,
    onLeaveMatch: () -> Unit,
    onResetMatch: () -> Unit,
    onNavigateToDuel: (String, String, Int, Int) -> Unit
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val level = state.unlockedLevel.coerceAtLeast(1)
    var showDuel by remember { mutableStateOf(false) }
    var showEndless by rememberSaveable { mutableStateOf(false) }

    if (showDuel) {
        DuelMatchDialog(state, onJoinMatch, onLeaveMatch, onDismiss = { showDuel = false; onResetMatch() })
    }
    LaunchedEffect(showDuel, state.matchStatus, state.matchedGameId) {
        val gameId = state.matchedGameId
        if (showDuel && state.matchStatus == "matched" && !gameId.isNullOrEmpty()) {
            delay(1500)
            onNavigateToDuel(gameId, state.phone, state.duelLevel, state.gameSeed)
            showDuel = false
            onResetMatch()
        }
    }
    if (showEndless) {
        EndlessIntroDialog(state.currentSkin, state.isLoggedIn, onDismiss = { showEndless = false },
            onStart = { showEndless = false; TheRouter.build(RouterPath.Endless.PLAY).navigation(context) },
            onLoginClick = { showEndless = false; onLoginClick() })
    }

    Column(Modifier.fillMaxSize().background(colors.background).verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.widthIn(max = 620.dp).fillMaxWidth()) {
            LobbyMasthead(state.points)
            ChallengeHero(savedLevel ?: level, savedLevel != null) { onLevelClick(savedLevel ?: level) }
            AnnouncementsBanner(state.notices, onNoticeClick)
            val battle = state.gameModes?.battle == true
            val endless = state.gameModes?.endless == true
            if (battle || endless) {
                val matchHint = stringResource(R.string.home_match_hint)
                val endlessHint = stringResource(R.string.home_endless_login_hint)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (battle) DuelModeEntry(onClick = {
                        if (state.isLoggedIn) showDuel = true else { onLoginClick(); Toaster.show(matchHint) }
                    }, modifier = Modifier.weight(1f))
                    if (endless) EndlessModeEntry(state.currentSkin, state.isLoggedIn,
                        onOpenIntro = { showEndless = true },
                        onLoginClick = { onLoginClick(); Toaster.show(endlessHint) },
                        onShowLeaderboard = { TheRouter.build(RouterPath.Endless.LEADERBOARD).navigation(context) },
                        modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
            }
            StageAtlas(level, onLevelClick, onShowLeaderboard)
            Spacer(Modifier.height(LocalMenuBottomInset.current))
        }
    }
}

@Composable
private fun LobbyMasthead(points: Int) {
    val colors = MaterialTheme.colorScheme
    val balance = stringResource(R.string.points_balance, points)
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(R.string.app_name),
            modifier = Modifier.weight(1f).semantics { heading() },
            color = colors.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 37.sp,
            letterSpacing = (-1.2).sp
        )
        Spacer(Modifier.width(12.dp))
        Row(
            Modifier.background(colors.surface, RoundedCornerShape(50))
                .clearAndSetSemantics { contentDescription = balance }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Toll, null, tint = colors.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(points.toString(), color = colors.onSurface, fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun StageAtlas(level: Int, onLevelClick: (Int) -> Unit, onShowLeaderboard: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val count = maxOf(20, level + 7)
    val pageSize = 6
    val currentLevelPage = (level - 1) / pageSize
    val pages = (count + pageSize - 1) / pageSize
    val pager = rememberPagerState(initialPage = currentLevelPage) { pages }
    val scope = rememberCoroutineScope()
    var lastLevel by rememberSaveable { mutableIntStateOf(level) }
    LaunchedEffect(level) {
        if (lastLevel != level) {
            pager.scrollToPage(currentLevelPage)
            lastLevel = level
        }
    }

    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(MenuR.string.lobby_atlas), modifier = Modifier.weight(1f).semantics { heading() },
            color = colors.onBackground, fontSize = 23.sp, lineHeight = 28.sp,
            letterSpacing = (-.6).sp, fontWeight = FontWeight.Bold)
        Text("${(pager.currentPage * pageSize + 1).toString().padStart(2, '0')}–${minOf(count, (pager.currentPage + 1) * pageSize).toString().padStart(2, '0')}",
            color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        IconButton(onClick = { scope.launch { pager.animateScrollToPage(currentLevelPage) } }) {
            Icon(Icons.Default.MyLocation, stringResource(R.string.locate_current_level),
                tint = colors.primary, modifier = Modifier.size(32.dp)
                    .background(colors.surface, RoundedCornerShape(50)).padding(7.dp))
        }
    }
    HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top,
        pageSpacing = 12.dp) { page ->
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { row ->
                Row(Modifier.fillMaxWidth().heightIn(min = 65.dp).height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (column in 0..1) {
                        val id = page * pageSize + row * 2 + column + 1
                        if (id <= count) {
                            LevelAtlasTile(id, level, { onLevelClick(id) }, { onShowLeaderboard(id) }, Modifier.weight(1f))
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        val hint = stringResource(R.string.home_levels_unlocked_desc)
        TextButton(onClick = { Toaster.show(hint) }, modifier = Modifier.weight(1f, fill = false)) {
            Text(stringResource(MenuR.string.lobby_unlock_tip), color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.weight(1f))
        IconButton(enabled = pager.currentPage > 0, onClick = {
            scope.launch { pager.animateScrollToPage((pager.currentPage - 1).coerceAtLeast(0)) }
        }) { Icon(Icons.Default.ChevronLeft, stringResource(MenuR.string.lobby_previous_page)) }
        Text("${pager.currentPage + 1} / $pages", color = colors.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium)
        IconButton(enabled = pager.currentPage < pages - 1, onClick = {
            scope.launch { pager.animateScrollToPage((pager.currentPage + 1).coerceAtMost(pages - 1)) }
        }) { Icon(Icons.Default.ChevronRight, stringResource(MenuR.string.lobby_next_page)) }
    }
}
