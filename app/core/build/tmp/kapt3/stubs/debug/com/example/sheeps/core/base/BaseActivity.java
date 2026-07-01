package com.example.sheeps.core.base;

import android.os.Bundle;
import androidx.activity.ComponentActivity;
import androidx.core.view.WindowCompat;
import com.blankj.utilcode.util.LogUtils;

/**
 * 应用内所有 Activity 的基类。
 * 提供基础的生命周期日志记录、沉浸式状态栏配置以及主题管理。
 */
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\b&\u0018\u00002\u00020\u0001B\u0007\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u0010\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u0007H\u0014J\u0012\u0010\b\u001a\u00020\u00052\b\u0010\t\u001a\u0004\u0018\u00010\nH\u0014J\u0012\u0010\u000b\u001a\u00020\u00052\b\u0010\t\u001a\u0004\u0018\u00010\nH&J\b\u0010\f\u001a\u00020\u0005H&J\b\u0010\r\u001a\u00020\u0005H\u0014\u00a8\u0006\u000e"}, d2 = {"Lcom/example/sheeps/core/base/BaseActivity;", "Landroidx/activity/ComponentActivity;", "<init>", "()V", "attachBaseContext", "", "newBase", "Landroid/content/Context;", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "initView", "initData", "onDestroy", "core_debug"})
public abstract class BaseActivity extends androidx.activity.ComponentActivity {
    
    public BaseActivity() {
        super();
    }
    
    @java.lang.Override()
    protected void attachBaseContext(@org.jetbrains.annotations.NotNull()
    android.content.Context newBase) {
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    /**
     * 在此方法中进行视图相关的初始化（如设置 Compose 内容、ViewBinding 等）。
     * @param savedInstanceState 界面销毁重建时保存的状态包
     */
    public abstract void initView(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState);
    
    /**
     * 在此方法中进行数据加载、网络请求或 ViewModel 的订阅。
     */
    public abstract void initData();
    
    @java.lang.Override()
    protected void onDestroy() {
    }
}