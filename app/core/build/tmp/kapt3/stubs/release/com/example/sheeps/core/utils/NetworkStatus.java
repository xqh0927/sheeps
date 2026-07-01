package com.example.sheeps.core.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import dagger.hilt.android.qualifiers.ApplicationContext;
import kotlinx.coroutines.flow.StateFlow;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * 描述网络连接状态。
 */
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0005\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003j\u0002\b\u0004j\u0002\b\u0005\u00a8\u0006\u0006"}, d2 = {"Lcom/example/sheeps/core/utils/NetworkStatus;", "", "<init>", "(Ljava/lang/String;I)V", "ONLINE", "OFFLINE", "core_release"})
public enum NetworkStatus {
    /*public static final*/ ONLINE /* = new ONLINE() */,
    /*public static final*/ OFFLINE /* = new OFFLINE() */;
    
    NetworkStatus() {
    }
    
    @org.jetbrains.annotations.NotNull()
    public static kotlin.enums.EnumEntries<com.example.sheeps.core.utils.NetworkStatus> getEntries() {
        return null;
    }
}