package com.example.sheeps.menu;

import android.os.Bundle;
import androidx.compose.animation.*;
import androidx.compose.animation.core.*;
import androidx.compose.foundation.layout.*;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.graphics.Brush;
import com.example.sheeps.core.base.BaseActivity;
import com.example.sheeps.menu.state.*;
import com.example.sheeps.menu.ui.components.*;
import com.example.sheeps.menu.ui.dialogs.*;
import com.example.sheeps.menu.ui.screens.*;
import com.example.sheeps.menu.viewmodel.MenuViewModel;
import com.example.sheeps.theme.*;
import com.hjq.toast.Toaster;
import com.therouter.TheRouter;
import com.therouter.router.Route;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;
import com.example.sheeps.data.network.ApiService;
import com.example.sheeps.core.preference.UserPreferences;
import com.example.sheeps.data.model.DailyPopupResponse;

/**
 * 游戏主入口菜单 Activity。
 * 承载了游戏主页、商店、个人中心三个主要 Tab 页。
 * 负责处理多语言切换、登录冲突解决、App 更新检测以及全局弹窗逻辑。
 */
@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000:\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0007\u0018\u00002\u00020\u0001B\u0007\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u0012\u0010\u001c\u001a\u00020\u001d2\b\u0010\u001e\u001a\u0004\u0018\u00010\u001fH\u0016J\b\u0010 \u001a\u00020\u001dH\u0016J\b\u0010!\u001a\u00020\u001dH\u0014R\u001b\u0010\u0004\u001a\u00020\u00058BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\b\u0010\t\u001a\u0004\b\u0006\u0010\u0007R\u001e\u0010\n\u001a\u00020\u000b8\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\f\u0010\r\"\u0004\b\u000e\u0010\u000fR\u001e\u0010\u0010\u001a\u00020\u00118\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0012\u0010\u0013\"\u0004\b\u0014\u0010\u0015R\u001e\u0010\u0016\u001a\u00020\u00178\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0018\u0010\u0019\"\u0004\b\u001a\u0010\u001b\u00a8\u0006\""}, d2 = {"Lcom/example/sheeps/menu/MenuActivity;", "Lcom/example/sheeps/core/base/BaseActivity;", "<init>", "()V", "viewModel", "Lcom/example/sheeps/menu/viewmodel/MenuViewModel;", "getViewModel", "()Lcom/example/sheeps/menu/viewmodel/MenuViewModel;", "viewModel$delegate", "Lkotlin/Lazy;", "json", "Lkotlinx/serialization/json/Json;", "getJson", "()Lkotlinx/serialization/json/Json;", "setJson", "(Lkotlinx/serialization/json/Json;)V", "apiService", "Lcom/example/sheeps/data/network/ApiService;", "getApiService", "()Lcom/example/sheeps/data/network/ApiService;", "setApiService", "(Lcom/example/sheeps/data/network/ApiService;)V", "userPrefs", "Lcom/example/sheeps/core/preference/UserPreferences;", "getUserPrefs", "()Lcom/example/sheeps/core/preference/UserPreferences;", "setUserPrefs", "(Lcom/example/sheeps/core/preference/UserPreferences;)V", "initView", "", "savedInstanceState", "Landroid/os/Bundle;", "initData", "onResume", "feature_menu_release"})
@com.therouter.router.Route(path = "/menu/main")
public final class MenuActivity extends com.example.sheeps.core.base.BaseActivity {
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy viewModel$delegate = null;
    @javax.inject.Inject()
    public kotlinx.serialization.json.Json json;
    @javax.inject.Inject()
    public com.example.sheeps.data.network.ApiService apiService;
    @javax.inject.Inject()
    public com.example.sheeps.core.preference.UserPreferences userPrefs;
    
    public MenuActivity() {
        super();
    }
    
    private final com.example.sheeps.menu.viewmodel.MenuViewModel getViewModel() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.serialization.json.Json getJson() {
        return null;
    }
    
    public final void setJson(@org.jetbrains.annotations.NotNull()
    kotlinx.serialization.json.Json p0) {
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
    
    @java.lang.Override()
    protected void onResume() {
    }
}