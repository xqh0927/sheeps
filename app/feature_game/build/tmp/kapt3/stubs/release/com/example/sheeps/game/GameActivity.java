package com.example.sheeps.game;

import android.os.Bundle;
import androidx.compose.ui.Modifier;
import com.therouter.router.Route;
import com.example.sheeps.core.base.BaseActivity;
import com.example.sheeps.game.state.GameViewIntent;
import com.example.sheeps.game.viewmodel.GameViewModel;
import com.hjq.toast.Toaster;

/**
 * 单机游戏模式 Activity。
 * 负责接收外部参数（如关卡ID、携带道具），初始化游戏界面，并处理与 UI 相关的副作用（如振动、音效、弹窗）。
 */
@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0007\u0018\u00002\u00020\u0001B\u0007\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u0012\u0010\f\u001a\u00020\r2\b\u0010\u000e\u001a\u0004\u0018\u00010\u000fH\u0016J\b\u0010\u0010\u001a\u00020\rH\u0016R\u001b\u0010\u0004\u001a\u00020\u00058BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\b\u0010\t\u001a\u0004\b\u0006\u0010\u0007R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0011"}, d2 = {"Lcom/example/sheeps/game/GameActivity;", "Lcom/example/sheeps/core/base/BaseActivity;", "<init>", "()V", "viewModel", "Lcom/example/sheeps/game/viewmodel/GameViewModel;", "getViewModel", "()Lcom/example/sheeps/game/viewmodel/GameViewModel;", "viewModel$delegate", "Lkotlin/Lazy;", "levelId", "", "initView", "", "savedInstanceState", "Landroid/os/Bundle;", "initData", "feature_game_release"})
@com.therouter.router.Route(path = "/game/play")
public final class GameActivity extends com.example.sheeps.core.base.BaseActivity {
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy viewModel$delegate = null;
    private int levelId = 1;
    
    public GameActivity() {
        super();
    }
    
    private final com.example.sheeps.game.viewmodel.GameViewModel getViewModel() {
        return null;
    }
    
    @java.lang.Override()
    public void initView(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    @java.lang.Override()
    public void initData() {
    }
}