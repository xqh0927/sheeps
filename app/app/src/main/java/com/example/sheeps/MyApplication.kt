package com.example.sheeps

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.example.sheeps.core.utils.NetworkMonitor
import com.example.sheeps.theme.ThemeManager
import com.example.sheeps.core.utils.NetworkStatus
import com.example.sheeps.data.repository.SyncRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import logcat.logcat
import javax.inject.Inject

/**
 * 全局 Application 类。
 * 初始化核心组件、第三方库，并负责监控全局生命周期与网络变化以触发数据同步。
 */
@HiltAndroidApp
class MyApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncRepository: SyncRepository

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    /**
     * 配置 WorkManager 以支持 Hilt 依赖注入。
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        
        // --- 核心初始化 ---
        // 注意：MMKV, Toaster, Logcat, TheRouter 已通过 App Startup 库自动初始化。

        // 初始化主题管理器，恢复用户上次保存的主题偏好
        ThemeManager.init()

        // 注册前后台切换监听
        setupForegroundBackgroundListener()
        
        // 注册全局网络状态变化监听
        setupNetworkObserver()
    }

    /**
     * 设置应用前后台切换监听器。
     * 当应用进入前台或切往后台时，均会触发一次离线脏数据的静默同步。
     */
    private fun setupForegroundBackgroundListener() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                logcat("AppLifecycle") { "App moved to FOREGROUND" }
                // 回到前台，检查是否有未同步的成绩或道具
                CoroutineScope(Dispatchers.IO).launch {
                    syncRepository.syncDirtyData()
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                logcat("AppLifecycle") { "App moved to BACKGROUND" }
                // 切到后台，确保本地更改尽量提交到云端
                CoroutineScope(Dispatchers.IO).launch {
                    syncRepository.syncDirtyData()
                }
            }
        })
    }

    /**
     * 设置全局网络监控。
     * 当检测到网络恢复在线时，立即触发同步任务。
     */
    private fun setupNetworkObserver() {
        CoroutineScope(Dispatchers.IO).launch {
            networkMonitor.status.collectLatest { status ->
                if (status == NetworkStatus.ONLINE) {
                    logcat("NetworkObserver") { "Network came ONLINE, triggering sync..." }
                    syncRepository.syncDirtyData()
                }
            }
        }
    }
}
