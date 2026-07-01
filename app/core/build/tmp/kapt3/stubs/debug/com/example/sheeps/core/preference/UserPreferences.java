package com.example.sheeps.core.preference;

import android.content.Context;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;
import com.tencent.mmkv.MMKV;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000@\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b!\b\u0007\u0018\u00002\u00020\u0001B\u001b\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0001\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0004\b\u0006\u0010\u0007J\u0006\u0010\u0012\u001a\u00020\u000fJ\u0006\u0010\u0013\u001a\u00020\u000fJ\u000e\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u000fJ\u0006\u0010\u0017\u001a\u00020\u0018J\u000e\u0010\u0019\u001a\u00020\u00152\u0006\u0010\u001a\u001a\u00020\u0018J\u0006\u0010\u001b\u001a\u00020\u001cJ\u000e\u0010\u001d\u001a\u00020\u00152\u0006\u0010\u001e\u001a\u00020\u001cJ\u0006\u0010\u001f\u001a\u00020\u000fJ\u000e\u0010 \u001a\u00020\u00152\u0006\u0010!\u001a\u00020\u000fJ\b\u0010\"\u001a\u0004\u0018\u00010\u000fJ\u0010\u0010#\u001a\u00020\u00152\b\u0010$\u001a\u0004\u0018\u00010\u000fJ\b\u0010%\u001a\u0004\u0018\u00010\u000fJ\u0010\u0010&\u001a\u00020\u00152\b\u0010$\u001a\u0004\u0018\u00010\u000fJ\b\u0010\'\u001a\u0004\u0018\u00010\u000fJ\u0010\u0010(\u001a\u00020\u00152\b\u0010)\u001a\u0004\u0018\u00010\u000fJ\u0006\u0010*\u001a\u00020\u0018J\u000e\u0010+\u001a\u00020\u00152\u0006\u0010,\u001a\u00020\u0018J\u0006\u0010-\u001a\u00020\u001cJ\u0006\u0010.\u001a\u00020\u0015J\u0006\u0010/\u001a\u00020\u000fJ\u000e\u00100\u001a\u00020\u00152\u0006\u00101\u001a\u00020\u000fJ\u0006\u00102\u001a\u00020\u001cJ\u000e\u00103\u001a\u00020\u00152\u0006\u00104\u001a\u00020\u001cJ\u0006\u00105\u001a\u00020\u0018J\u000e\u00106\u001a\u00020\u00152\u0006\u00107\u001a\u00020\u0018J\u0006\u00108\u001a\u00020\u0018J\u000e\u00109\u001a\u00020\u00152\u0006\u0010\u001a\u001a\u00020\u0018J\u0006\u0010:\u001a\u00020\u000fJ\u000e\u0010;\u001a\u00020\u00152\u0006\u0010<\u001a\u00020\u000fR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001b\u0010\b\u001a\u00020\t8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\f\u0010\r\u001a\u0004\b\n\u0010\u000bR\u0011\u0010\u000e\u001a\u00020\u000f8G\u00a2\u0006\u0006\u001a\u0004\b\u0010\u0010\u0011\u00a8\u0006="}, d2 = {"Lcom/example/sheeps/core/preference/UserPreferences;", "", "kv", "Lcom/tencent/mmkv/MMKV;", "context", "Landroid/content/Context;", "<init>", "(Lcom/tencent/mmkv/MMKV;Landroid/content/Context;)V", "securePrefs", "Landroid/content/SharedPreferences;", "getSecurePrefs", "()Landroid/content/SharedPreferences;", "securePrefs$delegate", "Lkotlin/Lazy;", "userId", "", "fetchUserId", "()Ljava/lang/String;", "getUserId", "getUsername", "setUsername", "", "name", "getUnlockedLevel", "", "setUnlockedLevel", "level", "isPrivacyAccepted", "", "setPrivacyAccepted", "accepted", "getLanguage", "setLanguage", "lang", "getToken", "setToken", "token", "getRefreshToken", "setRefreshToken", "getPhone", "setPhone", "phone", "getPoints", "setPoints", "points", "isLoggedIn", "logout", "getCurrentSkin", "setCurrentSkin", "skin", "getTodaySigned", "setTodaySigned", "signed", "getSignStreak", "setSignStreak", "streak", "getHighestLevelCleared", "setHighestLevelCleared", "getLastShownDailyPopupDate", "setLastShownDailyPopupDate", "date", "core_debug"})
public final class UserPreferences {
    @org.jetbrains.annotations.NotNull()
    private final com.tencent.mmkv.MMKV kv = null;
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy securePrefs$delegate = null;
    
    @javax.inject.Inject()
    public UserPreferences(@org.jetbrains.annotations.NotNull()
    com.tencent.mmkv.MMKV kv, @dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    private final android.content.SharedPreferences getSecurePrefs() {
        return null;
    }
    
    @kotlin.jvm.JvmName(name = "fetchUserId")
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String fetchUserId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getUserId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getUsername() {
        return null;
    }
    
    public final void setUsername(@org.jetbrains.annotations.NotNull()
    java.lang.String name) {
    }
    
    public final int getUnlockedLevel() {
        return 0;
    }
    
    public final void setUnlockedLevel(int level) {
    }
    
    public final boolean isPrivacyAccepted() {
        return false;
    }
    
    public final void setPrivacyAccepted(boolean accepted) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getLanguage() {
        return null;
    }
    
    public final void setLanguage(@org.jetbrains.annotations.NotNull()
    java.lang.String lang) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getToken() {
        return null;
    }
    
    public final void setToken(@org.jetbrains.annotations.Nullable()
    java.lang.String token) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getRefreshToken() {
        return null;
    }
    
    public final void setRefreshToken(@org.jetbrains.annotations.Nullable()
    java.lang.String token) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getPhone() {
        return null;
    }
    
    public final void setPhone(@org.jetbrains.annotations.Nullable()
    java.lang.String phone) {
    }
    
    public final int getPoints() {
        return 0;
    }
    
    public final void setPoints(int points) {
    }
    
    public final boolean isLoggedIn() {
        return false;
    }
    
    public final void logout() {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getCurrentSkin() {
        return null;
    }
    
    public final void setCurrentSkin(@org.jetbrains.annotations.NotNull()
    java.lang.String skin) {
    }
    
    public final boolean getTodaySigned() {
        return false;
    }
    
    public final void setTodaySigned(boolean signed) {
    }
    
    public final int getSignStreak() {
        return 0;
    }
    
    public final void setSignStreak(int streak) {
    }
    
    public final int getHighestLevelCleared() {
        return 0;
    }
    
    public final void setHighestLevelCleared(int level) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getLastShownDailyPopupDate() {
        return null;
    }
    
    public final void setLastShownDailyPopupDate(@org.jetbrains.annotations.NotNull()
    java.lang.String date) {
    }
}