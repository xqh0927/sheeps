package com.example.sheeps.theme;

import com.tencent.mmkv.MMKV;
import kotlinx.coroutines.flow.StateFlow;

/**
 * 全局主题管理器（单例）
 * 使用 MMKV 持久化当前主题，切换后立即生效，无需重启 App
 */
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u00008\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000b\n\u0000\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u0006\u0010\r\u001a\u00020\u000eJ\u000e\u0010\u000f\u001a\u00020\u000e2\u0006\u0010\u0010\u001a\u00020\bJ\u0006\u0010\u000b\u001a\u00020\bJ\u0010\u0010\u0011\u001a\u00020\u00122\b\b\u0002\u0010\u0010\u001a\u00020\bJ\u0010\u0010\u0013\u001a\u00020\u00142\b\b\u0002\u0010\u0010\u001a\u00020\bR\u000e\u0010\u0004\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\b0\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\t\u001a\b\u0012\u0004\u0012\u00020\b0\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\f\u00a8\u0006\u0015"}, d2 = {"Lcom/example/sheeps/theme/ThemeManager;", "", "<init>", "()V", "THEME_KEY", "", "_currentTheme", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/example/sheeps/theme/AppTheme;", "currentTheme", "Lkotlinx/coroutines/flow/StateFlow;", "getCurrentTheme", "()Lkotlinx/coroutines/flow/StateFlow;", "init", "", "setTheme", "theme", "getThemeResId", "", "isDarkTheme", "", "core_debug"})
public final class ThemeManager {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String THEME_KEY = "app_current_theme";
    @org.jetbrains.annotations.NotNull()
    private static final kotlinx.coroutines.flow.MutableStateFlow<com.example.sheeps.theme.AppTheme> _currentTheme = null;
    @org.jetbrains.annotations.NotNull()
    private static final kotlinx.coroutines.flow.StateFlow<com.example.sheeps.theme.AppTheme> currentTheme = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.example.sheeps.theme.ThemeManager INSTANCE = null;
    
    private ThemeManager() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.example.sheeps.theme.AppTheme> getCurrentTheme() {
        return null;
    }
    
    /**
     * 在 App 启动时调用，从 MMKV 读取上次保存的主题
     */
    public final void init() {
    }
    
    /**
     * 切换到指定主题，并持久化到 MMKV
     */
    public final void setTheme(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.theme.AppTheme theme) {
    }
    
    /**
     * 获取当前主题（同步，用于非 Compose 场景）
     */
    @org.jetbrains.annotations.NotNull()
    public final com.example.sheeps.theme.AppTheme getCurrentTheme() {
        return null;
    }
    
    /**
     * 获取 XML 主题的 Style 资源 ID
     */
    public final int getThemeResId(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.theme.AppTheme theme) {
        return 0;
    }
    
    /**
     * 是否为暗色主题
     */
    public final boolean isDarkTheme(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.theme.AppTheme theme) {
        return false;
    }
}