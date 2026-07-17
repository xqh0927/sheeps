package com.example.sheeps.core.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 描述网络连接状态。
 */
enum class NetworkStatus {
    /** 联网状态 */
    ONLINE,
    /** 断网状态 */
    OFFLINE
}

/**
 * 全局网络状态监控器。
 * 使用 ConnectivityManager 的回调机制实时监听网络可用性。
 */
@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _status = MutableStateFlow(getInitialStatus())
    
    /**
     * 响应式的网络状态流。
     */
    val status: StateFlow<NetworkStatus> = _status.asStateFlow()

    init {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            // 注：NetworkMonitor 为 @Singleton（进程级单例），生命周期等同应用进程，故此处注册的
            //    NetworkCallback 无需反注册；若改为短生命周期组件持有，必须在 onDestroy 中调用
            //    unregisterNetworkCallback 以防回调持续持有引用导致泄漏。
            connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _status.value = NetworkStatus.ONLINE
                }

                override fun onLost(network: Network) {
                    // 确保所有可用网络都丢失后才标记为离线
                    if (!isNetworkConnected()) {
                        _status.value = NetworkStatus.OFFLINE
                    }
                }
            })
        } catch (e: Exception) {
            // 兜底：异常情况下默认在线，防止功能完全不可用
            _status.value = NetworkStatus.ONLINE
        }
    }

    /**
     * 检查当前是否在线。
     */
    fun isOnline(): Boolean {
        return _status.value == NetworkStatus.ONLINE
    }

    private fun getInitialStatus(): NetworkStatus {
        return if (isNetworkConnected()) NetworkStatus.ONLINE else NetworkStatus.OFFLINE
    }

    /**
     * 同步检测网络连接能力。
     */
    private fun isNetworkConnected(): Boolean {
        return try {
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }
}
