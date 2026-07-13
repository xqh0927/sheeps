package com.example.sheeps

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
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
class MyApplication : Application(), Configuration.Provider, ImageLoaderFactory {

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

    /**
     * 配置 Coil 全局 [ImageLoader]（实现 [ImageLoaderFactory]）。
     * 统一开启磁盘/内存/网络三级缓存与 crossfade 过渡，供 [TileIconProvider] 远程卡面
     * 与 [com.example.sheeps.ui.components.RemoteImage] 使用。
     *
     * 注意：Coil 2.x 的 [ImageLoaderFactory.newImageLoader] 方法签名为无参；Application 自身即
     * [android.content.Context]，故以 `this`（捕获为 [appContext]）作为各 Builder 的上下文，
     * 避免 lambda 内 `this` 指代 [ImageLoader.Builder] 而导致上下文类型错误。
     */
    override fun newImageLoader(): ImageLoader {
        val appContext: Application = this
        return ImageLoader.Builder(appContext)
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder(appContext)
                    .maxSizePercent(0.25)
                    .weakReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(appContext.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .build()
    }

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
        // 匿名 DefaultLifecycleObserver 注册到 ProcessLifecycleOwner（进程级生命周期，与 App 同寿，
        // 无需反注册，不会泄漏）。onStart/onStop 内各启动一个短生存的 CoroutineScope(Dispatchers.IO)
        // 执行一次性同步，协程会自然结束；如需可取消，可改用 lifecycleScope。
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
        // ⚠️ 协程/内存隐患：此处使用独立的顶层 `CoroutineScope(Dispatchers.IO)` 启动并永久 `collectLatest`
        // 网络状态流，该 CoroutineScope 全程没有任何地方调用 `cancel()`，会随进程一直存活，
        // 并持续持有 `syncRepository`（间接持有 Application 上下文）的引用。
        // 因 Application 本身与进程同生命周期，这不会造成传统泄漏，但属于「永不取消的全局协程」，
        // 会在后台持续触发同步、驻留引用。
        // 建议：改用 `ProcessLifecycleOwner.get().lifecycleScope` 启动收集，使流随进程生命周期自动取消。
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
