package com.example.sheeps.splash;

import android.os.Bundle;
import androidx.compose.animation.core.*;
import androidx.compose.foundation.layout.*;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.graphics.*;
import androidx.compose.ui.graphics.drawscope.Fill;
import androidx.compose.ui.graphics.drawscope.Stroke;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.style.TextAlign;
import com.blankj.utilcode.util.LogUtils;
import com.example.sheeps.core.base.BaseActivity;
import com.example.sheeps.core.preference.UserPreferences;
import com.example.sheeps.theme.*;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.hjq.toast.Toaster;
import com.therouter.TheRouter;
import com.therouter.router.Route;
import dagger.hilt.android.AndroidEntryPoint;
import kotlin.math.*;
import androidx.compose.ui.window.DialogProperties;
import javax.inject.Inject;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\b\u0007\u0018\u00002\u00020\u0001B\u0007\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u0012\u0010\n\u001a\u00020\u000b2\b\u0010\f\u001a\u0004\u0018\u00010\rH\u0016J\b\u0010\u000e\u001a\u00020\u000bH\u0016J\b\u0010\u000f\u001a\u00020\u000bH\u0002J\b\u0010\u0010\u001a\u00020\u000bH\u0002J\b\u0010\u0011\u001a\u00020\u000bH\u0002R\u001e\u0010\u0004\u001a\u00020\u00058\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0006\u0010\u0007\"\u0004\b\b\u0010\t\u00a8\u0006\u0012"}, d2 = {"Lcom/example/sheeps/splash/SplashActivity;", "Lcom/example/sheeps/core/base/BaseActivity;", "<init>", "()V", "userPrefs", "Lcom/example/sheeps/core/preference/UserPreferences;", "getUserPrefs", "()Lcom/example/sheeps/core/preference/UserPreferences;", "setUserPrefs", "(Lcom/example/sheeps/core/preference/UserPreferences;)V", "initView", "", "savedInstanceState", "Landroid/os/Bundle;", "initData", "showSplashContent", "requestPermissionsAndProceed", "navigateToMenu", "feature_splash_release"})
@com.therouter.router.Route(path = "/splash/entry")
public final class SplashActivity extends com.example.sheeps.core.base.BaseActivity {
    @javax.inject.Inject()
    public com.example.sheeps.core.preference.UserPreferences userPrefs;
    
    public SplashActivity() {
        super();
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
    
    private final void showSplashContent() {
    }
    
    private final void requestPermissionsAndProceed() {
    }
    
    private final void navigateToMenu() {
    }
}