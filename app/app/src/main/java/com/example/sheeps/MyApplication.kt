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
import androidx.lifecycle.lifecycleScope
import androidx.work.Configuration
import com.example.sheeps.lib_base.base.BaseActivityThemeDelegate
import com.example.sheeps.data.utils.NetworkMonitor
import com.example.sheeps.ui.theme.ThemeManager
import com.example.sheeps.data.repository.SyncRepository
import com.tencent.mmkv.MMKV
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers
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

    @Inject
    lateinit var kv: MMKV

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

        // 绑定底层 BaseActivity 主题提供者委托，打破物理反向依赖并让所有 Activity 一键获得主题设置支持
       BaseActivityThemeDelegate.themeProvider = {
            ThemeManager.getThemeResId()
        }

        // 初始化主题管理器，恢复用户上次保存的主题偏好
        ThemeManager.init(kv)

        // 注册前后台切换监听：仅在前后台切换时触发一次离线脏数据同步
        setupForegroundBackgroundListener()

        // ⚠️ 改动（P2-13）：原 setupNetworkObserver() 会在进程级「永不取消」的全局协程中
        // 监听网络恢复在线并每次都打 /user/sync。由于 syncDirtyData() 无脏数据时本身为 no-op，
        // 且同步已收敛到前后台切换（onStart/onStop），此处不再注册网络常驻同步，
        // 避免每次联网都触发一次同步请求。
    }

    /**
     * 设置应用前后台切换监听器。
     * 仅当应用进入前台（onStart）或切往后台（onStop）时，各触发一次离线脏数据的静默同步。
     *
     * 改动（P2-13）：
     *  - 原实现在 onStart/onStop 内使用顶层 `CoroutineScope(Dispatchers.IO)` 启动一次性同步协程，
     *    该作用域无统一生命周期管理；
     *  - 现改为使用 `owner.lifecycleScope`（即 ProcessLifecycleOwner 绑定的作用域），
     *    协程随进程生命周期自动取消，不再「永不取消」；
     *  - 同步仍只在前后台切换时触发一次，保持同步正确性（syncDirtyData() 无脏数据时 no-op 返回）。
     */
    private fun setupForegroundBackgroundListener() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                logcat("AppLifecycle") { "App moved to FOREGROUND" }
                // 回到前台，检查是否有未同步的成绩或道具
                owner.lifecycleScope.launch(Dispatchers.IO) {
                    syncRepository.syncDirtyData()
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                logcat("AppLifecycle") { "App moved to BACKGROUND" }
                // 切到后台，确保本地更改尽量提交到云端
                owner.lifecycleScope.launch(Dispatchers.IO) {
                    syncRepository.syncDirtyData()
                }
            }
        })
    }
}
