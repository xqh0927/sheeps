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

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0007\u0018\u00002\u00020\u0001B\u0007\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u0012\u0010\u0012\u001a\u00020\u00132\b\u0010\u0014\u001a\u0004\u0018\u00010\u0015H\u0016J\b\u0010\u0016\u001a\u00020\u0013H\u0016J\b\u0010\u0017\u001a\u00020\u0013H\u0002R\u001e\u0010\u0004\u001a\u00020\u00058\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0006\u0010\u0007\"\u0004\b\b\u0010\tR\u001e\u0010\n\u001a\u00020\u000b8\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\f\u0010\r\"\u0004\b\u000e\u0010\u000fR\u000e\u0010\u0010\u001a\u00020\u0011X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0018"}, d2 = {"Lcom/example/sheeps/leaderboard/LeaderboardActivity;", "Lcom/example/sheeps/core/base/BaseActivity;", "<init>", "()V", "apiService", "Lcom/example/sheeps/data/network/ApiService;", "getApiService", "()Lcom/example/sheeps/data/network/ApiService;", "setApiService", "(Lcom/example/sheeps/data/network/ApiService;)V", "userPrefs", "Lcom/example/sheeps/core/preference/UserPreferences;", "getUserPrefs", "()Lcom/example/sheeps/core/preference/UserPreferences;", "setUserPrefs", "(Lcom/example/sheeps/core/preference/UserPreferences;)V", "levelId", "", "initView", "", "savedInstanceState", "Landroid/os/Bundle;", "initData", "showLeaderboardContent", "feature_leaderboard_debug"})
@com.therouter.router.Route(path = "/leaderboard/show")
public final class LeaderboardActivity extends com.example.sheeps.core.base.BaseActivity {
    @javax.inject.Inject()
    public com.example.sheeps.data.network.ApiService apiService;
    @javax.inject.Inject()
    public com.example.sheeps.core.preference.UserPreferences userPrefs;
    private int levelId = 1;
    
    public LeaderboardActivity() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.sheeps.data.network.ApiService getApiService() {
        return null;
    }
    
    public final void setApiService(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.network.ApiService p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.sheeps.core.preference.UserPreferences getUserPrefs() {
        return null;
    }
    
    public final void setUserPrefs(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.core.preference.UserPreferences p0) {
    }
    
    @java.lang.Override()
    public void initView(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    @java.lang.Override()
    public void initData() {
    }
    
    private final void showLeaderboardContent() {
    }
}