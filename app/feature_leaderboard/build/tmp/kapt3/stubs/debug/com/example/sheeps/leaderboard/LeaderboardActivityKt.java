package com.example.sheeps.leaderboard;

import android.os.Bundle;
import androidx.compose.foundation.layout.*;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.style.TextAlign;
import com.blankj.utilcode.util.LogUtils;
import com.example.sheeps.core.base.BaseActivity;
import com.example.sheeps.core.R;
import com.example.sheeps.core.preference.UserPreferences;
import com.example.sheeps.data.model.RankingEntry;
import com.example.sheeps.data.network.ApiService;
import com.hjq.toast.Toaster;
import com.therouter.router.Route;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

@kotlin.Metadata(mv = {2, 2, 0}, k = 2, xi = 48, d1 = {"\u00000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\u001a\u001e\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00010\u0005H\u0007\u001a$\u0010\u0006\u001a\u00020\u00012\u0006\u0010\u0007\u001a\u00020\b2\u0012\u0010\t\u001a\u000e\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\u00010\nH\u0007\u001a \u0010\u000b\u001a\u00020\u00012\u0006\u0010\f\u001a\u00020\u00032\u0006\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\u0010H\u0007\u00a8\u0006\u0011"}, d2 = {"LeaderboardAppBar", "", "levelId", "", "onBack", "Lkotlin/Function0;", "LeaderboardTabs", "selectedTab", "", "onTabSelected", "Lkotlin/Function1;", "RankingRow", "index", "entry", "Lcom/example/sheeps/data/model/RankingEntry;", "isCurrentUser", "", "feature_leaderboard_debug"})
public final class LeaderboardActivityKt {
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void LeaderboardAppBar(int levelId, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onBack) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void LeaderboardTabs(@org.jetbrains.annotations.NotNull()
    java.lang.String selectedTab, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onTabSelected) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void RankingRow(int index, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.model.RankingEntry entry, boolean isCurrentUser) {
    }
}